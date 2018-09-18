package net.zettroke.PropertyHeatMapServer.handlers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphNode;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphKey;
import net.zettroke.PropertyHeatMapServer.utils.Jsonizer;
import net.zettroke.PropertyHeatMapServer.utils.ParamsChecker;
import net.zettroke.PropertyHeatMapServer.utils.TimeMeasurer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CloseObjectsHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;
    static final String path = "search/close_objects";

    @Override
    public String getPath() {
        return path;
    }

    final static ThreadLocal<ParamsChecker> pcheckers = new ThreadLocal<ParamsChecker>() {
        @Override
        protected ParamsChecker initialValue() {
            return new ParamsChecker()
                    .addParam("id").type(ParamsChecker.LongType).finish()
                    .addParam("max_dist").type(ParamsChecker.IntegerType).range(0, 36000).finish()
                    .addParam("foot").type(ParamsChecker.BooleanType).finish()
                    .addParam("max_num").type(ParamsChecker.IntegerType).range(1, 1000).finish();
        }
    };


    static class InfrObj{
        Node n;
        int dist;

        public InfrObj(Node n, int dist) {
            this.n = n;
            this.dist = dist;
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ParamsChecker checker = pcheckers.get();
        if (checker.isValid(decoder)) {
            long id = Long.decode(decoder.parameters().get("id").get(0));
            int max_dist = Integer.decode(decoder.parameters().get("max_dist").get(0));
            boolean foot = Boolean.parseBoolean(decoder.parameters().get("foot").get(0));
            int max_num = Integer.decode(decoder.parameters().get("max_num").get(0));

            int ind = propertyMap.getCalculatedGraphIndex(id, foot, max_dist);

            JsonObject answer = new JsonObject();

            JsonArray values = new JsonArray();
            ArrayList<InfrObj> objs = new ArrayList<>();
            for (Map.Entry<Long, RoadGraphNode[]> en : propertyMap.infrastructure_connections.entrySet()) {
                if (propertyMap.nodes.containsKey(en.getKey())) {
                    int min_dist = Integer.MAX_VALUE;
                    for (RoadGraphNode rgn : en.getValue()) {
                        if (rgn.dist[ind] != Integer.MAX_VALUE) {
                            min_dist = Math.min(min_dist, rgn.dist[ind] + 600);
                        }
                    }
                    if (min_dist <= max_dist) {
                        objs.add(new InfrObj(propertyMap.nodes.get(en.getKey()), min_dist));
                    }

                }
            }
            objs.sort(new Comparator<InfrObj>() {
                @Override
                public int compare(InfrObj o1, InfrObj o2) {
                    return o1.dist - o2.dist;
                }
            });


            for (int i = 0; i < Math.min(max_num, objs.size()); i++) {
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
        }else{
            JsonObject ans = new JsonObject();
            ans.add("status", "error");
            ans.add("error", checker.getErrorMessage());
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes(ans.toString().getBytes(Charset.forName("utf-8"))));
            response.headers().set("content-type", "text/json; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

    }

    public CloseObjectsHandler(PropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }
}
