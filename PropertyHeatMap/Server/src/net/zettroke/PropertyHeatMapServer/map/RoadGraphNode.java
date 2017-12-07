package net.zettroke.PropertyHeatMapServer.map;

import java.awt.*;
import java.util.HashSet;

public class RoadGraphNode {
    public Node n;
    public int dist = Integer.MAX_VALUE;
    public RoadGraphNode[] ref_to;
    public Integer[] distances;
    public HashSet<String> road_types = new HashSet<>();
    public HashSet<PropertyMap.RoadTypes> types = new HashSet<>();
    public boolean visited = false;

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
        }else if(s.equals("construction")){
            types.add(PropertyMap.RoadTypes.CONSTRUCTION);
        }
        road_types.add(s);
    }

    public RoadGraphNode clone(){
        RoadGraphNode res = new RoadGraphNode(n);
        res.road_types = road_types;
        return res;
    }

    public Color getNodeColor(int max_dist){
        if (this.dist <= max_dist) {

            Color cl = Color.getHSBColor((float)((1-this.dist/(double)max_dist)*120.0/360.0), 0.9f, 0.9f);
            return new Color(cl.getRed(), cl.getGreen(), cl.getBlue());

        }else{
            return new Color(168, 0, 22);
        }
    }
}
