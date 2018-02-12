package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.utils.CalculatedGraphKey;
import net.zettroke.PropertyHeatMapServer.utils.TimeMeasurer;

public class CloseObjectsHandler implements ShittyHttpHandler{

    PropertyMap propertyMap;
    static final String path = "";

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        long start_id = Long.decode(decoder.parameters().get("start_id").get(0));
        int max_dist =  Integer.decode(decoder.parameters().get("max_dist").get(0));
        boolean foot = Boolean.parseBoolean(decoder.parameters().get("foot").get(0));
        int ind;
        propertyMap.cache.lock.lock();
        long start = System.nanoTime();
        CalculatedGraphKey key = new CalculatedGraphKey(start_id, foot, max_dist);
        if (propertyMap.cache.contains(key)){
            ind = propertyMap.cache.getCachedIndex(key);
        }else {
            ind = propertyMap.cache.getNewIndexForGraph(key);
            propertyMap.calcRoadGraph(start_id, true, max_dist, ind);
            TimeMeasurer.printMeasure("Graph calculated in %t millis.", start);
        }
        propertyMap.cache.lock.unlock();


    }

    public CloseObjectsHandler(PropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }
}
