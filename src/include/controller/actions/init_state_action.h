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



#if ! defined CPPC_CONTROLLER_ACTIONS_INIT_STATE_ACTION_H
#define CPPC_CONTROLLER_ACTIONS_INIT_STATE_ACTION_H

#include <controller/controller.h>

#include <string>

using std::string;

using cppc::controller::Controller;

namespace cppc {
  namespace controller {
    namespace actions {

      class InitStateAction {

      public:
        InitStateAction( Controller::ControllerState& );
        ~InitStateAction();

        int execute();

      private:
        InitStateAction( const InitStateAction & );
        InitStateAction & operator=( const InitStateAction & );

        void createCppcDirectory();
        void flushCppcDirectory();
        void prepareRestart();
        bool chkptRead( unsigned );
        int chkptTest( int );


        Controller::ControllerState & controllerState;
      };

    }
  }
}

#endif
