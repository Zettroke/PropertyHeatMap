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

public class PriceTileHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;
    double coefficent = 1;

    final String path = "tile/price";
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
        int price = Integer.decode(decoder.parameters().get("price").get(0));
        double range = Double.parseDouble(decoder.parameters().get("range").get(0));


        coefficent = 1.0/mult;

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, 256, 256);

        QuadTreeNode treeNode = new QuadTreeNode(new int[]{x*mult*256, y*mult*256, (x+1)*mult*256, (y+1)*mult*256}, false);

        propertyMap.fillTreeNode(treeNode);


        drawTreeNode(g, treeNode, 256*x, 256*y, price, range);


        ByteBuf buf = ctx.alloc().buffer();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buf);
        ImageIO.write(image, "png", outputStream);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    int coef(int n){
        return (int)Math.round(coefficent*n);
    }

    void drawTreeNode(Graphics2D g, QuadTreeNode treeNode, int x_offset, int y_offset, int price, double range){
        double dist = price*range*2;
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (MapShape mh: treeNode.shapes){
            if (mh.isPoly && mh.way.data.containsKey("building")) {
                    g.setStroke(new BasicStroke(1f));
                    Polygon poly = new Polygon();
                    for (MapPoint p : mh.points) {
                        poly.addPoint(coef(p.x)-x_offset, coef(p.y)-y_offset);
                    }
                    if (mh.way.apartments != null) {
                        double ap_price = -1000000000000000000000.0;
                        for (Apartment ap: mh.way.apartments){
                            if (Math.abs(price-ap_price) > Math.abs(price-ap.price/ap.area)){
                                ap_price = ap.price/ap.area;
                            }
                        }
                        if (ap_price > price - price*range && ap_price < price+price*range) {
                            float color = (float) ((((price + price * range)-ap_price) / dist) * (24.0 / 36.0));
                            Color clr = Color.getHSBColor(color, 1.0f, 0.95f);

                            g.setColor(new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), 200));


                        }else{

                            g.setColor(new Color(255,255, 255, 0));
                            //g.setColor(new Color(207, 22, 187));
                        }
                    }else{
                        //g.setColor(new Color(207, 22, 187));
                        g.setColor(new Color(255,255, 255, 0));
                    }
                    g.fill(poly);
                    if (g.getColor().getAlpha() != 0) {
                        g.setColor(new Color(g.getColor().getRGB()));
                    }else{
                        g.setColor(new Color(255, 255, 255, 0));
                    }
                    Path2D.Float path2D = new Path2D.Float();

                    MapPoint p1 = mh.points.get(0);
                    MapPoint p2 = mh.points.get(1);
                    path2D.moveTo(coef(p1.x)-x_offset, coef(p1.y)-y_offset);
                    for (int i=2; i<mh.points.size(); i++){
                        if (!treeNode.onBounds(p1) || !treeNode.onBounds(p2)){
                            path2D.lineTo(coef(p2.x)-x_offset, coef(p2.y)-y_offset);
                        }else{
                            path2D.moveTo(coef(p2.x)-x_offset, coef(p2.y)-y_offset);
                        }
                        p1 = p2;
                        p2 = mh.points.get(i);
                    }
                    p1 = mh.points.get(0);
                    p2 = mh.points.get(mh.points.size()-1);
                    if (!treeNode.onBounds(p1) || !treeNode.onBounds(p2)){
                        path2D.lineTo(coef(p2.x)-x_offset, coef(p2.y)-y_offset);
                    }
                    g.draw(path2D);
            }


        }
    }

    public PriceTileHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }


}
