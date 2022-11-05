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

import cetus.hir.AccessExpression;
import cetus.hir.ConditionalExpression;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.SizeofExpression;
import cetus.hir.Typecast;

import cppc.compiler.cetus.CppcStatement;

import java.util.HashSet;
import java.util.Set;

public class CExpressionAnalyzer extends ExpressionAnalyzer {

    CExpressionAnalyzer() {}

    protected Set<Identifier> analyzeExpression( AccessExpression expr ) {
      return ExpressionAnalyzer.analyzeExpression( currentStatement,
        expr.getLHS() );
    }

    protected Set<Identifier> analyzeExpression( ConditionalExpression expr ) {
      Set<Identifier> resultSet = this.analyzeExpression( currentStatement,
        expr.getCondition() );
      resultSet.addAll( this.analyzeExpression( currentStatement,
        expr.getTrueExpression() ) );
      resultSet.addAll( this.analyzeExpression( currentStatement,
        expr.getFalseExpression() ) );

      return resultSet;
    }

    protected Set<Identifier> analyzeExpression(
      SizeofExpression sizeofExpression ) {

      // The Expression contained in a SizeofExpression can be null. That is
      // because the parameter passed to the sizeof() function in C doesn't even
      // get evaluated
      if( sizeofExpression.getExpression() != null ) {
        return ExpressionAnalyzer.analyzeExpression( currentStatement,
          sizeofExpression.getExpression() );
      }

      return new HashSet<Identifier>( 0 );
    }

    protected Set<Identifier> analyzeExpression( Typecast typecast ) {
      // The only way to access the Expression contained in the Typecast is
      // through getChildren().get( 0 )
      return analyzeExpression( currentStatement,
        (Expression)typecast.getChildren().get( 0 ) );
    }

}