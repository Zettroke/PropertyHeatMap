package net.zettroke.PropertyHeatMapServer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import net.zettroke.PropertyHeatMapServer.map.*;
import net.zettroke.PropertyHeatMapServer.utils.*;

import javax.imageio.ImageIO;


public class Main {

    static HashMap<Long, MapPoint> map = new HashMap<>();
    static PropertyMap propertyMap;
    static ArrayList<Polygon> mapPoly = new ArrayList<>();
    static double coefficent = 1;
    static int x;
    static int y;


    static int coef(int n) {
        return (int) Math.round(n * coefficent);
    }

    public static void rec(int[] max_shapes, int[] sum_shapes, int[] count, QuadTreeNode treeNode) {
        if (treeNode.isEndNode) {
            if (treeNode.shapes.size() > max_shapes[0]) {
                max_shapes[0] = treeNode.shapes.size();
            }
            sum_shapes[0] += treeNode.shapes.size();
            count[0]++;
        } else {
            for (QuadTreeNode tn : treeNode) {
                rec(max_shapes, sum_shapes, count, tn);
            }
        }
    }

    static int cf(int n, int off, double cf){
        return (int)Math.round((n-off)*cf);
    }

    public static void main(String[] args) throws Exception {
        String map_name = new Scanner(new FileInputStream("current_map_file.conf")).nextLine();
        ImageIO.setUseCache(false);
        //Drawer.forceCairo = true;
        if (args.length != 0) {
            for (String s : args) {
                if (s.contains("-draw=")) {
                    if (s.substring(6).equals("native")) {
                        Drawer.isGlobalSet = true;
                        Drawer.isNative = true;
                        //System.out.println("set drawer to native");
                    } else if (s.substring(6).equals("java")) {
                        Drawer.isGlobalSet = true;
                        Drawer.isNative = false;
                        //System.out.println("set drawer to java");
                    }
                }
            }
        }

        PropertyMap map = new PropertyMap();
        map.addMapLoader(new MapLoaderOSM(map_name));
        PropertyMapServer server = new PropertyMapServer(map);
        server.start();


        /*long start = System.nanoTime();

        PropertyMap propertyMap = new PropertyMap(new MapLoaderOSM(map_name));
        propertyMap.init();
        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        int[] max_shapes = new int[1];
        int[] sum_shapes = new int[1];
        int[] count_shapes = new int[1];

        rec(max_shapes, sum_shapes, count_shapes, propertyMap.tree.root);

        System.out.println("Max num of shapes in node " + max_shapes[0] + ". Average num of shapes is " + sum_shapes[0]/(double)count_shapes[0]);



        double size = 5000;
        int x_size = (int) size;
        coefficent = size/(propertyMap.x_end-propertyMap.x_begin);
        int y_size = coef(propertyMap.y_end-propertyMap.y_begin);

        System.out.println(coefficent);

        BufferedImage image = new BufferedImage(x_size, y_size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, x_size, y_size);
        g.setColor(new Color(0, 0, 0));
        ArrayList<int[]> rects = new ArrayList<>();
        draw(g, propertyMap.tree.root, rects);
        g.setColor(new Color(235, 6, 0));
        g.setStroke(new BasicStroke(16f));
        for (int[] arr: rects){
            g.drawRect(arr[0], arr[1], arr[2], arr[3]);
        }
        g.setColor(new Color(255, 0, 0));
        //for (MapPoint p: propertyMap.lost_price){
        //    g.fillRect(coef(p.x)-4, coef(p.y)-4, 8, 8);
        //    g.drawRect(coef(p.x)-16, coef(p.y)-16, 32, 32);
        //}

        //ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        //ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        //jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        //jpgWriteParam.setCompressionQuality(0.9f);

        //ImageOutputStream outputStream = new FileImageOutputStream(new File("QuadTreeSubdivision1.jpg")); // For example implementations see below
        //jpgWriter.setOutput(outputStream);
        //IIOImage outputImage = new IIOImage(image, null, null);
        //jpgWriter.write(null, outputImage, jpgWriteParam);
        //jpgWriter.dispose();

        ImageIO.write(image, "png", new File("QuadTreeSubdivision.png"));*/



    }

