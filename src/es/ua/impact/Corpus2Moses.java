package es.ua.impact;

import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author xavi
 */
public class Corpus2Moses {

    private Writer writer;
    private boolean metadata;
    private Document doc;
    public String originalName;

    public Corpus2Moses() {
        writer = new OutputStreamWriter(System.out);
        metadata = false;
        doc = null;
    }

    public void withMetadata() {
        metadata = true;
    }

    public void setOutput(Writer w) {
        if (w != null) {
            writer = w;
        }
    }

    public void extract() {
        try {
            NodeList sentences = doc.getElementsByTagName("s");

            for (int i = 0; i < sentences.getLength(); ++i) {
                Element sentence = (Element) sentences.item(i);

                StringBuilder source = new StringBuilder();
                StringBuilder target = new StringBuilder();
                StringBuilder align = new StringBuilder();

                NodeList nodes = sentence.getChildNodes();
                Element element = null;
                int a = 0, b = 0;
                String str = "";
                for (int j = 0; j < nodes.getLength(); ++j) {
                    if (nodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        element = (Element) nodes.item(j);

                        if (element.getNodeName().equals("w")) {
                            
                            int sizeSource = 0;
                            int sizeTarget = 0;
                            
                            String ssource = element.getTextContent();
                            sizeSource = ssource.split(" ").length;
                            source.append(ssource);
                            
                            String starget = element.getAttribute("mform").replaceAll("_"," ");
                            if(starget == null) {
                                starget = ssource;
                            }
                            sizeTarget = starget.split(" ").length;
                            target.append(starget);
                            
                            for(int ci = 0; ci < sizeSource; ci++) {
                                for(int cj = 0; cj < sizeTarget; cj++) {
                                    str += " "+(a+ci)+":"+(b+cj);
                                }
                            }
                            a += sizeSource;
                            b += sizeTarget;
                            align.append(str);
                            str = "";
                            
                        } else if (element.getNodeName().equals("pc")) {
                            str = " "+element.getTextContent();
                            source.append(str);
                            target.append(str);
                            str = " "+a+":"+b;
                            align.append(str);
                            str = "";
                            ++a;++b;
                        } else {
                            source.append(" ");
                            target.append(" ");
                        }
                    }
                }
                try {
                    writer.write(source.toString().trim() + " ||| " +target.toString().trim() 
                            + " ||| "+align.toString().trim() + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void init(String f) {
        try {
            originalName = f;
            File fXmlFile = new File(originalName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public void getText(String f) {
        this.init(f);
        this.extract();
    }

    public static void main(String[] args) {
        
        if(args.length==0) {
            args= new String[2];
            
            args[0] = "-i";
            args[1] = "/home/xavi/file.xml";
            
        }
        
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();

        Option oinput = parser.addStringOption('i', "input");
        Option ometadata = parser.addBooleanOption('m', "metadata");
//        Option oformat = parser.addStringOption('f', "format");
        Option ooutdir = parser.addStringOption('o', "output");
        
        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(Corpus2Moses.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        String input = optionTester.testFile(parser, oinput, false, false, true);
        boolean withMetadata = optionTester.testBoolean(parser, ometadata);
        //String format = (String) parser.getOptionValue(oformat, "word");

        Corpus2Moses c = new Corpus2Moses();
        c.setOutput(null);
        if (withMetadata) {
            c.withMetadata();
        }

        //c.setFormat(format);
        c.getText(input);
    }
}
