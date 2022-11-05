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



#if ! defined CPPC_DATA_CONTEXT_H
#define CPPC_DATA_CONTEXT_H

#include <data/register.h>
#include <writer/writer.h>

#include <string>
#include <vector>

using cppc::writer::Writer;

using std::string;
using std::vector;

namespace cppc {
  namespace data {

    class CallImage;

    class Context {

    public:
      Context( Context * );
      virtual ~Context();

      virtual bool operator==( const Context & ) = 0;
      inline bool operator!=( const Context & rhs ) {
        return !operator==(rhs);
      }

      inline Context * getParent() const { return parent; }
      inline void setParent( Context * c ) { parent = c; }

      inline vector<Context *> * getSubcontexts() { return subcontexts; }
      void addSubcontext( Context * );
      void removeSubcontext( Context * );
      inline void setSubcontexts( vector<Context *> * v ) { delete subcontexts; subcontexts = v; }

      virtual RegisterVector * getRegisters() = 0;
      virtual void addRegister( Register * ) = 0;
      virtual bool removeRegister( const string & ) = 0;
      virtual Register * getRegister( const string & ) = 0;
      virtual void clearRegisters() = 0;

      void addCallImage( CallImage * );
      inline vector<CallImage *> * getCallImages() { return callImages; }
      void fetchCallImage( string, unsigned int );
      inline CallImage * getCurrentCallImage() { return currentCallImage; }
      CallImage * getCallImage( string, unsigned int );
      void releaseCallImage();
      void removeCurrentCallImage();
      void setCallImages( vector<CallImage *> * );

      virtual string getFunctionName() = 0;
      virtual unsigned int getCalledFromLine() const = 0;

      virtual Context * clone() = 0;

      virtual bool isEmpty();

      virtual bool getCheckpointHere() = 0;
      virtual void setCheckpointHere( bool ) = 0;

      virtual Context * addSubcontextTree( Context *, BlockMap &, BlockMap &, MemoryBlock::MemoryType );
      virtual void update( Context *, BlockMap &, BlockMap &, MemoryBlock::MemoryType );

    protected:
      Context( const Context & );
      virtual Context & operator=( const Context & );

    private:
      Context * parent;
      vector<Context *> * subcontexts;
      vector<CallImage *> * callImages;
      CallImage * currentCallImage;
    };
  }
}

#endif
