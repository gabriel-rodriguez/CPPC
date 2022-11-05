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



#if ! defined CPPC_UTIL_COMMUNICATION_MPICOMMUNICATOR_H
#define CPPC_UTIL_COMMUNICATION_MPICOMMUNICATOR_H

#include <data/data_type.h>

#define OMPI_SKIP_MPICXX // Tell OpenMPI not to build MPI CXX bindings
#include <mpi.h>

using cppc::data::DataType;

namespace cppc {
  namespace util {
    namespace communication {

      class MpiCommunicator {

      public:

        static void barrier();
        static int getRank();
        static int getWorldSize();
        static DataType * reductionOnMax( DataType * data );
        static DataType * reductionOnMin( DataType * data );

      private:
        MpiCommunicator();
        MpiCommunicator( const MpiCommunicator & );
        MpiCommunicator & operator=( const MpiCommunicator & );
        ~MpiCommunicator();

        static MPI_Datatype convertType( DataType::DataTypeIdentifier );
        static DataType * reduction( DataType *, MPI_Op );
      };

    }
  }
}

#endif
