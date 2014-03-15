package es.ua.impact;

import es.ua.impact.gt.AttestedGTSorter;
import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.SAXReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

public class LexiconTestUpdater extends SAXReader {

    private String originalFile;
    private boolean inBib;
    private boolean inBibTitle;
    private Writer output;

    public static void main(String[] args) {
        if (args.length == 0) {
            
            LexiconTestUpdater lexpars = new LexiconTestUpdater();
            lexpars.setOutput(null);
                
            lexpars.parse("/home/xavi/extraction/test.xml");
        } else {
            

            CmdOptionTester optionTester = new CmdOptionTester();
            CmdOptions parser = new CmdOptions();

            Option oinput = parser.addStringOption('i', "input");
            Option ooutput = parser.addStringOption('o', "output");

            try {
                parser.parse(args);
            } catch (IllegalOptionValueException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownOptionException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            }

            String input = optionTester.testFile(parser, oinput, false, false, true);
            String output = optionTester.testFile(parser, oinput, false, false, false);
            
            Writer out = null;
            
            if(output!=null) try {
                out = new FileWriter(output);
            } catch (IOException ex) {
                out = null;
                Logger.getLogger(LexiconTestUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            LexiconTestUpdater lexpars = new LexiconTestUpdater();
            lexpars.setOutput(out);
                
            lexpars.parse(input);
        }
    }
    
    public LexiconTestUpdater() {
        inBib = false;
        inBibTitle = false;
        
        nsSupport = new NamespaceSupport();
	prefixTable = new Hashtable();
	forcedDeclTable = new Hashtable();
	doneDeclTable = new Hashtable();
        
    }

    public void parse(String f) {
        getText(f);
    }
    
     /**
     * Write the XML declaration at the beginning of the document.
     *
     * Pass the event on down the filter chain for further processing.
     *
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the XML declaration, or if a handler further down
     *            the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument ()
	throws SAXException
    {
	write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
	super.startDocument();
    }


    /**
     * Write a newline at the end of the document.
     *
     * Pass the event on down the filter chain for further processing.
     *
     * @exception org.xml.sax.SAXException If there is an error
     *            writing the newline, or if a handler further down
     *            the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument ()
	throws SAXException
    {
	write('\n');
	super.endDocument();
	try {
	    flush();
	} catch (IOException e) {
	    throw new SAXException(e);
	}
    }

    
    
    
    @Override
    public void characters(final char[] c, final int start, final int length) {
        try {
            String tmp = new String(c,start,length);
            writeNonEsc(c, start, length, false);
        } catch (SAXException ex) {
            Logger.getLogger(LexiconTestUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String tag, final Attributes attributes) throws SAXException {
        
        if(tag.equals("bib")) {
            inBib = true;
        }
        
        if(inBib && tag.equals("title")) {
            inBibTitle = true;
        }
        
        write('<');
	writeName(uri, localName, tag, true);
	writeAttributes(attributes);
        
        write('>');
        
    }

    /**
     * 
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String tag) throws SAXException {
        
        write("</");
	writeName(uri, localName, tag, true);
	write('>');
	if (elementLevel == 1) {
	    write('\n');
	}
        
    }
    
     /**
     * Write a raw character.
     *
     * @param c The character to write.
     * @exception org.xml.sax.SAXException If there is an error writing
     *            the character, this method will throw an IOException
     *            wrapped in a SAXException.
     */
    protected void write (char c)
	throws SAXException
    {
	try {
	    output.write(c);
	} catch (IOException e) {
	    throw new SAXException(e);
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
    protected void write (String s)
    throws SAXException
    {
	try {
	    output.write(s);
	} catch (IOException e) {
	    throw new SAXException(e);
	}
    }
    
    /**
     * Flush the output.
     *
     * <p>This method flushes the output stream.  It is especially useful
     * when you need to make certain that the entire document has
     * been written to output but do not want to close the output
     * stream.</p>
     *
     * <p>This method is invoked automatically by the
     * {@link #endDocument endDocument} method after writing a
     * document.</p>
     *
     * @see #reset
     */
    public void flush ()
	throws IOException 
    {
	output.flush();
    }
    

    /**
     * Set a new output destination for the document.
     *
     * @param writer The output destination, or null to use
     *        standard output.
     * @return The current output writer.
     * @see #flush
     */
    public void setOutput (Writer writer)
    {
	if (writer == null) {
	    output = new OutputStreamWriter(System.out);
	} else {
	    output = writer;
	}
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
    protected void writeEsc (char ch[], int start, int length, boolean isAttVal)
	throws SAXException
    {
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
		if (ch[i] > '\u007f') {
		    write("&#");
		    write(Integer.toString(ch[i]));
		    write(';');
		} else {
		    write(ch[i]);
		}
	    }
	}
    }
    
    /**
     * Write an array of data characters without escaping.
     *
     * @param ch The array of characters.
     * @param start The starting position.
     * @param length The number of characters to use.
     * @param isAttVal true if this is an attribute value literal.
     * @exception org.xml.SAXException If there is an error writing
     *            the characters, this method will throw an
     *            IOException wrapped in a SAXException.
     */    
    protected void writeNonEsc (char ch[], int start,
			     int length, boolean isAttVal)
	throws SAXException
    {
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
     * Write an element or attribute name.
     *
     * @param uri The Namespace URI.
     * @param localName The local name.
     * @param qName The prefixed name, if available, or the empty string.
     * @param isElement true if this is an element name, false if it
     *        is an attribute name.
     * @exception org.xml.sax.SAXException This method will throw an
     *            IOException wrapped in a SAXException if there is
     *            an error writing the name.
     */
    private void writeName (String uri, String localName,
			      String qName, boolean isElement)
	throws SAXException
    {
	write(qName);
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
    private void writeAttributes (Attributes atts)
	throws SAXException
    {
	int len = atts.getLength();
	for (int i = 0; i < len; i++) {
	    char ch[] = atts.getValue(i).toCharArray();
	    write(' ');
	    writeName(atts.getURI(i), atts.getLocalName(i),
		      atts.getQName(i), false);
	    write("=\"");
	    writeNonEsc(ch, 0, ch.length, true);
	    write('"');
	}
    }
    
    
    /**
     * Write out the list of Namespace declarations.
     *
     * @exception org.xml.sax.SAXException This method will throw
     *            an IOException wrapped in a SAXException if
     *            there is an error writing the Namespace
     *            declarations.
     */    
    private void writeNSDecls ()
	throws SAXException
    {
	Enumeration prefixes = nsSupport.getDeclaredPrefixes();
	while (prefixes.hasMoreElements()) {
	    String prefix = (String) prefixes.nextElement();
	    String uri = nsSupport.getURI(prefix);
	    if (uri == null) {
		uri = "";
	    }
	    char ch[] = uri.toCharArray();
	    write(' ');
	    if ("".equals(prefix)) {
		write("xmlns=\"");
	    } else {
		write("xmlns:");
		write(prefix);
		write("=\"");
	    }
	    writeEsc(ch, 0, ch.length, true);
	    write('\"');
	}
    }
    
    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    private Hashtable prefixTable;
    private Hashtable forcedDeclTable;
    private Hashtable doneDeclTable;
    private int elementLevel = 0;
    private NamespaceSupport nsSupport;
    private int prefixCounter = 0;
}