    static void drawTransport(Way w, Relation r, int min_x, int max_x, int min_y, int max_y, double cfc, int off, int ind){
        BufferedImage image = new BufferedImage((int)Math.round((max_x-min_x)*cfc)+off*2, (int)Math.round((max_y-min_y)*cfc)+off*2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, max_x-min_x, max_y-min_y);
        Path2D path = new Path2D.Float();
        g.setColor(new Color(255, 0, 0));
        for (Node n: r.nodes){
            int x = cf(n.x, min_x, cfc)+off;
            int y = cf(n.y, min_y, cfc)+off;
            g.fillOval(x-23, y-23, 46, 46);
        }
        g.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 0, 0));
        path.moveTo(cf(w.nodes.get(0).x, min_x, cfc)+off, cf(w.nodes.get(0).y , min_y, cfc)+off);
        ArrayList<MapPoint> pts = new ArrayList<>();
        int i = 0;
        for (SimpleNode n: w.nodes){
            pts.add(new MapPoint(cf(n.x, min_x, cfc)+off, cf(n.y, min_y, cfc)+off));
            path.lineTo(cf(n.x, min_x, cfc)+off, cf(n.y, min_y, cfc)+off);
            //g.drawString("" + i++, cf(n.x, min_x, cfc)+off-30, cf(n.y, min_y, cfc)+off+50);
        }
        g.draw(path);


        g.setColor(new Color(0 ,0, 0));
        i = 0;
        for (Node n: r.nodes){
            int x = cf(n.x, min_x, cfc)+off;
            int y = cf(n.y, min_y, cfc)+off;
            //g.drawString(""+i++, x, y);
        }
        /*g.setColor(new Color(0, 235, 0));
        for (MapPoint p: pts){
            g.drawRect(p.x-3, p.y-3, 6, 6);
        }*/

        try{
        ImageIO.write(image, "png", new File(String.format("Lel%d.png", ind)));}catch (Exception e){e.printStackTrace();}
    }

    static void draw(Graphics2D g, QuadTreeNode t, ArrayList<int[]> rects) {

        if (!t.isEndNode) {
            for (QuadTreeNode tn : t) {
                draw(g, tn, rects);
            }
        } else {

            rects.add(new int[]{coef(t.bounds[0]), coef(t.bounds[1]), coef(t.bounds[2] - t.bounds[0]), coef(t.bounds[3] - t.bounds[1])});
            //g.setColor(new Color(255, 238, 192));
            g.setColor(new Color(0, 0, 0));
            for (MapShape mh : t.shapes) {
                if (mh.isPoly && mh.way.data.containsKey("building")) {
                    g.setStroke(new BasicStroke(1f));
                    Polygon poly = new Polygon();
                    for (MapPoint p : mh.points) {
                        poly.addPoint(coef(p.x), coef(p.y));
                    }
                    if (mh.way.data.containsKey("building")) {
                        g.setColor(new Color(255, 238, 192));
                        g.fill(poly);
                    }
                    g.setColor(new Color(0, 0, 0));
                    g.draw(poly);
                } else {// if (!(mh.way.data.get("highway").equals("footway") || mh.way.data.get("highway").equals("path"))){
                    g.setColor(new Color(0, 0, 0));

                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D path2D = new Path2D.Float();
                    path2D.moveTo(coef(mh.points.get(0).x), coef(mh.points.get(0).y));
                    for (MapPoint p : mh.points) {
                        path2D.lineTo(coef(p.x), coef(p.y));
                    }

                    g.draw(path2D);
                }

            }
        }
    }
    static int cnt = 0;
    public static void drawQuadTreeNode(QuadTreeNode n){
        try {
            double size = 1280;
            int x_size = (int) size;
            coefficent = size / (n.bounds[2] - n.bounds[0]);
            int y_size = coef(n.bounds[3] - n.bounds[1]);
            y_size+=y_size&1;

            //System.out.println(coefficent);

            BufferedImage image = new BufferedImage(x_size, y_size, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = (Graphics2D) image.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255));
            g.fillRect(0, 0, x_size, y_size);
            g.setColor(new Color(0, 0, 0));
            ArrayList<int[]> rects = new ArrayList<>();
            draw(g, n, rects);
            g.setColor(new Color(235, 6, 0, 130));
            g.setStroke(new BasicStroke(4.5f));
            for (int[] arr : rects) {
                g.drawRect(arr[0], arr[1], arr[2], arr[3]);
            }
            g.setColor(new Color(255, 0, 0));


            ImageIO.write(image, "bmp", new File(String.format("QuadTreeAnimation/QuadTreeSubdivision%05d.bmp", ++cnt)));
        }catch (Exception e) {

        }
    }
}
