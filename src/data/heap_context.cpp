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



#include <data/heap_context.h>

#include <cassert>

namespace cppc {
  namespace data {

    HeapContext::HeapContext( Context * c, string & s, unsigned int l )
      : Context( c ), functionName( s ), calledFromLine( l ),
      registers( new RegisterVector() ), checkpointHere( false ) {}

    // Deep copy of this Context's hierarchy
    HeapContext::HeapContext( const HeapContext & copy )
      : Context( copy ), functionName( copy.functionName ),
      calledFromLine( copy.calledFromLine ), registers( new RegisterVector() ),
      checkpointHere( copy.checkpointHere ) {

      for( RegisterVector::const_iterator it = copy.registers->begin();
        it != copy.registers->end();
        it++ ) {

        registers->push_back( new Register( **it ) );
      }
    }

    HeapContext::~HeapContext() {
      for( RegisterVector::iterator it = registers->begin();
        it != registers->end();
        it++ ) {

        delete *it;
      }

      delete registers;
    }

    HeapContext & HeapContext::operator=( const HeapContext & rhs ) {

      if( this == &rhs ) {
        return *this;
      }

      Context::operator=( rhs );

      functionName = rhs.functionName;
      calledFromLine = rhs.calledFromLine;
      registers = rhs.registers;
      checkpointHere = rhs.checkpointHere;

      return *this;
    }

    void HeapContext::addRegister( Register * r ) {
      for( RegisterVector::iterator it = registers->begin();
        it != registers->end();
        it++ ) {

        if( **it == *r ) {
          return;
        }
      }

      registers->push_back( r );
    }

    bool HeapContext::removeRegister( const string & name ) {
      for( RegisterVector::iterator it = registers->begin();
        it != registers->end();
        it++ ) {

        if( (*it)->name() == name ) {
          registers->erase( it );
          return true;
        }
      }

      return false;
    }

    Register * HeapContext::getRegister( const string & name ) {
      for( RegisterVector::iterator it = registers->begin();
        it != registers->end();
        it++ ) {

        if( (*it)->name() == name ) {
          return *it;
        }
      }

      return 0;
    }

    void HeapContext::setRegisters( RegisterVector * rv ) {
      for( RegisterVector::iterator it = registers->begin();
        it != registers->end(); it++ ) {

        delete *it;
      }

      delete registers;
      registers = rv;
    }

    bool HeapContext::operator==( const Context & rhs ) {
      const HeapContext * safe_rhs = dynamic_cast<const HeapContext *>( &rhs );
      if( safe_rhs == 0 ) {
        return false;
      }

      bool equalParents = false;
      if( this->getParent() == 0 ) {
        equalParents = (safe_rhs->getParent() == 0);
      } else {
        equalParents = ( (*this->getParent()) == (*safe_rhs->getParent()) );
      }

      return( equalParents &&
        ( this->functionName == safe_rhs->functionName ) &&
        ( this->calledFromLine == safe_rhs->calledFromLine ) );
    }

    bool HeapContext::isEmpty() {

      if( !Context::isEmpty() ) {
        return false;
      }

      return( registers->size() == 0 );
    }

    Context * HeapContext::addSubcontextTree( Context * rhs, BlockMap & blocks, BlockMap & rhsBlocks, MemoryBlock::MemoryType fragmentSize ) {
      Context * newSubcontext = Context::addSubcontextTree( rhs, blocks, rhsBlocks, fragmentSize );

      HeapContext * safe_rhs = dynamic_cast<HeapContext*>( newSubcontext );
      assert( safe_rhs && "Adding subcontext tree with LoopContext" );

      // Add memory blocks referenced by new registers
      for( vector<Register*>::iterator i = safe_rhs->registers->begin(); i != safe_rhs->registers->end(); ++i ) {
        Register * newReg = *i;
        //FIXME:remove
        MemoryBlock::BlockCode code = newReg->getCode();
        BlockMap::iterator b = blocks.find( code );
        if( b == blocks.end() ) {
          // Block did not exist. Add it.
          blocks.insert( BlockMap::value_type( code, rhsBlocks.find(code)->second ) );
        } else {
          // Update block
          b->second->update( rhsBlocks.find(code)->second, fragmentSize );
        }
      }

      return safe_rhs;
    }

    void HeapContext::update( Context * rhs, BlockMap & blocks, BlockMap & rhsBlocks, MemoryBlock::MemoryType fragmentSize ) {
      Context::update( rhs, blocks, rhsBlocks, fragmentSize );

      HeapContext * safe_rhs = dynamic_cast<HeapContext*>( rhs );
      assert( safe_rhs && "Updating HeapContext with LoopContext" );
      RegisterVector * newRegisters = new RegisterVector();

      // Update registers
      for( vector<Register *>::iterator i = safe_rhs->registers->begin(); i != safe_rhs->registers->end(); ++i ) {
        Register * rhsReg = *i;
        bool updated = false;
        for( vector<Register *>::iterator j = this->registers->begin(); j != this->registers->end(); ++j ) {
          Register * reg = *j;
          // In this case, registers have to be compared attending at their name (their memory address *will* be different)
          if( reg->name() == rhsReg->name() ) {
            newRegisters->push_back( new Register( *reg ) );
            MemoryBlock::BlockCode code = reg->getCode();
            MemoryBlock::BlockCode rhsCode = rhsReg->getCode();

            MemoryBlock * block = blocks.find( code )->second;
            BlockMap::iterator k = rhsBlocks.find( rhsCode );
            if( k != rhsBlocks.end() ) {
              // Each block needs to be updated only once. After this happens, remove it from
              // rhs so that it is not updated again.
              block->update( k->second, fragmentSize );
              rhsBlocks.erase( k );
            }

            updated = true;
            break;
          }
        }

        if( !updated ) {
          // This is a new register. Copy over.
          BlockMap::iterator j = rhsBlocks.find( rhsReg->getCode() );
          assert( j != rhsBlocks.end() );
          // Remove block from rhs so that reserved memory does not get freed when deleting incremental checkpoint
          rhsBlocks.erase( j );
          // Ensure that this block code does not exist in the full checkpoint. I cannot think of a situation where this assertion will fail,
          // but better safe than sorry.
          assert( blocks.find( rhsReg->getCode()) == blocks.end() );
          blocks.insert( BlockMap::value_type( rhsReg->getCode(), j->second ) );
          MemoryBlock * b = j->second;
          // The new block should not contain incremental memory, as there is nothing to build upon.
          assert( b->getFragments().empty() );
          newRegisters->push_back( rhsReg );
        }
      }

      // Remove old registers that are not present in the incremental checkpoint
      for( vector<Register*>::iterator i = this->registers->begin(); i < this->registers->end(); ++i ) {
        delete *i;
      }

      delete registers;
      registers = newRegisters;

      // Update checkpointHere
      this->checkpointHere = safe_rhs->checkpointHere;
    }
  }
}
