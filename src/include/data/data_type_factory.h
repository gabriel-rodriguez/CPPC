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



#if ! defined CPPC_DATA_DATATYPEFACTORY_H
#define CPPC_DATA_DATATYPEFACTORY_H

#include <map>

#include <data/data_type.h>
#include <data/cppc_array.h>
#include <data/cppc_basic.h>
#include <util/singleton/singleton.h>

using std::map;

using cppc::util::singleton::Singleton;

namespace cppc {
  namespace data {

    class DataTypeFactory:public Singleton<DataTypeFactory> {

    public:
      DataType * getDataType( DataType::DataTypeIdentifier );
      DataType * getDataType( DataType::DataTypeIdentifier, void *,
        int size = 1 );
      DataType::DataTypeSize getDataTypeSize( DataType::DataTypeIdentifier );

      template <typename T, DataType::DataTypeIdentifier Id>
        static DataType * createBasic( const T * value, int size=1 ) {

        if( size == 1 ) {
          return new Basic<T,Id>( *value );
        } else {
          return new Array( value, size, Id );
        }
      }

      DataType * createBinary( const void * const, int );
      bool registerDataType( DataType::DataTypeIdentifier,
        DataType::DataTypeSize, CreateDataTypeCallback );

    private:
      DataTypeFactory();
      ~DataTypeFactory();
      DataTypeFactory(const DataTypeFactory&);
      DataTypeFactory & operator=( const DataTypeFactory & );

      typedef map<DataType::DataTypeIdentifier,CreateDataTypeCallback>
        CallbackMap;
      typedef map<DataType::DataTypeIdentifier,DataType::DataTypeSize> SizeMap;
      CallbackMap dataTypes;
      SizeMap dataSizes;

      friend class Singleton<DataTypeFactory>;

    };

  }
}

#endif
