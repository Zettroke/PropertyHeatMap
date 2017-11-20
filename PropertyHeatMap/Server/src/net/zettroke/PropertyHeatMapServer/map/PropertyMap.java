package net.zettroke.PropertyHeatMapServer.map;

import java.util.ArrayList;
import java.util.List;

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
    public void init(){
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
        for (ParallelInitThread pit: threads){
            try {
                pit.join();
            }catch (InterruptedException e){}
        }

        System.gc();

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

    public ArrayList<Way> findShapesByCircle(MapPoint center, int radius) throws Exception{
        QuadTreeNode treeNode = tree.getEndNode(center);
        while (true){
            if (center.x - treeNode.bounds[0] > radius && treeNode.bounds[2] - center.x > radius &&
                    center.y - treeNode.bounds[1] > radius && treeNode.bounds[3] - center.y > radius){
                break;
            }else {
                if (treeNode.parent != null){
                    treeNode = treeNode.parent;
                }else{
                    break;
                }
            }
        }

        ArrayList<QuadTreeNode> nodeToSearch = new ArrayList<>();
        if (treeNode.isEndNode){
            nodeToSearch.add(treeNode);
        }else {
            rec_circle_tree_node_search(treeNode, nodeToSearch, center, radius);
        }

        ArrayList<Way> result = new ArrayList<>();
        //TODO: filter result to buildings and other stuff
        for (QuadTreeNode node: nodeToSearch){
            if (new MapPoint(node.bounds[0], node.bounds[1]).inCircle(center, radius) || new MapPoint(node.bounds[2], node.bounds[1]).inCircle(center, radius) ||
                    new MapPoint(node.bounds[2], node.bounds[3]).inCircle(center, radius) || new MapPoint(node.bounds[0], node.bounds[3]).inCircle(center, radius)){
                for (MapShape shape: node.shapes){
                    result.add(shape.way);
                }
            }else{
                for (MapShape shape: node.shapes){
                    if (circle_contain_shape_or_cross(shape, center, radius)) {
                        result.add(shape.way);
                    }
                }
            }
        }

        return result;
    }

    private boolean circle_contain_node_or_cross(QuadTreeNode node, MapPoint center, int radius){
        MapPoint p1 = new MapPoint(node.bounds[0], node.bounds[1]);
        MapPoint p2 = new MapPoint(node.bounds[2], node.bounds[1]);
        MapPoint p3 = new MapPoint(node.bounds[2], node.bounds[3]);
        MapPoint p4 = new MapPoint(node.bounds[0], node.bounds[3]);
        if (p1.inCircle(center, radius) || p2.inCircle(center, radius) ||
                p3.inCircle(center, radius) || p4.inCircle(center, radius)){

            return true;

        }else{
            if (Math.abs(center.x-node.bounds[0]) < radius || Math.abs(center.x-node.bounds[2]) < radius ||
                    Math.abs(center.y-node.bounds[1]) < radius || Math.abs(center.y-node.bounds[3]) < radius){

                return true;

            }else{
                return false;
            }
        }

    }

    private void rec_circle_tree_node_search(QuadTreeNode node, ArrayList<QuadTreeNode> list, MapPoint p, int radius){
        if (node.isEndNode){
            if (circle_contain_node_or_cross(node, p, radius)){
                list.add(node);
            }
        }else{
            for (QuadTreeNode treeNode: node) {
                rec_circle_tree_node_search(treeNode, list, p, radius);
            }
        }

    }

    private boolean circle_contain_shape_or_cross(MapShape shape, MapPoint center, int radius){
        MapPoint p1 = shape.points.get(0);
        if (p1.inCircle(center, radius)){
            return true;
        }

        MapPoint p2;
        for (int i=1; i<shape.points.size(); i++){
            p2 = shape.points.get(i);
            if (p2.inCircle(center, radius)){
                return true;
            }


            p1 = p2;
        }


        return false;
    }

}




