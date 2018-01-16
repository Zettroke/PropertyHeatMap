package net.zettroke.PropertyHeatMapServer.utils;


import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CalculatedGraphCache {

    public static HashMap<CalculatedGraphKey, ReentrantLock> current_processing = new HashMap<>();

    static int max_cache_size = 10;

    static Map<CalculatedGraphKey, HashMap<Long, RoadGraphNode>> storage = Collections.synchronizedMap(new LinkedHashMap<CalculatedGraphKey, HashMap<Long, RoadGraphNode>>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > max_cache_size;
        }
    });



    public static HashMap<Long, RoadGraphNode> get(long id, boolean foot, int max_dist){
        CalculatedGraphKey key = new CalculatedGraphKey(id, foot, max_dist);
        HashMap<Long, RoadGraphNode> res = storage.get(key);
        storage.put(key, res);
        return res;
    }

    public synchronized static void store(long id, boolean foot, int max_dist, HashMap<Long, RoadGraphNode> graph){
        CalculatedGraphKey key = new CalculatedGraphKey(id, foot, max_dist);
        storage.put(key, graph);
    }

    public static boolean contain(long id, boolean foot, int max_dist){
        return storage.containsKey(new CalculatedGraphKey(id, foot, max_dist));
    }

}


