package net.zettroke.PropertyHeatMapServer.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Relation implements Serializable{
    long id;
    public ArrayList<Node> nodes = new ArrayList<>();
    public ArrayList<Way> ways = new ArrayList<>();
    public ArrayList<Relation> relations = new ArrayList<>();
    public HashMap<String, String> data = new HashMap<>();
}
