package net.zettroke.PropertyHeatMapServer;

import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Node {
    long id;
    public double x;
    public double y;

    public double lon;
    public double lat;

    HashMap<String, String> data = new HashMap<>();
    boolean isSimple;
}
