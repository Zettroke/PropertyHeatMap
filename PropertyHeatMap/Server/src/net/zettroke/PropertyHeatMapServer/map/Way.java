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
    public MiddleMapPoint minDistToPoint(MapPoint p) {
        MapPoint min = new MapPoint(Integer.MAX_VALUE, Integer.MAX_VALUE);
        MapPoint p1 = nodes.get(0);
        try{
        min = min.distTo(p) > p1.distTo(p) ? p: min;}
        catch (Exception e){
            System.out.println("exception");
        }
        int min_ind = 0;
        MapPoint p2;
        for (int i = 1; i < nodes.size(); i++) {
            p2 = nodes.get(i);
            min = min.distTo(p) > p2.distTo(p) ? p2: min;
            double A = p1.y - p2.y;
            double B = p2.x - p1.x;
            if (A == 0) {
                MapPoint tp = new MapPoint(p1.x, p.y);
                //minDist = Math.min(minDist, Math.abs(p1.y - p.y));
                if (p.distTo(min) > p.distTo(tp)){
                    min = tp;
                    min_ind = i-1;
                }
            } else if (B == 0) {
                MapPoint tp = new MapPoint(p.x, p1.y);
                //minDist = Math.min(minDist, Math.abs(p1.x - p.x));
                min = min.distTo(p) > tp.distTo(p) ? tp: min;
            } else {
                double k1 = -A / B;
                double k2 = B / A;
                double b1 = -(p1.x * p2.y - p2.x * p1.y) / B;
                double b2 = -B / A * p.x + p.y;
                int x = (int) Math.round((b2 - b1) / (k1 - k2));
                int y = (int) Math.round(k1 * ((b2 - b1) / (k1 - k2)) + b1);
                MapPoint tp = new MapPoint(x, y);
                //minDist = Math.min(minDist, Math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y)));
                min = min.distTo(p) > tp.distTo(p) ? tp: min;
            }


            p1 = p2;
        }
        // TODO: lon lat Node init
        return new MiddleMapPoint(new Node(min), min_ind);
    }


}
class MiddleMapPoint {
    Node p;
    int ind;

    public MiddleMapPoint(Node p, int ind) {
        this.p = p;
        this.ind = ind;
    }
}