package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;

public class StringSearchHandler implements ShittyHttpHandler{
    private PropertyMap propertyMap;
    final String path = "search/string";
    @Override
    public String getPath() {
        return path;
    }
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println("handled string search");
        ctx.close();
    }



    public StringSearchHandler(PropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }
}
