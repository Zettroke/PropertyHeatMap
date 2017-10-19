package net.zettroke.PropertyHeatMapServer;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMapLoaderOSM implements PropertyMapLoader{
    private boolean dataLoad = false;
    private HashMap<Long, Node> nodes = new HashMap<>();
    private HashMap<Long, Way> ways = new HashMap<>();
    private HashMap<Long, Relation> relations = new HashMap<>();
    private String name;

    @Override
    public void load() {

        try {
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(name));

            Node tempNode = null;
            Way tempWay = null;
            Relation tempRelation = null;
            while (streamReader.hasNext()) {
                if (streamReader.hasName()) {
                    if (streamReader.isStartElement()) {
                        switch (streamReader.getLocalName()) {

                            case "node":
                                tempNode = new Node();
                                tempNode.id = Long.decode(streamReader.getAttributeValue(0));
                                break;
                            case "way":
                                tempWay = new Way();
                                tempWay.id = Long.decode(streamReader.getAttributeValue(0));
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
                                tempWay.nodes.add(nodes.get(Long.decode(streamReader.getAttributeValue(0))));
                                break;
                            case "relation":
                                tempRelation = new Relation();
                                tempRelation.id = Long.decode(streamReader.getAttributeValue(0));
                                break;
                            case "member":
                                long id = Long.decode(streamReader.getAttributeValue(1));
                                switch (streamReader.getAttributeValue(0)) {
                                    case "node":
                                        tempRelation.nodes.add(nodes.get(id));
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
            dataLoad = true;

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public ArrayList<Node> getNodes() {
        ArrayList<Node> nds = new ArrayList<>(nodes.values());
        nodes = null;
        return nds;
    }

    @Override
    public ArrayList<Way> getWays() {
        ArrayList<Way> wys = new ArrayList<>(ways.values());
        ways = null;
        return wys;
    }

    @Override
    public ArrayList<Relation> getRelations() {
        ArrayList<Relation> rlt = new ArrayList<>(relations.values());
        relations = null;
        return rlt;
    }

    PropertyMapLoaderOSM(String name){
        this.name = name;
    }
}
