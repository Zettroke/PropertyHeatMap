package net.zettroke.PropertyHeatMapServer;


import java.util.ArrayList;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int THRESHOLD = 100;

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
            this.isEndNode = false;

            int hx = (bounds[0] + bounds[2])/2;
            int hy = (bounds[1] + bounds[3])/2;

            this.nw = new TreeNode(new int[]{bounds[0], bounds[1], hx, hy});
            this.ne = new TreeNode(new int[]{hx, bounds[1], bounds[2], hy});
            this.sw = new TreeNode(new int[]{bounds[0], hy, hx, bounds[3]});
            this.se = new TreeNode(new int[]{hx, hy, bounds[2], bounds[3]});

            for (Node n: nodes){
                if (n.x <= (bounds[0] + bounds[2])/2){
                    if (n.y <= (bounds[1] + bounds[3])/2){
                        nw.add(n);
                    }else{
                        ne.add(n);
                    }
                }else{
                    if (n.y <= (bounds[1] + bounds[3])/2){
                        sw.add(n);
                    }else{
                        se.add(n);
                    }
                }
            }
            nodes = null;





        }

        void add(Node n){
            if (!this.isEndNode){
                // compare and add recursive
                if (n.x <= (bounds[0] + bounds[2])/2){
                    if (n.y <= (bounds[1] + bounds[3])/2){
                        nw.add(n);
                    }else{
                        ne.add(n);
                    }
                }else{
                    if (n.y <= (bounds[1] + bounds[3])/2){
                        sw.add(n);
                    }else{
                        se.add(n);
                    }
                }
            }else{
                this.nodes.add(n);
                this.items++;
                if (this.items > THRESHOLD){
                    this.split();
                }
            }
        }

        void add(MapShape m){
            // Уххх, это надолго.....
        }

    }



    TreeNode root;



    public QuadTree(int[] bounds){
        root = new TreeNode(bounds);
    }


}
