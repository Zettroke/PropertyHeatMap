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
        m.points.add(new MapPoint(300, 30));
        m.points.add(new MapPoint(550, 300));
        m.points.add(new MapPoint(350, 260));
        m.points.add(new MapPoint(50, 330));
        m.points.add(new MapPoint(330, 510));
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


        MapShape sh = t.shapes.get(0);
        //Polygon polygonSH = new Polygon();
        int count = 0;
        for (MapPoint p: sh.points){
            g.setColor(new Color(255, 0, 0));
            g.fillRect(p.x-3, p.y-3, 6, 6);
            g.setColor(new Color(0, 0, 0));
            g.drawString("" + count, p.x+2, p.y-2);
            count++;
        }
        //g.draw(polygon);


        ImageIO.write(image, "png", new File("testPolygonClip.png"));
    }
}
