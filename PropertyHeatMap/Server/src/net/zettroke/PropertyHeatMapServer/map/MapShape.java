package net.zettroke.PropertyHeatMapServer.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class MapShape {
    public ArrayList<MapPoint> points = new ArrayList<>();


    public Way way;
    public boolean isPoly;

    MapShape() {
    }

    public MapShape clone() {
        MapShape m = new MapShape();
        m.points = (ArrayList<MapPoint>) points.clone();
        m.way = way;
        m.isPoly = isPoly;
        return m;
    }

    MapShape(ArrayList<MapPoint> mp) {
        points = (ArrayList<MapPoint>) mp.clone();
    }

    MapShape(Way w) {
        way = w;
        w.initBounds();
        points = (ArrayList<MapPoint>) w.nodes.clone();
        isPoly = way.data.containsKey("building");
        if (isPoly) {
            makeClockwise();
            if (!points.get(0).equals(points.get(points.size() - 1))) {
                points.add(points.get(0).clone());
            }
        }
    }

    void copyParams(MapShape m) {
        way = m.way;
        isPoly = m.isPoly;
    }

    boolean isClockwise() {
        MapPoint p1 = points.get(0);
        long signed_area = 0;
        for (int i = 1; i < points.size(); i++) {
            MapPoint p2 = points.get(i);
            //signed_area += (p2.x-p1.x)*(p2.y+p1.y);
            signed_area += (p1.x * p2.y - p2.x * p1.y);
            p1 = p2;
        }
        return signed_area > 0;
    }

    void makeClockwise() {
        if (!isClockwise()) {
            Collections.reverse(points);
        }
    }

    public boolean contain(MapPoint p) {
        //HashSet<Integer> crosses = new HashSet<>();
        int counter = 0;
        int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0};
        MapPoint p1 = points.get(0);
        bounds[0] = Math.min(bounds[0], p1.x);
        bounds[2] = Math.max(bounds[2], p1.x);
        bounds[1] = Math.min(bounds[1], p1.y);
        bounds[3] = Math.max(bounds[3], p1.y);
        MapPoint p2;
        for (int i = 0; i < points.size() - 1; i++) {
            p1 = points.get(i);
            p2 = points.get(i + 1);

            bounds[0] = Math.min(bounds[0], p2.x);
            bounds[2] = Math.max(bounds[2], p2.x);
            bounds[1] = Math.min(bounds[1], p2.y);
            bounds[3] = Math.max(bounds[3], p2.y);
            if (p2.y == p.y) {
                if (!(p1.y >= p.y && points.get((i + 2) % points.size()).y >= p.y || p1.y <= p.y && points.get((i + 2) % points.size()).y <= p.y)) {
                    //crosses.add(points.get(0).x);
                    if (p2.x > p.x) {
                        counter++;
                    }
                }
            } else if (!((p1.y >= p.y && p2.y >= p.y) || (p1.y <= p.y && p2.y <= p.y))) {
                int x = (int) Math.round(p1.x + ((p.y - p1.y) / (double) (p2.y - p1.y)) * (p2.x - p1.x));
                if (x > p.x) {
                    //crosses.add(x);
                    counter++;
                }
            }
        }

        return counter % 2 == 1 && p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3];
    }

    public boolean contain(QuadTreeNode.DMapPoint p) {
        //HashSet<Integer> crosses = new HashSet<>();
        int counter = 0;
        int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0};
        MapPoint p1 = points.get(0);
        bounds[0] = Math.min(bounds[0], p1.x);
        bounds[2] = Math.max(bounds[2], p1.x);
        bounds[1] = Math.min(bounds[1], p1.y);
        bounds[3] = Math.max(bounds[3], p1.y);
        MapPoint p2;
        for (int i = 0; i < points.size() - 1; i++) {
            p1 = points.get(i);
            p2 = points.get(i + 1);

            bounds[0] = Math.min(bounds[0], p2.x);
            bounds[2] = Math.max(bounds[2], p2.x);
            bounds[1] = Math.min(bounds[1], p2.y);
            bounds[3] = Math.max(bounds[3], p2.y);
            if (p2.y == p.y) {
                if (!(p1.y >= p.y && points.get((i + 2) % points.size()).y >= p.y || p1.y <= p.y && points.get((i + 2) % points.size()).y <= p.y)) {
                    //crosses.add(points.get(0).x);
                    if (p2.x > p.x) {
                        counter++;
                    }
                }
            } else if (!((p1.y >= p.y && p2.y >= p.y) || (p1.y <= p.y && p2.y <= p.y))) {
                double x = p1.x + ((p.y - p1.y) / (double) (p2.y - p1.y)) * (p2.x - p1.x);
                if (x > p.x) {
                    //crosses.add(x);
                    counter++;
                }
            }
        }

        return counter % 2 == 1 && p.x >= bounds[0] && p.x <= bounds[2] && p.y >= bounds[1] && p.y <= bounds[3];
    }

    public double minDistToPoint(MapPoint p) {
        double minDist = Double.MAX_VALUE;
        MapPoint p1 = points.get(0);
        minDist = Math.min(minDist, p1.distTo(p));

        MapPoint p2;
        for (int i = 1; i < points.size(); i++) {
            p2 = points.get(i);
            minDist = Math.min(minDist, p2.distTo(p));
            double A = p1.y - p2.y;
            double B = p2.x - p1.x;
            if (A == 0) {
                minDist = Math.min(minDist, Math.abs(p1.y - p.y));
            } else if (B == 0) {
                minDist = Math.min(minDist, Math.abs(p1.x - p.x));
            } else {
                double k1 = -A / B;
                double k2 = B / A;
                double b1 = -(p1.x * p2.y - p2.x * p1.y) / B;
                double b2 = -B / A * p.x + p.y;
                int x = (int) Math.round((b2 - b1) / (k1 - k2));
                int y = (int) Math.round(k1 * ((b2 - b1) / (k1 - k2)) + b1);

                minDist = Math.min(minDist, Math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y)));
            }


            p1 = p2;
        }

        return minDist;
    }

    Polygon getPolygon() {
        Polygon polygon = new Polygon();

        for (MapPoint p : points) {
            polygon.addPoint(p.x, p.y);
        }

        return polygon;
    }

    void closePolygon() {
        if (!points.get(0).equals(points.get(points.size() - 1))) {
            points.add(points.get(0).clone());
        }
    }
}