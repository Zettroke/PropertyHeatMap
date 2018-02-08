package net.zettroke.PropertyHeatMapServer.map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

/**
 * Created by Zettroke on 29.10.2017.
 */
public class TestPolygonClipping {
    public static void test() throws Exception{
        //int[] bounds = new int[]{20736, 23040, 23040, 25344};
        int[] bounds = new int[]{221181, 331780, 225277, 335876};
        //BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        BufferedImage image = new BufferedImage(bounds[2]-bounds[0]+500, bounds[3]-bounds[1]+500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        QuadTreeNode t = new QuadTreeNode(new int[]{250, 250, 250+bounds[2]-bounds[0], 250+bounds[3]-bounds[1]});
        MapShape m = new MapShape((Way)(new ObjectInputStream(new FileInputStream("way.obj"))).readObject());
        m.isPoly = true;

        for (int i=0; i<m.points.size()-1; i++){
            m.points.get(i).x += (250-bounds[0]); m.points.get(i).y += (250-bounds[1]);
        }

        System.out.println(m.isClockwise());
        m.makeClockwise();


        int circle_x=400, circle_y=820;
        int radius = 200;
        //System.out.println();


        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0, 0, 0));
        g.drawRect(t.bounds[0], t.bounds[1], t.bounds[2]-t.bounds[0], t.bounds[3]-t.bounds[1]);

        g.setStroke(new BasicStroke(2.5f));


        Polygon polygon = new Polygon();
        for (MapPoint p: m.points){
            polygon.addPoint(p.x, p.y);
            g.drawString(""+m.points.indexOf(p), p.x+8, p.y-5);

        }
        g.setColor(new Color(44, 230, 168));
        g.draw(polygon);

        t.add(m);
        System.out.println("added");
        //QuadTree tree = new QuadTree(new int[]{0, 0, 0, 0});
        //tree.root = t;
        //Collection<Way> ways = tree.findShapesByCircle(new MapPoint(circle_x, circle_y), radius);
        //if (ways.size()!=0){
        //    System.out.println("FOUND");
        //}
        //g.setColor(new Color(255,0, 0));
        //g.fillRect(circle_x-2, circle_y-radius-2, 4, 4);

        for (MapShape shp: t.shapes){
            Polygon poly = new Polygon();
            for (MapPoint p: shp.points){
                poly.addPoint(p.x, p.y);

            }
            g.setColor(new Color(198, 209, 0, 100));
            g.fill(poly);
            g.setColor(new Color(0, 0, 0));
            g.draw(poly);
        }

        g.setColor(new Color(255, 0, 0));
        for (MapPoint point: QuadTreeNode.intersec_debug){
            g.fillRect(point.x-2, point.y-2, 4, 4);
        }

        //g.setStroke(new BasicStroke(2));
        //g.setColor(new Color(255, 0, 0));
//
        //g.drawOval(circle_x-radius, circle_y-radius, 2*radius, 2*radius);

        ImageIO.write(image, "png", new File("testPolygonClip.png"));
        System.out.println("done");
        /*Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        bench(m, t);
        scanner.nextLine();*/

    }

    static void bench(MapShape m, QuadTreeNode t){
        // warm up
        /*for (int i=0; i<1000000; i++) {
            t.add(m);
            t.shapes.clear();
        }*/

        int test_iter = 10000000;
        for (int i=0; i<test_iter; i++) {
            t.add(m);
            t.shapes.clear();
        }
        long start = System.nanoTime();
        for (int i=0; i<test_iter; i++) {
            t.add(m);
            t.shapes.clear();
        }
        long end = System.nanoTime();
        System.out.println("Total time: " + (end-start)/1000000D + " milliseconds. Time per op: " + ((end-start)/(double)test_iter)/1000D + " microseconds.");
    }
}
