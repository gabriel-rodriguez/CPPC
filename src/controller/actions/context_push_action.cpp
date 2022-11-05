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



#include <controller/actions/context_push_action.h>
#include <controller/actions/controller_helper.h>
#include <data/heap_context.h>

using cppc::data::HeapContext;

namespace cppc {
  namespace controller {
    namespace actions {

      ContextPushAction::ContextPushAction( const string & s, unsigned int l,
        Controller::ControllerState & state ) : functionName( s ), line( l ),
        controllerState( state ) {}

      ContextPushAction::~ContextPushAction() {}

      ContextPushAction::ContextPushAction( const ContextPushAction & copy )
        : functionName( copy.functionName ), line( copy.line ),
        controllerState( copy.controllerState ) {}

      ContextPushAction & ContextPushAction::operator=(
        const ContextPushAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        functionName = rhs.functionName;
        line = rhs.line;
        controllerState = rhs.controllerState;

        return *this;
      }

      void ContextPushAction::execute() {
        Context * current = controllerState.checkpointSkeleton->getContext();
        HeapContext * newContext = new HeapContext( current, functionName, line );

        // Create a new context in the current hierarchy
        current->addSubcontext( newContext );
        controllerState.checkpointSkeleton->setContext( newContext );

        if( ControllerHelper::getRestartParameter() ) {
          // Was the state initialized yet? If so, we check for the old context.
          // Else, the context will be fetched upon initializing
          if( controllerState.initState ) { 
            // Enter the old copy of the current context in the saved hierarchy
            Context * savedContext =
              controllerState.checkpointer->getCheckpoint()->getContext();
            if( savedContext == 0 ) {
              return;
            }

            vector<Context *> * subcontexts = savedContext->getSubcontexts();
            for( vector<Context *>::iterator it = subcontexts->begin();
              it != subcontexts->end(); it++ ) {

              if( (**it) == (*controllerState.checkpointSkeleton->getContext()) ) {
                controllerState.checkpointer->getCheckpoint()->setContext( *it );
                return;
              }
            }

            // If no matching saved context was found, create an empty one to avoid
            // internal operations to be applied to the current saved context
            HeapContext * emptyContext = new HeapContext( savedContext, functionName, line );
            controllerState.checkpointer->getCheckpoint()->setContext( emptyContext );
          }
        }
      }

    }
  }
}
