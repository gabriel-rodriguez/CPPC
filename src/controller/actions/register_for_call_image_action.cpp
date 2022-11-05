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
#include <controller/actions/register_for_call_image_action.h>
#include <data/call_image.h>
#include <data/data_type_factory.h>
#include <data/register.h>

#include <cassert>
#include <cstring>

using cppc::data::CallImage;
using cppc::data::DataTypeFactory;
using cppc::data::Register;

namespace cppc {
  namespace controller {
    namespace actions {

      RegisterForCallImageAction::RegisterForCallImageAction( void * data,
        int size, DataType::DataTypeIdentifier type, const string & s,
        bool allocMem, Controller::ControllerState & state ) : memDir( data ),
        elements( size ), datatype( type ), registerName( s ),
        allocatedMemory( allocMem ), controllerState( state ) {}

      RegisterForCallImageAction::RegisterForCallImageAction(
        const RegisterForCallImageAction & copy ) : memDir( copy.memDir ),
        elements( copy.elements ), datatype( copy.datatype ),
        registerName( copy.registerName ),
        allocatedMemory( copy.allocatedMemory ),
        controllerState( copy.controllerState ) {}

      RegisterForCallImageAction & RegisterForCallImageAction::operator=(
        const RegisterForCallImageAction & rhs ) {

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

      RegisterForCallImageAction::~RegisterForCallImageAction() {}

      void * RegisterForCallImageAction::execute() {

        unsigned int registerSize = DataTypeFactory::instance().getDataTypeSize(
          datatype ) * elements;
        unsigned char * memContents;

        if( ControllerHelper::getRestartParameter() ) {
          memContents = static_cast<unsigned char *>(
            this->getSavedParameter( registerSize ) );

          if( memContents != 0 ) {
            if( !allocatedMemory ) {
              // Return a fresh copy of the data: otherwise maybe the data to be
              // saved in subsequent checkpoints as part of the current CallImage
              // will get modified
              memDir = static_cast<void *>( new unsigned char[ registerSize ] );
            }

            std::memcpy( memDir, memContents, registerSize );
            // Do not delete memContents: they will get saved when checkpointing
            // as part of the current CallImage
          } else {
          }
        } else {
          // Create a new parameter in the current CallImage
          addNewParameter();
        }

        return memDir;
      }

      void * RegisterForCallImageAction::getSavedParameter(
        unsigned int bytes ) {

        CallImage * savedImage = controllerState.checkpointer->getCheckpoint()->
          getContext()->getCurrentCallImage();

        Register * r = savedImage->getParameter( registerName );
        if( r == 0 ) {
          // Parameter not found in this CallImage
          return 0;
        }

        // Copy this parameter to the current CallImage
        CallImage * currentImage = controllerState.checkpointSkeleton->getContext()->getCurrentCallImage();
        currentImage->addParameter( r );
        savedImage->removeParameter( r->name() );

        MemoryBlock::MemoryType bytesOrig =
          reinterpret_cast<MemoryBlock::MemoryType>( r->endAddress() ) -
          reinterpret_cast<MemoryBlock::MemoryType>( r->initAddress() );

        assert( bytes == bytesOrig );

        return r->initAddress();
      }

      void RegisterForCallImageAction::addNewParameter() {

        assert( memDir != 0 );

        DataType::DataTypeSize typeExtent =
          DataTypeFactory::instance().getDataTypeSize( datatype );

        unsigned long memEnd = reinterpret_cast<unsigned long>( memDir ) + typeExtent * elements;
        Register * r = new Register( memDir, (void *)memEnd, datatype, registerName );

        controllerState.checkpointSkeleton->getContext()->getCurrentCallImage()->addParameter( r );
      }

    }
  }
}
