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
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

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

        int[] max_depth = new int[1];
        int[] sum_depth = new int[1];
        int[] count = new int[1];

        rec(max_depth, sum_depth, count, propertyMap.tree.root);

        System.out.println("Max depth is " + max_depth[0] + ". Average depth is " + sum_depth[0]/(double)count[0]);

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

    public void rec(int[] max_depth, int[] sum_depth, int[] count, QuadTreeNode treeNode){
        if (treeNode.isEndNode){
            if (treeNode.depth > max_depth[0]) {
                max_depth[0] = treeNode.depth;
            }
            sum_depth[0] += treeNode.depth;
            count[0]++;
        }else{
            for (QuadTreeNode tn: treeNode){
                rec(max_depth, sum_depth, count, tn);
            }
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
        p.addLast(new RouteHandler(PathRouter.getPathRouter(propertyMap)));

    }

    public ServerInitializer(PropertyMap p) {
        this.propertyMap = p;
    }
}


