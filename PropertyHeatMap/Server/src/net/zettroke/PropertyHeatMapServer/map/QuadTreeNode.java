package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphLine;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.IntArrayList;

import java.util.*;

public class QuadTreeNode implements Iterable<QuadTreeNode>{
    static class DMapPoint{
        MapPoint p;
        double x;
        double y;
        boolean intersec;

        DMapPoint(MapPoint p){
            this.p = p;
            x = p.x;
            y = p.y;
        }

        DMapPoint(double x, double y){
            this.x = x;
            this.y = y;
            p = new MapPoint((int)Math.round(x), (int)Math.round(y));
        }

        DMapPoint(int x, int y){
            this.x = x;
            this.y = y;
            p = new MapPoint(x, y);
        }

        boolean equals(DMapPoint p){
            return p.x==x && p.y == y;
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

    public int depth = 0;

    int items = 0;
    boolean divide = true;
    public boolean isEndNode = true;
    public int[] bounds;
    QuadTreeNode parent;
    QuadTreeNode nw;
    QuadTreeNode ne;
    QuadTreeNode sw;
    QuadTreeNode se;
    public ArrayList<MapShape> shapes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    public ArrayList<RoadGraphNode> roadGraphNodes = new ArrayList<>();
    public ArrayList<RoadGraphLine> roadGraphLines = new ArrayList<>();

    public QuadTreeNode(int[] bounds){
        this.bounds = bounds;
    }

    public QuadTreeNode(int[] bounds, boolean divide) {
        this.divide = divide;
        this.bounds = bounds;
    }

    boolean can_contain_shape(MapShape m) {
        if (inBounds(new MapPoint(m.way.bounds[0], m.way.bounds[1])) || inBounds(new MapPoint(m.way.bounds[0], m.way.bounds[3])) || inBounds(new MapPoint(m.way.bounds[2], m.way.bounds[1])) || inBounds(new MapPoint(m.way.bounds[2], m.way.bounds[3]))){
            return true;
        }else{
            QuadTreeNode temp = new QuadTreeNode(m.way.bounds);
            return temp.inBounds(new MapPoint(bounds[0], bounds[1])) || temp.inBounds(new MapPoint(bounds[0], bounds[3])) || temp.inBounds(new MapPoint(bounds[2], bounds[1])) || temp.inBounds(new MapPoint(bounds[2], bounds[3]));
        }
    }
    
    boolean contain_tree_node(QuadTreeNode t){
        return inBounds(new MapPoint(t.bounds[0], t.bounds[1])) && inBounds(new MapPoint(t.bounds[0], t.bounds[3])) && inBounds(new MapPoint(t.bounds[2], t.bounds[1])) && inBounds(new MapPoint(t.bounds[2], t.bounds[3]));
    }
    
    boolean intersec_with_tree_node(QuadTreeNode t){
        /*if (inBounds(new MapPoint(t.bounds[0], t.bounds[1])) || inBounds(new MapPoint(t.bounds[0], t.bounds[3])) || inBounds(new MapPoint(t.bounds[2], t.bounds[1]))
                || inBounds(new MapPoint(t.bounds[2], t.bounds[3])) || t.contain_tree_node(this)){

            return true;

        }else{

        }*/
        return !((t.bounds[0] < bounds[0] && t.bounds[2] < bounds[0]) || (t.bounds[0] > bounds[2] && t.bounds[2] > bounds[2]) ||
                 (t.bounds[1] < bounds[1] && t.bounds[3] < bounds[1]) || (t.bounds[1] > bounds[3] && t.bounds[3] > bounds[3]));


    }

    public boolean inBounds(MapPoint p){
        return (p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3]);
    }

    public boolean onBounds(MapPoint p){
        return p.x == bounds[0] || p.x == bounds[2] || p.y == bounds[1] || p.y == bounds[3];
    }

    boolean inBounds(DMapPoint p){
        return (p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3]);
    }

    boolean StrictInBounds(DMapPoint p){
        return (p.x > bounds[0] && p.x < bounds[2] && p.y > bounds[1] && p.y < bounds[3]);
    }

