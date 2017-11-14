package net.zettroke.PropertyHeatMapServer;

import javax.sound.midi.Soundbank;
import java.util.ArrayList;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    public static int MAP_RESOLUTION = 67108864*2; //(2**10)*256
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
        int x = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.toRadians(lat)+Math.PI))-x_begin);
        int y = (int)(Math.round(MAP_RESOLUTION/2/Math.PI*(Math.PI-Math.log(Math.tan(Math.toRadians(lon)/2+Math.PI/4))))-y_begin);
        return new int[]{x, y};
    }

    PropertyMap(){

    }

    void init(){
        tree = new QuadTree(0, 0, x_end-x_begin, y_end-y_begin);
        QuadTree.TreeNode t = tree.root;
        //t.split();
        for (Node n: nodes){
            if (t.inBounds(n)){
                t.add(n);
            }
        }
        System.out.println("Done with nodes!");
        for (int i=0; i<ways.size(); i++){
            if (ways.get(i).data.containsKey("building") || ways.get(i).data.containsKey("highway") || ways.get(i).data.containsKey("railway") ) {
                t.add(new MapShape(ways.get(i)));
            }

        }
    }

    class ParallelInitThread extends Thread{

        int count_calls_addPoly = 0;
        int count_calls_contain = 0;

        QuadTree.TreeNode t;
        PropertyMap m;

        ParallelInitThread(QuadTree.TreeNode t, PropertyMap m) {
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
            System.out.println(getName() + " Done with nodes!");
            for (int i=0; i<m.ways.size(); i++){
                if (ways.get(i).data.containsKey("building") || ways.get(i).data.containsKey("highway") || ways.get(i).data.containsKey("railway")) {
                    t.add(new MapShape(ways.get(i)));
                }

            }
        }
    }

    void initParallel(){
        tree = new QuadTree(0, 0, x_end-x_begin, y_end-y_begin);
        tree.root.split();
        ArrayList<ParallelInitThread> threads = new ArrayList<>();
        for (QuadTree.TreeNode t: tree.root){
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
                System.out.println(pit.getName() + " is done!");
            }catch (InterruptedException e){}
        }
        System.out.println("addPoly calls - " + count_addPoly);
        System.out.println("contain calls - " + count_contain);
    }

}




