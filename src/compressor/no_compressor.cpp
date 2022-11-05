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
#include <compressor/no_compressor.h>

namespace {

  using cppc::compressor::Compressor;
  using cppc::compressor::NoCompressor;
  using cppc::compressor::CompressorFactory;
  using cppc::compressor::CompressorType;
  using cppc::writer::Buffer;

  Compressor *createCompressor( Buffer * b ) {
    return new NoCompressor( b );
  }

  const CompressorType compressorType( 0 );
  const bool registered = CompressorFactory::instance().registerCompressor( compressorType, createCompressor );

}

namespace cppc {
  namespace compressor {

    NoCompressor::NoCompressor( Buffer * b ) : Compressor( b ) {}

    NoCompressor::NoCompressor( const NoCompressor & c_bin )
      : Compressor( c_bin ) {}

    NoCompressor & NoCompressor::operator=( const NoCompressor & rhs ) {

      if( this == &rhs ) {
	return *this;
      }

      return *this;
    }

    NoCompressor::~NoCompressor() {}

  }
}
