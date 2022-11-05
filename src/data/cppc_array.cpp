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




#include <data/cppc_array.h>
#include <data/data_type_factory.h>
#include <writer/writer.h>

#include <cassert>
#include <cstring>

namespace cppc {
  namespace data {

    Array::Array( DataTypeIdentifier t, int size )
      : typeIdentifier( t ), typeSize( getTypeSize() ), elements( 0 ),
        value( new unsigned char[ size * typeSize ] ), rawSize( size ),
        ownsMem( true ) {}

    Array::Array( const void * data, int size, DataTypeIdentifier t )
      : typeIdentifier( t ), typeSize( getTypeSize() ),
        elements( size/typeSize ), value( (unsigned char *)data ),
        rawSize( size ), ownsMem( false ) {}

    Array::Array( const Array & a )
      : typeIdentifier( a.typeIdentifier ), typeSize( a.typeSize ),
        elements( a.elements ), value( a.value ), rawSize( a.rawSize ),
        ownsMem( false ) {}

    Array& Array::operator=( const Array & rhs ) {
      if( this == &rhs ) {
        return *this;
      }

      typeIdentifier = rhs.typeIdentifier;
      typeSize = rhs.typeSize;
      elements = rhs.elements;
      value = rhs.value;
      rawSize = rhs.rawSize;
      ownsMem = false;

      return *this;
    }

    Array::~Array() {
      if( ownsMem ) {
        delete [] value;
      }
    }

    void Array::setValue( void * v, int size ) {
      if( ownsMem ) {
        delete [] value;
      }

      assert( size % typeSize == 0 );
      elements = size/typeSize;
      rawSize = size;
      value = (unsigned char *)v;
      ownsMem = false;
    }

    void Array::add( DataType * object ) {
      if( elements * typeSize + typeSize >= rawSize ) {
        unsigned char * aux = new unsigned char[ rawSize * 2 ];
        std::memcpy( value, aux, rawSize );
        rawSize *= 2;

        delete [] value;
        value = aux;
      }

      std::memcpy( value+typeSize*elements, object->getValue(), typeSize );
    }

    vector<DataType *> Array::getDataTypes() {
      vector<DataType *> ret = vector<DataType *>();

      for( unsigned int i = 0; i < elements; i++ ) {
        ret.push_back( DataTypeFactory::instance().getDataType( typeIdentifier,
          value+i*typeSize ) );
      }

      return ret;

    }

    DataType::DataTypeSize Array::getTypeSize() {
      return DataTypeFactory::instance().getDataTypeSize( typeIdentifier );
    }

  }
}
