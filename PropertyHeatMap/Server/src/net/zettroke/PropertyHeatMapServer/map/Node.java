package net.zettroke.PropertyHeatMapServer.map;

import java.util.HashMap;

public class Node extends SimpleNode{
    HashMap<String, String> data = new HashMap<>();
    boolean isRoadNode = false;
    boolean publicTransportStop = false;

    public Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

    public Node(){}

    // TODO: lon lat Node init
    public Node(MapPoint p){
        x = p.x;
        y = p.y;
        double[] coords = PropertyMap.propertyMap.inverse_mercator(x, y);
        lon = coords[0];
        lat = coords[1];
    }
}
