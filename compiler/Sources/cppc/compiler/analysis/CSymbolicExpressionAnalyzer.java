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

import cetus.hir.ArrayAccess;
import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.SizeofExpression;
import cetus.hir.Typecast;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.List;

public class CSymbolicExpressionAnalyzer extends SymbolicExpressionAnalyzer {

  CSymbolicExpressionAnalyzer() {}

  protected Expression analyzeExpression( AssignmentExpression expr ) {
    // If this is a normal assignment, use superclass version
    if( expr.getOperator().equals( AssignmentOperator.NORMAL ) ) {
      return super.analyzeExpression( expr );
    }

    // Else, the right hand side cannot be folded normally. Construct a virtual
    // BinaryExpression to help folding correctly
    BinaryOperator operator = this.getEquivalentOperator( expr.getOperator() );
    BinaryExpression virtualExpr = new BinaryExpression(
      (Expression)expr.getLHS().clone(), operator,
      (Expression)expr.getRHS().clone() );
    Expression folded = this.analyzeExpression( virtualExpr );

    // Update known symbols list
    if( expr.getLHS() instanceof Identifier ) {
      List<Expression> value = new ArrayList<Expression>( 1 );
      value.add( folded );
      SymbolicAnalyzer.addKnownSymbol( knownSymbols, (Identifier)expr.getLHS(),
        value );
    }

    AssignmentExpression newExpr = new AssignmentExpression( expr.getLHS(),
      AssignmentOperator.NORMAL, folded );
    newExpr.setParens( false );

    // Call to super.analyzeExpression to ensure consistency of the symbols
    // table
    return super.analyzeExpression( newExpr );
  }

  protected Expression analyzeExpression( BinaryExpression expr ) {
    // Fold LHS
    Expression foldedLHS = this.analyzeExpression( expr.getLHS(),
      knownSymbols );

    // Fold RHS
    Expression foldedRHS = this.analyzeExpression( expr.getRHS(),
      knownSymbols );

    // Deal with multiexpressions
    BinaryExpression virtual = new BinaryExpression( foldedLHS,
      expr.getOperator(), foldedRHS );
    virtual.setParent( expr.getParent() );

    List<UnfoldedExpression> unfoldedExpressions = this.unfoldMultiExpressions(
      virtual );

    if( unfoldedExpressions != null ) {
      MultiExpression mexpr = new MultiExpression(
        unfoldedExpressions.get(0).getVar() );
      for( UnfoldedExpression unfolded: unfoldedExpressions ) {
        mexpr.addExpression( unfolded.getVarValue(), this.analyzeExpression(
          (BinaryExpression)unfolded.getExpression() ) );
      }

      return mexpr;
    }

    // If not both LHS and RHS are constants, return
    if( !(foldedLHS instanceof Literal) ||
      !(foldedRHS instanceof Literal ) ) {

      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    // Else, apply the binary operator
    BinaryOperator operator = expr.getOperator();

    if( operator.equals( BinaryOperator.BITWISE_INCLUSIVE_OR ) ) {
      // Cannot bitwise nothing, since binary representations are not portable
      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    if( operator.equals( BinaryOperator.SHIFT_LEFT ) ) {
      // Cannot shift left, since binary representations are not portable
      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    return super.analyzeExpression( expr );
  }

  protected Expression analyzeExpression( SizeofExpression sizeofExpression ) {
    // The Expression contained in a SizeofExpression can be null. That is
    // because the parameter passed to the sizeof() function in C doesn't even
    // get evaluated
    if( sizeofExpression.getExpression() != null ) {
      return this.analyzeExpression( sizeofExpression.getExpression(),
        knownSymbols );
    }

    return (Expression)sizeofExpression.clone();
  }

  protected Expression analyzeExpression( Typecast typecast ) {
    // The only way to access the Expression contained in the Typecast is
    // through getChildren().get( 0 )
    return this.analyzeExpression( (Expression)typecast.getChildren().get( 0 ),
      knownSymbols );
  }

  protected Expression analyzeExpression( UnaryExpression expr ) {
    UnaryOperator operator = expr.getOperator();

    // If the operator is an ADDRESS_OF, there's no need to fold anything since
    // the C compiler won't evaluate the expression either. And since we can't
    // obtain the address at compile time (we wouldn't want to do it anyway)
    // the best thing to do is just return a clone of the original expression
    // without further analysis.
    if( operator.equals( UnaryOperator.ADDRESS_OF ) ) {
      return (Expression)expr.clone();
    }

    // Fold the inner expression
    Expression folded = this.analyzeExpression( expr.getExpression(),
      knownSymbols );

    // If the inner expression is not constant, return
    if( !(folded instanceof Literal) ) {
      return new UnaryExpression( operator, folded );
    }

    // Else, apply the unary operator
    if( operator.equals( UnaryOperator.BITWISE_COMPLEMENT ) ) {
      // Cannot complement bitwise, since binary representations are not
      // portable
      return new UnaryExpression( operator, folded );
    }

    if( operator.equals( UnaryOperator.PRE_DECREMENT ) ) {
      // Update value, return new
      BinaryExpression virtualExpr = new BinaryExpression( folded,
        BinaryOperator.SUBTRACT, new IntegerLiteral( 1 ) );
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr );
      virtualAssignment.setParent( expr.getParent() );
      folded = this.analyzeExpression( virtualAssignment, knownSymbols );

      folded.setParent( null );
      return folded;
    }

    if( operator.equals( UnaryOperator.PRE_INCREMENT ) ) {
      // Update value, return new
      BinaryExpression virtualExpr = new BinaryExpression( folded,
        BinaryOperator.ADD, new IntegerLiteral( 1 ) );
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr );
      virtualAssignment.setParent( expr.getParent() );
      folded = this.analyzeExpression( virtualAssignment, knownSymbols );

      folded.setParent( null );
      return folded;
    }

    if( operator.equals( UnaryOperator.POST_DECREMENT ) ) {
      // Update value, return current
      BinaryExpression virtualExpr = new BinaryExpression( folded,
        BinaryOperator.SUBTRACT, new IntegerLiteral( 1 ) );
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr );
      virtualAssignment.setParent( expr.getParent() );
      this.analyzeExpression( virtualAssignment, knownSymbols );

      folded.setParent( null );
      return folded;
    }

    if( operator.equals( UnaryOperator.POST_INCREMENT ) ) {
      // Update value, return new
      BinaryExpression virtualExpr = new BinaryExpression( folded,
        BinaryOperator.ADD, new IntegerLiteral( 1 ) );
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr );
      virtualAssignment.setParent( expr.getParent() );
      this.analyzeExpression( virtualAssignment, knownSymbols );

      folded.setParent( null );
      return folded;
    }

