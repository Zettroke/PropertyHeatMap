package net.zettroke.PropertyHeatMapServer.map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;
import net.zettroke.PropertyHeatMapServer.utils.IntArrayList;
import net.zettroke.PropertyHeatMapServer.utils.StringPredictor;

import java.io.*;
import java.util.*;
import java.util.List;


/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    static final int earthRadius = 6371000;
    public static PropertyMap propertyMap = null;

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

    public static final HashSet<RoadType> foot_exclude = new HashSet<>(Arrays.asList(RoadType.CONSTRUCTION, RoadType.PATH));
    public static final HashSet<RoadType> car_exclude = new HashSet<>(Arrays.asList(RoadType.SUBWAY, RoadType.BUS, RoadType.TRAM, RoadType.TROLLEYBUS,
            RoadType.SERVICE, RoadType.FOOTWAY, RoadType.CONSTRUCTION, RoadType.PATH));

    double minlat, minlon;
    double maxlat, maxlon;

    public int x_begin=0, y_begin=0;
    public int x_end=0, y_end=0;

    public StringPredictor predictor = new StringPredictor();
    public HashMap<String, Way> searchMap = new HashMap<>();

    //ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    HashMap<Long, Node> nodes = new HashMap<>();
    HashMap<Long, Way> ways = new HashMap<>();
    ArrayList<Relation> relations = new ArrayList<>();

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

    public PropertyMap() {
        propertyMap = this;
    }

    public void init(){
        tree = new QuadTree(new int[]{0, 0, x_end-x_begin, y_end-y_begin});
        //t.split();
        for (Node n: nodes.values()){
            tree.add(n);
        }
        System.out.println("Done with nodes!");
        for (Long i:ways.keySet()){
            if (ways.get(i).data.containsKey("building") || ways.get(i).data.containsKey("highway")){// || ways.get(i).data.containsKey("railway") ) {
                tree.add(new MapShape(ways.get(i)));
            }

        }
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
            for (Way w: ways.values()){
                if (w.data.containsKey("building") || w.data.containsKey("highway")){// || ways.get(i).data.containsKey("railway")) {
                    t.add(new MapShape(w));
                }

            }
        }
    }

    public void initParallel(){
        tree = new QuadTree(new int[]{0, 0, x_end-x_begin, y_end-y_begin});
        tree.root.split();
        ArrayList<ParallelInitThread> threads = new ArrayList<>();
        for (QuadTreeNode t: tree.root){
            ParallelInitThread p = new ParallelInitThread(t, this);
            //t.th_temp = p;
            threads.add(p);
            p.start();
        }
        for (ParallelInitThread pit: threads){
            try {
                pit.join();
            }catch (InterruptedException e){}
        }
        long start = System.nanoTime();
        load_prices();
        System.out.println("Loaded prices in " + (System.nanoTime()-start)/1000000.0 + " millis.");

        start = System.nanoTime();
        public_transport_init();
        System.out.println("Public transport init in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        System.gc();
    }

    void public_transport_init(){
        for (Relation rel: relations){
            if (rel.data.containsKey("route_master") && supportedRoutes.contains(rel.data.get("route_master"))){
                if (rel.id == 1472548){
                    System.out.println();
                }
                Relation r = null;// = rel.relations.get(0);// should be with max id
                for (int i=0; i<rel.relations.size(); i++){
                    if (rel.relations.get(i) != null){
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
                        MiddleMapPoint mdmp = way.minDistToPoint(r.nodes.get(i));

                        Node trn = r.nodes.get(i);
                        trn.publicTransportStop = true;
                        way.nodes.add(mdmp.ind + 1, trn);
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
                                MiddleMapPoint mdmp = way.minDistToPoint(n);
                                way.nodes.add(mdmp.ind + 1, n);
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
                                MiddleMapPoint mdmp = way.minDistToPoint(n);
                                way.nodes.add(mdmp.ind + 1, n);
                            }
                        }
                    }
                }

                RoadGraphNode prev_stop = null;
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
                ArrayList<Integer> rgn_to_connect_indexes = new ArrayList<>();

                SimpleNode prev_simple_node = null;
                for (int i=0; i < way.nodes.size(); i++){
                    if (way.nodes.get(i) instanceof Node){
                        if (((Node)way.nodes.get(i)).publicTransportStop){
                            prev_stop = new RoadGraphNode((Node)way.nodes.get(i));
                            int ind;
                            if (!roadGraphIndexes.containsKey(prev_stop.n.id)) {
                                prev_stop.types.add(roadType);
                                ind = roadGraphNodes.size();
                                roadGraphIndexes.put(prev_stop.n.id, ind);
                                roadGraphNodes.add(prev_stop);
                                roadGraphConnections.add(new IntArrayList());
                                roadGraphConnectionsTypes.add(new ArrayList<>());
                                roadGraphDistancesCar.add(new IntArrayList());
                                roadGraphDistancesFoot.add(new IntArrayList());
                            }else{
                                ind = roadGraphIndexes.get(prev_stop.n.id);
                                prev_stop = roadGraphNodes.get(ind);
                                prev_stop.types.add(roadType);

                            }
                            rgn_to_connect_indexes.add(ind);
                            /*List<RoadGraphNode> nodes = findRoadGraphNodesInCircle(prev_stop.n, 300); // 300 radius ~ 50m.
                            for (RoadGraphNode toConnect: nodes){
                                toConnect.types.add(roadType);
                                roadGraphConnections.get(ind).add(roadGraphIndexes.get(toConnect.n.id));
                                roadGraphConnections.get(roadGraphIndexes.get(toConnect.n.id)).add(ind);
                                roadGraphConnectionsTypes.get(ind).add(roadType);
                                roadGraphConnectionsTypes.get(roadGraphIndexes.get(toConnect.n.id)).add(roadType);
                                int dist = Math.round(calculateDistance(prev_stop.n, toConnect.n)/divider);
                                roadGraphDistancesFoot.get(ind).add(dist);
                                roadGraphDistancesFoot.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                                roadGraphDistancesCar.get(ind).add(dist);
                                roadGraphDistancesCar.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                            }*/
                            start_ind = i+1;
                            prev_simple_node = prev_stop.n;
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
                            int ind = roadGraphNodes.size();
                            int prev_ind = roadGraphIndexes.get(prev_stop.n.id);
                            RoadGraphNode rgn = new RoadGraphNode(n);
                            if (!roadGraphIndexes.containsKey(rgn.n.id)){
                                rgn.types.add(roadType);
                                roadGraphIndexes.put(rgn.n.id, ind);
                                roadGraphNodes.add(rgn);
                                roadGraphConnections.add(new IntArrayList());
                                roadGraphConnectionsTypes.add(new ArrayList<>());
                                roadGraphDistancesCar.add(new IntArrayList());
                                roadGraphDistancesFoot.add(new IntArrayList());
                            }else{
                                ind = roadGraphIndexes.get(rgn.n.id);
                                rgn = roadGraphNodes.get(ind);
                            }


                            roadGraphConnections.get(ind).add(prev_ind);
                            roadGraphConnections.get(prev_ind).add(ind);
                            roadGraphConnectionsTypes.get(ind).add(roadType);
                            roadGraphConnectionsTypes.get(prev_ind).add(roadType);
                            //int dist = Math.round(calculateDistance(rgn.n, prev_stop.n)/divider);
                            roadGraphDistancesFoot.get(ind).add(dist);
                            roadGraphDistancesFoot.get(prev_ind).add(dist);
                            roadGraphDistancesCar.get(ind).add(dist);
                            roadGraphDistancesCar.get(prev_ind).add(dist);


                            rgn_to_connect_indexes.add(ind);
                            /*List<RoadGraphNode> nodes = findRoadGraphNodesInCircle(rgn.n, 300); // 300 radius ~ 50m.
                            for (RoadGraphNode toConnect: nodes){
                                toConnect.types.add(roadType);
                                roadGraphConnections.get(ind).add(roadGraphIndexes.get(toConnect.n.id));
                                roadGraphConnections.get(roadGraphIndexes.get(toConnect.n.id)).add(ind);
                                roadGraphConnectionsTypes.get(ind).add(roadType);
                                roadGraphConnectionsTypes.get(roadGraphIndexes.get(toConnect.n.id)).add(roadType);
                                dist = Math.round(calculateDistance(rgn.n, toConnect.n)/divider);
                                roadGraphDistancesFoot.get(ind).add(dist);
                                roadGraphDistancesFoot.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                                roadGraphDistancesCar.get(ind).add(dist);
                                roadGraphDistancesCar.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                            }*/
                            prev_stop = rgn;
                            dist = 0;
                        }
                    }
                    prev_simple_node = way.nodes.get(i);
                }
                for (int ind: rgn_to_connect_indexes){
                    RoadGraphNode rgn = roadGraphNodes.get(ind);
                    List<RoadGraphNode> nodes = findRoadGraphNodesInCircle(roadGraphNodes.get(ind).n, 300);
                    for (RoadGraphNode toConnect: nodes){
                        toConnect.types.add(roadType);
                        roadGraphConnections.get(ind).add(roadGraphIndexes.get(toConnect.n.id));
                        roadGraphConnections.get(roadGraphIndexes.get(toConnect.n.id)).add(ind);
                        roadGraphConnectionsTypes.get(ind).add(roadType);
                        roadGraphConnectionsTypes.get(roadGraphIndexes.get(toConnect.n.id)).add(roadType);
                        dist = Math.round(calculateDistance(rgn.n, toConnect.n)/divider);
                        roadGraphDistancesFoot.get(ind).add(dist);
                        roadGraphDistancesFoot.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                        roadGraphDistancesCar.get(ind).add(dist);
                        roadGraphDistancesCar.get(roadGraphIndexes.get(toConnect.n.id)).add(dist);
                    }
                }
            }
        }
    }

    void load_prices(){
        int counter = 0;
        try {
            System.out.println("Loading prices...");
            BufferedReader reader = new BufferedReader(new FileReader("data_json"));
            String line = reader.readLine();
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

                        way.apartments.add(new Apartment(price, area, Integer.parseInt(floors_string[0]), Integer.parseInt(floors_string[1])));

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
        // TODO: Избавится от оторванных кусков графа.
        // TODO: Доделать пешеходный режим.
        //long start_t = System.nanoTime();
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
            if (!(rgn.types.size() + rgn.road_types.size() == 1 && (rgn.types.iterator().hasNext() && exclude.contains(rgn.types.iterator().next())))){// || rgn.types.contains(RoadType.LIVING_STREET)))) {
                RoadGraphNode clone = rgn.clone();
                clone.index = cnt++;
                res.put(rgn.n.id, clone);
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
                curr_node.distances = distances.toArray(new Integer[distances.size()]);
                curr_node.ref_to = ref_to.toArray(new RoadGraphNode[ref_to.size()]);
                distances.clear();
                ref_to.clear();
            }
        }

        boolean found = false;
        MapPoint center = ways.get(id).getCenter();
        Node start = new Node();
        start.x = Integer.MAX_VALUE; start.y = Integer.MAX_VALUE;

        for (int radius=100; radius<20000; radius+=100){
            List<Node> nds = findNodesInCircle(center, radius);
            for (Node n: nds){
                if (n.isRoadNode){
                    if (res.keySet().contains(n.id)){
                        start = n;
                        found = true;
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
    }

    void recCalculateDistances(RoadGraphNode rgn, final int max_dist){
        for (int i=0; i<rgn.ref_to.length; i++){
            RoadGraphNode to = rgn.ref_to[i];
            int dist = rgn.distances[i];
            if (rgn.dist + dist < to.dist){
                to.dist = rgn.dist + dist;
                if (to.dist <= max_dist) {
                    recCalculateDistances(to, max_dist);
                }
            }
        }
    }

    // 10 times faster *-*
    void widthRecCalculateDistance(RoadGraphNode[] src, RoadGraphNode[] dest, final int max_dist){
        int max_consuption = 0;
        RoadGraphNode[] temp;
        RoadGraphNode rgn, to;
        int dist_to;
        int src_size = 1;
        int dest_ind = 0;
        while (src_size != 0) {
            for (int i = 0; i < src_size; i++) {
                rgn = src[i];
                for (int j = 0; j < rgn.ref_to.length; j++) {
                    to = rgn.ref_to[j];
                    dist_to = rgn.dist + rgn.distances[j];
                    if (dist_to < to.dist) {
                        to.dist = dist_to;
                        if (dist_to <= max_dist) {
                            dest[dest_ind++] = to;
                        }
                    }
                }
            }
            src_size = dest_ind;
            if (src_size > max_consuption){
                max_consuption = src_size;
            }
            dest_ind = 0;
            temp = src;
            src = dest;
            dest = temp;
        }
        System.out.println("Max array usage - " + max_consuption);
    }

}