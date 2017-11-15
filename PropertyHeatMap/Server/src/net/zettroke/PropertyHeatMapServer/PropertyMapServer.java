package net.zettroke.PropertyHeatMapServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.nio.charset.Charset;

public class PropertyMapServer {

    public void start() throws Exception{
        PropertyMap propertyMap = new PropertyMap();
        PropertyMapLoaderOSM.load(propertyMap, new File("map_small.osm"));
        long start = System.nanoTime();
        propertyMap.initParallel();

        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ServerInitializer(propertyMap));

            Channel ch = b.bind(24062).sync().channel();


            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private PropertyMap propertyMap;
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();
        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast(new MapRequestHandler(propertyMap));
    }

    public ServerInitializer(PropertyMap p) {
        this.propertyMap = p;
    }
}


class MapRequestHandler extends SimpleChannelInboundHandler<Object> {
    private PropertyMap propertyMap;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest){
            HttpRequest request = (HttpRequest) msg;

            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());

            int z = Integer.decode(decoder.parameters().get("z").get(0));
            int mult = (int)Math.pow(2, PropertyMap.default_zoom - z);
            int x = mult*Integer.decode(decoder.parameters().get("x").get(0));
            int y = mult*Integer.decode(decoder.parameters().get("y").get(0));
            JSONObject answer = new JSONObject();

            Way w = propertyMap.findShapeByPoint(new MapPoint(x, y));
            if (w != null) {
                answer.put("status", "success");
                answer.put("status", "success");
                answer.put("data", w.data);
                answer.put("id", w.id);
                answer.put("zoom_level", PropertyMap.default_zoom);
                JSONArray points = new JSONArray();
                for (MapPoint p: w.nodes){
                    JSONArray point = new JSONArray(); point.add(p.x); point.add(p.y);
                    points.add(point);
                }
                answer.put("points", points);
            }else{
                answer.put("status", "not found");
            }
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    MapRequestHandler(PropertyMap p){
        propertyMap = p;
    }
}
