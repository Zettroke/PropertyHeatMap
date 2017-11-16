package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.Charset;

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
        JSONObject answer = new JSONObject();
        Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
        if (w != null) {
            answer.put("status", "success");
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
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);


    }

    public MapSearchHandler(PropertyMap p){
        propertyMap = p;
    }
}

