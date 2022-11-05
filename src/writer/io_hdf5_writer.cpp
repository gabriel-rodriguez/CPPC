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



#include <data/cppc_basic.h>
#include <checkpointer/checkpoint.h>
#include <data/data_type_factory.h>
#include <writer/io_hdf5_writer.h>
#include <writer/writer_factory.h>
#include <util/configuration/configuration_manager.h>
#include <H5Zpublic.h>

#include <cassert>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <sstream>

#include <openssl/sha.h>

using std::fstream;
using std::ifstream;
using std::ostringstream;

using cppc::checkpointer::Checkpoint;
using cppc::checkpointer::CheckpointCode;
using cppc::data::DataTypeFactory;
using cppc::data::IntegerType;
using cppc::data::RegisterVector;
using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterValueType;

namespace cppc {
  namespace writer {

    const unsigned int
      IOHdf5Writer::FILE_CREATION_PROPERTY_LIST_USER_BLOCK_PARAMETER( 512 );

    const string IOHdf5Writer::BLOCKCODE_ATTRIBUTE_NAME( "BlockCode" );
    const string IOHdf5Writer::BLOCKSIZE_ATTRIBUTE_NAME( "BlockSize" );
    const string IOHdf5Writer::BLOCKTYPE_ATTRIBUTE_NAME( "BlockType" );
    const string IOHdf5Writer::CALLEDFROMLINE_ATTRIBUTE_NAME(
      "CalledFromLine" );
    const string IOHdf5Writer::CHECKPOINTCODE_ATTRIBUTE_NAME(
      "CheckpointCode" );
    const string IOHdf5Writer::CHECKPOINTHERE_ATTRIBUTE_NAME(
      "CheckpointHere" );
    const string IOHdf5Writer::COMPRESSORTYPE_ATTRIBUTE_NAME(
      "CompressorType" );
    const string IOHdf5Writer::CONTEXTTYPE_ATTRIBUTE_NAME( "ContextType" );
    const string IOHdf5Writer::DESCRIPTORTYPE_ATTRIBUTE_NAME(
      "DescriptorType" );
    const string IOHdf5Writer::ENDOFFSET_ATTRIBUTE_NAME( "EndOffset" );
    const string IOHdf5Writer::FILECODE_ATTRIBUTE_NAME( "FileCode" );
    const string IOHdf5Writer::FILENAME_ATTRIBUTE_NAME( "FileName" );
    const string IOHdf5Writer::FILEOFFSET_ATTRIBUTE_NAME( "FileOffset" );
    const string IOHdf5Writer::FRAGMENTSIZE_ATTRIBUTE_NAME( "FragmentSize" );
    const string IOHdf5Writer::FUNCTIONNAME_ATTRIBUTE_NAME( "FunctionName" );
    const string IOHdf5Writer::INITOFFSET_ATTRIBUTE_NAME( "InitOffset" );
    const string IOHdf5Writer::LOOPVARNAME_ATTRIBUTE_NAME( "LoopVarName" );
    const string IOHdf5Writer::VARVALUE_ATTRIBUTE_NAME( "LoopVarValue" );

    const string IOHdf5Writer::CONTENT_OBJECT_NAME( "Content" );
    const string IOHdf5Writer::HEADER_DATASET_NAME( "/Header" );

    const hid_t IOHdf5Writer::MEMORY_HEADER_DATATYPE(
      IOHdf5Writer::createMemoryHeaderDataType() );

    const string IOHdf5Writer::CALLIMAGES_GROUP_NAME( "CallImages" );
    const string IOHdf5Writer::CONTEXT_GROUP_NAME( "Context" );
    const string IOHdf5Writer::FILEMAP_GROUP_NAME( "FileMap" );
    const string IOHdf5Writer::MEMBLOCKS_GROUP_NAME( "MemBlocks" );
    const string IOHdf5Writer::REGISTERS_GROUP_NAME( "Registers" );
    const string IOHdf5Writer::SUBCONTEXTS_GROUP_NAME( "Subcontexts" );

    const unsigned char IOHdf5Writer::HEAP_CONTEXT_CODE( 0 );
    const unsigned char IOHdf5Writer::LOOP_CONTEXT_CODE( 1 );

    hid_t IOHdf5Writer::file_id( -1 );
    hid_t IOHdf5Writer::fcpl( -1 );
    hid_t IOHdf5Writer::datasetProperties( 0 );
    hsize_t IOHdf5Writer::compressionChunk( 0 );
    string IOHdf5Writer::file_path( "" );
    const WriterType IOHdf5Writer::writerType( 3 );

    IOHdf5Writer::IOHdf5Writer()
      : Writer() {

      // Turn error auto-write off
      H5Eset_auto( H5E_DEFAULT, 0, 0 );

      // Create the dataset properties (for MemoryBlock data)
      datasetProperties = H5Pcreate( H5P_DATASET_CREATE );

      // Find out the compression level to use
      const ParameterValueType * const compressor =
        ConfigurationManager::instance().getParameter(
          ParameterKeyType( "CPPC/Writer/HDF-5/Compression" ) );
      if( (*compressor) == ParameterValueType( "ZLib" ) ) {
        const ParameterValueType * const chunkS =
          ConfigurationManager::instance().getParameter(
            ParameterKeyType( "CPPC/Writer/HDF-5/MinimumSize" ) );
        const ParameterValueType * const levelS =
          ConfigurationManager::instance().getParameter(
            ParameterKeyType( "CPPC/Writer/HDF-5/CompressionLevel" ) );
        compressionChunk = std::atoi( chunkS->c_str() );
        int level = std::atoi( levelS->c_str() );
        H5Pset_chunk( datasetProperties, 1, &compressionChunk );
        H5Pset_deflate( datasetProperties, level );
      }
    }

    IOHdf5Writer::IOHdf5Writer( const IOHdf5Writer & c_hdf5 )
      : Writer( c_hdf5 ) {}

    IOHdf5Writer & IOHdf5Writer::operator=( const IOHdf5Writer & rhs ) {
      return *this;
    }

    IOHdf5Writer::~IOHdf5Writer() {}

