package net.zettroke.PropertyHeatMapServer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;

import net.zettroke.PropertyHeatMapServer.map.*;


//-XX:+UnlockCommercialFeatures -XX:+FlightRecorder

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
        //ab -n 5000 -c 8 "http://localhost:24062/draw?text=Zettroke"
        //ab -n 50000 -c 8 "http://localhost:24062/search/?x=946&y=205&z=16"
        PropertyMapServer server = new PropertyMapServer();
        server.start();
        /*Scanner scanner = new Scanner(System.in);

        propertyMap = new PropertyMap();
        PropertyMapLoaderOSM.load(propertyMap, "map_small.osm");
        long start = System.nanoTime();
        propertyMap.initParallel();

        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");*/
        /*String s = scanner.nextLine();
        JSONParser parser = new JSONParser();
        while (!s.equals("stop")){
            JSONObject jsonObject = (JSONObject) parser.parse(s);
            JSONObject answer = new JSONObject();

            int z = ((Long) jsonObject.get("z")).intValue();

            int mult = (int)Math.pow(2, PropertyMap.default_zoom - z);

            int x = mult*((Long)jsonObject.get("x")).intValue();
            int y = mult*((Long)jsonObject.get("y")).intValue();

            Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
            if (w != null){
                answer.put("status", "success");
                answer.put("data", w.data);
                answer.put("id", w.id);
                answer.put("zoom_level", PropertyMap.default_zoom);
                JSONArray points = new JSONArray();
                for (MapPoint p: w.nodes){
                    JSONArray point = new JSONArray(); point.add(p.x); point.add(p.y);
                    points.add(point);
                }
                answer.put("points", points);
            }else{
                answer.put("status", "not found");
            }
            System.out.println(answer.toString());
            s = scanner.nextLine();
        }*/


        //scanner.nextLine();


        //double size = 30000;
        //int x_size = (int) size;
        //coefficent = size/(propertyMap.x_end-propertyMap.x_begin);
        //int y_size = coef(propertyMap.y_end-propertyMap.y_begin);

        //{"x": 929, "y": 819, "z": 16}
        //

        /*int z = 16;
        int mult = (int)Math.pow(2, PropertyMap.default_zoom - z);
        int x = mult*929;
        int y = mult*819;

        int x_size = propertyMap.x_end-propertyMap.x_begin;
        int y_size = propertyMap.y_end-propertyMap.y_begin;

        BufferedImage image = new BufferedImage(x_size, y_size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, x_size, y_size);
        g.setColor(new Color(0, 0, 0));
        draw(g, propertyMap.tree.root);

        g.setColor(new Color(255, 0, 0));
        g.fillRect(x-100, y-100, 200, 200);
        ImageIO.write(image, "png", new FileOutputStream("QuadTreeSubdivision.png"));*/


        //TestPolyContain.test();

    }

    /*static void draw(Graphics2D g, QuadTreeNode t){
        if (!t.isEndNode){
            for (QuadTree.TreeNode tn: t){
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
                    g.setStroke(new BasicStroke(3f));
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
                } else if (!(mh.way.data.get("highway").equals("footway") || mh.way.data.get("highway").equals("path"))){
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
    }*/
}
