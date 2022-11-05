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



#include <util/xml/xerces_sax_parser.h>
#include <util/xml/xml_parser_factory.h>

#include <xercesc/framework/MemBufFormatTarget.hpp>
#include <xercesc/framework/XMLFormatter.hpp>
#include <xercesc/sax/HandlerBase.hpp>
#include <xercesc/util/PlatformUtils.hpp>

#include <iostream>

#if ! defined XERCES_STD_QUALIFIER // Old versions of Xerces do not define this
#define XERCES_STD_QUALIFIER std::
#endif

namespace {

  using cppc::util::xml::XercesSaxParser;
  using cppc::util::xml::XmlParser;
  using cppc::util::xml::XmlParserFactory;

  XmlParser * createParser() {
    return new XercesSaxParser();
  }

  const bool registered = XmlParserFactory::instance().registerParser( XercesSaxParser::staticParserType(), createParser );

}

namespace cppc {
  namespace util {
    namespace xml {

      namespace xerces_sax_parser_internal {

	using std::cerr;

	class CppcConfigurationHandler:public HandlerBase {
	  
	public:
	  virtual void startElement( const XMLCh * const name, AttributeList & attributes ) {

	    string elementType( readXmlCh( name ) );
	    if( elementType == "cppc" ) {
	      actualNamespace = "CPPC";
	      return;
	    }
	    if( elementType == "module" ) {
	      int i = 0;
	      string moduleName( readXmlCh( attributes.getValue( i++ ) ) );
	      actualNamespace += "/" + moduleName;
	      return;
	    }
	    if( elementType == "parameter" ) {
	      int i = 0;
	      string parameterKey( actualNamespace + "/" + readXmlCh( attributes.getValue( i++ ) ) );
	      string parameterValue( readXmlCh( attributes.getValue( i++ ) ) );
	      params->insert( ParameterMap::value_type( parameterKey, parameterValue ) );
	    }
	  }

	  virtual void endElement( const XMLCh * const name ) {

	    string element( readXmlCh( name ) );
	    if( element == "module" ) {
	      actualNamespace = actualNamespace.substr( 0, actualNamespace.rfind('/') );
	    }
	  }

	  virtual void warning( const SAXParseException & e ) {
	    
	    string fileName( readXmlCh( e.getSystemId() ) );
	    string message( readXmlCh( e.getMessage() ) );
	    XERCES_STD_QUALIFIER cerr << "\nWarning parsing configuration file. XERCES explanation:\n"
				      << "Warning at file " << fileName
				      << ", line " << e.getLineNumber()
				      << ", char " << e.getColumnNumber()
				      << "\nMessage: " << message << XERCES_STD_QUALIFIER endl;
	  }

	  virtual void fatalError( const SAXParseException & e ) {

	    string fileName( readXmlCh( e.getSystemId() ) );
	    string message( readXmlCh( e.getMessage() ) );
	    XERCES_STD_QUALIFIER cerr << "\nCannot parse configuration file. XERCES explanation:\n"
				      << "Fatal error at file " << fileName
				      << ", line " << e.getLineNumber()
				      << ", char " << e.getColumnNumber()
 				      << "\nMessage: " << message << XERCES_STD_QUALIFIER endl;
 	  }

	  virtual void error( const SAXParseException & e ) {

	    string fileName( readXmlCh( e.getSystemId() ) );
	    string message( readXmlCh( e.getMessage() ) );	    
	    XERCES_STD_QUALIFIER cerr << "\nCannot parse configuration file. XERCES explanation:\n"
				      << "Error at file " << fileName
				      << ", line " << e.getLineNumber()
				      << ", char " << e.getColumnNumber()
				      << "\nMessage: " << message << XERCES_STD_QUALIFIER endl;
	  }

	private:
	  typedef map<string,string> ParameterMap;

	  CppcConfigurationHandler( ParameterMap * e_params ) : HandlerBase(),
	      params( e_params ), actualNamespace( "" ), encodingName( "LATIN1" ), formatTarget( new MemBufFormatTarget() ),
	      fFormatter( encodingName.c_str(), formatTarget, XMLFormatter::NoEscapes, XMLFormatter::UnRep_CharRef ) {}

	  virtual ~CppcConfigurationHandler() {
	    delete formatTarget;
	  }

	  string readXmlCh( const XMLCh * const value ) {
	    fFormatter << value;
	    string s_value( (char *)formatTarget->getRawBuffer() );
	    formatTarget->reset();

	    return s_value;
	  }

	  ParameterMap * params;
	  string actualNamespace;
	  const string encodingName;
	  MemBufFormatTarget * formatTarget;
	  XMLFormatter fFormatter;

	  friend class cppc::util::xml::XercesSaxParser;
	};

      }

      using cppc::util::xml::xerces_sax_parser_internal::CppcConfigurationHandler;

      const ParserType XercesSaxParser::parserType = 1;

      XercesSaxParser::XercesSaxParser() : XmlParser(), xerces_parser( NULL ) {
	XMLPlatformUtils::Initialize();
	xerces_parser = new SAXParser();
	xerces_parser->setValidationScheme( SAXParser::Val_Auto ); // Auto: if no DTD is found then the file is not validated
	xerces_parser->setDoNamespaces( false ); // We don't use namespaces in the configuration file
	xerces_parser->setDoSchema( false ); // We use DTDs to validate the configuration file
	xerces_parser->setValidationSchemaFullChecking( false );
      }

      XercesSaxParser::XercesSaxParser( const XercesSaxParser & xsp ) : XmlParser( xsp ) {}

      XercesSaxParser & XercesSaxParser::operator=( const XercesSaxParser & rhs ) {	
      	XmlParser::operator=( rhs );
      	return *this;
      }

      XercesSaxParser::~XercesSaxParser() {
	delete xerces_parser;
	XMLPlatformUtils::Terminate();
      }

      void XercesSaxParser::parseFile( string path ) {
	CppcConfigurationHandler * handler = new CppcConfigurationHandler( &params );
	xerces_parser->setDocumentHandler( handler );
	xerces_parser->setErrorHandler( handler );
	xerces_parser->parse( path.c_str() );
	delete handler;
      }
    }
  }
}