    void IOHdf5Writer::write( Checkpoint * c ) {

      this->startCheckpoint( c->getPath() );
      this->writeHeader( c );

      // Create file map group
      hid_t filemapGroup = H5Gcreate( file_id, FILEMAP_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      assert( filemapGroup != -1 );

      // Write file map to disk
      FileMap * files = c->getFiles();
      for( FileMap::iterator fileIt = files->begin();
        fileIt != files->end();
        fileIt++ ) {

        this->write( fileIt->second, filemapGroup );
      }
      H5Gclose( filemapGroup );

      // Create contexts group and write contexts to disk
      hid_t contextGroup = H5Gcreate( file_id, CONTEXT_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      this->write( c->getContext(), contextGroup );
      H5Gclose( contextGroup );

      // Create memoryblocks group
      hid_t memBlocksGroup = H5Gcreate( file_id, MEMBLOCKS_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      assert( memBlocksGroup != -1 );

      // Write memory blocks to disk
      BlockMap * memBlocks = c->getMemBlocks();
      for( BlockMap::iterator blockIt = memBlocks->begin();
        blockIt != memBlocks->end();
        blockIt++ ) {

        this->write( blockIt->second, memBlocksGroup );
      }
      H5Gclose( memBlocksGroup );

      this->commitCheckpoint( c );
    }

    void IOHdf5Writer::write( Context * context, hid_t parentGroup ) {

      hid_t contextGroup = 0;

      // Write specific data for context subclasses
      LoopContext * lcontext;
      if( (lcontext = dynamic_cast<LoopContext *>( context )) ) {
        contextGroup = H5Gcreate( parentGroup, this->getLoopContextGroupName( lcontext ).c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
        this->write( lcontext, contextGroup );
      }

      HeapContext * hcontext;
      if( (hcontext = dynamic_cast<HeapContext *>( context )) ) {
        contextGroup = H5Gcreate( parentGroup, this->getHeapContextGroupName( hcontext ).c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
        this->write( hcontext, contextGroup );
      }

      // Common dataspace
      hsize_t one = 1;
      hid_t attributeDataSpace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataSpace != -1 );

      // Write call images
      if( context->getCallImages()->size() > 0 ) {
        hid_t callImagesGroup = H5Gcreate( contextGroup, CALLIMAGES_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
        for( vector<CallImage *>::iterator it =
          context->getCallImages()->begin();
          it != context->getCallImages()->end(); it++ ) {

          this->write( *it, callImagesGroup );
        }
        H5Gclose( callImagesGroup );
      }

      // Write subcontexts
      if( context->getSubcontexts()->size() > 0 ) {
        hid_t subcontextsGroup = H5Gcreate( contextGroup, SUBCONTEXTS_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
        for( vector<Context *>::iterator it =
          context->getSubcontexts()->begin();
          it != context->getSubcontexts()->end(); it++ ) {

          this->write( *it, subcontextsGroup );
        }

        H5Gclose( subcontextsGroup );
      }

      H5Sclose( attributeDataSpace );
      H5Gclose( contextGroup );
    }

    void IOHdf5Writer::write( CallImage * callImage, hid_t callImagesGroup ) {

      // Get the name for this call image group
      unsigned int calledFromLine = callImage->getCalledFromLine();
      ostringstream callImageGroupName;
      callImageGroupName << calledFromLine;

      // Create a group for this call image
      hid_t callImageGroup = H5Gcreate( callImagesGroup, callImageGroupName.str().c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );

      // Create dataspace for function name
      hsize_t nameSize = callImage->getFunctionName().size() + 1;
      hid_t nameDataspace = H5Screate_simple( 1, &nameSize, 0 );
      assert( nameDataspace != -1 );

      // Write function name
      hid_t functionNameAttribute = H5Acreate( callImageGroup, FUNCTIONNAME_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_CHAR, nameDataspace,
                                               H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( functionNameAttribute, H5T_NATIVE_CHAR,
        callImage->getFunctionName().c_str() ) >= 0 );
      H5Aclose( functionNameAttribute );
      H5Sclose( nameDataspace );

      // Write function parameters
      hid_t parametersGroup = H5Gcreate( callImageGroup, REGISTERS_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      for( vector<Register *>::iterator paramIt =
        callImage->getParameters()->begin();
        paramIt != callImage->getParameters()->end(); paramIt++ ) {

        this->write( *paramIt, parametersGroup );
      }
      H5Gclose( parametersGroup );

      // Write memory blocks
      hid_t memBlocksGroup = H5Gcreate( callImageGroup, MEMBLOCKS_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      assert( memBlocksGroup != -1 );
      for( BlockMap::iterator blockIt = callImage->getBlocks()->begin();
        blockIt != callImage->getBlocks()->end(); blockIt++ ) {

        this->write( blockIt->second, memBlocksGroup );
      }
      H5Gclose( memBlocksGroup );
      H5Gclose( callImageGroup );
    }

    void IOHdf5Writer::write( HeapContext * context, hid_t contextGroup ) {

      // Create dataspace
      hsize_t one = 1;
      hid_t attributeDataSpace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataSpace != -1 );

      // Write line called from
      unsigned int calledFromLine = context->getCalledFromLine();
      hid_t calledFromLineAttribute = H5Acreate( contextGroup, CALLEDFROMLINE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UINT,
                                                 attributeDataSpace, H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( calledFromLineAttribute, H5T_NATIVE_UINT,
              &calledFromLine ) >= 0 );
      H5Aclose( calledFromLineAttribute );

      // If this is the context from where the checkpoint was called,
      // write attribute for marking
      if( context->getCheckpointHere() ) {
        unsigned char one = 1;
        hid_t checkpointHereAttribute = H5Acreate( contextGroup, CHECKPOINTHERE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UCHAR,
                                                   attributeDataSpace, H5P_DEFAULT, H5P_DEFAULT );
        assert( H5Awrite( checkpointHereAttribute, H5T_NATIVE_UCHAR,
                &one ) >= 0 );
        H5Aclose( checkpointHereAttribute );
      }

      // Write registers to their own subgroup
      if( context->getRegisters()->size() > 0 ) {
        hid_t registersGroup = H5Gcreate( contextGroup, REGISTERS_GROUP_NAME.c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
        RegisterVector * registers = context->getRegisters();
        for( RegisterVector::iterator it = registers->begin();
          it != registers->end(); it ++ ) {

          this->write( *it, registersGroup );
        }
        H5Gclose( registersGroup );
      }

      // Write context type
      hid_t contextTypeAttribute = H5Acreate( contextGroup, CONTEXTTYPE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UCHAR,
                                              attributeDataSpace, H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( contextTypeAttribute, H5T_NATIVE_UCHAR,
        &HEAP_CONTEXT_CODE ) >= 0 );
      H5Aclose( contextTypeAttribute );

      H5Sclose( attributeDataSpace );
    }

    void IOHdf5Writer::write( LoopContext * context, hid_t contextGroup ) {

      // Create dataspace for iteration variable name
      hsize_t nameSize = context->getVarName().size() + 1;
      hid_t nameDataspace = H5Screate_simple( 1, &nameSize, 0 );
      assert( nameDataspace != -1 );

      // Write function name
      hid_t nameAttribute = H5Acreate( contextGroup, FUNCTIONNAME_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_CHAR, nameDataspace,
                                       H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( nameAttribute, H5T_NATIVE_CHAR,
        context->getVarName().c_str() ) >= 0 );
      H5Aclose( nameAttribute );
      H5Sclose( nameDataspace );

      // Create value dataspace
      hsize_t one = 1;
      hid_t attributeDataspace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataspace != -1 );

      // Write value type
      DataType::DataTypeIdentifier cppcType =
        context->getVarValue()->getIdentifier();
      hid_t valueTypeAttribute = H5Acreate( contextGroup, BLOCKTYPE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UCHAR, attributeDataspace,
                                            H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( valueTypeAttribute, H5T_NATIVE_UCHAR,
        &cppcType ) >= 0 );
      H5Aclose( valueTypeAttribute );

      // Write value
      hid_t hdf5Type = getHdf5TypeIdentifier(
        context->getVarValue()->getIdentifier() );
      hid_t valueAttribute = H5Acreate( contextGroup, VARVALUE_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataspace,
                                        H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( valueAttribute, hdf5Type,
        context->getVarValue()->getValue() ) >= 0 );
      H5Aclose( valueAttribute );

      // Write context type
      hid_t contextTypeAttribute = H5Acreate( contextGroup, CONTEXTTYPE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UCHAR,
        attributeDataspace, H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( contextTypeAttribute, H5T_NATIVE_UCHAR,
        &LOOP_CONTEXT_CODE ) >= 0 );
      H5Aclose( contextTypeAttribute );

      H5Sclose( attributeDataspace );
    }

    void IOHdf5Writer::write( MemoryBlock * b, hid_t memBlocksGroup ) {

      // Get the name for this memory block's group
      MemoryBlock::BlockCode blockCode = b->getCode();
      ostringstream blockGroupName;
      blockGroupName << blockCode;

      // Create group for this block
      hid_t blockGroup = H5Gcreate( memBlocksGroup, blockGroupName.str().c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );

      // Create attribute dataspace
      hsize_t one = 1;
      hid_t attributeDataSpace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataSpace != -1 );

      // Write block type
      hid_t blockTypeAttribute = H5Acreate( blockGroup, BLOCKTYPE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_UCHAR,
                                            attributeDataSpace, H5P_DEFAULT, H5P_DEFAULT );
      DataType::DataTypeIdentifier type = b->getType();
      assert( H5Awrite( blockTypeAttribute, H5T_NATIVE_UCHAR, &type ) >= 0 );
      H5Aclose( blockTypeAttribute );

      H5Sclose( attributeDataSpace );

      hid_t hdf5Type = getHdf5TypeIdentifier( b->getType() );
      DataType::DataTypeSize typeSize = DataTypeFactory::instance(). getDataTypeSize( b->getType() );
      // If this is a regular block, store in full
      if( b->getFullStore() && b->getFragments().empty() ) {
        // Create dataspace for data writing
        hsize_t elements = (b->getEndAddr() - b->getInitAddr()) / typeSize;
        hid_t contentDataSpace = H5Screate_simple( 1, &elements, 0 );
        assert( contentDataSpace != -1 );

        // Create dataset with ZLib compression and write block data
        hid_t localProperties = H5P_DEFAULT;
        if( ( compressionChunk > 0 ) && ( elements >= compressionChunk ) ) {
          localProperties = datasetProperties;
        }

        hid_t contentDataSet = H5Dcreate( blockGroup, CONTENT_OBJECT_NAME.c_str(), hdf5Type, contentDataSpace,
                                          H5P_DEFAULT, localProperties, H5P_DEFAULT );
        MemoryBlock::MemoryType memInit = b->getInitAddr();
        assert( H5Dwrite(contentDataSet, hdf5Type, contentDataSpace, H5S_ALL, H5P_DEFAULT, reinterpret_cast<void *>(memInit)) >= 0 );
        H5Dclose( contentDataSet );
        H5Sclose( contentDataSpace );
      } else if( !b->getFragments().empty() ) { // If no fragments, just write nothing
        // Create "./Content" not as a dataset, but as a group.
        // Order links inside the group in creation order. This may improve cache behavior.
        hid_t gcpl = H5Pcreate( H5P_GROUP_CREATE );
        H5Pset_link_creation_order( gcpl, H5P_CRT_ORDER_TRACKED || H5P_CRT_ORDER_INDEXED );
        hid_t contentGroup = H5Gcreate( blockGroup, CONTENT_OBJECT_NAME.c_str(), H5P_DEFAULT, gcpl, H5P_DEFAULT );

        // Create a dataset for each fragment. The dataspace should be common to most of the fragments. If the fragment
        // contains zeros, just create an empty dataset. Upon reading, when finding an empty dataspace the zeros have
        // to be reconstructed. Note that it is guaranteed that all zero-fragments will have MemoryBlock::fragmentSize
        // elements, because zero-blocks are detected by comparing against the SHA1 of a block of zeros this size.
        assert( MemoryBlock::fragmentSize % typeSize == 0 );
        hsize_t elements = MemoryBlock::fragmentSize / typeSize;
        hid_t contentDataspace = H5Screate_simple( 1, &elements, 0 );

        vector<MemoryBlock::BlockFragment> & fragments = b->getFragments();
        for( vector<MemoryBlock::BlockFragment>::iterator i = fragments.begin(); i != fragments.end(); ++i ) {
          assert( i->fragmentSize % typeSize == 0 );
          ostringstream fragmentStream;
          fragmentStream << i->fragmentCode;
          MemoryBlock::MemoryType memInit;
          hid_t datatype = hdf5Type;
          if( i->fragmentAddress ) {
            elements = i->fragmentSize / typeSize;
            memInit = b->getInitAddr() + i->fragmentCode * MemoryBlock::fragmentSize;
          } else {
            elements = 1;
            fragmentStream << "Z"; // Mark zero-blocks with a "Z" at the end of the dataset name
            datatype = H5T_NATIVE_UINT;
          }
          H5Sset_extent_simple( contentDataspace, 1, &elements, 0 );
          assert( contentDataspace != -1 );
          hid_t localProperties = H5P_DEFAULT;

          if( ( compressionChunk > 0 ) && ( elements >= compressionChunk ) ) {
            localProperties = datasetProperties;
          }

          hid_t contentDataset = H5Dcreate( contentGroup, fragmentStream.str().c_str(), datatype, contentDataspace,
                                            H5P_DEFAULT, localProperties, H5P_DEFAULT );
          if( i->fragmentAddress ) {
            assert( H5Dwrite( contentDataset, hdf5Type, contentDataspace, H5S_ALL, H5P_DEFAULT, reinterpret_cast<void*>(memInit) ) >= 0 );
          } else {
            unsigned zero_elems = i->fragmentSize / typeSize;
            assert( H5Dwrite( contentDataset, H5T_NATIVE_UINT, contentDataspace, H5S_ALL, H5P_DEFAULT, &zero_elems ) >= 0 );
          }
          H5Dclose( contentDataset );
        }

        H5Gclose( contentGroup );
        H5Sclose( contentDataspace );
      }

      H5Gclose( blockGroup );
    }

    void IOHdf5Writer::write( CppcFile * f, hid_t filemapGroup ) {

      // Get the name for this file's group
      CppcFile::FileCode fileCode = f->get_file_code();
      ostringstream fileGroupName;
      fileGroupName << (unsigned)fileCode;

      // Create group for this file
      hid_t fileGroup = H5Gcreate( filemapGroup, fileGroupName.str().c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      assert( fileGroup >= 0 );

      // Create attribute dataspace
      hsize_t one = 1;
      hid_t attributeDataSpace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataSpace != -1 );

      // Write FileCode
      hid_t hdf5Type = getHdf5TypeIdentifier(
        CppcFile::FileCodeType::typeIdentifier );
      hid_t fileCodeAttribute = H5Acreate( fileGroup, FILECODE_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
                                           H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( fileCodeAttribute, hdf5Type, &fileCode ) >= 0 );
      H5Aclose( fileCodeAttribute );

      // Write FileOffset
      hdf5Type = getHdf5TypeIdentifier(
        CppcFile::FileOffsetType::typeIdentifier );
      CppcFile::FileOffset fileOffset = f->get_file_offset();
      hid_t fileOffsetAttribute = H5Acreate( fileGroup, FILEOFFSET_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
                                             H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( fileOffsetAttribute, hdf5Type, &fileOffset ) >= 0 );
      H5Aclose( fileOffsetAttribute );

      // Write Descriptor type
      CppcFile::DescriptorType dtype = f->get_descriptor_type();
      hdf5Type = getHdf5TypeIdentifier(
        cppc::data::getBasicTypeCode<CppcFile::DescriptorType>() );
      hid_t descriptorTypeAttribute = H5Acreate( fileGroup, DESCRIPTORTYPE_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
                                                 H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( descriptorTypeAttribute, hdf5Type, &dtype ) >= 0 );
      H5Aclose( descriptorTypeAttribute );

      //Close attribute dataspace
      H5Sclose( attributeDataSpace );

      // Create dataspace for file path
      hsize_t pathSize = f->get_file_path().size() + 1;
      hid_t pathDataSpace = H5Screate_simple( 1, &pathSize, 0 );
      assert( pathDataSpace != -1 );

      // Write file path
      hid_t fileNameAttribute = H5Acreate( fileGroup, FILENAME_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_CHAR, pathDataSpace,
                                           H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( fileNameAttribute, H5T_NATIVE_CHAR,
        f->get_file_path().c_str() ) >= 0 );
      H5Aclose( fileNameAttribute );

      H5Sclose( pathDataSpace );

      H5Gclose( fileGroup );
    }

    void IOHdf5Writer::write( Register * r, hid_t registersGroup ) {

      // Create group for this register
      hid_t registerGroup = H5Gcreate( registersGroup, r->name().c_str(), H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );
      assert( registerGroup != -1 );

      // Create dataspace for attributes
      hsize_t one = 1;
      hid_t attributeDataSpace = H5Screate_simple( 1, &one, 0 );
      assert( attributeDataSpace != -1 );

      // Write memory block code
      MemoryBlock::BlockCode code = r->getCode();
      hid_t hdf5Type = getHdf5TypeIdentifier(
        MemoryBlock::BlockCodeType::typeIdentifier );
      hid_t codeAttribute = H5Acreate( registerGroup, BLOCKCODE_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
                                       H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( codeAttribute, hdf5Type, &code ) >= 0 );
      H5Aclose( codeAttribute );

      // Initial offset into the memory block: first registered element
      DataType::DataTypeSize typeSize =
        DataTypeFactory::instance().getDataTypeSize( r->type() );
      MemoryBlock::MemoryType initOffset =
        reinterpret_cast<MemoryBlock::MemoryType>( r->initAddress() );
      initOffset = initOffset / typeSize;
      hdf5Type = getHdf5TypeIdentifier(
        MemoryBlock::MemoryTypeType::typeIdentifier );
      hid_t initOffsetAttribute = H5Acreate( registerGroup, INITOFFSET_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
                                             H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( initOffsetAttribute, hdf5Type, &initOffset ) >= 0 );
      H5Aclose( initOffsetAttribute );

      // Final offset into the memory block: last registered element
      MemoryBlock::MemoryType endOffset =
        reinterpret_cast<MemoryBlock::MemoryType>( r->endAddress() );
      endOffset = endOffset / typeSize;
      hid_t endOffsetAttribute = H5Acreate( registerGroup, ENDOFFSET_ATTRIBUTE_NAME.c_str(), hdf5Type, attributeDataSpace,
        H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( endOffsetAttribute, hdf5Type, &endOffset ) >= 0 );
      H5Aclose( endOffsetAttribute );

      // Type of this register
      CPPC_Datatype type = r->type();
      hid_t typeAttribute = H5Acreate( registerGroup, BLOCKTYPE_ATTRIBUTE_NAME.c_str(), H5T_NATIVE_INT,
                                       attributeDataSpace, H5P_DEFAULT, H5P_DEFAULT );
      assert( H5Awrite( typeAttribute, H5T_NATIVE_INT, &type ) >= 0 );
      H5Aclose( typeAttribute );

      H5Sclose( attributeDataSpace );
      H5Gclose( registerGroup );
    }

    void IOHdf5Writer::writeHeader( Checkpoint * c ) {
      assert( file_id != -1 );

      // Dataspace creation
      hsize_t one = 1;
      hid_t headerDataSpace = H5Screate_simple( 1, &one, 0 );

      // Dataset creation
      hid_t headerDataSet = H5Dcreate( file_id, HEADER_DATASET_NAME.c_str(), MEMORY_HEADER_DATATYPE, headerDataSpace,
                                       H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT );

      // Object to write
      HeaderType h;
      h.compressorType = c->getCompressorType().getStaticValue();
      h.checkpointCode = c->getCheckpointCode().getStaticValue();
      h.fragmentSize = MemoryBlock::fragmentSize;

      H5Dwrite( headerDataSet, MEMORY_HEADER_DATATYPE, headerDataSpace, H5S_ALL,
        H5P_DEFAULT, &h );

      H5Dclose( headerDataSet );
      H5Sclose( headerDataSpace );
    }

    Checkpoint * IOHdf5Writer::readCheckpoint( string path ) {
      // Read whether checkpoint is full (second byte)
      char full;
      ifstream file( path.c_str(), std::ios::in );
      assert( file.is_open() );
      file.seekg( 1 );
      file.read( &full, 1 );

      // Open file
      file_id = H5Fopen( path.c_str(), H5F_ACC_RDONLY, H5P_DEFAULT );
      assert( file_id != -1 );

      // Open header dataset
      hid_t headerDataSet = H5Dopen( file_id, HEADER_DATASET_NAME.c_str(), H5P_DEFAULT );
      assert( headerDataSet != -1 );

      // Create header dataspace
      hsize_t one = 1;
      hid_t headerDataSpace = H5Screate_simple( 1, &one, 0 );

      // Read header
      HeaderType h;
      assert( H5Dread( headerDataSet, MEMORY_HEADER_DATATYPE, headerDataSpace,
        H5S_ALL, H5P_DEFAULT, &h ) >= 0 );
      H5Sclose( headerDataSpace );
      H5Dclose( headerDataSet );

      Checkpoint * c = new Checkpoint( h.fragmentSize );
      c->setWriterType( staticWriterType() );
      c->setCompressorType( CompressorType( h.compressorType ) );
      c->setCheckpointCode( CheckpointCode( h.checkpointCode ) );
      c->setPath( path );
      c->setFullCheckpoint( full );

      // Read filemap iterating over the group
      FileMap * filemap = new FileMap();
      hid_t filemapGroup = H5Gopen( file_id, FILEMAP_GROUP_NAME.c_str(), H5P_DEFAULT );
      hsize_t iterationIndex = 0;
      while( H5Literate( filemapGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readFile, filemap ) );
      c->setFiles( filemap );

      // Read contexts recursively over the hierarchy
      Context * context;
      context = 0;
      hid_t contextGroup = H5Gopen( file_id, CONTEXT_GROUP_NAME.c_str(), H5P_DEFAULT );
      iterationIndex = 0;
      while( H5Literate( contextGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readContext, &context ) != 0 );
      c->setContext( context );

      // Read memory blocks iterating over the group
      BlockMap * memBlocks = new BlockMap();
      hid_t memblocksGroup = H5Gopen( file_id, MEMBLOCKS_GROUP_NAME.c_str(), H5P_DEFAULT );
      iterationIndex = 0;
      while( H5Literate( memblocksGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readBlock, memBlocks ) != 0 );

      c->setMemBlocks( memBlocks );

      H5Fclose( file_id );

      return c;
    }

    herr_t readBlock( hid_t blocksGroup, const char * memberName, const H5L_info_t * info,  void * operator_data ) {
      // Convert the operator data to our blockmap
      BlockMap * memBlocks = static_cast<BlockMap *>( operator_data );

      // Open group
      hid_t blockGroup = H5Gopen( blocksGroup, memberName, H5P_DEFAULT );

      // Extract block code from group name
      MemoryBlock::BlockCode blockCode = std::atoi( memberName );

      // Read block type
      CPPC_Datatype blockType;
      hid_t hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        IntegerType::typeIdentifier );
      hid_t blockTypeAttribute = H5Aopen_name( blockGroup,
        IOHdf5Writer::BLOCKTYPE_ATTRIBUTE_NAME.c_str() );
      assert( blockTypeAttribute != -1 );
      assert( H5Aread( blockTypeAttribute, hdf5Type, &blockType ) >= 0 );
      H5Aclose( blockTypeAttribute );

      // Read content
      hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier( blockType );
      hsize_t typeSize = DataTypeFactory::instance().getDataTypeSize( blockType );
      hid_t contentObject = H5Oopen( blockGroup, IOHdf5Writer::CONTENT_OBJECT_NAME.c_str(), H5P_DEFAULT );

      // If no content, then this is a fragmented block with no new fragments
      if( contentObject < 0 ) {
        vector<MemoryBlock::BlockFragment> fragments;
        MemoryBlock * b = new MemoryBlock( blockCode, 0, 0, blockType );
        b->setFragments( fragments );
        memBlocks->insert( BlockMap::value_type( blockCode, b ) );
      } else {
        H5O_info_t contentInfo;
        H5Oget_info( contentObject, &contentInfo );
        if( contentInfo.type == H5O_TYPE_DATASET ) {
          // Regular, non-fragmented memory block
          hid_t contentDataspace = H5Dget_space( contentObject );
          hsize_t dataSize;
          assert( H5Sget_simple_extent_dims( contentDataspace, &dataSize, 0 ) >= 0 );
          unsigned char * content = new unsigned char[ dataSize * typeSize ];
          assert( H5Dread( contentObject, hdf5Type, contentDataspace, H5S_ALL, H5P_DEFAULT, content ) >= 0 );
          H5Oclose( contentObject );
          H5Sclose( contentDataspace );

          // Create memory block and insert it into the blockmap
          MemoryBlock::MemoryType addr = reinterpret_cast<MemoryBlock::MemoryType>( content );
          MemoryBlock * b = new MemoryBlock( blockCode, addr, addr+dataSize*typeSize, blockType );
          memBlocks->insert( BlockMap::value_type( blockCode, b ) );
        } else {
          assert( contentInfo.type == H5O_TYPE_GROUP );
          H5G_info_t groupInfo;
          H5Gget_info( contentObject, &groupInfo );
          hsize_t iterationIndex = 0;
          vector<MemoryBlock::BlockFragment> fragments;
          ReadBlockFragmentParams params;
          params.fragments = &fragments;
          params.memoryType = hdf5Type;
          params.typeSize = typeSize;
          H5Literate( contentObject, H5_INDEX_CRT_ORDER, H5_ITER_NATIVE, &iterationIndex, readBlockFragment, &params );

          // Create memory block and insert it into the blockmap
          MemoryBlock * b = new MemoryBlock( blockCode, 0, 0, blockType );
          b->setFragments( fragments );
          memBlocks->insert( BlockMap::value_type( blockCode, b ) );
        }
      }

      H5Gclose( blockGroup );
      return 0;
    }

    herr_t readBlockFragment( hid_t group, const char * name, const H5L_info_t * info, void * op_data ) {
      hid_t fragmentDataset = H5Dopen( group, name, H5P_DEFAULT );
      hid_t fragmentDataspace = H5Dget_space( fragmentDataset );
      hsize_t dataSize;
      assert( H5Sget_simple_extent_dims( fragmentDataspace, &dataSize, 0 ) >= 0 );

      ReadBlockFragmentParams * params = reinterpret_cast<ReadBlockFragmentParams*>( op_data );
      MemoryBlock::BlockFragment fragment;
      fragment.fragmentSize = 0;
      fragment.fragmentCode = std::atoi( name );
      unsigned char * content = 0;
      if( name[std::strlen(name)-1] == 'Z' ) {
        // Recover zero-block
        assert( H5Dread( fragmentDataset, H5T_NATIVE_UINT, fragmentDataspace, H5S_ALL, H5P_DEFAULT, &fragment.fragmentSize ) >= 0 );
        fragment.fragmentSize *= params->typeSize;
        content = new unsigned char[ fragment.fragmentSize ](); // Zero-initialized
      } else {
        fragment.fragmentSize = dataSize * params->typeSize;
        content = new unsigned char[ dataSize * params->typeSize ];
        assert( H5Dread( fragmentDataset, params->memoryType, fragmentDataspace, H5S_ALL, H5P_DEFAULT, content ) >= 0 );
      }
      fragment.fragmentAddress = reinterpret_cast<MemoryBlock::MemoryType>( content );
      params->fragments->push_back( fragment );

      return 0;
    }

    herr_t readCallImage( hid_t callImagesGroup, const char * memberName, const H5L_info_t * info, void * operator_data ) {
      // Convert the operator data to the CallImage vector
      vector<CallImage *> * callImages = static_cast<vector<CallImage *> *>(
        operator_data );

      // Open group
      hid_t callImageGroup = H5Gopen( callImagesGroup, memberName, H5P_DEFAULT );

      // The called from line unsigned int is the group name
      unsigned int calledFromLine = std::atoi( memberName );

      // Read function name
      hid_t functionNameAttribute = H5Aopen_name( callImageGroup, IOHdf5Writer::FUNCTIONNAME_ATTRIBUTE_NAME.c_str() );
      hid_t functionNameDataspace = H5Aget_space( functionNameAttribute );
      hsize_t functionNameSize;
      assert( H5Sget_simple_extent_dims( functionNameDataspace,
        &functionNameSize, 0 ) >= 0 );
      char * functionName = new char[ functionNameSize ];
      assert( H5Aread( functionNameAttribute, H5T_NATIVE_CHAR, functionName )
        >= 0 );
      H5Aclose( functionNameAttribute );
      H5Sclose( functionNameDataspace );

      // Create call image
      CallImage * c = new CallImage( string( functionName ), calledFromLine );
      delete [] functionName;

      // Read function parameters
      hsize_t iterationIndex = 0;
      hid_t registersGroup = H5Gopen( callImageGroup, IOHdf5Writer::REGISTERS_GROUP_NAME.c_str(), H5P_DEFAULT );
      RegisterVector * registers = new RegisterVector();
      while( H5Literate( registersGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readRegister, registers ) );

      for( RegisterVector::iterator it = registers->begin();
        it != registers->end(); it++ ) {

        c->addParameter( *it );
      }
      delete registers;

      // Read memory blocks
      BlockMap * blocks = new BlockMap();
      iterationIndex = 0;
      hid_t memblocksGroup = H5Gopen( callImageGroup, IOHdf5Writer::MEMBLOCKS_GROUP_NAME.c_str(), H5P_DEFAULT );
      while( H5Literate( memblocksGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readBlock, blocks ) );
      c->setBlocks( blocks );

      callImages->push_back( c );

      H5Gclose( callImageGroup );
      return 0;
    }

    herr_t readContext( hid_t contextsGroup, const char * memberName, const H5L_info_t * info, void * operator_data ) {
      // Convert the operator data to the parent Context
      Context ** context = static_cast<Context **>( operator_data );

      // Open group
      hid_t contextGroup = H5Gopen( contextsGroup, memberName, H5P_DEFAULT );

      // Read context type code
      unsigned char contextTypeCode;
      hid_t contextTypeAttribute = H5Aopen_name( contextGroup, IOHdf5Writer::CONTEXTTYPE_ATTRIBUTE_NAME.c_str() );
      assert( contextTypeAttribute != -1 );
      assert( H5Aread( contextTypeAttribute, H5T_NATIVE_UCHAR,
        &contextTypeCode ) >= 0 );
      H5Aclose( contextTypeAttribute );

      Context * child;
      if( contextTypeCode == IOHdf5Writer::HEAP_CONTEXT_CODE ) {
        child = IOHdf5Writer::readHeapContext( contextGroup, memberName );
      }

      if( contextTypeCode == IOHdf5Writer::LOOP_CONTEXT_CODE ) {
        child = IOHdf5Writer::readLoopContext( contextGroup, memberName );
      }

      if( *context == 0 ) {
        *context = child;
      } else {
        (*context)->addSubcontext( child );
      }

      // Read call images
      vector<CallImage *> * callImages = new vector<CallImage *>();
      if( H5Gget_objinfo( contextGroup, IOHdf5Writer::CALLIMAGES_GROUP_NAME.c_str(), 0, 0 ) >= 0 ) {
        hsize_t iterationIndex = 0;
        hid_t callimagesGroup = H5Gopen( contextGroup, IOHdf5Writer::CALLIMAGES_GROUP_NAME.c_str(), H5P_DEFAULT );
        while( H5Literate( callimagesGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readCallImage, callImages ) );
      }
      child->setCallImages( callImages );

      // Read subcontexts
      if( H5Gget_objinfo( contextGroup, IOHdf5Writer::SUBCONTEXTS_GROUP_NAME.c_str(), 0, 0 ) >= 0 ) {
        hsize_t iterationIndex = 0;
        hid_t callImagesGroup = H5Gopen( contextGroup, IOHdf5Writer::SUBCONTEXTS_GROUP_NAME.c_str(), H5P_DEFAULT );
        while( H5Literate( callImagesGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readContext, &child ) );
      }

      H5Gclose( contextGroup );
      return 0;
    }

    Context * IOHdf5Writer::readHeapContext( hid_t contextGroup,
      const char * memberName ) {

      // Read line called from
      unsigned int calledFromLine;
      hid_t calledFromLineAttribute = H5Aopen_name( contextGroup,
        IOHdf5Writer::CALLEDFROMLINE_ATTRIBUTE_NAME.c_str() );
      assert( calledFromLineAttribute != -1 );
      assert( H5Aread( calledFromLineAttribute, H5T_NATIVE_UINT,
        &calledFromLine ) >= 0 );
      H5Aclose( calledFromLineAttribute );

      string functionName( memberName );
      functionName = functionName.substr( 0, functionName.find( " ", 0 ) );
      HeapContext * context = new HeapContext( 0, functionName,
        calledFromLine );

      // Check if "CheckpointHere" is a valid attribute inside this group
      hid_t checkpointHereAttribute = H5Aopen_name( contextGroup,
        IOHdf5Writer::CHECKPOINTHERE_ATTRIBUTE_NAME.c_str() );
      if( checkpointHereAttribute >= 0 ) {
        // No need to read anything: the attribute existence suffices
        context->setCheckpointHere( true );
        H5Aclose( checkpointHereAttribute );
      }

      // Read registers
      if( H5Gget_objinfo( contextGroup,
        IOHdf5Writer::REGISTERS_GROUP_NAME.c_str(), 0, 0 ) >= 0 ) {

        RegisterVector * registers = new RegisterVector();
        hsize_t iterationIndex = 0;
        hid_t registersGroup = H5Gopen( contextGroup, REGISTERS_GROUP_NAME.c_str(), H5P_DEFAULT );
        while( H5Literate( registersGroup, H5_INDEX_NAME, H5_ITER_NATIVE, &iterationIndex, readRegister, registers ) );

        context->setRegisters( registers );
      }

      return context;
    }

    Context * IOHdf5Writer::readLoopContext( hid_t contextGroup,
      const char * memberName ) {

      // Read variable name
      hid_t nameAttribute = H5Aopen_name( contextGroup,
        IOHdf5Writer::FUNCTIONNAME_ATTRIBUTE_NAME.c_str() );
      hid_t nameDataspace = H5Aget_space( nameAttribute );
      hsize_t nameSize;
      assert( H5Sget_simple_extent_dims( nameDataspace, &nameSize, 0 ) >= 0 );
      char * name = new char[ nameSize ];
      assert( H5Aread( nameAttribute, H5T_NATIVE_CHAR, name ) >= 0 );
      H5Aclose( nameAttribute );
      H5Sclose( nameDataspace );
      string varName( name );
      delete [] name;

      // Read value type
      DataType::DataTypeIdentifier valueType;
      hid_t valueTypeAttribute = H5Aopen_name( contextGroup,
        IOHdf5Writer::BLOCKTYPE_ATTRIBUTE_NAME.c_str() );
      assert( valueTypeAttribute != -1 );
      assert( H5Aread( valueTypeAttribute, H5T_NATIVE_UCHAR, &valueType ) >= 0 );
      H5Aclose( valueTypeAttribute );

      // Read value
      hid_t hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier( valueType );
      hsize_t typeSize = DataTypeFactory::instance().getDataTypeSize(
        valueType );
      unsigned char * value = new unsigned char[ typeSize ];
      hid_t valueAttribute = H5Aopen_name( contextGroup,
        IOHdf5Writer::VARVALUE_ATTRIBUTE_NAME.c_str() );
      assert( valueAttribute != -1 );
      assert( H5Aread( valueAttribute, hdf5Type, value ) >= 0 );
      H5Aclose( valueAttribute );

      DataType * valueDatatype = DataTypeFactory::instance().getDataType(
        valueType, value );

      return new LoopContext( 0, varName, valueDatatype );
    }

    herr_t readFile( hid_t filesGroup, const char * memberName, const H5L_info_t * info, void * operator_data ) {
      // Convert the operator data to our filemap
      FileMap * filemap = static_cast<FileMap *>( operator_data );

      // Open group
      hid_t fileGroup = H5Gopen( filesGroup, memberName, H5P_DEFAULT );

      // Read filecode
      CppcFile::FileCode fileCode;
      hid_t hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        CppcFile::FileCodeType::typeIdentifier );
      hid_t fileCodeAttribute = H5Aopen_name( fileGroup,
        IOHdf5Writer::FILECODE_ATTRIBUTE_NAME.c_str() );
      assert( fileCodeAttribute != -1 );
      assert( H5Aread( fileCodeAttribute, hdf5Type, &fileCode ) >= 0 );
      H5Aclose( fileCodeAttribute );

      // Read file offset
      CppcFile::FileOffset fileOffset;
      hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        CppcFile::FileOffsetType::typeIdentifier );
      hid_t fileOffsetAttribute = H5Aopen_name( fileGroup,
        IOHdf5Writer::FILEOFFSET_ATTRIBUTE_NAME.c_str() );
      assert( fileOffsetAttribute != -1 );
      assert( H5Aread( fileOffsetAttribute, hdf5Type, &fileOffset ) >= 0 );
      H5Aclose( fileOffsetAttribute );

      // Read descriptor type
      CppcFile::DescriptorType dtype;
      hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        cppc::data::getBasicTypeCode<CppcFile::DescriptorType>() );
      hid_t descriptorTypeAttribute = H5Aopen_name( fileGroup,
        IOHdf5Writer::DESCRIPTORTYPE_ATTRIBUTE_NAME.c_str() );
      assert( descriptorTypeAttribute != -1 );
      assert( H5Aread( descriptorTypeAttribute, hdf5Type, &dtype ) >= 0 );
      H5Aclose( descriptorTypeAttribute );

      // Read file path
      hid_t filePathAttribute = H5Aopen_name( fileGroup,
        IOHdf5Writer::FILENAME_ATTRIBUTE_NAME.c_str() );
      hid_t filePathDataspace = H5Aget_space( filePathAttribute );
      hsize_t filePathSize;
      assert( H5Sget_simple_extent_dims( filePathDataspace, &filePathSize, 0 )
        >= 0 );
      char * filePath = new char[filePathSize];
      assert( H5Aread( filePathAttribute, H5T_NATIVE_CHAR, filePath ) >= 0 );
      H5Aclose( filePathAttribute );
      H5Sclose( filePathDataspace );

      // Create CppcFile and insert into the filemap
      CppcFile * f = new CppcFile( fileCode, 0, dtype, fileOffset,
        filePath );
      filemap->insert( FileMap::value_type( fileCode, f ) );
      delete [] filePath;

      H5Gclose( fileGroup );
      return 0;
    }

    herr_t readRegister( hid_t registersGroup, const char * memberName, const H5L_info_t * info, void * operation_data ) {
      // Convert the operation data to our RegisterVector
      RegisterVector * registers = static_cast<RegisterVector *>( operation_data );

      // Open this register's group
      hid_t registerGroup = H5Gopen( registersGroup, memberName, H5P_DEFAULT );

      // Read memory block code
      MemoryBlock::BlockCode blockCode;
      hid_t hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        MemoryBlock::BlockCodeType::typeIdentifier );
      hid_t blockCodeAttribute = H5Aopen_name( registerGroup,
        IOHdf5Writer::BLOCKCODE_ATTRIBUTE_NAME.c_str() );
      assert( blockCodeAttribute != -1 );
      assert( H5Aread( blockCodeAttribute, hdf5Type, &blockCode ) >= 0 );
      H5Aclose( blockCodeAttribute );

      // Read initial offset into the memory block
      MemoryBlock::MemoryType initOffset;
      hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        MemoryBlock::MemoryTypeType::typeIdentifier );
      hid_t initOffsetAttribute = H5Aopen_name( registerGroup,
          IOHdf5Writer::INITOFFSET_ATTRIBUTE_NAME.c_str() );
      assert( initOffsetAttribute != -1 );
      assert( H5Aread( initOffsetAttribute, hdf5Type, &initOffset ) >= 0 );
      H5Aclose( initOffsetAttribute );

      // Read final offset into the memory block
      MemoryBlock::MemoryType endOffset;
      hid_t endOffsetAttribute = H5Aopen_name( registerGroup,
        IOHdf5Writer::ENDOFFSET_ATTRIBUTE_NAME.c_str() );
      assert( endOffsetAttribute != -1 );
      assert( H5Aread( endOffsetAttribute, hdf5Type, &endOffset ) >= 0 );
      H5Aclose( endOffsetAttribute );

      // Read type of this register
      CPPC_Datatype type;
      hdf5Type = IOHdf5Writer::getHdf5TypeIdentifier(
        IntegerType::typeIdentifier );
      hid_t typeAttribute = H5Aopen_name( registerGroup,
        IOHdf5Writer::BLOCKTYPE_ATTRIBUTE_NAME.c_str() );
      assert( typeAttribute != -1 );
      assert( H5Aread( typeAttribute, hdf5Type, &type ) >= 0 );
      H5Aclose( typeAttribute );

      // Convert initial and final offsets to bytes into the MemoryBlock
      DataType::DataTypeSize typeSize =
        DataTypeFactory::instance().getDataTypeSize( type );
      initOffset = initOffset * typeSize;
      endOffset = endOffset * typeSize;

      // Create new register and add it to the vector
      Register * r = new Register( reinterpret_cast<void *>( initOffset ), reinterpret_cast<void *>( endOffset ), type, memberName );
      r->setCode( blockCode );
      registers->push_back( r );

      H5Gclose( registerGroup );
      return 0;
    }

