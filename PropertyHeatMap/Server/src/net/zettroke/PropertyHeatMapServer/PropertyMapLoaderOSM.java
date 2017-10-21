package net.zettroke.PropertyHeatMapServer;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMapLoaderOSM{
    static void load(PropertyMap m, String name) throws XMLStreamException, FileNotFoundException{
        HashMap<Long, Node> nodes = new HashMap<>();
        HashMap<Long, Way> ways = new HashMap<>();
        HashMap<Long, Relation> relations = new HashMap<>();

        XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(name));
        Node tempNode = null;
        Way tempWay = null;
        Relation tempRelation = null;
        int count = 0;
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
                                tempNode.data.put(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
                            } else if (tempWay != null) {
                                tempWay.data.put(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
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
        System.out.println(nodes.size());
        System.out.println(ways.size());
        System.out.println(relations.size());
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

        System.gc();


    }

}
