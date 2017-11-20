package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;

public class MapCircleSearchHandler implements ShittyHttpHandler{

    static String path = "search/circle";

    private PropertyMap propertyMap;
    @Override
    public String getPath() {
        return path;
    }
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        ctx.close();
    }

    public MapCircleSearchHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }


}
