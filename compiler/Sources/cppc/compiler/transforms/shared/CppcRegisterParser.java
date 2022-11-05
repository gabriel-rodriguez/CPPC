//    This file is part of CPPC.
//
//    CPPC is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    CPPC is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with CPPC; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA




package cppc.compiler.transforms.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CppcRegisterParser extends DefaultHandler {

  // Document structure
  private static final String XML_MODULE_TAG_NAME = "module";
  private static final String XML_MODULE_FILE_ATTRIBUTE = "file";

  // Private members
  private SAXParser parser;

  private CppcRegisterParser() throws ParserConfigurationException,
    SAXException {

    // Get the default SAXParserFactory
    SAXParserFactory factory = SAXParserFactory.newInstance();
    parser = factory.newSAXParser();
  }

  public static void parse( File file ) {

    try {
      FileInputStream inputStream = new FileInputStream( file );
      InputSource inputSource = new InputSource( inputStream );
      CppcRegisterParser parser = new CppcRegisterParser();
      parser.parse( inputSource );
    } catch( FileNotFoundException e ) {
      System.err.println( "WARNING: cannot open file " + file.getPath() + ": " +
        e.getMessage() );
    } catch( SecurityException e ) {
      System.err.println( "WARNING: access denied reading " + file.getPath() +
        ": " + e.getMessage() );
    } catch( ParserConfigurationException e ) {
      System.err.println( "WARNING: error parsing file " + file.getPath() + ": "
        + e.getMessage() );
    } catch( SAXException e ) {
      System.err.println( "WARNING: error parsing file " + file.getPath() + ": "
        + e.getMessage() );
    } catch( IOException e ) {
      System.err.println( "WARNING: error reading file " + file.getPath() + ": "
        + e.getMessage() );
    }

  }

  public void parse( InputSource inputSource ) throws SAXException,
    IOException {

    parser.parse( inputSource, this );
  }

  public void startElement( String uri, String localName, String qName,
    Attributes attributes ) {

    if( qName.equals( XML_MODULE_TAG_NAME ) ) {
      // Parse a the file containing a family of functions
      String fileName = attributes.getValue( XML_MODULE_FILE_ATTRIBUTE );

      File file = new File( fileName );
      if( file == null ) {
        System.err.println( "WARNING: cannot access file " + fileName +
          " for reading" );
      } else {
        CppcRegisterModuleParser.parse( file );
      }
    }
  }
}