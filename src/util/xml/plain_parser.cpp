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



#include <util/xml/plain_parser.h>
#include <util/xml/xml_parser_factory.h>

#include <fstream>
#include <iostream>
#include <string>

using std::cerr;
using std::ifstream;
using std::ios;
using std::string;

namespace {

  using cppc::util::xml::PlainParser;
  using cppc::util::xml::XmlParser;
  using cppc::util::xml::XmlParserFactory;

  XmlParser * createParser() {
    return new PlainParser();
  }

  const bool registered = XmlParserFactory::instance().registerParser( PlainParser::staticParserType(), createParser );

}

namespace cppc {
  namespace util {
    namespace xml {

      const ParserType PlainParser::parserType = 2;

      PlainParser::PlainParser() : XmlParser() {}
//      PlainParser::PlainParser() : XmlParser(), params() {}

      PlainParser::PlainParser( const PlainParser & xsp ) : XmlParser( xsp ) {}

      PlainParser & PlainParser::operator=( const PlainParser & rhs ) {
	XmlParser::operator=( rhs );
	return *this;
      }

      PlainParser::~PlainParser() {}

      void PlainParser::parseLine( ifstream & file, char * line_buffer ) {

//	char * line_c = new char[256];
	file.getline( line_buffer, 256 );

	string line( line_buffer );
	
	// If line is blank or begins with '#': Skip
	if( ( line.size() == 0 ) ||
	    ( line.at( 0 ) == '#' ) ) {
	  return;
	}

	int pos = line.find( "=" );

	if( pos == -1 ) {
	  cerr << "Syntax error: " + line << "\n";
	  return;
	}

	string key = line.substr( 0, pos );
	string value = line.substr( pos+1 );

	params.insert( ParameterMap::value_type( key, value ) );
      }

      void PlainParser::parseFile( string path ) {

	// We create line_buffer here to avoid the overhead of memory
	// creation/deletion inside the parseLine function
	ifstream file( path.c_str(), ios::in );
	char * line_buffer = new char[256];

	while( file.good() ) {
	  this->parseLine( file, line_buffer );
	}

	delete [] line_buffer;
	file.close();
      }

//      string PlainParser::getParameter( const string & key ) {
//
//	ParameterMap::const_iterator i = params.find( key );
//	if( i == params.end() ) {
//	  return string("");
//	} else {
//	  return i->second;
//	}
//      }
	  
      
    }
  }
}
