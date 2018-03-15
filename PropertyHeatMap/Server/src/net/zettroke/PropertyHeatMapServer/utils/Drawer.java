package net.zettroke.PropertyHeatMapServer.utils;

import net.zettroke.PropertyHeatMapServer.PropertyMapServer;
import net.zettroke.PropertyHeatMapServer.map.*;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphLine;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;

public class Drawer {

    public static boolean isGlobalSet = false;
    public static boolean isNative;

    public static boolean test = false;


    private final boolean isCairoAvailable;
    private final boolean isOpenGLAvailable;
    final static int zoom_level = 13;
    static Drawer instance = null;

    private Drawer(){
        boolean tempOpenGL, tempCairo;
        if (!isGlobalSet || isNative) {

            try{
                System.loadLibrary("JNI-OpenGL-Drawer");
                System.loadLibrary("JNI-CairoDrawer");
                initOpenGLRenderer(PropertyMapServer.PROC_NUM);
                // OpenGL всегда фейлит первую картинку. Почему не знаю. Решено отрисовкой одной картинки.
                /*int mx = 0;
                for (RoadGraphNode rgn: PropertyMap.init_node.roadGraphNodes){
                    mx = Math.max(rgn.index, mx);
                }
                drawNative(PropertyMap.init_node, 0, 0, 11, 8, 1, 18000, 0, mx+2);*/
                tempOpenGL = true;

            }catch (UnsatisfiedLinkError e){
                tempOpenGL = false;
                System.out.println("OpenGl draw not available");
            }
            if (!tempOpenGL) {
                try {
                    System.loadLibrary("JNI-CairoDrawer");

                    tempCairo = true;
                } catch (UnsatisfiedLinkError e) {
                    System.out.println("Cairo draw not available");
                    tempCairo = false;
                    System.out.println("Все в порядке, cairo недоступен, поэтому будет использован рендер на java. Для повторной попытки загрузить cairo перезапустите сервер.");
                    System.out.println("It's okay. Cairo not available, so using java render. If you want to try load cairo again restart server.");
                }
            }else{
                tempCairo = false;
            }
            isCairoAvailable = tempCairo;
            isOpenGLAvailable = tempOpenGL;
        }else{
            isCairoAvailable =  false;
            isOpenGLAvailable = false;
        }
    }

    public synchronized static Drawer getInstance() {
        if (instance == null) {
            instance = new Drawer();
        }
        return instance;

    }
    //[x1, y1, color1, x2, y2, color2, width] - Graph
    //[x1, y1, x2, y2, x3, y3 .... xn, yn, color1, x1, y2 ...] - Building
    public native byte[] drawGraphCairoCall(int[] data, int len, int divider);
    public native byte[] drawBuildingCairoCall(int[] data, int len, int[] polyLens, int len2, int divider);

    public native void initOpenGLRenderer(int num_process);

    public native byte[] drawGraphOpenGLCall(int[] data, int len, int divider, int zoom_level, int process_num, int max_dist);
    public native byte[] drawBuildingOpenGLCall(int[] data, int len, int[] polyLens, int len2, int divider, int zoom_level, int process_num);

