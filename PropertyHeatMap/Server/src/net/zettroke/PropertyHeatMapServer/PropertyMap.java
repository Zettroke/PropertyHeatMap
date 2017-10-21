package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {
    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();

    QuadTree tree;

}



class MapShape {
    ArrayList<MapPoint> points;


    Way way;
    int index;
    boolean isPoly;
}
