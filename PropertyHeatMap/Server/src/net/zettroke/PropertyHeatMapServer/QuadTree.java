package net.zettroke.PropertyHeatMapServer;


import java.util.ArrayList;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int MAX_AMOUNT = 100;

    class TreeNode{
        int items = 0;
        boolean isEndNode = true;
        int[] bounds;
        TreeNode nw;
        TreeNode ne;
        TreeNode sw;
        TreeNode se;
        ArrayList<MapShape> shapes = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();

        TreeNode(int[] bounds){
            this.bounds = bounds;
        }

        void split(){
            int hx = (bounds[0] + bounds[2])/2;
            int hy = (bounds[1] + bounds[3])/2;
            nw = new TreeNode(new int[]{bounds[0], bounds[1], hx, hy});
            ne = new TreeNode(new int[]{hx, bounds[1], bounds[2], hy});
            sw = new TreeNode(new int[]{bounds[0], hy, hx, bounds[3]});
            se = new TreeNode(new int[]{hx, hy, bounds[2], bounds[3]});



        }
    }



    TreeNode root;

    public QuadTree(int[] bounds){
        root = new TreeNode(bounds);
    }


}
