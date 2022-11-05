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



#include <controller/actions/unregister_descriptor_action.h>

namespace cppc {
  namespace controller {
    namespace actions {

      UnregisterDescriptorAction::UnregisterDescriptorAction( void * e_desc,
        Controller::ControllerState & c_state )
        : descriptor( e_desc ), controllerState( c_state ) {}

      UnregisterDescriptorAction::UnregisterDescriptorAction(
        const UnregisterDescriptorAction & rda )
        : descriptor( rda.descriptor ),
          controllerState( rda.controllerState ) {}

      UnregisterDescriptorAction & UnregisterDescriptorAction::operator=(
        const UnregisterDescriptorAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        descriptor = rhs.descriptor;
        controllerState = rhs.controllerState;

        return *this;
      }

      UnregisterDescriptorAction::~UnregisterDescriptorAction() {}

      void UnregisterDescriptorAction::execute() {
        FileMap * files = controllerState.checkpointSkeleton->getFiles();
        for( FileMap::iterator i = files->begin(); i != files->end(); i++ ) {
          if( i->second->get_file_descriptor() == descriptor ) {
            files->erase( i );
            return;
          }
        }
      }

    }
  }
}
