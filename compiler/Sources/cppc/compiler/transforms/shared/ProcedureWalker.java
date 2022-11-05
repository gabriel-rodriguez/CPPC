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




package cppc.compiler.transforms.shared;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.Procedure;
import cetus.hir.Program;

import java.util.NoSuchElementException;

public abstract class ProcedureWalker {

    private Program program;

    protected ProcedureWalker( Program program ) {
      this.program = program;
    }

    protected void start() {

      BreadthFirstIterator iter = new BreadthFirstIterator( this.program );
      iter.pruneOn( Procedure.class );

      while( iter.hasNext() ) {
        Procedure procedure = null;
        try {
          procedure = (Procedure)iter.next( Procedure.class );
        } catch( NoSuchElementException e ) {
          return;
        }

        this.walkOverProcedure( procedure );
      }
    }

    protected abstract void walkOverProcedure( Procedure procedure );
}
