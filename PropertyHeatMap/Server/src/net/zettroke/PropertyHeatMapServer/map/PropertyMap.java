package net.zettroke.PropertyHeatMapServer.map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;
import net.zettroke.PropertyHeatMapServer.utils.RoadType;

import java.io.*;
import java.util.*;
import java.util.List;


/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    static final int earthRadius = 6371000;

    public static int default_zoom = 19;
    public static int MAP_RESOLUTION = (int)Math.pow(2, default_zoom)*256; //(2**19)*256
    public int max_calculation_dist = 16000;

    
    


    public int max_price_per_metr = Integer.MIN_VALUE;
    public int min_price_per_metr = Integer.MAX_VALUE;
    double minlat, minlon;
    double maxlat, maxlon;

    public int x_begin=0, y_begin=0;
    public int x_end=0, y_end=0;

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    HashMap<Long, Node> nodes = new HashMap<>();
    HashMap<Long, Way> ways = new HashMap<>();
    ArrayList<Relation> relations = new ArrayList<>();
    ArrayList<Relation> public_transport = new ArrayList<>();


    HashMap<Long, Integer> roadGraphIndexes = new HashMap<>();
    ArrayList<RoadGraphNode> roadGraphNodes = new ArrayList<>();
    ArrayList<ArrayList<Integer>> roadGraphConnections = new ArrayList<>();
    ArrayList<ArrayList<Integer>> roadGraphDistances = new ArrayList<>();
    ArrayList<ArrayList<RoadType>> roadGraphConnectionsTypes = new ArrayList<>();


    public QuadTree tree;

    int[] mercator(double lon, double lat){
        int x = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.toRadians(lon)+Math.PI))-x_begin);
        int y = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.PI-Math.log(Math.tan(Math.toRadians(lat)/2+Math.PI/4))))-y_begin);
        return new int[]{x, y};
    }

    public PropertyMap() {}

    public void init(){
        tree = new QuadTree(new int[]{0, 0, x_end-x_begin, y_end-y_begin});
        //t.split();
        for (Node n: nodes.values()){
            tree.add(n);
        }
        System.out.println("Done with nodes!");
        for (int i=0; i<ways.size(); i++){
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

        public_transport_init();


        System.gc();

        //System.out.println("addPoly calls - " + count_addPoly);
        //System.out.println("contain calls - " + count_contain);
    }

    void public_transport_init(){

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

                        if ((int) Math.round(price / area) < 10000000) {

                            max_price_per_metr = Math.max(max_price_per_metr, (int) Math.round(price / area));
                            min_price_per_metr = Math.min(min_price_per_metr, (int) Math.round(price / area));
                        }
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

    public Way findShapeByPoint(MapPoint p) throws Exception{
        return tree.findShapeByPoint(p);
    }

    public void fillTreeNode(QuadTreeNode n){
        tree.fillTreeNode(n);
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

    public HashMap<Long, RoadGraphNode> getCalculatedRoadGraph(long id, HashSet<RoadType> exclude, int max_dist){
        //long start_t = System.nanoTime();
        this.max_calculation_dist = max_dist;
        HashMap<Long, RoadGraphNode> res = new HashMap<>();
        for (RoadGraphNode rgn: roadGraphNodes){
            if (!(rgn.types.size() + rgn.road_types.size() == 1 && (rgn.types.iterator().hasNext() && exclude.contains(rgn.types.iterator().next())))){// || rgn.types.contains(RoadType.LIVING_STREET)))) {
                res.put(rgn.n.id, rgn.clone());
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
                        distances.add(roadGraphDistances.get(i).get(j));
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
        recCalculateDistances(start_rgn);


        return res;
    }

    void recCalculateDistances(RoadGraphNode rgn){
        for (int i=0; i<rgn.ref_to.length; i++){
            RoadGraphNode to = rgn.ref_to[i];
            int dist = rgn.distances[i];
            if (rgn.dist + dist < to.dist){
                to.dist = rgn.dist + dist;
                if (to.dist <= max_calculation_dist) {
                    recCalculateDistances(to);
                }
            }
        }
    }

}




