package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.MapShape;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;
import net.zettroke.PropertyHeatMapServer.utils.Drawer;
import net.zettroke.PropertyHeatMapServer.utils.ParamsChecker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashSet;

public class PriceTileHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;

    final String path = "tile/price";

    final static ThreadLocal<ParamsChecker> pcheckers = new ThreadLocal<ParamsChecker>() {
        @Override
        protected ParamsChecker initialValue() {
            return new ParamsChecker()
                    .addParam("x").type(ParamsChecker.IntegerType).finish()
                    .addParam("y").type(ParamsChecker.IntegerType).finish()
                    .addParam("z").type(ParamsChecker.IntegerType).finish()
                    .addParam("price").type(ParamsChecker.IntegerType).finish()
                    .addParam("range").type(ParamsChecker.DoubleType).finish();
        }
    };

    @Override
    public String getPath() {
        return path;
    }
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ParamsChecker checker = pcheckers.get();
        if (checker.isValid(decoder)) {
            int z = Integer.decode(decoder.parameters().get("z").get(0));
            int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
            int x = Integer.decode(decoder.parameters().get("x").get(0));
            int y = Integer.decode(decoder.parameters().get("y").get(0));
            int price = Integer.decode(decoder.parameters().get("price").get(0));
            double range = Double.parseDouble(decoder.parameters().get("range").get(0));
            boolean absolute = false;
            if (decoder.parameters().containsKey("absolute")){
                absolute = Boolean.parseBoolean(decoder.parameters().get("absolute").get(0));
            }
            if (absolute){
                //x -= (int)(propertyMap.off_x * Math.pow(2, z-10)); y -= (int)(propertyMap.off_y * Math.pow(2, z-10));
            }

            QuadTreeNode treeNode = new QuadTreeNode(new int[]{x * mult * 256, y * mult * 256, (x + 1) * mult * 256, (y + 1) * mult * 256}, false);
            propertyMap.fillTreeNode(treeNode);

            byte[] arr = Drawer.getInstance().drawBuilding(treeNode, x, y, z, price, range);

            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(arr);
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set("Content-Type", "image/png");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }else{
            JsonObject ans = checker.createErrorAnswer();
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes(ans.toString().getBytes(Charset.forName("utf-8"))));
            response.headers().set("content-type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public PriceTileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }


}
