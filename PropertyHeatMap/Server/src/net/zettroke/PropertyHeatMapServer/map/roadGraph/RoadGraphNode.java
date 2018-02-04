package net.zettroke.PropertyHeatMapServer.map.roadGraph;

import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.map.RoadType;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;

public class RoadGraphNode {
    public int index;
    public Node n;
    public int dist[];
    public RoadGraphNode[][] ref_to;
    public int[][] distancesTo;
    public RoadType[][] ref_types;

    public static final HashSet<RoadType> foot_exclude = new HashSet<>(Arrays.asList(RoadType.CONSTRUCTION, RoadType.PATH));
    public static final HashSet<RoadType> car_exclude = new HashSet<>(Arrays.asList(RoadType.SUBWAY, RoadType.BUS, RoadType.TRAM, RoadType.TROLLEYBUS,
            RoadType.SERVICE, RoadType.FOOTWAY, RoadType.CONSTRUCTION, RoadType.PATH));

    RoadGraphNode(Node n){
        this.n = n;
    }

    /*public void addWay(Way way){
        types.add(RoadType.getType(way.data));
        // road_types.add(s);
    }*/

    /*public RoadGraphNode clone(){
        RoadGraphNode res = new RoadGraphNode(n);
        res.road_types = road_types;
        res.types = types;
        return res;
    }*/

    public Color getNodeColor(int max_dist, int val){
        float target_low_b = 0.6f;
        if (this.dist[val] <= max_dist) {
            float b = 1;
            float hue = (float) Math.pow((1 - (this.dist[val] / (double) max_dist)), 0.7);
            if (hue < 0.15) {
                b = target_low_b + hue * (1-target_low_b)/0.15f;
            }
            Color cl = Color.getHSBColor(hue * (120f / 360f), 1, b);
            /*double c = Math.pow(this.dist[val]/(double)max_dist, 2);
            //double hue = (240-Math.pow(c-0.5, 5)*480*7.3333333+10*c+120)/360.0;
            Color cl = Color.getHSBColor((float)((1-c)*120.0/360.0), 0.9f, 0.9f);*/
            //Color cl = Color.getHSBColor((float)hue, 0.9f, 0.9f);
            return cl;

        }else{
            return Color.getHSBColor(0, 1, target_low_b);
        }
    }
}
