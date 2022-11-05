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



#include <data/call_image.h>

#include <cassert>
#include <cstring>

namespace cppc {
  namespace data {

    CallImage::CallImage( const string & s, unsigned int l )
      : functionName( s ), calledFromLine( l ),
      parameters( new vector<Register *>() ), blocks( new BlockMap() ),
      ownsBlockMemory( false ) {}

    CallImage::CallImage( const CallImage & copy )
      : functionName( copy.functionName ),
      calledFromLine( copy.calledFromLine ),
      parameters( new vector<Register *>() ),
      blocks( copy.blocks ), ownsBlockMemory( false ) {

      for( vector<Register *>::const_iterator it =
        copy.parameters->begin(); it != copy.parameters->end(); it++ ) {

        parameters->push_back( new Register( **it ) );
      }
    }
    // ownsBlockMemory is always false, except if this object is the original
    // creator of the copy => it is the one in charge of deleting it.
    // The other approach would be using an intelligent pointer in charge of
    // controlling copies of the memory, but it would be less efficient at runtime.

    CallImage::~CallImage() {
      for( vector<Register *>::iterator it = parameters->begin();
        it != parameters->end(); it++ ) {
        delete *it;
      }

      delete parameters;
      if( ownsBlockMemory ) {
        for( BlockMap::iterator it = blocks->begin(); it != blocks->end(); it++ ) {
          char * memory = reinterpret_cast<char *>( it->second->getInitAddr() );
          delete [] memory;
          delete it->second;
        }

        delete blocks;
      }
    }

    CallImage & CallImage::operator=( const CallImage & rhs ) {

      if( this == &rhs ) {
        return *this;
      }

      functionName = rhs.functionName;
      calledFromLine = rhs.calledFromLine;
      parameters = rhs.parameters;
      blocks = rhs.blocks;
      ownsBlockMemory = rhs.ownsBlockMemory;

      return *this;
    }

    bool CallImage::operator==( const CallImage & rhs ) {

      // Checks equality from a semantic point of view
      return( ( functionName == rhs.functionName ) &&
        ( calledFromLine == rhs.calledFromLine ) );
    }

    Register * CallImage::getParameter( const string & name ) {

      for( vector<Register *>::iterator it = parameters->begin();
        it != parameters->end(); it++ ) {

        if( (*it)->name() == name ) {
          return *it;
        }
      }

      return 0;
    }

    void CallImage::addParameter( Register * r ) {

      for( vector<Register *>::iterator it = parameters->begin();
        it != parameters->end(); it++ ) {

        if( (*it)->name() == r->name() ) {
          return;
        }
      }

      parameters->push_back( r );
    }

    void CallImage::removeParameter( const string & name ) {

      for( vector<Register *>::iterator it = parameters->begin();
        it != parameters->end(); it++ ) {

        if( (*it)->name() == name ) {
          parameters->erase( it );
          return;
        }
      }
    }

    void CallImage::setBlocks( BlockMap * m ) {
      if( ownsBlockMemory ) {
        for( BlockMap::iterator it = blocks->begin(); it != blocks->end(); it++ ) {
          char * memory = reinterpret_cast<char *>( it->second->getInitAddr() );
          delete memory;
          delete it->second;
          blocks->erase( it );
        }
        delete blocks;
      }

      blocks = m;
      ownsBlockMemory = true;
    }

    void CallImage::splitMemory() {
      assert( !ownsBlockMemory );

      // The parameters are not shared with any other structure. Hence, the
      // only thing to be copied are the memory blocks contents (that is,
      // only their "memInit" and "memEnd" attributes have to be modified)
      for( BlockMap::iterator it = blocks->begin();
        it != blocks->end(); it++ ) {

        MemoryBlock * b = it->second;
        MemoryBlock::MemoryType blockSize = b->getEndAddr() - b->getInitAddr();

        unsigned char * memCopy = new unsigned char[ blockSize ];
        std::memcpy( memCopy, reinterpret_cast<void *>( b->getInitAddr() ), blockSize );

        RegisterVector * regs = b->getRegisters();
        for( vector<Register *>::iterator it = regs->begin(); it != regs->end(); it ++ ) {
          MemoryBlock::MemoryType rInit = reinterpret_cast<MemoryBlock::MemoryType>( (*it)->initAddress() );
          MemoryBlock::MemoryType rEnd = reinterpret_cast<MemoryBlock::MemoryType>( (*it)->endAddress() );
          MemoryBlock::MemoryType initOffset = rInit - b->getInitAddr();
          MemoryBlock::MemoryType sizeOffset = rEnd - rInit;
          MemoryBlock::MemoryType newInit = reinterpret_cast<MemoryBlock::MemoryType>( memCopy ) + initOffset;
          (*it)->setInitAddress( reinterpret_cast<void *>( newInit ) );
          (*it)->setEndAddress( reinterpret_cast<void *>( newInit + sizeOffset ) );
        }

        b->setInitAddr( reinterpret_cast<MemoryBlock::MemoryType>( memCopy ) );
        b->setEndAddr( b->getInitAddr() + blockSize );
      }

      ownsBlockMemory = true;
    }

    void CallImage::update( CallImage * rhs, MemoryBlock::MemoryType fragmentSize ) {
      assert( ownsBlockMemory );

      // Assume function name and line number are the same. Update the parameters' values.
      for( vector<Register *>::iterator i = rhs->parameters->begin(); i != rhs->parameters->end(); ++i ) {
        Register * rhsParam = *i;
        for( vector<Register *>::iterator j = this->parameters->begin(); j != this->parameters->end(); ++j ) {
          Register * param = *j;
          // In this case, registers have to be compared attending at their name (their memory address *will* be different)
          if( param->name() == rhsParam->name() ) {
            MemoryBlock::BlockCode code = param->getCode();
            MemoryBlock::BlockCode rhsCode = rhsParam->getCode();

            MemoryBlock * block = blocks->find( code )->second;
            BlockMap::iterator k = rhs->blocks->find( rhsCode );
            if( k != rhs->blocks->end() ) {
              // Each block needs to be updated only once. After this happens, remove it from
              // rhs so that it is not updated again.
              block->update( k->second, fragmentSize );
              rhs->blocks->erase( k );
            }
          }
        }
      }
    }

  }
}
