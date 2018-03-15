package net.zettroke.PropertyHeatMapServer.map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.zettroke.PropertyHeatMapServer.Main;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphBuilder;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphLine;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNodeBuilder;
import net.zettroke.PropertyHeatMapServer.utils.*;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;


/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    static final int earthRadius = 6371000;

    public static int default_zoom = 19;
    public static int cache_size = 10;
    public static int MAP_RESOLUTION = (int)Math.pow(2, default_zoom)*256; //(2**19)*256
    private final static HashSet<String> supportedRoutes = new HashSet<>(Arrays.asList("subway", "tram", "trolleybus", "bus"));
    public final static HashSet<String> keysInfrastructureObject = new HashSet<>(Arrays.asList("shop", "amenity", "craft"));
    final static HashSet<String> available_amenity = new HashSet<>(Arrays.asList("pharmacy", "kindergarten", "post_office", "police", "library", "clinic",
            "veterinary", "dentist", "bank", "atm", "cafe"));

    // Speed in meters per 0.1 seconds.
    public static final float car_speed = 1.38888f;
    public static final float foot_speed = 0.138888f;
    public static final float subway_speed = 1.25f;
    public static final float bus_speed = 0.8333f;
    public static final float tram_speed = 0.55555f;

    public boolean test = true;

    MapLoader loader;

    public int x_begin=0, y_begin=0;
    public int x_end=0, y_end=0;

    public StringPredictor predictor = new StringPredictor();
    public HashMap<String, Way> searchMap = new HashMap<>();

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    public HashMap<Long, Node> nodes = new HashMap<>();
    public HashMap<Long, Way> ways = new HashMap<>();
    public HashMap<Long, Relation> relations = new HashMap<>();

    // Что бы каждый раз при запросе инфраструктуры не нужно было искать бижайшие ноды, я нахожу их во время инициализации.
    public HashMap<Long, RoadGraphNode[]> infrastructure_connections = new HashMap<>();

    RoadGraphBuilder rgnBuilder;
    public HashMap<Long, RoadGraphNode> roadGraph;
    public CalculatedGraphCache cache;

    public QuadTree tree;

    RoadGraphNode start_node;

    int[] mercator(double lon, double lat){
        int x = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.toRadians(lon)+Math.PI))-x_begin);
        int y = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.PI-Math.log(Math.tan(Math.toRadians(lat)/2+Math.PI/4))))-y_begin);
        return new int[]{x, y};
    }

    double[] inverse_mercator(double x, double y){
        double lon = Math.toDegrees((x + x_begin)/(MAP_RESOLUTION/2/Math.PI) - Math.PI);
        double lat = Math.toDegrees(-(2*Math.atan(Math.exp((y + y_begin)/(MAP_RESOLUTION/2/Math.PI) - Math.PI)) - Math.PI/2));
        return new double[]{lon, lat};
    }

    public PropertyMap(MapLoader loader) {
        this.loader = loader;
    }

    class ParallelInitThread extends Thread{

        QuadTreeNode t;
        PropertyMap m;

        ParallelInitThread(QuadTreeNode t, PropertyMap m) {
            super();
            this.t = t;
            this.m = m;
        }

        @Override
        public void run() {
            for (Node n: m.nodes.values()){
                if (t.inBounds(n)){
                    t.add(n);
                }
            }
            //System.out.println(getName() + " Done with nodes!");
            //int cnt = 0;
            int cnt = 0;
            for (Way w: ways.values()){
                if (w.data.containsKey("building") || w.data.containsKey("highway")){// || ways.get(i).data.containsKey("railway")) {
                    t.add(new MapShape(w));
                }
                cnt++;
            }
        }
    }

    boolean canCovertToSimpleNode(Node n){
        if (!Collections.disjoint(n.data.keySet(), keysInfrastructureObject)){
            return !(!n.data.containsKey("amenity") || (n.data.containsKey("amenity") && available_amenity.contains(n.data.get("amenity"))));
        }else{
            return !rgnBuilder.isRGN(n);
        }
    }

    public void init(){
        try {
            int[] coords = loader.getCoordBounds(this);

            x_begin = coords[0]; y_begin = coords[1]; x_end = coords[2]; y_end = coords[3];

            rgnBuilder = new RoadGraphBuilder(new int[]{0, 0, x_end - x_begin, y_end - y_begin}, cache_size);

            loader.load(rgnBuilder, this);

            nodes = loader.getNodes();
            ways = loader.getWays();
            simpleNodes = loader.getSimpleNodes();
            relations = loader.getRelations();

            System.out.println("Creating QuadTree...");
            tree = new QuadTree(new int[]{0, 0, x_end - x_begin, y_end - y_begin});
            tree.root.split();
            long start = System.nanoTime();
            ArrayList<ParallelInitThread> threads = new ArrayList<>();
            for (QuadTreeNode t : tree.root) {
                ParallelInitThread p = new ParallelInitThread(t, this);
                //t.th_temp = p;
                threads.add(p);
                p.start();
            }
            for (ParallelInitThread pit : threads) {
                try {
                    pit.join();
                } catch (InterruptedException e) {}
            }

            System.out.println("QuadTree created in " + (System.nanoTime() - start) / 1000000.0 + " millis.");

            start = System.nanoTime();
            load_prices();
            System.out.println("Loaded prices in " + (System.nanoTime() - start) / 1000000.0 + " millis.");

            start = System.nanoTime();
            public_transport_init();
            System.out.println("Public transport init in " + (System.nanoTime() - start) / 1000000.0 + " millis.");
            roadGraph = rgnBuilder.getRoadGraph();
            rgnBuilder = null;
            for (RoadGraphNode rgn: roadGraph.values()){
                tree.add(rgn);
            }
            cache = new CalculatedGraphCache(roadGraph, cache_size);

            for (Node n: nodes.values()){
                if (n.data != null){
                    if (n.data.size() != 0){
                        if (!Collections.disjoint(n.data.keySet(), keysInfrastructureObject)) {
                            if (!n.data.containsKey("amenity") || (n.data.containsKey("amenity") && available_amenity.contains(n.data.get("amenity")))) {

                                List<RoadGraphNode> temp = findRoadGraphNodesInCircle(n, 300);
                                if (temp.size() < 4) {
                                    temp = findRoadGraphNodesInCircle(n, 600);
                                }
                                infrastructure_connections.put(n.id, temp.toArray(new RoadGraphNode[temp.size()]));
                            }
                        }
                    }
                }
            }
            init_roadGraphLines();
            /*for (RoadGraphNode rgn: roadGraph.values()){
                rgn.dist[0] = Integer.MAX_VALUE;
            }
            RoadGraphNode st = roadGraph.get(256021027L);
            st.dist[0] = 0;
            RoadGraphNode[] src = new RoadGraphNode[roadGraph.size()];
            RoadGraphNode[] dst = new RoadGraphNode[roadGraph.size()];
            src[0] = st;
            start_node = st;
            widthRecCalculateDistance(src, dst,5000, 0, 0);
            System.out.println(big_cnt);
            BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)image.getGraphics();
            g.setColor(new Color(0, 0, 0 ,0));
            g.fillRect(0, 0, 1024, 1024);
            drawGraph(g, 5000);
            ImageIO.write(image, "png", new File("skak.png"));

            //int mult = (int) Math.pow(2, PropertyMap.default_zoom - 16);
            //double cf = 1.0 / mult;
            //BufferedImage image2 = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
            //Graphics2D g2 = (Graphics2D) image2.getGraphics();
            ////g.setColor(new Color(255, 0, 0, 255));
            ////g2.fillRect(0 ,0, 1024, 1024);
            //g2.setColor(new Color(255, 0, 0));
            //g2.setStroke(new BasicStroke(50f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            //g2.drawRect(coef(start_node.n.x, cf)-20, coef(start_node.n.y, cf)-20, 40, 40);
            //g2.fillRect(coef(start_node.n.x, cf)-10, coef(start_node.n.y, cf)-10, 20, 20);
            //ImageIO.write(image2, "png", new File("start_overlay.png"));
            System.exit(0);*/
            //init_node = treeNode;
            System.gc();
        }catch (Exception e){
            System.err.println("PropertyMapInit Failed!!!");
            e.printStackTrace();
        }
    }

    private void init_roadGraphLines(){
        boolean[] visited = new boolean[roadGraph.size()];
        for (RoadGraphNode rgn: roadGraph.values()){
            visited[rgn.index] = true;
            for (int i=0; i<rgn.ref_to[0].length; i++){
                if (!visited[rgn.ref_to[0][i].index]) {
                    tree.add(new RoadGraphLine(rgn, rgn.ref_to[0][i], rgn.ref_types[0][i]));
                }
            }
        }
    }

    private void public_transport_init(){
        int cnt = 0;
        System.out.println(relations.size());
        for (Relation rel: relations.values()){
            cnt++;
            if ((cnt+1)%10000 == 0){
                System.out.println("public transport " + cnt/(double)relations.size());
            }
            if (rel.data.containsKey("route_master") && supportedRoutes.contains(rel.data.get("route_master"))){
                Relation r = null;
                for (int i=0; i<rel.relations.size(); i++){
                    if (rel.relations.get(i) != null && rel.relations.get(i).data.containsKey("route")){
                        r = rel.relations.get(i);
                        break;
                    }
                }
                if (r == null) continue;
                if (r.nodes.size() == 1) continue;
                Way way = new Way();
                int relation_way_index_begin = -1;
                int relation_way_index_end = -1;
                int relation_node_index_begin = -1;
                int relation_node_index_end = -1;

                // finding first and last not null way
                for (int i=0; i<r.ways.size(); i++){
                    if (r.ways.get(i) != null && relation_way_index_begin == -1){
                        relation_way_index_begin = i;
                    }
                    if (relation_way_index_begin != -1 && r.ways.get(i) == null){
                        relation_way_index_end = i;
                        break;
                    }
                }
                if (relation_way_index_end == -1){relation_way_index_end = r.ways.size();}
                if (relation_way_index_begin == -1) continue;

                // finding first and last not null node
                for (int i=0; i<r.nodes.size(); i++){
                    if (r.nodes.get(i) != null && relation_node_index_begin == -1){
                        relation_node_index_begin = i;
                    }else if (relation_node_index_begin != -1 && r.nodes.get(i) == null){
                        relation_node_index_end = i;
                        break;
                    }
                }
                if (relation_node_index_end == -1){relation_node_index_end = r.nodes.size();}
                if (relation_node_index_begin == -1) continue;

                //Assembling all ways into one big.
                //Assembling only one segment
                if (relation_way_index_end-relation_way_index_begin > 1) {
                    int st = relation_way_index_begin;
                    Way tmp = r.ways.get(st);
                    if (tmp.nodes.get(0) == r.ways.get(st + 1).nodes.get(0) || tmp.nodes.get(0) == r.ways.get(st + 1).nodes.get(r.ways.get(st + 1).nodes.size() - 1)) {
                        for (int i = r.ways.get(st).nodes.size() - 1; i >= 0; i--) {
                            way.nodes.add(tmp.nodes.get(i));
                        }
                    } else {
                        /*for (int j = 0; j < r.ways.get(relation_way_index_begin).nodes.size(); j++) {
                            way.nodes.add(r.ways.get(relation_way_index_begin).nodes.get(j));
                        }*/
                        way.nodes.addAll(r.ways.get(relation_way_index_begin).nodes);
                    }

                    Way temp0;
                    Way temp1;
                    for (int i = relation_way_index_begin + 1; i < relation_way_index_end; i++) {
                        temp0 = r.ways.get(i - 1);
                        temp1 = r.ways.get(i);
                        if (temp0.nodes.get(temp0.nodes.size() - 1) == temp1.nodes.get(0) || temp0.nodes.get(0) == temp1.nodes.get(0)) {
                            for (int j = 1; j < temp1.nodes.size(); j++) {
                                way.nodes.add(temp1.nodes.get(j));
                            }
                        } else {
                            for (int j = temp1.nodes.size() - 2; j >= 0; j--) {
                                way.nodes.add(temp1.nodes.get(j));
                            }
                        }
                    }
                }else{
                    way.nodes.addAll(r.ways.get(relation_way_index_begin).nodes);
                }
                // Mark stops with publicTransportStop=true
                RoadType roadType = RoadType.DEFAULT;
                String route_type = r.data.get("route");
                if (route_type.equals("bus") || route_type.equals("trolleybus")) {
                    roadType = route_type.equals("bus") ? RoadType.BUS: RoadType.TROLLEYBUS;
                    for (int i = relation_node_index_begin; i < relation_node_index_end; i++) {
                        Node trn = r.nodes.get(i);
                        int mdmpind = way.minDistToPoint(trn);
                        trn.publicTransportStop = true;
                        way.nodes.add(mdmpind + 1, trn);
                    }
                }else if(route_type.equals("subway") || route_type.equals("tram")){
                    if (route_type.equals("subway")) {
                        roadType = RoadType.SUBWAY;
                        HashSet<Long> ids = new HashSet<>();
                        for (SimpleNode sn: way.nodes){
                            if (sn instanceof Node) {
                                Node n = (Node) sn;
                                if (n.data != null && n.data.containsKey("station") && n.data.get("station").equals("subway")) {
                                    ids.add(n.id);
                                }
                            }
                        }
                        for (int i=relation_node_index_begin; i<relation_node_index_end; i++){
                            Node n = r.nodes.get(i);
                            n.publicTransportStop = true;
                            if (!ids.contains(n.id)){
                                int mdmpind = way.minDistToPoint(n);
                                way.nodes.add(mdmpind + 1, n);
                            }
                        }
                    }else{
                        roadType = RoadType.TRAM;
                        HashSet<Long> ids = new HashSet<>();
                        for (SimpleNode sn: way.nodes){
                            if (sn instanceof Node) {
                                Node n = (Node) sn;
                                if (n.data != null && n.data.containsKey("public_transport") && n.data.get("public_transport").equals("stop_position")) {
                                    ids.add(n.id);
                                }
                            }
                        }
                        for (int i=relation_node_index_begin; i<relation_node_index_end; i++){
                            Node n = r.nodes.get(i);
                            n.publicTransportStop = true;
                            if (!ids.contains(n.id)){
                                int mdmpind = way.minDistToPoint(n);
                                way.nodes.add(mdmpind + 1, n);
                            }
                        }
                    }
                }

                Node prev_stop = null;
                int start_ind = Integer.MAX_VALUE;
                float divider = 1;
                switch (roadType){
                    case SUBWAY:
                        divider = subway_speed;
                        break;
                    case BUS:
                        divider = bus_speed;
                        break;
                    case TROLLEYBUS:
                        divider = bus_speed;
                        break;
                    case TRAM:
                        divider = tram_speed;
                        break;
                }

                SimpleNode prev_simple_node = null;

                for (int i=0; i < way.nodes.size(); i++){
                    if (way.nodes.get(i) instanceof Node){
                        if (((Node)way.nodes.get(i)).publicTransportStop){
                            start_ind = i+1;
                            prev_simple_node = way.nodes.get(i);
                            prev_stop = (Node) way.nodes.get(i);
                            rgnBuilder.add(prev_stop);
                            rgnBuilder.connectNodeToNodesInRadius(prev_stop, 300, roadType);
                            break;
                        }
                    }
                }
                int dist = 0;
                for (int i=start_ind; i < way.nodes.size(); i++){
                    dist += Math.round(calculateDistance(new Node(prev_simple_node, this), new Node(way.nodes.get(i), this))/divider);
                    if (way.nodes.get(i) instanceof Node){
                        Node n = (Node) way.nodes.get(i);
                        if (n.publicTransportStop){
                            rgnBuilder.connect(n, prev_stop, roadType, dist);
                            rgnBuilder.connectNodeToNodesInRadius(n, 300, roadType);
                            prev_stop = n;
                            dist = 0;
                        }
                    }
                    prev_simple_node = way.nodes.get(i);
                }
                way.nodes.size();

            }
        }
    }

    public void load_prices(){
        HashMap<String, String> deduplicator = new HashMap<>();
        int counter = 0;
        try {
            System.out.println("Loading prices...");
            BufferedReader reader = new BufferedReader(new FileReader("data_json"));
            String line = reader.readLine();
            Field f = JsonObject.Member.class.getDeclaredField("name");
            f.setAccessible(true);
            while (line != null) {

                JsonObject jsonObject = Json.parse(line).asObject();
                JsonArray coords = jsonObject.get("coords").asArray();
                int[] t = mercator(coords.get(0).asDouble(), coords.get(1).asDouble());
                int x = t[0];
                int y = t[1];
                MapPoint p = new MapPoint(x, y);
                Way way = findShapeByPoint(p);
                if (tree.root.inBounds(p) && way == null) {
                    /*counter++;
                    lost_price.add(p);*/
                    List<Way> wayList = findShapesByCircle(p, 100);
                    if (wayList.size() == 1) {
                        way = wayList.get(0);
                    } else {
                        counter++;
                    }
                }
                if (way != null) {
                    try {
                        if (way.apartments == null) {
                            way.apartments = new ArrayList<>();
                        }
                        double area = Double.valueOf(jsonObject.get("Общая площадь").asString().replace("м2", "").replace(',', '.'));
                        JsonValue floor = jsonObject.get("Этаж");
                        String[] floors_string;
                        if (floor != null) {
                            floors_string = jsonObject.get("Этаж").asString().split("/");
                            if (floors_string.length < 2) {
                                floors_string = new String[]{floors_string[0], "-1"};
                            }
                        } else {
                            floors_string = new String[]{"-1", "-1"};
                        }
                        long pricel = jsonObject.get("Цена").asLong();
                        int price;
                        if (pricel > Integer.MAX_VALUE) {
                            price = 1;
                        } else {
                            price = (int) pricel;
                        }

                        //jsonObject optimize
                        for (JsonObject.Member mem: jsonObject){
                            if (deduplicator.containsKey(mem.getName())){
                                f.set(mem, deduplicator.get(mem.getName()));
                            }else{
                                deduplicator.put(mem.getName(), mem.getName());
                            }
                        }
                        way.apartments.add(new Apartment(price, area, Integer.parseInt(floors_string[0]), Integer.parseInt(floors_string[1]), jsonObject));

                        /*if ((int) Math.round(price / area) < 10000000) {

                            max_price_per_metr = Math.max(max_price_per_metr, (int) Math.round(price / area));
                            min_price_per_metr = Math.min(min_price_per_metr, (int) Math.round(price / area));
                        }*/
                    } catch (Exception e) {
                        //System.err.println("LoadPrices Exception");
                        //e.printStackTrace();
                    }

                }
                line = reader.readLine();
                /*counter++;
                if (counter%1000 == 0){
                    System.out.println(counter);
                }*/
            }
        } catch (Exception e) {
            System.err.println("LoadPrices Exception");
            e.printStackTrace();
        }
    }

    public List<Way> findShapesByCircle(MapPoint center, int radius){
        return tree.findShapesByCircle(center, radius);
    }

    public List<Node> findNodesInCircle(MapPoint center, int radius){
        return tree.findNodesInCircle(center, radius);
    }

    public List<RoadGraphNode> findRoadGraphNodesInCircle(MapPoint center, int radius){
        return tree.findRoadGraphNodesInCircle(center, radius);
    }

    public Way findShapeByPoint(MapPoint p) throws Exception{
        return tree.findShapeByPoint(p);
    }

    public void fillTreeNode(QuadTreeNode n){
        tree.fillTreeNode(n);
    }

    public void fillTreeNodeWithRoadGraphNodes(QuadTreeNode n){
        tree.fillTreeNodeWithRoadGraphNodes(n);
    }

    public void fillTreeNodeWithRoadGraphLines(QuadTreeNode n){
        tree.fillTreeNodeWithRoadGraphLines(n);
    }

    /*public static int calculateDistance(SimpleNode n1, SimpleNode n2}){
        return calculateDistance(new Node(n1, this),)
        double lat1 = Math.toRadians(n1.lat);
        double lat2 = Math.toRadians(n2.lat);
        double dlat = Math.toRadians(n2.lat-n1.lat);
        double dlon = Math.toRadians(n2.lon-n1.lon);

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (int)Math.round(earthRadius*c);

    }*/

    public static int calculateDistance(Node n1, Node n2){
        double lat1 = Math.toRadians(n1.lat);
        double lat2 = Math.toRadians(n2.lat);
        double dlat = Math.toRadians(n2.lat-n1.lat);
        double dlon = Math.toRadians(n2.lon-n1.lon);

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (int)Math.round(earthRadius*c);

    }

    public void calcRoadGraph(long id, boolean foot, int max_dist, int ind) {
        HashSet<RoadType> exclude = foot ? RoadGraphNode.foot_exclude : RoadGraphNode.car_exclude;
        for (RoadGraphNode rgn: roadGraph.values()){
            rgn.dist[ind] = Integer.MAX_VALUE;
        }
        int mode = foot ? 0 : 1;
        MapPoint center = ways.get(id).getCenter();
        RoadGraphNode start = null;
        boolean found = false;
        for (int radius = 100; radius < 20000; radius += 100) {
            List<RoadGraphNode> nds = findRoadGraphNodesInCircle(center, radius);
            for (RoadGraphNode n : nds) {
                for (RoadType type : n.ref_types[mode]) {
                    if (!exclude.contains(type)) {
                        start = n;
                        found = true;
                        break;
                    }
                }

            }
            if (found) {
                break;
            }
        }
        start.dist[ind] = 0;
        RoadGraphNode[] src = new RoadGraphNode[roadGraph.size()];
        RoadGraphNode[] res = new RoadGraphNode[roadGraph.size()];
        src[0] = start;
        if (test) {
            widthRecCalculateDistance(src, res, max_dist, mode, ind);
        }else {
            recCalculateDistances(start, max_dist, mode, ind);
        }
        //System.out.println("Graph Calculated!");
        if (!found) {
            System.err.println("Doesnt found close road to building");
        }
    }

    public List<Object> getCloseObjects(long id, boolean foot, int max_dist, int ind){


        return null;
    }

    void widthRecCalculateDistance(RoadGraphNode[] src, RoadGraphNode[] dest, final int max_dist, final int mode, final int put){
        RoadGraphNode[] temp;
        RoadGraphNode rgn, to;
        int dist_to;
        int src_size = 1;
        int dest_ind = 0;
        while (src_size != 0) {
            for (int i = 0; i < src_size; i++) {
                rgn = src[i];
                for (int j = 0; j < rgn.ref_to[mode].length; j++) {
                    to = rgn.ref_to[mode][j];
                    dist_to = rgn.dist[put] + rgn.distancesTo[mode][j];
                    if (dist_to < to.dist[put]) {
                        /*big_cnt++;
                        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = (Graphics2D) image.getGraphics();
                        drawGraph(g, 5000);
                        int mult = (int) Math.pow(2, PropertyMap.default_zoom - 16);
                        double cf = 1.0 / mult;
                        g.setColor(new Color(0, 0, 240));
                        g.setStroke(new BasicStroke(50f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawLine(coef(rgn.n.x, cf), coef(rgn.n.y, cf), coef(to.n.x, cf), coef(to.n.y, cf));
                        try {
                            ImageIO.write(image, "png", new File(String.format("GraphCalcAnimationWidth/img%05d.png", cnt++)));
                            System.out.println("Draw " + (cnt - 1)); } catch (Exception e) {
                            e.printStackTrace(); }*/
                        to.dist[put] = dist_to;
                        if (dist_to <= max_dist) {
                            dest[dest_ind++] = to;
                        }
                    }
                }
            }
            src_size = dest_ind;
            dest_ind = 0;
            temp = src;
            src = dest;
            dest = temp;
        }
    }

    void drawGraph(Graphics2D g, int max_dist){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int z = 16;
        int zoom_level = 13;
        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int mode = 0;

        BasicStroke secondary_stroke = new BasicStroke(65f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke primary_stroke = new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke tertiary_stroke = new BasicStroke(60f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke service_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke residential_stroke = new BasicStroke(50f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke living_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke default_stroke = new BasicStroke(20f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke unknown_stroke = new BasicStroke(10f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        double cf = 1.0/mult;
        int offx = 0*mult*256;
        int offy = 0*mult*256;
        boolean dont_draw = false;
        int lines = 0;
        boolean[] visited = new boolean[roadGraph.size()];
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : roadGraph.values()){
            //if (treeNode.inBounds(rgn.n)){
            visited[rgn.index] = true;
            for (int i=0; i<rgn.ref_to[mode].length; i++){
                RoadGraphNode ref = rgn.ref_to[mode][i];
                if (!visited[ref.index]){
                    switch (rgn.ref_types[mode][i]){
                        case SECONDARY:
                            if (z > zoom_level) {
                                g.setStroke(secondary_stroke);
                            }else{
                                g.setStroke(new BasicStroke(120f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            }
                            break;
                        case RESIDENTIAL:
                            if (z > zoom_level) {
                                g.setStroke(residential_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case SERVICE:
                            if (z > zoom_level) {
                                g.setStroke(service_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case TERTIARY:
                            if (z > zoom_level) {
                                g.setStroke(tertiary_stroke);
                            }else{
                                g.setStroke(tertiary_stroke);
                                //dont_draw = true;
                            }
                            break;
                        case PRIMARY:
                            if (z > zoom_level) {
                                g.setStroke(primary_stroke);
                            }else{
                                g.setStroke(new BasicStroke(120f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            }
                            break;
                        case TRUNK:
                            g.setStroke(new BasicStroke(80f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            break;
                        case DEFAULT:
                            if (z > zoom_level) {
                                g.setStroke(default_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case LIVING_STREET:
                            if (z > zoom_level) {
                                g.setStroke(living_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case SUBWAY:
                            dont_draw = true;
                            break;
                        case TRAM:
                            dont_draw = true;
                            break;
                        case BUS:
                            dont_draw = true;
                            break;
                        case TROLLEYBUS:
                            dont_draw = true;
                            break;
                        case INVISIBLE:
                            dont_draw = true;
                            break;
                        default:
                            if (z > zoom_level) {
                                g.setStroke(unknown_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                    }
                    if (!dont_draw) {

                        g.setPaint(new GradientPaint(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), rgn.getNodeColor(max_dist, 0),
                                coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf), ref.getNodeColor(max_dist, 0)));
                        g.drawLine(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf));
                        //lines++;
                    }else{
                        dont_draw = false;
                    }
                }
            }

        }
    }

    public static int coef(int n, double cf){
        return (int)Math.round(cf*n);
    }
    int big_cnt = 0;
    int cnt = 0;
    void recCalculateDistances(RoadGraphNode rgn, final int max_dist, final int mode, final int put){
        for (int i=0; i<rgn.ref_to[mode].length; i++){
            RoadGraphNode to = rgn.ref_to[mode][i];

            int dist = rgn.distancesTo[mode][i];
            if (rgn.dist[put] + dist < to.dist[put]){
                if (big_cnt % 60 == 0) {
                    BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    drawGraph(g, 5000);

                    int mult = (int) Math.pow(2, PropertyMap.default_zoom - 16);
                    double cf = 1.0 / mult;
                    g.setColor(new Color(0, 0, 240));
                    g.setStroke(new BasicStroke(50f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(coef(rgn.n.x, cf), coef(rgn.n.y, cf), coef(to.n.x, cf), coef(to.n.y, cf));
                    try {
                        ImageIO.write(image, "png", new File(String.format("GraphCalcAnimation/img%05d.png", cnt++)));
                        System.out.println("Draw " + (cnt - 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                big_cnt++;
                to.dist[put] = rgn.dist[put] + dist;
                if (to.dist[put] <= max_dist) {
                    recCalculateDistances(to, max_dist, mode, put);
                }
            }
        }
    }




}