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



#include <data/context.h>
#include <data/call_image.h>

#include <cassert>

namespace cppc {
  namespace data {

    Context::Context( Context * c )
      : parent( c ), subcontexts( new vector<Context *>() ),
      callImages( new vector<CallImage *>() ), currentCallImage( 0 ) {}

    // Deep copy of this Context's hierarchy
    Context::Context( const Context & copy )
      : parent( copy.parent ), subcontexts( new vector<Context *>() ),
      callImages( new vector<CallImage *>() ), currentCallImage( 0 ) {

      for( vector<Context *>::const_iterator contextIt =
        copy.subcontexts->begin(); contextIt != copy.subcontexts->end();
        contextIt++ ) {

        Context * child = (*contextIt)->clone();
        child->setParent( this ); // If this is not changed, the parent will be "copy"
        subcontexts->push_back( child );
      }

      for( vector<CallImage *>::const_iterator callIt =
        copy.callImages->begin(); callIt != copy.callImages->end();
        callIt++ ) {

        CallImage * image = new CallImage( **callIt );

        callImages->push_back( image );
        if( copy.currentCallImage == *callIt ) {
          this->currentCallImage = image;
        }
      }
    }

    Context::~Context() {
      for( vector<Context *>::iterator contextIt = subcontexts->begin();
        contextIt != subcontexts->end(); contextIt++ ) {

        delete *contextIt;
      }

      delete subcontexts;

      for( vector<CallImage *>::iterator callIt = callImages->begin();
        callIt != callImages->end(); callIt++ ) {

        delete *callIt;
      }

      delete callImages;
    }

    Context & Context::operator=( const Context & rhs ) {
      if( this == &rhs ) {
        return *this;
      }

      parent = rhs.parent;
      subcontexts = rhs.subcontexts;
      callImages = rhs.callImages;
      currentCallImage = rhs.currentCallImage;

      return *this;
    }

    void Context::addSubcontext( Context * c ) {
      for( vector<Context *>::iterator it = subcontexts->begin();
        it != subcontexts->end();
        it++ ) {

        if( (**it) == *c ) {
          return;
        }
      }

      subcontexts->push_back( c );
      c->setParent( this );
    }

    void Context::removeSubcontext( Context * c ) {
      for( vector<Context *>::iterator it = subcontexts->begin();
        it != subcontexts->end(); it++ ) {

        if( *it == c ) {
          subcontexts->erase( it );
          return;
        }
      }
    }

    void Context::addCallImage( CallImage * ci ) {

      assert( currentCallImage == 0 );

      for( vector<CallImage *>::iterator it = callImages->begin();
        it != callImages->end();
        it++ ) {

        if( (**it) == (*ci) ) {
          return;
        }
      }

      callImages->push_back( ci );
      currentCallImage = ci;
    }

    void Context::fetchCallImage( string s, unsigned int l ) {
      assert( currentCallImage == 0 );
      currentCallImage = this->getCallImage( s, l );
      assert( currentCallImage != 0 );
    }

    CallImage * Context::getCallImage( string s, unsigned int l ) {

      for( vector<CallImage *>::iterator it = callImages->begin();
        it != callImages->end(); it++ ) {

          if( ( (*it)->getFunctionName() == s ) &&
            ( (*it)->getCalledFromLine() == l ) ) {

            return *it;
          }
      }

      return 0;
    }

    void Context::releaseCallImage() {
      currentCallImage = 0;
    }

    void Context::removeCurrentCallImage() {

      assert( currentCallImage != 0 );

      for( vector<CallImage *>::iterator it = callImages->begin();
        it != callImages->end(); it++ ) {

        if( *it == currentCallImage ) {
          callImages->erase( it );
          currentCallImage = 0;
          return;
        }
      }

      currentCallImage = 0;
    }

    void Context::setCallImages( vector<CallImage *> * v ) {

      for( vector<CallImage *>::iterator it = callImages->begin();
        it != callImages->end();
        it++ ) {

        delete *it;
      }

      delete callImages;
      callImages = v;
    }

    bool Context::isEmpty() {
      return( ( subcontexts->size() == 0 ) &&
        ( callImages->size() == 0 ) );
    }

    Context * Context::addSubcontextTree( Context * rhs, BlockMap & blocks, BlockMap & rhsBlocks, MemoryBlock::MemoryType fragmentSize ) {
      // Clone the subcontext
      Context * newSubcontext = rhs->clone();
      newSubcontext->setParent( this );

      // Call recursively for each sub-subcontext
      for( vector<Context*>::iterator i = rhs->subcontexts->begin(); i != rhs->subcontexts->end(); ++i ) {
        newSubcontext->addSubcontextTree( *i, blocks, rhsBlocks, fragmentSize );
      }

      return newSubcontext;
    }

    void Context::update( Context * rhs, BlockMap & blocks, BlockMap & rhsBlocks, MemoryBlock::MemoryType fragmentSize ) {
      // Update subcontexts
      vector<Context*> * updatedSubcontexts = new vector<Context*>();

      // Go through the new subcontexts, updating the local ones as necessary
      for( vector<Context*>::iterator i = rhs->subcontexts->begin(); i != rhs->subcontexts->end(); ++i ) {
        Context * rhsSubcontext = *i;
        bool updated = false;
        vector<vector<Context*>::iterator> erase;
        for( vector<Context*>::iterator j = this->subcontexts->begin(); j != this->subcontexts->end(); ++j ) {
          Context * subcontext = *j;
          if( *subcontext == *rhsSubcontext ) {
            erase.push_back( j );
            subcontext->update( rhsSubcontext, blocks, rhsBlocks, fragmentSize );
            updatedSubcontexts->push_back( subcontext );
            updated = true;
            break;
          }
        }

        for( vector<vector<Context*>::iterator>::iterator j = erase.begin(); j != erase.end(); ++j ) {
          subcontexts->erase(*j);
        }

        if( !updated ) {
          // New subcontext tree
          updatedSubcontexts->push_back( this->addSubcontextTree( rhsSubcontext, blocks, rhsBlocks, fragmentSize ) );
        }
      }

      // The remaining local contexts (those that have not been updated) have disappeared from the context tree: remove
      for( vector<Context*>::iterator i = subcontexts->begin(); i != subcontexts->end(); ++i ) {
        delete * i;
      }

      delete subcontexts;
      subcontexts = updatedSubcontexts;

      vector<CallImage *> * updatedCallImages = new vector<CallImage*>();
      // Update call images
      assert( currentCallImage == 0 ) ;
      assert( rhs->currentCallImage == 0 );
      for( vector<CallImage*>::iterator i = rhs->callImages->begin(); i != rhs->callImages->end(); ++i ) {
        CallImage * rhsCI = *i;
        vector<vector<CallImage*>::iterator> erase;
        for( vector<CallImage*>::iterator j = this->callImages->begin(); j != this->callImages->end(); ++j ) {
          CallImage * CI = *j;
          if( *CI == *rhsCI ) {
            erase.push_back( j );
            CI->update( rhsCI, fragmentSize );
            updatedCallImages->push_back( CI );
          }
        }

        for( vector<vector<CallImage*>::iterator>::iterator j = erase.begin(); j != erase.end(); ++j ) {
          callImages->erase( *j );
        }
      }

      // Remove remaining call images, update vector
      for( vector<CallImage*>::iterator i = callImages->begin(); i != callImages->end(); ++i ) {
        delete *i;
      }
      delete callImages;
      callImages = updatedCallImages;
    }

  }
}
