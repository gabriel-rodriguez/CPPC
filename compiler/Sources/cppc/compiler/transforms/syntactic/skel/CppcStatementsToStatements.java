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

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.ProcedureWalker;

import java.util.NoSuchElementException;

public class CppcStatementsToStatements extends ProcedureWalker {

  private static String passName = "[CppcStatementsToStatements]";

  private CppcStatementsToStatements( Program program ) {
    super( program );
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    CppcStatementsToStatements transform = new CppcStatementsToStatements(
      program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator procIter = new DepthFirstIterator( statementList );
    procIter.next(); //Discharge the CompoundStatement

    while( procIter.hasNext() ) {
      Statement statement = null;
      try {
        statement = (Statement)procIter.next( Statement.class );
      } catch( NoSuchElementException e ) {
        return;
      }

      if( statement instanceof CppcStatement ) {
        CppcStatement cppcStatement = (CppcStatement)statement;
        Statement reference = cppcStatement.getStatement();
        cppcStatement.swapWith( reference );
      }
    }
  }
}
