package net.zettroke.PropertyHeatMapServer.map;

import java.util.HashMap;

public class Node extends SimpleNode{
    HashMap<String, String> data = new HashMap<>();
    boolean isRoadNode = false;

    public Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

    public Node(){}
}
