package net.zettroke.PropertyHeatMapServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
//-XX:+UnlockCommercialFeatures -XX:+FlightRecorder

public class Main {

    static HashMap<Long, MapPoint> map = new HashMap<>();
    static PropertyMap propertyMap;
    static ArrayList<Polygon> mapPoly = new ArrayList<>();

    public static void main(String[] args) throws Exception{
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        propertyMap = new PropertyMap();
        PropertyMapLoaderOSM.load(propertyMap, "map_small.osm");
        long start = System.nanoTime();
        //propertyMap.tree.root.split();
        propertyMap.initParallel();


        //propertyMap.ways.stream().filter(p -> p.data.containsKey("highway") && p.data.containsKey("alt_name:mcm") && p.data.get("alt_name:mcm").equals("Измайловское шоссе")).collect(Collectors.toList());


        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        scanner.nextLine();



        BufferedImage image = new BufferedImage(propertyMap.x_end-propertyMap.x_begin, propertyMap.y_end-propertyMap.y_begin, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, propertyMap.x_end-propertyMap.x_begin, propertyMap.y_end-propertyMap.y_begin);
        g.setColor(new Color(0, 0, 0));
        draw(g, propertyMap.tree.root);

        g.setColor(new Color(255, 0, 0));
        for (Polygon polygon: mapPoly) {
            g.fill(polygon);
        }
        ImageIO.write(image, "png", new FileOutputStream("QuadTreeSubdivision.png"));


        //TestPolygonClipping.test();

    }

    static void draw(Graphics2D g, QuadTree.TreeNode t){
        if (!t.isEndNode){
            for (QuadTree.TreeNode tn: t){
                draw(g, tn);
            }
        }else{
            g.setColor(new Color(7, 228, 0, 74));
            g.setStroke(new BasicStroke(8f));
            g.drawRect(t.bounds[0], t.bounds[1], t.bounds[2]-t.bounds[0], t.bounds[3]-t.bounds[1]);
            //g.setColor(new Color(255, 238, 192));
            g.setColor(new Color(0, 0, 0));
            for (MapShape mh: t.shapes){
                if (mh.isPoly && mh.way.data.containsKey("building")) {
                    g.setStroke(new BasicStroke(4f));
                    Polygon poly = new Polygon();
                    for (MapPoint p : mh.points) {
                        poly.addPoint(p.x, p.y);
                    }
                    if (mh.way.data.containsKey("building")) {
                        g.setColor(new Color(255, 238, 192));
                        g.fill(poly);
                    }
                    g.setColor(new Color(0, 0, 0));
                    g.draw(poly);
                } else{
                    g.setColor(new Color(0, 0, 0));
                    /*if (mh.way.data.get("highway").equals("secondary") && mh.way.data.containsKey("alt_name:mcm")) {
                        if (mh.way.data.get("alt_name:mcm").equals("Измайловское шоссе")) {
                            g.setColor(new Color(255, 0, 0));
                        }
                    }*/

                    g.setStroke(new BasicStroke(10f));
                    Path2D path2D = new Path2D.Float();
                    path2D.moveTo(mh.points.get(0).x, mh.points.get(0).y);
                    for (MapPoint p : mh.points) {
                        path2D.lineTo(p.x, p.y);
                    }

                    g.draw(path2D);
                }

            }


        }
    }

    static int recursive_count(QuadTree.TreeNode t){
        if (t.isEndNode){
            int points = 0;
            for (MapShape m: t.shapes){
                for (int i=0; i<m.points.size(); i++) {
                    MapPoint p = m.points.get(i);
                    long key = (long) p.x << 32 | (long) p.y;
                    if (!map.containsKey(key)) {
                        map.put(key, p);
                        points++;
                    }else {
                        m.points.set(i, map.get(key));
                    }
                }
            }
            return points;
        }else{
            int sum = 0;
            for (QuadTree.TreeNode tn: t){
                sum += recursive_count(tn);
            }
            return sum;
        }
    }
}
