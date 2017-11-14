package net.zettroke.PropertyHeatMapServer;

import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */

// Only node whose related to roads(highways) or relations(public transport)
public class Node extends SimpleNode{

    @Nullable
    ArrayList<Way> ways = new ArrayList<>();

    @Nullable
    ArrayList<Relation> relations = new ArrayList<>();

    @Nullable
    HashMap<String, String> data = new HashMap<>();

    Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

}
