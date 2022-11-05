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



#if ! defined CPPC_WRITER_WRITER_H
#define CPPC_WRITER_WRITER_H

#include <data/cppc_basic.h>
#include <writer/buffer.h>

using cppc::data::BasicUCharacterType;
using cppc::data::UCharacterType;
using cppc::data::DataType;

namespace cppc {

  namespace checkpointer {
    class Checkpoint;
  }

  namespace writer {

    typedef BasicUCharacterType BasicWriterType;
    typedef UCharacterType WriterType;

    class Writer {

    public:
      virtual ~Writer();

      virtual void write( cppc::checkpointer::Checkpoint * ) = 0;
      virtual cppc::checkpointer::Checkpoint * readCheckpoint( string ) = 0;
      virtual bool testCheckpoint( string ) = 0;

      virtual WriterType getWriterType() = 0;

    protected:
      Writer();
      Writer(const Writer&);
      virtual Writer& operator=(const Writer&);

    };
  }
}

#endif
