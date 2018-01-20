package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.BoolArrayPool;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphCache;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphKey;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class RoadGraphTileHandler implements ShittyHttpHandler{
    PropertyMap propertyMap;

    static Lock global_rgn_lock = new ReentrantLock();


    final String path = "tile/road";
    @Override
    public String getPath() {
        return path;
    }
    double coefficent = 1;

    int coef(int n){
        return (int)Math.round(coefficent*n);
    }


    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        int z = Integer.decode(decoder.parameters().get("z").get(0));
        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int x = Integer.decode(decoder.parameters().get("x").get(0));
        int y = Integer.decode(decoder.parameters().get("y").get(0));
        long start_id = Long.decode(decoder.parameters().get("start_id").get(0));
        int max_dist =  Integer.decode(decoder.parameters().get("max_dist").get(0));
        boolean foot = Boolean.parseBoolean(decoder.parameters().get("foot").get(0));
        //System.out.println("Id - " + id + " Thread - " + Thread.currentThread().getName());

        coefficent = 1.0/mult;

        QuadTreeNode treeNode = new QuadTreeNode(new int[]{(x-1)*mult*256, (y-1)*mult*256, (x+2)*mult*256, (y+2)*mult*256});
        //QuadTreeNode treeNode = new QuadTreeNode(new int[]{(x)*mult*256, (y)*mult*256, (x+1)*mult*256, (y+1)*mult*256});
        //QuadTreeNode treeNode = propertyMap.tree.root;
        BufferedImage imageTemp = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) imageTemp.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        HashMap<Long, RoadGraphNode> graph = null;

        global_rgn_lock.lock();
        if (CalculatedGraphCache.contain(start_id, foot, max_dist)){
            graph = CalculatedGraphCache.get(start_id, foot,  max_dist);
            global_rgn_lock.unlock();
        }else {
            //System.out.println("Start calculating roadgraph - " + Thread.currentThread().getName());
            CalculatedGraphKey key = new CalculatedGraphKey(start_id, foot, max_dist);
            if (!CalculatedGraphCache.current_processing.containsKey(key)) {
                ReentrantLock lck = new ReentrantLock();
                CalculatedGraphCache.current_processing.put(key, lck);
                lck.lock();
                global_rgn_lock.unlock();

                long start = System.nanoTime();
                graph = propertyMap.getCalculatedRoadGraph(start_id, foot, max_dist);
                CalculatedGraphCache.store(start_id, foot, max_dist, graph);
                System.out.println("Graph Calculated in " + ((System.nanoTime()-start) / 1000000.0) + "millis.");
                lck.unlock();
            }else{
                ReentrantLock lck = CalculatedGraphCache.current_processing.get(key);
                global_rgn_lock.unlock();
                lck.lock();
                graph = CalculatedGraphCache.get(start_id, foot, max_dist);
                if (!lck.hasQueuedThreads()){
                    CalculatedGraphCache.current_processing.remove(key);
                }
                lck.unlock();

            }
        }

        BasicStroke secondary_stroke = new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke primary_stroke = new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke tertiary_stroke = new BasicStroke(60f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke service_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke residential_stroke = new BasicStroke(50f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke living_stroke = new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke default_stroke = new BasicStroke(20f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke unknown_stroke = new BasicStroke(10f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);



        int offx = x*mult*256;
        int offy = y*mult*256;
        boolean dont_draw = false;
        boolean[] visited = BoolArrayPool.getArray(graph.size());
        //HashSet<Integer> visited = new HashSet<>();
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : graph.values()){
            if (treeNode.inBounds(rgn.n)){
                visited[rgn.index] = true;
                for (int i=0; i<rgn.ref_to.length; i++){
                    RoadGraphNode ref = rgn.ref_to[i];
                    if (!visited[ref.index]){
                        switch (rgn.ref_types.get(i)){
                            case SECONDARY:
                                g.setStroke(secondary_stroke);
                                break;
                            case RESIDENTIAL:
                                g.setStroke(residential_stroke);
                                break;
                            case SERVICE:
                                g.setStroke(service_stroke);
                                break;
                            case TERTIARY:
                                g.setStroke(tertiary_stroke);
                                break;
                            case PRIMARY:
                                g.setStroke(primary_stroke);
                                break;
                            case TRUNK:
                                g.setStroke(new BasicStroke(80f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case DEFAULT:
                                g.setStroke(default_stroke);
                                break;
                            case LIVING_STREET:
                                g.setStroke(living_stroke);
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
                                if (z > 14) {
                                    g.setStroke(unknown_stroke);
                                }else{
                                    dont_draw = true;
                                }
                                break;
                        }
                        if (!dont_draw) {
                            g.setPaint(new GradientPaint(coef(rgn.n.x - offx), coef(rgn.n.y - offy), rgn.getNodeColor(max_dist), coef(ref.n.x - offx), coef(ref.n.y - offy), ref.getNodeColor(max_dist)));
                            g.drawLine(coef(rgn.n.x - offx), coef(rgn.n.y - offy), coef(ref.n.x - offx), coef(ref.n.y - offy));
                        }else{
                            dont_draw = false;
                        }
                    }
                }
            }
        }
        /*for (RoadGraphNode rgn: to_clear) {
            rgn.visited = false;
        }*/
        BoolArrayPool.returnArray(visited);
        ByteBuf buf = ctx.alloc().buffer();
        ByteBufOutputStream out = new ByteBufOutputStream(buf);
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = (Graphics2D)image.getGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.drawImage(imageTemp, 0, 0, null);

        ImageIO.write(image, "png", out);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    public RoadGraphTileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }



}
