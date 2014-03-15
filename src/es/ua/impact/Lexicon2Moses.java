package es.ua.impact;

import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.EditDistanceAligner;
import es.ua.impact.utils.SAXReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Lexicon2Moses extends SAXReader {

    private Writer output;
    private StringBuilder buffer;
    private StringBuilder beforeWord, afterWord;
    private boolean oVar;
    private boolean inLemma;
    private boolean inWordform;
    private boolean inHistorical;
    private boolean inPoS;
    private boolean inOrth;
    private boolean inCited;
    private boolean inOVar;
    private boolean inQuote;
    private String lastLemma;
    private String lastModern;
    private String lastPoS;
    private String historical;
    private int segmentation;
    private int contextSegmentation;
    private static final int WORD = 0;
    private static final int CHARACTER = 1;
    private boolean printMetadata;
    private int contextSize;
    private HashMap<String, String> charAlignments;

    /**
     * Constructor
     */
    public Lexicon2Moses() {
        buffer = new StringBuilder();
        beforeWord = new StringBuilder();
        afterWord = new StringBuilder();
        segmentation = WORD;
        contextSegmentation = WORD;
        contextSize = 0;
        charAlignments = new HashMap<String, String>();
    }

    /**
     * Main method
     *      -i|--input              TEI XML lexicon file
     *      -c|--contextSize        Size wrapping the aligned text
     *      -m|--metadata           Boolean to add metadata (factored models)
     *      -f|--format             character|all-character|word
     *      -o|--output             output file
     * @param args 
     */
    public static void main(String[] args) {
        


            CmdOptionTester optionTester = new CmdOptionTester();
            CmdOptions parser = new CmdOptions();

            Option oinput = parser.addStringOption('i', "input");
            Option ocontext = parser.addIntegerOption('c', "contextSize");
            Option ometadata = parser.addBooleanOption('m', "metadata");
            Option oformat = parser.addStringOption('f', "format");
            Option ooutdir = parser.addStringOption('o', "output");
            
            try {
                parser.parse(args);
            } catch (IllegalOptionValueException ex) {
                Logger.getLogger(Lexicon2Moses.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownOptionException ex) {
                Logger.getLogger(Lexicon2Moses.class.getName()).log(Level.SEVERE, null, ex);
            }
            

            String input = optionTester.testFile(parser, oinput, false, false, true);
            boolean withMetadata = optionTester.testBoolean(parser, ometadata);
            String format = (String) parser.getOptionValue(oformat, "word");
            int cs = (Integer) parser.getOptionValue(ocontext, 0);
            String odir = null;
            
            
            
            Lexicon2Moses l = new Lexicon2Moses();
            l.setOutput(null);
            if(withMetadata)
                l.withMetadata();
            l.setContextSize(cs);
            l.setFormat(format);
            l.getText(input);
    }

    /**
     * Parses the content of a tag
     * @param c
     * @param start
     * @param length 
     */
    @Override
    public void characters(final char[] c, final int start, final int length) {
        if (inLemma && inOrth) {
            buffer.append(c, start, length);
        }
        if (inWordform && inOrth && !inHistorical) {
            buffer.append(c, start, length);
        }
        if (inWordform && inOrth && inHistorical && !inCited) {
            buffer.append(c, start, length);
        }
        if (inLemma && inPoS) {
            buffer.append(c, start, length);
        }
        if (inWordform && inHistorical && inCited && inQuote) {
            if (!inOVar) {
                if (!oVar) {
                    beforeWord.append(c, start, length);
                } else {
                    afterWord.append(c, start, length);
                }
            }
        }
    }

    /**
     * Detects the tag we've just entered
     * @param uri
     * @param localName
     * @param tag
     * @param atts
     * @throws SAXException 
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String tag, final Attributes atts) throws SAXException {

        if (tag.equals("oVar")) {
            oVar = true;
            inOVar = true;
        }

        if (tag.equals("quote")) {
            inQuote = true;
        }

        if (tag.equals("entry")) {
            lastPoS = "";
        }

        if (tag.equals("form")) {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("lemma")) {
                    inLemma = true;
                    lastLemma = "";
                }
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("wordform")) {
                    inWordform = true;
                    lastModern = "";
                }
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("historical")) {
                    inHistorical = true;
                }
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("cited")) {
                    inCited = true;
                }
            }
        }

        if (inLemma && tag.equals("gram")) {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("PoS")) {
                    inPoS = true;
                }
            }
        }

        if (tag.equals("orth")) {
            inOrth = true;
        }
    }

    /**
     * Detects the tag we've just closed
     * @param uri
     * @param localName
     * @param tag
     * @throws SAXException 
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String tag) throws SAXException {

        if (tag.equals("oVar")) {
            inOVar = false;
        }

        if (tag.equals("quote")) {
            inQuote = false;
            oVar = false;
            try {
                printQuote();
            } catch (IOException e) {
                System.err.println("Error");
            }
            beforeWord = new StringBuilder();
            afterWord = new StringBuilder();
        }

        if (tag.equals("orth")) {
            if (inLemma && inOrth) {
                lastLemma = buffer.toString();
                buffer = new StringBuilder();
            }

            if (inWordform && inOrth && !inHistorical) {
                lastModern = buffer.toString();
                buffer = new StringBuilder();
            }

            if (inWordform && inOrth && inHistorical && !inCited) {
                historical = buffer.toString();
                buffer = new StringBuilder();

                if (historical.endsWith(".") || historical.endsWith(",")) {
                    historical = historical.substring(0, historical.length() - 1);
                }
            }

            inOrth = false;
        }

        if (tag.equals("gram")) {
            if (inLemma && inPoS) {
                lastPoS += "+" + buffer.toString();
                buffer = new StringBuilder();
            }
            inPoS = false;
        }

        if (tag.equals("form")) {
            if (inCited) {
                inCited = false;
            } else if (inHistorical) {
                inHistorical = false;
            } else if (inWordform) {
                inWordform = false;
            } else if (inLemma) {
                inLemma = false;
                lastPoS = lastPoS.substring(1);
            }
        }
    }

    /**
     * Removes all non-text characters from a piece of text
     * @param word
     * @return 
     */
    private String cleanWord(String word) {
        return word.replaceAll("(\\p{P})"," $1 ").replaceAll("\\p{Z}+"," ").trim();
    }

    /**
     * Prints the left-context
     * @return 
     */
    private int printBeforeContext() {
        int ret = 0;
        String pre = "";
        
        String before = cleanWord(beforeWord.toString().toLowerCase());
                
        if(contextSegmentation == CHARACTER) {
            before = before.replaceAll(" ","#");
            
            for (int i = before.length() - 1; i >= 0 && ret < contextSize; --i) {
                pre = before.charAt(i) + " " + pre;
                ++ret;
            }
            
        } else {
            String[] befores = before.split(" ");

            for (int i = befores.length - 1; i > 0 && ret < contextSize; --i) {
                String cleaned = cleanWord(befores[i].toLowerCase());
                if (!cleaned.isEmpty()) {
                    pre = cleaned + " " + pre;
                    ++ret;
                }
            }
        }

        System.out.print(pre);
        return ret;
    }

    /**
     * Prints the right-context
     */
    private void printAfterContext() {
        int post = 0;
        String after = cleanWord(afterWord.toString().toLowerCase());
        if(contextSegmentation == CHARACTER) {
            
            String ret = "";
            boolean first = true;
            for(String s : after.split(" ")) {
                if(!first) {
                    ret += "#";
                }
                ret += s;
            }
            for (int i = 0; i <  ret.length() - 1 && post < contextSize; ++i) {
                System.out.print(" " + ret.charAt(i));
                ++post;
            }
            
        } else {
            String[] afters = after.split(" ");
            
            for (int i = 0; i < afters.length - 1 && post < contextSize; ++i) {
                if (!afters[i].isEmpty()) {
                    System.out.print(" " + afters[i]);
                    ++post;
                }
            }
        }
    }

    /**
     * Sets metadata
     */
    public void withMetadata() {
        printMetadata = true;
    }

    /**
     * Sets the size of the context
     * @param c 
     */
    public void setContextSize(int c) {
        if (c < 0) c = 0;

        contextSize = c;
    }

    /**
     * Set the type of segmentation printed
     * @param word 
     */
    public void setFormat(String word) {
        if (word.equals("character")) {
            segmentation = CHARACTER;
        }
        if (word.equals("all-character")) {
            segmentation = CHARACTER;
            contextSegmentation = CHARACTER;
        }
    }
    
    /**
     * Prints a single quote as a moses-entry
     * @throws IOException 
     */
    private void printQuote() throws IOException {

        int nH = 1, nM = 1;
        int sH = 0;

        if (segmentation == WORD) {
            if (contextSize > 0) {
                sH = printBeforeContext();
            }

            if (historical.contains(" ")) {
                String[] partsH = historical.split(" ");
                int i = 0;
                nH = partsH.length;
                for (String part : partsH) {
                    if (i != 0) {
                        System.out.print(" ");
                    }
                    if(printMetadata)
                        System.out.print(part.toLowerCase() + "|" + lastLemma + "|" + lastPoS + "|es|" + nH + "|" + (++i));
                    else
                        System.out.print(part.toLowerCase());
                }
            } else {
                if(printMetadata)
                    System.out.print(historical.toLowerCase() + "|" + lastLemma + "|" + lastPoS + "|es|1|1");
                else 
                    System.out.print(historical.toLowerCase());
            }


            if (contextSize > 0) {
                printAfterContext();
            }
            System.out.print(" ||| ");


            if (lastModern.contains(" ")) {
                String[] partsM = lastModern.split(" ");
                int i = 0;
                nM = partsM.length;
                for (String part : partsM) {
                    if (i != 0) {
                        System.out.print(" ");
                    }
                    if(printMetadata)
                        System.out.print(part.toLowerCase() + "|" + lastLemma + "|" + lastPoS + "|es|" + nM + "|" + (++i));
                    else
                        System.out.print(part.toLowerCase());
                }
            } else {
                if(printMetadata)
                    System.out.print(lastModern.toLowerCase() + "|" + lastLemma + "|" + lastPoS + "|es|1|1");
                else
                    System.out.print(lastModern.toLowerCase());
            }

            System.out.print(" |||");

            for (int i = 0; i < nH; i++) {
                for (int j = 0; j < nM; j++) {
                    System.out.print(" " + (i + sH) + ":" + (j));
                }
            }
        } else if (segmentation == CHARACTER) {
            printCharacterAlignment(historical.toLowerCase(), lastModern.toLowerCase());
        }

        System.out.println();
    }

    /**
     * Creates the alignment of two words
     * @param wd
     * @param w1
     * @param w2
     * @param add
     * @throws IOException 
     */
    private void createCharacterAlignments(String wd, String w1, String w2, int add) throws IOException {

        StringWriter out = new StringWriter();

        int[] alignment;
        int dist;

        // Both words are the same
        if (w1.equalsIgnoreCase(w2)) {
            dist = 0;
            
            for(int i=0;i<w1.length()+2;i++) {
                out.write(""+(add+i)+"-"+i+" ");
            }
        } else {
            dist = EditDistanceAligner.getEditDistance(w1, w2);

            alignment = EditDistanceAligner.getAlignment(w1, w2);

            String[] ws = wd.split("\\|");

            out.write("" + add + "-0");
            int lastJ = -1;
            int i = 0;
            for (; i < alignment.length; i++) {
                if (alignment[i] >= 0) {
                    lastJ = alignment[i];
                    out.write(" " + (i + add + 1) + "-" + (lastJ + 1));
                } else {
                    boolean keep = true;

                    int nI = i;
                    int nJ;
                    do {
                        while (nI < alignment.length && (alignment[nI] < 0)) {
                            ++nI;
                        }
                        if (nI >= alignment.length) {
                            nJ = w2.length();
                        } else {
                            nJ = alignment[nI];
                        }

                        for (int k = i; k < nI; ++k) {
                            for (int kj = lastJ + 1; kj < nJ; kj++) {
                                out.write(" " + (k + add + 1) + "-" + (kj + 1));
                            }
                        }

                        if (nI >= alignment.length) {
                            keep = false;
                        } else if (alignment[nI] >= 0) {
                            keep = false;
                        }
                        i = nI;
                    } while (keep);
                    --i;
                }
            }

            out.write(" " + (w1.length() + 1 + add) + "-" + (w2.length() + 1));
        }


        charAlignments.put(wd + "\\|" + contextSize, out.toString().trim());
    }

    /**
     * Method which prints the alignment of two given words
     * @param w1
     * @param w2 
     */
    private void printCharacterAlignment(String w1, String w2) throws IOException {

        String wd = EditDistanceAligner.getKey(w1.toLowerCase(), w2.toLowerCase());
        String[] ws = wd.split("\\|");


        int add = 0;
        if (contextSize > 0) {
            add = printBeforeContext();
        }

        if (!charAlignments.containsKey(wd + "\\|" + add)) {
            createCharacterAlignments(wd, w1, w2, add);
        }

        for (int i = 0; i < ws[0].length(); i++) {
            if (i != 0) {
                System.out.print(" ");
            }
            System.out.print((ws[0]).charAt(i));
        }

        if (contextSize > 0) {
            printAfterContext();
        }

        System.out.print(" ||| ");

        for (int i = 0; i < ws[1].length(); i++) {
            if (i != 0) {
                System.out.print(" ");
            }
            System.out.print((ws[1]).charAt(i));
        }

        System.out.print(" ||| ");

        System.out.print(charAlignments.get(wd + "\\|" + contextSize));
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
