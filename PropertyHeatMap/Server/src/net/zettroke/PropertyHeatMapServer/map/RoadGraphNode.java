package net.zettroke.PropertyHeatMapServer.map;

import java.util.ArrayList;
import java.util.HashSet;

public class RoadGraphNode {
    Node n;
    int dist = Integer.MAX_VALUE;
    //ArrayList<RoadGraphNode> ref_to = new ArrayList<>();
    RoadGraphNode[] ref_to;
    //ArrayList<Integer> distances = new ArrayList<>();
    Integer[] distances;
    public HashSet<String> road_types = new HashSet<>();
    public HashSet<PropertyMap.RoadTypes> types = new HashSet<>();
    boolean visited = false;

    RoadGraphNode(Node n){
        this.n = n;
    }

    public void addWay(Way way){
        if (way.data.containsKey("living_street")){
            types.add(PropertyMap.RoadTypes.LIVING_STREET);
        }
        String s = way.data.get("highway");
        if (s.equals("footway")){
            types.add(PropertyMap.RoadTypes.FOOTWAY);
            return;
        }
        road_types.add(s);
    }

    public RoadGraphNode clone(){
        RoadGraphNode res = new RoadGraphNode(n);
        res.road_types = road_types;
        return res;
    }
}
