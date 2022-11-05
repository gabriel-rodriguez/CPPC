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



#if ! defined CPPC_DATA_LOOPCONTEXT_H
#define CPPC_DATA_LOOPCONTEXT_H

#include <data/data_type.h>
#include <data/context.h>

namespace cppc {
  namespace data {

    class LoopContext : public Context {

    public:
      LoopContext( Context *, const string &, DataType * );
      virtual ~LoopContext();

      virtual bool operator==( const Context & );

      virtual inline RegisterVector * getRegisters() {
        return 0;
      }

      virtual inline void addRegister( Register * r ) {
        getParent()->addRegister( r );
      }

      virtual inline bool removeRegister( const string & name ) {
        return getParent()->removeRegister( name );
      }

      virtual inline Register * getRegister( const string & name ) {
        return getParent()->getRegister( name );
      }

      virtual inline void clearRegisters() { return; }

      virtual inline string getFunctionName() {
        return getParent()->getFunctionName();
      }

      virtual inline unsigned int getCalledFromLine() const {
        return getParent()->getCalledFromLine();
      }

      inline string getVarName() { return varName; }
      inline DataType * getVarValue() { return varValue; }
      void setVarValue( DataType * );

      virtual inline Context * clone() { return new LoopContext( *this ); }
      virtual inline LoopContext * shallowClone() { return new LoopContext( *this, false ); }

      virtual inline bool getCheckpointHere() {
        return this->getParent()->getCheckpointHere();
      }
      virtual inline void setCheckpointHere( bool b ) {
        this->getParent()->setCheckpointHere( b );
      }

      virtual void update( Context *, BlockMap &, BlockMap &, MemoryBlock::MemoryType );

    protected:
      LoopContext( const LoopContext & );
      LoopContext( const LoopContext &, bool );

    private:
      virtual LoopContext & operator=( const LoopContext & );

      string varName;
      DataType * varValue;
    };

  }
}

#endif
