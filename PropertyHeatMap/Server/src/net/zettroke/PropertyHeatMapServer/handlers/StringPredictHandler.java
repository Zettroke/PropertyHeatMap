package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.ParamsChecker;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class StringPredictHandler implements ShittyHttpHandler{
    private PropertyMap propertyMap;
    final String path = "search/predict";
    final static ThreadLocal<ParamsChecker> pcheckers = new ThreadLocal<ParamsChecker>() {
        @Override
        protected ParamsChecker initialValue() {
            return new ParamsChecker()
                    .addParam("text").type(ParamsChecker.StringType).finish()
                    .addParam("suggestions").type(ParamsChecker.IntegerType).range(0, 1000).finish();
        }
    };
    @Override
    public String getPath() {
        return path;
    }
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ParamsChecker checker = pcheckers.get();
        if (checker.isValid(decoder)) {
            String req = decoder.parameters().get("text").get(0);
            int num = Integer.parseInt(decoder.parameters().get("suggestions").get(0));
            JsonObject obj = new JsonObject();
            ArrayList<String> preds = propertyMap.predictor.predict(req, num);

            obj.add("status", "success");

            JsonArray suggesitons = new JsonArray();
            for (String s : preds) {
                suggesitons.add(s);
            }
            obj.add("suggestions", suggesitons);
            //}
            ByteBuf buf = ctx.alloc().buffer().writeBytes(obj.toString().getBytes(Charset.forName("utf-8")));
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set("content-type", "text/json; charset=UTF-8");
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



    public StringPredictHandler(PropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }
}
