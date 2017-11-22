package net.zettroke.PropertyHeatMapServer.map;

import java.util.*;

public class QuadTreeNode implements Iterable<QuadTreeNode>{

    public static ArrayList<MapPoint> intersec_debug = new ArrayList<>();

    static class SuperSampledMapPoint extends MapPoint{
        static int n = 4;
        MapPoint p;
        boolean intersec = false;

        SuperSampledMapPoint(MapPoint p){
            this(p, false);
        }

        SuperSampledMapPoint(MapPoint p, boolean reversed){
            if (reversed){
                this.p = new MapPoint(Math.round(p.x / (float) n), Math.round(p.y / (float) n));
                this.x = p.x;
                this.y = p.y;
            }else {
                this.p = p;
                x = p.x * n;
                y = p.y * n;
            }
        }
        SuperSampledMapPoint(int x, int y, boolean reversed){
            if (reversed) {
                this.p = new MapPoint(Math.round(x / (float) n), Math.round(y / (float) n));
                this.x = x;
                this.y = y;
            }else{
                this.p = new MapPoint(x, y);
                this.x = p.x*n;
                this.y = p.y*n;
            }
        }

        static MapPoint deSuperSample(MapPoint p){
            return new MapPoint(Math.round(p.x/(float)n), Math.round(p.y/(float)n));
        }


    }

