package net.zettroke.PropertyHeatMapServer.utils;


import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphNode;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class CalculatedGraphCache {

    public ReentrantLock lock = new ReentrantLock();
    public HashMap<CalculatedGraphKey, ReentrantLock> loading= new HashMap<>();
    HashMap<Long, RoadGraphNode> roadGraph;
    LinkedHashMap<CalculatedGraphKey, Integer> cached = new LinkedHashMap<CalculatedGraphKey, Integer>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<CalculatedGraphKey, Integer> eldest) {
            if (free_indexes.size() == 0){
                free_indexes.add(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    public ArrayList<Integer> free_indexes = new ArrayList<>();
    public int capacity = 0;

    public CalculatedGraphCache(HashMap<Long, RoadGraphNode> roadGraph, int initialCapacity){
        this.roadGraph = roadGraph;
        capacity = initialCapacity;
        for (int i=0; i<initialCapacity; i++){
            free_indexes.add(i);
        }
    }

    public synchronized int getNewIndexForGraph(CalculatedGraphKey key){
        int ind = free_indexes.get(free_indexes.size()-1);
        free_indexes.remove(free_indexes.size()-1);
        cached.put(key, ind);
        for (RoadGraphNode rgn: roadGraph.values()){
            rgn.dist[ind] = Integer.MAX_VALUE;
        }
        return ind;
    }

    public int getCachedIndex(CalculatedGraphKey key){
        return cached.get(key);

    }
    public boolean contains(CalculatedGraphKey key){
        return cached.containsKey(key);
    }

}


