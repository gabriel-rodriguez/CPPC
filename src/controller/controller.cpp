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



#include <controller/controller.h>
#include <controller/actions/add_loop_index_action.h>
#include <controller/actions/checkpoint_action.h>
#include <controller/actions/commit_call_image_action.h>
#include <controller/actions/context_pop_action.h>
#include <controller/actions/context_push_action.h>
#include <controller/actions/controller_helper.h>
#include <controller/actions/create_call_image_action.h>
#include <controller/actions/init_configuration_action.h>
#include <controller/actions/init_state_action.h>
#include <controller/actions/offset_set_action.h>
#include <controller/actions/register_action.h>
#include <controller/actions/register_descriptor_action.h>
#include <controller/actions/register_for_call_image_action.h>
#include <controller/actions/remove_loop_index_action.h>
#include <controller/actions/set_loop_index_action.h>
#include <controller/actions/shutdown_action.h>
#include <controller/actions/unregister_action.h>
#include <controller/actions/unregister_descriptor_action.h>
#include <controller/actions/update_descriptor_action.h>
#include <data/heap_context.h>

using cppc::data::HeapContext;

namespace cppc {
  namespace controller {

    /* Plain constructor */
    Controller::Controller()
    : Singleton<Controller>(), state() {
      state.initConfig = false;
      state.initState = false;
      state.checkpointer = NULL;
      state.loopVarName = 0;
      state.loopVarType = 0;
      state.lastCheckpointTouched = 0;
      state.epoch = 0;
      state.touchedCheckpoints = 0;
      state.checkpointSkeleton = 0;
    }

    /* Copy constructor (shouldn't ever be used) */
    Controller::Controller(const Controller & orig)
      : Singleton<Controller>( orig ), state( orig.state ) {}

    /* Destructor */
    Controller::~Controller() {
      // We do nothing when deleting the controller. If we called
      // ShutdownAction then horrible things could happen. Example: the
      // user states that he wants to delete checkpoints (parameter
      // CPPC/Controller/DeleteCheckpoints). When the program catched an
      // exception like SIGINT, the controller would be deleted and so
      // will be the files.
    }

    int Controller::CPPC_Init_configuration( int * argno, char *** args ) {
      actions::InitConfigurationAction action( argno, args, state );
      return action.execute();
    }

    int Controller::CPPC_Init_state() {
      actions::InitStateAction action( state );
      return action.execute();
    }

    int Controller::CPPC_Shutdown() {
      actions::ShutdownAction action( state );
      return action.execute();
    }

    void * Controller::CPPC_Register( void * dato, int nelems,
      DataType::DataTypeIdentifier type, string name, bool allocatedMemory )
        const {

      actions::RegisterAction action( dato, nelems, type, name,
        allocatedMemory, (ControllerState &)state );

      return action.execute();
    }

    CppcFile * Controller::CPPC_Register_descriptor( CppcFile::FileCode code,
      void * descriptor, CppcFile::DescriptorType type, string path ) {

      actions::RegisterDescriptorAction action( code, descriptor, type, path,
        (ControllerState &)state );

      return action.execute();
    }

    void Controller::CPPC_Unregister( const string & varName ) const {
      actions::UnregisterAction action( varName, (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Unregister_descriptor( void * descriptor ) {
      actions::UnregisterDescriptorAction action( descriptor,
        (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Update_descriptor( CppcFile::FileCode code,
      void * descriptor, CppcFile::DescriptorType dtype, string path ) {

      actions::UpdateDescriptorAction action( code, descriptor, dtype, path,
        (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Offset_set( CppcFile::FileCode code,
      CppcFile::FileOffset offset ) {

      actions::OffsetSetAction action( code, offset, (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Context_push( const string & functionName,
      unsigned int line ) {

      actions::ContextPushAction action( functionName, line,
        (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Context_pop() {
      actions::ContextPopAction action( (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Add_loop_index( const string & indexName,
      DataType::DataTypeIdentifier type ) {

      actions::AddLoopIndexAction action( indexName, type,
        (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Set_loop_index( void * data ) {
      actions::SetLoopIndexAction action( data, (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Remove_loop_index() {
      actions::RemoveLoopIndexAction action( (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Create_call_image( string functionName,
      unsigned int line ) {

      actions::CreateCallImageAction action( functionName, line,
        (ControllerState &)state );
      action.execute();
    }

    void * Controller::CPPC_Register_for_call_image( void * data, int nelems,
      DataType::DataTypeIdentifier type, string name, bool allocatedMemory ) {

      actions::RegisterForCallImageAction action( data, nelems, type, name,
        allocatedMemory, (ControllerState &)state );
      return action.execute();
    }

    void Controller::CPPC_Commit_call_image() {
      actions::CommitCallImageAction action( (ControllerState &)state );
      action.execute();
    }

    void Controller::CPPC_Do_checkpoint( CheckpointCode num ) const {
      actions::CheckpointAction action( num, (ControllerState &)state );
      action.execute();
    }


    bool Controller::CPPC_Jump_next() const {
      return actions::ControllerHelper::getRestartParameter();
    }

  }
}
