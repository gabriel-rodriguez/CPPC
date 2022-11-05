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




#include <controller/actions/offset_set_action.h>

namespace cppc {
  namespace controller {
    namespace actions {

      OffsetSetAction::OffsetSetAction( CppcFile::FileCode e_code,
        CppcFile::FileOffset e_offset, Controller::ControllerState & c_state )
        : code( e_code ), offset( e_offset ), controllerState( c_state ) {}

      OffsetSetAction::OffsetSetAction( const OffsetSetAction & osa )
      : code( osa.code ), offset( osa.offset ),
        controllerState( osa.controllerState ) {}

      OffsetSetAction & OffsetSetAction::operator=(
        const OffsetSetAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        code = rhs.code;
        offset = rhs.offset;
        controllerState = rhs.controllerState;

        return *this;
      }

      OffsetSetAction::~OffsetSetAction() {}

      void OffsetSetAction::execute() {
        FileMap * files = controllerState.checkpointSkeleton->getFiles();
        FileMap::iterator i = files->find( code );

        if( i == files->end() ) {
          return;
        }

        CppcFile * file = i->second;
        file->set_file_offset( offset );
      }

    }
  }
}
