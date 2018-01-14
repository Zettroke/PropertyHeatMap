package net.zettroke.PropertyHeatMapServer.map;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
                                tempRelation.data.put(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
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
                                    break;
                                case "way":
                                    tempRelation.ways.add(ways.get(id));
                                    break;
                                case "relation":
                                    // fixme: relations might be null
                                    tempRelation.relations.add(relations.get(id));
                                    break;
                            }
                            break;
                    }
                } else {
                    switch (streamReader.getLocalName()) {
                        case "node":

                            coords = m.mercator(tempNode.lon, tempNode.lat);
                            tempNode.x = coords[0]; tempNode.y = coords[1];
                            nodes.put(tempNode.id, tempNode);
                            tempNode = null;
                            break;
                        case "way":
                            ways.put(tempWay.id, tempWay);
                            if (tempWay.data.containsKey("highway")){
                                Node n1 = tempNodeList.get(0);
                                int prev_index;
                                if (!m.roadGraphIndexes.containsKey(n1.id)){
                                    prev_index = m.roadGraphNodes.size();
                                    m.roadGraphIndexes.put(n1.id, m.roadGraphNodes.size());
                                    RoadGraphNode rgn = new RoadGraphNode(n1);
                                    rgn.addWay(tempWay);
                                    m.roadGraphNodes.add(rgn);
                                    m.roadGraphConnections.add(new ArrayList<>());
                                    m.roadGraphDistancesCar.add(new ArrayList<>());
                                    m.roadGraphDistancesFoot.add(new ArrayList<>());
                                    m.roadGraphConnectionsTypes.add(new ArrayList<>());
                                }else{
                                    prev_index = m.roadGraphIndexes.get(n1.id);
                                    m.roadGraphNodes.get(prev_index).addWay(tempWay);
                                }
                                RoadType roadType = RoadType.getType(tempWay.data);
                                for (int i = 1; i < tempNodeList.size(); i++){
                                    int index;
                                    if (m.roadGraphIndexes.containsKey(tempNodeList.get(i).id)){
                                        index = m.roadGraphIndexes.get(tempNodeList.get(i).id);
                                    }else{
                                        RoadGraphNode rgn = new RoadGraphNode(tempNodeList.get(i));
                                        index = m.roadGraphNodes.size();
                                        m.roadGraphIndexes.put(rgn.n.id, m.roadGraphNodes.size());
                                        m.roadGraphNodes.add(rgn);
                                        m.roadGraphConnections.add(new ArrayList<>());
                                        m.roadGraphDistancesCar.add(new ArrayList<>());
                                        m.roadGraphDistancesFoot.add(new ArrayList<>());
                                        m.roadGraphConnectionsTypes.add(new ArrayList<>());

                                    }
                                    //m.roadGraphNodes.get(index).addWay(tempWay);
                                    int dst = PropertyMap.calculateDistance(m.roadGraphNodes.get(index).n, m.roadGraphNodes.get(prev_index).n);
                                    int distCar = Math.round(dst/PropertyMap.car_speed);
                                    int distFoot = Math.round(dst/PropertyMap.foot_speed);

                                    m.roadGraphConnections.get(index).add(prev_index);
                                    m.roadGraphDistancesCar.get(index).add(distCar);
                                    m.roadGraphDistancesFoot.get(index).add(distFoot);
                                    m.roadGraphNodes.get(index).addWay(tempWay);
                                    m.roadGraphConnectionsTypes.get(index).add(roadType);

                                    m.roadGraphConnections.get(prev_index).add(index);
                                    m.roadGraphDistancesCar.get(prev_index).add(distCar);
                                    m.roadGraphDistancesFoot.get(prev_index).add(distFoot);
                                    m.roadGraphNodes.get(prev_index).addWay(tempWay);
                                    m.roadGraphConnectionsTypes.get(prev_index).add(roadType);

                                    prev_index = index;
                                }
                                if (tempWay.data.containsKey("name") && tempWay.data.containsKey("highway") && !tempWay.data.get("highway").equals("trunk")){
                                    m.predictor.add(tempWay.data.get("name"), tempWay);
                                    /*if (tempWay.data.get("name").toLowerCase().equals("«м-1 “беларусь” – крёкшино – троицк» – ильичёвка")){
                                        System.out.println();
                                    }*/
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

        System.gc();
        System.out.println("Loaded osm in " + (System.nanoTime()-start)/1000000.0 + " millis.");
        System.out.println("Nodes: "+nodes.size());
        System.out.println("Ways: "+ways.size());
        System.out.println("Relations: "+relations.size());
        Iterator<HashMap.Entry<Long, Node>> iter = nodes.entrySet().iterator();
        while (iter.hasNext()){
            HashMap.Entry<Long, Node> entry = iter.next();
            Node n = entry.getValue();
            /*if (!m.roadGraphIndexes.containsKey(n.id) && n.data.size()){
                iter.remove();
                m.simpleNodes.add(new SimpleNode(n));
            }else{*/
                if (n.data.size() == 0) {
                    n.data = null;
                }
            //}
        }
        m.nodes = nodes;
        m.ways = ways;
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

