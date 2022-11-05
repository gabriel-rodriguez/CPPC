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



#if ! defined CPPC_UTIL_XML_XERCESSAXPARSER_H
#define CPPC_UTIL_XML_XERCESSAXPARSER_H

#include <util/xml/xml_parser.h>
#include <xercesc/parsers/SAXParser.hpp>

//#include <map>

//using std::map;

#if defined XERCES_CPP_NAMESPACE_USE // Old versions of Xerces do not use namespaces
XERCES_CPP_NAMESPACE_USE // The XERCES namespace changes with every new version
#endif

namespace cppc {
  namespace util {
    namespace xml {

      class XercesSaxParser:public XmlParser {

      public:
        XercesSaxParser();
        XercesSaxParser( const XercesSaxParser & );
        XercesSaxParser & operator=( const XercesSaxParser &  );
        virtual ~XercesSaxParser();

        static inline ParserType staticParserType() { return parserType; }
        virtual inline ParserType getParserType() { return staticParserType(); }

        virtual void parseFile( string );

      private:
        const static ParserType parserType;
        SAXParser * xerces_parser;
      };

    }
  }
}

#endif
