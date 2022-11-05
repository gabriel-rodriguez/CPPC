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
#include <controller/actions/commit_call_image_action.h>
#include <data/call_image.h>

using cppc::data::CallImage;

namespace cppc {
  namespace controller {
    namespace actions {

      CommitCallImageAction::CommitCallImageAction( Controller::ControllerState & state )
      : controllerState( state ) {}

      CommitCallImageAction::CommitCallImageAction( const CommitCallImageAction & copy )
      : controllerState( copy.controllerState ) {}

      CommitCallImageAction::~CommitCallImageAction() {}

      CommitCallImageAction & CommitCallImageAction::operator=( const CommitCallImageAction & rhs ) {
        if( this == &rhs ) {
          return *this;
        }

        controllerState = rhs.controllerState;

        return *this;
      }

      void CommitCallImageAction::execute() {
        if( ControllerHelper::getRestartParameter() ) {
          controllerState.checkpointer->getCheckpoint()->getContext()->removeCurrentCallImage();
        }

        Context *current = controllerState.checkpointSkeleton->getContext();
        CallImage *callImage = current->getCurrentCallImage();
        vector<Register*> * params = callImage->getParameters();
        for(vector<Register*>::iterator it = params->begin(); it != params->end();it++){
          ControllerHelper::addMemblocksForRegister(*it, callImage->getBlocks());
        }

        current->getCurrentCallImage()->splitMemory();
        current->releaseCallImage();
      }

    }
  }
}