    static SuperSampledMapPoint HorzCross(int horz, int x1, int x2, MapPoint p1, MapPoint p2){
        if ((p1.y > horz && p2.y > horz) || (p1.y < horz && p2.y < horz)){
            return null;
        }else{
            int x = (int)Math.round(p1.x + ((horz-p1.y)/(double)(p2.y-p1.y))*(p2.x-p1.x));
            if (x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) {
                SuperSampledMapPoint p = new SuperSampledMapPoint(x, horz, true);
                p.intersec = true;
                return p;
            }else {
                return null;
            }
        }
    }

    
    static SuperSampledMapPoint VertCross(int vert, int y1, int y2, MapPoint p1, MapPoint p2){
        if ((p1.x > vert && p2.x > vert) || (p1.x < vert && p2.x < vert)){
            return null;
        }else{
            int y = (int)Math.round(p1.y + ((vert-p1.x)/(double)(p2.x-p1.x))*(p2.y-p1.y));
            if (y >= Math.min(y1, y2) && y <= Math.max(y1, y2)) {
                 SuperSampledMapPoint p = new SuperSampledMapPoint(vert, y, true);
                 p.intersec = true;
                 return p;
            }else {
                return null;
            }
        }
    }

    
    static MapPoint RoadHorzCross(int horz, int x1, int x2, MapPoint p1, MapPoint p2){
        if ((p1.y >= horz && p2.y >= horz) || (p1.y <= horz && p2.y <= horz)){
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

    
    static MapPoint RoadVertCross(int vert, int y1, int y2, MapPoint p1, MapPoint p2){
        if ((p1.x >= vert && p2.x >= vert) || (p1.x <= vert && p2.x <= vert)){
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


    class SquareComparator implements Comparator<MapPoint> {

        ArrayList<MapPoint> points;

        @Override
        public int compare(MapPoint point1, MapPoint point2) {
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

            for (int i=0; i<4; i++){
                MapPoint p_temp = points.get(i);
                if (point1.equals(p_temp)){
                    side1 = i;
                }
                if (point2.equals(p_temp)){
                    side2 = i;
                }
            }


            if (side1 == side2){
                return (Math.abs(point1.x-points.get(side1).x) + Math.abs(point1.y-points.get(side1).y)) - (Math.abs(point2.x-points.get(side1).x) + Math.abs(point2.y-points.get(side1).y));
            }else{
                return side1 - side2;
            }

        }

        public SquareComparator(MapPoint p0, MapPoint p1, MapPoint p2, MapPoint p3) {
            points = new ArrayList<>(Arrays.asList(p0, p1, p2, p3));
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

    //PropertyMap.ParallelInitThread th_temp;

    int depth = 0;

    int items = 0;
    public boolean isEndNode = true;
    public int[] bounds;
    QuadTreeNode parent;
    QuadTreeNode nw;
    QuadTreeNode ne;
    QuadTreeNode sw;
    QuadTreeNode se;
    public ArrayList<MapShape> shapes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();

    QuadTreeNode(int[] bounds){
        this.bounds = bounds;
    }

    boolean inBounds(MapPoint p){
        return inBounds(p, false);
    }

    boolean inBounds(MapPoint p, boolean super_sampled){
        if (super_sampled){
            return (p.x >= bounds[0]*SuperSampledMapPoint.n && p.x <= bounds[2]*SuperSampledMapPoint.n && p.y >= bounds[1]*SuperSampledMapPoint.n && p.y <= bounds[3]*SuperSampledMapPoint.n);
        }else {
            return (p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3]);
        }
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
            this.items++;
            if (this.items > QuadTree.THRESHOLD) {
                this.split();
            }
        }
    }

    private void addRoad(final MapShape m){
        MapShape shape = new MapShape();
        shape.copyParams(m);
        MapPoint p1 = m.points.get(0);
        boolean inTreeNode = inBounds(p1);
        if (inTreeNode){shape.points.add(p1);}

        for (int i=1; i < m.points.size(); i++){
            MapPoint p2 = m.points.get(i);
            MapPoint h1 = RoadHorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
            MapPoint h2 = RoadHorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
            MapPoint v1 = RoadVertCross(bounds[0], bounds[1], bounds[3], p1, p2);
            MapPoint v2 = RoadVertCross(bounds[2], bounds[1], bounds[3], p1, p2);
            MapPoint temp[] = new MapPoint[]{h1, h2, v1, v2};
            for (int z1=0; z1<4; z1++){
                for (int z2=0; z2<4; z2++){
                    if (z1 != z2 && temp[z1] != null && temp[z2] != null){
                        if (temp[z1].equals(temp[z2])){
                            temp[z2] = null;
                        }
                    }

                }
            }

            int intersections = (temp[0] != null ? 1: 0) + (temp[1] != null ? 1: 0) + (temp[2] != null ? 1: 0) + (temp[3] != null ? 1: 0);
            if (inTreeNode && intersections == 0){
                shape.points.add(p2);
            }else if (intersections == 1){
                inTreeNode = !inTreeNode;
                if (inTreeNode){
                    shape = new MapShape();
                    shape.copyParams(m);
                }else{
                    shapes.add(shape);
                }
                shape.points.add((temp[0] != null ? temp[0]:(temp[1] != null? temp[1]: (temp[2] != null ? temp[2]: temp[3]))));
                if (inTreeNode) {
                    shape.points.add(p2);
                }
            }else if (intersections == 2){
                for (MapPoint point: temp){
                    if (point != null){
                        shape.points.add(point);
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

    private void addPoly(final MapShape m_input){
        // super sampling
        int[] bounds = new int[]{this.bounds[0]*SuperSampledMapPoint.n,this.bounds[1]*SuperSampledMapPoint.n, this.bounds[2]*SuperSampledMapPoint.n, this.bounds[3]*SuperSampledMapPoint.n};


        boolean containShape = true;
        MapShape m = new MapShape();
        m.copyParams(m_input);
        for (MapPoint p : m_input.points){
            containShape &= inBounds(p);
            m.points.add(new SuperSampledMapPoint(p));
        }
        if (containShape){
            shapes.add(m_input);
            items++;
            return;
        }
        SuperSampledMapPoint p00 = new SuperSampledMapPoint(new MapPoint(this.bounds[0], this.bounds[1])), p11=new SuperSampledMapPoint(new MapPoint(this.bounds[2], this.bounds[1]));
        SuperSampledMapPoint p22 = new SuperSampledMapPoint(new MapPoint(this.bounds[2], this.bounds[3])), p33=new SuperSampledMapPoint(new MapPoint(this.bounds[0], this.bounds[3]));
        ArrayList<SuperSampledMapPoint> shape = new ArrayList<>();
        ArrayList<SuperSampledMapPoint> square = new ArrayList<>();
        SuperSampledMapPoint p1 = (SuperSampledMapPoint)m.points.get(0);
        shape.add(p1);
        int total_intersec = 0;
        boolean flag = true;

        for (int i=1; i<m.points.size(); i++){
            SuperSampledMapPoint p2 = (SuperSampledMapPoint)m.points.get(i);
            SuperSampledMapPoint h1 = HorzCross(bounds[1], bounds[0], bounds[2], p1, p2);
            SuperSampledMapPoint h2 = HorzCross(bounds[3], bounds[0], bounds[2], p1, p2);
            SuperSampledMapPoint v1 = VertCross(bounds[0], bounds[1], bounds[3], p1, p2);
            SuperSampledMapPoint v2 = VertCross(bounds[2], bounds[1], bounds[3], p1, p2);

            SuperSampledMapPoint[] lul = new SuperSampledMapPoint[]{h1, h2, v1, v2};
            int intersections = 0;
            for (int z=0; z<4; z++){
                if (lul[z] != null) {
                    if (lul[z].equals(p1) || lul[z].equals(p2)) {
                        MapPoint pp, pc;
                        if (lul[z].equals(p1)) {
                            pp = m.points.get(i - 2 >= 0 ? i - 2 : m.points.size() - 1);
                            pc = p2;
                        } else {
                            pp = m.points.get((i + 1) % m.points.size());
                            pc = p1;
                        }
                        switch (z) {
                            case 0:
                                if (pc.y >= bounds[1] && pp.y >= bounds[1] || pc.y <= bounds[1] && pp.y <= bounds[1]) {
                                    lul[z] = null;
                                }
                                break;
                            case 1:
                                if (pc.y >= bounds[3] && pp.y >= bounds[3] || pc.y <= bounds[3] && pp.y <= bounds[3]) {
                                    lul[z] = null;
                                }
                                break;
                            case 2:
                                if (pc.x >= bounds[0] && pp.x >= bounds[0] || pc.x <= bounds[0] && pp.x <= bounds[0]) {
                                    lul[z] = null;
                                }
                                break;
                            case 3:
                                if (pc.x >= bounds[2] && pp.x >= bounds[2] || pc.x <= bounds[2] && pp.x <= bounds[2]) {
                                    lul[z] = null;
                                }
                                break;
                        }
                    }
                }
                if (lul[z] != null) {
                    if (lul[z].equals(p00) || lul[z].equals(p11) || lul[z].equals(p22) || lul[z].equals(p33)) {
                        if (!(inBounds(p1,true) || inBounds(p2, true))) {
                            lul[z] = null;
                            // TODO: Чекнуть на пересечение одиной из диагоналей квадрата.
                        }
                    }
                }

                if (lul[z] != null){

                    intersections++;

                    if (lul[z].equals(p00)) {
                        lul[z] = p00;
                    } else if (lul[z].equals(p11)) {
                        lul[z] = p11;
                    } else if (lul[z].equals(p22)) {
                        lul[z] = p22;
                    } else if (lul[z].equals(p33)) {
                        lul[z] = p33;
                    }


                    square.add(lul[z]);
                    intersec_debug.add(lul[z].p);
                }
            }

            total_intersec += intersections;
            if (intersections == 1){
                SuperSampledMapPoint pt = (lul[0] != null ? lul[0]: (lul[1] != null? lul[1]: (lul[2] != null ? lul[2]: lul[3])));
                if (p1.equals(pt)){
                    shape.remove(shape.size()-1);
                }
                if (p2.equals(pt)){
                    flag = false;
                }

                shape.add(pt);
            }else if (intersections == 2){
                SuperSampledMapPoint intersec1 = (lul[0] != null ? lul[0]:(lul[1] != null? lul[1]: (lul[2] != null ? lul[2]: lul[3])));
                SuperSampledMapPoint intersec2 = (lul[3] != null ? lul[3]:(lul[2] != null? lul[2]: (lul[1] != null ? lul[1]: lul[0])));


                if (p1.equals(intersec1) || p1.equals(intersec2)){
                    shape.remove(shape.size()-1);
                }
                if (p2.equals(intersec1) || p2.equals(intersec2)) {
                    flag = false;
                }

                if ((p1.x-intersec1.x)*(p1.x-intersec1.x) + (p1.y-intersec1.y)*(p1.y-intersec1.y)
                        < (p1.x-intersec2.x)*(p1.x-intersec2.x) + (p1.y-intersec2.y)*(p1.y-intersec2.y)){
                    shape.add(intersec1);
                    shape.add(intersec2);
                }else{
                    shape.add(intersec2);
                    shape.add(intersec1);
                }
            }
            if (flag) {
                shape.add(p2);
            }else{
                flag = true;
            }
            p1 = p2;
        }

        if (total_intersec < 2){
            //th_temp.count_calls_contain++;
            if (m.contain(p00) && m.contain(p11) && m.contain(p22) && m.contain(p33)){

                MapShape sh = new MapShape(new ArrayList<>(Arrays.asList(p00.p, p11.p, p22.p, p33.p, p00.p)));

                sh.copyParams(m);
                shapes.add(sh);
                items++;
            }
            return;
        }

        shape = deduplicate(shape, true);

        square.add(p00);
        square.add(p11);
        square.add(p22);
        square.add(p33);

        square = deduplicate(square, false);

        square.sort(new QuadTreeNode.SquareComparator(p00, p11, p22, p33));

        //Linking cross points on shape and on square.
        ArrayList<Integer> shapeCrossPointsInd = new ArrayList<>();
        ArrayList<Integer> squareCrossPointsInd = new ArrayList<>(Arrays.asList(new Integer[square.size()]));
        Collections.fill(squareCrossPointsInd, -1);
        ArrayList<Boolean> shapeAvail = new ArrayList<>();

        for (MapPoint p: shape){
            shapeAvail.add(inBounds(p, true));
        }


        int index = _findFineStartIndex(shape, shapeAvail)+1;
        if (index == 0){
            return; // magic
        }

        for (int i=0; i<shape.size(); i++){
            int ind = square.indexOf(shape.get(i));
            shapeCrossPointsInd.add(ind);
            if (ind != -1){
                squareCrossPointsInd.set(ind, i);
            }
        }

        MapShape currentShape = new MapShape();
        currentShape.copyParams(m);
        MapPoint start = shape.get(index-1);
        boolean onShape = true;
        SuperSampledMapPoint p;
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
            currentShape.points.add(p.p);
            if (p == start){
                currentShape.closePolygon();
                shapes.add(currentShape);
                items++;
                shapeAvail.set(shape.indexOf(start), false);
                currentShape = new MapShape();
                currentShape.copyParams(m);
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

    private ArrayList<SuperSampledMapPoint> deduplicate(ArrayList<SuperSampledMapPoint> m, boolean last_equals){
        ArrayList<SuperSampledMapPoint> res = new ArrayList<>();
        for (SuperSampledMapPoint p: m){
            if (!res.contains(p)){
                res.add(p);
            }
        }
        if (last_equals && !res.get(0).equals(res.get(res.size()-1))) {
            res.add(res.get(0));
        }
        return res;
    }

    private int _findFineStartIndex(final List<SuperSampledMapPoint> m, final ArrayList<Boolean> avail){
        for (int i=0; i<m.size()-1; i++){
            if (avail.get(i) && m.get((i+1)%m.size()).intersec){
                return i;
                /*MapPoint p = m.get(i+1);
                if (p.intersec && inBounds(m.get((i+2)%m.size()))){//if (inBounds(m.get(i+1)))
                    return i;
                }*/
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
                //shapes.add(m);
            }
        }else{
            nw.add(m);
            ne.add(m);
            sw.add(m);
            se.add(m);
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