package net.zettroke.PropertyHeatMapServer.map;


import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MapLoader {
    void load(RoadGraphBuilder builder) throws Exception;

    int[] getCoordBounds() throws Exception;

    Map<Long, Node> getNodes() throws Exception;

    List<SimpleNode> getSimpleNodes() throws Exception;

    Map<Long, Way> getWays();

    Map<Long, Relation> getRelations() throws Exception;

    Map<String, Way> getSearchStrings() throws Exception;

}
