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



#include <checkpointer/checkpointer.h>
#include <controller/controller.h>
#include <controller/actions/controller_helper.h>
#include <controller/actions/register_action.h>
#include <data/data_type.h>
#include <data/data_type_factory.h>

#include <cassert>
#include <cstring>

using cppc::data::DataType;
using cppc::data::DataTypeFactory;

using cppc::data::Register;
using cppc::data::RegisterVector;

namespace cppc {
  namespace controller {
    namespace actions {

      RegisterAction::RegisterAction( void * data, int size,
        DataType::DataTypeIdentifier type, string name, bool allocMem,
        Controller::ControllerState & state ) : memDir( data ),
        elements( size ), datatype( type ), registerName( name ),
        allocatedMemory( allocMem ), controllerState( state ) {}

      RegisterAction::RegisterAction( const RegisterAction & sa )
        : memDir( sa.memDir ), elements( sa.elements ), datatype( sa.datatype ),
        registerName( sa.registerName ), allocatedMemory( sa.allocatedMemory ),
        controllerState( sa.controllerState ) {}

      RegisterAction & RegisterAction::operator=( const RegisterAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        memDir = rhs.memDir;
        elements = rhs.elements;
        datatype = rhs.datatype;
        registerName = rhs.registerName;
        allocatedMemory = rhs.allocatedMemory;
        controllerState = rhs.controllerState;

        return *this;
      }

      RegisterAction::~RegisterAction() {}

      void * RegisterAction::execute() {

        unsigned int registerSize = DataTypeFactory::instance().getDataTypeSize( datatype ) * elements;
        unsigned char * memContents;

        if( ControllerHelper::getRestartParameter() ) {
          memContents = static_cast<unsigned char *>( controllerState.checkpointer->partialRestart( registerName, registerSize ) );
          if( memContents != 0 ) {
            if( allocatedMemory ) {
              std::memcpy( memDir, memContents, registerSize );
              delete [] memContents;
            } else {
              memDir = memContents;
            }

            addRegister();
          }
        } else {
          addRegister();
        }

        return memDir;
      }

      Register * RegisterAction::addRegister() {
        // Memory is valid
        assert( memDir != 0 );

        DataType::DataTypeSize typeExtent = DataTypeFactory::instance().getDataTypeSize( datatype );

        unsigned long memEnd = reinterpret_cast<unsigned long>( memDir ) + typeExtent * elements;

        Register * r = new Register( memDir, reinterpret_cast<void*>(memEnd), datatype, registerName );
        controllerState.checkpointSkeleton->getContext()->addRegister( r );

        // Build associated memblock in skeleton checkpoint
        ControllerHelper::addMemblocksForRegister( r, controllerState.checkpointSkeleton->getMemBlocks() );
        return r;
      }

    }
  }
}
