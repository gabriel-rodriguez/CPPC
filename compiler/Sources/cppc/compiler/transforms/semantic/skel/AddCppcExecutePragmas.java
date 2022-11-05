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



package cppc.compiler.transforms.semantic.skel;

import cetus.hir.Annotation;
import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class AddCppcExecutePragmas extends ProcedureWalker {

  private Program program;
  private static final String passName = "[AddCppcExecutePragmas]";
  private static final String CPPC_NONPORTABLE_STATE_CREATOR_ROLE =
    "CPPC/Nonportable";
  private static final HashSet<Identifier> nonportableFunctions =
    initHashSet( CPPC_NONPORTABLE_STATE_CREATOR_ROLE );

  private static final HashSet<Identifier> initHashSet( String role ) {
    return CppcRegisterManager.getProceduresWithRole( role );
  }

  protected AddCppcExecutePragmas( Program program ) {
    super( program );
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    AddCppcExecutePragmas transform = new AddCppcExecutePragmas( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected boolean parse( String pragmaText ) {
    throw new IllegalAccessError();
  }

  protected void walkOverProcedure( Procedure procedure ) {

    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    // Get function calls which fulfill the CPPC/Nonportable role
    while( iter.hasNext() ) {
      try {
        FunctionCall call = (FunctionCall)iter.next( FunctionCall.class );

        // Enclose such functions using execute/end execute directives
        if( nonportableFunctions.contains( call.getName() ) ) {
          Statement stmt = (Statement)ObjectAnalizer.getParentOfClass( call,
            Statement.class );
//           ObjectAnalizer.encloseWithExecutes( stmt );
          addNonportableFunctionMark( stmt );
//           this.addExecuteArticulation( stmt, call );
        }
      } catch( NoSuchElementException e ) {}
    }
  }

  private void addNonportableFunctionMark( Statement stmt ) {

    // Create annote for marking nonportable function position
//     Annotation annote = new Annotation( NONPORTABLE_FUNCTION_MARK_STRING );
//     Statement annoteStatement = new DeclarationStatement( annote );
    Statement markStatement = new CppcNonportableFunctionMark();

    // Get the compound statement containing the nonportable call
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( stmt,
        CompoundStatement.class );

    // Insert the annotation before the nonportable call
//     statementList.addStatementBefore( stmt, annoteStatement );
    statementList.addStatementBefore( stmt, markStatement );
  }

//   private void addExecuteArticulation( Statement stmt, FunctionCall call ) {
//
//     Identifier functionName = (Identifier)call.getName();
//
//     // Get compound statement for this statement
//     CompoundStatement statementList =
//       (CompoundStatement)ObjectAnalizer.getParentOfClass( stmt,
//         CompoundStatement.class );
//
//     // Add CPPC_Create_call_image before function call
//     FunctionCall cppcCreateCallImageCall = new FunctionCall( new Identifier(
//       GlobalNamesFactory.getGlobalNames().CREATE_CALL_IMAGE_FUNCTION() ) );
//     cppcCreateCallImageCall.addArgument(
//       LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
//         functionName.toString() ) );
//     cppcCreateCallImageCall.addArgument( new IntegerLiteral( stmt.where() ) );
//     Statement cppcCreateCallImageStatement = new ExpressionStatement(
//       cppcCreateCallImageCall );
//
//     statementList.addStatementBefore( stmt, cppcCreateCallImageStatement );
//
//     // For each input parameter, add a CPPC_Register_for_call_image
// //     ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
// //       (Identifier)call.getName() );
// //     Set<ProcedureParameter> consumed = c.getConsumed();
// //     for( ProcedureParameter p: consumed ) {
// //       System.out.println( "Adding CPPC_Register_for_call_image for parameter "
// //           + "in position " + p.getPosition() + " for function " + call.getName() );
// //       addCppcRegisterForCallImage( stmt, p );
// //     }
//     addCppcRegisterForCallImage( stmt, null );
//   }
//
// //   private void addCppcRegisterForCallImage( Statement stmt,
// //     ProcedureParameter p ) {
// //
// //     cppc.compiler.cetus.CppcStatement cppcStmt =
// //       new cppc.compiler.cetus.CppcStatement( stmt );
// //     cppc.compiler.analysis.StatementAnalyzer.analyzeStatement( cppcStmt );
// //     for( Identifier id: cppcStmt.getConsumed() ) {
// //       System.out.println( "Adding cppc register for call image for parameter: " + id );
// //     }
// //   }
}
