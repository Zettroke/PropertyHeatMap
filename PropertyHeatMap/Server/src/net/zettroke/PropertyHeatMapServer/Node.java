package net.zettroke.PropertyHeatMapServer;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */

// Only node whose related to roads(highways) or relations(public transport)
public class Node extends SimpleNode{

    ArrayList<Way> ways = new ArrayList<>();

    ArrayList<Relation> relations = new ArrayList<>();

    HashMap<String, String> data = new HashMap<>();

    Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

}
