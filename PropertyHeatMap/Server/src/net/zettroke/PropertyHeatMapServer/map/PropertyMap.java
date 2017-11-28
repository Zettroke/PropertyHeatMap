package net.zettroke.PropertyHeatMapServer.map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    public static int default_zoom = 19;
    public static int MAP_RESOLUTION = (int)Math.pow(2, default_zoom)*256; //(2**19)*256
    public int max_price_per_metr = Integer.MIN_VALUE;
    public int min_price_per_metr = Integer.MAX_VALUE;
    double minlat, minlon;
    double maxlat, maxlon;

    public int x_begin=0, y_begin=0;
    public int x_end=0, y_end=0;

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();

    public ArrayList<MapPoint> lost_price = new ArrayList<>();

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
        for (Node n: nodes){
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

        int count_calls_addPoly = 0;
        int count_calls_contain = 0;

        QuadTreeNode t;
        PropertyMap m;

        ParallelInitThread(QuadTreeNode t, PropertyMap m) {
            super();
            this.t = t;
            this.m = m;
        }

        @Override
        public void run() {
            for (Node n: m.nodes){
                if (t.inBounds(n)){
                    t.add(n);
                }
            }
            //System.out.println(getName() + " Done with nodes!");
            for (int i=0; i<m.ways.size(); i++){
                if (ways.get(i).data.containsKey("building") || ways.get(i).data.containsKey("highway")){// || ways.get(i).data.containsKey("railway")) {
                    t.add(new MapShape(ways.get(i)));
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
        load_prices();

        System.gc();

        //System.out.println("addPoly calls - " + count_addPoly);
        //System.out.println("contain calls - " + count_contain);
    }

    public void load_prices(){
        int counter = 0;
        try{
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
                if (tree.root.inBounds(p) && way == null){
                    /*counter++;
                    lost_price.add(p);*/
                    List<Way> wayList = findShapesByCircle(p, 100);
                    if (wayList.size() == 1){
                        way = wayList.get(0);
                    }else{
                        counter++;
                        lost_price.add(p);
                    }
                }
                if (way != null) {
                    if (way.apartments == null){
                        way.apartments = new ArrayList<>();
                    }
                    double area = Double.valueOf(jsonObject.get("Общая площадь").asString().replace("м2", "").replace(',', '.'));
                    String[] floors_string = jsonObject.get("Этаж").asString().split("/");
                    if (floors_string.length < 2){
                        floors_string = new String[]{floors_string[0], "-1"};
                    }
                    int price = jsonObject.get("Цена").asInt();

                    way.apartments.add(new Apartment(price, area, Integer.parseInt(floors_string[0]), Integer.parseInt(floors_string[1])));

                    if ((int)Math.round(price/area) < 1000000){

                        max_price_per_metr = Math.max(max_price_per_metr, (int)Math.round(price/area));
                        min_price_per_metr = Math.min(min_price_per_metr, (int)Math.round(price/area));
                    }

                }
                line = reader.readLine();
            }
            System.out.println("lost_prices: " + counter);
        }catch (Exception e){
            System.err.println("LoadPrices Exception");
            e.printStackTrace();
        }

    }

    public List<Way> findShapesByCircle(MapPoint center, int radius) throws Exception{
        return tree.findShapesByCircle(center, radius);
    }

    public Way findShapeByPoint(MapPoint p) throws Exception{
        return tree.findShapeByPoint(p);
    }

    public void fillTreeNode(QuadTreeNode n){
        tree.fillTreeNode(n);
    }


}




