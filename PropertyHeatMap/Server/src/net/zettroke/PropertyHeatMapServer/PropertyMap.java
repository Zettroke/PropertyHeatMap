package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMap {

    public static int MAP_RESOLUTION = 262144; //(2**10)*256

    ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();

    QuadTree tree = new QuadTree(new int[]{0, 0, MAP_RESOLUTION, MAP_RESOLUTION});

}




