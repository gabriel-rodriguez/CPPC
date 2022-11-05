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



#if ! defined CPPC_CONTROLLER_ACTIONS_CHECKPOINT_ACTION_H
#define CPPC_CONTROLLER_ACTIONS_CHECKPOINT_ACTION_H

#include <checkpointer/checkpoint.h>
#include <controller/controller.h>
#include <data/context.h>
#include <data/memory_block.h>
#include <data/register.h>

#include <pthread.h>

using cppc::data::MemoryBlock;

using cppc::controller::Controller;
using cppc::checkpointer::CheckpointCode;
using cppc::data::Context;
using cppc::data::Register;

namespace cppc {
  namespace controller {
    namespace actions {

      class CheckpointAction {

      public:
        CheckpointAction( CheckpointCode, Controller::ControllerState & );
        ~CheckpointAction();

        void execute();

      private:
        CheckpointAction( const CheckpointAction & );
        CheckpointAction & operator=( const CheckpointAction & );

        void Do_checkpoint( bool = true );

        CheckpointCode c_code;
        Controller::ControllerState & controllerState;
      };

    }
  }
}

#endif
