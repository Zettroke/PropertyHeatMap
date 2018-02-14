package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface ShittyHttpHandler {
    String getPath();
    void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception;

}
