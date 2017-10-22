package net.zettroke.PropertyHeatMapServer;

/**
 * Created by Zettroke on 21.10.2017.
 */

// Points created by QuadTree. Have doesn't exist on real map.
public class MapPoint {
    int x;
    int y;

    public MapPoint(){}

    public MapPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
