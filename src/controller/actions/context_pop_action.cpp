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



#include <controller/actions/context_pop_action.h>
#include <controller/actions/controller_helper.h>
#include <data/register.h>

using cppc::data::RegisterVector;

#include <cassert>

namespace cppc {
  namespace controller {
    namespace actions {

      ContextPopAction::ContextPopAction( Controller::ControllerState & state )
        : controllerState( state ) {}

      ContextPopAction::~ContextPopAction() {}

      ContextPopAction::ContextPopAction( const ContextPopAction & copy )
        : controllerState( copy.controllerState ) {}

      ContextPopAction & ContextPopAction::operator=(
        const ContextPopAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        controllerState = rhs.controllerState;

        return *this;
      }

      void ContextPopAction::execute() {
        Checkpoint * chkpt = controllerState.checkpointSkeleton;
        Context * current = chkpt->getContext();
        Context * parent = current->getParent();
        assert( parent != 0 );

        // Remove registers inside the popped context. This may imply removing the associated memory blocks.
        RegisterVector * registers = current->getRegisters();
        for( RegisterVector::iterator i = registers->begin(); i != registers->end(); ++i ) {
          ControllerHelper::removeMemblocksForRegister( *i, chkpt->getMemBlocks() );
        }
        current->clearRegisters();

        // Pop the context in the current hierarchy
        if( current->isEmpty() ) {
          parent->removeSubcontext( current );
          delete current;
        }

        controllerState.checkpointSkeleton->setContext( parent );

        if( ControllerHelper::getRestartParameter() ) {
          // Pop the context in the saved hierarchy
          Context * savedContext =
            controllerState.checkpointer->getCheckpoint()->getContext();

          if( savedContext->getParent() == 0 ) {
            return;
          }

          if( *savedContext->getParent() != *controllerState.checkpointSkeleton->getContext() ) {
            return;
          }

          Context * parent = savedContext->getParent();

          if( savedContext->isEmpty() ) {
            parent->removeSubcontext( savedContext );
            delete savedContext;
          }

          controllerState.checkpointer->getCheckpoint()->setContext( parent );
        }
      }

    }
  }
}
