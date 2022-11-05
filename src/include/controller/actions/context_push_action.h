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



#if ! defined CONTEXT_PUSH_ACTION_H
#define CONTEXT_PUSH_ACTION_H

#include <controller/controller.h>

using cppc::controller::Controller;

namespace cppc {
  namespace controller {
    namespace actions {

      class ContextPushAction {

      public:
        ContextPushAction( const string &, unsigned int,
          Controller::ControllerState & );
        ~ContextPushAction();

        void execute();

      private:
        ContextPushAction( const ContextPushAction & );
        ContextPushAction & operator=( const ContextPushAction & );

        string functionName;
        unsigned int line;
        Controller::ControllerState & controllerState;
      };

    }
  }
}

#endif
