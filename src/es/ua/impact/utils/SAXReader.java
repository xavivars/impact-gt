/*
 * Copyright (C) 2007
 *  
 * Authors:
 *  Enrique Benimeli Bofarull <ebenimeli@gmail.com>
 *  David Ortega Parilla <dortegaparrilla@gmail.com>
 *  Xavier Ivars i Ribes <xavi.ivars@gmail.com>
 *  
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package es.ua.impact.utils;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

/**
 * 
 * @author david
 */
public abstract class SAXReader extends DefaultHandler {

        /**
         * 
         */
        protected final int kStringBuilder = 10000;

        /**
         * 
         */
        protected StringBuilder buffer = new StringBuilder(kStringBuilder);

        /**
         * 
         */
        protected org.xml.sax.XMLReader reader = null;

        /**
         * Creates a new instance of SaxReader
         * 
         */
        public SAXReader() {

        }

        /**
         * 
         */
        @Override
        public void characters(final char[] c, final int start, final int length) {
        }

        @Override
        public void startElement(final String uri, final String localName,
                        final String tag, final Attributes attributes) throws SAXException {

        }

        /**
         * 
         */
        @Override
        public void endElement(final String uri, final String localName,
                        final String tag) throws SAXException {
        }

        /**
         * 
         * @return
         */
        public void getXMLReader() {
                try {
                        reader = SAXParserFactory.newInstance().newSAXParser()
                                        .getXMLReader();
                        reader.setContentHandler(this);
                } catch (Exception x) {
                        System.err.println(x.getMessage());
                }
        }

        /**
         * 
         * @param fileName
         * @return
         */
        public String getText(final String fileName) {

                getXMLReader();
                buffer = new StringBuilder(kStringBuilder);
                try {
                        reader.parse(fileName);
                } catch (IOException x) {
                        System.err.println("Error parsing " + fileName + ": " + x);
                } catch (SAXException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                return buffer.toString();
        }
        
        /**
         * 
         * @param fileName
         * @return
         */
        public String getText(final InputStream stream) {

                getXMLReader();
                buffer = new StringBuilder(kStringBuilder);
                try {
                        reader.parse(new InputSource(stream));
                } catch (IOException x) {
                        System.err.println("Error parsing " + stream.toString() + ": " + x);
                } catch (SAXException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                return buffer.toString();
        }
}