    private byte[] drawGraphNativeCairo(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind){
        IntArrayList bundle = new IntArrayList(treeNode.roadGraphLines.size());
        int th_ind = ((IndexedThread)Thread.currentThread()).index;
        int secondary_stroke =      Math.round(65f/mult*100);
        int primary_stroke =        Math.round(75f/mult*100);
        int tertiary_stroke =       Math.round(60f/mult*100);
        int service_stroke =        Math.round(25f/mult*100);
        int residential_stroke =    Math.round(50f/mult*100);
        int living_stroke =         Math.round(25f/mult*100);
        int default_stroke =        Math.round(20f/mult*100);
        int unknown_stroke =        Math.round(10f/mult*100);

        int offx = x*mult*256;
        int offy = y*mult*256;
        boolean dont_draw = false;
        int stroke = 100;
        HashSet<RoadType> excl = mode == 0 ? RoadGraphNode.foot_exclude : RoadGraphNode.car_exclude;
        for (RoadGraphLine rgl: treeNode.roadGraphLines){
            if (!excl.contains(rgl.type)) {
                switch (rgl.type) {
                    case SECONDARY:
                        if (z > zoom_level) {
                            stroke = secondary_stroke;
                        } else {
                            stroke = Math.round(160f / mult * 100);
                        }
                        break;
                    case RESIDENTIAL:
                        if (z > zoom_level) {
                            stroke = residential_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SERVICE:
                        if (z > zoom_level) {
                            stroke = service_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case TERTIARY:
                        if (z > zoom_level) {
                            stroke = tertiary_stroke;
                        } else {
                            stroke = tertiary_stroke;
                            //dont_draw = true;
                        }
                        break;
                    case PRIMARY:
                        if (z > zoom_level) {
                            stroke = primary_stroke;
                        } else {
                            stroke = Math.round(160f / mult * 100);
                        }
                        break;
                    case TRUNK:
                        stroke = Math.round(160f / mult * 100);
                        break;
                    case DEFAULT:
                        if (z > zoom_level) {
                            stroke = default_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case LIVING_STREET:
                        if (z > zoom_level) {
                            stroke = living_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SUBWAY:
                        dont_draw = true;
                        break;
                    case TRAM:
                        dont_draw = true;
                        break;
                    case BUS:
                        dont_draw = true;
                        break;
                    case TROLLEYBUS:
                        dont_draw = true;
                        break;
                    case INVISIBLE:
                        dont_draw = true;
                        break;
                    default:
                        if (z > zoom_level) {
                            stroke = unknown_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                }
                if (!dont_draw) {
                    bundle.addAll(rgl.n1.n.x - offx, rgl.n1.n.y - offy, rgl.n1.getNodeColor(max_dist, ind).getRGB(),
                            rgl.n2.n.x - offx, rgl.n2.n.y - offy, rgl.n2.getNodeColor(max_dist, ind).getRGB(), stroke);
                    //lines++;
                } else {
                    dont_draw = false;
                }
            }
        }
        return drawGraphCairoCall(bundle.toArray(), bundle.size(), mult);

    }


    private byte[] drawBuildingNativeCairo(QuadTreeNode treeNode, int x, int y, int z, int price, double range){

        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int offx = x*mult*256;
        int offy = y*mult*256;
        double dist = price*range*2;
        IntArrayList points = new IntArrayList();
        IntArrayList polyLens = new IntArrayList();

        for (MapShape mh: treeNode.shapes) {
            if (mh.isPoly && mh.way.data.containsKey("building")) {
                int poly_color = 0;

                if (mh.way.apartments != null) {
                    double ap_price = -1000000000000000000000.0;
                    for (Apartment ap : mh.way.apartments) {
                        if (Math.abs(price - ap_price) > Math.abs(price - ap.price / ap.area)) {
                            ap_price = ap.price / ap.area;
                        }
                    }
                    if (ap_price > price - price * range && ap_price < price + price * range) {
                        float color = (float) ((((price + price * range) - ap_price) / dist) * (24.0 / 36.0));
                        poly_color = Color.getHSBColor(color, 1.0f, 0.95f).getRGB();
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                polyLens.add(mh.points.size());

                for (MapPoint p : mh.points) {
                    points.addAll(p.x - offx, p.y - offy);
                }
                points.add(poly_color);

            }
        }

        return drawBuildingCairoCall(points.toArray(), points.size(), polyLens.toArray(), polyLens.size(), mult);
    }

    private byte[] drawGraphNativeOpenGL(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind){

        IntArrayList bundle = new IntArrayList(treeNode.roadGraphLines.size());
        int th_ind = ((IndexedThread)Thread.currentThread()).index;
        int secondary_stroke =      Math.round(65f/mult*100);
        int primary_stroke =        Math.round(75f/mult*100);
        int tertiary_stroke =       Math.round(60f/mult*100);
        int service_stroke =        Math.round(25f/mult*100);
        int residential_stroke =    Math.round(50f/mult*100);
        int living_stroke =         Math.round(25f/mult*100);
        int default_stroke =        Math.round(20f/mult*100);
        int unknown_stroke =        Math.round(10f/mult*100);

        int offx = x*mult*256;
        int offy = y*mult*256;
        boolean dont_draw = false;
        int stroke = 100;
        HashSet<RoadType> excl = mode == 0 ? RoadGraphNode.foot_exclude : RoadGraphNode.car_exclude;
        for (RoadGraphLine rgl: treeNode.roadGraphLines){
            if (!excl.contains(rgl.type)) {
                switch (rgl.type) {
                    case SECONDARY:
                        if (z > zoom_level) {
                            stroke = secondary_stroke;
                        } else {
                            stroke = Math.round(160f / mult * 100);
                        }
                        break;
                    case RESIDENTIAL:
                        if (z > zoom_level) {
                            stroke = residential_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SERVICE:
                        if (z > zoom_level) {
                            stroke = service_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case TERTIARY:
                        if (z > zoom_level) {
                            stroke = tertiary_stroke;
                        } else {
                            stroke = tertiary_stroke;
                            //dont_draw = true;
                        }
                        break;
                    case PRIMARY:
                        if (z > zoom_level) {
                            stroke = primary_stroke;
                        } else {
                            stroke = Math.round(160f / mult * 100);
                        }
                        break;
                    case TRUNK:
                        stroke = Math.round(160f / mult * 100);
                        break;
                    case DEFAULT:
                        if (z > zoom_level) {
                            stroke = default_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case LIVING_STREET:
                        if (z > zoom_level) {
                            stroke = living_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SUBWAY:
                        dont_draw = true;
                        break;
                    case TRAM:
                        dont_draw = true;
                        break;
                    case BUS:
                        dont_draw = true;
                        break;
                    case TROLLEYBUS:
                        dont_draw = true;
                        break;
                    case INVISIBLE:
                        dont_draw = true;
                        break;
                    default:
                        if (z > zoom_level) {
                            stroke = unknown_stroke;
                        } else {
                            dont_draw = true;
                        }
                        break;
                }
                if (!dont_draw) {
                    bundle.addAll(rgl.n1.n.x - offx, rgl.n1.n.y - offy, rgl.n1.dist[ind],
                            rgl.n2.n.x - offx, rgl.n2.n.y - offy, rgl.n2.dist[ind], stroke);
                    //lines++;
                } else {
                    dont_draw = false;
                }
            }
        }

        return drawGraphOpenGLCall(bundle.toArray(), bundle.size(), mult, z, th_ind, max_dist);
    }

    public static int coef(int n, double cf){
        return (int)Math.round(cf*n);
    }

    private byte[] drawGraphJava(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind, int roadGraphNodeNum) throws IOException{
        BufferedImage imageBackground = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gBackground = (Graphics2D) imageBackground.getGraphics();
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        BasicStroke secondary_stroke = new BasicStroke(65f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke primary_stroke = new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke tertiary_stroke = new BasicStroke(60f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke service_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke residential_stroke = new BasicStroke(50f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke living_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke default_stroke = new BasicStroke(20f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke unknown_stroke = new BasicStroke(10f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        double cf = 1.0/mult;
        int offx = x*mult*256;
        int offy = y*mult*256;
        boolean dont_draw = false;
        int lines = 0;
        HashSet<RoadType> excl = mode == 0 ? RoadGraphNode.foot_exclude : RoadGraphNode.car_exclude;
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphLine rgl: treeNode.roadGraphLines){
            if (!excl.contains(rgl.type)) {
                switch (rgl.type) {
                    case SECONDARY:
                        if (z > zoom_level) {
                            g.setStroke(secondary_stroke);
                        } else {
                            g.setStroke(new BasicStroke(120f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        }
                        break;
                    case RESIDENTIAL:
                        if (z > zoom_level) {
                            g.setStroke(residential_stroke);
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SERVICE:
                        if (z > zoom_level) {
                            g.setStroke(service_stroke);
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case TERTIARY:
                        if (z > zoom_level) {
                            g.setStroke(tertiary_stroke);
                        } else {
                            g.setStroke(tertiary_stroke);
                            //dont_draw = true;
                        }
                        break;
                    case PRIMARY:
                        if (z > zoom_level) {
                            g.setStroke(primary_stroke);
                        } else {
                            g.setStroke(new BasicStroke(120f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        }
                        break;
                    case TRUNK:
                        g.setStroke(new BasicStroke(80f / mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        break;
                    case DEFAULT:
                        if (z > zoom_level) {
                            g.setStroke(default_stroke);
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case LIVING_STREET:
                        if (z > zoom_level) {
                            g.setStroke(living_stroke);
                        } else {
                            dont_draw = true;
                        }
                        break;
                    case SUBWAY:
                        dont_draw = true;
                        break;
                    case TRAM:
                        dont_draw = true;
                        break;
                    case BUS:
                        dont_draw = true;
                        break;
                    case TROLLEYBUS:
                        dont_draw = true;
                        break;
                    case INVISIBLE:
                        dont_draw = true;
                        break;
                    default:
                        if (z > zoom_level) {
                            g.setStroke(unknown_stroke);
                        } else {
                            dont_draw = true;
                        }
                        break;
                }
                if (!dont_draw) {

                    g.setPaint(new GradientPaint(coef(rgl.n1.n.x - offx, cf), coef(rgl.n1.n.y - offy, cf), rgl.n1.getNodeColor(max_dist, ind),
                            coef(rgl.n2.n.x - offx, cf), coef(rgl.n2.n.y - offy, cf), rgl.n2.getNodeColor(max_dist, ind)));
                    g.drawLine(coef(rgl.n1.n.x - offx, cf), coef(rgl.n1.n.y - offy, cf), coef(rgl.n2.n.x - offx, cf), coef(rgl.n2.n.y - offy, cf));
                    //lines++;
                } else {
                    dont_draw = false;
                }
            }
        }


        gBackground.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        gBackground.drawImage(image, 0, 0, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(imageBackground, "png", out);
        //System.out.println(lines);
        return out.toByteArray();
    }

    private byte[] drawBuildingJava(QuadTreeNode treeNode, int x, int y, int z, int price, double range){
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, 256, 256);
        
        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int offx = x*mult*256;
        int offy = y*mult*256;
        double cf = 1.0/mult;
        double dist = price*range*2;
        g.setStroke(new BasicStroke(1f));
        for (MapShape mh: treeNode.shapes){
            if (mh.isPoly && mh.way.data.containsKey("building")) {
                g.setStroke(new BasicStroke(1f));
                Polygon poly = new Polygon();
                for (MapPoint p : mh.points) {
                    poly.addPoint(coef(p.x-offx, cf), coef(p.y-offy, cf));
                }
                if (mh.way.apartments != null) {
                    double ap_price = -1000000000000000000000.0;
                    for (Apartment ap: mh.way.apartments){
                        if (Math.abs(price-ap_price) > Math.abs(price-ap.price/ap.area)){
                            ap_price = ap.price/ap.area;
                        }
                    }
                    if (ap_price > price - price*range && ap_price < price+price*range) {
                        float color = (float) ((((price + price * range)-ap_price) / dist) * (24.0 / 36.0));
                        Color clr = Color.getHSBColor(color, 1.0f, 0.95f);

                        g.setColor(new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), 200));


                    }else{

                        continue;//g.setColor(new Color(255,255, 255, 0));
                        //g.setColor(new Color(207, 22, 187));
                    }
                }else{
                    //g.setColor(new Color(207, 22, 187));
                    continue;//g.setColor(new Color(255,255, 255, 0));
                }
                g.fill(poly);
                if (g.getColor().getAlpha() != 0) {
                    g.setColor(new Color(g.getColor().getRGB()));
                }else{
                    g.setColor(new Color(255, 255, 255, 0));
                }
                Path2D.Float path2D = new Path2D.Float();

                MapPoint p1 = mh.points.get(0);
                MapPoint p2 = mh.points.get(1);
                path2D.moveTo(coef(p1.x-offx, cf), coef(p1.y-offy, cf));
                for (int i=2; i<mh.points.size(); i++){
                    if (!treeNode.onBounds(p1) || !treeNode.onBounds(p2)){
                        path2D.lineTo(coef(p2.x-offx, cf), coef(p2.y-offy, cf));
                    }else{
                        path2D.moveTo(coef(p2.x-offx, cf), coef(p2.y-offy, cf));
                    }
                    p1 = p2;
                    p2 = mh.points.get(i);
                }
                p1 = mh.points.get(0);
                p2 = mh.points.get(mh.points.size()-1);
                if (!treeNode.onBounds(p1) || !treeNode.onBounds(p2)){
                    path2D.lineTo(coef(p2.x-offx, cf), coef(p2.y-offy, cf));
                }
                g.draw(path2D);
            }


        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try{ImageIO.write(image, "png", out);}catch (Exception e){}
        //System.out.println(lines);
        return out.toByteArray();
    }

    public byte[] drawGraph(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind, int roadGraphNodeNum) throws Exception{
        if (isOpenGLAvailable || isCairoAvailable){
            if (isCairoAvailable) {
                return drawGraphNativeCairo(treeNode, x, y, z, mult, mode, max_dist, ind);
            }else{
                return drawGraphNativeOpenGL(treeNode, x, y, z, mult, mode, max_dist, ind);
            }
        }else{
            return drawGraphJava(treeNode, x, y, z, mult, mode, max_dist, ind, roadGraphNodeNum);
        }
    }

    public byte[] drawBuilding(QuadTreeNode treeNode, int x, int y, int z, int price, double range ){
        if (isOpenGLAvailable || isCairoAvailable){
            if (isCairoAvailable) {
                return drawBuildingNativeCairo(treeNode, x, y, z, price, range);
            }else{
                //return drawGraphNativeOpenGL(treeNode, x, y, z, mult, mode, max_dist, ind);
                return null;
            }
        }else{
            return drawBuildingJava(treeNode, x, y, z, price, range);

        }
    }
}
