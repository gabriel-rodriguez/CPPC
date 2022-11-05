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




#if ! defined CPPC_COMPRESSOR_COMPRESSOR_H
#define CPPC_COMPRESSOR_COMPRESSOR_H

#include <data/cppc_basic.h>
#include <writer/buffer.h>

using cppc::data::UCharacterType;
using cppc::writer::Buffer;

namespace cppc {
  namespace compressor {

    typedef unsigned char BasicCompressorType;
    typedef UCharacterType CompressorType;

    class Compressor {

    public:
      Compressor( Buffer * );
      virtual ~Compressor();

      virtual void comprime()=0;
      virtual void descomprime()=0;

    protected:
      Compressor(const Compressor&);
      virtual Compressor& operator=(const Compressor&);

      Buffer *in,*out;    
    };

  }
}

#endif
