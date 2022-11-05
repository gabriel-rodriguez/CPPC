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



#include <checkpointer/checkpointer.h>
#include <data/call_image.h>
#include <data/memory_block.h>
#include <data/register.h>
#include <writer/writer.h>
#include <writer/writer_factory.h>
#include <util/filemanager/file_system_manager.h>

#include <cassert>
#include <cstring>
#include <fstream>

using std::ifstream;

using cppc::data::CallImage;
using cppc::data::MemoryBlock;
using cppc::data::Register;
using cppc::data::RegisterVector;
using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterValueType;
using cppc::writer::Writer;
using cppc::writer::WriterFactory;
using cppc::util::filesystem::FileSystemManager;

namespace cppc {
  namespace checkpointer {

    void * thread_run( void * );

    pthread_t *Checkpointer::last_thread = NULL;

    Checkpointer::Checkpointer()
      : chkpt( new Checkpoint() ) {}

    Checkpointer::Checkpointer( Checkpoint * c )
      : chkpt( c ) {}

    Checkpointer::~Checkpointer() {
      delete chkpt;
    }

    Checkpointer::Checkpointer( const Checkpointer & c_check )
      : chkpt( c_check.chkpt ) {}

    Checkpointer& Checkpointer::operator=(const Checkpointer &c_check) {

      if(this == &c_check) {
        return(*this);
      }

      chkpt = c_check.chkpt;

      return *this;
    }

    void Checkpointer::checkpoint() {

      assert( chkpt->getContext() != 0 );

      // Wait for last thread to exit (it's no good to have multiple
      // checkpoints being written to memory at once)
      if( last_thread != NULL ) {
        pthread_join( *last_thread, NULL );
      } else {
        last_thread = new pthread_t;
      }

      pthread_create( last_thread, 0, &thread_run, this );
    }

    bool Checkpointer::integrityTest( string path ) {
      WriterType writerType;

      // If the file does not exist the integrity test fails
      if( !FileSystemManager::fileExists( path ) ) {
        return false;
      }

      // If the file is empty the integrity test fails
      if( FileSystemManager::getFileSize( path ) == 0 ) {
        return false;
      }

      // The first byte of the file must contain the writer type
      ifstream file( path.c_str(), std::ios::in );
      assert( file.is_open() );
      file.read( reinterpret_cast<char *>( writerType.getValue() ) ,
        writerType.getSize() );
      file.close();

      // Try to obtain the writer to read the file
      Writer * w = WriterFactory::instance().getWriter( writerType.getStaticValue() );
      if( !w ) {
        return false;
      }

      bool ret = w->testCheckpoint( path );

      delete w;
      return ret;
    }

    bool Checkpointer::isFullCheckpoint( string path ) {
      // The second byte of the file must contain whether this is a full checkpoint
      ifstream file( path.c_str(), std::ios::in );
      assert( file.is_open() );
      file.seekg( 1 );
      char full;
      file.read( &full, 1 );

      return full;
    }

    CheckpointCode Checkpointer::readCheckpoint( string path ) {
      WriterType writerType;

      ifstream file( path.c_str(), std::ios::in );
      assert( file.is_open() );
      file.read( reinterpret_cast<char *>( writerType.getValue() ),
        WriterType::staticSize() );

      file.close();

      this->chkpt = WriterFactory::instance().getWriter( writerType.getStaticValue() )->readCheckpoint( path );
      return chkpt->getCheckpointCode();
    }

    void * Checkpointer::partialRestart( const string & varName, unsigned int bytes ) {

      Context * context = chkpt->getContext();
      if( context == 0 ) {
        return 0;
      }

      Register * r = context->getRegister( varName );
      if( r == 0 ) {
        // Register not found in this context
        return 0;
      }

      context->removeRegister( varName );

      MemoryBlock::MemoryType bytesOrig =
        reinterpret_cast<MemoryBlock::MemoryType>( r->endAddress() ) -
        reinterpret_cast<MemoryBlock::MemoryType>( r->initAddress() );

      assert( bytes == bytesOrig );

      return r->initAddress();
    }

    bool Checkpointer::moreRestart( CheckpointCode & code ) {
      return( !chkpt->getContext()->getCheckpointHere() ||
        (code.getStaticValue() != chkpt->getCheckpointCode().getStaticValue()) );
    }

    void *thread_run( void *e_checkpointer ) {

      Checkpointer * checkpointer = static_cast<Checkpointer *>(
        e_checkpointer );

      Checkpoint * checkpoint = checkpointer->chkpt;

      Writer * w = WriterFactory::instance().getWriter(
        checkpoint->getWriterType() );

      w->write( checkpoint );

      checkpoint->depopulate();
      delete w;
      delete checkpointer;

      pthread_exit( NULL );
      return 0;
    }

    void Checkpointer::Shutdown() {
      if (last_thread != NULL ) {
        pthread_join( *last_thread, NULL );
      }
    }
  }
}
