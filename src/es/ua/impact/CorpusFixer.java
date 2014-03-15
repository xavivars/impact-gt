package es.ua.impact;

import es.ua.impact.utils.BiblInfo;
import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.StringModifier;
import es.ua.impact.utils.Syllabicator;
import es.ua.impact.utils.XMLWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class CorpusFixer extends XMLWriter {

    private StringBuilder buffer;
    private String originalName;
    private boolean inDiv, inAB, inW, hasBIBL;
    private Syllabicator syl;
    private StringModifier sm;

    public CorpusFixer(String f, XMLReader x) {
        super(x);
        originalName = f;
        buffer = new StringBuilder();
        syl = new Syllabicator();
        sm = new StringModifier();
    }

    public CorpusFixer() {
        super();
        buffer = new StringBuilder();
        syl = new Syllabicator();
        sm = new StringModifier();
    }

    public CorpusFixer(XMLReader x) {
        super(x);
        buffer = new StringBuilder();
        syl = new Syllabicator();
        sm = new StringModifier();
    }

    public void writeBibl() throws SAXException{
        String[] ids = originalName.split("\\D+");
        String id;
        if (ids[0].equalsIgnoreCase("")) {
            id = ids[1];
        } else {
            id = ids[0];
        }

        BiblInfo bibl = new BiblInfo();

        write("<bibl>"+bibl.getBiblXML(id)+"</bibl>\n  ");
        hasBIBL = true;
    }

    public void startDocument()
            throws SAXException {
        reset();
        write("<?xml version=\"1.0\"  encoding=\"utf-8\" standalone=\"no\"?>\n");
        super.superStartDocument();
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

        writeNonEsc(ch, start, len, false);
        super.superCharacters(ch, start, len);
    }

    protected void writeAttributesW(Attributes atts) throws SAXException {
        String ctag = atts.getValue("ctag");
        if (ctag == null) {
            inW = false;
            writeAttributes(atts);
            return;
        }

        String lemma = atts.getValue("lemma");
        if (lemma == null || lemma.contains("_")) {
            inW = false;
            writeAttributes(atts);
            System.err.println("lemma: " + lemma + ", modern:" + atts.getValue("mform"));
            return;
        }

        int parts = ctag.split("_").length - 1;

        int len = atts.getLength();

        for (int i = 0; i < len; i++) {
            char ch[] = atts.getValue(i).toCharArray();

            String qName = atts.getQName(i);
            String localName = atts.getLocalName(i);

            if ((parts > 0 && ctag.startsWith("vblex_"))
                    && (localName.equals("mform") || (localName.equals("lemma")))) {
                String nstr = atts.getValue(i);
                int nsyl = syl.countSyllables(syl.split(nstr));

                String[] s = syl.split(nstr).split("-");
                nstr = "";
                for (int j = 0; j < nsyl - parts; j++) {
                    nstr += s[j];
                }
                for (int j = nsyl - parts; j < nsyl; j++) {
                    nstr += "-" + s[j];
                }

                nstr = sm.joinWithDiacritics(nstr.split("-"));

                ch = nstr.toCharArray();
            }

            if (localName.equals("lang")) {
                if (!qName.startsWith("xml:")) {
                    localName = "xml:" + localName;
                }
            }

            write(' ');
            writeName(atts.getURI(i), localName,
                    qName, false);
            write("=\"");
            writeNonEsc(ch, 0, ch.length, true);
            write('"');
        }
    }

    @Override
    protected void writeAttributes(Attributes atts)
            throws SAXException {
        int len = atts.getLength();
        boolean hasN = false, hasFacs = false, parsed = false;
        int parts = 0;
        ArrayList<Integer> toAdd = new ArrayList<Integer>();

        if (inW) {
            writeAttributesW(atts);
            return;
        }

        for (int i = 0; i < len; i++) {
            char ch[] = atts.getValue(i).toCharArray();



            String qName = atts.getQName(i);
            String localName = atts.getLocalName(i);
            if (inAB && localName.equals("id")) {
                localName = "xml:" + localName;
            }

            if (inAB && localName.equals("type")) {
                if (atts.getValue(i).equals("h")) {
                    ch = "head".toCharArray();
                }
            }



            write(' ');
            writeName(atts.getURI(i), localName,
                    qName, false);
            write("=\"");
            writeNonEsc(ch, 0, ch.length, true);
            write('"');
        }

        if (inDiv) {
            if (!hasN) {
                write(' ');
                writeName("", "n", "n", false);
                write("=\"\"");
            }
            if (!hasFacs) {
                write(' ');
                writeName("", "facs", "facs", false);
                write("=\"\"");
            }
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
    public void startElement(String uri, String localName,
            String qName, Attributes atts)
            throws SAXException {
        if (localName.equals("bibl")) {
            hasBIBL = true;
        }
        if (localName.equals("div")) {
            inDiv = true;
        }
        if (localName.equals("ab")) {
            inAB = true;
            if (!hasBIBL) {
                writeBibl();
            }
        }

        if (localName.equals("w")) {
            inW = true;
        }

        super.startElement(uri, localName, qName, atts);
        inDiv = false;
        inAB = false;
        inW = false;
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

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[2];
            args[0] = "-i";
            args[1] = "/home/xavi/extraction/new/test.xml";
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

        System.err.println(input + "...");
        //new java.util.Scanner(System.in).nextLine();
        CorpusFixer l = new CorpusFixer(input, XMLReaderFactory.createXMLReader());
        l.save(out);
        System.err.println("\t\t\t\t\t\tdone!");
    }
}
