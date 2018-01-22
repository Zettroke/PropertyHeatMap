package net.zettroke.PropertyHeatMapServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.handlers.PathRouter;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.PropertyMapLoaderOSM;
import net.zettroke.PropertyHeatMapServer.handlers.*;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;

import java.io.File;

public class PropertyMapServer {
    String map_name;
    PropertyMapServer(String map){
        map_name = map;
    }

    public void start() throws Exception{
        PropertyMap propertyMap = new PropertyMap(new PropertyMapLoaderOSM(map_name));
        //PropertyMapLoaderOSM.load(propertyMap, new File(map_name));
        //PropertyMapLoaderOSM.load(propertyMap, new File("C:/PropertyHeatMap/map.osm"));
        long start = System.nanoTime();
        propertyMap.init();
        //propertyMap.init();

        System.out.println("Init in " + (System.nanoTime()-start)/1000000.0 + " millis.");

        int[] max_shapes = new int[1];
        int[] sum_shapes = new int[1];
        int[] count_shapes = new int[1];

        rec(max_shapes, sum_shapes, count_shapes, propertyMap.tree.root);

        System.out.println("Max num of shapes in node " + max_shapes[0] + ". Average num of shapes is " + sum_shapes[0]/(double)count_shapes[0]);

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

    public void rec(int[] max_shapes, int[] sum_shapes, int[] count, QuadTreeNode treeNode){
        if (treeNode.isEndNode){
            if (treeNode.shapes.size() > max_shapes[0]) {
                max_shapes[0] = treeNode.shapes.size();
            }
            sum_shapes[0] += treeNode.shapes.size();
            count[0]++;
        }else{
            for (QuadTreeNode tn: treeNode){
                rec(max_shapes, sum_shapes, count, tn);
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


