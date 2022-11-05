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



#include <util/configuration/configuration_manager.h>
#include <util/xml/xml_parser.h>
#include <util/xml/xml_parser_factory.h>

#include <cstdlib>

#include <iostream>

using std::cout;

using cppc::util::xml::XmlParserFactory;

namespace cppc {
  namespace util {
    namespace configuration {

      const ParameterKeyType ConfigurationManager::CPPC_PARAMETER_PREFIX(
        "CPPC" );
      string ConfigurationManager::CPPC_CONFIGURATION_FILE_PATH( "" );


      ConfigurationManager::ConfigurationManager()
        : configuration( ParameterMap() ), initialized( false ),
        parser( 0 ) {}

      ConfigurationManager::ConfigurationManager(
        const ConfigurationManager & cm )
        : configuration( cm.configuration ), initialized( cm.initialized ),
        parser( cm.parser ) {}

      ConfigurationManager & ConfigurationManager::operator=(
        const ConfigurationManager & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        configuration = rhs.configuration;
        initialized = rhs.initialized;
        parser = rhs.parser;

        return *this;
      }


      void ConfigurationManager::init( const ParameterVector & parameters ) {

        if( initialized ) {
          return;
        }

        ParameterIterator i = parameters.begin();

        while( i != parameters.end() ) {

          ParameterType parameter = (*i);
          configuration.insert( ParameterMap::value_type( parameter.key,
            parameter.value ) );

          i++;
        }

        // Now we have our command line parameters we look for the configuration
        // file name
        initCppcConfigurationFilePath();

        // Getting the XML Parser...
        parser = XmlParserFactory::instance().getParser(
          getParserTypeParameter() );
        parser->parseFile( CPPC_CONFIGURATION_FILE_PATH );

        initialized = true;
      }

      const ParameterValueType * const ConfigurationManager::getParameter(
        const ParameterKeyType & key ) {

        // 1st: Look in the cache
        const ParameterValueType * newParam = getParameterFromCache( key );

        // The parameter is already cached, so we can safely return
        if( newParam != 0 ) {
          return newParam;
        }

        // If it is not cached then we must find it and $ it

        // 2nd: Look in environment
        if( newParam == 0 ) {
          newParam = getParameterFromEnvironment( key );
        }

        // 3rd: Look in the configuration file
        if( newParam == 0 ) {
          newParam = getParameterFromConfigurationFile( key );
        }

        if( newParam != 0 ) {
          configuration.insert( ParameterMap::value_type( key, *newParam ) );
          return newParam;
        }

        // Maybe this politic could be changed
        cout << "Missing Parameter: " << key << "\n";
        exit( 0 );

        return NULL;

      }

      const ParameterValueType * ConfigurationManager::getParameterFromCache(
        const ParameterKeyType & key ) const {

        ParameterMap::const_iterator i = configuration.find( key );

        if( i != configuration.end() ) {
          return &(i->second);
        } else {
          return NULL;
        }
      }

      ParameterValueType * ConfigurationManager::getParameterFromEnvironment(
        const ParameterKeyType & key ) const {

        ParameterKeyType env_key( key );

        for( unsigned int i = 0; i < env_key.length(); i++ ) {
          if( env_key[i] == '/' ) {
            env_key[i] = '_';
          }
        }

        char * value = std::getenv( env_key.c_str() );

        if( value != NULL ) {
          return new ParameterValueType( value );
        }

        return NULL;

      }

      ParameterValueType *
        ConfigurationManager::getParameterFromConfigurationFile(
        const ParameterKeyType & key ) const {

        if( !initialized ) {
          return NULL;
        }

        string value = parser->getParameter( key );
        if( value == "" ) {
          return NULL;
        } else {
          return new string( value );
        }
      }

      void ConfigurationManager::writeStateToConfigurationFile() const {
      }

      const ParameterKeyType
        ConfigurationManager::CPPC_CONFIGURATION_FILE_PARAMETER_KEY(
          "CPPC/Configuration/FilePath" );
      void ConfigurationManager::initCppcConfigurationFilePath() {

        const ParameterValueType * cppcConfFile = getParameterFromCache(
          CPPC_CONFIGURATION_FILE_PARAMETER_KEY );
        if( cppcConfFile == NULL ) {
          cppcConfFile = getParameterFromEnvironment(
            CPPC_CONFIGURATION_FILE_PARAMETER_KEY );
        }
        if( cppcConfFile != NULL ) {
          CPPC_CONFIGURATION_FILE_PATH = *(cppcConfFile);
        } else {
          string defaultName( ".cppc_config" );
          ParameterValueType * homeDir = getParameterFromEnvironment( "HOME" );
          CPPC_CONFIGURATION_FILE_PATH = *homeDir + "/" + defaultName;
          cout << "CPPC: Trying to use default file " <<
            CPPC_CONFIGURATION_FILE_PATH << " for configuration. Override " <<
              "with CPPC/Configuration/FilePath\n";
          delete homeDir;
        }
      }

      const ParameterKeyType
        ConfigurationManager::CPPC_PARSER_TYPE_PARAMETER_KEY(
        "CPPC/XmlParser/Type" );
      ParserType ConfigurationManager::getParserTypeParameter() {

        const ParameterValueType * parserType = getParameterFromCache(
          CPPC_PARSER_TYPE_PARAMETER_KEY );
        if( parserType == NULL ) {
          parserType = getParameterFromEnvironment(
            CPPC_PARSER_TYPE_PARAMETER_KEY );
        }
        if( parserType != NULL ) {
          return atoi( parserType->c_str() );
        } else {
          return 0;
        }
      }

    }
  }
}
