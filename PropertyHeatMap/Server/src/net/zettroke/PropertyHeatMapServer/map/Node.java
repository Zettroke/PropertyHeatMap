package net.zettroke.PropertyHeatMapServer.map;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */

// Only node whose related to roads(highways) or relations(public transport)
public class Node extends SimpleNode{

    public ArrayList<Way> ways = new ArrayList<>();

    public ArrayList<Relation> relations = new ArrayList<>();

    public HashMap<String, String> data = new HashMap<>();

    public Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

}
