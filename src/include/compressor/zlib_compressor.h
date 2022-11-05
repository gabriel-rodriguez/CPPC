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




#if ! defined CPPC_COMPRESSOR_ZLIBCOMPRESSOR_H
#define CPPC_COMPRESSOR_ZLIBCOMPRESSOR_H

#include <compressor/compressor.h>
#include <util/configuration/configuration_manager.h>

using cppc::util::configuration::ParameterKeyType;

namespace cppc {
  namespace compressor {

    class ZLibCompressor:public Compressor {

    public:
      ZLibCompressor( Buffer * );
      virtual ~ZLibCompressor();

      virtual void comprime();
      virtual void descomprime();

    private:
      ZLibCompressor(const ZLibCompressor&);
      virtual ZLibCompressor& operator=(const ZLibCompressor&);

      static const ParameterKeyType ZLIB_COMPRESSOR_COMPRESSION_LEVEL_PARAMETER_KEY;
      static unsigned char getCompressionLevelParameter();
    };

  };
};

#endif