    void split(){
        if (isEndNode) {
            this.isEndNode = false;

            int hx = (bounds[0] + bounds[2]) / 2;
            int hy = (bounds[1] + bounds[3]) / 2;

            this.nw = new QuadTreeNode(new int[]{bounds[0], bounds[1], hx, hy});
            this.ne = new QuadTreeNode(new int[]{hx, bounds[1], bounds[2], hy});
            this.sw = new QuadTreeNode(new int[]{bounds[0], hy, hx, bounds[3]});
            this.se = new QuadTreeNode(new int[]{hx, hy, bounds[2], bounds[3]});

            nw.depth = depth + 1;
            ne.depth = depth + 1;
            sw.depth = depth + 1;
            se.depth = depth + 1;

            nw.parent = ne.parent = sw.parent = se.parent = this;

            for (Node n : nodes) {
                if (n.x <= hx) {
                    if (n.y <= hy) {
                        nw.add(n);
                    } else {
                        sw.add(n);
                    }
                } else {
                    if (n.y <= hy) {
                        ne.add(n);
                    } else {
                        se.add(n);
                    }
                }
            }
            nodes.clear();
            nodes = null;
            
            for (RoadGraphNode rgn: roadGraphNodes){
                if (rgn.n.x <= hx) {
                    if (rgn.n.y <= hy) {
                        nw.add(rgn);
                    } else {
                        sw.add(rgn);
                    }
                } else {
                    if (rgn.n.y <= hy) {
                        ne.add(rgn);
                    } else {
                        se.add(rgn);
                    }
                }
            }
            roadGraphNodes.clear();
            roadGraphNodes = null;

            for (MapShape m : shapes) {
                nw.add(m);
                ne.add(m);
                sw.add(m);
                se.add(m);
            }

            shapes.clear();
            shapes = null;
            //------------------------------------------

        }
    }

    void add (RoadGraphNode rgn){

        if (!this.isEndNode) {
            if (rgn.n.x <= (bounds[0] + bounds[2]) / 2) {
                if (rgn.n.y <= (bounds[1] + bounds[3]) / 2) {
                    nw.add(rgn);
                } else {
                    sw.add(rgn);
                }
            } else {
                if (rgn.n.y <= (bounds[1] + bounds[3]) / 2) {
                    ne.add(rgn);
                } else {
                    se.add(rgn);
                }
            }
        } else {
            roadGraphNodes.add(rgn);
            if (divide && roadGraphNodes.size() > QuadTree.THRESHOLD) {
                split();
            }
        }
    }

    void add(Node n) {
        if (!this.isEndNode) {
            if (n.x <= (bounds[0] + bounds[2]) / 2) {
                if (n.y <= (bounds[1] + bounds[3]) / 2) {
                    nw.add(n);
                } else {
                    sw.add(n);
                }
            } else {
                if (n.y <= (bounds[1] + bounds[3]) / 2) {
                    ne.add(n);
                } else {
                    se.add(n);
                }
            }
        } else {
            this.nodes.add(n);
            if (divide && this.nodes.size() > QuadTree.NODE_THRESHOLD) {
                this.split();
            }
        }
    }

    void add(final MapShape m){
        if (this.isEndNode){
            if (m.isPoly){
                //addPoly_double(new MapShape(m.way)); //TODO: simple poly adding
                addPoly_simple(m);
                //addPoly_double(m);
            }else {
                addRoad(m);
            }
            if (divide && shapes.size() > QuadTree.THRESHOLD_SHAPE){
                split();
            }
        }else{
            for (QuadTreeNode node: this) {
                if (node.can_contain_shape(m)) {
                    node.add(m);
                }
            }
        }

    }

    void add(RoadGraphLine line){
        if (isEndNode){
            DMapPoint p1 = new DMapPoint(line.n1.n);
            DMapPoint p2 = new DMapPoint(line.n2.n);
            if (inBounds(p1) || inBounds(p2)){
                roadGraphLines.add(line);
            }else {
                DMapPoint h1 = DHorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
                DMapPoint h2 = DHorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
                DMapPoint v1 = DVertCross(bounds[0], bounds[1], bounds[3], p1, p2);
                DMapPoint v2 = DVertCross(bounds[2], bounds[1], bounds[3], p1, p2);

                if (h1 != null || h2 != null || v1 != null || v2 != null){
                    roadGraphLines.add(line);
                }
            }
        }else{
            for (QuadTreeNode t: this){
                DMapPoint p1 = new DMapPoint(line.n1.n);
                DMapPoint p2 = new DMapPoint(line.n2.n);
                if (t.inBounds(p1) || t.inBounds(p2)){
                    t.add(line);
                }else {
                    DMapPoint h1 = DHorzCross(t.bounds[1], t.bounds[0], t.bounds[2], p1, p2);
                    DMapPoint h2 = DHorzCross(t.bounds[3], t.bounds[0], t.bounds[2], p1, p2);
                    DMapPoint v1 = DVertCross(t.bounds[0], t.bounds[1], t.bounds[3], p1, p2);
                    DMapPoint v2 = DVertCross(t.bounds[2], t.bounds[1], t.bounds[3], p1, p2);

                    if (h1 != null || h2 != null || v1 != null || v2 != null){
                        t.add(line);
                    }
                }
            }
        }
    }

