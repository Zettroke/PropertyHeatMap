package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.ArrayList;

public class RouteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    PathRouter pathRouter;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) throws Exception {
        pathRouter.getHandler(request.uri()).handle(channelHandlerContext, request);

    }

    public RouteHandler(PathRouter pathRouter){
        this.pathRouter = pathRouter;
    }
}
