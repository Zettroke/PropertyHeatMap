package net.zettroke.PropertyHeatMapServer.utils;


import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CalculatedGraphCache {

    static int max_cache_size = 100;

    static Map<CalculatedGraphKey, HashMap<Long, RoadGraphNode>> storage = Collections.synchronizedMap(new LinkedHashMap<CalculatedGraphKey, HashMap<Long, RoadGraphNode>>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {

            return this.size() > max_cache_size;
        }
    });



    public static HashMap<Long, RoadGraphNode> get(long id, int max_dist){
        CalculatedGraphKey key = new CalculatedGraphKey(id, max_dist);
        HashMap<Long, RoadGraphNode> res = storage.get(key);
        storage.put(key, res);
        return res;
    }

    public synchronized static void store(long id,int max_dist, HashMap<Long, RoadGraphNode> graph){
        CalculatedGraphKey key = new CalculatedGraphKey(id,max_dist);
        storage.put(key, graph);
    }

    public static boolean contain(long id, int max_dist){
        return storage.containsKey(new CalculatedGraphKey(id,max_dist));
    }

}
class CalculatedGraphKey {
    long id;
    int max_dist;

    public CalculatedGraphKey(long id, int max_dist) {
        this.id = id;
        this.max_dist = max_dist;
    }

    @Override
    public int hashCode() {
        return (int)(id + max_dist ^ id);
    }

    @Override
    public boolean equals(Object obj) {
        return id == ((CalculatedGraphKey)obj).id && max_dist == ((CalculatedGraphKey)obj).max_dist;
    }
}


