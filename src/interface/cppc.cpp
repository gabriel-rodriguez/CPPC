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



extern "C" {
  #include <cppc.h>
}
#include <controller/controller.h>
#include <util/filemanager/file_system_manager.h>

#include <map>

using cppc::controller::Controller;
using cppc::util::filesystem::FileSystemManager;

using std::map;

namespace cppc {
  namespace interface {
    namespace c {

      typedef struct {
        void * descriptor;
        CppcFile::DescriptorType type;
        CppcFile::FileCode code;
      } RegisteredFile;

      typedef map<CppcFile::FileCode,RegisteredFile> RegisteredFiles;
      RegisteredFiles registeredFiles;
    }
  }
}

extern "C" {

  void * CPPC_Register( void * data, int nelems, CPPC_Datatype type,
    char * name, unsigned char isPointer ) {

    return (Controller::instance()).CPPC_Register( data, nelems, type,
      string( name ), !isPointer );
  }

  void CPPC_Unregister( char * varName ) {
    (Controller::instance()).CPPC_Unregister( string( varName ) );
  }

  void CPPC_Do_checkpoint(unsigned char num) {

    // Update file offsets before checkpointing
    for( cppc::interface::c::RegisteredFiles::iterator i =
      cppc::interface::c::registeredFiles.begin(); i !=
      cppc::interface::c::registeredFiles.end(); i++ ) {

      CppcFile::FileOffset offset = FileSystemManager::offsetGet(
        i->second.descriptor, i->second.type );
      Controller::instance().CPPC_Offset_set( i->second.code, offset );
    }

    Controller::instance().CPPC_Do_checkpoint(num);
  }

  int CPPC_Init_configuration( int *argno, char ***args ) {
    return Controller::instance().CPPC_Init_configuration( argno, args );
  }

  int CPPC_Init_state() {
    return Controller::instance().CPPC_Init_state();
  }

  void CPPC_Shutdown() {
    Controller::instance().CPPC_Shutdown();
  }

  int CPPC_Jump_next() {
    return (int)(Controller::instance()).CPPC_Jump_next();
  }

  void * CPPC_Register_descriptor( int code, void * descriptor,
    CPPC_DescriptorType type, char * path ) {

    cppc::interface::c::RegisteredFiles::iterator i =
      cppc::interface::c::registeredFiles.find( code );

    // If the file has already been registered, return the descriptor
    if( i != cppc::interface::c::registeredFiles.end() ) {
      return i->second.descriptor;
    }

    // Else, create/recover the CppcFile object
    CppcFile * file = Controller::instance().CPPC_Register_descriptor( code, descriptor, type, string( path ) );

    if( file ) {
      if( CPPC_Jump_next() ) {
        // If the object exists AND this is a restart, we need to open and reposition the file
        descriptor = FileSystemManager::openFile( descriptor, file->get_descriptor_type(), file->get_file_path() );

        // Update descriptor into controller
        Controller::instance().CPPC_Update_descriptor( code, descriptor, file->get_descriptor_type(), file->get_file_path() );
      }

      // In any case, the descriptor needs to be cached, because the file exists and is now registered
      // Note that it did not exist before
      cppc::interface::c::RegisteredFile rfile;
      rfile.descriptor = descriptor;
      rfile.type = type;
      rfile.code = code;
      cppc::interface::c::registeredFiles.insert( cppc::interface::c::RegisteredFiles::value_type( code, rfile ) );

      if( file->get_file_offset() == -1 ) {
        return descriptor;
      }

      FileSystemManager::offsetSeek( descriptor, file->get_descriptor_type(), file->get_file_offset() );
    }

    return descriptor;
  }

  void CPPC_Unregister_descriptor( void * fd ) {

    for( cppc::interface::c::RegisteredFiles::iterator i =
      cppc::interface::c::registeredFiles.begin(); i !=
      cppc::interface::c::registeredFiles.end(); i++ ) {

        if( i->second.descriptor == fd ) {
          cppc::interface::c::registeredFiles.erase( i );
          Controller::instance().CPPC_Unregister_descriptor( fd );
          return;
        }
    }
  }

  void CPPC_Context_push( char * name, unsigned int calledFromLine ) {
    Controller::instance().CPPC_Context_push( string( name ), calledFromLine );
  }

  void CPPC_Context_pop() {
    Controller::instance().CPPC_Context_pop();
  }

  void CPPC_Add_loop_index( char * name, CPPC_Datatype type ) {
    Controller::instance().CPPC_Add_loop_index( string( name ), type );
  }

  void CPPC_Set_loop_index( void * data ) {
    Controller::instance().CPPC_Set_loop_index( data );
  }

  void CPPC_Remove_loop_index() {
    Controller::instance().CPPC_Remove_loop_index();
  }

  void CPPC_Create_call_image( char * name, unsigned int calledFromLine ) {
    Controller::instance().CPPC_Create_call_image( string( name ),
      calledFromLine );
  }

  void * CPPC_Register_for_call_image( void * data, int nelems,
    CPPC_Datatype type, char * name, unsigned char isPointer ) {

    return Controller::instance().CPPC_Register_for_call_image( data, nelems,
      type, string( name ), !isPointer );
  }

  void CPPC_Commit_call_image() {
    Controller::instance().CPPC_Commit_call_image();
  }
}
