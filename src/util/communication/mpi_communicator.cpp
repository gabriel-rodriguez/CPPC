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



#include <util/communication/mpi_communicator.h>

#include <data/data_type_factory.h>

#include <cassert>

using cppc::data::DataType;
using cppc::data::DataTypeFactory;

namespace cppc {
  namespace util {
    namespace communication {

      MpiCommunicator::MpiCommunicator() {}

      MpiCommunicator::MpiCommunicator( const MpiCommunicator & ) {}

      MpiCommunicator & MpiCommunicator::operator=( const MpiCommunicator & ) { return *this; }

      MpiCommunicator::~MpiCommunicator() {}

      void MpiCommunicator::barrier() {
        MPI_Barrier( MPI_COMM_WORLD );
      }

      int MpiCommunicator::getRank() {

        int rank;
        MPI_Comm_rank( MPI_COMM_WORLD, &rank );
        return rank;
      }

      int MpiCommunicator::getWorldSize() {

        int worldSize;
        MPI_Comm_size( MPI_COMM_WORLD, &worldSize );
        return worldSize;
      }

      DataType * MpiCommunicator::reductionOnMax( DataType * data ) {
        return MpiCommunicator::reduction( data, MPI_MAX );
      }

      DataType * MpiCommunicator::reductionOnMin( DataType * data ) {
        return MpiCommunicator::reduction( data, MPI_MIN );
      }

      DataType * MpiCommunicator::reduction( DataType * data, MPI_Op mpiOp ) {

        DataType::DataTypeIdentifier cppcType( data->getIdentifier() );
        DataType::DataTypeSize typeSize(
          DataTypeFactory::instance().getDataTypeSize( cppcType ) );
        MPI_Datatype mpiType( MpiCommunicator::convertType( cppcType ) );
        DataType::DataTypeSize dataSize( data->getSize() );

        assert( ( dataSize % typeSize ) == 0 );
        unsigned int elements = dataSize / typeSize;

        unsigned char * result = new unsigned char[ dataSize ];
        MPI_Allreduce( data->getValue(), result, elements, mpiType, mpiOp,
          MPI_COMM_WORLD );

        DataType * ret = DataTypeFactory::instance().getDataType( cppcType, result, elements );
        delete [] result;
        return ret;
      }

      MPI_Datatype MpiCommunicator::convertType(
        DataType::DataTypeIdentifier cppcType ) {

        switch( cppcType ) {
          case CPPC_CHAR: return MPI_CHAR;
          case CPPC_UCHAR: return MPI_UNSIGNED_CHAR;
          case CPPC_SHORT: return MPI_SHORT;
          case CPPC_USHORT: return MPI_UNSIGNED_SHORT;
          case CPPC_INT: return MPI_INT;
          case CPPC_UINT: return MPI_UNSIGNED;
          case CPPC_LONG: return MPI_LONG;
          case CPPC_ULONG: return MPI_UNSIGNED_LONG;
          case CPPC_FLOAT: return MPI_FLOAT;
          case CPPC_DOUBLE: return MPI_DOUBLE;
          default: return MPI_BYTE;
        }
      }

    }
  }
}
