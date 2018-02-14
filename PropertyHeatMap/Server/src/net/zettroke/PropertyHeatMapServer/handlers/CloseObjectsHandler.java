package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import javafx.util.Pair;
import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphKey;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.TimeMeasurer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public class CloseObjectsHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;
    static final String path = "search/close_objects";

    @Override
    public String getPath() {
        return path;
    }


    static class InfrObj{
        Node n;
        int dist;

        public InfrObj(Node n, int dist) {
            this.n = n;
            this.dist = dist;
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        long id = Long.decode(decoder.parameters().get("id").get(0));
        int max_dist =  Integer.decode(decoder.parameters().get("max_dist").get(0));
        boolean foot = Boolean.parseBoolean(decoder.parameters().get("foot").get(0));
        int max_num = Integer.MAX_VALUE;
        if (decoder.parameters().get("max_num").size() != 0) {
            max_num = Integer.decode(decoder.parameters().get("max_num").get(0));
        }
        int ind;
        propertyMap.cache.lock.lock();
        long start = System.nanoTime();
        CalculatedGraphKey key = new CalculatedGraphKey(id, foot, max_dist);
        if (propertyMap.cache.contains(key)){
            ind = propertyMap.cache.getCachedIndex(key);
        }else {
            ind = propertyMap.cache.getNewIndexForGraph(key);
            propertyMap.calcRoadGraph(id, true, max_dist, ind);
            TimeMeasurer.printMeasure("Graph calculated in %t millis.", start);
        }
        propertyMap.cache.lock.unlock();
        JsonObject answer = new JsonObject();

        JsonArray values = new JsonArray();
        ArrayList<InfrObj> objs = new ArrayList<>();
        for (Map.Entry<Long, RoadGraphNode[]> en: propertyMap.infrastructure_connections.entrySet()){
            if (propertyMap.nodes.containsKey(en.getKey())){
                int min_dist = Integer.MAX_VALUE;
                for (RoadGraphNode rgn: en.getValue()){
                    if (rgn.dist[ind] != Integer.MAX_VALUE){
                        min_dist = Math.min(min_dist, rgn.dist[ind]+600);
                    }
                }
                if (min_dist <= max_dist){
                    objs.add(new InfrObj(propertyMap.nodes.get(en.getKey()), min_dist));
                }

            }
        }
        objs.sort(new Comparator<InfrObj>() {
            @Override
            public int compare(InfrObj o1, InfrObj o2) {
                return o1.dist-o2.dist;
            }
        });


        for (int i=0; i<Math.min(max_num, objs.size()); i++){
            JsonObject node = Jsonizer.toJson(objs.get(i).n);
            node.add("dist", objs.get(i).dist);
            values.add(node);
        }
        answer.add("status", "success");
        answer.add("objects", values);

        ByteBuf buf = ctx.alloc().buffer().writeBytes(answer.toString().getBytes(Charset.forName("utf-8")));
        //System.out.println("handled string search");
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set("content-type", "text/json; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    public CloseObjectsHandler(PropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }
}
