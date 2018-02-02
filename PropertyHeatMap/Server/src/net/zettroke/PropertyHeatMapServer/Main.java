package net.zettroke.PropertyHeatMapServer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.PrettyPrint;
import com.eclipsesource.json.WriterConfig;
import net.zettroke.PropertyHeatMapServer.map.*;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.RoadGraphDrawer;
import net.zettroke.PropertyHeatMapServer.utils.StringPredictor;
import net.zettroke.PropertyHeatMapServer.utils.TimeMeasurer;
/*import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;*/

import javax.imageio.ImageIO;


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

    public static void main(String[] args) throws Exception{
        String map_name = new Scanner(new FileInputStream("current_map_file.conf")).nextLine();
        ImageIO.setUseCache(false);
        //ab -n 5000 -c 8 "http://localhost:24062/draw?text=Zettroke"
        //ab -n 5000 -c 4 "http://127.0.0.1:24062/search/circle/?x=730&y=1432&z=14&r=551"
        //ab -n 50000 -c 8 "http://192.168.1.150:24062/search/circle/?x=300&y=300&r=200&z=16"
        //ab -n 50000 -c 8 "http://178.140.109.241:24062/search/circle/?x=1769&y=203&z=16&r=280"
        //ab -n 5000 -c 8 "http://178.140.109.241:24062/tile?x=6&y=0&z=16"
        /*Scanner scanner = new Scanner(System.in);
        scanner.nextLine();*/
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
        /*BufferedImage img = new BufferedImage(1280, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();

        for (int i=0; i<img.getWidth(); i++){
            g.setColor(Color.getHSBColor((i/(float)img.getWidth())*(240f/360f), 1, 1));
            g.drawRect(i, 0, 1, img.getHeight());
        }
        BufferedReader reader = new BufferedReader(new FileReader("data_json"));
        String line = reader.readLine();
        int size = 100;
        double[] min_price = new double[size];
        double[] max_price = new double[size];
        for (int i=0; i<size; i++){
            min_price[i] = Double.MAX_VALUE;
            max_price[i] = 0;
        }
        int cnt = 0;
        while (line != null) {
            try {
                JsonObject jsonObject = Json.parse(line).asObject();

                double area = Double.valueOf(jsonObject.get("Общая площадь").asString().replace("м2", "").replace(',', '.'));
                long pricel = jsonObject.get("Цена").asLong();
                double price = pricel/area;
                for (int i=0; i<size; i++) {
                    if (price < min_price[i]){
                        min_price[i] = price;
                        break;
                    }
                }

                for (int i=0; i<size; i++) {
                    if (price > max_price[i]){
                        max_price[i] = price;
                        break;
                    }
                }
                cnt++;
            }catch (Exception e){}
            line = reader.readLine();
        }
        ImageIO.write(img, "png", new File("gradient.png"));
        */
        //System.out.println(Math.round(min_price) + " " + Math.round(max_price));

        /*long start = System.nanoTime();
        for (int i=0; i<10000; i++){
            TestJNI.call(i);
        }
        System.out.println("1 call in " + (System.nanoTime()-start)/1000.0/10000.0 + " microseconds.");

        start = System.nanoTime();
        for (int i=0; i<10000; i++){
            TestJNI.call(i);
        }
        System.out.println("1 call in " + (System.nanoTime()-start)/1000.0/10000.0 + " microseconds.");*/

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

       /* long start = System.nanoTime();

        PropertyMap propertyMap = new PropertyMap(new PropertyMapLoaderOSM(map_name));
        //PropertyMapLoaderOSM.load(propertyMap, new File(map_name));
        propertyMap.init();
        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");


        //scanner.nextLine();


        double size = 15000;
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
        draw(g, propertyMap.tree.root);

        g.setColor(new Color(255, 0, 0));
        //for (MapPoint p: propertyMap.lost_price){
        //    g.fillRect(coef(p.x)-4, coef(p.y)-4, 8, 8);
        //    g.drawRect(coef(p.x)-16, coef(p.y)-16, 32, 32);
        //}

        ImageIO.write(image, "png", new FileOutputStream("QuadTreeSubdivision.png"));*/


        //TestPolyContain.test();

    }

    static void draw(Graphics2D g, QuadTreeNode t){
        if (!t.isEndNode){
            for (QuadTreeNode tn: t){
                draw(g, tn);
            }
        }else{
            g.setColor(new Color(7, 228, 0, 75));
            g.setStroke(new BasicStroke(8f));
            g.drawRect(coef(t.bounds[0]), coef(t.bounds[1]), coef(t.bounds[2]-t.bounds[0]), coef(t.bounds[3]-t.bounds[1]));
            //g.setColor(new Color(255, 238, 192));
            g.setColor(new Color(0, 0, 0));
            for (MapShape mh: t.shapes){
                if (mh.isPoly && mh.way.data.containsKey("building")) {
                    g.setStroke(new BasicStroke(1.5f));
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

                    g.setStroke(new BasicStroke(6f));
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
