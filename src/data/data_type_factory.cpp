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
#include <data/cppc_binary.h>

#include <cassert>

namespace cppc {
  namespace data {

    DataTypeFactory::DataTypeFactory()
      : dataTypes( CallbackMap() ), dataSizes( SizeMap() ) {}

    DataTypeFactory::~DataTypeFactory() {}

    DataTypeFactory::DataTypeFactory( const DataTypeFactory & dtf )
      : dataTypes( dtf.dataTypes ), dataSizes( dtf.dataSizes ) {}

    DataTypeFactory & DataTypeFactory::operator=( const DataTypeFactory & rhs ) {
      if( this == &rhs ) {
        return *this;
      }

      dataTypes = rhs.dataTypes;
      dataSizes = rhs.dataSizes;

      return *this;
    }

    DataType * DataTypeFactory::getDataType( DataType::DataTypeIdentifier code ) {
      CallbackMap::const_iterator i = dataTypes.find( code );
      assert( i != dataTypes.end() );
      return (i->second)();
    }

    DataType * DataTypeFactory::getDataType( DataType::DataTypeIdentifier code, void * v, int size ) {
      if( size == 1 ) {
        DataType * object = getDataType( code );
        object->setValue( v );
        return object;
      } else {
        DataType * array = new Array( v, size, code );
        return array;
      }
    }

    DataType::DataTypeSize DataTypeFactory::getDataTypeSize( DataType::DataTypeIdentifier code ) {
      SizeMap::const_iterator i = dataSizes.find( code );
      assert( i != dataSizes.end() );
      return i->second;
    }

    DataType * DataTypeFactory::createBinary( const void * const v, int size ) {
      return new Binary( v, size );
    }

    bool DataTypeFactory::registerDataType( DataType::DataTypeIdentifier id,
      DataType::DataTypeSize size, CreateDataTypeCallback callback ) {

      dataSizes.insert( SizeMap::value_type( id, size ) ).second;
      return dataTypes.insert( CallbackMap::value_type( id, callback ) ).second;
    }

  }
}
