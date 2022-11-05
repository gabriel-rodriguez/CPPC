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



#if ! defined CPPC_DATA_DATATYPE_H
#define CPPC_DATA_DATATYPE_H

#include <data/cppc_types.h>
#include <cstddef>

namespace cppc {

  namespace writer {
    class Writer;
  }

  namespace data {

    class DataType;

    typedef DataType *(*CreateDataTypeCallback)();

    using cppc::writer::Writer;

    class DataType {

    public:
      typedef unsigned long DataTypeSize;
      typedef unsigned char DataTypeIdentifier;

      virtual ~DataType();

      virtual DataTypeIdentifier getIdentifier() = 0;
      virtual void * getValue() = 0;
      virtual void setValue( void * ) = 0;
      virtual DataTypeSize getSize() = 0;
      virtual void add( DataType * d );

    };

  }
}

#endif
