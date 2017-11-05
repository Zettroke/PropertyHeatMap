package net.zettroke.PropertyHeatMapServer;


import com.sun.istack.internal.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Zettroke on 19.10.2017.
 */
public class QuadTree {
    static int THRESHOLD = 100;

    @Nullable
    static MapPoint HorzCross(int horz, int x1, int x2, MapPoint p1, MapPoint p2){
        if ((p1.y > horz && p2.y > horz) || (p1.y < horz && p2.y < horz)){
            return null;
        }else{
            int x = (int)Math.round(p1.x + ((horz-p1.y)/(double)(p2.y-p1.y))*(p2.x-p1.x));
            if (x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) {
                return new MapPoint(x, horz);
            }else {
                return null;
            }
        }
    }

    @Nullable
    static MapPoint VertCross(int vert, int y1, int y2, MapPoint p1, MapPoint p2){
        if ((p1.x > vert && p2.x > vert) || (p1.x < vert && p2.x < vert)){
            return null;
        }else{
            int y = (int)Math.round(p1.y + ((vert-p1.x)/(double)(p2.x-p1.x))*(p2.y-p1.y));
            if (y >= Math.min(y1, y2) && y <= Math.max(y1, y2)) {
                return new MapPoint(vert, y);
            }else {
                return null;
            }
        }
    }

    static class TreeNode implements Iterable<TreeNode>{

        static class SquareSidesComparator implements Comparator<MapPoint>{
            MapPoint begin;

            @Override
            public int compare(MapPoint p1, MapPoint p2) {
                return  (Math.abs(p1.x-begin.x)+Math.abs(p1.y-begin.y)) - (Math.abs(p2.x-begin.x)+Math.abs(p2.y-begin.y));

            }

            SquareSidesComparator(MapPoint begin){
                this.begin = begin;

            }
        }

        static class SquareComparator implements Comparator<MapPoint>{


            ArrayList<MapPoint> points;

            @Override
            public int compare(MapPoint point1, MapPoint point2) {
                if (point1.x == 100 && point1.y == 281 || point2.x == 100 && point2.y == 281){
                    System.out.println("dkasl;d");
                }
                int side1 = 0;
                int side2 = 0;

                for (int i=0; i<4; i++){
                    if (i % 2 == 1){
                        if (point1.x == points.get(i).x){
                            side1 = i;
                        }
                        if (point2.x == points.get(i).x){
                            side2 = i;
                        }
                    }else{
                        if (point1.y == points.get(i).y){
                            side1 = i;
                        }
                        if ( point2.y == points.get(i).y){
                            side2 = i;
                        }
                    }

                }


                int ind1 = points.indexOf(point1);
                if (ind1 != -1){
                    side1 = ind1;
                }

                int ind2 = points.indexOf(point2);
                if (ind2 != -1){
                    side2 = ind2;
                }


                if (side1 == side2){
                    return (Math.abs(point1.x-points.get(side1).x) + Math.abs(point1.y-points.get(side1).y)) - (Math.abs(point2.x-points.get(side1).x) + Math.abs(point2.y-points.get(side1).y));
                }else{
                    return side1 - side2;
                }

            }

            public SquareComparator(MapPoint p0, MapPoint p1, MapPoint p2, MapPoint p3) {
                points = new ArrayList<MapPoint>(Arrays.asList(p0, p1, p2, p3));
            }
        }

        /*
        -------------------
        |        |        |
        |   NW   |   NE   |
        |        |        |
        ---------|---------
        |        |        |
        |   SW   |   SE   |
        |        |        |
        -------------------
         */

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

        boolean inBounds(MapPoint p){
            return (p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3]);
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

        void add(Node n) {
            if (!this.isEndNode) {
                if (n.x <= (bounds[0] + bounds[2]) / 2) {
                    if (n.y <= (bounds[1] + bounds[3]) / 2) {
                        nw.add(n);
                    } else {
                        ne.add(n);
                    }
                } else {
                    if (n.y <= (bounds[1] + bounds[3]) / 2) {
                        sw.add(n);
                    } else {
                        se.add(n);
                    }
                }
            } else {
                this.nodes.add(n);
                this.items++;
                if (this.items > THRESHOLD) {
                    this.split();
                }
            }
        }

