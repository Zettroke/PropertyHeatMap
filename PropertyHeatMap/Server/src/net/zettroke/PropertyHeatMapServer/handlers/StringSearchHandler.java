package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.ParamsChecker;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class StringSearchHandler implements ShittyHttpHandler{
    final static String path = "search/string";
    PropertyMap propertyMap;
    @Override
    public String getPath() {
        return path;
    }

    final static ThreadLocal<ParamsChecker> pcheckers = new ThreadLocal<ParamsChecker>() {
        @Override
        protected ParamsChecker initialValue() {
            return new ParamsChecker()
                    .addParam("text").type(ParamsChecker.StringType).finish();
        }
    };


    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ParamsChecker checker = pcheckers.get();
        if (checker.isValid(decoder)) {
            String text = decoder.parameters().get("text").get(0);


            Way way = propertyMap.searchMap.get(text.toLowerCase());
            JsonObject ans = new JsonObject();
            if (way == null) {
                ans.add("status", "not found");
            } else {
                ans.add("status", "found");
                ans.add("result", Jsonizer.toJson(way, true));

            }
            ByteBuf buf = ctx.alloc().buffer().writeBytes(ans.toString().getBytes(Charset.forName("utf-8")));
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
    }


    StringSearchHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }
}
