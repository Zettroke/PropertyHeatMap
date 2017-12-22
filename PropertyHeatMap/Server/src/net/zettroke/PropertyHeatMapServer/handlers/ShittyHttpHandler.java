package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface ShittyHttpHandler {

    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception;
    public String getPath();

}
