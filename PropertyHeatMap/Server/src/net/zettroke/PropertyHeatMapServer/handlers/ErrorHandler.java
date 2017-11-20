package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

public class ErrorHandler implements ShittyHttpHandler {
    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println("Error!!");
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
