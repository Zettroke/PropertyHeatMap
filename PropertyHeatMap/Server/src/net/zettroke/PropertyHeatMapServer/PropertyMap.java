package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();

}


class MapPoint{
    double x, y;
    boolean isShadow;
    Node node;
    ArrayList<MapPolygon> poly;
    ArrayList<Road> road;
}
class MapPolygon{
    ArrayList<MapPoint> points;
    Way way;
}
class Road{
    int ind;
    double length;
    MapPoint p1, p2;
    Way way;
}
