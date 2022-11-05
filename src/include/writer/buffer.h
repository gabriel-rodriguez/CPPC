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



#if ! defined CPPC_WRITER_BUFFER_H
#define CPPC_WRITER_BUFFER_H

#include <data/data_type.h>
#include <data/memory_block.h>
#include <util/configuration/configuration_manager.h>

using cppc::data::DataType;
using cppc::data::MemoryBlock;
using cppc::util::configuration::ParameterKeyType;

namespace cppc {
  namespace writer {

    class Buffer {

    public:
      // Constructors
      Buffer();
      Buffer( const DataType::DataTypeSize ); // Sized
      Buffer( const DataType::DataTypeSize, const void * const ); // With size & data
      Buffer( const Buffer& );

      // Destructor
      ~Buffer();

      // Operators
      Buffer& operator=( Buffer& );
      bool operator==( Buffer& );

      // Methods

      // Write a certain amount of bytes
      void escribe( const void * const, const DataType::DataTypeSize );

      // Reads a certain amount of bytes
      DataType::DataTypeSize lee( void * const, const DataType::DataTypeSize );

      inline void rewind() { seek( 0 ); } //Restart pointer to 0
      inline void forward() { seek( b_rtam ); } //Set pointer in first free position
      inline void seek(const int& byte) { b_pos = byte; } //Set pointer to given offset
      inline MemoryBlock::MemoryType getTam() { return(b_rtam); }
      inline MemoryBlock::MemoryType getPos() { return(b_pos); }
      inline const unsigned char * datos() { return buffer; }
      void reset();

    private:
      // Members
      MemoryBlock::MemoryType b_tam; // Buffer size in memory
      unsigned char *buffer; // Data
      MemoryBlock::MemoryType b_pos; // Pointer to current position inside buffer
      MemoryBlock::MemoryType b_rtam; // Pointer to first free (non-written) position into buffer

      static MemoryBlock::MemoryType getBlockTam();
      const static ParameterKeyType BLOCK_SIZE_PARAMETER_KEY;
      static MemoryBlock::MemoryType BLOCK_SIZE_PARAMETER;
      const static MemoryBlock::MemoryType DEFAULT_BLOCK_SIZE;
    };
  }
}

#endif
