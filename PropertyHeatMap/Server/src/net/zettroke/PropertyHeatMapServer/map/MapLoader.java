package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.map.roadGraph.RoadGraphBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public interface MapLoader {
    void load(RoadGraphBuilder builder, PropertyMap context) throws Exception;

    double[] getDegreesBounds() throws Exception;

    int[] getCoordBounds(PropertyMap context) throws Exception;

    HashMap<Long, Node> getNodes() throws Exception;

    ArrayList<SimpleNode> getSimpleNodes() throws Exception;

    HashMap<Long, Way> getWays();

    HashMap<Long, Relation> getRelations() throws Exception;
}
