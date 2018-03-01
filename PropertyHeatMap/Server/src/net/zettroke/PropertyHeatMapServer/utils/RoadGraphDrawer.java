package net.zettroke.PropertyHeatMapServer.utils;

import io.netty.buffer.ByteBufOutputStream;
import net.zettroke.PropertyHeatMapServer.PropertyMapServer;
import net.zettroke.PropertyHeatMapServer.map.MapShape;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.RoadType;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphLine;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;

public class RoadGraphDrawer{

    public static boolean isGlobalSet = false;
    public static boolean isNative;
    public static boolean forceCairo = false;

    public static boolean test = false;


    private final boolean isCairoAvailable;
    private final boolean isOpenGLAvailable;
    final static int zoom_level = 13;
    static RoadGraphDrawer instance = null;

    private RoadGraphDrawer(PropertyMap propertyMap){
        boolean tempOpenGL=false, tempCairo;
        if (!isGlobalSet || isNative) {
            if (!forceCairo) {
                try {
                    System.loadLibrary("JNI-OpenGL-Drawer");
                    IntArrayList rgns = new IntArrayList();
                    for (int i=0; i<propertyMap.roadGraph.size()*2; i++){
                        rgns.add(0);
                    }
                    for (RoadGraphNode rgn: propertyMap.roadGraph.values()){
                        rgns.set(rgn.index*2, rgn.n.x);
                        rgns.set(rgn.index*2+1, rgn.n.y);
                    }
                    IntArrayList rgls = new IntArrayList();
                    for (RoadGraphLine rgl: propertyMap.roadGraphLines){
                        rgls.addAll(rgl.n1.index, rgl.n2.index, rgl.type.ordinal());
                    }
                    initOpenGLRenderer(10, PropertyMap.default_zoom, rgns.toArray(), rgns.size(), rgls.toArray(), rgls.size(), propertyMap.tree.root.bounds, PropertyMapServer.PROC_NUM);
                    // OpenGL всегда фейлит первую картинку. Почему не знаю. Решено отрисовкой одной картинки.
                /*int mx = 0;
                for (RoadGraphNode rgn: PropertyMap.init_node.roadGraphNodes){
                    mx = Math.max(rgn.index, mx);
                }
                drawNative(PropertyMap.init_node, 0, 0, 11, 8, 1, 18000, 0, mx+2);*/
                    tempOpenGL = true;

                } catch (UnsatisfiedLinkError e) {
                    tempOpenGL = false;
                    System.out.println("OpenGl draw not available");
                }
            }
            if (!tempOpenGL) {
                try {
                    System.loadLibrary("JNI-CairoDrawer");
                    System.out.println("Selected Cairo!");
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

    public synchronized static RoadGraphDrawer getInstance(PropertyMap propertyMap) {
        if (instance == null) {
            instance = new RoadGraphDrawer(propertyMap);
        }
        return instance;

    }
    //[x1, y1, color1, x2, y2, color2, width]
    public native byte[] drawCairoCall(int[] data, int len, int divider, int zoom_level);

    public native void initOpenGLRenderer(int cache_size, int server_zoom, int[] roadGraphNodes, int len1, int[] roadGraphLines, int len2, int[] bounds, int num_process);

    public native byte[] drawOpenGLCall(int[] data, int len, int divider, int zoom_level, int process_num, int max_dist);

    public native void closeOpenGLRenderer();
    public native void updateOpenGLDistances(int[] data, int len, int ind);
    public native byte[] drawOpenGLTile(int x, int y, int z, int max_dist, int ind, int proc_ind);

    private byte[] drawNativeCairo(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind){
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
        return drawCairoCall(bundle.toArray(), bundle.size(), mult, z);

    }

    private byte[] drawNativeOpenGL(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind){

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

        return drawOpenGLCall(bundle.toArray(), bundle.size(), mult, z, th_ind, max_dist);
    }

    public static int coef(int n, double cf){
        return (int)Math.round(cf*n);
    }

    private byte[] drawJava(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind, int roadGraphNodeNum) throws IOException{
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
        boolean[] visited = BoolArrayPool.getArray(roadGraphNodeNum);
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : treeNode.roadGraphNodes){
            //if (treeNode.inBounds(rgn.n)){
            visited[rgn.index] = true;
            for (int i=0; i<rgn.ref_to[mode].length; i++){
                RoadGraphNode ref = rgn.ref_to[mode][i];
                if (!visited[ref.index]){
                    switch (rgn.ref_types[mode][i]){
                        case SECONDARY:
                            if (z > zoom_level) {
                                g.setStroke(secondary_stroke);
                            }else{
                                g.setStroke(new BasicStroke(120f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            }
                            break;
                        case RESIDENTIAL:
                            if (z > zoom_level) {
                                g.setStroke(residential_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case SERVICE:
                            if (z > zoom_level) {
                                g.setStroke(service_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case TERTIARY:
                            if (z > zoom_level) {
                                g.setStroke(tertiary_stroke);
                            }else{
                                g.setStroke(tertiary_stroke);
                                //dont_draw = true;
                            }
                            break;
                        case PRIMARY:
                            if (z > zoom_level) {
                                g.setStroke(primary_stroke);
                            }else{
                                g.setStroke(new BasicStroke(120f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            }
                            break;
                        case TRUNK:
                            g.setStroke(new BasicStroke(80f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            break;
                        case DEFAULT:
                            if (z > zoom_level) {
                                g.setStroke(default_stroke);
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case LIVING_STREET:
                            if (z > zoom_level) {
                                g.setStroke(living_stroke);
                            }else{
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
                            }else{
                                dont_draw = true;
                            }
                            break;
                    }
                    if (!dont_draw) {

                        g.setPaint(new GradientPaint(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), rgn.getNodeColor(max_dist, ind),
                                coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf), ref.getNodeColor(max_dist, ind)));
                        g.drawLine(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf));
                        //lines++;
                    }else{
                        dont_draw = false;
                    }
                }
            }

        }

        BoolArrayPool.returnArray(visited);

        gBackground.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        gBackground.drawImage(image, 0, 0, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(imageBackground, "png", out);
        //System.out.println(lines);
        return out.toByteArray();
    }

    public byte[] draw(QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind, int roadGraphNodeNum) throws Exception{
        if (isOpenGLAvailable || isCairoAvailable){
            if (isCairoAvailable) {
                return drawNativeCairo(treeNode, x, y, z, mult, mode, max_dist, ind);
            }else{
                return drawNativeOpenGL(treeNode, x, y, z, mult, mode, max_dist, ind);
            }
        }else{
            return drawJava(treeNode, x, y, z, mult, mode, max_dist, ind, roadGraphNodeNum);
        }
    }
}
