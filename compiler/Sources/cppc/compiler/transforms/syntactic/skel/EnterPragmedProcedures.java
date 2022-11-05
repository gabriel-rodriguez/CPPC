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
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.NoSuchElementException;

public class EnterPragmedProcedures extends ProcedureWalker {

  private static String passName = "[EnterPragmedProcedures]";

  private EnterPragmedProcedures( Program program ) {
    super( program );
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    EnterPragmedProcedures transform = new EnterPragmedProcedures( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    while( iter.hasNext() ) {

      try {
        FunctionCall call = (FunctionCall)iter.next( FunctionCall.class );
        Identifier fName = (Identifier)call.getName();
        if( CppcRegisterManager.isRegistered( fName ) ) {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            fName );

          // Find a call to a pragmed procedure
          if( c.getPragmed() ) {
            Statement callStatement =
              (Statement)ObjectAnalizer.getParentOfClass( call,
                Statement.class );

            // Context management
            Statement push = addContextPushBefore( callStatement,
              (Identifier)call.getName() );
            Statement pop = addContextPopAfter( callStatement );

            // Add an execute pragma
            CppcExecutePragma pragma = new CppcExecutePragma( push, pop );
            CompoundStatement statementList = (CompoundStatement)
              ObjectAnalizer.getParentOfClass( push, CompoundStatement.class );
            statementList.addStatementBefore( push, pragma );
          }
        }
      } catch( NoSuchElementException e ) {}
    }
  }

  private Statement addContextPushBefore( Statement ref,
    Identifier functionName ) {

    // Create function call statement
    FunctionCall call = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().CONTEXT_PUSH_FUNCTION() ) );

    // Add function name parameter (StringLiteral)
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
        functionName.toString() ) );

    // Add line number parameter (IntegerLiteral)
    call.addArgument( new IntegerLiteral( ref.where() ) );

    // Create statement for the call and add it to the CompoundStatement
    Statement callStatement = new ExpressionStatement( call );

    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( ref,
        CompoundStatement.class );
    statementList.addStatementBefore( ref, callStatement );

    return callStatement;
  }

  private Statement addContextPopAfter( Statement ref ) {

    // Create function call statement
    FunctionCall call = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().CONTEXT_POP_FUNCTION() ) );

    // Create statement for the call and add it to the CompoundStatement
    Statement callStatement = new ExpressionStatement( call );

    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( ref,
        CompoundStatement.class );
    statementList.addStatementAfter( ref, callStatement );

    return callStatement;
  }
}
