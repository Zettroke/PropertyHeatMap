package net.zettroke.PropertyHeatMapServer.map;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */

public class SimpleNode extends MapPoint{

    long id;
    double lon;
    double lat;




    public SimpleNode(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }

    public SimpleNode(){}

    public SimpleNode(Node n){
        id = n.id;
        lon = n.lon;
        lat = n.lat;
        x = n.x;
        y = n.y;
    }
    public SimpleNode(MapPoint p){
        x = p.x;
        y = p.y;
    }
}
