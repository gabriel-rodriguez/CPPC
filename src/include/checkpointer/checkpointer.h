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



#if ! defined CPPC_CHECKPOINTER_CHECKPOINTER_H
#define CPPC_CHECKPOINTER_CHECKPOINTER_H

#include <checkpointer/checkpoint.h>
#include <data/register.h>
#include <util/configuration/configuration_manager.h>

//FIXME: In C++11 this should be implemented using std::thread.
#include <pthread.h>

using cppc::data::Register;
using cppc::util::configuration::ParameterKeyType;

namespace cppc {
  namespace checkpointer {

    class Checkpointer {

    public:
      Checkpointer();
      Checkpointer( Checkpoint * );
      ~Checkpointer();

      //Check data correctness
      static bool integrityTest( string );
      static bool isFullCheckpoint( string );

      //Read file contents into memory for incremental restart
      CheckpointCode readCheckpoint( string );

      void checkpoint();

      // Recovers a single variable
      void * partialRestart( const string &, unsigned int );

      inline Checkpoint * getCheckpoint() { return chkpt; }
      bool moreRestart( CheckpointCode& );

      // Wait for last thread before allowing end
      static void Shutdown();

    private:
      Checkpointer(const Checkpointer&);
      Checkpointer& operator=(const Checkpointer&);

      // Members
      Checkpoint * chkpt; // Checkpoint to write
      static pthread_t * last_thread; // Last executed thread (only in multithreaded execution)

      // Friend multithread function
      friend void * thread_run( void * );
    };
  }
}

#endif
