package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.utils.Apartment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Way implements Serializable{
    public long id;
    public ArrayList<SimpleNode> nodes = new ArrayList<>();
    public HashMap<String, String> data = new HashMap<>();
    public int[] legth;
    public ArrayList<Apartment> apartments;

    public MapPoint getCenter(){
        long x = 0;
        long y = 0;
        for (SimpleNode n: nodes){
            x += n.x;
            y += n.y;
        }
        return new MapPoint(Math.round(x/(float)nodes.size()), Math.round(y/(float)nodes.size()));
    }

}
