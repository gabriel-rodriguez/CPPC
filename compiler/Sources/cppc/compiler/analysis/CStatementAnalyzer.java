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




package cppc.compiler.analysis;

import cetus.hir.BreakStatement;
import cetus.hir.Case;
import cetus.hir.Default;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.ReturnStatement;
import cetus.hir.SwitchStatement;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.SetOperations;

import java.util.Set;

public class CStatementAnalyzer extends StatementAnalyzer {

  CStatementAnalyzer() {}

  protected void analyzeStatement( BreakStatement s ) {
    currentStatement.setWeight( 0 );
    currentStatement.statementCount = 0;
  }

  protected void analyzeStatement( Case stmt ) {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
        stmt.getExpression() ) );
  }

  protected void analyzeStatement( Default stmt ) {}

  protected void analyzeStatement( ForLoop forLoop ) {
    // Get what is consumed/generated in the initializer, and add it to this
    // cppcStatement
    CppcStatement initializer = (CppcStatement)forLoop.getInitialStatement();
    analyzeStatement( initializer );

    Set<Identifier> consumedByInit = initializer.getConsumed();

    Set<Identifier> consumed = currentStatement.getConsumed();
    consumed.addAll( consumedByInit );

    // Do not take into account what is generated in the initializer, since it
    // will prevent loop variables from being registered when analyzing data
    // flow. This is not optimal, though.
    Set<Identifier> generated = currentStatement.getGenerated();

    // Get an instance of SetOperations<Identifier>
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();

    // Get what is consumed/generated in the condition
    ExpressionStatement expressionStatement = new ExpressionStatement(
      (Expression)forLoop.getCondition().clone() );
    CppcStatement conditionStatement = new CppcStatement(
      expressionStatement );
    conditionStatement.setParent( forLoop );
    analyzeStatement( conditionStatement );

    // // Add consumed identifiers as: cppcStatement.consumed =
       // cppcStatement.consumed + {conditionStatement.consumed -
       // cppcStatement.generated}
    currentStatement.getConsumed().addAll( setOps.setMinus(
      conditionStatement.getConsumed(), currentStatement.getGenerated() ) );
    // // Add generated identifiers as: cppcStatement.generated =
       // cppcStatement.generated + conditionStatement.generated
    currentStatement.getGenerated().addAll(
      conditionStatement.getGenerated() );
    currentStatement.getPartialGenerated().addAll(
      conditionStatement.getPartialGenerated() );

    // Get what is consumed/generated in the step
    ExpressionStatement stepExpression = new ExpressionStatement(
      (Expression)forLoop.getStep().clone() );
    CppcStatement stepStatement = new CppcStatement( stepExpression );
    stepStatement.setParent( forLoop );
    analyzeStatement( stepStatement );

    // // Add consumed identifiers as: cppcStatement.consumed =
       // cppcStatement.consumed + {stepStatement.consumed -
       // cppcStatement.generated}
    currentStatement.getConsumed().addAll( setOps.setMinus(
      stepStatement.getConsumed(), currentStatement.getGenerated() ) );
    // // Add generated identifiers as: cppcStatement.generated =
       // cppcStatement.generated + stepStatement.generated
    currentStatement.getGenerated().addAll( stepStatement.getGenerated() );
    currentStatement.getPartialGenerated().addAll(
      stepStatement.getPartialGenerated() );

    // Analyze body
    this.analyzeStatement( (CppcStatement)forLoop.getBody() );
    currentStatement.setWeight(
      ((CppcStatement)forLoop.getBody()).getWeight() );
    currentStatement.statementCount +=
      ((CppcStatement)forLoop.getBody()).statementCount;
  }

  protected void analyzeStatement( ReturnStatement returnStatement ) {
    if( returnStatement.getExpression() != null ) {
      currentStatement.getConsumed().addAll(
        ExpressionAnalyzer.analyzeExpression(
          currentStatement, returnStatement.getExpression() ) );
    }
  }

  protected void analyzeStatement( SwitchStatement stmt ) {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
        stmt.getExpression() ) );

    // Analyze body (direct access to children since this method returns a
    // compound statement, and we transformed it into a cppc statement.
    // Otherwise it would raise a class cast exception)
    this.analyzeStatement( (CppcStatement)stmt.getChildren().get( 1 ) );
    currentStatement.setWeight(
      ((CppcStatement)stmt.getChildren().get( 1 )).getWeight() );
    currentStatement.statementCount =
      ((CppcStatement)stmt.getChildren().get( 1 )).statementCount;
  }
}
