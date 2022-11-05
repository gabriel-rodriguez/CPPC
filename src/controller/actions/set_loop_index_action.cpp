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
#include <controller/actions/set_loop_index_action.h>
#include <data/data_type.h>
#include <data/data_type_factory.h>
#include <data/loop_context.h>

using cppc::data::DataType;
using cppc::data::DataTypeFactory;
using cppc::data::LoopContext;

namespace cppc {
  namespace controller {
    namespace actions {

      SetLoopIndexAction::SetLoopIndexAction( void * d,
        Controller::ControllerState & s ) : data( d ), controllerState( s ) {}

      SetLoopIndexAction::SetLoopIndexAction( const SetLoopIndexAction & copy )
        : data( copy.data ), controllerState( copy.controllerState ) {}

      SetLoopIndexAction & SetLoopIndexAction::operator=(
        const SetLoopIndexAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        data = rhs.data;
        controllerState = rhs.controllerState;

        return *this;
      }

      SetLoopIndexAction::~SetLoopIndexAction() {}

      void SetLoopIndexAction::execute() {

        bool firstIndex = ( controllerState.loopVarName != 0 );

        // If the variable name is stored in the controller state: first context
        // for the current loop
        Context * current = controllerState.checkpointSkeleton->getContext();
        if( firstIndex ) {
          // Create a new loop context with the corresponding data as index
          DataType * indexData = DataTypeFactory::instance().getDataType(
            controllerState.loopVarType, data );
          LoopContext * newContext = new LoopContext( current, *controllerState.loopVarName, indexData );

          // Add the new context to the current hierarchy
          current->addSubcontext( newContext );
          controllerState.checkpointSkeleton->setContext( newContext );
          delete controllerState.loopVarName;
          controllerState.loopVarName = 0;
        } else {
          // If the variable for the name is null, then clone the current
          // context. Rationale: first time create the context, subsequent
          // times just clone the current one
          LoopContext * newContext = static_cast<LoopContext*>( current )->shallowClone();
          DataType * indexData = DataTypeFactory::instance().getDataType(
            controllerState.loopVarType, data );
          newContext->setVarValue( indexData );

          // If the context is empty: remove
          Context * parent = current->getParent();
          if( current->isEmpty() ) {
            parent->removeSubcontext( current );
            delete current;
          }

          // Add the new context to the parent context of the current one
          parent->addSubcontext( newContext );
          controllerState.checkpointSkeleton->setContext( newContext );
        }

        if( ControllerHelper::getRestartParameter() ) {
          // Enter the old copy of the current context in the saved hierarchy
          Context * savedContext = controllerState.checkpointer->getCheckpoint()->
            getContext();
          if( !firstIndex ) {
            // If this is not the first loop context in this loop: fetch the parent,
            // since the current context is just another loop context for this loop.
            savedContext = savedContext->getParent();
          }

	  if( savedContext == 0 ) {
	    return;
	  }

          vector<Context *> * subcontexts = savedContext->getSubcontexts();
          for( vector<Context *>::iterator it = subcontexts->begin(); it != subcontexts->end(); it++ ) {
            if( (**it) == (*controllerState.checkpointSkeleton->getContext()) ) {
              controllerState.checkpointer->getCheckpoint()->setContext( *it );
              return;
            }
          }
        }
      }

    }
  }
}
