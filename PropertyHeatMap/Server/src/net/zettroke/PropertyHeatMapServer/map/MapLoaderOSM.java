package net.zettroke.PropertyHeatMapServer.map;


import net.zettroke.PropertyHeatMapServer.map.road_graph.RoadGraphBuilder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class MapLoaderOSM implements MapLoader{
    private String filename;
    private HashMap<Long, Node> nodes = new HashMap<>();
    private ArrayList<SimpleNode> simpleNodes;
    private HashMap<Long, Way> ways = new HashMap<>();
    private HashMap<Long, Relation> relations = new HashMap<>();
    private HashMap<String, Way> searchStrings = new HashMap<>();

    private static class Deduplicator{
        //before ~1823MB
        //after ~1430MB
        //OSM reading +5sec
        // ez 400MB win
        HashMap<String, String> container = new HashMap<>();

        String dedup(String t){
            String res = container.putIfAbsent(t, t);
            return res == null ? t: res;
        }
    }

    @Override
    public int[] getCoordBounds() throws Exception{
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(filename));
        XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(fileIn);
        while (true) {
            if (streamReader.hasName()) {
                if (streamReader.isStartElement()) {
                    if (streamReader.getLocalName().equals("bounds")) {
                        break;
                    }
                }
            }
            streamReader.next();
        }

        double[] degrees = new double[]{Double.parseDouble(streamReader.getAttributeValue("", "minlon")),
                Double.parseDouble(streamReader.getAttributeValue("", "minlat")),
                Double.parseDouble(streamReader.getAttributeValue("", "maxlon")),
                Double.parseDouble(streamReader.getAttributeValue("", "maxlat"))};
        streamReader.close();
        fileIn.close();

        int[] coords =  PropertyMap.mercator(degrees[0], degrees[1]);
        int[] coords1 = PropertyMap.mercator(degrees[2], degrees[3]);

        return new int[]{coords[0], coords1[1], coords1[0], coords[1]};
    }

    @Override
    public Map<Long, Node> getNodes() {
        return nodes;
    }

    @Override
    public List<SimpleNode> getSimpleNodes() {
        return simpleNodes;
    }

    @Override
    public Map<Long, Relation> getRelations() {
        return relations;
    }

    @Override
    public Map<Long, Way> getWays() {
        return ways;
    }

    @Override
    public Map<String, Way> getSearchStrings() throws Exception {
        return searchStrings;
    }

    @Override
    public void load(RoadGraphBuilder builder) throws XMLStreamException, FileNotFoundException{

        Deduplicator dedup = new Deduplicator();

        HashSet<Long> relation_nodes = new HashSet<>();

        FileInputStream fileIn = new FileInputStream(filename);
        XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(fileIn);

        long start = System.nanoTime();
        Node tempNode = null;
        Way tempWay = null;
        Relation tempRelation = null;
        int count = 0;
        int[] coords;

        ArrayList<Node> tempNodeList = new ArrayList<>();

        while (streamReader.hasNext()) {
            if (streamReader.hasName()) {
                if (streamReader.isStartElement()) {
                    switch (streamReader.getLocalName()) {
                        case "node":
                            tempNode = new Node(Double.valueOf(streamReader.getAttributeValue("", "lon")), Double.valueOf(streamReader.getAttributeValue("", "lat")));
                            tempNode.id = Long.valueOf(streamReader.getAttributeValue(0));
                            break;
                        case "way":
                            tempWay = new Way();
                            tempWay.id = Long.valueOf(streamReader.getAttributeValue(0));
                            break;
                        case "tag":
                            if (tempNode != null) {
                                tempNode.data.put(dedup.dedup(streamReader.getAttributeValue(0)), dedup.dedup(streamReader.getAttributeValue(1)));
                            } else if (tempWay != null) {
                                tempWay.data.put(dedup.dedup(streamReader.getAttributeValue(0)), dedup.dedup(streamReader.getAttributeValue(1)));
                            } else if (tempRelation != null) {
                                tempRelation.data.put(dedup.dedup(streamReader.getAttributeValue(0)), dedup.dedup(streamReader.getAttributeValue(1)));
                            }
                            break;
                        case "nd":
                            Node n = nodes.get(Long.decode(streamReader.getAttributeValue(0)));
                            if (n != null) {
                                tempNodeList.add(n);
                                tempWay.nodes.add(n);
                            }
                            break;
                        case "relation":
                            tempRelation = new Relation();
                            tempRelation.id = Long.valueOf(streamReader.getAttributeValue(0));
                            break;
                        case "member":
                            long id = Long.decode(streamReader.getAttributeValue(1));
                            switch (streamReader.getAttributeValue(0)) {
                                case "node":
                                    Node n1 = nodes.get(id);
                                    tempRelation.nodes.add(n1);
                                    if (n1 != null) {
                                        relation_nodes.add(n1.id);
                                    }
                                    break;
                                case "way":
                                    tempRelation.ways.add(ways.get(id));
                                    break;
                                case "relation":
                                    tempRelation.relations.add(relations.get(id));
                                    break;
                            }
                            break;
                    }
                } else {
                    switch (streamReader.getLocalName()) {
                        case "node":
                            coords = PropertyMap.mercator(tempNode.lon, tempNode.lat);
                            tempNode.x = coords[0]; tempNode.y = coords[1];
                            nodes.put(tempNode.id, tempNode);
                            if (tempNode.data.size() == 0) {
                                tempNode.data = null;
                            }
                            tempNode = null;
                            break;
                        case "way":
                            ways.put(tempWay.id, tempWay);
                            if (tempWay.data.containsKey("highway")){
                                RoadType roadType = RoadType.getType(tempWay.data);
                                float car_speed;
                                if (tempWay.data.containsKey("maxspeed")){
                                    try {
                                        car_speed = Integer.parseInt(tempWay.data.get("maxspeed")) / 36.0f; // хрен знает что в maxspeed может лежать кроме числа....
                                    }catch (NumberFormatException e){
                                        car_speed = PropertyMap.car_speed;
                                    }
                                }else{
                                    car_speed = PropertyMap.car_speed;
                                }

                                for (int i=1; i<tempNodeList.size(); i++){
                                    builder.connect(tempNodeList.get(i-1), tempNodeList.get(i), roadType, car_speed);
                                }

                                if (tempWay.data.containsKey("name") && tempWay.data.containsKey("highway") && !tempWay.data.get("highway").equals("trunk")){
                                    searchStrings.put(tempWay.data.get("name").toLowerCase(), tempWay);
                                }
                            }
                            tempWay = null;
                            tempNodeList.clear();
                            break;
                        case "relation":
                            relations.put(tempRelation.id, tempRelation);
                            tempRelation = null;
                            break;
                    }
                }
            }
            streamReader.next();
        }
        streamReader.close();
        dedup = null;
        System.gc();
        System.out.println("Loaded osm in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        System.out.println("Nodes: "+nodes.size());
        System.out.println("Ways: "+ways.size());
        System.out.println("Relations: "+relations.size());

        HashMap<Long, SimpleNode> simpleNodeHashMap = new HashMap<>();
        int cnt = 0;
        HashSet<Long> s = new HashSet<>(nodes.keySet());
        Iterator<Long> iter = s.iterator();
        int steps = 15;
        for (int z=0;z<steps;z++) {
            for (int i = 0; i < s.size()/steps; i++) {
                Long l = iter.next();
                Node n = nodes.get(l);
                if (n.data != null) {
                    if (!builder.isRGN(n) && !relation_nodes.contains(n.id) && Collections.disjoint(n.data.keySet(), PropertyMap.keysInfrastructureObject)) {
                        nodes.remove(l);
                        simpleNodeHashMap.put(n.id, new SimpleNode(n));
                    }
                } else {
                    if (!builder.isRGN(n) && !relation_nodes.contains(n.id)) {
                        nodes.remove(l);
                        simpleNodeHashMap.put(n.id, new SimpleNode(n));
                    }
                }
            }
            System.out.println("simplify node step: " + z);
            for (Way w : ways.values()) {
                for (int i = 0; i < w.nodes.size(); i++) {
                    SimpleNode n = w.nodes.get(i);
                    if (simpleNodeHashMap.containsKey(n.id)) {
                        w.nodes.set(i, simpleNodeHashMap.get(n.id));
                    }
                }
            }
        }
        /*for (Long l: s){
            Node n = nodes.get(l);
            if (n.data != null){
                if (!builder.isRGN(n) && !relation_nodes.contains(n.id) && Collections.disjoint(n.data.keySet(), PropertyMap.keysInfrastructureObject)) {
                    nodes.remove(l);
                    simpleNodeHashMap.put(n.id, new SimpleNode(n));
                }
            }else{
                if (!builder.isRGN(n) && !relation_nodes.contains(n.id)){
                    nodes.remove(l);
                    simpleNodeHashMap.put(n.id, new SimpleNode(n));
                }
            }
        }
        s = null;
        for (Way w: ways.values()){
            for (int i=0; i<w.nodes.size(); i++){
                SimpleNode n = w.nodes.get(i);
                if (simpleNodeHashMap.containsKey(n.id)){
                    w.nodes.set(i, simpleNodeHashMap.get(n.id));
                }
            }
        }*/
        for (Way w: ways.values()){
            w.initBounds();
        }

        simpleNodes = new ArrayList<>(simpleNodeHashMap.values());
        simpleNodeHashMap = null;
        relation_nodes = null;
        System.out.println("SimpleNodes: " + simpleNodes.size());
        System.out.println("Complex Nodes: " + nodes.size());

        System.gc();


    }

    public MapLoaderOSM(String filename){
        this.filename = filename;
    }
}

