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




#include <compressor/compressor.h>

#include <unistd.h>

namespace cppc {
  namespace compressor {

    Compressor::Compressor( Buffer * b ) 
      : in( b ), out( NULL ) {}

    Compressor::Compressor(const Compressor &e_obj)
      : in( NULL ), out( NULL ) {}

    Compressor& Compressor::operator=(const Compressor &rhs) {

      if(this == &rhs) {
	return *this;
      }

      if( out != NULL ) {
      	delete out;
      }

      in = rhs.in;
      out = rhs.out;

      return( *this );
    }

    Compressor::~Compressor() {

      if( out != NULL ) {
      	delete out;
      }
    }

  }
}
