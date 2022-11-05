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



#include <checkpointer/checkpoint.h>
#include <controller/actions/controller_helper.h>
#include <data/call_image.h>
#include <data/register.h>
#include <util/filemanager/file_system_manager.h>

#include <cassert>
#include <cstdlib>

#include <iostream>
#include <sstream>

using cppc::checkpointer::BasicCheckpointCode;
using cppc::data::CallImage;
using cppc::data::Register;
using cppc::data::RegisterVector;
using cppc::util::configuration::ConfigurationManager;
using cppc::util::configuration::ParameterValueType;
using cppc::util::filesystem::FileSystemManager;

using std::cout;
using std::ostringstream;

namespace cppc {
  namespace controller {
    namespace actions {

      ControllerHelper::ControllerHelper() {}

      ControllerHelper::ControllerHelper( const ControllerHelper & ch ) {}

      ControllerHelper & ControllerHelper::operator=(
        const ControllerHelper & rhs ) {

        return *this;
      }

      ControllerHelper::~ControllerHelper() {}

      void ControllerHelper::createGenericDirectory( const string path ) {
        // If file exists: return
        if( FileSystemManager::fileExists( path ) ) {
          return;
        }

        // Check if we should create parent directory
        std::size_t pos = path.rfind( "/", path.length()-1 );
        if( pos != string::npos ) {
          if( pos != 0 ) {
            ControllerHelper::createGenericDirectory( path.substr( 0, pos ) );
          }
        }

        // Double-check (should be enough, as the lock should be provided by
        // the operative system)
        if( !FileSystemManager::fileExists( path ) &&
          !FileSystemManager::createDirectory( path, 0755 ) ) {
          if( !FileSystemManager::fileExists( path ) ) {
            cout << "CPPC: Cannot create directory: " << path << "\n";
            exit( -1 );
          }
        }
      }

      string ControllerHelper::getChkptPath( string localDir, CheckpointCode num ) {
        BasicCheckpointCode aux = num.getStaticValue();
        ostringstream s;
        s << aux;

        string path = localDir + s.str() + getSuffixParameter();
        return path;
      }

      const ParameterKeyType
        ControllerHelper::CPPC_APPLICATION_NAME_PARAMETER_KEY(
          "CPPC/Controller/ApplicationName" );

      string ControllerHelper::getApplicationNameParameter() {
        static const ParameterValueType * const value =
          ConfigurationManager::instance().getParameter(
            CPPC_APPLICATION_NAME_PARAMETER_KEY );

        return *value;
      }

      const ParameterKeyType
        ControllerHelper::CPPC_CHECKPOINT_ON_FIRST_TOUCH_PARAMETER_KEY(
          "CPPC/Controller/CheckpointOnFirstTouch" );

      bool ControllerHelper::getCheckpointOnFirstTouchParameter() {
        static const ParameterValueType * const value =
          ConfigurationManager::instance().getParameter(
            CPPC_CHECKPOINT_ON_FIRST_TOUCH_PARAMETER_KEY );
        static const bool checkpointOnFirstTouch = ( value != NULL ) &&
          ( (*value) == "true" );

        return checkpointOnFirstTouch;
      }

      const ParameterKeyType ControllerHelper::CPPC_FREQUENCY_PARAMETER_KEY( "CPPC/Controller/Frequency" );
      unsigned int ControllerHelper::getFrequencyParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_FREQUENCY_PARAMETER_KEY );
        static unsigned int frequency = std::atoi( value->c_str() );

        return frequency;
      }

