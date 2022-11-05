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



#if ! defined CPPC_CONTROLLER_ACTIONS_INIT_CONFIGURATION_ACTION_H
#define CPPC_CONTROLLER_ACTIONS_INIT_CONFIGURATION_ACTION_H

#include <controller/controller.h>

#include <string>

using cppc::controller::Controller;

using std::string;

namespace cppc {
  namespace controller {
    namespace actions {

      class InitConfigurationAction {

      public:
        InitConfigurationAction( int *, char ***,
          Controller::ControllerState& );
        ~InitConfigurationAction();

        int execute();

      private:
        InitConfigurationAction( const InitConfigurationAction & );
        InitConfigurationAction & operator=( const InitConfigurationAction & );

        void initConfigurationManager( int *, char *** );
        void deleteCppcParameter( int *, char ***, int );

        int * argsno;
        char *** args;
        Controller::ControllerState & controllerState;

        const static string CPPC_ARGS_PREFIX;
      };

    }
  }
}

#endif
