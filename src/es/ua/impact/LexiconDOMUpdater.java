package es.ua.impact;

import es.ua.impact.utils.BiblInfo;
import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LexiconDOMUpdater implements LexiconUpdater {

    Document doc;
    BiblInfo bibliografia;
    String originalName;

    public LexiconDOMUpdater(String f) {

        try {
            originalName = f;
            bibliografia = new BiblInfo();
            File fXmlFile = new File(originalName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException parserConfigurationException) {
            Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, parserConfigurationException);
        } catch (SAXException sAXException) {
            Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, sAXException);
        } catch (IOException iOException) {
            Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, iOException);
        }

    }
    
    public void update() {
        updateBibl();
    }

    public void updateBibl() {
        NodeList bibs = doc.getElementsByTagName("bibl");

        for (int i = 0; i < bibs.getLength(); ++i) {

            if (i % 1000 == 0) {
                System.err.print('|');
            } else if (i % 100 == 0) {
                System.err.print('.');
            }
            Element bib = (Element) bibs.item(i);
            NodeList titles = bib.getElementsByTagName("title");
            Node nnode;
            Node parent;
            Node node = null;
            String id, bibl;
            for (int j = 0; j < titles.getLength(); ++j) {
                Element title = (Element) titles.item(j);
                id = title.getTextContent();
                String[] ids = (id.split("\\D+"));
                if (ids.length == 0) {
                    System.err.print("·");
                    id = title.getNodeValue();
                    continue;
                }
                if (ids[0].equalsIgnoreCase("")) {
                    id = ids[1];
                } else {
                    id = ids[0];
                }
                bibl = bibliografia.getBiblXML(id, true);
                try {
                    node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bibl.getBytes())).getDocumentElement();
                    removeAllChildren(title);
                    break;
                } catch (SAXException ex) {
                    Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParserConfigurationException ex) {
                    Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (node != null) {
                nnode = doc.importNode(node, true);
                parent = bib.getParentNode();
                parent.insertBefore(nnode, bib);
                parent.removeChild(bib);
            }
        }
    }

    public void save(Writer writer) {
        try {
            if (writer == null) {
                writer = new OutputStreamWriter(System.out);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);

            transformer.transform(source, result);

            writer.write(sw.toString());
            writer.flush();
            writer.close();
        } catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError) {
        } catch (TransformerException transformerException) {
        } catch (IOException iOException) {
        }
    }

    private static void removeAllChildren(Node node) {
        while (node.hasChildNodes()) {
            Node child = node.getFirstChild();
            removeAllChildren(child);
            node.removeChild(child);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[4];
            args[0] = "-i";
            args[1] = "/home/xavi/extraction/test.xml";

        }
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();

        Option oinput = parser.addStringOption('i', "input");

        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }

        String input = optionTester.testFile(parser, oinput, false, false, true);
        String output = null;

        Writer out = null;

        if (output != null) {
            try {
                out = new FileWriter(output);
            } catch (IOException ex) {
                out = null;
                Logger.getLogger(LexiconDOMUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.err.println("Before parsing...");
        //new java.util.Scanner(System.in).nextLine();
        LexiconUpdater l = new LexiconDOMUpdater(input);
        System.err.println("Parsing finished.");
        System.err.println("Before updating...");
        //new java.util.Scanner(System.in).nextLine();
        l.update();
        System.err.println("Updating finished.");
        System.err.println("Before saving...");
        //new java.util.Scanner(System.in).nextLine();
        l.save(out);
        System.err.println("Saving finished.");
    }
}
