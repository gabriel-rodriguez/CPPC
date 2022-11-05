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



#if ! defined CPPC_DATA_REGISTER_H
#define CPPC_DATA_REGISTER_H

#include <data/cppc_types.h>
#include <data/memory_block.h>

#include <string>
#include <vector>

using std::string;
using std::vector;

namespace cppc {
  namespace data {

    class Register {

    public:
      Register( void *, void *, DataType::DataTypeIdentifier, string );
      Register( const Register & );
      Register & operator=( const Register & );
      ~Register();


      inline void * initAddress() { return( _initDir ); }
      inline void setInitAddress( void * initAddress ) { _initDir = initAddress; }
      inline void * endAddress() { return( _endDir ); }
      inline void setEndAddress( void * endDir ) { _endDir = endDir; }
      inline DataType::DataTypeIdentifier type() { return( _type ); }
      inline string name() { return( _name ); }
      inline void setName( string s ) { _name = s; }
      inline void setCode( MemoryBlock::BlockCode code ) { _blockCode = code; }
      inline MemoryBlock::BlockCode getCode() { return _blockCode; }

      bool operator==(const Register & );

    private:
      Register();

      void * _initDir;
      void * _endDir;
      DataType::DataTypeIdentifier _type;
      string _name;
      MemoryBlock::BlockCode _blockCode;
    };

    typedef vector<Register *> RegisterVector;
  }
}

#endif
