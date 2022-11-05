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



#if ! defined CPPC_UTIL_FILESYSTEM_UNIXFILESYSTEMPLUGIN_H
#define CPPC_UTIL_FILESYSTEM_UNIXFILESYSTEMPLUGIN_H

#include <data/cppc_file.h>
#include <util/filemanager/directory_stream.h>

#include <cstddef>
#include <string>
#include <sys/stat.h>
#include <sys/types.h>

#include <dirent.h>

using cppc::data::CppcFile;

using std::string;

namespace cppc {
  namespace util {
    namespace filesystem {

      class UnixFileSystemPlugin {

      public:

        static bool createDirectory( const string, ::mode_t );
        static bool fileExists( const string );
        static std::size_t getFileSize( const string );
        static CppcFile::FileOffset offsetGet( void *,
          CppcFile::DescriptorType );
        static void offsetSeek( void *, CppcFile::DescriptorType,
          CppcFile::FileOffset );
        static void * openFile( void *, const CppcFile::DescriptorType,
          const string );
        static DirectoryStream * openDirectory( const string );
        static bool removeDirectory( const string );
        static bool removeFile( const string );

      private:
        UnixFileSystemPlugin();
        UnixFileSystemPlugin( const UnixFileSystemPlugin & );
        UnixFileSystemPlugin & operator=( const UnixFileSystemPlugin & );
        ~UnixFileSystemPlugin();
      };

      class UnixDirectoryStream : public DirectoryStream {

      public:
        virtual string next();
        virtual bool hasNext();
        virtual ~UnixDirectoryStream();

      private:
        UnixDirectoryStream( const string );
        UnixDirectoryStream( const UnixDirectoryStream & );
        virtual UnixDirectoryStream & operator=( const UnixDirectoryStream & );

        DIR * _directory;
        struct dirent * _dirent;

        friend class UnixFileSystemPlugin;
      };

    }
  }
}

#endif
