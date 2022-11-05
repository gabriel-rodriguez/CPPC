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




#if ! defined CPPC_UTIL_CONFIGURATION_CONFIGURATIONMANAGER_H
#define CPPC_UTIL_CONFIGURATION_CONFIGURATIONMANAGER_H

#include <util/singleton/singleton.h>
#include <util/xml/xml_parser.h>

#include <map>
#include <string>
#include <vector>

using std::map;
using std::string;
using std::vector;

using cppc::util::singleton::Singleton;
using cppc::util::xml::ParserType;
using cppc::util::xml::XmlParser;

namespace cppc {
  namespace util {
    namespace configuration {

      typedef string ParameterValueType;
      typedef string ParameterKeyType;
      typedef struct {
	ParameterKeyType key;
	ParameterValueType value;
      } ParameterType;
      typedef vector<ParameterType> ParameterVector;
      typedef ParameterVector::const_iterator ParameterIterator;

      class ConfigurationManager:public Singleton<ConfigurationManager> {

      public:
	void init( const ParameterVector & );
	const ParameterValueType * const getParameter( const ParameterKeyType & );

      private:
	ConfigurationManager();
	ConfigurationManager( const ConfigurationManager & );
	ConfigurationManager & operator=( const ConfigurationManager & );

	const ParameterValueType * getParameterFromCache( const ParameterKeyType & ) const;
	ParameterValueType * getParameterFromEnvironment( const ParameterKeyType & ) const;
	ParameterValueType * getParameterFromConfigurationFile( const ParameterKeyType & ) const;
	void writeStateToConfigurationFile() const;

	typedef map<ParameterKeyType,ParameterValueType> ParameterMap;

	ParameterMap configuration;
	bool initialized;
	XmlParser * parser;

	friend class Singleton<ConfigurationManager>;

	const static ParameterKeyType CPPC_PARAMETER_PREFIX;
	const static ParameterKeyType CPPC_CONFIGURATION_FILE_PARAMETER_KEY;
	const static ParameterKeyType CPPC_PARSER_TYPE_PARAMETER_KEY;
	static string CPPC_CONFIGURATION_FILE_PATH;
	void initCppcConfigurationFilePath();
	ParserType getParserTypeParameter();

      };

    }
  }
}

#endif
