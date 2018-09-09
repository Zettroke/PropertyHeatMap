package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MapLoader {
    void load(RoadGraphBuilder builder) throws Exception;

    int[] getCoordBounds() throws Exception;

    HashMap<Long, Node> getNodes() throws Exception;

    ArrayList<SimpleNode> getSimpleNodes() throws Exception;

    HashMap<Long, Way> getWays();

    HashMap<Long, Relation> getRelations() throws Exception;

    HashMap<String, Way> getSearchStrings() throws Exception;

}
