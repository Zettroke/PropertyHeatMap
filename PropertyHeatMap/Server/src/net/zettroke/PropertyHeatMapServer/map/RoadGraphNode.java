package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.utils.RoadType;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class RoadGraphNode {
    public Node n;
    public int dist = Integer.MAX_VALUE;
    public RoadGraphNode[] ref_to;
    public Integer[] distances;
    public HashSet<String> road_types = new HashSet<>();
    public HashSet<RoadType> types = new HashSet<>();
    public ArrayList<RoadType> ref_types = new ArrayList<>();
    public boolean visited = false;

    RoadGraphNode(Node n){
        this.n = n;
        n.isRoadNode = true;
    }

    public void addWay(Way way){
        types.add(RoadType.getType(way.data));
        // road_types.add(s);
    }

    public RoadGraphNode clone(){
        RoadGraphNode res = new RoadGraphNode(n);
        res.road_types = road_types;
        res.types = types;
        return res;
    }

    public Color getNodeColor(int max_dist){
        if (this.dist <= max_dist) {
            double c = Math.pow(this.dist/(double)max_dist, 2);
            //double hue = (240-Math.pow(c-0.5, 5)*480*7.3333333+10*c+120)/360.0;
            Color cl = Color.getHSBColor((float)((1-c)*120.0/360.0), 0.9f, 0.9f);
            //Color cl = Color.getHSBColor((float)hue, 0.9f, 0.9f);
            return new Color(cl.getRed(), cl.getGreen(), cl.getBlue());

        }else{
            return new Color(168, 0, 22);
        }
    }
}
