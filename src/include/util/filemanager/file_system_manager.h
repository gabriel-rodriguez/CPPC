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



#if ! defined CPPC_UTIL_FILESYSTEM_FILEMANAGER_H
#define CPPC_UTIL_FILESYSTEM_FILEMANAGER_H

#include <data/cppc_file.h>
#include <util/filemanager/directory_stream.h>

#include <cstddef>
#include <string>

#include <sys/types.h>

using cppc::data::CppcFile;

using std::string;

//FIXME mode_t is not a completely portable solution

namespace cppc {
  namespace util {
    namespace filesystem {

      class CPPC_FILESYSTEM_PLUGIN {
      public:
        static bool createDirectory( const string, const mode_t );
        static bool fileExists( const string );
        static std::size_t getFileSize( const string );
        static CppcFile::FileOffset offsetGet( void *,
          CppcFile::DescriptorType );
        static void * openFile( void *, CppcFile::DescriptorType, string );
        static void offsetSeek( void *, CppcFile::DescriptorType,
          CppcFile::FileOffset );
        static DirectoryStream * openDirectory( const string );
        static bool removeDirectory( const string );
        static bool removeFile( const string );
      };

      template <typename FileSystemPlugin> class InternalFileSystemManager {

      public:

        inline static bool createDirectory( const string path,
          const mode_t mode ) {

          return FileSystemPlugin::createDirectory( path, mode );
        }

        inline static bool fileExists( const string path ) {
          return FileSystemPlugin::fileExists( path );
        }

        inline static std::size_t getFileSize( const string path ) {
          return FileSystemPlugin::getFileSize( path );
        }

        inline static CppcFile::FileOffset offsetGet( void * descriptor,
          CppcFile::DescriptorType type ) {

          return FileSystemPlugin::offsetGet( descriptor, type );
        }

        inline static void offsetSeek( void * descriptor,
          CppcFile::DescriptorType dtype, CppcFile::FileOffset offset ) {

          FileSystemPlugin::offsetSeek( descriptor, dtype, offset );
        }

        inline static void * openFile( void * descriptor,
          const CppcFile::DescriptorType dtype, const string path ) {

          return FileSystemPlugin::openFile( descriptor, dtype, path );
        }

        inline static DirectoryStream * openDirectory( const string path ) {
          return FileSystemPlugin::openDirectory( path );
        }

        inline static bool removeDirectory( const string path ) {
          return FileSystemPlugin::removeDirectory( path );
        }

        inline static bool removeFile( const string path ) {
          return FileSystemPlugin::removeFile( path );
        }

      private:
        InternalFileSystemManager() {}
        InternalFileSystemManager( const InternalFileSystemManager & ) {}
        InternalFileSystemManager & operator=(
          const InternalFileSystemManager & ) { return *this; }
        ~InternalFileSystemManager() {}

      };

      typedef InternalFileSystemManager<CPPC_FILESYSTEM_PLUGIN> FileSystemManager;
    }
  }
}

#endif
