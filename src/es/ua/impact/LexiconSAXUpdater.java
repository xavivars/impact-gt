package es.ua.impact;

import es.ua.impact.utils.BiblInfo;
import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.StringModifier;
import es.ua.impact.utils.XMLWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class LexiconSAXUpdater extends XMLWriter implements LexiconUpdater {

    String originalName;
    boolean inBib, inBibTitle, inForm, inWordform, inPoS,
            inLemma, inOrth, inWordformOrth, inOrthNormal;
    StringBuilder buffer;
    BiblInfo bibliografia;
    static StringModifier modifier = new StringModifier();

    public LexiconSAXUpdater(String f, XMLReader x) {
        super(x);
        originalName = f;
        bibliografia = new BiblInfo();
        buffer = new StringBuilder();
    }

    public LexiconSAXUpdater() {
        super();
        bibliografia = new BiblInfo();
        buffer = new StringBuilder();
    }

    public LexiconSAXUpdater(XMLReader x) {
        super(x);
        bibliografia = new BiblInfo();
        buffer = new StringBuilder();
    }

    public void update() {
    }

    public void save(Writer w) {
        init(w);
        try {
            parse(originalName);
        } catch (SAXException ex) {
            Logger.getLogger(LexiconSAXUpdater.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LexiconSAXUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Write a start tag.
     *
     * Pass the event on down the filter chain for further processing.
     *
     * @param uri The Namespace URI, or the empty string if none
     *        is available.
     * @param localName The element's local (unprefixed) name (required).
     * @param qName The element's qualified (prefixed) name, or the
     *        empty string is none is available.  This method will
     *        use the qName as a template for generating a prefix
     *        if necessary, but it is not guaranteed to use the
     *        same qName.
     * @param atts The element's attribute list (must not be null).
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the start tag, or if a handler further down
     *            the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String tag, final Attributes attributes) throws SAXException {

        if (tag.equals("bibl")) {
            inBib = true;
        }

        if (tag.equals("form")) {
            inForm = true;
        }

        if (tag.equals("orth")) {
            inOrth = true;
            if (inWordform) {
                inWordformOrth = true;
            }
        }

        if (inBib && tag.equals("title")) {
            inBibTitle = true;
        } else {
            super.startElement(uri, localName, tag, attributes);
        }
        
        if(tag.equals("gram")) {
            inPoS = true;
        }
    }

    /**
     * Write an end tag.
     *
     * Pass the event on down the filter chain for further processing.
     *
     * @param uri The Namespace URI, or the empty string if none
     *        is available.
     * @param localName The element's local (unprefixed) name (required).
     * @param qName The element's qualified (prefixed) name, or the
     *        empty string is none is available.  This method will
     *        use the qName as a template for generating a prefix
     *        if necessary, but it is not guaranteed to use the
     *        same qName.
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the end tag, or if a handler further down
     *            the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    @SuppressWarnings("CallToThreadDumpStack")
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (inBib && qName.equals("title")) {
            inBibTitle = false;

            String id = buffer.toString();

            String[] ids = (id.split("\\D+"));

            if (ids[0].equalsIgnoreCase("")) {
                id = ids[1];
            } else {
                id = ids[0];
            }
            write(bibliografia.getBiblXML(id));
            buffer = new StringBuilder();

        } else if (inOrthNormal && inWordformOrth && qName.equals("orth")) {
            String tmp = buffer.toString();
            boolean inEmpty = false;
            String[] tmpWords = tmp.split(" ");
            ArrayList<String> words = new ArrayList<String>();
            ArrayList<String> toJoin = new ArrayList<String>();
            for (String wd : tmpWords) {
                if (wd.equals("EMPTY")) {
                    inEmpty = true;
                    continue;
                }

                if (inEmpty) {
                    String[] tj = new String[toJoin.size()];
                    tj = toJoin.toArray(tj);
                    words.add(modifier.joinWithDiacritics(tj));
                    toJoin.clear();
                }

                toJoin.add(wd);

                inEmpty = false;
            }

            String[] tj = new String[toJoin.size()];
            tj = toJoin.toArray(tj);
            words.add(modifier.joinWithDiacritics(tj));
            toJoin.clear();
            tmp = "";
            for (String wd : words) {
                tmp += wd + " ";
            }

            write(tmp.trim());
            buffer = new StringBuilder();
            inWordformOrth = false;
            inOrthNormal = false;
            super.endElement(uri, localName, qName);
        } else if(inOrthNormal && inLemma && qName.equals("orth")) {
            String tmp = buffer.toString();
            write(modifier.joinLemmas(tmp));
            buffer = new StringBuilder();
            super.endElement(uri, localName, qName);
        } else {
            super.endElement(uri, localName, qName);
        }

        if (qName.equals("orth")) {
            inOrth = false;
            inWordformOrth = false;
            inOrthNormal = false;
        }

        if (qName.equals("form")) {
            inForm = false;
            inWordform = false;
            inLemma = false;
        }

        if (qName.equals("bibl")) {
            inBib = false;
        }
        
        if(qName.equals("gram")) {
            inPoS = false;
        }
    }

    /**
     * Write character data.
     *
     * Pass the event on down the filter chain for further processing.
     *
     * @param ch The array of characters to write.
     * @param start The starting position in the array.
     * @param length The number of characters to write.
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the characters, or if a handler further down
     *            the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    @Override
    public void characters(char ch[], int start, int len)
            throws SAXException {
        if (inBibTitle || inOrthNormal) {
            buffer.append(ch, start, len);
        } else if(inPoS) {
            String tmp = new String(ch,start,len);
            tmp = tmp.toLowerCase();
            char [] c = tmp.toCharArray();
            writeNonEsc(c, 0,c.length , false);
        } else {
            writeNonEsc(ch, start, len, false);
        }
        super.superCharacters(ch, start, len);
    }

    /**
     * Write an array of data characters with escaping.
     *
     * @param ch The array of characters.
     * @param start The starting position.
     * @param length The number of characters to use.
     * @param isAttVal true if this is an attribute value literal.
     * @exception org.xml.SAXException If there is an error writing
     *            the characters, this method will throw an
     *            IOException wrapped in a SAXException.
     */
    protected void writeNonEsc(char ch[], int start,
            int length, boolean isAttVal)
            throws SAXException {
        for (int i = start; i < start + length; i++) {
            switch (ch[i]) {
                case '&':
                    write("&amp;");
                    break;
                case '<':
                    write("&lt;");
                    break;
                case '>':
                    write("&gt;");
                    break;
                case '\"':
                    if (isAttVal) {
                        write("&quot;");
                    } else {
                        write('\"');
                    }
                    break;
                default:
                    write(ch[i]);
            }
        }
    }

    /**
     * Write out an attribute list, escaping values.
     *
     * The names will have prefixes added to them.
     *
     * @param atts The attribute list to write.
     * @exception org.xml.SAXException If there is an error writing
     *            the attribute list, this method will throw an
     *            IOException wrapped in a SAXException.
     */
    @Override
    protected void writeAttributes(Attributes atts)
            throws SAXException {
        int len = atts.getLength();
        for (int i = 0; i < len; i++) {
            char ch[] = atts.getValue(i).toCharArray();

            if (inForm && atts.getLocalName(i).equals("type") && atts.getValue(i).equals("wordform")) {
                inWordform = true;
            }

            if (inForm && atts.getLocalName(i).equals("type") && atts.getValue(i).equals("lemma")) {
                inLemma = true;
            }


            if (inOrth && atts.getLocalName(i).equals("type") && atts.getValue(i).equals("normal")) {
                inOrthNormal = true;
            }

            write(' ');
            writeName(atts.getURI(i), atts.getLocalName(i),
                    atts.getQName(i), false);
            write("=\"");
            writeNonEsc(ch, 0, ch.length, true);
            write('"');
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[2];
            args[0] = "-i";
            args[1] = "/home/xavi/extraction/test.xml";

        }
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();

        Option oinput = parser.addStringOption('i', "input");

        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(LexiconSAXUpdater.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(LexiconSAXUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }

        String input = optionTester.testFile(parser, oinput, false, false, true);
        String output = null;

        Writer out = null;

        if (output != null) {
            try {
                out = new FileWriter(output);
            } catch (IOException ex) {
                out = null;
                Logger.getLogger(LexiconSAXUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.err.println("Before parsing...");
        //new java.util.Scanner(System.in).nextLine();
        LexiconUpdater l = new LexiconSAXUpdater(input, XMLReaderFactory.createXMLReader());
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