    hid_t IOHdf5Writer::createMemoryHeaderDataType() {

      // Header datatype (memory) creation
      hid_t memoryHeaderDataType = H5Tcreate( H5T_COMPOUND, sizeof( HeaderType ) );
      H5Tinsert( memoryHeaderDataType, COMPRESSORTYPE_ATTRIBUTE_NAME.c_str(), HOFFSET( HeaderType, compressorType ), H5T_NATIVE_UCHAR );
      H5Tinsert( memoryHeaderDataType, CHECKPOINTCODE_ATTRIBUTE_NAME.c_str(), HOFFSET( HeaderType, checkpointCode ), H5T_NATIVE_INT );
      H5Tinsert( memoryHeaderDataType, FRAGMENTSIZE_ATTRIBUTE_NAME.c_str(), HOFFSET( HeaderType, fragmentSize ), H5T_NATIVE_ULONG );

      return memoryHeaderDataType;
    }

    bool IOHdf5Writer::testCheckpoint( string path ) {
      // Open file
      fstream file( path.c_str(), std::ios::in | std::ios::out | std::ios::binary );
      assert( file.is_open() );

      // Read the SHA1
      unsigned char old_sha1[SHA_DIGEST_LENGTH];
      file.seekg( sizeof(BasicWriterType) + sizeof(unsigned char) );
      file.read( reinterpret_cast<char*>(old_sha1), SHA_DIGEST_LENGTH );

      // Recalculate the SHA1 for the original file
      SHA_CTX sha_ctx;
      unsigned const chunk = 1024*1024;
      char data[ chunk ];
      file.seekg( 0 );
      SHA1_Init( &sha_ctx );
      file.read( data, chunk );
      // Zero the SHA1 bytes in the file
      unsigned char * zeros = new unsigned char[SHA_DIGEST_LENGTH](); //this version initializes memory to zero
      std::memcpy( &data[sizeof(BasicWriterType) + sizeof(unsigned char)], zeros, SHA_DIGEST_LENGTH );
      SHA1_Update( &sha_ctx, data, file.gcount() );
      while( file.read( data, chunk ) ) {
        SHA1_Update( &sha_ctx, data, file.gcount() );
      }
      SHA1_Update( &sha_ctx, data, file.gcount() );
      unsigned char new_sha1[SHA_DIGEST_LENGTH];
      SHA1_Final( new_sha1, &sha_ctx );

      // Compare old and new SHA1
      delete [] zeros;
      file.close();
      return !std::memcmp( reinterpret_cast<char*>(old_sha1),  reinterpret_cast<char*>(new_sha1), SHA_DIGEST_LENGTH );
    }

