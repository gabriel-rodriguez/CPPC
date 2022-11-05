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



#if ! defined CPPC_CONTROLLER_ACTIONS_CONTROLLER_HELPER_H
#define CPPC_CONTROLLER_ACTIONS_CONTROLLER_HELPER_H

#include <checkpointer/checkpoint.h>
#include <compressor/compressor.h>
#include <controller/controller.h>
#include <data/context.h>
#include <util/configuration/configuration_manager.h>
#include <writer/writer.h>

#include <string>

using std::string;

using cppc::checkpointer::CheckpointCode;
using cppc::compressor::CompressorType;
using cppc::controller::Controller;
using cppc::data::Context;
using cppc::util::configuration::ParameterKeyType;
using cppc::writer::WriterType;

namespace cppc {
  namespace controller {
    namespace actions {

      class ControllerHelper {

      public:
        static void createGenericDirectory( const string );
        static string getApplicationNameParameter();
        static string getChkptPath( string, CheckpointCode );
        static bool getCheckpointOnFirstTouchParameter();
        static unsigned int getFrequencyParameter();
        static unsigned int getFullFrequencyParameter();
        static bool getRestartParameter();

        static inline void setRestartParameter( bool restart ) {
          CPPC_RESTART_PARAMETER = restart;
        }

        static unsigned int getRecoverySetsParameter();
        static string getRootDirectoryParameter();
        static string getSuffixParameter();
        static WriterType getWriterTypeParameter();

        static Context * getHierarchyRoot( Context * );
        static void addMemblocksForRegister( Register *, BlockMap * );
        static void removeMemblocksForRegister( Register *, BlockMap * );

      private:
        ControllerHelper();
        ControllerHelper( const ControllerHelper & );
        ControllerHelper & operator=( const ControllerHelper & );
        ~ControllerHelper();

        const static ParameterKeyType CPPC_APPLICATION_NAME_PARAMETER_KEY;
        const static ParameterKeyType CPPC_CHECKPOINT_ON_FIRST_TOUCH_PARAMETER_KEY;
        const static ParameterKeyType CPPC_FREQUENCY_PARAMETER_KEY;
        const static ParameterKeyType CPPC_FULL_FREQUENCY_PARAMETER_KEY;
        const static ParameterKeyType CPPC_MULTITHREADED_PARAMETER_KEY;
        const static ParameterKeyType CPPC_RECOVERY_SETS_PARAMETER_KEY;
        const static ParameterKeyType CPPC_RESTART_PARAMETER_KEY;
        static bool CPPC_RESTART_PARAMETER;
        const static ParameterKeyType CPPC_ROOT_DIRECTORY_PARAMETER_KEY;
        const static ParameterKeyType CPPC_SUFFIX_PARAMETER_KEY;
        const static ParameterKeyType CPPC_WRITER_TYPE_PARAMETER_KEY;
      };

    }
  }
}

#endif
