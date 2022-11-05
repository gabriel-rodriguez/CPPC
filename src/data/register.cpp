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



#include <data/register.h>

namespace cppc {
  namespace data {

    Register::Register()
      : _initDir( 0 ), _endDir( 0 ), _type( 0 ), _name( "" ) {}

    Register::Register( void * initDir, void * endDir,
      DataType::DataTypeIdentifier type, string name )
      : _initDir( initDir ), _endDir( endDir ), _type( type ), _name( name ) {}

    Register::Register( const Register & copy )
      : _initDir( copy._initDir ), _endDir( copy._endDir ), _type( copy._type ),
        _name( copy._name ), _blockCode( copy._blockCode ) {}

    Register & Register::operator=( const Register & rhs ) {

      if( this == &rhs ) {
        return *this;
      }

      _initDir = rhs._initDir;
      _endDir = rhs._endDir;
      _type = rhs._type;
      _name = rhs._name;
      _blockCode = rhs._blockCode;

      return *this;
    }

    Register::~Register() {}

    bool Register::operator==( const Register & rhs ) {

      return ( ( _initDir == rhs._initDir ) &&
          ( _endDir == rhs._endDir ) &&
          ( _type == rhs._type ) );
    }

  }
}
