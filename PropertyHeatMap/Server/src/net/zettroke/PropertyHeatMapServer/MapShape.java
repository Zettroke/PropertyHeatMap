package net.zettroke.PropertyHeatMapServer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

class MapShape {
    ArrayList<MapPoint> points = new ArrayList<>();


    Way way;
    int index;
    boolean isPoly;

    MapShape(){}

    public MapShape clone(){
        MapShape m = new MapShape();
        m.points = (ArrayList<MapPoint>) points.clone();
        m.way = way;
        m.index = index;
        m.isPoly = isPoly;
        return m;
    }

    MapShape(ArrayList<MapPoint> mp){
        points = (ArrayList<MapPoint>) mp.clone();
    }

    MapShape(Way w){
        way = w;
        points = (ArrayList<MapPoint>) w.nodes.clone();
        isPoly = way.data.containsKey("building");
        if (isPoly) {
            makeClockwise();
        }
    }

    void copyParams(MapShape m){
        way = m.way;
        index = m.index;
        isPoly = m.isPoly;
    }

    boolean isClockwise(){
        MapPoint p1 = points.get(0);
        long signed_area = 0;
        for (int i=1; i<points.size(); i++){
            MapPoint p2 = points.get(i);
            //signed_area += (p2.x-p1.x)*(p2.y+p1.y);
            signed_area += (p1.x*p2.y - p2.x*p1.y);
            p1 = p2;
        }
        return signed_area > 0;
    }

    void makeClockwise(){
        if (!isClockwise()){
            Collections.reverse(points);
        }
    }

    boolean contain(MapPoint p){
        int cross = 0;
        MapPoint p1, p2;
        for (int i=0; i<points.size()-1; i++){
            p1 = points.get(i);
            p2 = points.get(i+1);
            if (Math.max(p1.y, p2.y) >= p.y && Math.min(p1.y, p2.y) <= p.y) {
                if (!(p1.y == p.y && p2.y == p.y)) {
                    if (p1.x >= p.x && p2.x >= p.x) {
                        cross++;
                    } else if (p1.x >= p.x || p2.x >= p.x) {
                        double coef = (p.y - p1.y) / (double) (p2.y - p1.y);
                        int x = (int) Math.round(p1.x + (p2.x - p1.x) * coef);
                        if (x >= p.x) {
                            cross++;
                        }
                    }
                }
            }
        }
        return cross % 2 == 1;
    }

    Polygon getPolygon(){
        Polygon polygon = new Polygon();

        for (MapPoint p: points){
            polygon.addPoint(p.x, p.y);
        }

        return polygon;
    }
}