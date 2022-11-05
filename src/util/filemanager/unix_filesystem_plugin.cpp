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
#include <util/filemanager/unix_filesystem_plugin.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>

#include <climits>
#include <cstdlib>

using cppc::data::CppcFile;

namespace cppc {
  namespace util {
    namespace filesystem {

      UnixFileSystemPlugin::UnixFileSystemPlugin() {}

      UnixFileSystemPlugin::UnixFileSystemPlugin(
        const UnixFileSystemPlugin & ufsp ) {}

      UnixFileSystemPlugin & UnixFileSystemPlugin::operator=(
        const UnixFileSystemPlugin & rhs ) {

        return *this;
      }

      UnixFileSystemPlugin::~UnixFileSystemPlugin() {}

      bool UnixFileSystemPlugin::createDirectory( const string path,
        mode_t mode ) {

        return ::mkdir( path.c_str(), mode ) == 0;
      }

      bool UnixFileSystemPlugin::fileExists( const string path ) {
        struct stat buffer;
        return ::stat( path.c_str(), &buffer ) == 0;
      }

      std::size_t UnixFileSystemPlugin::getFileSize( const string path ) {
        struct stat buffer;
        ::stat( path.c_str(), &buffer );
        return buffer.st_size;
      }

      CppcFile::FileOffset UnixFileSystemPlugin::offsetGet( void * descriptor,
        CppcFile::DescriptorType dtype ) {

          switch( dtype ) {
            case CPPC_UNIX_FD:
              return lseek( *((int *)descriptor), 0, SEEK_CUR );
            case CPPC_UNIX_FILE:
              return ftell( (FILE *)descriptor );
          }

          return -1;
      }

      void UnixFileSystemPlugin::offsetSeek( void * descriptor,
        CppcFile::DescriptorType dtype, CppcFile::FileOffset offset ) {

          switch( dtype ) {
            case CPPC_UNIX_FD:
              lseek( *((int *)descriptor), offset, SEEK_SET );
              return;
            case CPPC_UNIX_FILE:
              fseek( (FILE *)descriptor, offset, SEEK_SET );
              return;
          }
      }

      void * UnixFileSystemPlugin::openFile( void * descriptor, const
        CppcFile::DescriptorType dtype, const string path ) {

        int fd;
        int * fdp = reinterpret_cast<int *>( descriptor );
        FILE * filep;

        switch( dtype ) {
          case CPPC_UNIX_FD:
            fd = open( path.c_str(), O_RDWR );
            *fdp = fd;
            return descriptor;
          case CPPC_UNIX_FILE:
            filep = fopen( path.c_str(), "r+" );
            return filep;
        }

        return 0;
      }

      DirectoryStream * UnixFileSystemPlugin::openDirectory(
        const string path ) {

        UnixDirectoryStream * s( new UnixDirectoryStream( path ) );

        return s;
      }

      bool UnixFileSystemPlugin::removeDirectory( const string path ) {
        return( ::rmdir( path.c_str() ) == 0 );
      }

      bool UnixFileSystemPlugin::removeFile( const string path ) {
        return( ::unlink( path.c_str() ) == 0 );
      }

      UnixDirectoryStream::UnixDirectoryStream( const string path )
        : DirectoryStream(), _directory( 0 ), _dirent( 0 ) {

        _directory = opendir( path.c_str() );
      }

      UnixDirectoryStream::UnixDirectoryStream(
        const UnixDirectoryStream & uds )
        : DirectoryStream( uds ), _directory( 0 ), _dirent( 0 ) {}

      UnixDirectoryStream & UnixDirectoryStream::operator=(
        const UnixDirectoryStream & rhs ) {

        DirectoryStream::operator=( rhs );
        return *this;
      }

      UnixDirectoryStream::~UnixDirectoryStream() {
        closedir( _directory );
      }

      string UnixDirectoryStream::next() {
        return string( _dirent->d_name );
      }

      bool UnixDirectoryStream::hasNext() {

        _dirent = readdir( _directory );
        return( _dirent != 0 );
      }

    }
  }
}
