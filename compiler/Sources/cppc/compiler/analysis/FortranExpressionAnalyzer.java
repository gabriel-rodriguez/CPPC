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

import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;

import cppc.compiler.cetus.ImpliedDoLoop;
import cppc.compiler.cetus.IOCall;
import cppc.compiler.fortran.SubstringExpression;

import java.util.HashSet;
import java.util.Set;

public class FortranExpressionAnalyzer extends ExpressionAnalyzer {

    FortranExpressionAnalyzer() {}

    protected Set<Identifier> analyzeExpression( IOCall ioCall ) {

      // We build a virtual FunctionCall with both the IOCall parameters and
      // varargs as parameters
      FunctionCall virtualCall = new FunctionCall(
        (Identifier)ioCall.getName().clone() );

      // Thus we appropiately simulate that the virtualCall is the IOCall
      virtualCall.setParent( ioCall.getParent() );

      for( Expression expr: ioCall.getParameters() ) {
        virtualCall.addArgument( (Expression)expr.clone() );
      }
      for( Expression expr: ioCall.getVarargs() ) {
        virtualCall.addArgument( (Expression)expr.clone() );
      }

      // We analyze this virtual function as a normal FunctionCall
      return analyzeExpression( virtualCall );
    }

    protected Set<Identifier> analyzeExpression(
      SubstringExpression substringExpression ) {

      // The name of the String will always be consumed
      Set<Identifier> consumed = new HashSet<Identifier>();
      consumed.add( (Identifier)substringExpression.getStringName().clone() );

      // Now we need to add what is consumed by the lower and upper bounds
      consumed.addAll( analyzeExpression( currentStatement,
        substringExpression.getLBound() ) );
      consumed.addAll( analyzeExpression( currentStatement,
        substringExpression.getUBound() ) );

      return consumed;
    }

    protected Set<Identifier> analyzeExpression( ImpliedDoLoop impliedDo ) {

      // A Fortran Implied Do Loop does not generate anything but it's loop
      // variable
      currentStatement.getGenerated().add( impliedDo.getDoVar() );
      currentStatement.getPartialGenerated().add( impliedDo.getDoVar() );

      // And it consumes everything consumed in it's expressions...
      Set<Identifier> consumed = new HashSet<Identifier>();
      for( Expression expr: impliedDo.getExpressions() ) {
        consumed.addAll( analyzeExpression( currentStatement, expr ) );
      }

      // ...and everything consumed in the beginning and ending expression of
      // the loop...
      consumed.addAll( analyzeExpression( currentStatement,
        impliedDo.getStart() ) );
      consumed.addAll( analyzeExpression( currentStatement,
        impliedDo.getStop() ) );

      // ...except the loop variable
      consumed.remove( impliedDo.getDoVar() );

      return consumed;
    }
}
