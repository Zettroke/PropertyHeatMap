package net.zettroke.PropertyHeatMapServer;

import java.io.Serializable;

/**
 * Created by Zettroke on 21.10.2017.
 */

// Points created by QuadTree. Doesn't exist on real map.
public class MapPoint implements Serializable{
    int x;
    int y;

    public MapPoint(){}

    public MapPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public boolean equals(MapPoint p) {
        return x == p.x && y == p.y;

    }

    public MapPoint clone(){
        return new MapPoint(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /*@Override
    public int hashCode() {
        return x << 16 | y & 0xFFFF;
    }*/
}
