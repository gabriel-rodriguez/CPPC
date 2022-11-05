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
#include <controller/actions/shutdown_action.h>
#include <data/memory_block.h>
#include <util/filemanager/directory_stream.h>
#include <util/filemanager/file_system_manager.h>

#include <cassert>

using cppc::controller::actions::ControllerHelper;
using cppc::data::MemoryBlock;
using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterValueType;
using cppc::util::filesystem::DirectoryStream;
using cppc::util::filesystem::FileSystemManager;

namespace cppc {
  namespace controller {
    namespace actions {

      ShutdownAction::ShutdownAction( Controller::ControllerState & c_state )
        : controllerState( c_state ) {}

      ShutdownAction::ShutdownAction( const ShutdownAction & sa )
        : controllerState( sa.controllerState ) {}

      ShutdownAction & ShutdownAction::operator=( const ShutdownAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        controllerState = rhs.controllerState;

        return *this;
      }

      ShutdownAction::~ShutdownAction() {}

      int ShutdownAction::execute() {
        assert( controllerState.initConfig );
        assert( controllerState.initState );

        Checkpointer::Shutdown();

        if( getDeleteCheckpointsParameter() ) {

          DirectoryStream * stream = FileSystemManager::openDirectory( controllerState.localDir );

          while( stream->hasNext() ) {
            string path = stream->next();
            if( ( path != "." ) && ( path != ".." ) ) {
              FileSystemManager::removeFile( path );
            }
          }

          delete stream;

          FileSystemManager::removeDirectory( controllerState.localDir );

          string rootDir = ControllerHelper::getRootDirectoryParameter();
          int aux = controllerState.localDir.rfind( '/' );
          controllerState.localDir = controllerState.localDir.substr( 0, aux );
          aux = controllerState.localDir.rfind( '/' );
          while( ( controllerState.localDir + '/' ) != rootDir ) {
            controllerState.localDir = controllerState.localDir.substr( 0, aux );
            FileSystemManager::removeDirectory( controllerState.localDir );
            aux = controllerState.localDir.rfind( '/' );
          }
        }

        controllerState.initConfig = false;
        controllerState.initState = false;
        // Delete the context hierarchy
        delete ControllerHelper::getHierarchyRoot( controllerState.checkpointSkeleton->getContext() );

        // Delete memory blocks in the skeleton checkpoint (these are not deleted by ~Checkpoint()
        // to enable reuse and sharing of hashcodes)
        BlockMap * memBlocks = controllerState.checkpointSkeleton->getMemBlocks();
        for( BlockMap::iterator it = memBlocks->begin(); it != memBlocks->end(); ++it ) {
          delete it->second;
        }
        delete memBlocks;

        // Delete the skeleton checkpoint
        delete controllerState.checkpointSkeleton;

        // Free cppc::data::MemoryBlock static memory
        MemoryBlock::freeStaticMemory();

        return 0;
      }

      const ParameterKeyType ShutdownAction::CPPC_DELETE_CHECKPOINTS_PARAMETER_KEY( "CPPC/Controller/DeleteCheckpoints" );
      bool ShutdownAction::getDeleteCheckpointsParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_DELETE_CHECKPOINTS_PARAMETER_KEY );
        static const bool deleteCheckpoints = ( value != NULL ) && ( (*value) == "true" );
        return deleteCheckpoints;
      }

    }
  }
}

