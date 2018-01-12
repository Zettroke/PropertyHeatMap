package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

public class DrawerHandler implements ShittyHttpHandler{
    final static Font font = new Font("Arial", Font.BOLD, 15);
    static String path = "draw";
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        String text = queryStringDecoder.parameters().get("text").get(0);
        List<String> lw = queryStringDecoder.parameters().get("w");
        List<String> lh = queryStringDecoder.parameters().get("h");
        Integer w;
        Integer h;
        if (lw == null || lh == null){
            w = 256; h = 256;
        }else{
            w = Integer.parseInt(lw.get(0));
            h = Integer.parseInt(lh.get(0));
        }


        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(2f));

        g.setColor(new Color(209, 45, 65));
        for (int i = 0; i < Math.min(w, h)/2-16; i+=8){
            g.drawRect(i, i, w-2*i, h-2*i);
            if (i/8 % 2 == 0){
                g.setColor(new Color(18, 78, 209));
            }else{
                g.setColor(new Color(209, 45, 65));
            }
        }

        ImageIO.setUseCache(false);


        g.setColor(new Color(0, 0, 0));
        g.setFont(font);

        FontMetrics fontMetrics = g.getFontMetrics(font);
        g.drawString(text, (w-fontMetrics.stringWidth(text))/2, (h+fontMetrics.getHeight()/2)/2);

        ByteBuf buf = ctx.alloc().buffer();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buf);

        ImageIO.write(image, "png", outputStream);

        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
