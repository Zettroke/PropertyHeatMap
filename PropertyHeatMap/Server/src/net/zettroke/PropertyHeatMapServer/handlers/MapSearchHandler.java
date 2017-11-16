package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import javafx.util.Pair;
import jdk.nashorn.internal.ir.debug.JSONWriter;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;

import java.nio.charset.Charset;
import java.util.Map;

public class MapSearchHandler implements ShittyHttpHandler {
    private PropertyMap propertyMap;

   public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //System.out.println(Thread.currentThread().getName());
        //System.out.println(request.uri());


        long start = System.nanoTime();
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (!(decoder.parameters().containsKey("x")&&decoder.parameters().containsKey("y")&&decoder.parameters().containsKey("z"))){
            return;
        }
        int z = Integer.decode(decoder.parameters().get("z").get(0));
        int mult = (int)Math.pow(2, PropertyMap.default_zoom - z);
        int x = mult*Integer.decode(decoder.parameters().get("x").get(0));
        int y = mult*Integer.decode(decoder.parameters().get("y").get(0));
        JsonObject answer = new JsonObject();
        //JSONObject answer = new JSONObject();
        Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
        if (w != null) {

            answer.add("status", "success");
            answer.add("status", "success");
            JsonObject data = new JsonObject();
            for (Map.Entry<String, String> p: w.data.entrySet()){
                answer.add(p.getKey(), p.getValue());
            }
            answer.add("data", data);
            answer.add("id", w.id);
            answer.add("zoom_level", PropertyMap.default_zoom);
            JsonArray points = new JsonArray();
            for (MapPoint p: w.nodes){
                JsonArray point = new JsonArray(); point.add(p.x); point.add(p.y);
                points.add(point);
            }
            answer.add("points", points);
        }else{
            answer.add("status", "not found");
        }

        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);


    }

    public MapSearchHandler(PropertyMap p){
        propertyMap = p;
    }
}

