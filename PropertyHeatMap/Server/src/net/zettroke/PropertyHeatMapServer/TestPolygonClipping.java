package net.zettroke.PropertyHeatMapServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Zettroke on 29.10.2017.
 */
public class TestPolygonClipping {
    static void test() throws IOException {
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, 600, 600);
        QuadTree.TreeNode t = new QuadTree.TreeNode(new int[]{100, 100, 500, 500});
        MapShape m = new MapShape();
        m.isPoly = true;
        m.points.add(new MapPoint(10, 300));
        m.points.add(new MapPoint(250, 250));
        m.points.add(new MapPoint(300, 30));
        m.points.add(new MapPoint(550, 300));
        m.points.add(new MapPoint(350, 260));
        m.points.add(new MapPoint(50, 330));
        m.points.add(new MapPoint(330, 510));
        m.points.add(new MapPoint(480, 350));
        m.points.add(new MapPoint(480, 600));
        m.points.add(new MapPoint(100, 570));
        m.points.add(new MapPoint(10, 300));
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0, 0, 0));
        g.drawRect(100, 100, 400, 400);

        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(44, 230, 168));

        Polygon polygon = new Polygon();
        for (MapPoint p: m.points){
            polygon.addPoint(p.x, p.y);
        }

        g.draw(polygon);

        t.add(m);
        // warm up
        /*for (int i=0; i<1000000; i++) {
            t.add(m);
            t.shapes.clear();
        }

        int test_iter = 1000000;
        long start = System.nanoTime();
        for (int i=0; i<test_iter; i++) {
            t.add(m);
            t.shapes.clear();
        }
        long end = System.nanoTime();
        System.out.println("Total time: " + (end-start)/1000000D + " milliseconds. Time per op: " + ((end-start)/(double)test_iter)/1000D + " microseconds.");
*/

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


        ImageIO.write(image, "png", new File("testPolygonClip.png"));
    }
}
