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




#if ! defined CPPC_DATA_ARRAY_H
#define CPPC_DATA_ARRAY_H

#include <data/data_type.h>

#include <vector>

using std::vector;

namespace cppc {
  namespace data {

    class Array:public DataType {

    public:
      Array( DataTypeIdentifier, int );
      Array( const void *, int, DataTypeIdentifier );
      Array( const Array & );
      virtual Array & operator=( const Array & );
      virtual ~Array();

      virtual inline DataTypeIdentifier getIdentifier() { return typeIdentifier; }
      virtual inline void * getValue() { return value; }
      virtual inline void setValue( void * v ) { setValue( v, 1 ); }
      void setValue( void *, int );
      virtual inline DataTypeSize getSize() { return typeSize * elements; }
      virtual void add( DataType * );      
      vector<DataType *> getDataTypes();

    private:
      typedef unsigned char * ValueType;

      DataTypeSize getTypeSize();

      DataTypeIdentifier typeIdentifier;
      DataTypeSize typeSize;
      unsigned int elements;
      ValueType value;
      unsigned int rawSize;
      bool ownsMem;

    };

  }
}

#endif
