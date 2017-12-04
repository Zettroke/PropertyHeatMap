package net.zettroke.PropertyHeatMapServer.map;

import net.zettroke.PropertyHeatMapServer.utils.Apartment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class Way implements Serializable{
    public long id;
    public ArrayList<Node> nodes = new ArrayList<>();
    public HashMap<String, String> data = new HashMap<>();
    public int[] legth;
    public ArrayList<Apartment> apartments;

}
