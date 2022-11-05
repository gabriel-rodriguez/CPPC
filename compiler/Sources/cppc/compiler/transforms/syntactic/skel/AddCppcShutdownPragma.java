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
import cetus.hir.DeclarationStatement;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.ReturnStatement;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.utils.ObjectAnalizer;

/**
 * There is initially no need to make a semantic transform here. Let's assume
 * that simply placing a CPPC_Shutdown before finishing the application will
 * suffice, and see how this works.
 */
public class AddCppcShutdownPragma {

  private Program program;
  private static final String passName = "[AddCppcShutdownPragma]";

  public AddCppcShutdownPragma( Program program ) {
    this.program = program;
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    AddCppcShutdownPragma transform = new AddCppcShutdownPragma( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  private void start() {

    // Insert the pragma at the end of the last Statement in the main procedure
    Procedure mainProc = ObjectAnalizer.findMainProcedure( program );

    // Find last statement into the main procedure
    Statement lastStmt = ObjectAnalizer.findLastStatement( mainProc );

    // Add the pragma after the last statement
    addCppcShutdownPragma( lastStmt );
  }

  private void addCppcShutdownPragma( Statement ref ) {
    CppcShutdownPragma pragma = new CppcShutdownPragma();

    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( ref.getParent(),
        CompoundStatement.class );

    if( ref instanceof ReturnStatement ) {
      statementList.addStatementBefore( ref, pragma );
    } else {
      statementList.addStatementAfter( ref, pragma );
    }
  }
}
