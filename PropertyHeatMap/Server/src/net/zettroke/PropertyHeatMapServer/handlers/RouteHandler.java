package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import javafx.util.Pair;

import java.util.ArrayList;

public class RouteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    PathRouter pathRouter;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) throws Exception {
        //System.out.println("request " + request.uri());
        String uri = request.uri().replace("/api", "");
        if (uri.contains("exit")){
            System.out.println("Trying to shutdown");
            channelHandlerContext.channel().close();
            channelHandlerContext.channel().parent().close();
        }
        pathRouter.getHandler(uri).handle(channelHandlerContext, request);

    }

    public RouteHandler(PathRouter pathRouter){
        this.pathRouter = pathRouter;
    }
}