    return super.analyzeExpression( expr );
  }

  protected Expression foldArrayValue( ArrayAccess expr ) {
    // Now, if this array is constant, fold it
    Expression name = expr.getArrayName();
    if( !(name instanceof Identifier) ) {
      // This is not a variable (probably a pointer dereference)
      return null;
    }
    
    Identifier base = (Identifier)expr.getArrayName();
    if( knownSymbols.containsKey( base ) ) {

      // First n-1 dimensions: iterate through the inner initializers
      List values = (List)knownSymbols.get( base );
      List<Expression> indexes = this.foldArrayIndexes( expr );

      if( indexes == null ) {
        return null;
      }

      for( int i = 0; i < indexes.size()-1; i++ ) {
        Expression index = indexes.get( i );

        // Apply index i
        int iindex = (int)((IntegerLiteral)index).getValue();
        values = ((Initializer)values.get( iindex )).getChildren();
      }

      // Last dimension: fold access
      Expression index = indexes.get( indexes.size() - 1 );
      if( index instanceof IntegerLiteral ) {
        int iindex = (int)((IntegerLiteral)index).getValue();
        Expression folded = (Expression)values.get( iindex );
        return folded;
      }
    }

    return null;
  }

  protected Expression applyLogicalAnd( Expression lhs, Expression rhs ) {
    if( !(lhs instanceof IntegerLiteral) ||
      !(rhs instanceof IntegerLiteral) ) {

      System.out.println( "WARNING: line " + lhs.getStatement().where() +
        " trying to fold logical and applied to non-boolean operands." );
      return new BinaryExpression( lhs, BinaryOperator.LOGICAL_AND, rhs );
    }

    IntegerLiteral safeLhs = (IntegerLiteral)lhs;
    IntegerLiteral safeRhs = (IntegerLiteral)rhs;

    if( (safeLhs.getValue() == 1 ) &&
      (safeRhs.getValue() == 1 ) ) {

      return new IntegerLiteral( 1 );
    } else {
      return new IntegerLiteral( 0 );
    }
  }

  protected Expression applyLogicalNegation( Expression expr ) {

    if( !(expr instanceof IntegerLiteral) ) {
      System.out.println( "WARNING: line " + expr.getStatement().where() +
        " trying to fold logical negation applied to non-integer operand." );
      return expr;
    }

    IntegerLiteral safeExpr = (IntegerLiteral)expr;
    return buildBooleanLiteral( safeExpr.getValue() == 0 );
  }

  protected Expression applyLogicalOr( Expression lhs, Expression rhs ) {
    if( !(lhs instanceof IntegerLiteral) ||
      !(rhs instanceof IntegerLiteral) ) {

      System.out.println( "WARNING: line " + lhs.getStatement().where() +
        " trying to fold logical or applied to non-boolean operands." );
      return new BinaryExpression( lhs, BinaryOperator.LOGICAL_OR, rhs );
    }

    IntegerLiteral safeLhs = (IntegerLiteral)lhs;
    IntegerLiteral safeRhs = (IntegerLiteral)rhs;

    return buildBooleanLiteral( !(safeLhs.getValue()==0) ||
      !(safeRhs.getValue()==0) );
  }

  protected Expression buildBooleanLiteral( boolean b ) {
    if( b ) {
      return new IntegerLiteral( 1 );
    } else {
      return new IntegerLiteral( 0 );
    }
  }

  private BinaryOperator getEquivalentOperator( AssignmentOperator op ) {

    if( op.equals( AssignmentOperator.ADD ) ) {
      return BinaryOperator.ADD;
    }

    if( op.equals( AssignmentOperator.BITWISE_AND ) ) {
      return BinaryOperator.BITWISE_AND;
    }

    if( op.equals( AssignmentOperator.BITWISE_EXCLUSIVE_OR ) ) {
      return BinaryOperator.BITWISE_EXCLUSIVE_OR;
    }

    if( op.equals( AssignmentOperator.BITWISE_INCLUSIVE_OR ) ) {
      return BinaryOperator.BITWISE_INCLUSIVE_OR;
    }

    if( op.equals( AssignmentOperator.DIVIDE ) ) {
      return BinaryOperator.DIVIDE;
    }

    if( op.equals( AssignmentOperator.MODULUS ) ) {
      return BinaryOperator.MODULUS;
    }

    if( op.equals( AssignmentOperator.MULTIPLY ) ) {
      return BinaryOperator.MULTIPLY;
    }

    if( op.equals( AssignmentOperator.SHIFT_LEFT ) ) {
      return BinaryOperator.SHIFT_LEFT;
    }

    if( op.equals( AssignmentOperator.SHIFT_RIGHT ) ) {
      return BinaryOperator.SHIFT_RIGHT;
    }

    if( op.equals( AssignmentOperator.SUBTRACT ) ) {
      return BinaryOperator.SUBTRACT;
    }

    return AssignmentOperator.NORMAL;
  }

  protected boolean initializeArrayValue( ArrayAccess access ) {
    // Find array specs
    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        access, (Identifier)access.getArrayName() );
      } catch( SymbolIsNotVariableException e ) {
      return false;
    } catch( SymbolNotDefinedException e ) {
      return false;
    }

    VariableDeclarator vdeclarator = null;
    for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
      if( vd.getDeclarator( i ).getSymbol().equals(
        access.getArrayName() ) ) {

        vdeclarator = (VariableDeclarator)vd.getDeclarator( i );
        break;
      }
    }

    if( vdeclarator == null ) {
      return false;
    }

    List<ArraySpecifier> aspecs =
      (List<ArraySpecifier>)vdeclarator.getTrailingSpecifiers();

    if( aspecs.isEmpty() ) {
      return false;
    }

    ArraySpecifier aspec = aspecs.get( 0 );
    List values = this.createValuesList( aspec, 0, null, 0 );

    if( values == null ) {
      return false;
    }

    SymbolicAnalyzer.addKnownSymbol( knownSymbols,
      (Identifier)access.getArrayName(), (List<Expression>)values );
    return true;
  }

  private List createValuesList( ArraySpecifier aspec, int index,
    List values, int valuesSize ) {

    Expression size = aspec.getDimension( index );
    Expression foldedSize = this.analyzeExpression( size, knownSymbols );
    if( !(foldedSize instanceof IntegerLiteral) ) {
      return null;
    }

    int intSize = (int)((IntegerLiteral)foldedSize).getValue();

    if( values == null ) {
      values = new ArrayList( intSize );

      if( index == aspec.getNumDimensions() - 1 ) {
        for( int i = 0; i < intSize; i++ ) {
          values.add( null );
        }
      } else {
        if( this.createValuesList( aspec, index+1, values, intSize ) == null ) {
          return null;
        }
      }

      return values;
    }

    for( int i = 0; i < valuesSize; i++ ) {
      List innerValues = new ArrayList( intSize );
      values.add( innerValues );

      if( index < aspec.getNumDimensions() - 1 ) {
        if( this.createValuesList( aspec, index+1, values, intSize ) == null ) {
          return null;
        }
      } else {
        for( int j = 0; j < intSize; j++ ) {
          innerValues.add( null );
        }
      }
    }

    return values;
  }
}