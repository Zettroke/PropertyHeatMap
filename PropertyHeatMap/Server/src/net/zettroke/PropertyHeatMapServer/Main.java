package net.zettroke.PropertyHeatMapServer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.PrettyPrint;
import com.eclipsesource.json.WriterConfig;
import net.zettroke.PropertyHeatMapServer.map.*;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.*;
/*import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;*/

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;


public class Main {

    static HashMap<Long, MapPoint> map = new HashMap<>();
    static PropertyMap propertyMap;
    static ArrayList<Polygon> mapPoly = new ArrayList<>();
    static double coefficent = 1;
    static int x;
    static int y;

    static int coef(int n){
        return (int)Math.round(n*coefficent);
    }
    public static void rec(int[] max_shapes, int[] sum_shapes, int[] count, QuadTreeNode treeNode){
        if (treeNode.isEndNode){
            if (treeNode.shapes.size() > max_shapes[0]) {
                max_shapes[0] = treeNode.shapes.size();
            }
            sum_shapes[0] += treeNode.shapes.size();
            count[0]++;
        }else{
            for (QuadTreeNode tn: treeNode){
                rec(max_shapes, sum_shapes, count, tn);
            }
        }
    }
    public static void main(String[] args) throws Exception{
        String map_name = new Scanner(new FileInputStream("current_map_file.conf")).nextLine();
        ImageIO.setUseCache(false);

        /*Scanner scanner = new Scanner(System.in);
        scanner.nextLine();*/
        //System.out.println(ClassLayout.parseClass(int[].class).toPrintable());
        if (args.length != 0){
            for (String s: args){
                if (s.contains("-draw=")){
                    if (s.substring(6).equals("native")){
                        RoadGraphDrawer.isGlobalSet = true;
                        RoadGraphDrawer.isNative = true;
                        //System.out.println("set drawer to native");
                    }else if(s.substring(6).equals("java")){
                        RoadGraphDrawer.isGlobalSet = true;
                        RoadGraphDrawer.isNative = false;
                        //System.out.println("set drawer to java");
                    }
                }
            }
        }

        PropertyMapServer server = new PropertyMapServer(map_name);
        server.start();


        //TestPolygonClipping.test();

        /*BufferedImage img = new BufferedImage(2500, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        int w = 2300;
        float target_low_b = 0.7f;
        for (int i=0; i<img.getWidth(); i++){
            if (i <= w) {
                float b = 1;
                float hue = (float) Math.pow((1 - (i / (double) w)), 0.7);
                if (hue < 0.15) {
                    b = target_low_b + hue * (1-target_low_b)/0.15f;
                }
                Color c = Color.getHSBColor(hue * (120f / 360f), 1, b);
                g.setColor(c);
            }else{
                g.setColor(Color.getHSBColor(0, 1, target_low_b));
            }
            g.drawRect(i, 0, 1, img.getHeight());
        }
        g.setColor(Color.BLACK);
        g.drawRect(w, img.getHeight()-1, 1, 1);


        ImageIO.write(img, "png", new File("gradientRoad.png"));

        //System.out.println(Math.round(min_price) + " " + Math.round(max_price));


        /*System.out.println(VM.current().details());
        System.out.println(ClassLayout.parseClass(RoadGraphNode.class).toPrintable());*/
        /*PropertyMap propertyMap = new PropertyMap(new PropertyMapLoaderOSM(map_name));
        propertyMap.init();
        Scanner scanner = new Scanner(System.in);

        System.out.println(propertyMap.roadGraph.size());
        scanner.nextLine();*/
        //ClassLayout gl = ClassLayout.parseInstance(propertyMap.roadGraph.values().iterator().next());
        //System.out.println(gl.toPrintable());
        //System.out.println(gl.totalSize());

        /*StringPredictor pred = propertyMap.predictor;
        pred.add("Zettroke", null);
        pred.add("Olleggerr", null);
        pred.add("Zettroker", null);
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        while (!s.equals("end")){
            String[] req = s.split(" ", 2);
            if (req[0].equals("1")){
                pred.add(req[1], null);
                System.out.println("Added " + '"' + req[1] + '"');
            }else if(req[0].equals("2")){

                ArrayList<String> strs = pred.predict(req.length != 1 ? req[1]:"", pred.size());
                System.out.println("Predicts(" + strs.size()+"):");
                for (String str: strs){
                    System.out.println("    " + str);
                }
            }
            s = sc.nextLine();
        }*/

        //TestPolygonClipping.test();



        //TestPolygonClipping.test();

        long start = System.nanoTime();

        PropertyMap propertyMap = new PropertyMap(new MapLoaderOSM(map_name));
        //PropertyMapLoaderOSM.load(propertyMap, new File(map_name));
        propertyMap.init();
        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        int[] max_shapes = new int[1];
        int[] sum_shapes = new int[1];
        int[] count_shapes = new int[1];

        rec(max_shapes, sum_shapes, count_shapes, propertyMap.tree.root);

        System.out.println("Max num of shapes in node " + max_shapes[0] + ". Average num of shapes is " + sum_shapes[0]/(double)count_shapes[0]);

        //scanner.nextLine();


        double size = 25000;
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
        g.setColor(new Color(7, 228, 0, 150));
        g.setStroke(new BasicStroke(8f));
        for (int[] arr: rects){
            g.drawRect(arr[0], arr[1], arr[2], arr[3]);
        }
        g.setColor(new Color(255, 0, 0));
        //for (MapPoint p: propertyMap.lost_price){
        //    g.fillRect(coef(p.x)-4, coef(p.y)-4, 8, 8);
        //    g.drawRect(coef(p.x)-16, coef(p.y)-16, 32, 32);
        //}

        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.7f);

        ImageOutputStream outputStream = new FileImageOutputStream(new File("QuadTreeSubdivision1.jpg")); // For example implementations see below
        jpgWriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(image, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);
        jpgWriter.dispose();
        /*ImageIO.write(image, "jpg", new FileOutputStream("QuadTreeSubdivision.jpg"));
        System.out.println((System.nanoTime()-start)/1000000.0 + " millis.");*/

        //TestPolyContain.test();

    }


    static void draw(Graphics2D g, QuadTreeNode t, ArrayList<int[]> rects){

        if (!t.isEndNode){
            for (QuadTreeNode tn: t){
                draw(g, tn, rects);
            }
        }else{

            rects.add(new int[]{coef(t.bounds[0]), coef(t.bounds[1]), coef(t.bounds[2]-t.bounds[0]), coef(t.bounds[3]-t.bounds[1])});
            //g.setColor(new Color(255, 238, 192));
            g.setColor(new Color(0, 0, 0));
            for (MapShape mh: t.shapes){
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
                } else{// if (!(mh.way.data.get("highway").equals("footway") || mh.way.data.get("highway").equals("path"))){
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

    /*static int recursive_count(QuadTree.TreeNode t){
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
    }*/
}
