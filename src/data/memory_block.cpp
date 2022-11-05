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



#include <data/memory_block.h>

#include <cassert>
#include <cmath>
#include <cstring>

namespace cppc {
  namespace data {
    MemoryBlock::BlockCode MemoryBlock::nextCode = 0;

    // This magic number is the least common multiple of 12 and 16. In some machines, the storage size of a long double is 12 bytes.
    // Using this value we ensure that, whichever the data type, we store a natural number of elements per block. If we built a
    // fragment cutting an element in half the resulting checkpoint file would not be portable.
    const MemoryBlock::MemoryType MemoryBlock::fragmentSize = 4112;
    unsigned char * initializeZeroHash() {
      // This memory needs to be freed by calling MemoryBlock::freeStaticMemory() upon shutdown
      unsigned char * zeroHash = new unsigned char[ SHA_DIGEST_LENGTH ];
      unsigned char * zeros = new unsigned char[MemoryBlock::fragmentSize](); // Use the constructor that zero-initializes memory
      SHA1( zeros, MemoryBlock::fragmentSize, zeroHash );
      delete [] zeros;
      return zeroHash;
    }
    const unsigned char * MemoryBlock::zeroHash = initializeZeroHash();

    MemoryBlock::MemoryBlock( MemoryType memInit_p, MemoryType memEnd_p,
        DataType::DataTypeIdentifier type_p, RegisterVector * registers_p )
    : code( nextCode++ ), memInit( memInit_p ), memEnd( memEnd_p ), type( type_p ),
      registers( registers_p ), hashCodes( 0 ), fragments( ), fullStore( true ) {}
    //FIXME: in C++11 this should be implemented by delegation into the next constructor

    MemoryBlock::MemoryBlock( BlockCode code_p, MemoryType memInit_p, MemoryType memEnd_p,
        DataType::DataTypeIdentifier type_p, RegisterVector * registers_p )
    : code( code_p ), memInit( memInit_p ), memEnd( memEnd_p ), type( type_p ),
      registers( registers_p ), hashCodes( 0 ), fragments( ), fullStore( true ) {

      if( code >= nextCode ) {
        nextCode = code+1;
      }
    }

    MemoryBlock::MemoryBlock( const MemoryBlock & rhs )
    : code( rhs.code ), memInit( rhs.memInit ), memEnd( rhs.memEnd ),
      type( rhs.type ), registers( new RegisterVector( *rhs.registers ) ),
      hashCodes( 0 ), fragments( rhs.fragments ),
      fullStore( rhs.fullStore ) {

      // Copy hash codes (if they exist)
      if( rhs.hashCodes ) {
        MemoryType blockSize = memEnd - memInit;
        unsigned pages = std::ceil( blockSize / MemoryBlock::fragmentSize );
        hashCodes = new SHA1Type[ pages ];
        std::memcpy( hashCodes, rhs.hashCodes, pages*SHA_DIGEST_LENGTH );
      }
    }

    MemoryBlock::~MemoryBlock() {
      delete registers;
      if( hashCodes ) {
        delete [] hashCodes;
      }
    }

    MemoryBlock * MemoryBlock::deepClone() {
      // Copy memory of this block
      MemoryBlock::MemoryType blockSize = this->memEnd - this->memInit;
      unsigned char * copy = new unsigned char[ blockSize ];
      std::memcpy( copy, reinterpret_cast<void *>( this->memInit ), blockSize );
      MemoryBlock::MemoryType newInit = reinterpret_cast<MemoryBlock::MemoryType>( copy );

      MemoryBlock * clone = new MemoryBlock( *this );
      clone->memInit = newInit;
      clone->memEnd = newInit+blockSize;

      return clone;
    }

