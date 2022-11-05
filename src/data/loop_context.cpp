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



#include <data/loop_context.h>

#include <cassert>
#include <cstring>

namespace cppc {
  namespace data {

    LoopContext::LoopContext( Context * c, const string & s, DataType * v )
      : Context( c ), varName( s ), varValue( v ) {}

    // Deep copy of this Context's hierarchy
    LoopContext::LoopContext( const LoopContext & copy )
      : Context( copy ), varName( copy.varName ), varValue( copy.varValue ) {}

    // Shallow copy of this Context's hierarchy. The trick here is taking
    // advantage of the regular constructor instead of the copy one. Usage of
    // the bool variable to tell one from the other is pretty vile, but it
    // works.
    LoopContext::LoopContext( const LoopContext & copy, bool )
      : Context( copy.getParent() ), varName( copy.varName ),
        varValue( copy.varValue ) {}

    LoopContext::~LoopContext() {}

    bool LoopContext::operator==( const Context & rhs ) {

      const LoopContext * safe_rhs = dynamic_cast<const LoopContext *>( &rhs );
      if( safe_rhs == 0 ) {
        return false;
      }

      return( ( this->getParent() == safe_rhs->getParent() ) &&
        ( varName == safe_rhs->varName ) &&
        ( varValue == safe_rhs->varValue ) );
    }

    LoopContext & LoopContext::operator=( const LoopContext & rhs ) {
      if( this == &rhs ) {
        return *this;
      }

      Context::operator=( rhs );

      varName = rhs.varName;
      varValue = rhs.varValue;

      return *this;
    }

    void LoopContext::setVarValue( DataType * d ) {
      varValue = d;
    }

    void LoopContext::update( Context * rhs, BlockMap & blocks, BlockMap & rhsBlocks, MemoryBlock::MemoryType fragmentSize ) {
      Context::update( rhs, blocks, rhsBlocks, fragmentSize );

      LoopContext * safe_rhs = dynamic_cast<LoopContext*>( rhs );
      assert( safe_rhs && "Updating LoopContext with HeapContext" );

      // Copy over the iteration value
      std::memcpy( varValue->getValue(), safe_rhs->varValue->getValue(), varValue->getSize() );
    }
  }
}
