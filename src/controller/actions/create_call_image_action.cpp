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
#include <controller/actions/create_call_image_action.h>
#include <data/call_image.h>

using cppc::data::CallImage;

namespace cppc {
  namespace controller {
    namespace actions {

      CreateCallImageAction::CreateCallImageAction( string & s, unsigned int l,
        Controller::ControllerState & state ) : functionName( s ), line( l ),
        controllerState( state ) {}

      CreateCallImageAction::~CreateCallImageAction() {}

      CreateCallImageAction::CreateCallImageAction(
        const CreateCallImageAction & copy )
        : functionName( copy.functionName ), line( copy.line ),
        controllerState( copy.controllerState ) {}

      CreateCallImageAction & CreateCallImageAction::operator=( const CreateCallImageAction & rhs ) {
        if( this == &rhs ) {
          return *this;
        }

        functionName = rhs.functionName;
        line = rhs.line;
        controllerState = rhs.controllerState;

        return *this;
      }

      void CreateCallImageAction::execute() {
        // Create a new CallImage as requested into the current Context
        CallImage * newImage = new CallImage( functionName, line );
        controllerState.checkpointSkeleton->getContext()->addCallImage( newImage );

        // If a restart is taking place, fetch the old CallImage
        if( ControllerHelper::getRestartParameter() ) {
          controllerState.checkpointer->getCheckpoint()->getContext()->fetchCallImage( functionName, line );
        }
      }
    }
  }
}
