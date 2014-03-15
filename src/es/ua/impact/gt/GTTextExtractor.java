package es.ua.impact.gt;

import es.ua.impact.utils.SAXReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GTTextExtractor extends SAXReader {

    private ArrayList<ReplaceRule> replacements;
    private StringBuilder content;
    private SortedHandler sortedHandler;
    private boolean inUnicode;
    private boolean inCredit;
    private boolean sorted;
    private boolean all;
    private boolean showXML;
    private boolean debug;

    

    public GTTextExtractor(ArrayList<ReplaceRule> reps) {
        replacements = reps;
        inUnicode = false;
        inCredit = false;
        sorted = false;
        all = false;
        content = new StringBuilder();
    }

    public void setDebug(boolean d) {
        debug = d;
    }
    
    public void setShowXML(boolean d) {
        showXML = d;
    }
    
    public void print() {
        System.out.println(content);
    }

    public void clear() {
        content = new StringBuilder();
    }

    public void save(String f) {
        if(sorted)
            f = f + ".out.sorted.txt";
        else
            f = f + ".out.txt";

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
            pw.println(content);
            pw.close();
        } catch (IOException ex) {
            Logger.getLogger(GTTextExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String replace() {

        String str;
        if(!sorted)
            str = content.toString();
        else
            str = sortedHandler.getText();
        
        for (ReplaceRule e : replacements) {
            str = str.replaceAll(e.getSource(), e.getTarget());
        }

        content = new StringBuilder(str);
        return str;
    }

    @Override
    public void startElement(final String uri, final String localName, final String tag, final Attributes attributes) throws SAXException {
        if (tag.equals("TextRegion")) {
            if (attributes.getValue("type").equalsIgnoreCase("credit")) {
                inCredit = true;
            }
        }

        if (tag.equals("Unicode")) {
            inUnicode = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String tag) {
        if (inCredit && tag.equals("TextRegion")) {
            inCredit = false;
        }
        
        if (tag.equals("Unicode")) {
            inUnicode = false;
            content.append("\n\n");
        }
    }

    @Override
    public void characters(char[] c, int start, int length) {

        if (!inCredit && (inUnicode && (length > 0))) {
            content.append(c, start, length);
        }
    }

    public void showAll() {
        all = true;
    }

    public void sort() {
        sorted = true;
    }

    @Override
    public void getXMLReader() {
        sortedHandler = new SortedHandler();
        try {
            reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            if(sorted) {
                sortedHandler.debug = this.debug;
                reader.setContentHandler(sortedHandler);
            } else {
                reader.setContentHandler(this);
            }
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
    }

    private class SortedHandler extends DefaultHandler {

        int order;
        String region;
        ArrayList<String> orderList;
        HashMap<String,Integer> orderHash;
        boolean inOrdered;
        HashMap<String,StringBuilder> hash;
        private StringBuilder content;
        private HashMap<String,Boolean> sortedtypes;
        private boolean debug;
        private boolean pageNumberFirst;
        private boolean pageNumberSeen;
        
        
        public SortedHandler() {
            sortedtypes = new HashMap<String, Boolean>();
            sortedtypes.put("drop-capital",true);
            sortedtypes.put("paragraph",true);
            sortedtypes.put("heading",true);
            sortedtypes.put("TOC-entry",true);
            sortedtypes.put("catch-word",true);
            
            order = 0;
            region = "";
            hash = new HashMap<String, StringBuilder>();
            orderHash = new HashMap<String, Integer>();
            content = new StringBuilder();
            inOrdered = false;
            pageNumberFirst = true;
            pageNumberSeen = false;
        
        }
        
        @Override
        public void startElement(final String uri, final String localName, final String tag, final Attributes attributes) throws SAXException {
            if (tag.equals("RegionRefIndexed")) {
                orderHash.put(attributes.getValue("regionRef")
                        ,Integer.parseInt(attributes.getValue("index")));
            }
            
            
            
            if (tag.equals("TextRegion")) {
                if (attributes.getValue("type").equalsIgnoreCase("credit")) {
                    inCredit = true;
                }
                
                if(attributes.getValue("type").equalsIgnoreCase("header")) {
                    if(!pageNumberSeen) pageNumberFirst = false;
                }
                if(attributes.getValue("type").equalsIgnoreCase("page-number")) {
                    pageNumberSeen = true;
                }
                
                if(attributes.getValue("type").equalsIgnoreCase("catch-word")) {
                    if(!sortedtypes.containsKey("catch-word"))
                        orderHash.remove(attributes.getValue("id"));
                }
                
                inOrdered = false;
                if (sortedtypes.containsKey(attributes.getValue("type"))) {
                    region = attributes.getValue("id");
                    content = new StringBuilder();
                    if(showXML && attributes.getValue("type").equalsIgnoreCase("heading"))
                        content.append("<h>");
                    hash.put(region, content);
                    inOrdered = true;
                } else {
                    region = attributes.getValue("type");
                    inOrdered = false;
                    if(hash.containsKey(region)) {
                        content = hash.get(region);
                        content.append("\n\n");
                    } else {
                        content = new StringBuilder();
                        hash.put(region, content);
                    }
                }
            }

            if (tag.equals("Unicode")) {
                inUnicode = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String tag) {
            if (inCredit && tag.equals("TextRegion")) {
                inCredit = false;
            }

            if (tag.equals("Unicode") && inUnicode) {
                inUnicode = false;
            }
            
            if(tag.equals("Page")) {
                getOrder();
            }
        }

        @Override
        public void characters(char[] c, int start, int length) {

            //if (!inCredit && (inUnicode && (length > 0))) {
            if ((inUnicode && (length > 0))) {
                content.append(c, start, length);
            }
        }
        
        public void getOrder() {
            int total = orderHash.size();
            orderList = new ArrayList(total);
            
            for(int i=0;i<total;++i) {
                orderList.add(null);
            }
            
            Set<String> keys = orderHash.keySet();
            for(String s : keys) {
                int o = orderHash.get(s);
                orderList.set(o, s);
            }
        }
        
        public String getText() {
            StringBuilder tmp = new StringBuilder();
            String aux;
            /*
            if(hash.containsKey("page-number")) {
                tmp.append(hash.get("page-number"));
                tmp.append(" ");
            }
            
            if(hash.containsKey("header")) {
                tmp.append(hash.get("header"));
                tmp.append("\n");
            } */
            
            for(String s : orderList) {
                if(debug) System.err.println(s);
                StringBuilder sb = hash.get(s);
                if(sb == null) {
                    continue;
                }
                String saux = sb.toString();
                tmp.append(saux);
                if(saux.length() > 1) {
                    if(showXML)
                        tmp.append("\n<br />\n");
                    else
                        tmp.append("\n");
                }
            }
            
            for(String s: orderHash.keySet()) {
                if(s.equalsIgnoreCase("page-number")
                        || s.equalsIgnoreCase("header"))
                    continue;
                
                if(!orderList.contains(s)) {
                    tmp.append(hash.get(s));
                    tmp.append("\n");
                }
            }
            
            
            return tmp.toString();
        }
    }
}
