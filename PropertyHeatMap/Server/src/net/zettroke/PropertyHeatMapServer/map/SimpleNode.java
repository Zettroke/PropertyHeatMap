package net.zettroke.PropertyHeatMapServer.map;

/**
 * Created by Olleggerr on 15.10.2017.
 */

public class SimpleNode extends MapPoint{

    public long id;



    public SimpleNode(){}

    public SimpleNode(Node n){
        id = n.id;
        x = n.x;
        y = n.y;
    }
    public SimpleNode(MapPoint p){
        x = p.x;
        y = p.y;
    }
}
