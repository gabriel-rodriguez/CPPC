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



#if ! defined CPPC_WRITER_IOHDF5WRITER_H
#define CPPC_WRITER_IOHDF5WRITER_H

#include <checkpointer/checkpoint.h>
#include <compressor/compressor.h>
#include <data/call_image.h>
#include <data/context.h>
#include <data/heap_context.h>
#include <data/loop_context.h>
#include <data/cppc_file.h>
#include <data/memory_block.h>
#include <data/register.h>
#include <writer/buffer.h>
#include <writer/writer.h>

#include <cstddef>

#include <hdf5.h>

using cppc::data::CallImage;
using cppc::data::Context;
using cppc::data::HeapContext;
using cppc::data::LoopContext;
using cppc::data::CppcFile;
using cppc::data::MemoryBlock;
using cppc::data::Register;

namespace cppc {
  namespace writer {

    using cppc::checkpointer::BasicCheckpointCode;
    using cppc::compressor::BasicCompressorType;

    class IOHdf5Writer:public Writer {

      typedef struct {
        BasicCompressorType compressorType;
        BasicCheckpointCode checkpointCode;
        MemoryBlock::MemoryType fragmentSize; // Only set to a natural number if this is a full checkpoint
      } HeaderType;

      public:
        IOHdf5Writer();
        virtual ~IOHdf5Writer();

        virtual void write( cppc::checkpointer::Checkpoint * );
        virtual cppc::checkpointer::Checkpoint * readCheckpoint( string );
        virtual bool testCheckpoint( string );

        static inline WriterType staticWriterType() { return writerType; }
        virtual inline WriterType getWriterType() { return staticWriterType(); }

      private:
        IOHdf5Writer( const IOHdf5Writer & );
        virtual IOHdf5Writer & operator=( const IOHdf5Writer & );

        void startCheckpoint( string );
        void commitCheckpoint( cppc::checkpointer::Checkpoint * );
        void write( CallImage *, hid_t );
        void write( CppcFile *, hid_t );
        void write( Context *, hid_t );
        void write( HeapContext *, hid_t );
        void write( LoopContext *, hid_t );
        void write( MemoryBlock *, hid_t );
        void write( Register *, hid_t );
        void writeHeader( cppc::checkpointer::Checkpoint * );

        static Context * readHeapContext( hid_t, const char * );
        static Context * readLoopContext( hid_t, const char * );

        static string getLoopContextGroupName( LoopContext * );
        static string getHeapContextGroupName( HeapContext * );
        static hid_t getHdf5TypeIdentifier( DataType::DataTypeIdentifier );
        static hid_t createMemoryHeaderDataType();
        static hid_t file_id;
        static hid_t fcpl;
        static hid_t datasetProperties;
        static hsize_t compressionChunk;

        static string file_path;

        const static unsigned int
          FILE_CREATION_PROPERTY_LIST_USER_BLOCK_PARAMETER;

        const static string BLOCKCODE_ATTRIBUTE_NAME;
        const static string BLOCKSIZE_ATTRIBUTE_NAME;
        const static string BLOCKTYPE_ATTRIBUTE_NAME;
        const static string CALLEDFROMLINE_ATTRIBUTE_NAME;
        const static string CHECKPOINTCODE_ATTRIBUTE_NAME;
        const static string CHECKPOINTHERE_ATTRIBUTE_NAME;
        const static string COMPRESSORTYPE_ATTRIBUTE_NAME;
        const static string CONTEXTTYPE_ATTRIBUTE_NAME;
        const static string DESCRIPTORTYPE_ATTRIBUTE_NAME;
        const static string ENDOFFSET_ATTRIBUTE_NAME;
        const static string FILECODE_ATTRIBUTE_NAME;
        const static string FILENAME_ATTRIBUTE_NAME;
        const static string FILEOFFSET_ATTRIBUTE_NAME;
        const static string FRAGMENTSIZE_ATTRIBUTE_NAME;
        const static string FUNCTIONNAME_ATTRIBUTE_NAME;
        const static string INITOFFSET_ATTRIBUTE_NAME;
        const static string LOOPVARNAME_ATTRIBUTE_NAME;
        const static string VARVALUE_ATTRIBUTE_NAME;

        const static string CONTENT_OBJECT_NAME;
        const static string HEADER_DATASET_NAME;

        const static hid_t MEMORY_HEADER_DATATYPE;

        const static string CALLIMAGES_GROUP_NAME;
        const static string CONTEXT_GROUP_NAME;
        const static string FILEMAP_GROUP_NAME;
        const static string MEMBLOCKS_GROUP_NAME;
        const static string REGISTERS_GROUP_NAME;
        const static string SUBCONTEXTS_GROUP_NAME;

        const static unsigned char HEAP_CONTEXT_CODE;
        const static unsigned char LOOP_CONTEXT_CODE;

        const static WriterType writerType;

        friend herr_t readBlock( hid_t, const char *, const H5L_info_t *, void * );
        friend herr_t readCallImage( hid_t, const char *, const H5L_info_t *, void * );
        friend herr_t readContext( hid_t, const char *, const H5L_info_t *, void * );
        friend herr_t readFile( hid_t, const char *, const H5L_info_t *, void * );
        friend herr_t readRegister( hid_t, const char *, const H5L_info_t *, void * );
        friend herr_t readBlockFragment( hid_t, const char *, const H5L_info_t *, void * );
    };

    // Explicit prototype declaration needed since GCC v4.1.1
    herr_t readBlock( hid_t, const char *, const H5L_info_t *, void * );
    herr_t readCallImage( hid_t, const char *, const H5L_info_t *, void * );
    herr_t readContext( hid_t, const char *, const H5L_info_t *, void * );
    herr_t readFile( hid_t, const char *, const H5L_info_t *, void * );
    herr_t readRegister( hid_t, const char *, const H5L_info_t *, void * );
    herr_t readBlockFragment( hid_t, const char *, const H5L_info_t *, void * );
    typedef struct {
      vector<MemoryBlock::BlockFragment> * fragments;
      hid_t memoryType;
      hsize_t typeSize;
    } ReadBlockFragmentParams;
  }
}


#endif
