package net.zettroke.PropertyHeatMapServer;


import com.sun.istack.internal.Nullable;

import java.util.ArrayList;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int THRESHOLD = 100;

    @Nullable
    static int[] HorzCross(int horz, int x1, int x2, MapPoint p1, MapPoint p2){
        if ((p1.y > horz && p2.y > horz) || (p1.y < horz && p2.y < horz)){
            return null;
        }else{
            int x = (int)Math.round(p1.x + ((horz-p1.y)/(double)(p2.y-p1.y))*(p2.x-p1.x));
            if (x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) {
                return new int[]{x, horz};
            }else {
                return null;
            }
        }
    }

    @Nullable
    static int[] VertCross(int vert, int y1, int y2, MapPoint p1, MapPoint p2){
        if ((p1.x > vert && p2.x > vert) || (p1.x < vert && p2.x < vert)){
            return null;
        }else{
            int y = (int)Math.round(p1.y + ((vert-p1.x)/(double)(p2.x-p1.x))*(p2.y-p1.y));
            if (y >= Math.min(y1, y2) && y <= Math.max(y1, y2)) {
                return new int[]{vert, y};
            }else {
                return null;
            }
        }
    }

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
            //------------------------------------------


        }

        void add(Node n){
            if (!this.isEndNode){
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
            if (this.isEndNode){
                shapes.add(m);
            }else{
                if (m.isPoly){

                }else{

                }
            }
        }

    }



    TreeNode root;




    public QuadTree(int[] bounds){
        root = new TreeNode(bounds);
    }


}
