package net.zettroke.PropertyHeatMapServer;

import net.zettroke.PropertyHeatMapServer.QuadTree;
import sun.reflect.generics.tree.Tree;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Zettroke on 28.10.2017.
 */
public class TestRoadClipping {
    static void test() throws IOException{
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, 600, 600);
        QuadTree.TreeNode t = new QuadTree.TreeNode(new int[]{100, 100, 500, 500});
        t.split();
        MapShape m = new MapShape();
        m.isPoly = false;
        m.points.add(new MapPoint(30, 180));
        m.points.add(new MapPoint(180, 30));
        m.points.add(new MapPoint(150, 150));
        m.points.add(new MapPoint(250, 30));
        m.points.add(new MapPoint(170, 320));
        m.points.add(new MapPoint(290, 170));
        m.points.add(new MapPoint(240, 400));
        m.points.add(new MapPoint(400, 280));
        m.points.add(new MapPoint(350, 350));
        m.points.add(new MapPoint(550, 400));
        m.points.add(new MapPoint(450, 450));
        m.points.add(new MapPoint(600, 550));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(60, 230, 57));
        Path2D path = new Path2D.Float();
        path.moveTo(m.points.get(0).x, m.points.get(0).y);
        for (MapPoint p: m.points){
            path.lineTo(p.x, p.y);
        }
        g.draw(path);

        g.setColor(new Color(100, 100, 100));
        for (QuadTree.TreeNode tn: t){
            g.drawRect(tn.bounds[0], tn.bounds[1], tn.bounds[2] - tn.bounds[0], tn.bounds[3] - tn.bounds[1]);
        }
        t.add(m);
        g.setColor(new Color(0, 0, 0));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        ArrayList<MapPoint> marks = new ArrayList<>();
        for (QuadTree.TreeNode tn: t){
        //QuadTree.TreeNode tn = t.nw;
            for (MapShape mapShape: tn.shapes){
                Path2D path2D = new Path2D.Float();
                marks.add(mapShape.points.get(0));
                marks.add(mapShape.points.get(mapShape.points.size()-1));
                path2D.moveTo(mapShape.points.get(0).x, mapShape.points.get(0).y);
                for (MapPoint point: mapShape.points){
                    path2D.lineTo(point.x, point.y);
                }
                g.draw(path2D);
            }
        }
        g.setColor(new Color(255, 0, 0));
        for (MapPoint mp: marks){
            g.fillRect(mp.x-3, mp.y-3, 6, 6);
        }

        ImageIO.write(image, "png", new File("testRoadClip.png"));


    }
}
