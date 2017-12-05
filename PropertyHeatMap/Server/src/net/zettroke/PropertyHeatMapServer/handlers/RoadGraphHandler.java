package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphCache;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class RoadGraphHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;

    static String path = "tile/road_graph";

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

        HashMap<Long, RoadGraphNode> graph;
        if (CalculatedGraphCache.contain(start_id, max_dist)){
            graph = CalculatedGraphCache.get(start_id, max_dist);
        }else{
            graph = propertyMap.getCalculatedRoadGraph(start_id, new HashSet<>(Arrays.asList(PropertyMap.RoadTypes.FOOTWAY, PropertyMap.RoadTypes.CONSTRUCTION)));
            CalculatedGraphCache.store(start_id, max_dist, graph);
        }



    }

    RoadGraphHandler(PropertyMap propertyMap){
        this.propertyMap = propertyMap;
    }

    void depth_graph_draw(RoadGraphNode n, Graphics2D g){
        n.visited = true;
        for (int i=0; i<n.ref_to.length; i++){
            RoadGraphNode n1 = n.ref_to[i];
            boolean flag = true;
            if (n.dist == Integer.MAX_VALUE){
                g.setColor(Color.RED);
                g.fillRect(coef(n.n.x)-10, coef(n.n.y)-10, 20, 20);
            }
            g.setPaint(new GradientPaint(coef(n.n.x), coef(n.n.y), getNodeColor(n, max_dist), coef(n1.n.x), coef(n1.n.y), getNodeColor(n1, max_dist)));
            //g.setColor(Color.BLACK);
            if (n.road_types.contains("secondary") && n1.road_types.contains("secondary")){
                g.setStroke(new BasicStroke(15, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                //g.setColor(new Color(247, 250, 190));
            }else if (n.road_types.contains("residential") && n1.road_types.contains("residential")) {
                //g.setColor(new Color(0, 0, 0));
                g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else if(n.road_types.contains("footway") && n1.road_types.contains("footway")){
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                //flag = false;
                //continue;
                //} else if(n.road_types.contains("service") && n1.road_types.contains("service")){
                //flag = false;
            }else{
                //g.setColor(new Color(0, 0, 0));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            if (flag) {
                g.drawLine(coef(n.n.x), coef(n.n.y), coef(n1.n.x), coef(n1.n.y));
            }
            if (!n1.visited) {
                depth_graph_draw(n1, g);
            }
        }
    }
    Color getNodeColor(RoadGraphNode n, int max_dist){
        if (n.dist <= max_dist) {

            /*int r = (int) (255 * n.dist / (double) max_dist);
            int g = 255 - (int) (255 * (n.dist / (double) max_dist));
            return new Color(r, g, 0);*/
            return Color.getHSBColor((float)((1-n.dist/(double)max_dist)*120.0/360.0), 0.9f, 0.9f);

        }else{
            return new Color(168, 0, 22);
        }
    }


}
