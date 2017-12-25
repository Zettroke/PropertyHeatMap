package net.zettroke.PropertyHeatMapServer.map;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */

public class Node extends MapPoint{

    long id;
    double lon;
    double lat;

    public HashMap<String, String> data = new HashMap<>();

    boolean isRoadNode = false;

    public Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

    public Node(){}

}
