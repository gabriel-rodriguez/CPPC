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
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.ReturnStatement;
import cetus.hir.Statement;
import cetus.hir.SwitchStatement;

import cppc.compiler.cetus.MultiExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cppc.compiler.cetus.CppcStatement;

public class CSymbolicAnalyzer extends SymbolicAnalyzer {

  CSymbolicAnalyzer() {}

  protected void analyzeStatement( BreakStatement s ) {}

  protected void analyzeStatement( Case stmt ) {
    SymbolicExpressionAnalyzer.analyzeExpression( stmt.getExpression(),
      knownSymbols );
  }

  private Identifier getForLoopVar( ForLoop forLoop ) {
    CppcStatement cppcStatement = (CppcStatement)forLoop.getParent();
    Set<Identifier> generated = cppcStatement.getGenerated();
    if( generated.size() == 1 ) {
      return generated.iterator().next();
    }

    return null;
  }

  protected void enterLoop( ForLoop forLoop ) {
    // Analyze the initializer
    this.analyzeStatement( (CppcStatement)forLoop.getInitialStatement(),
      knownSymbols );

    // Remove loop carried symbols
    this.removeLoopCarriedSymbols( forLoop );

    // Try to build a multiexpression for loop variables. Just aim to analyze
    // normal "i=A; f(i); g(i)" loops
    Identifier loopVar = this.getForLoopVar( forLoop );
    if( loopVar != null ) {
      Map<Identifier,List> localKnown = new HashMap<Identifier,List>(
        knownSymbols );
      this.analyzeStatement(
        (CppcStatement)forLoop.getInitialStatement(), localKnown );
      if( localKnown.containsKey( loopVar ) ) {
        List values = localKnown.get( loopVar );
        if( values.size() == 1 ) {
          Expression startValue = (Expression)values.get( 0 );
          if( startValue instanceof IntegerLiteral ) {
            // Got the first value. Iterate checking condition and updating the
            // value until the condition is not met.
            List<Expression> iterationValues = new ArrayList<Expression>();

            Expression currentValue = startValue;
            Expression truthValue =
              SymbolicExpressionAnalyzer.analyzeExpression(
                forLoop.getCondition(), localKnown );
            Expression booleanTrue =
              SymbolicExpressionAnalyzer.instance.buildBooleanLiteral( true );

            while( truthValue.equals( booleanTrue ) ) {
              iterationValues.add( (Expression)currentValue.clone() );

              // Update value
              SymbolicExpressionAnalyzer.analyzeExpression( forLoop.getStep(),
                localKnown );
              values = localKnown.get( loopVar );
              currentValue = (Expression)values.get( 0 );

              // Update truth value
              truthValue = SymbolicExpressionAnalyzer.analyzeExpression(
                forLoop.getCondition(), localKnown );
            }

            MultiExpression mexpr = new MultiExpression( loopVar );
            for( Expression iter: iterationValues ) {
              mexpr.addExpression( iter, iter );
            }

            if( !mexpr.getChildren().isEmpty() ) {
              values = new ArrayList<Expression>( 1 );
              values.add( mexpr );
              this.addKnownSymbol( knownSymbols, loopVar, values, forLoop );
            }
          }
        }
      }
    } else {
      // Analyze the condition
      SymbolicExpressionAnalyzer.analyzeExpression( forLoop.getCondition(),
        knownSymbols );

      // Analyze the step
      SymbolicExpressionAnalyzer.analyzeExpression( forLoop.getStep(),
        knownSymbols );
    }
  }

  protected void exitLoop( ForLoop forLoop ) {
    // Get loop variable and remove the multi expression
    Identifier loopVar = this.getForLoopVar( forLoop );
    if( knownSymbols.containsKey( loopVar ) ) {
      List<Expression> values = (List<Expression>)knownSymbols.get( loopVar );
      if( values.size() == 1 ) {
        // Under normal circumstances, the variable value upon loop exit would
        // be the last iteration's value
        Expression value = values.get( 0 );
        if( value instanceof MultiExpression ) {
          Expression lastChild = (Expression)value.getChildren().get(
            value.getChildren().size() - 1 );
          values.clear();
          values.add( lastChild );
        }
      } else {
        // The loop is over and we have to void the value of the loop variable.
        // If we don't know what value will the loop variable have upon loop
        // exit, we better just remove it from the known symbols list
        knownSymbols.remove( loopVar );
      }
    }
  }

  protected void analyzeStatement( ForLoop forLoop ) {}

  protected void analyzeStatement( ReturnStatement returnStatement ) {
    SymbolicExpressionAnalyzer.analyzeExpression(
      returnStatement.getExpression(), knownSymbols );
  }

  protected void analyzeStatement( SwitchStatement stmt ) {
    SymbolicExpressionAnalyzer.analyzeExpression( stmt.getExpression(),
      knownSymbols );

    // If we use "getBody" an Exception is raised, since the return type of
    // getBody is CompoundStatement, and it is now a CppcStatement
    this.analyzeStatement( (CppcStatement)stmt.getChildren().get(1),
      knownSymbols );
  }
}
