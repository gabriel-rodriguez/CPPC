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
#include <controller/actions/register_descriptor_action.h>
#include <util/filemanager/file_system_manager.h>

using cppc::util::filesystem::FileSystemManager;

namespace cppc {
  namespace controller {
    namespace actions {

      RegisterDescriptorAction::RegisterDescriptorAction(
        CppcFile::FileCode e_code, void * e_descriptor,
        CppcFile::DescriptorType e_type, string e_path,
        Controller::ControllerState & c_state )
        : code( e_code ), descriptor( e_descriptor ), type( e_type ),
          path( e_path ), controllerState( c_state ) {}

      RegisterDescriptorAction::RegisterDescriptorAction(
        const RegisterDescriptorAction & rda )
        : code( rda.code ), descriptor( rda.descriptor ), type( rda.type ),
          path( rda.path ), controllerState( rda.controllerState ) {}

      RegisterDescriptorAction & RegisterDescriptorAction::operator=(
        const RegisterDescriptorAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        code = rhs.code;
        descriptor = rhs.descriptor;
        type = rhs.type;
        path = rhs.path;
        controllerState = rhs.controllerState;

        return *this;
      }

      RegisterDescriptorAction::~RegisterDescriptorAction() {}

      CppcFile * RegisterDescriptorAction::execute() {
        CppcFile * file = 0;

        if( ControllerHelper::getRestartParameter() ) {
          FileMap * files = controllerState.checkpointer->getCheckpoint()->getFiles();
          FileMap::const_iterator i = files->find( code );
          if( i == files->end() ) {
            return 0;
          } else {
            // When the restart ends, the checkpoint used for restart is deleted
            // along with its files. Therefore, make a clone for use in the
            // skeleton checkpoint.
            file = new CppcFile( *i->second );
          }
        } else {
          file = new CppcFile( code, descriptor, type, 0, path );
        }

        FileMap * fmap = controllerState.checkpointSkeleton->getFiles();
        fmap->insert( FileMap::value_type( code, file ) );

        return file;
      }

    }
  }
}
