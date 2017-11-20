package net.zettroke.PropertyHeatMapServer.map;


import java.util.*;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int THRESHOLD = 2000;

    //@Nullable


    void add(Node n){
        if (root.inBounds(n)) {
            root.add(n);
        }
    }

    void add(MapShape m){
        root.add(m);

    }

    QuadTreeNode root;

    public QuadTree(int... bounds){
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

}
