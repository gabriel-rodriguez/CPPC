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



#include <checkpointer/checkpoint.h>
#include <controller/actions/controller_helper.h>
#include <controller/actions/init_state_action.h>
#include <data/cppc_basic.h>
#include <util/communication/communication_manager.h>
#include <util/filemanager/directory_stream.h>
#include <util/filemanager/file_system_manager.h>

#include <cassert>
#include <cstdlib>
#include <iostream>
#include <stack>
#include <sstream>

using std::cout;
using std::stack;
using std::ostringstream;

using cppc::checkpointer::Checkpoint;
using cppc::data::IntegerType;
using cppc::util::communication::CommunicationManager;
using cppc::util::filesystem::DirectoryStream;
using cppc::util::filesystem::FileSystemManager;

namespace cppc {
  namespace controller {
    namespace actions {

      InitStateAction::InitStateAction( Controller::ControllerState & c_state )
        : controllerState( c_state ) {}

      InitStateAction::InitStateAction( const InitStateAction & ia )
        : controllerState( ia.controllerState ) {}

      InitStateAction & InitStateAction::operator=(
        const InitStateAction & rhs ) {

        if( this == &rhs ) {
          return *this;
        }

        controllerState = rhs.controllerState;

        return *this;
      }

      InitStateAction::~InitStateAction() {}

      int InitStateAction::execute() {

        assert( !controllerState.initState );

        // Creation of the CPPC work directory, if it has not been created yet.
        createCppcDirectory();

        // Assertion of the Controller::running variable, we are almost ready to
        // go!
        controllerState.initState = true;

        // If we are restarting from a checkpoint, we must be prepared for it
        if( ControllerHelper::getRestartParameter() ) {
          prepareRestart();

          // Check if we have already entered some context
          Context * current = controllerState.checkpointSkeleton->getContext();
          if( current->getParent() != 0 ) {
            // Rebuild the context hierarchy in the Checkpoint object
            Context * root = current;
            Context * savedRoot = controllerState.checkpointer->getCheckpoint()->getContext();
            stack<Context *> route;
            while( root->getParent() != 0 ) { 
              route.push( root );
              root = root->getParent();
            }

            bool found = true;

            // This loop ends if:
            // 1. The current context has been found
            // 2. There's no such context in the saved hierarchy (because they
            //    had no registers)
            while( !route.empty() && found ) {
              assert( savedRoot != 0 );

              root = route.top();
              found = false;

              vector<Context *> * subcontexts = savedRoot->getSubcontexts();
              for( vector<Context *>::iterator it = subcontexts->begin();
                (it != subcontexts->end()) && !found; it++ ) {

                if( (**it) == (*root) ) {
                  controllerState.checkpointer->getCheckpoint()->setContext(
                    *it );
                  route.pop();
                  found = true;
                }
              }
            }
          }
        }

        // Waiting for other processes and ignition...
        CommunicationManager::barrier();

        return 0;
      }

      void InitStateAction::createCppcDirectory() {

        string commonDir = ControllerHelper::getRootDirectoryParameter();

        if( commonDir.rfind('/') != commonDir.length()-1 ) {
          commonDir += "/";
        }

        ControllerHelper::createGenericDirectory( commonDir.c_str() );

        commonDir = commonDir + controllerState.appName + "/";
        ControllerHelper::createGenericDirectory( commonDir.c_str() );

        CommunicationManager::RankType rank = CommunicationManager::getRank();

        ostringstream rankStream;
        rankStream << rank;
        controllerState.localDir = commonDir + rankStream.str() + "/";
        ControllerHelper::createGenericDirectory( controllerState.localDir );

        if( !ControllerHelper::getRestartParameter() ) {
          flushCppcDirectory();
        }
      }

      void InitStateAction::flushCppcDirectory() {

        DirectoryStream * stream = FileSystemManager::openDirectory(
          controllerState.localDir );

        while( stream->hasNext() ) {
          if( ( stream->next() != "." ) && ( stream->next() != ".." ) ) {
            string file = controllerState.localDir + stream->next();
            FileSystemManager::removeFile( file );
          }
        }

        delete stream;
      }

