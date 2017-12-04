package net.zettroke.PropertyHeatMapServer.map;

import java.util.ArrayList;
import java.util.HashSet;

public class RoadGraphNode {
    Node n;
    int dist = Integer.MAX_VALUE;
    //ArrayList<RoadGraphNode> ref_to = new ArrayList<>();
    RoadGraphNode[] ref_to;
    //ArrayList<Integer> distances = new ArrayList<>();
    int[] distances;
    public HashSet<String> road_types = new HashSet<>();
    boolean visited = false;

    RoadGraphNode(Node n){
        this.n = n;
    }

    public void addWay(Way way){
        road_types.add(way.data.get("highway"));
    }

    public RoadGraphNode clone(){
        RoadGraphNode res = new RoadGraphNode(n);
        res.road_types = road_types;
        return res;
    }
}
