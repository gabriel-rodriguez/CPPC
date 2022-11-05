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




package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.Declaration;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class CheckPragmedProcedures extends ProcedureWalker {

  private static String passName = "[CheckPragmedProcedures]";
  private ArrayList<Procedure> notPragmed;

  private CheckPragmedProcedures( Program program ) {
    super( program );
    notPragmed = new ArrayList<Procedure>();
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    CheckPragmedProcedures transform = new CheckPragmedProcedures( program );
    transform.start();
    transform.addTransitivity();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    BreadthFirstIterator iter = new BreadthFirstIterator( procedure );
    iter.pruneOn( Expression.class );
    while( iter.hasNext() ) {
      try {
        Statement stmt = (Statement)iter.next( Statement.class );

        if( stmt instanceof CppcLabel ) {
          this.setPragmed( procedure );
          return;
        }

        if( stmt instanceof CppcPragmaStatement ) {
          this.setPragmed( procedure );
          return;
        }
      } catch( NoSuchElementException e ) {
        notPragmed.add( procedure );
      }
    }
  }

  private void setPragmed( Procedure procedure ) {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)procedure.getName() );

    if( c == null ) {
      throw new InternalError( "Procedure " + procedure.getName() +
        " not registered" );
    }

    if( !c.getPragmed() ) {
      c.setPragmed( true );
      LanguageAnalyzerFactory.getLanguageAnalyzer().checkIncludes( procedure );
    }
  }

  // This function adds to the "pragmed" list those procedures which
  // contain calls to pragmed procedures.
  private void addTransitivity() {

    boolean modified = true;
    ArrayList<Procedure> removeFromList = new ArrayList<Procedure>(
      notPragmed.size() ); // Used due to ArrayList iterators not being "safe"

    // While the list of not pragmed procedures is not empty, and there
    // have been modifications on the last iteration
    while( ( notPragmed.size() != 0 ) && modified ) {

      // Set modified to false
      modified = false;

      // Check for transitivity in each "notPragmed" procedure
      for( Procedure procedure: notPragmed ) {

        boolean isPragmed = false;

        // Search for calls to pragmed procedures
        DepthFirstIterator procIter = new DepthFirstIterator( procedure );
        procIter.pruneOn( Expression.class );
        while( procIter.hasNext() && !isPragmed ) {
          try {
            FunctionCall call = (FunctionCall)procIter.next(
              FunctionCall.class );
            Identifier fName = (Identifier)call.getName();
            if( CppcRegisterManager.isRegistered( fName ) ) {
              ProcedureCharacterization calledChar =
                CppcRegisterManager.getCharacterization( fName );
              if( calledChar.getPragmed() ) {
                // Call to pragmed procedure: add this procedure to pragmed list
                this.setPragmed( procedure );

                // Schedule it for deletion, and set modified to true
                removeFromList.add( procedure );
                modified = true;
                isPragmed = true;
              }
            }
          } catch( NoSuchElementException e ) {}
        }
      }

      // Removed scheduled procedures and clean scheduled list
      for( Procedure pragmed: removeFromList ) {
        notPragmed.remove( pragmed );
      }
      removeFromList.clear();
    }
  }
}
