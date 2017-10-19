package net.zettroke.PropertyHeatMapServer;

import javafx.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Olleggerr on 15.10.2017.
 */
public class PropertyMapLoaderOSM implements PropertyMapLoader{
    private HashMap<Long, Node> nodes = new HashMap<>();
    private ArrayList<Way> ways = new ArrayList<>();
    private String name;

    class OSMhandler extends DefaultHandler{
        Node tempNode;
        Way tempWay;
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case "node":
                    Long id = Long.decode(attributes.getValue(0));
                    tempNode = new Node();
                    tempNode.id = id;
                    break;
                case "way":
                    tempWay = new Way();
                case "tag":
                    if (tempNode != null) {
                        tempNode.data.put(attributes.getValue(0), attributes.getValue(1));
                    }else if (tempWay != null){
                        tempWay.data.put(attributes.getValue(0), attributes.getValue(1));
                    }
                    break;
                case "nd":

                    tempWay.nodes.add(nodes.get(Long.decode(attributes.getValue(0))));

                    break;


            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName){
                case "node":
                    nodes.put(tempNode.id, tempNode);
                    tempNode = null;
                    break;
                case "way":
                    ways.add(tempWay);
                    tempWay = null;

            }
        }
    }


    @Override
    public void load() {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            // parser.getXMLReader().setContentHandler(new OSMhandler());
            parser.parse(new FileInputStream(name), new OSMhandler());

            System.out.println(nodes.size());
            System.out.println(ways.size());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public ArrayList<Node> getNodes() {
        return null;
    }

    @Override
    public ArrayList<Way> getWays() {
        return null;
    }

    @Override
    public ArrayList<Relation> getRelations() {
        return null;
    }

    public PropertyMapLoaderOSM(String name){
        this.name = name;
    }
}
