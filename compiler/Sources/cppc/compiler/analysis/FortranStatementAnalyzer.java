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

import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Identifier;
import cetus.hir.ReturnStatement;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.FormatStatement;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.fortran.ComputedGotoStatement;
import cppc.compiler.fortran.FortranDoLoop;

public class FortranStatementAnalyzer extends StatementAnalyzer {

  FortranStatementAnalyzer() {}

  protected void analyzeStatement( ComputedGotoStatement s ) {}

  // We have to be aware of CommonBlock's, unlike the general version of this
  // function. The behaviour is the same, anyway, just taking into account CB's
  protected void analyzeStatement( DeclarationStatement s ) {
    Declaration declaration = s.getDeclaration();

    // If it is a CommonBlock: extract inner declaration
    if( declaration instanceof CommonBlock ) {
      for( VariableDeclaration inner:
        ((CommonBlock)declaration).getDeclarations() ) {

        DeclarationStatement virtual = new DeclarationStatement( inner );
        super.analyzeStatement( virtual );
        inner.setParent( declaration );
      }

      // Declarations have no weight
      currentStatement.setWeight( 0 );
      return;
    }

    // Else: use the default behaviour
    super.analyzeStatement( s );
  }

  protected void analyzeStatement( FormatStatement formatStatement ) {
    currentStatement.setWeight( 0 );
    currentStatement.statementCount = 0;
  }

  protected void analyzeStatement( FortranDoLoop fortranDoLoop ) {
    // Fortran does not generate anything in control expressions, thus:
    // Get what is consumed in the control expressions: start, stop, step
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(
        currentStatement, fortranDoLoop.getStart() ) );

    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
      fortranDoLoop.getStop() ) );

    if( fortranDoLoop.getStep() != null ) {
      currentStatement.getConsumed().addAll(
        ExpressionAnalyzer.analyzeExpression( currentStatement,
        fortranDoLoop.getStep() ) );
    }

    // It generates its loop variable
    currentStatement.getGenerated().add(
      (Identifier)fortranDoLoop.getLoopVar().clone() );
    currentStatement.getPartialGenerated().add(
      (Identifier)fortranDoLoop.getLoopVar().clone() );

    // Analyze body
    this.analyzeStatement( (CppcStatement)fortranDoLoop.getBody() );
    currentStatement.setWeight(
      ((CppcStatement)fortranDoLoop.getBody()).getWeight() );
    currentStatement.statementCount +=
      ((CppcStatement)fortranDoLoop.getBody()).statementCount;
  }

  protected void analyzeStatement( ReturnStatement returnStatement ) {}
}
