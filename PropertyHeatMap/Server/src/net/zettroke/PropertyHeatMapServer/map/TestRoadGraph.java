package net.zettroke.PropertyHeatMapServer.map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.*;

public class TestRoadGraph {
    static double coefficent = 1;
    static Graphics2D g;
    static int max_dist = 6500;

    static int coef(int n){
        return (int)Math.round(n*coefficent);
    }



    public static void test() throws Exception{
        PropertyMap propertyMap = new PropertyMap();
        PropertyMapLoaderOSM.load(propertyMap, "map_small.osm");

        double size = 5000;
        int x_size = (int) size;
        coefficent = size/(propertyMap.x_end-propertyMap.x_begin);
        int y_size = coef(propertyMap.y_end-propertyMap.y_begin);

        //System.out.println(coefficent);

        BufferedImage image = new BufferedImage(x_size, y_size, BufferedImage.TYPE_INT_RGB);

        g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, x_size, y_size);

        g.setColor(Color.BLACK);
        /*Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        long start = System.nanoTime();
        bench(propertyMap);

        System.out.println((System.nanoTime()-start)/1000000000.0 + "sec.");
        scanner.nextLine();*/
        long id = 933754795;
        //Scanner scanner = new Scanner(System.in);
        //scanner.nextLine();
        long start = System.nanoTime();
        HashMap<Long, RoadGraphNode> roadGraph = propertyMap.getCalculatedRoadGraph(933754795, false,  10000);

        //System.out.println((System.nanoTime()-start)/1000000000.0 + "sec. Recursion");
        //scanner.nextLine();"933754783" -> "933754783" ->


        depth_graph_draw(roadGraph.get(id));

        g.setColor(new Color(255, 0, 0));
        g.fillRect(coef(roadGraph.get(id).n.x)-10, coef(roadGraph.get(id).n.y)-10, 20, 20);
        g.drawRect(coef(roadGraph.get(id).n.x)-30, coef(roadGraph.get(id).n.y)-30, 60, 60);

        //g.setPaint(new GradientPaint(1, 1, new Color(0, 255, 0), 1000, 1000, new Color(255, 0, 0), true));
        //g.setStroke(new BasicStroke(50f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        //g.drawLine(500, 500, 1000, 500);
        //g.drawLine(1000, 500, 1000, 2000);




        ImageIO.write(image, "png", new FileOutputStream("testRoadGraph.png"));
    }
    final static int repeats = 20;
    static void bench(PropertyMap propertyMap){
        for (int i=0; i<repeats; i++) {
            HashMap<Long, RoadGraphNode> roadGraph = propertyMap.getCalculatedRoadGraph(933754795, false, 1);
        }

        long time = 0;
        for (int i=0; i<repeats; i++) {
            long start = System.nanoTime();
            HashMap<Long, RoadGraphNode> roadGraph = propertyMap.getCalculatedRoadGraph(933754795, false, 1);
            time += System.nanoTime()-start;
        }
        System.out.println((time/(double)repeats)/1000000.0 + "millis.");
    }


    static void depth_graph_draw(RoadGraphNode n){
        n.visited = true;
        for (int i=0; i<n.ref_to.length; i++){
            RoadGraphNode n1 = n.ref_to[i];
            boolean flag = true;
            g.setPaint(new GradientPaint(coef(n.n.x), coef(n.n.y), getNodeColor(n, max_dist), coef(n1.n.x), coef(n1.n.y), getNodeColor(n1, max_dist)));
            //g.setColor(Color.BLACK);
            if (n.road_types.contains("secondary") && n1.road_types.contains("secondary")){
                g.setStroke(new BasicStroke(15, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                //g.setColor(new Color(247, 250, 190));
            }else if (n.road_types.contains("residential") && n1.road_types.contains("residential")) {
                //g.setColor(new Color(0, 0, 0));
                g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else if(n.road_types.contains("footway") && n1.road_types.contains("footway")){
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                //flag = false;
                //continue;
            //} else if(n.road_types.contains("service") && n1.road_types.contains("service")){
                //flag = false;
            }else{
                //g.setColor(new Color(0, 0, 0));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            if (flag) {
                g.drawLine(coef(n.n.x), coef(n.n.y), coef(n1.n.x), coef(n1.n.y));
            }
            if (!n1.visited) {
                depth_graph_draw(n1);
            }
        }
    }
    static Color getNodeColor(RoadGraphNode n, int max_dist){
        if (n.dist <= max_dist) {

            /*int r = (int) (255 * n.dist / (double) max_dist);
            int g = 255 - (int) (255 * (n.dist / (double) max_dist));
            return new Color(r, g, 0);*/
            double c = 1-n.dist/(double)max_dist;
            return Color.getHSBColor((float)((1-c)*120.0/360.0), 0.9f, 0.9f);

        }else{
            return new Color(168, 0, 22);
        }
    }
}