      const ParameterKeyType ControllerHelper::CPPC_FULL_FREQUENCY_PARAMETER_KEY( "CPPC/Controller/FullFrequency" );
      unsigned int ControllerHelper::getFullFrequencyParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_FULL_FREQUENCY_PARAMETER_KEY );
        static unsigned int fullFreq = std::atoi( value->c_str() );

        return fullFreq;
      }

      const ParameterKeyType ControllerHelper::CPPC_RECOVERY_SETS_PARAMETER_KEY( "CPPC/Controller/StoredRecoverySets" );
      unsigned int ControllerHelper::getRecoverySetsParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_RECOVERY_SETS_PARAMETER_KEY );
        unsigned int recoverySets = std::atoi( value->c_str() );
        return recoverySets;
      }

      bool ControllerHelper::CPPC_RESTART_PARAMETER = false;
      const ParameterKeyType ControllerHelper::CPPC_RESTART_PARAMETER_KEY( "CPPC/Controller/Restart" );
      bool ControllerHelper::getRestartParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_RESTART_PARAMETER_KEY );
        static bool restart = ( value != NULL ) && ( (*value) == "true" );
        const static bool initParameterValueWorkaround = ( CPPC_RESTART_PARAMETER = restart );
        return CPPC_RESTART_PARAMETER;
      }

      const ParameterKeyType ControllerHelper::CPPC_ROOT_DIRECTORY_PARAMETER_KEY( "CPPC/Controller/RootDir" );
      string ControllerHelper::getRootDirectoryParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_ROOT_DIRECTORY_PARAMETER_KEY );
        static string rootDirectory = value->rfind( '/' ) == value->length() ? (*value) : ( *value + '/' );
        return rootDirectory;
      }

      const ParameterKeyType ControllerHelper::CPPC_SUFFIX_PARAMETER_KEY( "CPPC/Controller/Suffix" );
      string ControllerHelper::getSuffixParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_SUFFIX_PARAMETER_KEY );
        return *value;
      }

      const ParameterKeyType ControllerHelper::CPPC_WRITER_TYPE_PARAMETER_KEY("CPPC/Writer/Type" );
      WriterType ControllerHelper::getWriterTypeParameter() {
        static const ParameterValueType * const value = ConfigurationManager::instance().getParameter( CPPC_WRITER_TYPE_PARAMETER_KEY );
        static const WriterType writerType = std::atoi( value->c_str() );
        return writerType;
      }

      Context * ControllerHelper::getHierarchyRoot( Context * c ) {

        while( c->getParent() != 0 ) {
          c = c->getParent();
        }

        return c;
      }

      void ControllerHelper::addMemblocksForRegister( Register * r, BlockMap * blocks ) {
        bool finished = false;
        unsigned long rInit = reinterpret_cast<unsigned long>( r->initAddress() );
        unsigned long rEnd = reinterpret_cast<unsigned long>( r->endAddress() );

        for( BlockMap::iterator it = blocks->begin(); it != blocks->end();
          it++ ) {

          MemoryBlock * b = it->second;

          if( rInit < b->getInitAddr() && rEnd > b->getEndAddr() ) {
            // Aliasing contained
            b->setLimits( rInit, rEnd );
            r->setCode( b->getCode() );
            b->getRegisters()->push_back( r );
            finished = true;
          } else {
            if( ( rInit < b->getInitAddr() ) && ( rEnd > b->getInitAddr() ) ) {
              // Initial aliasing
              b->setInitAddr( rInit );
              r->setCode( b->getCode() );
              b->getRegisters()->push_back( r );
              finished = true;
            } else {
              if( ( rInit >= b->getInitAddr() ) && ( rEnd <= b->getEndAddr() ) ) {
                // Contains
                r->setCode( b->getCode() );
                b->getRegisters()->push_back( r );
                finished = true;
              } else {
                if( ( rInit < b->getEndAddr() ) && ( rEnd > b->getEndAddr() ) ) {
                  // Final aliasing
                  b->setEndAddr( rEnd );
                  r->setCode( b->getCode() );
                  b->getRegisters()->push_back( r );
                  finished = true;
                }
              }
            }
          }
        }

        if( !finished ) {
          MemoryBlock * newBlock = new MemoryBlock( rInit, rEnd, r->type() );
          newBlock->getRegisters()->push_back( r );
          blocks->insert( BlockMap::value_type( newBlock->getCode(), newBlock ) );
          r->setCode( newBlock->getCode() );
        }
      }

      void ControllerHelper::removeMemblocksForRegister( Register * r, BlockMap * blocks ) {
        // Get associated memory block
        BlockMap::iterator i = blocks->find( r->getCode() );
        assert( i != blocks->end() );
        MemoryBlock * b = i->second;

        // If this block contains a single register, remove the block.
        if( b->getRegisters()->size() == 1 ) {
          delete b;
          blocks->erase( i );
          return;
        }

        // If it contains more than one register, modify the block.

        // If this register's lower bound is equal to the memory block's
        // lower bound, then there's a chance that the block has to be shrinked
        // on the left
        RegisterVector * regs = b->getRegisters();
        if( r->initAddress() == reinterpret_cast<void*>(b->getInitAddr()) ) {
          // Go through all registers in the block which are not r, looking for the
          // new lower limit
          unsigned long newInit = b->getEndAddr();
          for( RegisterVector::iterator i = regs->begin(); i != regs->end(); ++i ) {
            Register * r2 = *i;
            if( r2 == r ) { continue; }
            if( r2->initAddress() == reinterpret_cast<void*>(b->getInitAddr()) ) {
              // r2 starts at the same address as r. No changes to the block.
              newInit = b->getInitAddr();
              break;
            }
            if( r2->initAddress() < reinterpret_cast<void*>(newInit) ) {
              newInit = reinterpret_cast<unsigned long>(r2->initAddress());
            }
          }

          b->setInitAddr( newInit );
        }

        // If this register's upper bound is equal to the memory block's
        // upper bound, then there's a chance that the block has to be shrinked on
        // the right
        if( r->endAddress() == reinterpret_cast<void*>(b->getEndAddr()) ) {
          // Go through all registers in the block which are not r, look for the
          // new upper limit
          unsigned long newEnd = b->getInitAddr();
          for( RegisterVector::iterator i = regs->begin(); i != regs->end(); ++i ) {
            Register * r2 = *i;
            if( r2 == r ) { continue; }
            if( r2->endAddress() == reinterpret_cast<void*>(b->getEndAddr()) ) {
              // r2 ends at the same address as r. No changes to the block.
              newEnd = b->getEndAddr();
              break;
            }
            if( r2->endAddress() > reinterpret_cast<void*>(newEnd) ) {
              newEnd = reinterpret_cast<unsigned long>(r2->endAddress());
            }
          }

          b->setEndAddr( newEnd );
        }

        // Erase the register from the list of registers in the block
        for( RegisterVector::iterator i = regs->begin(); i != regs->end(); ++i ) {
          if( *i == r ) {
            regs->erase( i );
            return;
          }
        }

        // This should never happen, the register should be erased from the block
        assert( false );
      }
    }
  }
}
