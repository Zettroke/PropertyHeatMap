package net.zettroke.PropertyHeatMapServer.map;


import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int THRESHOLD = 100;


    public QuadTreeNode root;

    void add(Node n){
        if (root.inBounds(n)) {
            root.add(n);
        }
    }

    void add(MapShape m){
        root.add(m);

    }



    public QuadTree(int[] bounds){
        root = new QuadTreeNode(bounds);
    }

    
    public QuadTreeNode getEndNode(MapPoint p){
        QuadTreeNode curr = root;
        while (true) {
            if (curr.isEndNode) {
                return curr;
            } else {
                if (p.x <= (curr.bounds[0] + curr.bounds[2]) / 2) {
                    if (p.y <= (curr.bounds[1] + curr.bounds[3]) / 2) {
                        curr = curr.nw;
                    } else {
                        curr = curr.sw;
                    }
                } else {
                    if (p.y <= (curr.bounds[1] + curr.bounds[3]) / 2) {
                        curr = curr.ne;
                    } else {
                        curr = curr.se;
                    }
                }
            }
        }
    }

    Way findShapeByPoint(MapPoint p) throws Exception{

        QuadTreeNode treeNode = getEndNode(p);
        int count = 0;
        Way res = null;
        for (MapShape mh: treeNode.shapes){
            if (mh.way.id == 29544240){
                Way suka = new Way();
                suka.id = mh.way.id;
                for(MapPoint psuka: mh.way.nodes){
                    suka.nodes.add(new SimpleNode(psuka.x, psuka.y));
                }

                new ObjectOutputStream(new FileOutputStream("fuck.obj")).writeObject(suka);
            }
            if (mh.isPoly && mh.contain(p)){
                count++;
                res = mh.way;
            }
        }
        return res;

    }

    Collection<Way> findShapesByCircle(MapPoint center, int radius) throws Exception{
        QuadTreeNode treeNode = getEndNode(center);
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

        HashMap<Long, Way> result = new HashMap<>();
        //TODO: filter result to buildings and other stuff
        for (QuadTreeNode node: nodeToSearch){
            if (new MapPoint(node.bounds[0], node.bounds[1]).inCircle(center, radius) && new MapPoint(node.bounds[2], node.bounds[1]).inCircle(center, radius) &&
                    new MapPoint(node.bounds[2], node.bounds[3]).inCircle(center, radius) && new MapPoint(node.bounds[0], node.bounds[3]).inCircle(center, radius)){
                for (MapShape shape: node.shapes){
                    if (shape.isPoly) {
                        result.put(shape.way.id, shape.way);
                    }
                }
            }else{
                for (MapShape shape: node.shapes){
                    if (shape.isPoly && circle_contain_shape_or_cross(shape, center, radius)) {
                        result.put(shape.way.id, shape.way);
                    }
                }
            }
        }

        return result.values();
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
        if (shape.contain(center)){
            return true;
        }
        MapPoint p1 = shape.points.get(0);
        if (p1.inCircle(center, radius)){
            return true;
        }

        MapPoint p2;
        for (int i=1; i<shape.points.size(); i++){
            p2 = shape.points.get(i);
            if (p2.inCircle(center, radius)){
                return true;
            }else {
                double A = p1.y-p2.y;
                double B = p2.x-p1.x;
                if (A == 0) {
                    if (center.y < Math.max(p1.y, p2.y) && center.y > Math.min(p1.y, p2.y)) {
                        if ((p1.y - center.y) * (p1.y - center.y) <= radius * radius) {
                            return true;
                        }
                    }
                }else if (B == 0){
                    if (center.x < Math.max(p1.x, p2.x) && center.x > Math.min(p1.x, p2.x)) {
                        if ((p1.x - center.x) * (p1.x - center.x) <= radius * radius) {
                            return true;
                        }
                    }
                }else{
                    double k1 = -A/B;
                    double k2 = B/A;
                    double b1 = -(p1.x*p2.y-p2.x*p1.y)/B;
                    double b2 = -B/A*center.x+center.y;
                    int x = (int)Math.round((b2-b1)/(k1-k2));
                    int y = (int)Math.round(k1*((b2-b1)/(k1-k2))+b1);

                    if (Math.max(p1.x, p2.x) > x && Math.min(p1.x, p2.x) < x && Math.max(p1.y, p2.y) > y && Math.min(p1.y, p2.y) < y) {
                        if ((center.x-x)*(center.x-x)+(center.y-y)*(center.y-y) <= radius*radius){
                            return true;
                        }
                    }
                }
            }


            p1 = p2;
        }


        return false;
    }

    public void fillTreeNode(QuadTreeNode n){
        MapPoint p0 = new MapPoint(n.bounds[0], n.bounds[1]), p1 = new MapPoint(n.bounds[2], n.bounds[1]), p2 = new MapPoint(n.bounds[2], n.bounds[3]), p3 = new MapPoint(n.bounds[0], n.bounds[3]);
        QuadTreeNode treeNode = getEndNode(new MapPoint(n.bounds[0], n.bounds[1]));
        while (treeNode.parent != null){
            if (treeNode.inBounds(p0)&&treeNode.inBounds(p1)&&treeNode.inBounds(p2)&&treeNode.inBounds(p3)){
                break;
            }else{
                treeNode = treeNode.parent;
            }
        }

        HashSet<Long> alreadyAdd = new HashSet<>();
        rec_add_from_nodes_to_node(n, treeNode, alreadyAdd);
    }

    private void rec_add_from_nodes_to_node(QuadTreeNode res, QuadTreeNode source, HashSet<Long> alreadyAdd){
        if (!source.isEndNode){
            for (QuadTreeNode node: source){
                rec_add_from_nodes_to_node(res, node, alreadyAdd);
            }
        }else{
            for (MapShape shape: source.shapes){
                if (!alreadyAdd.contains(shape.way.id)) {
                    res.add(new MapShape(shape.way));
                    alreadyAdd.add(shape.way.id);
                }
            }
        }
    }

}
