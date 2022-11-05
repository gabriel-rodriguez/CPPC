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
import cetus.hir.Identifier;
import cetus.hir.Label;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;


public class AddExitLabels extends ProcedureWalker {

  private static String passName = "[AddExitLabels]";

  private AddExitLabels( Program program ) {
    super( program );
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    AddExitLabels transform = new AddExitLabels( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    // The exit label must not be included in the main procedure
    if( ObjectAnalizer.isMainProcedure( procedure ) ) {
      return;
    }

    // The exit label must only be included in procedures that have
    // other #pragma's inserted, that is, in procedures that have a
    // CPPCC_LABEL_MARK annote (for instance)
    if( !ObjectAnalizer.isCppcProcedure( procedure ) ) {
      return;
    }

    // We obtain the procedure body
    CompoundStatement statementList = procedure.getBody();

    // Add the CPPC_ENTER_FUNCTION label after the declarations that
    // are placed at the beginning of the procedure body.
    Label enterLabel = new Label( new Identifier(
        GlobalNamesFactory.getGlobalNames().ENTER_FUNCTION_LABEL() ) );
    Statement ref = ObjectAnalizer.findLastDeclaration( procedure );

    // If no declaration found: add the label at the very beginning of the block
    if( ref == null ) {
      procedure.getBody().getChildren().add( 0, enterLabel );
      enterLabel.setParent( procedure.getBody() );
    } else {
      procedure.getBody().addStatementAfter( ref, enterLabel );
    }

    // Put the conditional jump mark after the enter label
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter( enterLabel, jump );

    // Add the CPPC_EXIT_FUNCTION label at the end of the procedure body
    // This is a CppcLabel, as it must be the target of a conditional jump.
    CppcLabel exitLabel = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().EXIT_FUNCTION_LABEL() ) );

    Statement last = ObjectAnalizer.findLastStatement( procedure );
    if( last instanceof cetus.hir.ReturnStatement ) {
      statementList.addStatementBefore( last, exitLabel );
    } else {
      statementList.addStatement( exitLabel );
      statementList.addStatement( new cetus.hir.ReturnStatement() );
    }
  }
}
