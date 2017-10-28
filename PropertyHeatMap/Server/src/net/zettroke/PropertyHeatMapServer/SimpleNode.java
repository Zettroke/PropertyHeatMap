package net.zettroke.PropertyHeatMapServer;

/**
 * Created by Zettroke on 21.10.2017.
 */

// Nodes which exist on real map, but i don't need to know to which object they are related
public class SimpleNode extends MapPoint{
    long id;
    double lon;
    double lat;

    public SimpleNode(Node n){
        this.lon = n.lon;
        this.lat = n.lat;
        this.id = n.id;
    }
    public SimpleNode(){}

}