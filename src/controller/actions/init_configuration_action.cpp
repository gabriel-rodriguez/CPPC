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



#include <controller/actions/controller_helper.h>
#include <controller/actions/init_configuration_action.h>
#include <data/heap_context.h>
#include <util/configuration/configuration_manager.h>

#include <cassert>

using cppc::data::HeapContext;
using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterType;
using cppc::util::configuration::ParameterVector;

namespace cppc {
  namespace controller {
    namespace actions {

      const string InitConfigurationAction::CPPC_ARGS_PREFIX( "-CPPC," );

      InitConfigurationAction::InitConfigurationAction( int * e_argsno,
        char *** e_args, Controller::ControllerState & c_state )
        : argsno( e_argsno ), args( e_args ), controllerState( c_state ) {}

      InitConfigurationAction::InitConfigurationAction(
        const InitConfigurationAction & ia ) : argsno( ia.argsno ),
        args( ia.args ), controllerState( ia.controllerState ) {}

      InitConfigurationAction & InitConfigurationAction::operator=(
        const InitConfigurationAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        argsno = rhs.argsno;
        args = rhs.args;
        controllerState = rhs.controllerState;

        return *this;
      }

      InitConfigurationAction::~InitConfigurationAction() {}

      int InitConfigurationAction::execute() {

        assert( !controllerState.initConfig );

        // Initialization of the ConfigurationManager
        initConfigurationManager( argsno, args );

        // Register the application id
        controllerState.appName =
          ControllerHelper::getApplicationNameParameter();

        // Create the checkpoint skeleton
        string main_name( "main" );
        Context * root = new HeapContext( 0, main_name, 0 );
        controllerState.checkpointSkeleton = new Checkpoint( ControllerHelper::getWriterTypeParameter(), 0, root );

        // Assertion of the Controller::running variable, we are almost ready to go!
        controllerState.initConfig = true;

        return 0;
      }

      void InitConfigurationAction::initConfigurationManager( int * argsno,
        char *** args ) {

        ParameterVector configuration;

        for( int i = 1; i < (*argsno); i++ ) {

          string parameter = (*args)[ i ];
          if( parameter.find( CPPC_ARGS_PREFIX ) == 0 ) {

            parameter.erase( 0, CPPC_ARGS_PREFIX.length() );

            while( parameter.length() != 0 ) {
              ParameterType newParameter;
              string element = parameter.substr( 0, parameter.find( "," ) );
              newParameter.key = element.substr( 0, element.find( "=" ) );
              element.erase( 0, element.find( "=" ) + 1 );
              newParameter.value = element;

              configuration.push_back( newParameter );

              int cut_pos = parameter.find( "," );
              if( cut_pos != -1 ) {
                parameter.erase( 0, cut_pos + 1 );
              } else {
                parameter.erase( 0, cut_pos );
              }

            }

            deleteCppcParameter( argsno, args, i );
          }
        }

        ConfigurationManager::instance().init( configuration );
      }

      void InitConfigurationAction::deleteCppcParameter( int * argsno,
        char *** args, int position ) {

        for( int i = position; i < (*argsno)-1; i++ ) {
          (*args)[i] = (*args)[i+1];
        }
        (*argsno)--;
      }

    }
  }
}
