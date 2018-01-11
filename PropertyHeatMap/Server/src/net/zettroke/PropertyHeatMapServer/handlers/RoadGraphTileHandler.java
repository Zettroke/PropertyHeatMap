package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphCache;
import net.zettroke.PropertyHeatMapServer.utils.RoadType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class RoadGraphTileHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;

    static String path = "tile/road";

    double coefficent = 1;

    int coef(int n){
        return (int)Math.round(coefficent*n);
    }

    int max_dist = 0;

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        int z = Integer.decode(decoder.parameters().get("z").get(0));
        int mult = (int) Math.pow(2, PropertyMap.default_zoom - z);
        int x = Integer.decode(decoder.parameters().get("x").get(0));
        int y = Integer.decode(decoder.parameters().get("y").get(0));
        long start_id = Long.decode(decoder.parameters().get("start_id").get(0));
        int max_dist =  Integer.decode(decoder.parameters().get("max_dist").get(0));

        coefficent = 1.0/mult;
        max_dist = max_dist;

        QuadTreeNode treeNode = new QuadTreeNode(new int[]{(x-1)*mult*256, (y-1)*mult*256, (x+2)*mult*256, (y+2)*mult*256});
        BufferedImage imageTemp = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);



        Graphics2D g = (Graphics2D) imageTemp.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        HashMap<Long, RoadGraphNode> graph;

        if (CalculatedGraphCache.contain(start_id, max_dist)){
            graph = CalculatedGraphCache.get(start_id, max_dist);
            //System.out.println("get form cache");
        }else {
            graph = propertyMap.getCalculatedRoadGraph(start_id, new HashSet<>(Arrays.asList(RoadType.FOOTWAY,
                    RoadType.CONSTRUCTION, RoadType.SERVICE, RoadType.PATH)), max_dist);
            CalculatedGraphCache.store(start_id, max_dist, graph);
            //System.out.println("stored");
        }
        ArrayList<RoadGraphNode> to_clear = new ArrayList<>();
        int offx = x*mult*256;
        int offy = y*mult*256;
        g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (RoadGraphNode rgn : graph.values()){
            //if (rgn.dist != Integer.MAX_VALUE){
            if (treeNode.inBounds(rgn.n)){
                to_clear.add(rgn);
                rgn.visited = true;
                for (int i=0; i<rgn.ref_to.length; i++){
                    RoadGraphNode ref = rgn.ref_to[i];
                    if (!ref.visited){
                        switch (rgn.ref_types.get(i)){

                            case SECONDARY:
                                g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case RESIDENTIAL:
                                g.setStroke(new BasicStroke(50f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case SERVICE:
                                g.setStroke(new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case TERTIARY:
                                g.setStroke(new BasicStroke(60f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case PRIMARY:
                                g.setStroke(new BasicStroke(75f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case TRUNK:
                                g.setStroke(new BasicStroke(80f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case DEFAULT:
                                g.setStroke(new BasicStroke(20f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                            case LIVING_STREET:
                                g.setStroke(new BasicStroke(25f/mult, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                                break;
                        }
                        g.setPaint(new GradientPaint(coef(rgn.n.x-offx), coef(rgn.n.y-offy), rgn.getNodeColor(max_dist), coef(ref.n.x-offx), coef(ref.n.y-offy), ref.getNodeColor(max_dist)));
                        g.drawLine(coef(rgn.n.x-offx), coef(rgn.n.y-offy), coef(ref.n.x-offx), coef(ref.n.y-offy));
                    }
                }
            }
        }
        for (RoadGraphNode rgn: to_clear) {
            rgn.visited = false;
        }

        ByteBuf buf = ctx.alloc().buffer();
        ByteBufOutputStream out = new ByteBufOutputStream(buf);
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = (Graphics2D)image.getGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2.drawImage(imageTemp, 0, 0, null);

        ImageIO.write(image, "png", out);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    public RoadGraphTileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }



}
