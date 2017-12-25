package net.zettroke.PropertyHeatMapServer.map;

import java.io.Serializable;

/**
 * Created by Zettroke on 21.10.2017.
 */

// Points created by QuadTree. Doesn't exist on real map.
public class MapPoint implements Serializable{
    public int x;
    public int y;


    public MapPoint(){}

    public MapPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }




    public boolean equals(MapPoint p) {
        return x == p.x && y == p.y;

    }

    public boolean equalsShitty(MapPoint p){
        return Math.abs(x - p.x) + Math.abs(y - p.y) <= 2;
    }

    public MapPoint clone(){
        return new MapPoint(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public long squaredDistTo(MapPoint p){
        return (p.x-this.x)*(p.x-this.x)+(p.y-this.y)*(p.y-this.y);
    }

    public boolean inCircle(MapPoint center, int radius){
        return (this.x-center.x)*(this.x-center.x) + (this.y-center.y)*(this.y-center.y) <= radius*radius;
    }

    public int distTo(MapPoint p){
        return (int)Math.round(Math.sqrt(squaredDistTo(p)));
    }

    /*@Override
    public int hashCode() {
        return x << 16 | y & 0xFFFF;
    }*/
}
