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



#if ! defined CPPC_DATA_HEAPCONTEXT_H
#define CPPC_DATA_HEAPCONTEXT_H

#include <data/context.h>

#include <string>

using std::string;

namespace cppc {
  namespace data {

    class HeapContext : public Context {

    public:
      HeapContext( Context *, string &, unsigned int );
      virtual ~HeapContext();

      virtual bool operator==( const Context & );

      virtual inline RegisterVector * getRegisters() { return registers; }
      virtual void addRegister( Register * );
      virtual bool removeRegister( const string & );
      virtual Register * getRegister( const string & );
      void setRegisters( RegisterVector * );
      virtual inline void clearRegisters() {
        this->setRegisters( new RegisterVector() );
      }

      virtual inline string getFunctionName() { return functionName; }
      virtual inline unsigned int getCalledFromLine() const { return calledFromLine; }

      virtual inline Context * clone() { return new HeapContext( *this ); }

      virtual bool isEmpty();

      virtual inline bool getCheckpointHere() { return checkpointHere; }
      virtual inline void setCheckpointHere( bool b ) { checkpointHere = b; }

      virtual Context * addSubcontextTree( Context *, BlockMap &, BlockMap &, MemoryBlock::MemoryType );
      virtual void update( Context *, BlockMap &, BlockMap &, MemoryBlock::MemoryType );

    protected:
      HeapContext( const HeapContext & );

    private:
      virtual HeapContext & operator=( const HeapContext & );

      string functionName;
      unsigned int calledFromLine;
      RegisterVector * registers;
      bool checkpointHere;
    };

  }
}

#endif
