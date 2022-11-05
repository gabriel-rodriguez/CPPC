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




#include <writer/writer_factory.h>

#include <cassert>

#include <iostream>
using std::cout;

namespace cppc {
  namespace writer {

    WriterFactory::WriterFactory() : writers( CallbackMap() ) {}
 
    WriterFactory::~WriterFactory() {}

    WriterFactory::WriterFactory( const WriterFactory & wf ) : writers( wf.writers ) {}

    WriterFactory & WriterFactory::operator=( WriterFactory & rhs ) {

      if( this == &rhs ) {
	return *this;
      }

      writers = rhs.writers;

      return *this;
    }

    Writer * WriterFactory::getWriter( WriterType code ) {

      CallbackMap::const_iterator i = writers.find( code.getStaticValue() );
      if( i == writers.end() ) {
        // No such writer found. Return NULL.
        return 0;
      }

      return (i->second)();      
    }

    bool WriterFactory::registerWriter( WriterType type, CreateWriterCallback callback ) {

      bool ret = writers.insert( CallbackMap::value_type( type.getStaticValue(), callback ) ).second;

      if( !ret ) {
      	cout << "CPPC WARNING: Registering writer with code " << (unsigned int)type.getStaticValue() << " more than once\n";
      }

      return ret;

    }

  }
}
