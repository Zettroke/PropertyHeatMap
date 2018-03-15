package net.zettroke.PropertyHeatMapServer.map;

import java.util.HashMap;

public class Node extends SimpleNode{

    public HashMap<String, String> data = new HashMap<>();
    public boolean isRoadNode = false;
    public boolean publicTransportStop = false;

    public double lon;
    public double lat;

    public Node(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

    public Node(){}

    // TODO: lon lat Node init
    public Node(MapPoint p, PropertyMap context){
        x = p.x;
        y = p.y;
        double[] coords = context.inverse_mercator(x, y);
        lon = coords[0];
        lat = coords[1];
    }

    public Node(SimpleNode n, PropertyMap context){
        double[] crds = context.inverse_mercator(n.x, n.y);
        lon = crds[0];
        lat = crds[1];
        x = n.x;
        y = n.y;
        id = n.id;
    }
}
