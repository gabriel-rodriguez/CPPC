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



#include <writer/buffer.h>

#include <stdlib.h>
#include <string.h>

#include <iostream>

using std::cout;

using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterValueType;

namespace cppc {
  namespace writer {

    const ParameterKeyType Buffer::BLOCK_SIZE_PARAMETER_KEY = "CPPC/Writer/Buffer/BlockSize";
    MemoryBlock::MemoryType Buffer::BLOCK_SIZE_PARAMETER = 0;
    const MemoryBlock::MemoryType Buffer::DEFAULT_BLOCK_SIZE = 512 * 1024;

    Buffer::Buffer()
      : b_tam( getBlockTam() ), buffer( new unsigned char[ b_tam ] ),
	b_pos( 0 ), b_rtam( 0 ) {}

    Buffer::Buffer( const DataType::DataTypeSize e_tam )
      : b_tam( e_tam ), buffer( new unsigned char[ b_tam ] ),
	b_pos( 0 ), b_rtam( 0 ) {}

    Buffer::Buffer( const DataType::DataTypeSize e_tam, const void * const datos )
      : b_tam( e_tam ), buffer( new unsigned char[ b_tam ] ), b_pos( 0 ), b_rtam( e_tam ) {

      memcpy(buffer, datos, b_tam);
    }

    Buffer::Buffer( const Buffer & c_buffer )
      : b_tam( c_buffer.b_tam ), buffer( new unsigned char[ b_tam ] ),
	b_pos( c_buffer.b_pos ), b_rtam( c_buffer.b_rtam ) {

      memcpy( buffer, c_buffer.buffer, b_tam );
    }

    Buffer::~Buffer() {
      delete [] buffer;
    }

    Buffer& Buffer::operator=(Buffer& rhs) {

      if ( (*this) == rhs ) {
	return(*this);
      }

      delete [] buffer;

      b_tam = rhs.b_tam;
      b_pos = rhs.b_pos;
      b_rtam = rhs.b_rtam;

      buffer = new unsigned char[b_tam];
      memcpy(buffer,rhs.buffer,b_tam);

      return (*this);
    }

    bool Buffer::operator==(Buffer& rhs) {
      return( (buffer == rhs.buffer) && (b_tam == rhs.b_tam) && (b_pos == rhs.b_pos) && (b_rtam == rhs.b_rtam) );
    }

    void Buffer::escribe(const void * const dato,const MemoryBlock::MemoryType bytes) {

      if( b_pos + bytes > b_tam ) {
	MemoryBlock::MemoryType block_tam = getBlockTam();
	MemoryBlock::MemoryType block_num = ( ( b_pos + bytes ) / block_tam ) + 1;
	unsigned char * aux = new unsigned char[ block_num * block_tam ];
	
	
	memcpy(aux,buffer,b_tam);

	b_tam = block_num * block_tam;
      
	delete [] buffer;
	buffer = aux;
      }

      memcpy(&(buffer[b_pos]),dato,bytes);
      b_pos+=bytes;
      b_rtam+=bytes;
    }

    MemoryBlock::MemoryType Buffer::lee(void *const dato,const MemoryBlock::MemoryType bytes) {

      MemoryBlock::MemoryType e_bytes = bytes;
      if(b_pos+bytes > b_rtam) {
	e_bytes = b_rtam-b_pos;
      }

      memcpy(dato,&(buffer[b_pos]),e_bytes);

      b_pos+=e_bytes;
      return(e_bytes);
    }

    void Buffer::reset() {

      MemoryBlock::MemoryType block_tam = getBlockTam();

      delete [] buffer;

      buffer = new unsigned char[ block_tam ];
      b_tam = block_tam;
      b_pos = 0;
      b_rtam = 0;
    }

    MemoryBlock::MemoryType Buffer::getBlockTam() {

      if( Buffer::BLOCK_SIZE_PARAMETER != 0 ) {
	return Buffer::BLOCK_SIZE_PARAMETER;
      }
      
      const ParameterValueType * const blockSize = ConfigurationManager::instance().getParameter( Buffer::BLOCK_SIZE_PARAMETER_KEY );

      if( blockSize != NULL ) {
	Buffer::BLOCK_SIZE_PARAMETER = atoi( blockSize->c_str() );
      } else {
	Buffer::BLOCK_SIZE_PARAMETER = DEFAULT_BLOCK_SIZE;
      }

      return Buffer::BLOCK_SIZE_PARAMETER;

    }

  }
}
  
