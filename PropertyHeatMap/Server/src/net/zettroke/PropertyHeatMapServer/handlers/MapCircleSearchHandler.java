package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MapCircleSearchHandler implements ShittyHttpHandler{

    static String path = "search/circle";
    private PropertyMap propertyMap;
    @Override
    public String getPath() {
        return path;
    }
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        int z = Integer.decode(decoder.parameters().get("z").get(0));
        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int x = mult * Integer.decode(decoder.parameters().get("x").get(0));
        int y = mult * Integer.decode(decoder.parameters().get("y").get(0));
        int r = mult * Integer.decode(decoder.parameters().get("r").get(0));

        ArrayList<Way> ways = new ArrayList<>(propertyMap.findShapesByCircle(new MapPoint(x, y), r));

        JsonObject answer = new JsonObject();
        if (ways.size() == 0){
            answer.add("status", "not found");
        }else{
            answer.add("status", "success");
            answer.add("zoom_level", PropertyMap.default_zoom);
            JsonArray arr = new JsonArray();
            for (Way w: ways){
                arr.add(Jsonizer.toJson(w, true));
            }
            answer.add("objects", arr);
        }

        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set("Content-Type", "text/json; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    public MapCircleSearchHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }


}
