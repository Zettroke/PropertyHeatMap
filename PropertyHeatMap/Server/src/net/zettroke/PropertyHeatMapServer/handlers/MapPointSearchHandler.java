package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.Json;
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
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;

import java.nio.charset.Charset;
import java.util.Map;

public class MapPointSearchHandler implements ShittyHttpHandler {
    private PropertyMap propertyMap;


    static String path = "search/point";

    @Override
    public String getPath() {
        return path;
    }



    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //System.out.println(Thread.currentThread().getName());
        //System.out.println(request.uri());





        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (!(decoder.parameters().containsKey("x")&&decoder.parameters().containsKey("y")&&decoder.parameters().containsKey("z"))){
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        int z=0, x=0, y=0, mult=0;
        try {
            z = Integer.decode(decoder.parameters().get("z").get(0));
            mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
            x = mult * Integer.decode(decoder.parameters().get("x").get(0));
            y = mult * Integer.decode(decoder.parameters().get("y").get(0));
        }catch (NumberFormatException e){
            JsonObject answer = new JsonObject();
            ByteBuf buf = ctx.alloc().buffer();
            answer.add("status", "incorrect numbers");
            buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set("Content-Type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

            return;
        }
        JsonObject answer = new JsonObject();
        Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
        if (w != null) {
            answer.add("status", "success");

            JsonArray arr = new JsonArray();
            arr.add(Jsonizer.toJson(w, true));
            answer.add("objects", arr);
        }else{
            answer.add("status", "not found");
        }
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set("Content-Type", "text/json; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);


    }

    public MapPointSearchHandler(PropertyMap p){
        propertyMap = p;
    }
}