    void IOHdf5Writer::startCheckpoint( string path ) {

      // Create the file creation property list specifying a user-block size of 512 bytes.
      // The user-block is a fixed-length block of data located at the beginning of the file
      // which is ignored by the HDF5 library and may be used to store any data information found
      // to be useful to applications. In the CPPC case, we want to store the WriterType here to be
      // able to discriminate between different writing strategies when reading the checkpoint file
      fcpl = H5Pcreate( H5P_FILE_CREATE );
      H5Pset_userblock( fcpl, FILE_CREATION_PROPERTY_LIST_USER_BLOCK_PARAMETER );

      // We use the default access property list for the file
      file_id = H5Fcreate( path.c_str(), H5F_ACC_TRUNC, fcpl, H5P_DEFAULT );

      // We save the file path to be able to write the WriterType later ( see commitCheckpoint() )
      file_path = path;
    }

    void IOHdf5Writer::commitCheckpoint( Checkpoint * c ) {

      H5Fclose( file_id );
      H5Pclose( fcpl );
      H5Pclose( datasetProperties );

      // Now, we open the file for post processing

      // First: write the first byte (WriterType)
      fstream file( file_path.c_str(), std::ios::in | std::ios::out | std::ios::binary );
      assert( file.is_open() );
      file << writerType.getStaticValue();

      // Second byte: whether this is a full checkpoint;
      unsigned char full = c->isFullCheckpoint()? 1 : 0;
      file << full;

      // Third: calculate the SHA1 (1MB at a time to avoid duplicating memory footprint)
      SHA_CTX sha_ctx;
      unsigned const chunk = 1024*1024;
      char data[ chunk ];
      file.seekg(0);
      SHA1_Init( &sha_ctx );
      while( file.read( data, chunk ) ) {
        SHA1_Update( &sha_ctx, data, file.gcount() );
      }
      SHA1_Update( &sha_ctx, data, file.gcount() );
      unsigned char sha1[SHA_DIGEST_LENGTH];
      SHA1_Final( sha1, &sha_ctx );

      // Copy the SHA1 into the file
      file.clear();
      file.seekg( sizeof(BasicWriterType) + sizeof(unsigned char) );
      file.write( reinterpret_cast<char*>(sha1), SHA_DIGEST_LENGTH );
      file.close();
    }

