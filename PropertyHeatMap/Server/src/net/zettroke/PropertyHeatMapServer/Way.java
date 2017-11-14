package net.zettroke.PropertyHeatMapServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Way implements Serializable{
    long id;
    ArrayList<SimpleNode> nodes = new ArrayList<>();
    HashMap<String, String> data = new HashMap<>();
    int[] legth;
}
