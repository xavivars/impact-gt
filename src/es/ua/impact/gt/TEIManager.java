package es.ua.impact.gt;

import es.ua.impact.utils.BiblInfo;
import es.ua.impact.utils.IdResolver;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class TEIManager {

    Document doc;
    Node pointer = null;
    boolean finished = false;
    String originalName;
    private static BiblInfo bibliografia;

    public TEIManager(String f) {
        originalName = f;
        createDoc(f);
    }

    public String getBibl(String id) {

        return "<bibl>"
                + "<title>" + bibliografia.getTitle(id) + "</title>"
                + "<author>" + bibliografia.getAuthor(id) + "</author>"
                + "<date>" + bibliografia.getYear(id) + "</date>"
                + "<ident>" + bibliografia.getID(id) + "</ident>"
                + "</bibl>\n";
    }

    public boolean save() {
        try {

            String [] ids = (doc.getDocumentElement().getAttributeNode("xml:id").getTextContent().split("\\D+"));
            String id;
            if(ids[0].equalsIgnoreCase(""))
                id = ids[1];
            else 
                id = ids[0];

            String bibl = getBibl(id);
            

            Element el = doc.getDocumentElement();
            Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bibl.getBytes())).getDocumentElement();

            el.insertBefore(doc.importNode(node, true), el.getFirstChild());


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            String f = originalName + ".filled.xml";
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            File file = new File(f);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(sw.toString().replaceAll("<c> </c>\n      \n      <c> </c>", "<c> </c>").replaceAll("<bibl>","\n  <bibl>"));
            writer.flush();
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void export(ArrayList<String> ids, HashMap<String, ArrayList<String>> missing) {
        BufferedWriter writer = null;
        String f = originalName + ".missing.xml";
        try {
            writer = new BufferedWriter(new FileWriter(f));
            String[] parts = originalName.split("/");
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<blocks id=\"" + parts[parts.length - 1] + "\">\n");
            for (String id : ids) {
                writer.write("\t<block id=\"" + id + "\">\n");
                Element el = getID(id);
                NodeList words = el.getElementsByTagName("w");
                for (int i = 0; i < words.getLength(); ++i) {
                    writer.write("\t\t" + words.item(i).getTextContent() + "\n");
                }
                writer.write("\t</block>\n");
            }
            for (String id : missing.keySet()) {

                if (missing.get(id).isEmpty()) {
                    continue;
                }

                writer.write("\t<block id=\"" + id + "\">\n");
                for (String word : missing.get(id)) {
                    writer.write("\t\t" + word + "\n");
                }
                writer.write("\t</block>\n");
            }
            writer.write("<blocks>\n");

        } catch (IOException ex) {
            //System.err.println("ERROR exporting"+f);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(TEIManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void createDoc(String f) {
        try {
            if (bibliografia == null) {
                bibliografia = new BiblInfo();
            }
            File fXmlFile = new File(f);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Element getID(String id) {
        return IdResolver.getElementById(doc, id);
    }

    public Set<ArrayList<String>> getBlockText() {
        Set<ArrayList<String>> ret = new HashSet<ArrayList<String>>();

        NodeList nList = doc.getElementsByTagName("ab");

        ArrayList<String> list = null;

        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            Element eElement = (Element) nNode;

            list = new ArrayList<String>();

            String id = eElement.getAttribute("id");
            if (id != null) {
                list.add(id);
            }

            NodeList words = eElement.getElementsByTagName("w");

            for (int j = 0; j < words.getLength(); j++) {
                Node w = words.item(j);
                list.add(w.getTextContent());
            }

            ret.add(list);
        }

        return ret;
    }

    public static void main2(String[] args) {
        TEIManager tei = new TEIManager("/home/xavi/extraction/empty_teis/pc-00438755.xml.out.sorted.txt.xml");
        Set<ArrayList<String>> set = tei.getBlockText();
        Iterator<ArrayList<String>> iterator = set.iterator();

        while (iterator.hasNext()) {
            ArrayList<String> list = iterator.next();
            for (String w : list) {
                System.out.print(w + " ");
            }
            System.out.println();
        }
    }

    public Node getNextWord() {
        Node ret = null;
        if (pointer == null && !finished) {
            NodeList nList = doc.getElementsByTagName("w");
            if (nList.getLength() > 0) {
                pointer = nList.item(0);
            }
        }

        if (pointer != null) {
            ret = pointer;
            Node next = pointer.getNextSibling();
            while ((next != null) && ((next.getNodeType() != Node.ELEMENT_NODE) || (next.getNodeName() != "w"))) {
                next = next.getNextSibling();
            }

            if (next == null) {
                finished = true;
            }

            pointer = next;
        }

        return ret;
    }

    public Node testNextWord() {
        Node ret = null;
        Node tpointer = pointer;
        if (tpointer == null && !finished) {
            NodeList nList = doc.getElementsByTagName("w");
            if (nList.getLength() > 0) {
                tpointer = nList.item(0);
            }
        }

        if (tpointer != null) {
            ret = tpointer;
            Node next = tpointer.getNextSibling();
            while ((next != null) && ((next.getNodeType() != Node.ELEMENT_NODE) || (next.getNodeName() != "w"))) {
                next = next.getNextSibling();
            }

            if (next == null) {
                finished = true;
            }
        }

        return ret;
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }
}