      // Agreement is reached here and then the checkpoint is read.
      void InitStateAction::prepareRestart() {
        // 1. Determination of the file that has the necessary data
        // In order to achieve this we must communicate with the other processes
        // to check which one is the minimum of the maximum epochs. We first
        // test which one is the last checkpoint in THIS process

        int last_chkpt;
        string f_name;

        CommunicationManager::RankType rank = CommunicationManager::getRank();

        bool acuerdo = false;
        DirectoryStream * stream = FileSystemManager::openDirectory(
          controllerState.localDir );

        last_chkpt = -1;
        while( stream->hasNext() ) {
          string nombre( stream->next() );

          if( ( nombre != "." ) && ( nombre != ".." ) ) {
            int aux = nombre.find( '.' );
            string numero = nombre.substr( 0, aux );

            if( std::atoi( numero.c_str() ) > last_chkpt ) {
              last_chkpt = std::atoi( numero.c_str() );
            }
          }
        }

        delete stream;

        while( !acuerdo && ( last_chkpt >= 0 ) ) {
          acuerdo = true;

          // Look for a local checkpoint that does not contain errors.
          int new_last;
          while( ( last_chkpt >= 0 ) && ( ( new_last = chkptTest( last_chkpt ) ) != last_chkpt ) ) {
            string path = ControllerHelper::getChkptPath( controllerState.localDir, new_last+1 );
            cout << "CPPC: Process rank " << rank << " ignoring inconsistent " << "file: \"" << path << "\". Reason: integrity checks failed.\n";
            last_chkpt = new_last;
          }

          // Negotiate whether this local checkpoint is part of the recovery line
          IntegerType lastChkpt( last_chkpt );
          IntegerType * minBuffer = static_cast<IntegerType *>( CommunicationManager::reductionOnMin( &lastChkpt ) );
          IntegerType * maxBuffer = static_cast<IntegerType *>( CommunicationManager::reductionOnMax( &lastChkpt ) );

          if( maxBuffer->getStaticValue() > last_chkpt ) {
            acuerdo = false;
          }
          if( minBuffer->getStaticValue() < last_chkpt ) {
            last_chkpt = minBuffer->getStaticValue();
            acuerdo = false;
          }

          delete minBuffer;
          delete maxBuffer;
        }

        if( (last_chkpt == -1) && (rank == 0) ) {
          cout << "CPPC: ERROR: No checkpoint files available for restart. Execution cancelled.\n";
          exit(-1);
        }

        controllerState.epoch = last_chkpt+1;
        chkptRead( last_chkpt );
      }

      bool InitStateAction::chkptRead( unsigned code ) {
        unsigned full = code;
        string path = ControllerHelper::getChkptPath( controllerState.localDir, full );
        while( !Checkpointer::isFullCheckpoint( path ) ) {
          path = ControllerHelper::getChkptPath( controllerState.localDir, --full );
        }

        Checkpoint * checkpoint = new Checkpoint();
        controllerState.checkpointer = new Checkpointer( checkpoint );
        controllerState.lastCheckpointTouched = controllerState.checkpointer->readCheckpoint( path );
        controllerState.recoverySets.push_back( full );
        controllerState.checkpointer->getCheckpoint()->unpack();
        if( CommunicationManager::getRank() == 0 ) {
          std::cout << "Restarting from full checkpoint: " << path << "\n";
        }

        for( unsigned i = full+1; i <= code; ++i ) {
          path = ControllerHelper::getChkptPath( controllerState.localDir, i );
          Checkpointer * checkpointer = new Checkpointer();
          controllerState.lastCheckpointTouched = checkpointer->readCheckpoint( path );
          controllerState.checkpointer->getCheckpoint()->update( checkpointer->getCheckpoint() );
          delete checkpointer;
          if( CommunicationManager::getRank() == 0 ) {
            std::cout << "Incrementally applying checkpoint: " << path << "\n";
          }
        }

        controllerState.checkpointer->getCheckpoint()->independentOffsetsToAddresses();

        return true;
      }

      int InitStateAction::chkptTest( int last_chkpt ) {
        string path = ControllerHelper::getChkptPath( controllerState.localDir, last_chkpt );
        if( !Checkpointer::integrityTest(path) ) {
          return last_chkpt-1;
        }

        // If the file is consistent, but it is an incremental checkpoint, correctness is subject to all the previous checkpoints up to the last
        // full one being correct as well.
        if( !Checkpointer::isFullCheckpoint( path ) ) {
          int new_last = this->chkptTest( last_chkpt-1 );
          if( new_last != last_chkpt-1 ) {
            return new_last;
          }
        }

        return last_chkpt;
      }
    }
  }
}
