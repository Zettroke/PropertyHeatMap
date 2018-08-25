package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.utils.Apartment;

import java.awt.geom.Line2D;
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
    public ArrayList<Apartment> apartments;

    int[] bounds;

    public MapPoint getCenter(){
        long x = 0;
        long y = 0;
        for (SimpleNode n: nodes){
            x += n.x;
            y += n.y;
        }
        return new MapPoint(Math.round(x/(float)nodes.size()), Math.round(y/(float)nodes.size()));
    }
    public int minDistToPoint(MapPoint p) {
        MapPoint min = new MapPoint(-1000000, -1000000);
        double min_len = Double.MAX_VALUE;
        MapPoint p1 = nodes.get(0);
        try{
        min = min.distTo(p) > p1.distTo(p) ? p1: min;}
        catch (Exception e){
            System.out.println("exception");
        }
        int min_ind = 0;
        MapPoint p2;
        for (int i = 1; i < nodes.size(); i++) {
            p2 = nodes.get(i);
            //min = min.distTo(p) > p2.distTo(p) ? p2: min;
            int x2 = p2.x;
            int x1 = p1.x;
            int px = p.x;
            int y2 = p2.y;
            int y1 = p1.y;
            int py = p.y;
            x2 -= x1;
            y2 -= y1;
            // px,py becomes relative vector from x1,y1 to test point
            px -= x1;
            py -= y1;
            long dotprod = px * (long)x2 + py * (long)y2;
            double projlenSq;
            if (dotprod <= 0.0) {
                // px,py is on the side of x1,y1 away from x2,y2
                // distance to segment is length of px,py vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0.0;
            } else {
                // switch to backwards vectors relative to x2,y2
                // x2,y2 are already the negative of x1,y1=>x2,y2
                // to get px,py to be the negative of px,py=>x2,y2
                // the dot product of two negated vectors is the same
                // as the dot product of the two normal vectors
                px = x2 - px;
                py = y2 - py;
                dotprod = px * (long)x2 + py * (long)y2;
                if (dotprod <= 0.0) {
                    // px,py is on the side of x2,y2 away from x1,y1
                    // distance to segment is length of (backwards) px,py vector
                    // "length of its (clipped) projection" is now 0.0
                    projlenSq = 0.0;
                } else {
                    // px,py is between x1,y1 and x2,y2
                    // dotprod is the length of the px,py vector
                    // projected on the x2,y2=>x1,y1 vector times the
                    // length of the x2,y2=>x1,y1 vector
                    projlenSq = dotprod * dotprod / (double)(x2 * (long)x2 + y2 * (long)y2);
                }
            }
            // Distance to line is now the length of the relative point
            // vector minus the length of its projection onto the line
            // (which is zero if the projection falls outside the range
            //  of the line segment).
            double lenSq = px * (long)px + py * (long)py - projlenSq;
            double len = lenSq < 0 ? 0:Math.sqrt(lenSq);

            if (len < min_len){
                min_len = len;
                min_ind = i-1;
            }

            p1 = p2;
        }
        // TODO: lon lat Node init
        return min_ind;
    }
    public synchronized void initBounds(){
        if (bounds == null) {
            bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (SimpleNode n : nodes) {
                bounds[0] = Math.min(bounds[0], n.x);
                bounds[1] = Math.min(bounds[1], n.y);
                bounds[2] = Math.max(bounds[2], n.x);
                bounds[3] = Math.max(bounds[3], n.y);
            }
        }
    }
    public boolean inBounds(MapPoint p){
        return (p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3]);
    }
    public int minDistToBounds(MapPoint p){
        if (inBounds(p)){
            return -1;
        }
        if (p.y >= bounds[1] && p.y <= bounds[3]){
            return Math.min(Math.abs(bounds[0]-p.x), Math.abs(bounds[2]-p.x));
        }else if (p.x >= bounds[0] && p.x <= bounds[2]){
            return Math.min(Math.abs(bounds[1]-p.y), Math.abs(bounds[3]-p.y));
        }else{
            return Math.min(Math.min(p.distTo(new MapPoint(bounds[0], bounds[1])), p.distTo(new MapPoint(bounds[0], bounds[3]))),
                            Math.min(p.distTo(new MapPoint(bounds[2], bounds[1])), p.distTo(new MapPoint(bounds[2], bounds[3]))));
        }
    }

    public Way clone(){
        Way r = new Way();
        if (bounds != null) {
            r.bounds = bounds.clone();
        }
        r.id = id;
        r.nodes = (ArrayList<SimpleNode>) nodes.clone();
        r.data = (HashMap<String, String>) data.clone();
        if (apartments != null) {
            r.apartments = (ArrayList<Apartment>) apartments.clone();
        }
        return r;
    }

}