    string IOHdf5Writer::getHeapContextGroupName( HeapContext * c ) {
      ostringstream groupName;
      groupName << c->getFunctionName() << " - " << c->getCalledFromLine();
      return groupName.str();
    }

    string IOHdf5Writer::getLoopContextGroupName( LoopContext * c ) {

      unsigned long value;
      void * data = c->getVarValue()->getValue();

      switch( c->getVarValue()->getIdentifier() ) {

        case CPPC_CHAR:
          value = *((char *)data);
        case CPPC_UCHAR:
          value = *((unsigned char *)data);
          break;
        case CPPC_SHORT:
          value = *((short *)data);
          break;
        case CPPC_USHORT:
          value = *((unsigned short *)data);
          break;
        case CPPC_INT:
          value = *((int *)data);
          break;
        case CPPC_UINT:
          value = *((unsigned int *)data);
          break;
        case CPPC_LONG:
          value = *((long *)data);
          break;
        case CPPC_ULONG:
          value = *((unsigned long *)data);
          break;
        default:
          assert( false );
      }

      ostringstream groupName;
      groupName << c->getVarName() << value;
      return groupName.str();
    }

    hid_t IOHdf5Writer::getHdf5TypeIdentifier(
      DataType::DataTypeIdentifier cppcType ) {

      switch( cppcType ) {

      case CPPC_CHAR: return H5T_NATIVE_CHAR;
      case CPPC_UCHAR: return H5T_NATIVE_UCHAR;
      case CPPC_SHORT: return H5T_NATIVE_SHORT;
      case CPPC_USHORT: return H5T_NATIVE_USHORT;
      case CPPC_INT: return H5T_NATIVE_INT;
      case CPPC_UINT: return H5T_NATIVE_UINT;
      case CPPC_LONG: return H5T_NATIVE_LONG;
      case CPPC_ULONG: return H5T_NATIVE_ULONG;
      case CPPC_FLOAT: return H5T_NATIVE_FLOAT;
      case CPPC_DOUBLE: return H5T_NATIVE_DOUBLE;
      default: return H5T_NATIVE_UCHAR;

      }

    }

  }
}

namespace {

  using cppc::writer::Writer;
  using cppc::writer::IOHdf5Writer;
  using cppc::writer::WriterFactory;

  Writer * createWriter() {
    return new IOHdf5Writer();
  }

  const bool registered = WriterFactory::instance().registerWriter(
    IOHdf5Writer::staticWriterType().getStaticValue(), createWriter );

}
