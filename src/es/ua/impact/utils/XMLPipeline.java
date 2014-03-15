package es.ua.impact.utils;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XMLPipeline extends XMLWriter {
    
    public XMLPipeline() {
        super();
    }
    
    public XMLPipeline(XMLReader x) {
        super(x);
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
    public void characters (char ch[], int start, int length)
	throws SAXException
    {
	String tmp = new String(ch,start,length);
        writeNonEsc(ch, start, length, false);
        super.superCharacters(ch,start,length);
    }
    
    /**
     * Write an array of data characters with escaping.
     *
     * @param ch The array of characters.
     * @param start The starting position.
     * @param length The number of characters to use.
     * @param isAttVal true if this is an attribute value literal.
     * @throws org.xml.SAXException 
     *            If there is an error writing
     *            the characters, this method will throw an
     *            IOException wrapped in a SAXException.
     */    
    protected void writeNonEsc (char ch[], int start, int length, boolean isAttVal) throws SAXException
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
    
}
