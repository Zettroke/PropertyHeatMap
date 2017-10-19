package net.zettroke.PropertyHeatMapServer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Relation {
    long id;
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Way> ways = new ArrayList<>();
    ArrayList<Relation> relations = new ArrayList<>();
    HashMap<String, String> data = new HashMap<>();
}
