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



#include <checkpointer/checkpointer.h>
#include <controller/controller.h>
#include <controller/actions/controller_helper.h>
#include <controller/actions/unregister_action.h>
#include <data/register.h>

#include <cassert>

using cppc::controller::Controller;
using cppc::data::RegisterVector;

namespace cppc {
  namespace controller {
    namespace actions {

      UnregisterAction::UnregisterAction( const string & s,
        Controller::ControllerState & c_state )
        : varName( s ), controllerState( c_state ) {}

      UnregisterAction::UnregisterAction( const UnregisterAction & copy )
        : varName( copy.varName ), controllerState( copy.controllerState ) {}

      UnregisterAction & UnregisterAction::operator=(
        const UnregisterAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        varName = rhs.varName;
        controllerState = rhs.controllerState;

        return *this;
      }

      UnregisterAction::~UnregisterAction() {}

      void UnregisterAction::execute() {
        Context * current = controllerState.checkpointSkeleton->getContext();
        Register * r = current->getRegister( varName );
        if( r ) {
          ControllerHelper::removeMemblocksForRegister( r, controllerState.checkpointSkeleton->getMemBlocks() );
          current->removeRegister( varName );
        }
      }
    }
  }
}
