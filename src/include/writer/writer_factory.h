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




#if ! defined CPPC_WRITER_WRITERFACTORY_H
#define CPPC_WRITER_WRITERFACTORY_H

#include <util/singleton/singleton.h>
#include <writer/writer.h>

#include <map>

using cppc::util::singleton::Singleton;
using std::map;

namespace cppc {
  namespace writer {

    class WriterFactory:public Singleton<WriterFactory> {

      typedef Writer * (*CreateWriterCallback)();

    public:
      Writer * getWriter( WriterType );
      bool registerWriter( WriterType, CreateWriterCallback );

    private:
      WriterFactory();
      ~WriterFactory();
      WriterFactory(const WriterFactory&);
      WriterFactory& operator=(WriterFactory&);

      typedef map<BasicWriterType, CreateWriterCallback> CallbackMap;

      CallbackMap writers;

      friend class Singleton<WriterFactory>;

    };

  }
}

#endif
