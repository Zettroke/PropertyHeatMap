package net.zettroke.PropertyHeatMapServer.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class MapShape {
    public ArrayList<MapPoint> points = new ArrayList<>();


    public Way way;
    public int index;
    public boolean isPoly;

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
            if (!points.get(0).equals(points.get(points.size()-1))){
                 points.add(points.get(0).clone());
            }
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

    public boolean contain(MapPoint p){
        //HashSet<Integer> crosses = new HashSet<>();
        int counter = 0;
        int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0};
        MapPoint p1 = points.get(0);
        bounds[0] = Math.min(bounds[0], p1.x);
        bounds[2] = Math.max(bounds[2], p1.x);
        bounds[1] = Math.min(bounds[1], p1.y);
        bounds[3] = Math.max(bounds[3], p1.y);
        MapPoint p2;
        for (int i=0; i<points.size()-1; i++){
            p1 = points.get(i);
            p2 = points.get(i+1);

            bounds[0] = Math.min(bounds[0], p2.x);
            bounds[2] = Math.max(bounds[2], p2.x);
            bounds[1] = Math.min(bounds[1], p2.y);
            bounds[3] = Math.max(bounds[3], p2.y);
            if (p2.y == p.y){
                if (!(p1.y >= p.y && points.get((i+2)%points.size()).y >= p.y || p1.y <= p.y && points.get((i+2)%points.size()).y <= p.y)) {
                    //crosses.add(points.get(0).x);
                    if (p2.x > p.x) {
                        counter++;
                    }
                }
            }else if (!((p1.y >= p.y && p2.y >= p.y) || (p1.y <= p.y && p2.y <= p.y))){
                int x = (int)Math.round(p1.x + ((p.y-p1.y)/(double)(p2.y-p1.y))*(p2.x-p1.x));
                if (x > p.x) {
                    //crosses.add(x);
                    counter++;
                }
            }
        }

        return counter % 2 == 1 && p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3];
    }

    Polygon getPolygon(){
        Polygon polygon = new Polygon();

        for (MapPoint p: points){
            polygon.addPoint(p.x, p.y);
        }

        return polygon;
    }

    void closePolygon(){
        if (!points.get(0).equals(points.get(points.size()-1))){
            points.add(points.get(0).clone());
        }
    }
}