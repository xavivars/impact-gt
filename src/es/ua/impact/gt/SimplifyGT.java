package es.ua.impact.gt;

import es.ua.impact.LexiconSAXUpdater;
import es.ua.impact.utils.*;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class SimplifyGT extends XMLWriter {

    boolean inTextLine, inPage;
    String originalName;
    private static XMLReader reader;
    
    static {
        int i = 0;
        while ((reader == null)&&i<10) {
            try {
                reader = XMLReaderFactory.createXMLReader();
                i++;
            } catch (SAXException ex) {
                reader = null;
                Logger.getLogger(SimplifyGT.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public SimplifyGT() {
        super(reader);
        inTextLine = false;
        inPage = false;
    }

    public void load(String n) {
        originalName = n;
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

    @Override
    public void startElement(final String uri, final String localName,
            final String tag, final Attributes attributes) throws SAXException {

        if (tag.equals("TextLine")) {
            inTextLine = true;
        }
        
        if (tag.equals("Page")) {
            inPage = true;
        }

        if (!inTextLine) {
            super.startElement(uri, localName, tag, attributes);
        }
    }

    @Override
    public void characters(char ch[], int start, int len)
            throws SAXException {
        if (!inTextLine) {
            writeNonEsc(ch, start, len, false);
            super.superCharacters(ch, start, len);
        }
    }

    @Override
    public void endElement(String uri, String localName, String tag)
            throws SAXException {

        if (!inTextLine) {
            super.endElement(uri, localName, tag);
        }

        if (tag.equals("TextLine")) {
            inTextLine = false;
        }
        
        if (tag.equals("Page")) {
            inPage = false;
        }
    }

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
    
     @Override
    protected void writeAttributes(Attributes atts)
            throws SAXException {
        int len = atts.getLength();
        for (int i = 0; i < len; i++) {
            char ch[] = atts.getValue(i).toCharArray();
            
            if (inPage && atts.getLocalName(i).equals("imageFilename")) {
                write(' ');
                writeName(atts.getURI(i), atts.getLocalName(i),
                        atts.getQName(i), false);
                write("=\"");
                write(originalName.replace("./",""));
                write('"');
            } else {
                write(' ');
                writeName(atts.getURI(i), atts.getLocalName(i),
                        atts.getQName(i), false);
                write("=\"");
                writeNonEsc(ch, 0, ch.length, true);
                write('"');
            }
        }
    }
    
    public static void main(String[] args) {
        if(args.length == 0) {
            args = new String[2];
            args[0] = "-f";
            args[1] = "/home/xavi/Dropbox/universitat/IMPACT/xavi-isabel/RuyLopez/XMLsNoBin/RuyLopez-0010.xml";
        }
        String key, value;
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();
        boolean debug = false;


        Option ofile = parser.addStringOption('f', "files");
        Option odir = parser.addStringOption('d', "dir");

        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        }

        String f = optionTester.testFile(parser, ofile);
        SimplifyGT simplifier;
        if (f != null) {
            simplifier = new SimplifyGT();
            simplifier.load(f);
            simplifier.save(null);
        } else {
            String d = optionTester.testDirectory(parser, odir);
            if (d != null) {
                File dir = new File(d);
                FileListFilter select = new FileListFilter("", "xml");
                File[] files = dir.listFiles(select);

                for (File fi : files) {
                    simplifier = new SimplifyGT();
                    simplifier.load(fi.toString());
                    String out = fi.toString().replace(".xml", ".1.xml");
                    try {
                        simplifier.save(new FileWriter(out));
                    } catch (IOException ex) {
                        Logger.getLogger(SimplifyGT.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        }

    }
}
