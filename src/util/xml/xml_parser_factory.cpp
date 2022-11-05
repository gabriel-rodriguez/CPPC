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



#include <util/xml/xml_parser_factory.h>

#include <cassert>

namespace cppc {
  namespace util {
    namespace xml {

      XmlParserFactory::XmlParserFactory() : parsers( CallbackMap() ) {}

      XmlParserFactory::~XmlParserFactory() {}

      XmlParserFactory::XmlParserFactory( const XmlParserFactory & pf ) : parsers( pf.parsers ) {}

      XmlParserFactory & XmlParserFactory::operator=( const XmlParserFactory & rhs ) {

	if( this == &rhs ) {
	  return *this;
	}

	parsers = rhs.parsers;

	return *this;
      }

      //FIXME: this mechanism is not useful. Only a parser will be built into
      // the library, and therefore getParser() does not need an argument. Also, there's
      // no need for a parsers list, just a parser instance that this factory returns
      XmlParser * XmlParserFactory::getParser( ParserType code ) {

	// The special value code = 0 means *first found*
	// Return the first one on the parsers list
	if( code == 0 ) {
	  assert( !parsers.empty() );
	  return (parsers.begin()->second)();
	}

	CallbackMap::const_iterator i = parsers.find( code );
	assert( i != parsers.end() );

	return (i->second)();
      }

      bool XmlParserFactory::registerParser( ParserType type, CreateParserCallback callback ) {
	return parsers.insert( CallbackMap::value_type( type, callback ) ).second;
      }      

    }
  }
}
