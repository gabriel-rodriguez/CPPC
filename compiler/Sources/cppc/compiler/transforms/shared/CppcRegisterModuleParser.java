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

import cetus.hir.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CppcRegisterModuleParser extends DefaultHandler {

  // Document structure
  private static final String XML_FUNCTION_TAG = "function";
  private static final String XML_NAME_ATTR = "name";
  private static final String XML_INPUT_TAG = "input";
  private static final String XML_INPUT_OUTPUT_TAG = "input-output";
  private static final String XML_OUTPUT_TAG = "output";
  private static final String XML_PARAMETER_ATTR = "parameters";
  private static final String XML_VARARGS_VALUE = "...";
  private static final String XML_SEMANTICS_TAG = "semantics";
  private static final String XML_SEMANTIC_TAG = "semantic";
  private static final String XML_ROLE_ATTR = "role";
  private static final String XML_PARAMETER_TAG = "attribute";
  private static final String XML_VALUE_ATTR = "value";

  // Private members
  private SAXParser parser;
  private Set<ProcedureParameter> currentConsumed;
  private Set<ProcedureParameter> currentGenerated;
  private String currentProcedure;
  private String currentRole;
  private Hashtable<String,String> currentParameters;
  private Hashtable<String,Hashtable<String,String>> currentSemantics;

  private CppcRegisterModuleParser() throws ParserConfigurationException,
    SAXException {

    // Get the default SAXParserFactory
    SAXParserFactory factory = SAXParserFactory.newInstance();
    parser = factory.newSAXParser();
/*    currentConsumed = new HashSet<ProcedureParameter>();*/
    currentConsumed = null;
//     currentGenerated = new HashSet<ProcedureParameter>();
    currentGenerated = null;
    currentProcedure = null;
//     currentSemantics = new Hashtable<String,String>();
    currentRole = null;
    currentParameters = null;
    currentSemantics = null;
  }

  public static void parse( File file ) {

    try {
      FileInputStream inputStream = new FileInputStream( file );
      InputSource inputSource = new InputSource( inputStream );
      CppcRegisterModuleParser parser = new CppcRegisterModuleParser();
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
    Attributes atts ) {

    if( qName.equals( XML_FUNCTION_TAG ) ) {
      String functionName = atts.getValue( XML_NAME_ATTR );
      startFunction( functionName );
    }

    if( qName.equals( XML_INPUT_TAG ) ) {
      startInput( atts );
    }

    if( qName.equals( XML_OUTPUT_TAG ) ) {
      startOutput( atts );
    }

    if( qName.equals( XML_INPUT_OUTPUT_TAG ) ) {
      startIO( atts );
    }

    if( qName.equals( XML_SEMANTIC_TAG ) ) {
      startSemantic( atts );
    }

    if( qName.equals( XML_PARAMETER_TAG ) ) {
      startParameter( atts );
    }
  }

  private void startFunction( String functionName ) {
    currentProcedure = functionName;
    currentConsumed = new HashSet<ProcedureParameter>();
    currentGenerated = new HashSet<ProcedureParameter>();
    currentParameters = new Hashtable<String,String>();
    currentSemantics = new Hashtable<String,Hashtable<String,String>>();
  }

  private void startInput( Attributes atts ) {
    registerParameters( atts, currentConsumed );
  }

  private void startOutput( Attributes atts ) {
    registerParameters( atts, currentGenerated );
  }

  private void startIO( Attributes atts ) {
    registerParameters( atts, currentConsumed );
    registerParameters( atts, currentGenerated );
  }

  private void startSemantic( Attributes atts ) {
    currentRole = atts.getValue( XML_ROLE_ATTR );
  }

  private void startParameter( Attributes atts ) {
    String name = atts.getValue( XML_NAME_ATTR );
    String value = atts.getValue( XML_VALUE_ATTR );

    currentParameters.put( name, value );
  }

  private void registerParameters( Attributes atts,
    Set<ProcedureParameter> set ) {

    Set<ProcedureParameter> newElems = parseParameters( atts );
    set.addAll( newElems );
  }

  private Set<ProcedureParameter> parseParameters( Attributes atts ) {

    String value = atts.getValue( XML_PARAMETER_ATTR );
    String[] parameters = value.trim().split( "," );
    Set<ProcedureParameter> returnSet = new HashSet<ProcedureParameter>(
      parameters.length );

    for( int i = 0; i < parameters.length; i++ ) {

      String parameter = parameters[i].trim();

      if( parameter.equals( XML_VARARGS_VALUE ) ) {
        returnSet.add( ProcedureParameter.VARARGS );
      } else {
        // The first parameter is called "1" in the XML document, but it must be
        // ProcedureDeclaration( 0 )
        returnSet.add( new ProcedureParameter( new Integer(
          parameters[i].trim() ).intValue() - 1 ) );
      }
    }

    return returnSet;
  }

  public void endElement( String uri, String localName, String qName ) {

    if( qName.equals( XML_FUNCTION_TAG ) ) {
      endFunction();
    }

    if( qName.equals( XML_SEMANTIC_TAG ) ) {
      endSemantic();
    }
  }

  private void endSemantic() {
    currentSemantics.put( currentRole, currentParameters );
  }

  private void endFunction() {

    Identifier name = new Identifier( currentProcedure );
    ProcedureCharacterization characterization =
      new ProcedureCharacterization( name );
    characterization.setGenerated( currentGenerated );
    characterization.setConsumed( currentConsumed );
    characterization.setSemantics( currentSemantics );
    characterization.statementCount = 1;

    CppcRegisterManager.addProcedure( name, characterization );
  }
}
