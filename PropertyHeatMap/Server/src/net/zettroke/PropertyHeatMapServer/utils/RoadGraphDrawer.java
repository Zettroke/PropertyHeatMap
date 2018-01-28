package net.zettroke.PropertyHeatMapServer.utils;

import io.netty.buffer.ByteBufOutputStream;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RoadGraphDrawer{

    private boolean isNativeAvailable;
    final static int zoom_level = 13;
    static RoadGraphDrawer instance = null;

    private RoadGraphDrawer(){
        try {
            System.setProperty("java.library.path", System.getProperty("java.library.path") + ";./native_libs");

            System.loadLibrary("JNI-CairoDrawer");
            isNativeAvailable = true;
        }catch (UnsatisfiedLinkError e){
            System.err.println("Cairo draw not available");
	    e.printStackTrace();
            isNativeAvailable = false;
	    System.out.println(System.getProperty("java.library.path"));
        }
    }

    public static RoadGraphDrawer getInstance() {
        if (instance == null) {
            instance = new RoadGraphDrawer();
        }
        return instance;

    }
    //[x1, y1, color1, x2, y2, color2, width]
    private native byte[] drawNativeCall(int[] data, int len);

    private byte[] drawNative(PropertyMap propertyMap, QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind){
        IntArrayList bundle = new IntArrayList();

        int secondary_stroke =      Math.round(65f/mult*100);
        int primary_stroke =        Math.round(75f/mult*100);
        int tertiary_stroke =       Math.round(60f/mult*100);
        int service_stroke =        Math.round(25f/mult*100);
        int residential_stroke =    Math.round(50f/mult*100);
        int living_stroke =         Math.round(25f/mult*100);
        int default_stroke =        Math.round(20f/mult*100);
        int unknown_stroke =        Math.round(10f/mult*100);
        double cf = 1.0/mult;
        int offx = x*mult*256;
        int offy = y*mult*256;
        boolean dont_draw = false;
        boolean[] visited = BoolArrayPool.getArray(propertyMap.roadGraph.size());
        //g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : treeNode.roadGraphNodes){
            //if (treeNode.inBounds(rgn.n)){
            visited[rgn.index] = true;
            int stroke = 0;
            for (int i=0; i<rgn.ref_to[mode].length; i++){
                RoadGraphNode ref = rgn.ref_to[mode][i];
                if (!visited[ref.index]){
                    switch (rgn.ref_types[mode][i]){
                        case SECONDARY:
                            if (z > zoom_level) {
                                stroke = secondary_stroke;
                            }else{
                                stroke = Math.round(120f/mult*100);
                            }
                            break;
                        case RESIDENTIAL:
                            if (z > zoom_level) {
                                stroke = residential_stroke;
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case SERVICE:
                            if (z > zoom_level) {
                                stroke = service_stroke;
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case TERTIARY:
                            if (z > zoom_level) {
                                stroke = tertiary_stroke;
                            }else{
                                stroke = tertiary_stroke;
                                //dont_draw = true;
                            }
                            break;
                        case PRIMARY:
                            if (z > zoom_level) {
                                stroke = primary_stroke;
                            }else{
                                stroke = Math.round(120f/mult*100);
                            }
                            break;
                        case TRUNK:
                            stroke = Math.round(80f/mult*100);
                            break;
                        case DEFAULT:
                            if (z > zoom_level) {
                                stroke = default_stroke;
                            }else{
                                dont_draw = true;
                            }
                            break;
                        case LIVING_STREET:
                            if (z > zoom_level) {
                                stroke = living_stroke;
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
                                stroke = unknown_stroke;
                            }else{
                                dont_draw = true;
                            }
                            break;
                    }
                    if (!dont_draw) {
                        /*if (rgn.getNodeColor(max_dist, ind).getRGB() != -5767146){
                            System.out.println();
                        }*/
                        /*g.setPaint(new GradientPaint(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), rgn.getNodeColor(max_dist, ind),
                                coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf), ref.getNodeColor(max_dist, ind)));*/
                        bundle.addAll(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), rgn.getNodeColor(max_dist, ind).getRGB(),
                                            coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf), ref.getNodeColor(max_dist, ind).getRGB(), stroke);
                        //g.drawLine(coef(rgn.n.x - offx, cf), coef(rgn.n.y - offy, cf), coef(ref.n.x - offx, cf), coef(ref.n.y - offy, cf));
                    }else{
                        dont_draw = false;
                    }
                }
            }

        }

        BoolArrayPool.returnArray(visited);


        return drawNativeCall(bundle.toArray(), bundle.size());
    }

    public static int coef(int n, double cf){
        return (int)Math.round(cf*n);
    }

    private byte[] drawJava(PropertyMap propertyMap,  QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind) throws IOException{
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
        boolean[] visited = BoolArrayPool.getArray(propertyMap.roadGraph.size());
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
        return out.toByteArray();
    }

    public byte[] draw(PropertyMap propertyMap, QuadTreeNode treeNode, int x, int y, int z, int mult, int mode, int max_dist, int ind) throws Exception{
        if (isNativeAvailable){
            return drawNative(propertyMap, treeNode, x, y, z, mult, mode, max_dist, ind);
        }else{
            return drawJava(propertyMap, treeNode, x, y, z, mult, mode, max_dist, ind);
        }
    }
}
