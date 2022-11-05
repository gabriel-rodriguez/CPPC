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



#if ! defined CPPC_UTIL_FILESYSTEM_DIRECTORYSTREAM_H
#define CPPC_UTIL_FILESYSTEM_DIRECTORYSTREAM_H

#include <string>

using std::string;

namespace cppc {
  namespace util {
    namespace filesystem {

      class DirectoryStream {

      public:
        virtual string next() = 0;
        virtual bool hasNext() = 0;
        virtual ~DirectoryStream() {}

        protected:
          DirectoryStream() {}
          DirectoryStream( const DirectoryStream & ds ) {}
          virtual DirectoryStream & operator=( const DirectoryStream & rhs ) {
            return *this;
          }

      };

    }
  }
}

#endif
