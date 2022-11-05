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




#if ! defined CPPC_UTIL_XML_PLAINPARSER_H
#define CPPC_UTIL_XML_PLAINPARSER_H

#include <util/xml/xml_parser.h>

#include <fstream>
#include <map>

using std::ifstream;
using std::map;

namespace cppc {
  namespace util {
    namespace xml {

      class PlainParser:public XmlParser {

      public:
        PlainParser();
        PlainParser( const PlainParser & );
        PlainParser & operator=( const PlainParser & );
        virtual ~PlainParser();

        static inline ParserType staticParserType() { return parserType; }
        virtual inline ParserType getParserType() { return staticParserType(); }

        virtual void parseFile( string );

      private:
        void parseLine( ifstream &, char * );

        typedef map<string,string> ParameterMap;
        const static ParserType parserType;
      };

    }
  }
}

#endif
