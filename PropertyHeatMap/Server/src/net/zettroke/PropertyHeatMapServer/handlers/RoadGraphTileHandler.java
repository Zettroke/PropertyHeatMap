package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.*;

import java.awt.*;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

public class RoadGraphTileHandler implements ShittyHttpHandler{
    PropertyMap propertyMap;

    static final int zoom_level = 13; 

    final String path = "tile/road";
    @Override
    public String getPath() {
        return path;
    }
    double coefficent = 1;
    final int around = 8;
    final static ThreadLocal<ParamsChecker> pcheckers = new ThreadLocal<ParamsChecker>(){
        @Override
        protected ParamsChecker initialValue() {
            return new ParamsChecker()
                    .addParam("x").type(ParamsChecker.IntegerType).finish()
                    .addParam("y").type(ParamsChecker.IntegerType).finish()
                    .addParam("z").type(ParamsChecker.IntegerType).finish()
                    .addParam("start_id").type(ParamsChecker.LongType).finish()
                    .addParam("max_dist").type(ParamsChecker.IntegerType).range(0, 36000).finish()
                    .addParam("foot").type(ParamsChecker.BooleanType).finish();
        }
    };

    int coef(int n){
        return (int)Math.round(coefficent*n);
    }


    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        long st = System.nanoTime();
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ParamsChecker checker = pcheckers.get();
        if (checker.isValid(decoder)) {
            int z = Integer.decode(decoder.parameters().get("z").get(0));
            int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
            int x = Integer.decode(decoder.parameters().get("x").get(0));
            int y = Integer.decode(decoder.parameters().get("y").get(0));
            long start_id = Long.decode(decoder.parameters().get("start_id").get(0));
            int max_dist = Integer.decode(decoder.parameters().get("max_dist").get(0));
            boolean foot = Boolean.parseBoolean(decoder.parameters().get("foot").get(0));
            boolean absolute = false;
            if (decoder.parameters().containsKey("absolute")){
                absolute = Boolean.parseBoolean(decoder.parameters().get("absolute").get(0));
            }
            if (absolute){
                //x -= (int)(propertyMap.off_x * Math.pow(2, z-10)); y -= (int)(propertyMap.off_y * Math.pow(2, z-10));
            }
            coefficent = 1.0 / mult;

            QuadTreeNode treeNode = new QuadTreeNode(new int[]{x * mult * 256 - 400 , y * mult * 256 - 400, (x + 1) * mult * 256 + 400 , (y + 1) * mult * 256 + 400}, false);

            propertyMap.fillTreeNodeWithRoadGraphLines(treeNode);
            int ind = 0;

            ind = propertyMap.getCalculatedGraphIndex(start_id, foot, max_dist);
            /*
            if (propertyMap.cache.contains(key)) {

                ind = propertyMap.cache.getCachedIndex(key);
                propertyMap.cache.lock.unlock();
            } else {
                ind = propertyMap.cache.getNewIndexForGraph(key);
                propertyMap.calcRoadGraph(start_id, foot, max_dist, ind);
                TimeMeasurer.printMeasure("Graph calculated in %t millis.", start);
                propertyMap.cache.lock.unlock();
            }
            */// Это то, что было раньше. Расчет одного графа останавливал все запросы,
            /// в независимости от того собираются ли они считать граф или имеют уже готорый результат

            //System.out.println("Server thread " + ((IndexedThread)Thread.currentThread()).index);


            int mode = foot ? 0 : 1;
            byte[] img = Drawer.getInstance().drawGraph(treeNode, x, y, z, mult, mode, max_dist, ind, propertyMap.roadGraph.size());
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(img);

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }else{
            JsonObject ans = new JsonObject();
            ans.add("status", "error");
            ans.add("error", checker.getErrorMessage());
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes(ans.toString().getBytes(Charset.forName("utf-8"))));
            response.headers().set("content-type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
        //TimeMeasurer.printMeasure(st, "Tile done in %t millis");
    }

    public RoadGraphTileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }

    private void drawJava(Graphics2D g, QuadTreeNode container, int mult, int x, int y, int z, int mode, int max_dist, int ind){
        BasicStroke secondary_stroke = new BasicStroke(65f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
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
        boolean[] visited = BoolArrayPool.getArray(propertyMap.roadGraph.size());
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : container.roadGraphNodes){
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

                        g.setPaint(new GradientPaint(coef(rgn.n.x - offx), coef(rgn.n.y - offy), rgn.getNodeColor(max_dist, ind),
                                coef(ref.n.x - offx), coef(ref.n.y - offy), ref.getNodeColor(max_dist, ind)));
                        g.drawLine(coef(rgn.n.x - offx), coef(rgn.n.y - offy), coef(ref.n.x - offx), coef(ref.n.y - offy));
                    }else{
                        dont_draw = false;
                    }
                }
            }
            //}
        }

        BoolArrayPool.returnArray(visited);
    }

}
