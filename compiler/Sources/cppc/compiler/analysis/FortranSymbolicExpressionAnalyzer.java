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
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.BooleanLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.ImpliedDoLoop;
import cppc.compiler.cetus.IOCall;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.fortran.FortranArraySpecifier;
import cppc.compiler.fortran.SubstringExpression;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.List;

public class FortranSymbolicExpressionAnalyzer extends
  SymbolicExpressionAnalyzer {

  FortranSymbolicExpressionAnalyzer() {}

  private IntegerLiteral foldArrayIndex( FortranArraySpecifier aspec,
    IntegerLiteral index ) {

    int lowerBound = (int)((IntegerLiteral)aspec.getLowerBound()).getValue();
    int iindex = (int)index.getValue();

    iindex -= lowerBound;

    return new IntegerLiteral( iindex );
  }

  private List<Expression> innerConvertArrayIndexes( ArrayAccess expr,
    List<FortranArraySpecifier> aspecs ) {

    if( aspecs.size() != expr.getNumIndices() ) {
      System.err.println( "ERROR: Access to array doesn't use the same number "+
        "of indexes as its definition." );
      System.exit( 0 );
    }

    List<Expression> convertedIndexes = new ArrayList<Expression>(
      expr.getNumIndices() );
    for( int i = 0; i < expr.getNumIndices(); i++ ) {
      if( !(expr.getIndex( i ) instanceof IntegerLiteral) ) {
        return null;
      }

      convertedIndexes.add( this.foldArrayIndex( aspecs.get( i ),
        (IntegerLiteral)expr.getIndex( i ) ) );
    }

    return convertedIndexes;
  }

  private List<FortranArraySpecifier> getAspecs( ArrayAccess expr ) {
    //Get symbol tables
    Identifier base = (Identifier)expr.getArrayName();

    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        expr, base );
    } catch( SymbolNotDefinedException e ) {
      return null;
    } catch( SymbolIsNotVariableException e) {
      return null;
    }

    VariableDeclarator vdeclarator = null;
    for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
      if( vd.getDeclarator( i ).getSymbol().equals( base ) ) {
        vdeclarator = (VariableDeclarator)vd.getDeclarator( i );
        break;
      }
    }

    // No declarator: no index
    if( vdeclarator == null ) {
      return null;
    }

    // Get array specifiers
    List<FortranArraySpecifier> aspecs = new ArrayList<FortranArraySpecifier>(
      vdeclarator.getTrailingSpecifiers() );
    return aspecs;
  }

  protected List<List<Expression>> convertArrayIndexes( ArrayAccess expr,
    List<List<Expression>> indexes ) {

    // Get array specifiers
    List<FortranArraySpecifier> aspecs = this.getAspecs( expr );

    // Find true index value: acess index - lower bound
    List<List<Expression>> convertedIndexes = new ArrayList<List<Expression>>(
      indexes.size() );
    for( List<Expression> index: indexes ) {
      ArrayAccess virtual = new ArrayAccess(
        (Identifier)expr.getArrayName().clone(), index );
      virtual.setParent( expr.getParent() );
      convertedIndexes.add( this.innerConvertArrayIndexes( virtual, aspecs ) );
    }

    return convertedIndexes;
  }

  protected List<Expression> convertArrayIndexes( ArrayAccess expr ) {
    // Get array specifiers
    List<FortranArraySpecifier> aspecs = this.getAspecs( expr );
    return this.innerConvertArrayIndexes( expr, aspecs );
  }

  protected Expression foldArrayValue( ArrayAccess expr ) {
    // Now, if this array is constant, fold it
    Identifier base = (Identifier)expr.getArrayName();

    // Unfold multiexpressions
    List<UnfoldedExpression> unfoldedExpressions = this.unfoldMultiExpressions(
      expr );

    if( unfoldedExpressions == null ) {
      if( knownSymbols.containsKey( base ) ) {
        // First (n-1) dimensions: iterate through the inner values
        List values = (List)knownSymbols.get( base );
        List<Expression> indexes = this.foldArrayIndexes( expr );

        if( indexes == null ) {
          return null;
        }

        ArrayAccess virtualAccess = new ArrayAccess(
          (Identifier)expr.getArrayName(), indexes );
        virtualAccess.setParent( expr );
        indexes = this.convertArrayIndexes( virtualAccess );

        for( int i = 0; i < indexes.size() - 1; i++ ) {
          Expression index = indexes.get( i );

          if( !(index instanceof IntegerLiteral) ) {
            return null;
          }

          // Apply index i
          int iindex = (int)((IntegerLiteral)index).getValue();
          values = ((List)values.get( iindex ));
        }

        // Last dimension: fold access
        Expression index = indexes.get( indexes.size() - 1 );
        if( index instanceof IntegerLiteral ) {
          int iindex = (int)((IntegerLiteral)index).getValue();

          if( values.get( iindex ) != null ) {
            return (Expression)((Expression)values.get( iindex )).clone();
          }

          return null;
        }
      }
    } else {
      UnfoldedExpression first = unfoldedExpressions.get( 0 );
      MultiExpression returnExpression = new MultiExpression( first.getVar() );
      for( UnfoldedExpression unfolded: unfoldedExpressions ) {
        Expression foldedValue = this.foldArrayValue( (ArrayAccess)
          unfolded.getExpression() );

        if( foldedValue == null ) {
          return null;
        }

        returnExpression.addExpression( unfolded.getVarValue(),
          (Expression)foldedValue.clone() );
      }

      return SymbolicExpressionAnalyzer.filterMultiExpression(returnExpression);
    }

    return null;
  }

  protected Expression analyzeExpression( BinaryExpression expr ) {
    if( !expr.getOperator().equals( BinaryOperator.F_CONCAT ) &&
      !expr.getOperator().equals( BinaryOperator.F_POWER ) ) {

      return super.analyzeExpression( expr );
    }

    // Fold LHS
    Expression foldedLHS = this.analyzeExpression( expr.getLHS(),
      knownSymbols );

    // Fold RHS
    Expression foldedRHS = this.analyzeExpression( expr.getRHS(),
      knownSymbols );

    // If not both LHS and RHS are constants, return
    if( !(foldedLHS instanceof Literal) ||
      !(foldedRHS instanceof Literal ) ) {

      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    // Else, apply the binary operator
    BinaryOperator operator = expr.getOperator();
    Expression newExpr = null;

    if( operator.equals( BinaryOperator.F_CONCAT ) ) {
      if( (foldedLHS instanceof StringLiteral) &&
        (foldedRHS instanceof StringLiteral) ) {

        String lhs = ((StringLiteral)foldedLHS).getValue();
        String rhs = ((StringLiteral)foldedRHS).getValue();
        newExpr = new StringLiteral( lhs + rhs );
        return newExpr;
      }

      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    if( operator.equals( BinaryOperator.F_POWER ) ) {
      // Apply only to integers to avoid precision errors
      if( (foldedLHS instanceof IntegerLiteral) &&
        (foldedRHS instanceof IntegerLiteral) ) {

        long lhs = ((IntegerLiteral)foldedLHS).getValue();
        long rhs = ((IntegerLiteral)foldedRHS).getValue();
        newExpr = new IntegerLiteral( (long)Math.pow( lhs, rhs ) );
        return newExpr;
      }
      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    return super.analyzeExpression( expr );
  }

  protected Expression analyzeExpression( FunctionCall call ) {
    // For processing Fortran 77 functions such as sqrt, etc.
    String fName = call.getName().toString();

    if( !fName.equals( "DBLE" ) && !fName.equals( "DINT" ) &&
      !fName.equals( "DSQRT" ) && !fName.equals( "MOD" ) &&
      !fName.equals( "SQRT" ) && !fName.equals( "LOG" ) ) {

      return super.analyzeExpression( call );
    }

    // Fold all arguments
    FunctionCall virtualCall = new FunctionCall( new Identifier( fName ) );
    for( int i = 0; i < call.getNumArguments(); i++ ) {
      virtualCall.addArgument( this.analyzeExpression( call.getArgument( i ),
          knownSymbols ) );
    }
    virtualCall.setParent( call.getParent() );

    // If there are any multi expressions, unfold and recursive calls
    List<UnfoldedExpression> unfoldedExpressions =
      this.unfoldMultiExpressions( virtualCall );
    if( unfoldedExpressions != null ) {
      MultiExpression mexpr = new MultiExpression(
        unfoldedExpressions.get(0).getVar() );
      for( UnfoldedExpression unfolded: unfoldedExpressions ) {
        mexpr.addExpression( unfolded.getVarValue(), this.analyzeExpression(
          (FunctionCall)unfolded.getExpression() ) );
      }

      return mexpr;
    }

    if( fName.equals( "DBLE" ) ) {
      // Convert to double
      Expression param = call.getArgument( 0 );
      Expression folded = this.analyzeExpression( param, knownSymbols );
      Expression doubleValue =
        SymbolicExpressionAnalyzer.convertToDouble( folded );
      if( doubleValue instanceof Literal ) {
        return doubleValue;
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( doubleValue );
      return retValue;
    }

    if( fName.equals( "DINT" ) ) {
      // Truncate double value. Return as double.
      Expression parameter = call.getArgument( 0 );
      Expression folded = this.analyzeExpression( parameter, knownSymbols );
      if( folded instanceof Literal ) {
        double doubleValue = this.numericAsDouble( folded );
        double dintValue = Math.floor( doubleValue );
        return new DoubleLiteral( dintValue );
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( folded );
      return retValue;
    }

    if( fName.equals( "DSQRT" ) ) {
      // Square root returning a double value
      Expression parameter = call.getArgument( 0 );
      Expression folded = this.analyzeExpression( parameter, knownSymbols );
      if( folded instanceof Literal ) {
        double doubleValue = this.numericAsDouble( folded );
        double sqrtValue = Math.sqrt( doubleValue );
        return new DoubleLiteral( sqrtValue );
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( folded );
      return retValue;
    }

    if( fName.equals( "LOG" ) ) {
      // Neperian log
      Expression parameter = call.getArgument( 0 );
      Expression folded = this.analyzeExpression( parameter, knownSymbols );
      if( folded instanceof Literal ) {
        double doubleValue = this.numericAsDouble( folded );
        double logValue = Math.log( doubleValue );
        return new DoubleLiteral( logValue );
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( folded );
      return retValue;
    }

    if( fName.equals( "MOD" ) ) {
      // Try to avoid precision errors
      Expression lhs = call.getArgument( 0 );
      Expression rhs = call.getArgument( 1 );
      Expression foldedLHS = this.analyzeExpression( lhs, knownSymbols );
      Expression foldedRHS = this.analyzeExpression( rhs, knownSymbols );

      if( (foldedLHS instanceof Literal) &&
        (foldedRHS instanceof Literal) ) {

        double lhsValue = this.numericAsDouble( foldedLHS );
        double rhsValue = this.numericAsDouble( foldedRHS );
        double opValue = lhsValue % rhsValue;

        return this.binaryOperationResult( foldedLHS, foldedRHS, opValue );
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( foldedLHS );
      retValue.addArgument( foldedRHS );
      return retValue;
    }

    if( fName.equals( "SQRT" ) ) {
      // Square Root
      Expression parameter = call.getArgument( 0 );
      Expression folded = this.analyzeExpression( parameter, knownSymbols );
      if( folded instanceof Literal ) {
        double doubleValue = this.numericAsDouble( folded );
        double sqrtValue = Math.sqrt( doubleValue );
        return new FloatLiteral( sqrtValue );
      }

      FunctionCall retValue = new FunctionCall(
        (Identifier)call.getName().clone() );
      retValue.addArgument( folded );
      return retValue;
    }

    return super.analyzeExpression( call );
  }

  // FIXME: Is this necessary at all?
  protected Expression analyzeExpression( IOCall ioCall ) {
    // Fold parameters
    for( Expression expr: ioCall.getParameters() ) {
      this.analyzeExpression( expr, knownSymbols );
    }

    // Fold varargs
    for( Expression expr: ioCall.getVarargs() ) {
      this.analyzeExpression( expr, knownSymbols );
    }

    return (Expression)ioCall.clone();
  }

  protected Expression analyzeExpression(
    SubstringExpression substringExpression ) {

    // Try to fold the indexes
    Expression foldedLB = this.analyzeExpression(
      substringExpression.getLBound(), knownSymbols );
    Expression foldedUB = this.analyzeExpression(
      substringExpression.getUBound(), knownSymbols );

    // If both the string and the indexes are constant: automatically fold
    if( knownSymbols.containsKey( substringExpression.getStringName() ) ) {
      if( (foldedLB instanceof IntegerLiteral) &&
        (foldedUB instanceof IntegerLiteral) ) {

        List values = (List)knownSymbols.get(
          substringExpression.getStringName() );
        StringLiteral string = (StringLiteral)values.get( 0 );
        String value = string.getValue();
        IntegerLiteral lbound = (IntegerLiteral)foldedLB;
        IntegerLiteral ubound = (IntegerLiteral)foldedUB;

        // If this does not hold, it will be handled later by this function
        if( (lbound.getValue() <= ubound.getValue()) &&
          (ubound.getValue() <= value.length() ) ) {

          StringLiteral newExpr = new StringLiteral( value.substring(
            (int)lbound.getValue()-1, (int)ubound.getValue() ) );

          return newExpr;
        }
      }
    }

    // Else, if ubound < lbound, switch for an empty string
    Expression lbound = foldedLB;
    Expression ubound = foldedUB;

    if( (lbound instanceof IntegerLiteral) &&
        (ubound instanceof IntegerLiteral) ) {

      IntegerLiteral safeLBound = (IntegerLiteral)lbound;
      IntegerLiteral safeUBound = (IntegerLiteral)ubound;

      if( safeUBound.getValue() < safeLBound.getValue() ) {
        Expression newExpr = new StringLiteral( "" );

        return newExpr;
      }
    }

    return (Expression)substringExpression.clone();
  }

  // FIXME: Take a look at this
  protected Expression analyzeExpression( ImpliedDoLoop impliedDo ) {
//     // Fold expressions
//     for( Expression expr: impliedDo.getExpressions() ) {
//       this.fold( expr, constants );
//     }
// 
//     this.fold( impliedDo.getStart(), constants );
//     this.fold( impliedDo.getStop(), constants );
//     if( impliedDo.getStep() != null ) {
//       this.fold( impliedDo.getStep(), constants );
//     }

    return (Expression)impliedDo.clone();
  }

  protected Expression applyLogicalAnd( Expression lhs, Expression rhs ) {
    if( !(lhs instanceof BooleanLiteral) ||
      !(rhs instanceof BooleanLiteral) ) {

      System.out.println( "WARNING: line " + lhs.getStatement().where() +
        " trying to fold logical and applied to non-boolean operands." );
      return new BinaryExpression( lhs, BinaryOperator.LOGICAL_AND, rhs );
    }

    BooleanLiteral safeLhs = (BooleanLiteral)lhs;
    BooleanLiteral safeRhs = (BooleanLiteral)rhs;

    boolean value = safeLhs.getValue() && safeRhs.getValue();
    return new BooleanLiteral( value );
  }

  protected Expression applyLogicalNegation( Expression expr ) {

    if( !(expr instanceof BooleanLiteral) ) {
      System.out.println( "WARNING: line " + expr.getStatement().where() +
        " trying to fold logical negation applied to non-boolean operand." );

      return expr;
    }

    BooleanLiteral safeExpr = (BooleanLiteral)expr;
    return new BooleanLiteral( !safeExpr.getValue() );
  }

  protected Expression applyLogicalOr( Expression lhs, Expression rhs ) {
    if( !(lhs instanceof BooleanLiteral) ||
      !(rhs instanceof BooleanLiteral) ) {

      System.out.println( "WARNING: line " + lhs.getStatement().where() +
        " trying to fold logical or applied to non-boolean operands." );
      return new BinaryExpression( lhs, BinaryOperator.LOGICAL_OR, rhs );
    }

    BooleanLiteral safeLhs = (BooleanLiteral)lhs;
    BooleanLiteral safeRhs = (BooleanLiteral)rhs;

    boolean value = safeLhs.getValue() || safeRhs.getValue();
    return new BooleanLiteral( value );
  }

  protected Expression buildBooleanLiteral( boolean b ) {
    return new BooleanLiteral( b );
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

    List<FortranArraySpecifier> aspecs = (List<FortranArraySpecifier>)
      vdeclarator.getTrailingSpecifiers();

    if( aspecs.isEmpty() ) {
      return false;
    }

    List values = this.createValuesList( aspecs, 0, null, 0 );

    if( values == null ) {
      return false;
    }

    knownSymbols.put( (Identifier)access.getArrayName(), values );
//     SymbolicAnalyzer.addKnownSymbol( knownSymbols,
//       (Identifier)access.getArrayName(), (List<Expression>)values );
    return true;
  }

  private List createValuesList( List<FortranArraySpecifier> aspecs,
    int index, List values, int valuesSize ) {

    FortranArraySpecifier aspec = aspecs.get( index );

    Expression lbound = aspec.getLowerBound();
    Expression foldedLBound = this.analyzeExpression( lbound, knownSymbols );
    if( !(foldedLBound instanceof IntegerLiteral) ) {
      return null;
    }

    Expression ubound = aspec.getUpperBound();
    Expression foldedUBound = this.analyzeExpression( ubound, knownSymbols );
    if( !(foldedUBound instanceof IntegerLiteral) ) {
      return null;
    }

    int intLBound = (int)((IntegerLiteral)foldedLBound).getValue();
    int intUBound = (int)((IntegerLiteral)foldedUBound).getValue();
    int size = intUBound - intLBound +1;

    if( values == null ) {
      values = new ArrayList( size );

      if( index == aspecs.size() - 1 ) {
        for( int i = 0; i < size; i++ ) {
          values.add( null );
        }
      } else {
        if( this.createValuesList( aspecs, index + 1, values, size ) == null ) {
          return null;
        }
      }
      return values;
    }

    for( int i = 0; i < valuesSize; i++ ) {
      List innerValues = new ArrayList( size );
      values.add( innerValues );

      if( index < aspecs.size() - 1 ) {
        if( this.createValuesList( aspecs, index+1, innerValues, size ) ==
          null ) {

          return null;
        }
      } else {
        for( int j = 0; j < size; j++ ) {
          innerValues.add( null );
        }
      }
    }

    return values;
  }
}
