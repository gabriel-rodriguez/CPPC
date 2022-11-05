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



#include <autoconf.h>
#include <controller/controller.h>

using cppc::controller::Controller;

#include <cstring>
#include <map>
using std::map;

namespace cppc {
  namespace interface {
    namespace fortran {
      typedef map<int, CppcFile::FileCode> UnitMap;
      UnitMap unitCorrespondence;
    }
  }
}

// Mangling autoconf macros for Fortran compiler compatibility
#define CPPC_ADD_LOOP_INDEX_FUNCTION F77_FUNC_( cppc_add_loop_index, \
  CPPC_ADD_LOOP_INDEX )
#define CPPC_COMMIT_CALL_IMAGE_FUNCTION F77_FUNC_( cppc_commit_call_image, \
  CPPC_COMMIT_CALL_IMAGE )
#define CPPC_CONTEXT_POP_FUNCTION F77_FUNC_( cppc_context_pop, CPPC_CONTEXT_POP )
#define CPPC_CONTEXT_PUSH_FUNCTION F77_FUNC_( cppc_context_push, \
  CPPC_CONTEXT_PUSH )
#define CPPC_CREATE_CALL_IMAGE_FUNCTION F77_FUNC_( cppc_create_call_image, \
  CPPC_CREATE_CALL_IMAGE )
#define CPPC_DO_CHECKPOINT_FUNCTION F77_FUNC_( cppc_do_checkpoint, \
  CPPC_DO_CHECKPOINT )
#define CPPC_INIT_CONFIGURATION_FUNCTION F77_FUNC_( cppc_init_configuration,\
  CPPC_INIT_CONFIGURATION )
#define CPPC_INIT_STATE_FUNCTION F77_FUNC_( cppc_init_state, CPPC_INIT_STATE )
#define CPPC_JUMP_NEXT_FUNCTION F77_FUNC_( cppc_jump_next, CPPC_JUMP_NEXT )
#define CPPC_NEXT_UNIT_FUNCTION F77_FUNC_( cppc_next_unit, CPPC_NEXT_UNIT )
#define CPPC_OFFSET_SET_FUNCTION F77_FUNC_( cppc_offset_set, CPPC_OFFSET_SET )
#define CPPC_REGISTER_DESCRIPTOR_FUNCTION F77_FUNC_( cppc_register_descriptor, \
  CPPC_REGISTER_DESCRIPTOR )
#define CPPC_REGISTER_FOR_CALL_IMAGE_FUNCTION F77_FUNC_( \
  cppc_register_for_call_image, CPPC_REGISTER_FOR_CALL_IMAGE )
#define CPPC_REGISTER_FUNCTION F77_FUNC_( cppc_register, CPPC_REGISTER )
#define CPPC_REMOVE_LOOP_INDEX_FUNCTION F77_FUNC_( cppc_remove_loop_index, \
  CPPC_REMOVE_LOOP_INDEX )
#define CPPC_SET_LOOP_INDEX_FUNCTION F77_FUNC_( cppc_set_loop_index, \
  CPPC_SET_LOOP_INDEX )
#define CPPC_SHUTDOWN_FUNCTION F77_FUNC_( cppc_shutdown, CPPC_SHUTDOWN )
#define CPPC_UNREGISTER_DESCRIPTOR_FUNCTION F77_FUNC_( \
  cppc_unregister_descriptor, CPPC_UNREGISTER_DESCRIPTOR )
#define CPPC_UNREGISTER_FUNCTION F77_FUNC_( cppc_unregister, CPPC_UNREGISTER )
#define CPPC_UPDATE_DESCRIPTOR_FUNCTION F77_FUNC_( cppc_update_descriptor, \
  CPPC_UPDATE_DESCRIPTOR )

