package es.ua.impact.gt;

import es.ua.impact.utils.SAXReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class XMLReplacementsReader extends SAXReader {

    public XMLReplacementsReader() {
        replacements = new ArrayList<ReplaceRule>();
    }

    public void addXML(String s) {
        getText(s);
    }
    
    public void addXML(InputStream s) {
        getText(s);
    }

    public ArrayList<ReplaceRule> getReplacements() {
        Collections.sort(replacements);
        return replacements;
    }
    private ArrayList<ReplaceRule> replacements;

    private boolean addReplacement(String k, String v,String order) {

        StringBuilder key, value;
        boolean ret = true;
        int i = 0;
        
        key = new StringBuilder();
        Character c;
        String [] ks = k.split(",");
        for(String s : ks) {
            c = new Character((char)Integer.parseInt(s, 16));
            key.append(c);    
        }
        
        
        value = new StringBuilder();
        String [] vs = v.split(",");
        for(String s : vs) {
            c = new Character((char)Integer.parseInt(s,16));
            value.append(c);
        }
        
        replacements.add(new ReplaceRule(key.toString(), value.toString(),Integer.parseInt(order)));

        return ret;
    }

    @Override
    public void startElement(final String uri, final String localName, final String tag, final Attributes attributes) throws SAXException {
        if (tag.equals("Parameter")) {
            String aux;
            String[] reps;
            String order;
            int type;

            aux = attributes.getValue("type");
            if (aux == null) {
                return;
            }

            type = Integer.parseInt(aux);
            if (type != 4) {
                return;
            }

            aux = null;
            aux = attributes.getValue("value");
            if (aux == null) {
                return;
            }

            order = attributes.getValue("sortIndex");
            if (order == null) {
                return;
            }
            
            reps = aux.split(":=");

            addReplacement(reps[0], reps[1],order);
        }
    }
}
