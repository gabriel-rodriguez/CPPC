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



#include <controller/actions/update_descriptor_action.h>

namespace cppc {
  namespace controller {
    namespace actions {

      UpdateDescriptorAction::UpdateDescriptorAction(
        CppcFile::FileCode e_code, void * e_desc,
        CppcFile::DescriptorType e_type, string e_path,
        Controller::ControllerState & c_state )
        : code( e_code ), descriptor( e_desc ), dtype( e_type ),
          path( e_path ), controllerState( c_state ) {}

      UpdateDescriptorAction::UpdateDescriptorAction(
        const UpdateDescriptorAction & uda )
        : code( uda.code ), descriptor( uda.descriptor ), dtype( uda.dtype ),
          path( uda.path ), controllerState( uda.controllerState ) {}

      UpdateDescriptorAction & UpdateDescriptorAction::operator=(
        const UpdateDescriptorAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        code = rhs.code;
        descriptor = rhs.descriptor;
        dtype = rhs.dtype;
        path = rhs.path;
        controllerState = rhs.controllerState;

        return *this;
      }

      UpdateDescriptorAction::~UpdateDescriptorAction() {}

      void UpdateDescriptorAction::execute() {
        FileMap * files = controllerState.checkpointSkeleton->getFiles();
        FileMap::iterator i = files->find( code );

        if( i == files->end() ) {
          return;
        }

        CppcFile * file = i->second;
        file->set_file_descriptor( descriptor );
        file->set_descriptor_type( dtype );
        file->set_file_path( path );
      }

    }
  }
}
