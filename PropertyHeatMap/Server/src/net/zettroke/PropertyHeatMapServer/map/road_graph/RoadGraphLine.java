package net.zettroke.PropertyHeatMapServer.map.road_graph;

import net.zettroke.PropertyHeatMapServer.map.RoadType;

public class RoadGraphLine {
    public RoadGraphNode n1, n2;
    public RoadType type;

    public RoadGraphLine(RoadGraphNode n1, RoadGraphNode n2, RoadType type) {
        this.n1 = n1;
        this.n2 = n2;
        this.type = type;
    }
}
