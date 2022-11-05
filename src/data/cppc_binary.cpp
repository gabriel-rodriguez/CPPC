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




#include <data/cppc_binary.h>
#include <data/data_type_factory.h>
#include <writer/writer.h>

namespace {

  using cppc::data::Binary;
  using cppc::data::DataType;
  using cppc::data::DataTypeFactory;

  DataType * createBinary() {
    return new Binary();
  }

  const bool registered = DataTypeFactory::instance().registerDataType( Binary::typeIdentifier, 1, createBinary );
  
}

namespace cppc {
  namespace data {

    const DataType::DataTypeIdentifier Binary::typeIdentifier = 11;

    Binary::Binary() : value( NULL ), size( 0 ) {}

    Binary::Binary( const void * const v, int s ) : value( v ), size( s ) {}

    Binary::~Binary() {}

  }
}
    
