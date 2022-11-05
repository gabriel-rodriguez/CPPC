#include <compressor/zlib_compressor.h>
#include <compressor/compressor_factory.h>

#include <cassert>
#include <cstdlib>

#include <zlib.h>

namespace {

  using cppc::compressor::Compressor;
  using cppc::compressor::ZLibCompressor;
  using cppc::compressor::CompressorFactory;
  using cppc::compressor::CompressorType;
  using cppc::util::configuration::ConfigurationManager;
  using cppc::util::configuration::ParameterValueType;
  using cppc::writer::Buffer;

  Compressor *createCompressor( Buffer * b ) {
    return new ZLibCompressor( b );
  }

  const CompressorType compressorType( 1 );
  const bool registered = CompressorFactory::instance().registerCompressor(
    compressorType, createCompressor );

};

namespace cppc {
  namespace compressor {

    ZLibCompressor::ZLibCompressor( Buffer * b )
      : Compressor( b ) {}

    ZLibCompressor::ZLibCompressor(const ZLibCompressor & c_bin)
      : Compressor( c_bin ) {}

    ZLibCompressor& ZLibCompressor::operator=(const ZLibCompressor &rhs) {
      return( *this );
    }

    ZLibCompressor::~ZLibCompressor() {}

    void ZLibCompressor::comprime() {

      z_streamp stream = new z_stream;
      unsigned char *tmp = new unsigned char[in->getTam()];

      stream->next_in = (Bytef *)in->datos();
      stream->avail_in = in->getTam();

      stream->next_out = tmp;
      stream->avail_out = in->getTam();

      stream->zalloc = Z_NULL;
      stream->zfree = Z_NULL;
      stream->opaque = Z_NULL;

      deflateInit( stream, getCompressionLevelParameter() );

      out = new Buffer();
      while( (deflate(stream,Z_SYNC_FLUSH) == Z_OK) &&
        (stream->avail_out == 0) ) {

        out->escribe(tmp,stream->total_out);
        stream->next_out = tmp;
        stream->avail_out = in->getTam();
        stream->total_out = 0;
      }

      out->escribe(tmp,stream->total_out);

      deflateEnd( stream );

      in->reset();
      in->escribe( out->datos(), out->getTam() );

      delete tmp;
      delete out;
      out = NULL;
      delete stream;

      in->rewind();
    }

    void ZLibCompressor::descomprime() {

      z_streamp stream = new z_stream;
      unsigned char *tmp = new unsigned char[in->getTam()];

      stream->next_in = (Bytef *)in->datos();
      stream->avail_in = in->getTam();

      stream->next_out = tmp;
      stream->avail_out = stream->avail_in;

      stream->zalloc = Z_NULL;
      stream->zfree = Z_NULL;
      stream->opaque = Z_NULL;

      inflateInit( stream );

      out = new Buffer();

      while( (inflate(stream,Z_SYNC_FLUSH)==Z_OK) &&
        (stream->avail_out == 0) ) {

        out->escribe(tmp,stream->total_out);
        stream->next_out = tmp;
        stream->avail_out = in->getTam();
        stream->total_out = 0;
      }

      out->escribe(tmp,stream->total_out);

      inflateEnd( stream );

      in->reset();
      in->escribe( out->datos(), out->getTam() );

      delete [] tmp;
      delete out;
      out = NULL;
      delete stream;

      in->rewind();
    }

    const ParameterKeyType
      ZLibCompressor::ZLIB_COMPRESSOR_COMPRESSION_LEVEL_PARAMETER_KEY(
        "CPPC/Compressor/ZLibCompressor/CompressionLevel" );
    unsigned char ZLibCompressor::getCompressionLevelParameter() {
      const ParameterValueType * const value =
        ConfigurationManager::instance().getParameter(
          ZLIB_COMPRESSOR_COMPRESSION_LEVEL_PARAMETER_KEY );
      const unsigned char compressionLevel = std::atoi( value->c_str() );

      return compressionLevel;
    }

  };
};
