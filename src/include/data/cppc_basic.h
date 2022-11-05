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



#if ! defined CPPC_DATA_BASIC_H
#define CPPC_DATA_BASIC_H

#include <data/data_type.h>

namespace cppc {
  namespace data {

    template <typename T,DataType::DataTypeIdentifier Id> class Basic
      :public DataType {

    public:
      Basic() : value( 0 ) {}
      Basic( T v ) : value( v ) {}
      virtual ~Basic() {}

      virtual inline DataType::DataTypeIdentifier getIdentifier() {
        return typeIdentifier;
      }
      virtual inline void * getValue() { return (void *)(&value); }
      virtual inline T getStaticValue() const { return value; }
      virtual inline void setValue( void * v ) { value = *((T *)v); }
      static inline DataTypeSize staticSize() { return sizeof(T); }
      virtual inline DataTypeSize getSize() { return staticSize(); }

      virtual Basic<T,Id> operator=( const Basic<T,Id> & rhs ) {
        if( this == &rhs ) {
          return *this;
        }

        value = rhs.value;

        return *this;
      }

      bool operator==( const Basic<T,Id> & rhs ) const {
        return value == rhs.value;
      }
      bool operator>( const Basic<T,Id> & rhs ) const {
        return value > rhs.value;
      }
      bool operator<( const Basic<T,Id> & rhs ) const {
        return value < rhs.value;
      }
      Basic& operator++() {
        value++;
        return *this;
      }

      static const DataType::DataTypeIdentifier typeIdentifier = Id;

    private:
      T value;

    };

    // Data types definitions, and partial instatiation functions for
    // type-codes recovery
    template <typename T> static DataType::DataTypeIdentifier getBasicTypeCode();

    typedef char BasicCharacterType;
    typedef Basic<BasicCharacterType, CPPC_CHAR> CharacterType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicCharacterType>() {

      return CPPC_CHAR;
    }

    typedef unsigned char BasicUCharacterType;
    typedef Basic<BasicUCharacterType, CPPC_UCHAR> UCharacterType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicUCharacterType>() {

      return CPPC_UCHAR;
    }

    typedef double BasicDoubleType;
    typedef Basic<BasicDoubleType, CPPC_DOUBLE> DoubleType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicDoubleType>() {

      return CPPC_DOUBLE;
    }

    typedef float BasicFloatType;
    typedef Basic<BasicFloatType, CPPC_FLOAT> FloatType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicFloatType>() {

      return CPPC_FLOAT;
    }

    typedef int BasicIntegerType;
    typedef Basic<BasicIntegerType, CPPC_INT> IntegerType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicIntegerType>() {

      return CPPC_INT;
    }

    typedef unsigned int BasicUIntegerType;
    typedef Basic<BasicUIntegerType, CPPC_UINT> UIntegerType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicUIntegerType>() {

      return CPPC_UINT;
    }

    typedef long BasicLongType;
    typedef Basic<BasicLongType, CPPC_LONG> LongType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicLongType>() {

      return CPPC_LONG;
    }

    typedef unsigned long BasicULongType;
    typedef Basic<BasicULongType, CPPC_ULONG> ULongType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicULongType>() {

      return CPPC_ULONG;
    }

    typedef short BasicShortType;
    typedef Basic<BasicShortType, CPPC_SHORT> ShortType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicShortType>() {

      return CPPC_SHORT;
    }

    typedef unsigned short BasicUShortType;
    typedef Basic<BasicUShortType, CPPC_USHORT> UShortType;
    template <> inline DataType::DataTypeIdentifier
      getBasicTypeCode<BasicUShortType>() {

      return CPPC_USHORT;
    }

  }
}

#endif