        private void addRoad(final MapShape m){
            MapShape shape = new MapShape();
            MapPoint p1 = m.points.get(0);
            boolean inTreeNode = inBounds(p1);
            if (inTreeNode){shape.points.add(p1);}

            for (int i=1; i < m.points.size(); i++){
                MapPoint p2 = m.points.get(i);
                MapPoint h1 = HorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
                MapPoint h2 = HorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
                MapPoint v1 = VertCross(bounds[0], bounds[1], bounds[3], p1, p2);
                MapPoint v2 = VertCross(bounds[2], bounds[1], bounds[3], p1, p2);
                int intersections = (h1 != null ? 1: 0) + (h2 != null ? 1: 0) + (v1 != null ? 1: 0) + (v2 != null ? 1: 0);
                if (inTreeNode && intersections == 0){
                    shape.points.add(p2);
                }else if (intersections == 1){
                    inTreeNode = !inTreeNode;
                    if (inTreeNode){
                        shape = new MapShape();
                    }else{
                        shapes.add(shape);
                    }
                    shape.points.add((h1 != null ? h1:(h2 != null? h2: (v1 != null ? v1: v2))));
                    if (inTreeNode) {
                        shape.points.add(p2);
                    }
                }else if (intersections == 2){
                    shape = new MapShape();
                    if (h1 != null){
                        shape.points.add(h1);
                    }
                    if (h2 != null){
                        shape.points.add(h2);
                    }
                    if (v1 != null){
                        shape.points.add(v1);
                    }
                    if (v2 != null){
                        shape.points.add(v2);
                    }
                    shapes.add(shape);
                }
                p1 = p2;
            }

        }

