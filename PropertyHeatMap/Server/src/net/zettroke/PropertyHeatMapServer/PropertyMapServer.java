package net.zettroke.PropertyHeatMapServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.handlers.ErrorHandler;
import net.zettroke.PropertyHeatMapServer.handlers.PathRouter;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.PropertyMapLoaderOSM;
import net.zettroke.PropertyHeatMapServer.handlers.*;

import java.io.File;

public class PropertyMapServer {
    String map_name;
    PropertyMapServer(String map){
        map_name = map;
    }

    public void start() throws Exception{
        PropertyMap propertyMap = new PropertyMap();
        PropertyMapLoaderOSM.load(propertyMap, new File(map_name));
        //PropertyMapLoaderOSM.load(propertyMap, new File("C:/PropertyHeatMap/map.osm"));
        long start = System.nanoTime();
        propertyMap.initParallel();
        //propertyMap.init();

        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //.handler(new LoggingHandler(LogLevel.INFO))
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

    PathRouter getPathRouter(){
        PathRouter pathRouter = new PathRouter();
        pathRouter.addPath(new MapPointSearchHandler(propertyMap));
        pathRouter.addPath(new DrawerHandler());
        pathRouter.addPath(new MapCircleSearchHandler(propertyMap));
        pathRouter.addPath(new PriceTileHandler(propertyMap));
        pathRouter.setErrorHandler(new ErrorHandler());

        return pathRouter;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();

        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast(new RouteHandler(getPathRouter()));

    }

    public ServerInitializer(PropertyMap p) {
        this.propertyMap = p;
    }
}


