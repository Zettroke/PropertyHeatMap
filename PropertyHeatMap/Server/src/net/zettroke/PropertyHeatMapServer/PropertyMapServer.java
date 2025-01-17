package net.zettroke.PropertyHeatMapServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.handlers.PathRouter;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.MapLoaderOSM;
import net.zettroke.PropertyHeatMapServer.handlers.*;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.utils.IndexedThreadFactory;

public class PropertyMapServer {

    public static int PROC_NUM = 8;

    PropertyMap propertyMap;


    PropertyMapServer(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }

    public void start() throws Exception{

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
        System.out.println("init finished");
        EventLoopGroup bossGroup = new NioEventLoopGroup(PROC_NUM);
        EventLoopGroup workerGroup = new NioEventLoopGroup(PROC_NUM, new IndexedThreadFactory());
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
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline p = socketChannel.pipeline();

        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        p.addLast(new RouteHandler(PathRouter.getPathRouter(propertyMap)));

    }

    public ServerInitializer(PropertyMap p) {
        this.propertyMap = p;
    }
}


