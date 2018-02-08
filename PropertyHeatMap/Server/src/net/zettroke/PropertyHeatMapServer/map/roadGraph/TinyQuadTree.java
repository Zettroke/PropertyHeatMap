package net.zettroke.PropertyHeatMapServer.map.roadGraph;

import net.zettroke.PropertyHeatMapServer.map.MapPoint;

import java.util.ArrayList;

public class TinyQuadTree {
    static final int THRESHOLD = 100;

    TinyQuadTreeNode root;

    TinyQuadTree(int[] bounds) {
        root = new TinyQuadTreeNode(null, bounds);
    }

    void add(RoadGraphNodeBuilder b){
        root.add(b);
    }

    TinyQuadTreeNode getEndNode(MapPoint p){
        TinyQuadTreeNode curr = root;
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

    ArrayList<RoadGraphNodeBuilder> findRgnBuildersInCircle(MapPoint center, int radius){
        TinyQuadTreeNode treeNode = getEndNode(center);
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

        ArrayList<RoadGraphNodeBuilder> res = new ArrayList<>();

        rec_find_nodes_in_circle(treeNode, center, radius, res);

        return res;
    }

    private void rec_find_nodes_in_circle(TinyQuadTreeNode treeNode, MapPoint center, int radius, ArrayList<RoadGraphNodeBuilder> res){
        if (treeNode.isEndNode){
            for (RoadGraphNodeBuilder rgn: treeNode.data){
                if ((rgn.x-center.x)*(long)(rgn.x-center.x) + (rgn.y-center.y)*(long)(rgn.y-center.y) <= radius*radius){
                    if (!rgn.equals(center)) {
                        res.add(rgn);
                    }
                }
            }
        }else{
            rec_find_nodes_in_circle(treeNode.nw, center, radius, res);
            rec_find_nodes_in_circle(treeNode.ne, center, radius, res);
            rec_find_nodes_in_circle(treeNode.sw, center, radius, res);
            rec_find_nodes_in_circle(treeNode.se, center, radius, res);
        }
    }

    private class TinyQuadTreeNode {
        TinyQuadTreeNode parent;
        boolean isEndNode = true;
        TinyQuadTreeNode nw;
        TinyQuadTreeNode ne;
        TinyQuadTreeNode sw;
        TinyQuadTreeNode se;
        int[] bounds;
        ArrayList<RoadGraphNodeBuilder> data = new ArrayList<>();

        TinyQuadTreeNode(TinyQuadTreeNode p, int[] bounds) {
            parent = p;
            this.bounds = bounds;
        }

        void split() {
            int hx = (bounds[0] + bounds[2]) / 2;
            int hy = (bounds[1] + bounds[3]) / 2;
            nw = new TinyQuadTreeNode(this, new int[]{bounds[0], bounds[1], hx, hy});
            ne = new TinyQuadTreeNode(this, new int[]{hx, bounds[1], bounds[2], hy});
            sw = new TinyQuadTreeNode(this, new int[]{bounds[0], hy, hx, bounds[3]});
            se = new TinyQuadTreeNode(this, new int[]{hx, hy, bounds[2], bounds[3]});
            isEndNode = false;
            for (RoadGraphNodeBuilder b : data) {
                nw.add(b);
                ne.add(b);
                sw.add(b);
                se.add(b);
            }
            data = null;
        }

        boolean inBounds(RoadGraphNodeBuilder b) {
            return b.x >= bounds[0] && b.x <= bounds[2] && b.y >= bounds[1] && b.y <= bounds[3];
        }

        void add(RoadGraphNodeBuilder b) {
            if (inBounds(b)) {
                if (isEndNode) {
                    data.add(b);
                    if (data.size() > TinyQuadTree.THRESHOLD) {
                        split();
                    }
                } else {
                    nw.add(b);
                    ne.add(b);
                    sw.add(b);
                    se.add(b);
                }
            }
        }
    }
}
