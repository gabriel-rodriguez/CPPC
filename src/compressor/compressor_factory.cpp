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




#include <compressor/compressor_factory.h>

#include <cassert>

#include <iostream>
using std::cout;

namespace cppc {
  namespace compressor {

    CompressorFactory::CompressorFactory() : compressors( CallbackMap() ) {}

    CompressorFactory::~CompressorFactory() {}

    CompressorFactory::CompressorFactory( const CompressorFactory & cf )
      : compressors( cf.compressors ) {}

    CompressorFactory & CompressorFactory::operator=( const CompressorFactory & rhs ) {

      if( this == &rhs ) {
	return *this;
      }

      compressors = rhs.compressors;

      return *this;
    }

    Compressor * CompressorFactory::getCompressor( CompressorType code, Buffer * b ) {

      CallbackMap::const_iterator i = compressors.find( code.getStaticValue() );
      assert( i != compressors.end() );

      return (i->second)( b );
    }

    bool CompressorFactory::registerCompressor( CompressorType type, CreateCompressorCallback callback ) {
      
      bool ret = compressors.insert( CallbackMap::value_type( type.getStaticValue(), callback ) ).second;

      if( !ret ) {
        cout << "CPPC WARNING: Registering compressor with code " << (unsigned int)type.getStaticValue() << " more than once\n";
      }

      return ret;

    }

  }
}
