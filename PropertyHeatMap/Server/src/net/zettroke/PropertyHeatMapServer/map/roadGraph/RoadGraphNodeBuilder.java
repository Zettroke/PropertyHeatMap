package net.zettroke.PropertyHeatMapServer.map.roadGraph;

import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.utils.IntArrayList;

import java.util.ArrayList;

public class RoadGraphNodeBuilder extends MapPoint {
    protected RoadGraphNode rgn;
    ArrayList<RoadGraphNode> connectionsCar = new ArrayList<>();
    ArrayList<RoadGraphNode> connectionsFoot = new ArrayList<>();
    IntArrayList distancesCar = new IntArrayList();
    IntArrayList distancesFoot = new IntArrayList();



    public RoadGraphNodeBuilder(Node n){
        rgn = new RoadGraphNode(n);
        x = n.x;
        y = n.y;
    }

    public RoadGraphNode getRoadGraphNode() {

        rgn.distancesTo = new int[2][];
        rgn.distancesTo[0] = distancesFoot.toArray();
        rgn.distancesTo[1] = distancesCar.toArray();

        rgn.ref_to = new RoadGraphNode[2][];
        rgn.ref_to[0] = connectionsFoot.toArray(new RoadGraphNode[connectionsFoot.size()]);
        rgn.ref_to[1] = connectionsCar.toArray(new RoadGraphNode[connectionsCar.size()]);

        return rgn;
    }



}