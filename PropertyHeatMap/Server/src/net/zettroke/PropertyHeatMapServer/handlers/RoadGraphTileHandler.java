package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.*;

import java.awt.*;
import java.nio.charset.Charset;

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



}
