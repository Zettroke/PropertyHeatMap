package net.zettroke.PropertyHeatMapServer.map;

import java.util.ArrayList;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    public static int default_zoom = 19;
    public static int MAP_RESOLUTION = (int)Math.pow(2, default_zoom)*256; //(2**10)*256
    double minlat, minlon;
    double maxlat, maxlon;

    int x_begin=0, y_begin=0;
    int x_end=0, y_end=0;

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();

    QuadTree tree;

    int[] mercator(double lon, double lat){
        int x = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.toRadians(lon)+Math.PI))-x_begin);
        int y = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.PI-Math.log(Math.tan(Math.toRadians(lat)/2+Math.PI/4))))-y_begin);
        return new int[]{x, y};
    }

    public PropertyMap() {}
    void init(){
        tree = new QuadTree(0, 0, x_end-x_begin, y_end-y_begin);
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
        tree = new QuadTree(0, 0, x_end-x_begin, y_end-y_begin);
        tree.root.split();
        ArrayList<ParallelInitThread> threads = new ArrayList<>();
        for (QuadTreeNode t: tree.root){
            ParallelInitThread p = new ParallelInitThread(t, this);
            //t.th_temp = p;
            threads.add(p);
            p.start();
        }
        int count_addPoly = 0;
        int count_contain = 0;
        for (ParallelInitThread pit: threads){
            try {
                pit.join();
                count_addPoly += pit.count_calls_addPoly;
                count_contain += pit.count_calls_contain;
                //System.out.println(pit.getName() + " is done!");
            }catch (InterruptedException e){}
        }
        //System.out.println("addPoly calls - " + count_addPoly);
        //System.out.println("contain calls - " + count_contain);
    }

    public Way findShapeByPoint(MapPoint p) throws Exception{
        QuadTreeNode treeNode = tree.getEndNode(p);
        int count = 0;
        Way res = null;
        for (MapShape mh: treeNode.shapes){
            if (mh.isPoly && mh.contain(p)){
                count++;
                res = mh.way;
            }
        }
        return res;

    }

}