    void MemoryBlock::prepareForCheckpoint( bool fullCheckpoint ) {
      // If this checkpoint is a full checkpoint: invalidate previous hashes and fragments.
      if( fullCheckpoint ) {
        this->invalidateAll();
      }

      MemoryType blockSize = memEnd - memInit;
      if( blockSize < MemoryBlock::fragmentSize ) {
        // Do nothing for memory blocks smaller than pageSize
        return;
      }
      unsigned pages = std::ceil( (float)blockSize / MemoryBlock::fragmentSize );

      // If we have previous hashes and this is not a full checkpoint: save old hashes. Rehash.
      // Spot the differences. Create fragments as needed.
      SHA1Type * oldHashes = hashCodes;
      this->rehash( blockSize, pages );

      fragments.clear();
      bool fragmentStarted = false;
      bool startedIsZero = false;
      MemoryType fragmentStart = 0;
      MemoryType totalSize = 0;
      for( unsigned i = 0; i < pages; ++i ) {
        bool changed = !oldHashes || ( std::memcmp(hashCodes[i], oldHashes[i], SHA_DIGEST_LENGTH) != 0 );
        if( changed ) {
          bool fragmentZero = !std::memcmp( hashCodes[i], zeroHash, SHA_DIGEST_LENGTH );
          if( !fragmentStarted ) {
            fragmentStart = i;
            fragmentStarted = true;
            startedIsZero = fragmentZero;
          } else {
            if( fragmentZero != startedIsZero ) {
              //FIXME: this code is a candidate to be extracted into a function (repeated thrice)
              BlockFragment bf;
              bf.fragmentCode = fragmentStart;
              bf.fragmentSize = (i-fragmentStart) * MemoryBlock::fragmentSize;
              bf.fragmentAddress = startedIsZero? 0 : 1;
              totalSize += bf.fragmentSize;
              fragments.push_back( bf );
              fragmentStart = i;
              startedIsZero = fragmentZero;
            }
          }
        } else {
          if( fragmentStarted ) {
            BlockFragment bf;
            bf.fragmentCode = fragmentStart;
            bf.fragmentSize = (i - fragmentStart) * MemoryBlock::fragmentSize;
            bf.fragmentAddress = startedIsZero? 0 : 1;
            totalSize += bf.fragmentSize;
            fragments.push_back( bf );
            fragmentStarted = false;
          }
        }
      }

      if( fragmentStarted ) {
        BlockFragment bf;
        bf.fragmentCode = fragmentStart;
        bf.fragmentSize = blockSize - fragmentStart * MemoryBlock::fragmentSize;
        bf.fragmentAddress = startedIsZero? 0 : 1;
        totalSize += bf.fragmentSize;
        fragments.push_back( bf );
      }

      if( (fragments.size() == 1) && (fragments[0].fragmentSize == blockSize) ) {
        // All pages changed. Mark this block as being fully stored.
        this->fullStore = true;
      } else {
        // Only some fragments are to be stored.
        this->fullStore = false;
      }

      delete [] oldHashes;
    }

    void MemoryBlock::rehash( MemoryType blockSize, unsigned pages ) {
      // Calculate number of subblocks. Allocate memory for the hash codes.
      hashCodes = new SHA1Type[ pages ];

      for( unsigned i = 0; i < pages-1; ++i ) {
        MemoryType pageInit = memInit + i*MemoryBlock::fragmentSize;
        SHA1( reinterpret_cast<unsigned char *>( pageInit ), MemoryBlock::fragmentSize, hashCodes[i] );
      }
      MemoryType lastPage = memInit + (pages-1)*MemoryBlock::fragmentSize;
      MemoryType lastLength = memEnd - lastPage;
      SHA1( reinterpret_cast<unsigned char *>( lastPage), lastLength, hashCodes[pages-1] );
    }

    void MemoryBlock::unpack( MemoryType fragmentSize ) {
      if( fragments.empty() ) {
        return;
      }

      // Find out total block size
      unsigned totalSize = 0;
      for( vector<BlockFragment>::iterator i = fragments.begin(); i != fragments.end(); ++i ) {
        totalSize += i->fragmentSize;
      }

      // Allocate new, contiguous memory for this block. Copy data over.
      unsigned char * mem = new unsigned char[totalSize];
      this->memInit = reinterpret_cast<MemoryType>( mem );
      this->memEnd = memInit + totalSize;
      for( vector<BlockFragment>::iterator i = fragments.begin(); i != fragments.end(); ++i ) {
        if( i->fragmentAddress ) {
          std::memcpy( &(mem[i->fragmentCode*fragmentSize]), reinterpret_cast<void*>(i->fragmentAddress), i->fragmentSize );
          delete [] reinterpret_cast<unsigned char*>(i->fragmentAddress);
        } else {
          unsigned char * zeros = new unsigned char[ i->fragmentSize ](); // Zero-initialized
          std::memcpy( &(mem[i->fragmentCode*fragmentSize]), zeros, i->fragmentSize );
          delete [] zeros;
        }
      }
    }

    void MemoryBlock::update( MemoryBlock * rhs, MemoryType fragmentSize ) {
      // If rhs->fragments is empty, this memory block is whole. Copy over.
      if( rhs->fullStore ) {
        MemoryType size = memEnd-memInit, rhsSize = rhs->memEnd-rhs->memInit;
        assert( size == rhsSize );
        unsigned char * content = reinterpret_cast<unsigned char *>( memInit );
        delete [] content;
        memInit = rhs->memInit;
        memEnd = rhs->memEnd;
      } else {
        for( vector<BlockFragment>::iterator i = rhs->fragments.begin(); i != rhs->fragments.end(); ++i ) {
          std::memcpy( reinterpret_cast<void*>(this->memInit + i->fragmentCode*fragmentSize),
                       reinterpret_cast<void*>(i->fragmentAddress),
                       i->fragmentSize );

          delete [] reinterpret_cast<unsigned char*>( i->fragmentAddress );
        }
      }
    }
  }
}
