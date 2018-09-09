package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.ParamsChecker;

import java.nio.charset.Charset;

public class MapPointSearchHandler implements ShittyHttpHandler {
    private PropertyMap propertyMap;


    final String path = "search/point";

    static final ParamsChecker checker = new ParamsChecker()
            .or(new ParamsChecker()
                    .addName("x").addType(ParamsChecker.IntegerType).addNoRange()
                    .addName("y").addType(ParamsChecker.IntegerType).addNoRange()
                    .addName("z").addType(ParamsChecker.IntegerType).addNoRange(),
                new ParamsChecker()
                    .addName("lat").addType(ParamsChecker.DoubleType).addNoRange()
                    .addName("lon").addType(ParamsChecker.DoubleType).addNoRange()
            );

    @Override
    public String getPath() {
        return path;
    }
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (checker.isValid(decoder)) {
            int x;
            int y;
            boolean latlon = false;
            if (decoder.parameters().containsKey("lat") && decoder.parameters().containsKey("lon")){
                double lat = Double.parseDouble(decoder.parameters().get("lat").get(0));
                double lon = Double.parseDouble(decoder.parameters().get("lon").get(0));
                int[] xy =  PropertyMap.mercator(lon, lat);
                x = xy[0]; y = xy[1];
                latlon = true;
            }else {
                int z = Integer.decode(decoder.parameters().get("z").get(0));
                int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
                x = mult * Integer.decode(decoder.parameters().get("x").get(0));
                y = mult * Integer.decode(decoder.parameters().get("y").get(0));
            }
            JsonObject answer = new JsonObject();
            Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
            if (w != null) {
                answer.add("status", "success");

                JsonArray arr = new JsonArray();
                if (!latlon) {
                    arr.add(Jsonizer.toJson(w, true));
                }else{
                    arr.add(Jsonizer.toJson(w, true, true));
                }
                answer.add("objects", arr);

            } else {
                answer.add("status", "not found");
            }
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set("Content-Type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }else{
            JsonObject ans = new JsonObject();
            ans.add("status", "error");
            ans.add("error", checker.getErrorMessage());
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes(ans.toString().getBytes(Charset.forName("utf-8"))));
            response.headers().set("content-type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }


    }

    public MapPointSearchHandler(PropertyMap p){
        propertyMap = p;
    }
}

