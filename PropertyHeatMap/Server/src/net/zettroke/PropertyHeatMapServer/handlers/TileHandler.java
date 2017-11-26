package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.MapShape;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.QuadTreeNode;
import net.zettroke.PropertyHeatMapServer.utils.Apartment;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

public class TileHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;
    double coefficent = 1;

    static String path = "tile";

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
        coefficent = 1.0/mult;

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, 256, 256);

        QuadTreeNode treeNode = new QuadTreeNode(new int[]{x*mult*256, y*mult*256, (x+1)*mult*256, (y+1)*mult*256});

        propertyMap.fillTreeNode(treeNode);

        drawTreeNode(g, treeNode, 256*x, 256*y);

        ByteBuf buf = ctx.alloc().buffer();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buf);
        ImageIO.write(image, "png", outputStream);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    int coef(int n){
        return (int)Math.round(coefficent*n);
    }

    void drawTreeNode(Graphics2D g, QuadTreeNode treeNode, int x_offset, int y_offset){
        double dist = propertyMap.max_price_per_metr - propertyMap.min_price_per_metr;
        for (MapShape mh: treeNode.shapes){
            if (mh.isPoly && mh.way.data.containsKey("building")) {
                    g.setStroke(new BasicStroke(1f));
                    Polygon poly = new Polygon();
                    for (MapPoint p : mh.points) {
                        poly.addPoint(coef(p.x)-x_offset, coef(p.y)-y_offset);
                    }
                    if (mh.way.apartments != null) {
                        double av_price = Double.MAX_VALUE;
                        for (Apartment a: mh.way.apartments){
                            av_price = Math.min(a.price/a.area, av_price);
                        }
                        //av_price = av_price / mh.way.apartments.size();

                        float color = (float) (((av_price - propertyMap.min_price_per_metr) / dist)*(24.0/36.0));
                        Color clr = Color.getHSBColor(color, 1.0f, 1.0f);

                        g.setColor(new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), 128));
                    }else{
                        g.setColor(new Color(255,255, 255, 0));
                    }
                    g.fill(poly);
                //g.setColor(new Color(0, 0, 0));
                //g.draw(poly);
            }


        }
    }

    public TileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }


}
