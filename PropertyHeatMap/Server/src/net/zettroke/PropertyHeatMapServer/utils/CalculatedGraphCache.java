package net.zettroke.PropertyHeatMapServer.utils;


import net.zettroke.PropertyHeatMapServer.map.RoadGraphNode;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class CalculatedGraphCache {
    static LinkedList<CalcutatedGraphKey> key_order = new LinkedList<>();
    static ConcurrentHashMap<CalcutatedGraphKey, HashMap<Long, RoadGraphNode>> storage = new ConcurrentHashMap<>();
    static int access_counter = 0;
    static int max_cache_size = 100;

    public static HashMap<Long, RoadGraphNode> get(long id, int max_dist){
        /*access_counter++;

        if (access_counter > 2000) {
            synchronized (storage) {
                long curr_time = new Date().getTime();
                if (curr_time - key_order.get(0).accessed > 120000) {
                    for (int i = 0; i < key_order.size(); i++){
                        if (curr_time - key_order.get(i).accessed > 120000){
                            storage.remove(key_order.get(i));
                            key_order.remove(i);
                            i--;
                        }else{
                            break;
                        }
                    }
                }
                access_counter = 0;
            }
        }*/
        return storage.get(new CalcutatedGraphKey(id, max_dist));
    }

    public static void store(long id,int max_dist, HashMap<Long, RoadGraphNode> graph){
        storage.put(new CalcutatedGraphKey(id,max_dist), graph);
    }

    public static boolean contain(long id, int max_dist){
        return storage.containsKey(new CalcutatedGraphKey(id,max_dist));
    }

}
class CalcutatedGraphKey {
    long id;
    int max_dist;

    public CalcutatedGraphKey(long id, int max_dist) {
        this.id = id;
        this.max_dist = max_dist;
    }

    @Override
    public int hashCode() {
        return (int)(id % max_dist * id);
    }

    @Override
    public boolean equals(Object obj) {
        return id == ((CalcutatedGraphKey)obj).id && max_dist == ((CalcutatedGraphKey)obj).max_dist;
    }
}


