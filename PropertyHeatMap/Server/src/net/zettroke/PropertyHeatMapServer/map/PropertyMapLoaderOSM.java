package net.zettroke.PropertyHeatMapServer.map;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMapLoaderOSM{

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

    public static void load(PropertyMap m, FileInputStream fileIn) throws XMLStreamException, FileNotFoundException{
        Deduplicator dedup = new Deduplicator();
        HashMap<Long, Node> nodes = new HashMap<>();
        HashMap<Long, Way> ways = new HashMap<>();
        HashMap<Long, Relation> relations = new HashMap<>();

        XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(fileIn);
        long start = System.nanoTime();
        Node tempNode = null;
        Way tempWay = null;
        Relation tempRelation = null;
        int count = 0;
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
        //streamReader.next(); streamReader.next(); streamReader.next();
        m.maxlat = Double.valueOf(streamReader.getAttributeValue("", "maxlat"));
        m.maxlon = Double.valueOf(streamReader.getAttributeValue("", "maxlon"));
        m.minlat = Double.valueOf(streamReader.getAttributeValue("", "minlat"));
        m.minlon = Double.valueOf(streamReader.getAttributeValue("", "minlon"));
        int[] coords = m.mercator(m.maxlon, m.maxlat);
        int[] coords1 = m.mercator(m.minlon, m.minlat);
        m.x_end = coords[0]; m.y_begin = coords[1];
        m.x_begin = coords1[0]; m.y_end = coords1[1];


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
                                tempRelation.data.put(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
                            }
                            break;
                        case "nd":
                            Node n =  nodes.get(Long.decode(streamReader.getAttributeValue(0)));
                            tempWay.nodes.add(n);
                            n.ways.add(tempWay);
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
                                    if (n1 != null) {
                                        tempRelation.nodes.add(n1);
                                        n1.relations.add(tempRelation);
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
                            if (tempNode.data.size() == 0) {
                                tempNode.data = null;
                                count++;
                            }
                            coords = m.mercator(tempNode.lon, tempNode.lat);
                            tempNode.x = coords[0]; tempNode.y = coords[1];
                            nodes.put(tempNode.id, tempNode);
                            tempNode = null;
                            break;
                        case "way":
                            ways.put(tempWay.id, tempWay);
                            tempWay = null;
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

        dedup = null;
        System.gc();
        System.out.println("Loaded osm in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        System.out.println("Nodes: "+nodes.size());
        System.out.println("Ways: "+ways.size());
        System.out.println("Relations: "+relations.size());
        streamReader.close();
        ArrayList<SimpleNode> simpleNodes = new ArrayList<>();
        for (Node n: nodes.values()){
            boolean have_highway = false;
            for (Way w: n.ways){
                if (w.data.containsKey("highway")){
                    have_highway = true;
                    break;
                }
            }
            if (n.relations.size() == 0 && !have_highway){
                if (n.data == null){
                    simpleNodes.add(new SimpleNode(n));
                }else{
                    n.ways = null;
                    n.relations = null;
                }

            }
        }
        for (SimpleNode n: simpleNodes){
            Node n2 = nodes.get(n.id);
            for (Way w: n2.ways){
                w.nodes.set(w.nodes.indexOf((SimpleNode) n2), n);
            }
            nodes.remove(n.id);
        }
        m.simpleNodes = simpleNodes;
        m.nodes = new ArrayList<>(nodes.values());
        m.ways = new ArrayList<>(ways.values());
        m.relations = new ArrayList<>(relations.values());

        //System.gc();


    }

    public static void load(PropertyMap m, String name) throws XMLStreamException, FileNotFoundException{
        load(m, new FileInputStream(name));
    }

    public static void load(PropertyMap m, File file) throws XMLStreamException, FileNotFoundException{
        load(m, new FileInputStream(file));
    }
}