    private void addRoad(final MapShape m){
        MapShape shape = new MapShape();
        shape.copyParams(m);
        DMapPoint p1 = new DMapPoint(m.points.get(0));
        boolean inTreeNode = inBounds(p1);
        if (inTreeNode){shape.points.add(p1.p);}

        for (int i=1; i < m.points.size(); i++){
            DMapPoint p2 = new DMapPoint(m.points.get(i));
            DMapPoint h1 = DHorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
            DMapPoint h2 = DHorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
            DMapPoint v1 = DVertCross(bounds[0], bounds[1], bounds[3], p1, p2);
            DMapPoint v2 = DVertCross(bounds[2], bounds[1], bounds[3], p1, p2);

            DMapPoint temp[] = new DMapPoint[]{h1, h2, v1, v2};
            int intersections = 0;
            for (int z=0; z<4; z++){
                if (temp[z] != null){
                    for (int z1=z+1; z1<4; z1++){
                        if (temp[z1] != null && temp[z].equals(temp[z1])){
                            temp[z1] = null;
                        }
                    }
                    if (temp[z].equals(p1) || temp[z].equals(p2)){
                        temp[z] = null;
                    }
                }

                intersections += (temp[z] != null ? 1 : 0);
            }

            if (inTreeNode && intersections == 0){
                shape.points.add(p2.p);
            }else if (intersections == 1){
                inTreeNode = !inTreeNode;
                if (inTreeNode){
                    shape = new MapShape();
                    shape.copyParams(m);
                }else{
                    shapes.add(shape);
                    items++;
                }
                shape.points.add((temp[0] != null ? temp[0]:(temp[1] != null? temp[1]: (temp[2] != null ? temp[2]: temp[3]))).p);
                if (inTreeNode) {
                    shape.points.add(p2.p);
                }
            }else if (intersections == 2){
                for (DMapPoint point: temp){
                    if (point != null){
                        shape.points.add(point.p);
                    }
                }
                shapes.add(shape);
                items++;
                inTreeNode = false;
                shape = new MapShape();
                shape.copyParams(m);
            }
            p1 = p2;
        }
        if (shape.points.size() >= 2){
            shapes.add(shape);
        }
    }

    private void addPoly_simple(final MapShape m){
        if     (!((m.way.bounds[0] < bounds[0] && m.way.bounds[2] < bounds[0])
                ||(m.way.bounds[0] > bounds[2] && m.way.bounds[2] > bounds[2])
                ||(m.way.bounds[1] < bounds[1] && m.way.bounds[3] < bounds[1])
                ||(m.way.bounds[1] > bounds[3] && m.way.bounds[3] > bounds[3])
                )){
            shapes.add(m);
        }
    }

    static DMapPoint DHorzCross(int horz, int x1, int x2, DMapPoint p1, DMapPoint p2){
        if ((p1.y > horz && p2.y > horz) || (p1.y < horz && p2.y < horz)){
            return null;
        }else{
            double x = p1.x + ((horz-p1.y)/(p2.y-p1.y))*(p2.x-p1.x);
            if (x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) {
                DMapPoint p = new DMapPoint(x, horz);
                p.intersec = true;
                return p;
            }else {
                return null;
            }
        }
    }

    static DMapPoint DVertCross(int vert, int y1, int y2, DMapPoint p1, DMapPoint p2){
        if ((p1.x > vert && p2.x > vert) || (p1.x < vert && p2.x < vert)){
            return null;
        }else{
            double y = p1.y + ((vert-p1.x)/(p2.x-p1.x))*(p2.y-p1.y);
            if (y >= Math.min(y1, y2) && y <= Math.max(y1, y2)) {
                DMapPoint p = new DMapPoint(vert, y);
                p.intersec = true;
                return p;
            }else {
                return null;
            }
        }
    }


    public Iterator<QuadTreeNode> iterator(){
        return new Iterator<QuadTreeNode>() {
            byte ind = 0;
            @Override
            public boolean hasNext() {
                return ind < 4;
            }

            @Override
            public QuadTreeNode next() {
                switch (ind++){
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