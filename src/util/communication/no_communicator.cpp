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



#include <data/data_type_factory.h>
#include <util/communication/no_communicator.h>

#include <cassert>

using cppc::data::DataTypeFactory;
using cppc::data::DataType;

namespace cppc {
  namespace util {
    namespace communication {

      NoCommunicator::NoCommunicator() {}

      NoCommunicator::NoCommunicator( const NoCommunicator & ) {}

      NoCommunicator & NoCommunicator::operator=( const NoCommunicator & ) {
        return *this;
      }

      NoCommunicator::~NoCommunicator() {}

      void NoCommunicator::barrier() { return; }

      int NoCommunicator::getRank() { return 0; }

      int NoCommunicator::getWorldSize() { return 1; }

      DataType * NoCommunicator::reductionOnMax( DataType * data ) {
        return NoCommunicator::reduction( data );
      }

      DataType * NoCommunicator::reductionOnMin( DataType * data ) {
        return NoCommunicator::reduction( data );
      }

      DataType * NoCommunicator::reduction( DataType * data ) {

        DataType::DataTypeIdentifier cppcType( data->getIdentifier() );
        DataType::DataTypeSize typeSize(
          DataTypeFactory::instance().getDataTypeSize( cppcType ) );
        DataType::DataTypeSize dataSize( data->getSize() );

        assert( dataSize % typeSize == 0 );
        unsigned int elements = dataSize / typeSize;

        return DataTypeFactory::instance().getDataType( data->getIdentifier(),
          data->getValue(), elements );
      }

    };
  };
};
