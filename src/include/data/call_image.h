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



#if ! defined CPPC_DATA_CALL_IMAGE_H
#define CPPC_DATA_CALL_IMAGE_H

#include <data/context.h>

#include <string>
#include <vector>

using std::string;
using std::vector;

namespace cppc {
  namespace data {

    class CallImage {

    public:
      CallImage( const string &, unsigned int );
      CallImage( const CallImage & );
      ~CallImage();

      bool operator==( const CallImage & );

      inline string getFunctionName() { return functionName; }
      inline void setFunctionName( const string & s ) { functionName = s; }
      inline unsigned int getCalledFromLine() { return calledFromLine; }
      inline void setCalledFromLine( unsigned int l ) { calledFromLine = l; }

      void addParameter( Register * );
      Register * getParameter( const string & );
      void removeParameter( const string & );
      inline vector<Register *> * getParameters() { return parameters; }

      inline BlockMap * getBlocks() { return blocks; }
      void setBlocks( BlockMap * blocks );

      void splitMemory();
      void update( CallImage *, MemoryBlock::MemoryType );

    private:
      CallImage & operator=( const CallImage & );

      string functionName;
      unsigned int calledFromLine;
      vector<Register *> * parameters;
      BlockMap * blocks;
      bool ownsBlockMemory;
    };

  }
}

#endif
