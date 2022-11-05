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



#include <controller/actions/add_loop_index_action.h>

namespace cppc {
  namespace controller {
    namespace actions {

      AddLoopIndexAction::AddLoopIndexAction( const string & s,
        DataType::DataTypeIdentifier t, Controller::ControllerState & state )
        : varName( s ), varType( t ), controllerState( state ) {}

      AddLoopIndexAction::AddLoopIndexAction( const AddLoopIndexAction & copy )
        : varName( copy.varName ), varType( copy.varType ),
        controllerState( copy.controllerState ) {}

      AddLoopIndexAction & AddLoopIndexAction::operator=(
        const AddLoopIndexAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        varName = rhs.varName;
        varType = rhs.varType;
        controllerState = rhs.controllerState;

        return *this;
      }

      AddLoopIndexAction::~AddLoopIndexAction() {}

      void AddLoopIndexAction::execute() {
        // Store the loop variable name and type into the controller state
        controllerState.loopVarName = new string( varName );
        controllerState.loopVarType = varType;
      }
    }
  }
}