        private void addPoly(final MapShape m){
            ArrayList<MapPoint> s1 = new ArrayList<>();
            ArrayList<MapPoint> s2 = new ArrayList<>();
            ArrayList<MapPoint> s3 = new ArrayList<>();
            ArrayList<MapPoint> s4 = new ArrayList<>();
            ArrayList<MapPoint> shape = new ArrayList<>();
            MapPoint p1 = m.points.get(0);
            shape.add(p1);
            for (int i=1; i<m.points.size(); i++){
                MapPoint p2 = m.points.get(i);
                MapPoint h1 = HorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
                MapPoint h2 = HorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
                MapPoint v1 = VertCross(bounds[0], bounds[1], bounds[3], p1, p2);
                MapPoint v2 = VertCross(bounds[2], bounds[1], bounds[3], p1, p2);
                if (h1 != null){
                    s1.add(h1);
                }
                if (v2 != null){
                    s2.add(v2);
                }
                if (h2 != null){
                    s3.add(h2);
                }
                if (v1 != null){
                    s4.add(v1);
                }
                int intersections = (h1 != null ? 1: 0) + (h2 != null ? 1: 0) + (v1 != null ? 1: 0) + (v2 != null ? 1: 0);

                if (intersections == 1){
                    MapPoint pt = (h1 != null ? h1:(h2 != null? h2: (v1 != null ? v1: v2)));
                    shape.add(pt);
                }else if (intersections == 2){
                    MapPoint intersec1 = (h1 != null ? h1:(h2 != null? h2: (v1 != null ? v1: v2)));
                    MapPoint intersec2 = (v2 != null ? v2:(v1 != null? v1: (h2 != null ? h2: h1)));

                    if ((p1.x-intersec1.x)*(p1.x-intersec1.x) + (p1.y-intersec1.y)*(p1.y-intersec1.y)
                            < (p1.x-intersec2.x)*(p1.x-intersec2.x) + (p1.y-intersec2.y)*(p1.y-intersec2.y)){
                        shape.add(intersec1);
                        shape.add(intersec2);
                    }else{
                        shape.add(intersec2);
                        shape.add(intersec1);
                    }
                }
                shape.add(p2);
                p1 = p2;
            }

            s1.sort(new SquareSidesComparator(new MapPoint(bounds[0], bounds[1])));
            s2.sort(new SquareSidesComparator(new MapPoint(bounds[2], bounds[1])));
            s3.sort(new SquareSidesComparator(new MapPoint(bounds[2], bounds[3])));
            s4.sort(new SquareSidesComparator(new MapPoint(bounds[0], bounds[3])));

            ArrayList<MapPoint> square = new ArrayList<>();
            MapPoint p00 = new MapPoint(bounds[0], bounds[1]), p11=new MapPoint(bounds[2], bounds[1]), p22=new MapPoint(bounds[2], bounds[3]), p33=new MapPoint(bounds[0], bounds[3]);
            square.add(p00);
            square.addAll(s1);
            square.add(p11);
            square.addAll(s2);
            square.add(p22);
            square.addAll(s3);
            square.add(p33);
            square.addAll(s4);
            s1 = null; s2 = null; s3 = null; s4 = null;
            


            ArrayList<Integer> shapeCrossPointsInd = new ArrayList<>();
            ArrayList<Integer> squareCrossPointsInd = new ArrayList<>(Arrays.asList(new Integer[square.size()]));
            Collections.fill(squareCrossPointsInd, -1);
            ArrayList<Boolean> shapeAvail = new ArrayList<>();

            for (MapPoint p: shape){
                shapeAvail.add(inBounds(p));
            }
            for (int i=0; i<shape.size(); i++){
                int ind = square.indexOf(shape.get(i));
                shapeCrossPointsInd.add(ind);
                if (ind != -1){
                    squareCrossPointsInd.set(ind, i);
                }
            }

            // main loop of poly clipping

            MapShape currentShape = new MapShape();
            int index = _findFineStartIndex(shape, shapeAvail)+1;
            MapPoint start = shape.get(index-1);
            boolean onShape = true;
            MapPoint p;
            while (true){

                if (onShape) {
                    p = shape.get(index);
                    shapeAvail.set(index, false);
                    if (shapeCrossPointsInd.get(index) != -1) {

                        index = (shapeCrossPointsInd.get(index) + 1) % square.size();

                        onShape = false;
                    }else{
                        index = (index+1)%shape.size();
                    }
                }else{
                    p = square.get(index);
                    if (squareCrossPointsInd.get(index) != -1){
                        int ind = squareCrossPointsInd.get(index);
                        shapeAvail.set(ind, false);
                        index = (ind + 1) % shape.size();

                        onShape = true;
                    }else {
                        index = (index+1)%square.size();
                    }
                }
                currentShape.points.add(p);
                if (p == start){
                    shapes.add(currentShape);
                    shapeAvail.set(shape.indexOf(start), false);
                    currentShape = new MapShape();
                    index = _findFineStartIndex(shape, shapeAvail);
                    if (index == -1){
                        break;
                    }
                    start = shape.get(index);
                    index = (index+1)%shape.size();

                    onShape = true;
                }
            }

        }

        private int _findFineStartIndex(final List<MapPoint> m, final ArrayList<Boolean> avail){
            for (int i=0; i<m.size()-1; i++){
                if (avail.get(i)){
                    if (inBounds(m.get(i+1))){
                        return i;
                    }
                }
            }
            return -1;
        }

        void add(final MapShape m){
            // Уххх, это надолго.....
            if (this.isEndNode){
                if (m.isPoly){
                    addPoly(m);
                }else {
                    addRoad(m);
                }
            }else{
                nw.add(m);
                ne.add(m);
                sw.add(m);
                se.add(m);
            }

        }

        public Iterator<TreeNode> iterator(){
            return new Iterator<TreeNode>() {
                byte ind = 0;
                @Override
                public boolean hasNext() {
                    return ind < 4;
                }

                @Override
                public TreeNode next() {
                    ind++;
                    switch (ind-1){
                        case 0:
                            return nw;
                        case 1:
                            return ne;
                        case 2:
                            return sw;
                        case 3:
                            return se;
                        default:
                            return null;
                    }
                }
            };
        }
    }

    TreeNode root;

    public QuadTree(int[] bounds){
        root = new TreeNode(bounds);
    }


}
