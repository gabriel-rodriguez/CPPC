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



#include <data/cppc_file.h>
#include <util/filemanager/file_system_manager.h>

using cppc::util::filesystem::FileSystemManager;

namespace cppc {
  namespace data {

    CppcFile::CppcFile( FileCode e_code, void * e_desc,
      DescriptorType e_type, FileOffset e_offset, string e_path )
      : code( e_code ), descriptor( e_desc ), dtype( e_type ),
        offset( e_offset ), path( e_path ) {}

    CppcFile::CppcFile( const CppcFile & file )
      : code( file.code ), descriptor( file.descriptor ), dtype( file.dtype ),
        offset( file.offset ), path( file.path ) {}

    CppcFile & CppcFile::operator=( const CppcFile & rhs ) {

      if( this == &rhs ) {
        return *this;
      }

      code = rhs.code;
      descriptor = rhs.descriptor;
      dtype = rhs.dtype;
      offset = rhs.offset;
      path = rhs.path;

      return *this;
    }

    CppcFile::~CppcFile() {}

  }
}
