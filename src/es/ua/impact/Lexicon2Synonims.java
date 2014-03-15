package es.ua.impact;

import es.ua.impact.gt.AttestedGTSorter;
import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.SAXReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Lexicon2Synonims extends SAXReader {

    private Writer output;
    
    private boolean inLemma;
    private boolean inWordform;
    private boolean inHistorical;
    private boolean inPoS;
    private boolean inOrth;
    private boolean inCited;
    private boolean lemmaBased;
    private boolean printedLemma;
    private boolean printedWordform;
    private String lastLemma;
    private String lastModern;
    
    public Lexicon2Synonims() {
        buffer = new StringBuilder();
        lemmaBased = false;
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {

            Lexicon2Synonims l = new Lexicon2Synonims();
            l.setOutput(null);
            l.getText("/home/xavi/extraction/out.xml");
        } else {


            CmdOptionTester optionTester = new CmdOptionTester();
            CmdOptions parser = new CmdOptions();

            Option oinput = parser.addStringOption('i', "input");
            Option olemma = parser.addBooleanOption('l',"lemma");

            try {
                parser.parse(args);
            } catch (IllegalOptionValueException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownOptionException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            }

            String input = optionTester.testFile(parser, oinput, false, false, true);
            boolean lemma = optionTester.testBoolean(parser, olemma);

            Lexicon2Synonims l = new Lexicon2Synonims();
            l.setOutput(null);
            if(lemma) {
                l.setLemmaBased();
            }
            l.getText(input);
        }


    }

    public void setLemmaBased() {
        lemmaBased = true;
    }
    
    @Override
    public void characters(final char[] c, final int start, final int length) {
        if(inLemma && inOrth) {
            buffer.append(c, start, length);
        }
        if(inWordform && inOrth && !inHistorical) {
            buffer.append(c, start, length);
        }
        if(inWordform && inOrth && inHistorical && !inCited) {
            buffer.append(c, start, length);
        }
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String tag, final Attributes atts) throws SAXException {

        if (tag.equals("form")) {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                if(atts.getLocalName(i).equals("type") && atts.getValue(i).equals("lemma")) {
                    inLemma = true;
                    lastLemma = "";
                    printedLemma = false;
                }
                if(atts.getLocalName(i).equals("type") && atts.getValue(i).equals("wordform")) {
                    inWordform = true;
                    lastModern = "";
                    printedWordform = false;
                }
                if(atts.getLocalName(i).equals("type") && atts.getValue(i).equals("historical")) {
                    inHistorical = true;
                }
                 if(atts.getLocalName(i).equals("type") && atts.getValue(i).equals("cited")) {
                    inCited = true;
                }
            }
        }
        
        if(tag.equals("orth")) {
            inOrth = true;
        }
    }

    /**
     * 
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String tag) throws SAXException {
        if(tag.equals("orth")) {
            if(inLemma && inOrth) {
                lastLemma = buffer.toString();
                buffer = new StringBuilder();
                if(lemmaBased) {
                    System.out.println();
                }
            }
            
            if(inWordform && inOrth && !inHistorical) {
                lastModern = buffer.toString();
                buffer = new StringBuilder();
                if(!lemmaBased) {
                    System.out.println();
                }
            }
            
            if(inWordform && inOrth && inHistorical && !inCited) {
                String historical = buffer.toString();
                buffer = new StringBuilder();
                
                if(historical.endsWith(".") || historical.endsWith(",")) {
                    historical = historical.substring(0,historical.length()-1);
                }
                
                if(lemmaBased && !printedLemma) {
                    System.out.print(lastLemma);
                    printedLemma = true;
                }
                
                if(!printedWordform) {
                    if(lemmaBased) {
                        if(!lastModern.equals(lastLemma)) {
                            System.out.print(","+lastModern);
                        }
                    } else {
                        System.out.print(lastModern);
                    }
                    printedWordform = true;
                }
                
                if(!historical.equals(lastModern))
                    System.out.print(","+historical);
            }
            
            inOrth = false;
        }
        
        if(tag.equals("form")) {
            if(inCited) {
                inCited = false;
            } else if(inHistorical) {
                inHistorical = false;
            } else if (inWordform) {
               inWordform = false;
            } else if (inLemma) {
               inLemma = false;
            }
        }
    }

    /**
     * Write a raw string.
     *
     * @param s
     * @exception org.xml.sax.SAXException If there is an error writing
     *            the string, this method will throw an IOException
     *            wrapped in a SAXException
     */
    protected void write(String s)
            throws SAXException {
        try {
            output.write(s);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Set a new output destination for the document.
     *
     * @param writer The output destination, or null to use
     *        standard output.
     * @return The current output writer.
     * @see #flush
     */
    public void setOutput(Writer writer) {
        if (writer == null) {
            output = new OutputStreamWriter(System.out);
        } else {
            output = writer;
        }
    }
}
