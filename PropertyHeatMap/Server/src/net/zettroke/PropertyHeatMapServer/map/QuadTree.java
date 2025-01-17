package net.zettroke.PropertyHeatMapServer.map;


import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphLine;
import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphNode;

import java.util.*;

import static net.zettroke.PropertyHeatMapServer.map.QuadTreeNode.DHorzCross;
import static net.zettroke.PropertyHeatMapServer.map.QuadTreeNode.DVertCross;

/**
 * Created by Zettroke on 19.10.2017.
 * CRSJ419R60Z
 */
public class QuadTree {
    static int THRESHOLD = 3000;
    static int NODE_THRESHOLD = 50000;
    static int THRESHOLD_SHAPE = 800;


    public QuadTreeNode root;

    void add(RoadGraphNode rgn){
        if (root.inBounds(rgn.n)){
            root.add(rgn);
        }
    }

    void add(Node n){
        if (root.inBounds(n)) {
            root.add(n);
        }
    }

    void add(MapShape m){
        root.add(m);
    }

    void add(RoadGraphLine line){
        root.add(line);
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

    Way findShapeByPoint(MapPoint p) {

        QuadTreeNode treeNode = getEndNode(p);
        int count = 0;
        Way res = null;
        if (!root.inBounds(p)){
            return null;
        }
        for (MapShape mh: treeNode.shapes){
            /*if (mh.way.id == 198560029){
                Way suka = new Way();
                suka.id = mh.way.id;
                for(MapPoint psuka: mh.way.nodes){
                    suka.nodes.add(new SimpleNode(psuka.x, psuka.y));
                }

                new ObjectOutputStream(new FileOutputStream("fuck.obj")).writeObject(suka);
            }*/
            if (mh.isPoly && mh.contain(p)){
                count++;
                res = mh.way;
            }
        }
        return res;

    }

    List<Way> findShapesByCircle(MapPoint center, int radius){
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
            rec_find_treenode_crossed_by_circle(treeNode, nodeToSearch, center, radius);
        }

        HashMap<Long, Way> result = new HashMap<>();
        //TODO: filter result to buildings and other stuff
        for (QuadTreeNode node: nodeToSearch){
            if (new MapPoint(node.bounds[0], node.bounds[1]).inCircle(center, radius) && new MapPoint(node.bounds[2], node.bounds[1]).inCircle(center, radius) &&
                    new MapPoint(node.bounds[2], node.bounds[3]).inCircle(center, radius) && new MapPoint(node.bounds[0], node.bounds[3]).inCircle(center, radius)){
                for (MapShape shape: node.shapes){
                    result.put(shape.way.id, shape.way);
                }
            }else{
                for (MapShape shape: node.shapes){
                    if (circle_contain_shape_or_cross(shape, center, radius)) {
                        result.put(shape.way.id, shape.way);
                    }
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    List<Node> findNodesInCircle(MapPoint center, int radius){
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
        
        ArrayList<Node> res = new ArrayList<>();
        
        rec_circle_nodes_search(treeNode, res, center, radius);

        return res;
        
    }

    List<RoadGraphNode> findRoadGraphNodesInCircle(MapPoint center, int radius){
        QuadTreeNode treeNode = getEndNode(center);
        while (true){
            if (Math.abs(center.x - treeNode.bounds[0]) > radius && Math.abs(treeNode.bounds[2] - center.x) > radius &&
                    Math.abs(center.y - treeNode.bounds[1]) > radius && Math.abs(treeNode.bounds[3] - center.y) > radius){
                break;
            }else {
                if (treeNode.parent != null){
                    treeNode = treeNode.parent;
                }else{
                    break;
                }
            }
        }

        ArrayList<RoadGraphNode> res = new ArrayList<>();

        rec_circle_rgn_nodes_search(treeNode, res, center, radius);

        return res;
    }
    
    private void rec_circle_nodes_search(QuadTreeNode treeNode, ArrayList<Node> res, MapPoint center, int radius){
        if (treeNode.isEndNode){
            for (Node n: treeNode.nodes){
                if ((n.x-center.x)*(n.x-center.x) + (n.y-center.y)*(n.y-center.y) <= radius*radius){
                    res.add(n);
                }
            }
        }else{
            for (QuadTreeNode tr1: treeNode) {
                rec_circle_nodes_search(tr1, res, center, radius);
            }
        }
    }

    private void rec_circle_rgn_nodes_search(QuadTreeNode treeNode, ArrayList<RoadGraphNode> res, MapPoint center, int radius){
        if (treeNode.isEndNode){
            for (RoadGraphNode rgn: treeNode.roadGraphNodes){
                if ((rgn.n.x-center.x)*(long)(rgn.n.x-center.x) + (rgn.n.y-center.y)*(long)(rgn.n.y-center.y) <= radius*radius){
                    res.add(rgn);
                }
            }
        }else{
            for (QuadTreeNode tr1: treeNode) {
                if (tr1.inBounds(center) || Math.abs(tr1.bounds[0]-center.x) < radius || Math.abs(tr1.bounds[2]-center.x) < radius || Math.abs(tr1.bounds[1]-center.y) < radius
                        || Math.abs(tr1.bounds[3]-center.y) < radius)
                rec_circle_rgn_nodes_search(tr1, res, center, radius);
            }
        }
    }

    private boolean circle_contain_treeNode_or_cross(QuadTreeNode node, MapPoint center, int radius){
        MapPoint p1 = new MapPoint(node.bounds[0], node.bounds[1]);
        MapPoint p2 = new MapPoint(node.bounds[2], node.bounds[1]);
        MapPoint p3 = new MapPoint(node.bounds[2], node.bounds[3]);
        MapPoint p4 = new MapPoint(node.bounds[0], node.bounds[3]);
        if (p1.inCircle(center, radius) || p2.inCircle(center, radius) ||
                p3.inCircle(center, radius) || p4.inCircle(center, radius)){

            return true;

        }else{
            return Math.abs(center.x - node.bounds[0]) < radius || Math.abs(center.x - node.bounds[2]) < radius ||
                    Math.abs(center.y - node.bounds[1]) < radius || Math.abs(center.y - node.bounds[3]) < radius;
        }

    }

    private void rec_find_treenode_crossed_by_circle(QuadTreeNode node, ArrayList<QuadTreeNode> list, MapPoint p, int radius){
        if (node.isEndNode){
            if (circle_contain_treeNode_or_cross(node, p, radius)){
                list.add(node);
            }
        }else{
            for (QuadTreeNode treeNode: node) {
                rec_find_treenode_crossed_by_circle(treeNode, list, p, radius);
            }
        }

    }

    private boolean circle_contain_shape_or_cross(MapShape shape, MapPoint center, int radius){

        if (shape.way.minDistToBounds(center) > radius){
            return false;
        }

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
                        if ((p1.y - center.y) * (long)(p1.y - center.y) <= radius * radius) {
                            return true;
                        }
                    }
                }else if (B == 0){
                    if (center.x < Math.max(p1.x, p2.x) && center.x > Math.min(p1.x, p2.x)) {
                        if ((p1.x - center.x) * (long)(p1.x - center.x) <= radius * radius) {
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
                        if ((center.x-x)*(long)(center.x-x)+(center.y-y)*(long)(center.y-y) <= radius*(long)radius){
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
                if (res.intersec_with_tree_node(node)) {
                    rec_add_from_nodes_to_node(res, node, alreadyAdd);
                }
            }
        }else{
            for (MapShape shape: source.shapes){
                if (!alreadyAdd.contains(shape.way.id) && shape.way.apartments != null && shape.way.apartments.size() != 0) {
                    res.shapes.add(new MapShape(shape.way));
                    alreadyAdd.add(shape.way.id);
                }
            }
        }
    }

    public void fillTreeNodeWithRoadGraphNodes(QuadTreeNode n){
        MapPoint p0 = new MapPoint(n.bounds[0], n.bounds[1]), p1 = new MapPoint(n.bounds[2], n.bounds[1]), p2 = new MapPoint(n.bounds[2], n.bounds[3]), p3 = new MapPoint(n.bounds[0], n.bounds[3]);
        QuadTreeNode treeNode = getEndNode(new MapPoint(n.bounds[0], n.bounds[1]));
        while (treeNode.parent != null){
            if (treeNode.inBounds(p0)&&treeNode.inBounds(p1)&&treeNode.inBounds(p2)&&treeNode.inBounds(p3)){
                break;
            }else{
                treeNode = treeNode.parent;
            }
        }
        rec_add_rgn_from_nodes_to_node(n, treeNode);
    }

    private void rec_add_rgn_from_nodes_to_node(QuadTreeNode res, QuadTreeNode source){
        if (!source.isEndNode){
            for (QuadTreeNode node: source){
                if (res.intersec_with_tree_node(node)) {
                    rec_add_rgn_from_nodes_to_node(res, node);
                }
            }
        }else{
            if (res.contain_tree_node(source)) {
                res.roadGraphNodes.addAll(source.roadGraphNodes);
            }else {
                for (RoadGraphNode rgn : source.roadGraphNodes) {
                    if (res.inBounds(rgn.n)) {
                        res.roadGraphNodes.add(rgn);
                    }
                }
            }

        }
    }

    public void fillTreeNodeWithRoadGraphLines(QuadTreeNode n){
        MapPoint p0 = new MapPoint(n.bounds[0], n.bounds[1]), p1 = new MapPoint(n.bounds[2], n.bounds[1]), p2 = new MapPoint(n.bounds[2], n.bounds[3]), p3 = new MapPoint(n.bounds[0], n.bounds[3]);
        QuadTreeNode treeNode = getEndNode(new MapPoint(n.bounds[0], n.bounds[1]));
        while (treeNode.parent != null){
            if (treeNode.inBounds(p0)&&treeNode.inBounds(p1)&&treeNode.inBounds(p2)&&treeNode.inBounds(p3)){
                break;
            }else{
                treeNode = treeNode.parent;
            }
        }
        rec_add_rgl_from_nodes_to_node(n, treeNode);
    }

    private void rec_add_rgl_from_nodes_to_node(QuadTreeNode dst, QuadTreeNode src){
        if (src.isEndNode){
            if (dst.contain_tree_node(src)){
                dst.roadGraphLines.addAll(src.roadGraphLines);
            }else{
                for (RoadGraphLine rgl: src.roadGraphLines){
                    if (dst.inBounds(rgl.n1.n) || dst.inBounds(rgl.n2.n)){
                        dst.add(rgl);
                    }else{
                        QuadTreeNode.DMapPoint p1 = new QuadTreeNode.DMapPoint(rgl.n1.n);
                        QuadTreeNode.DMapPoint p2 = new QuadTreeNode.DMapPoint(rgl.n2.n);
                        QuadTreeNode.DMapPoint h1 = DHorzCross(dst.bounds[1], dst.bounds[0], dst.bounds[2], p1, p2);
                        if (h1 != null){
                            dst.roadGraphLines.add(rgl);
                            continue;
                        }
                        QuadTreeNode.DMapPoint h2 = DHorzCross(dst.bounds[3], dst.bounds[0], dst.bounds[2], p1, p2);
                        if (h2 != null){
                            dst.roadGraphLines.add(rgl);
                            continue;
                        }
                        QuadTreeNode.DMapPoint v1 = DVertCross(dst.bounds[0], dst.bounds[1], dst.bounds[3], p1, p2);
                        if (v1 != null){
                            dst.roadGraphLines.add(rgl);
                            continue;
                        }
                        QuadTreeNode.DMapPoint v2 = DVertCross(dst.bounds[2], dst.bounds[1], dst.bounds[3], p1, p2);
                        if (v2 != null){
                            dst.roadGraphLines.add(rgl);
                            continue;
                        }
                    }
                }
            }
        }else{
            for (QuadTreeNode t: src){
                if (dst.intersec_with_tree_node(t)) {
                    rec_add_rgl_from_nodes_to_node(dst, t);
                }
            }
        }
    }

}