extern "C" {

  void CPPC_REGISTER_FUNCTION( void * data, int * nelems, CPPC_Datatype * type,
    char * name ) {

    Controller::instance().CPPC_Register( data, *nelems, *type, string( name ),
      true );
  }

  void CPPC_UNREGISTER_FUNCTION( char * name ) {
    Controller::instance().CPPC_Unregister( string( name ) );
  }

  void CPPC_DO_CHECKPOINT_FUNCTION( int * num ) {
    Controller::instance().CPPC_Do_checkpoint( static_cast<unsigned char>( *num ) );
  }

  void CPPC_INIT_CONFIGURATION_FUNCTION( int * ierror ) {
    int argsno = 0;
    char ** args = 0;

    *ierror = Controller::instance().CPPC_Init_configuration( &argsno, &args );
  }

  void CPPC_INIT_STATE_FUNCTION() {
    Controller::instance().CPPC_Init_state();
  }

  void CPPC_SHUTDOWN_FUNCTION() {
    Controller::instance().CPPC_Shutdown();
  }

  void CPPC_JUMP_NEXT_FUNCTION( int * value ) {
    *value = static_cast<int>( Controller::instance().CPPC_Jump_next() );
  }

  void CPPC_REGISTER_DESCRIPTOR_FUNCTION( int * code, int * fd,
    int * fortran_unit, char * path, int * offset ) {

    if( *fortran_unit != 0 ) {
      cppc::interface::fortran::unitCorrespondence.insert(
        cppc::interface::fortran::UnitMap::value_type( *fortran_unit, *code ) );
    }

    CppcFile * file = Controller::instance().CPPC_Register_descriptor( *code,
      fd, CPPC_UNIX_FD, string( path ) );

    if( file == 0 ) {
      *offset = -1;
    } else {
      *offset = static_cast<int>( file->get_file_offset() );
      string file_path = file->get_file_path();
      std::memcpy( path, file_path.c_str(), file_path.length()+1 );
    }
  }

  void CPPC_UPDATE_DESCRIPTOR_FUNCTION( int * code, int * fd,
    int * fortran_unit, char * path, int * offset ) {

    cppc::interface::fortran::UnitMap::iterator i =
      cppc::interface::fortran::unitCorrespondence.find( *fortran_unit );
    if( i != cppc::interface::fortran::unitCorrespondence.end() ) {
      cppc::interface::fortran::unitCorrespondence.erase( i );
    }
      cppc::interface::fortran::unitCorrespondence.insert(
        cppc::interface::fortran::UnitMap::value_type( *fortran_unit, *code ) );

      Controller::instance().CPPC_Update_descriptor( *code, fd, CPPC_UNIX_FD,
        string( path ) );
  }

  void CPPC_OFFSET_SET_FUNCTION( int * code, int * offset ) {
      Controller::instance().CPPC_Offset_set( *code, *offset );
  }

  void CPPC_UNREGISTER_DESCRIPTOR_FUNCTION( int * fortran_unit, int * fd ) {
      cppc::interface::fortran::unitCorrespondence.erase( *fortran_unit );
      Controller::instance().CPPC_Unregister_descriptor( fd );
  }

  void CPPC_NEXT_UNIT_FUNCTION( int * current_unit, int * current_code ) {

    if( *current_unit == -1 ) {
      if( cppc::interface::fortran::unitCorrespondence.begin() ==
        cppc::interface::fortran::unitCorrespondence.end() ) {

        *current_unit = -1;
        *current_code = -1;
      } else {
        *current_unit =
          cppc::interface::fortran::unitCorrespondence.begin()->first;
        *current_code =
          cppc::interface::fortran::unitCorrespondence.begin()->second;
      }
      return;
    }

    cppc::interface::fortran::UnitMap::iterator i =
      cppc::interface::fortran::unitCorrespondence.find( *current_unit );

    if( ( i == cppc::interface::fortran::unitCorrespondence.end() ) ||
      ( ++i == cppc::interface::fortran::unitCorrespondence.end() ) ) {

      *current_unit = -1;
      *current_code = -1;
      return;
    } else {
      *current_unit = i->first;
      *current_code = i->second;
    }
  }

  void CPPC_CONTEXT_PUSH_FUNCTION( char * name,
    unsigned int * calledFromLine ) {

    Controller::instance().CPPC_Context_push( string( name ), *calledFromLine );
  }

  void CPPC_CONTEXT_POP_FUNCTION() {
    Controller::instance().CPPC_Context_pop();
  }

  void CPPC_ADD_LOOP_INDEX_FUNCTION( char * name, CPPC_Datatype * type ) {
    Controller::instance().CPPC_Add_loop_index( string( name ), *type );
  }

  void CPPC_SET_LOOP_INDEX_FUNCTION( void * data ) {
    Controller::instance().CPPC_Set_loop_index( data );
  }

  void CPPC_REMOVE_LOOP_INDEX_FUNCTION() {
    Controller::instance().CPPC_Remove_loop_index();
  }

  void CPPC_CREATE_CALL_IMAGE_FUNCTION( char * name,
    unsigned int * calledFromLine ) {

    Controller::instance().CPPC_Create_call_image( string( name ),
      *calledFromLine );
  }

  void CPPC_REGISTER_FOR_CALL_IMAGE_FUNCTION( void * data, int * nelems,
    CPPC_Datatype * type, char * name ) {

    Controller::instance().CPPC_Register_for_call_image( data, *nelems, *type,
      string( name ), true );
  }

  void CPPC_COMMIT_CALL_IMAGE_FUNCTION() {
    Controller::instance().CPPC_Commit_call_image();
  }
}
