package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;

/**
 * Created by Zettroke on 19.10.2017.
 */
public interface PropertyMapLoader {
    boolean isLoaded = false;

    public void load();

    ArrayList<Node> getNodes();
    ArrayList<Way> getWays();
    ArrayList<Relation> getRelations();

}
