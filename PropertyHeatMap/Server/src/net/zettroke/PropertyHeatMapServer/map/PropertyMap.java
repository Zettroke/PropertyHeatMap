package net.zettroke.PropertyHeatMapServer.map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphBuilder;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNodeBuilder;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;
import net.zettroke.PropertyHeatMapServer.utils.IntArrayList;
import net.zettroke.PropertyHeatMapServer.utils.StringPredictor;

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
    public static int MAP_RESOLUTION = (int)Math.pow(2, default_zoom)*256; //(2**19)*256
    private final static HashSet<String> supportedRoutes = new HashSet<>(Arrays.asList("subway", "tram", "trolleybus", "bus"));
    public int max_calculation_dist = 16000;

    // Speed in meters per 0.1 seconds.
    public static final float car_speed = 1.38888f;
    public static final float foot_speed = 0.138888f;
    public static final float subway_speed = 1.25f;
    public static final float bus_speed = 0.8333f;
    public static final float tram_speed = 0.55555f;

    MapLoader loader;

    double minlat, minlon;
    double maxlat, maxlon;

    public int x_begin=0, y_begin=0;
    public int x_end=0, y_end=0;

    public StringPredictor predictor = new StringPredictor();
    public HashMap<String, Way> searchMap = new HashMap<>();

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    HashMap<Long, Node> nodes = new HashMap<>();
    HashMap<Long, Way> ways = new HashMap<>();
    HashMap<Long, Relation> relations = new HashMap<>();

    RoadGraphBuilder rgnBuilder;
    public HashMap<Long, RoadGraphNode> roadGraph;
    HashMap<Long, RoadGraphNodeBuilder> rgnBuilders = new HashMap<>();
    HashMap<Long, Integer> roadGraphIndexes = new HashMap<>();
    ArrayList<RoadGraphNode> roadGraphNodes = new ArrayList<>();
    ArrayList<IntArrayList> roadGraphConnections = new ArrayList<>();
    ArrayList<ArrayList<RoadType>> roadGraphConnectionsTypes = new ArrayList<>();

    // Дистанция в 0.1 секундах.
    ArrayList<IntArrayList> roadGraphDistancesCar = new ArrayList<>();
    ArrayList<IntArrayList> roadGraphDistancesFoot = new ArrayList<>();

    public QuadTree tree;

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
            for (RoadGraphNode rgn: m.roadGraphNodes){
                if (t.inBounds(rgn.n)) {
                    t.add(rgn);
                }
            }
            //System.out.println(getName() + " Done with nodes!");
            //int cnt = 0;
            for (Way w: ways.values()){
                if (w.data.containsKey("building") || w.data.containsKey("highway")){// || ways.get(i).data.containsKey("railway")) {
                    t.add(new MapShape(w));
                }
            }
        }
    }

    public void init(){
        try {
            double[] degrees = loader.getDegreesBounds();

            minlon = degrees[0]; minlat = degrees[1]; maxlon = degrees[2]; maxlat = degrees[3];

            int[] coords = loader.getCoordBounds(this);

            x_begin = coords[0]; y_begin = coords[1]; x_end = coords[2]; y_end = coords[3];

            rgnBuilder = new RoadGraphBuilder(new int[]{0, 0, x_end - x_begin, y_end - y_begin});

            loader.load(rgnBuilder, this);

            nodes = loader.getNodes();
            ways = loader.getWays();
            simpleNodes = loader.getSimpleNodes();
            relations = loader.getRelations();

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
                } catch (InterruptedException e) {
                }
            }
            System.out.println("QuadTree created in " + (System.nanoTime() - start) / 1000000.0 + " millis.");

            start = System.nanoTime();
            load_prices();
            System.out.println("Loaded prices in " + (System.nanoTime() - start) / 1000000.0 + " millis.");

            start = System.nanoTime();
            public_transport_init();
            System.out.println("Public transport init in " + (System.nanoTime() - start) / 1000000.0 + " millis.");
            roadGraph = rgnBuilder.getRoadGraph();

        /*for (IntArrayList iar: roadGraphConnections){
            iar.shrink();
        }
        for (IntArrayList iar: roadGraphDistancesCar){
            iar.shrink();
        }
        for (IntArrayList iar: roadGraphDistancesFoot){
            iar.shrink();
        }
        System.gc();*/
        }catch (Exception e){
            System.err.println("PropertyMapInit Failed!!!");
            e.printStackTrace();
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
                Relation r = null;// = rel.relations.get(0);// should be with max id
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
                        int mdmpind = way.minDistToPoint(r.nodes.get(i));

                        Node trn = r.nodes.get(i);
                        trn.publicTransportStop = true;
                        way.nodes.add(mdmpind + 1, trn);
                    }
                }else if(route_type.equals("subway") || route_type.equals("tram")){
                    if (route_type.equals("subway")) {
                        roadType = RoadType.SUBWAY;
                        HashSet<Long> ids = new HashSet<>();
                        for (SimpleNode sn: way.nodes){
                            Node n = (Node) sn;
                            if (n.data != null && n.data.containsKey("station") && n.data.get("station").equals("subway")){
                                ids.add(n.id);
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
                            Node n = (Node) sn;
                            if (n.data != null && n.data.containsKey("public_transport") && n.data.get("public_transport").equals("stop_position")){
                                ids.add(n.id);
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
                    dist += Math.round(calculateDistance(prev_simple_node, way.nodes.get(i))/divider);
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


            }
        }
    }

    //TODO: optimize circle search. use bounds.
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
                        System.err.println("LoadPrices Exception");
                        e.printStackTrace();
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

    public static int calculateDistance(SimpleNode n1, SimpleNode n2){
        double lat1 = Math.toRadians(n1.lat);
        double lat2 = Math.toRadians(n2.lat);
        double dlat = Math.toRadians(n2.lat-n1.lat);
        double dlon = Math.toRadians(n2.lon-n1.lon);

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (int)Math.round(earthRadius*c);

    }

    public HashMap<Long, RoadGraphNode> getCalculatedRoadGraph(long id, boolean foot, final int max_dist){
        return  null;
    }
        /*
        // TODO: Избавится от оторванных кусков графа.
        long start_t = System.nanoTime();
        HashSet<RoadType> exclude;
        ArrayList<IntArrayList> rgn_distances;
        if (foot){
            exclude = foot_exclude;
            rgn_distances = roadGraphDistancesFoot;
        }else{
            exclude = car_exclude;
            rgn_distances = roadGraphDistancesCar;
        }
        this.max_calculation_dist = max_dist;
        HashMap<Long, RoadGraphNode> res = new HashMap<>();
        int cnt = 0;
        for (RoadGraphNode rgn: roadGraphNodes){
            for (RoadType t: rgn.types){
                if (!exclude.contains(t)){
                    RoadGraphNode clone = rgn.clone();
                    clone.index = cnt++;
                    res.put(rgn.n.id, clone);
                    break;
                }
            }
        }


        ArrayList<Integer> distances = new ArrayList<>(100);
        ArrayList<RoadGraphNode> ref_to = new ArrayList<>(100);
        for (int i=0; i<roadGraphNodes.size(); i++){
            RoadGraphNode curr_node = res.get(roadGraphNodes.get(i).n.id);
            if (curr_node != null) {
                for (int j = 0; j<roadGraphConnections.get(i).size(); j++){
                    if (!exclude.contains(roadGraphConnectionsTypes.get(i).get(j))){
                        curr_node.ref_types.add(roadGraphConnectionsTypes.get(i).get(j));
                        ref_to.add(res.get(roadGraphNodes.get(roadGraphConnections.get(i).get(j)).n.id));
                        distances.add(rgn_distances.get(i).get(j));
                    }
                }
                int[] distarr = new int[distances.size()];
                for (int i1=0; i1<distances.size(); i1++){
                    distarr[i1] = distances.get(i1);
                }
                curr_node.distances = distarr;
                curr_node.ref_to = ref_to.toArray(new RoadGraphNode[ref_to.size()]);
                distances.clear();
                ref_to.clear();
            }
        }

        TimeMeasurer.printMeasure(start_t,"Copied graph in %t millis.");

        boolean found = false;
        MapPoint center = ways.get(id).getCenter();
        Node start = new Node();
        start.x = Integer.MAX_VALUE; start.y = Integer.MAX_VALUE;

        for (int radius=100; radius<20000; radius+=100){
            List<Node> nds = findNodesInCircle(center, radius);
            for (Node n: nds){
                if (n.isRoadNode){
                    if (res.keySet().contains(n.id)){
                        for (RoadType type: res.get(n.id).types){
                            if (!exclude.contains(type)){
                                start = n;
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (found) {
                break;
            }
        }

        if (!found){
            System.err.println("Doesnt found close road to building");
        }

        RoadGraphNode start_rgn = res.get(start.id);
        start_rgn.dist = 0;
        //recCalculateDistances(start_rgn, max_dist);
        System.out.println("Initial arrays size - " + res.size());

        RoadGraphNode[] start_arr = new RoadGraphNode[res.size()];
        RoadGraphNode[] dest_arr = new RoadGraphNode[res.size()];
        start_arr[0] = start_rgn;
        widthRecCalculateDistance(start_arr, dest_arr, max_dist);

        return res;
    }*/

        void calcRoadGraph(long id, boolean foot){
            HashSet<RoadType> exclude = foot ? RoadGraphNode.foot_exclude: RoadGraphNode.car_exclude;
            int mode = foot ? 0: 1;
            MapPoint center = ways.get(id).getCenter();
            RoadGraphNode start = null;
            boolean found = false;
            for (int radius=100; radius<20000; radius+=100){
                List<RoadGraphNode> nds = findRoadGraphNodesInCircle(center, radius);
                for (RoadGraphNode n: nds){
                    for (RoadType type: n.ref_types[mode]){
                        if (!exclude.contains(type)){
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

            RoadGraphNode[] src = new RoadGraphNode[roadGraph.size()];
            RoadGraphNode[] res = new RoadGraphNode[roadGraph.size()];
            src[0] = start;
            widthRecCalculateDistance();
            if (!found){
                System.err.println("Doesnt found close road to building");
            }
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

    void recCalculateDistances(RoadGraphNode rgn, final int max_dist, final int mode, final int put){
        for (int i=0; i<rgn.ref_to[mode].length; i++){
            RoadGraphNode to = rgn.ref_to[mode][i];
            int dist = rgn.distancesTo[mode][i];
            if (rgn.dist[put] + dist < to.dist[put]){
                to.dist[put] = rgn.dist[put] + dist;
                if (to.dist[put] <= max_dist) {
                    recCalculateDistances(to, max_dist, mode, put);
                }
            }
        }
    }




}