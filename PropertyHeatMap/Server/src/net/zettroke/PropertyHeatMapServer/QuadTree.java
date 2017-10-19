package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int split_threshold = 100;

    class TreeNode{
        boolean isEndNode = true;
        int[] bounds;
        TreeNode nw;
        TreeNode ne;
        TreeNode sw;
        TreeNode se;
        ArrayList<MapPolygon> polys = new ArrayList<>();
        ArrayList<Node> node = new ArrayList<>();
        ArrayList<Road> roads = new ArrayList<>();

        TreeNode(int[] bounds){
            this.bounds = bounds;
        }


    }


    TreeNode root;

    public QuadTree(int[] bounds){
        root = new TreeNode(bounds);
    }


}
