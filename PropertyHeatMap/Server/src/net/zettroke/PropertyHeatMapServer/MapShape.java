package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;
import java.util.Collections;

class MapShape {
    ArrayList<MapPoint> points = new ArrayList<>();


    Way way;
    int index;
    boolean isPoly;

    MapShape(){}

    MapShape(MapShape m){
        this.points = m.points;
        this.way = m.way;
        this.index = m.index;
    }

    boolean isClockwise(){
        MapPoint p1 = points.get(0);
        long signed_area = 0;
        for (int i=1; i<points.size(); i++){
            MapPoint p2 = points.get(1);
            signed_area += (p2.x-p1.x)*(p2.y+p1.y);
            p1 = p2;
        }
        return signed_area > 0;
    }

    void makeClockwise(){
        if (!isClockwise()){
            Collections.reverse(points);
        }
    }
}