package net.zettroke.PropertyHeatMapServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.Scanner;
//-XX:+UnlockCommercialFeatures -XX:+FlightRecorder

public class Main {

    public static void main(String[] args) throws Exception{
        /*Scanner sc = new Scanner(System.in);
        System.out.println(sc.nextLine());
        long start = System.nanoTime();
        PropertyMap m = new PropertyMap();
        PropertyMapLoaderOSM.load(m, "C:/PropertyHeatMap/map.osm");
        System.out.println("done in " + (System.nanoTime()-start)/1000000000.0 + " sec.");
        System.out.println(sc.nextLine());*/

        /*BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();


        MapPoint p1 = new MapPoint(600, 100);
        MapPoint p2 = new MapPoint(600, 750);
        MapPoint p3 = new MapPoint(540, 600);
        MapPoint p4 = new MapPoint(700, 430);
        g.fillRect(0, 0, 1000, 1000);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(5f));
        g.setColor(new Color(240, 0, 0));
        g.drawLine(p1.x, p1.y, p2.x, p2.y);

        g.setColor(new Color(0, 0, 0));
        g.drawLine(p3.x, p3.y, p4.x, p4.y);

        int[] res = QuadTree.VertCross(p1.x, p1.y, p2.y, p3, p4);
        if (res != null){
            System.out.println(res[0]+" "+ res[1]);
            g.setColor(new Color(0, 200, 48));
            int s = 15;
            g.fillRect(res[0]-s/2, res[1]-s/2, s, s);
        }

        ImageIO.write(img, "png", new FileOutputStream("test.png"));*/

        TestRoadClipping.test();


    }
}
