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
#include <controller/actions/remove_loop_index_action.h>

#include <cassert>

namespace cppc {
  namespace controller {
    namespace actions {

      RemoveLoopIndexAction::RemoveLoopIndexAction(
        Controller::ControllerState & s ) : controllerState( s ) {}

      RemoveLoopIndexAction::RemoveLoopIndexAction( const RemoveLoopIndexAction & copy )
        : controllerState( copy.controllerState ) {}

      RemoveLoopIndexAction & RemoveLoopIndexAction::operator=(
        const RemoveLoopIndexAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        controllerState = rhs.controllerState;

        return *this;
      }

      RemoveLoopIndexAction::~RemoveLoopIndexAction() {}

      void RemoveLoopIndexAction::execute() {
        bool firstLoop = ( controllerState.loopVarName != 0 );
        Context * current = controllerState.checkpointSkeleton->getContext();
        assert( current->getParent() != 0 );

        // Pop the context in the current hierarchy
        // Warning: it may happen that the execution flow has not entered the
        // loop. Hence, it may not be correct to remove the current context
        if( firstLoop ) {
          delete controllerState.loopVarName;
          controllerState.loopVarName = 0;
        } else {
          controllerState.checkpointSkeleton->setContext( current->getParent() );
        }

        if( ControllerHelper::getRestartParameter() ) {

          if( !firstLoop ) {
            // Pop the context in the saved hierarchy
            Context * savedContext =
              controllerState.checkpointer->getCheckpoint()->getContext();

            if( savedContext == 0 ) {
              return;
            }

            Context * parent = savedContext->getParent();

            if( savedContext->isEmpty() ) {
              delete savedContext;
            }

            controllerState.checkpointer->getCheckpoint()->setContext( parent );
          }
        }
      }

    }
  }
}
