package net.zettroke.PropertyHeatMapServer.map.roadGraph;

import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.RoadType;

import java.util.HashMap;
import java.util.Map;

public class RoadGraphBuilder {
    int[] bounds;
    HashMap<Long, RoadGraphNodeBuilder> nodes = new HashMap<>();
    TinyQuadTree nodesTree;
    int cache_size;

    public RoadGraphBuilder(int[] bounds, int cache_size){
        this.bounds = bounds;
        nodesTree = new TinyQuadTree(bounds);
        this.cache_size = cache_size;
    }

    public void add(Node n){
        if (!nodes.containsKey(n.id)){
            RoadGraphNodeBuilder b = new RoadGraphNodeBuilder(n);
            nodes.put(n.id, b);
            nodesTree.add(b);
        }
    }

    public boolean isRGN(Node n){
        return nodes.containsKey(n.id);
    }

    public void connect(Node n1, Node n2, RoadType connectType){
        int dist = PropertyMap.calculateDistance(n1, n2);
        connect(n1, n2, connectType, dist);
    }

    public void connect(Node n1, Node n2, RoadType connectType, int dist){
        RoadGraphNodeBuilder b1;
        if (!nodes.containsKey(n1.id)){
            b1 = new RoadGraphNodeBuilder(n1);
            nodes.put(n1.id, b1);
            nodesTree.add(b1);
        }else{
            b1 = nodes.get(n1.id);
        }

        RoadGraphNodeBuilder b2;
        if (!nodes.containsKey(n2.id)){
            b2 = new RoadGraphNodeBuilder(n2);
            nodes.put(n2.id, b2);
            nodesTree.add(b2);
        }else{
            b2 = nodes.get(n2.id);
        }

        connect(b1, b2, connectType, dist);
    }

    private void connect(RoadGraphNodeBuilder rgn1, RoadGraphNodeBuilder rgn2, RoadType connType, int dist){
        if (!RoadGraphNode.car_exclude.contains(connType)){
            rgn1.connectionsCar.add(rgn2.rgn);
            rgn2.connectionsCar.add(rgn1.rgn);

            rgn1.distancesCar.add(Math.round(dist/ PropertyMap.car_speed));
            rgn2.distancesCar.add(Math.round(dist/PropertyMap.car_speed));

            rgn1.roadTypesCar.add(connType);
            rgn2.roadTypesCar.add(connType);
        }
        if (!RoadGraphNode.foot_exclude.contains(connType)){
            rgn1.connectionsFoot.add(rgn2.rgn);
            rgn2.connectionsFoot.add(rgn1.rgn);
            float divider;
            switch (connType){
                case SUBWAY:
                    divider = PropertyMap.subway_speed;
                    break;
                case BUS:
                    divider = PropertyMap.bus_speed;
                    break;
                case TRAM:
                    divider = PropertyMap.tram_speed;
                    break;
                case TROLLEYBUS:
                    divider = PropertyMap.bus_speed;
                    break;
                default:
                    divider = PropertyMap.foot_speed;
                    break;
            }
            rgn1.distancesFoot.add(Math.round(dist/divider));
            rgn2.distancesFoot.add(Math.round(dist/divider));

            rgn1.roadTypesFoot.add(connType);
            rgn2.roadTypesFoot.add(connType);
        }
    }

    public void connectNodeToNodesInRadius(Node n, int radius, RoadType roadType){
        for (RoadGraphNodeBuilder b: nodesTree.findRgnBuildersInCircle(n, radius)){
            connect(n, b.rgn.n, roadType);
        }
    }

    public HashMap<Long, RoadGraphNode> getRoadGraph(){
        HashMap<Long, RoadGraphNode> res = new HashMap<>();

        for (Map.Entry<Long, RoadGraphNodeBuilder> entry: nodes.entrySet()){
            res.put(entry.getKey(), entry.getValue().getRoadGraphNode(cache_size));
        }

        return res;
    }
}
