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
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.BooleanLiteral;
import cetus.hir.CharLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.DepthFirstIterator;
import cetus.hir.EscapeLiteral;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Procedure;
import cetus.hir.StringLiteral;
import cetus.hir.Traversable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class SymbolicExpressionAnalyzer extends
  FunctionDispatcher<Expression> {

  private static Class instanceClass;
  protected static SymbolicExpressionAnalyzer instance;
  protected static Map knownSymbols = null;

  static {
    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
          GlobalNames.SYMBOLIC_EXPRESSION_ANALYZER_CLASS_OPTION ) );
      instance = (SymbolicExpressionAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected SymbolicExpressionAnalyzer() {}

  protected static Expression filterMultiExpression( Expression expr ) {
    if( !(expr instanceof MultiExpression) ) {
      return expr;
    }

    MultiExpression mexpr = (MultiExpression)expr;
    Expression one = (Expression)mexpr.getChildren().get( 0 );
    if( mexpr.getChildren().size() == 1 ) {
      return (Expression)one.clone();
    }

    for( Expression value: (List<Expression>)mexpr.getChildren() ) {
      if( !value.equals( one ) ) {
        return mexpr;
      }
    }

    one.setParent( mexpr.getParent() );
    return (Expression)one.clone();
  }

  public static Expression analyzeExpression( Expression expr,
    Map knownSymbols ) {

    Method m = instance.dispatch( expr, "analyzeExpression" );

    if( m == null ) {
      System.err.println( "WARNING: "+
        "cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression " +
          "not implemented for " + expr.getClass() );

      return null;
    }

    Map oldSymbols = null;
    if( SymbolicExpressionAnalyzer.knownSymbols != knownSymbols ) {
      oldSymbols = SymbolicExpressionAnalyzer.knownSymbols;
      SymbolicExpressionAnalyzer.knownSymbols = knownSymbols;
    }
    try {
      Expression ret = (Expression)m.invoke( instance, expr );
      // Filter MultiExpressions with same values
      return SymbolicExpressionAnalyzer.filterMultiExpression( ret );
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    } finally {
      if( oldSymbols != null ) {
        SymbolicExpressionAnalyzer.knownSymbols = oldSymbols;
      }
    }

    return null;
  }

  private List<List<Expression>> expandIndexes(
    List<List<? extends Expression>> indexes ) {

    // Calculate total return list
    int indexesSize = 1;
    for( List<? extends Expression> index: indexes ) {
      indexesSize *= index.size();
    }

    // Create return list
    List<List<Expression>> expandedIndexes =
      new ArrayList<List<Expression>>( indexesSize );

    // Iterate over the indexes filling the indices array. The process consists
    // of taking the existing lists in the indices array, and cloning each of
    // them N times for adding each of the items in this index dimension.
    for( int i = indexes.size()-1; i >= 0; i-- ) {
      List<? extends Expression> index = indexes.get( i );
      if( expandedIndexes.isEmpty() ) {
        // If it is empty, this is the first dimension and we just add the
        // expressions we have
        for( Expression indexPart: index ) {
          List<Expression> expandedIndex =
            new ArrayList<Expression>( index.size() );
          if( indexPart instanceof UnfoldedExpression ) {
            expandedIndex.add(((UnfoldedExpression)indexPart).getExpression());
          } else {
            expandedIndex.add( indexPart );
          }
          expandedIndexes.add( expandedIndex );
        }
      } else {
        List<List<Expression>> masterClone =
          (List<List<Expression>>)((ArrayList)expandedIndexes).clone();
        expandedIndexes.clear();
        for( Expression indexPart: index ) {
          for( List<Expression> expandedIndex: masterClone ) {
            List<Expression> clone = (List<Expression>)
              ((ArrayList)expandedIndex).clone();
            if( indexPart instanceof UnfoldedExpression ) {
              //FIXME: This would be done faster if we used an structure other
              // than an array list, thought for inserting in a queue fashion
              clone.add( 0, ((UnfoldedExpression)indexPart).getExpression() );
            } else {
              clone.add( 0, indexPart );
            }
            expandedIndexes.add( clone );
          }
        }
      }
    }

    return expandedIndexes;
  }

  protected Expression analyzeExpression( ArrayAccess expr ) {
    // First: fold the access indexes
    List<Expression> foldedIndexes = this.foldArrayIndexes( expr );

    if( foldedIndexes == null ) {
      return (Expression)expr.clone();
    }

//     List<List<? extends Expression>> unfoldedIndexes =
//       new ArrayList<List<? extends Expression>>( expr.getNumIndices() );
//     for( Expression foldedIndex: foldedIndexes ) {
//       List<UnfoldedExpression> unfoldedIndex = this.unfoldMultiExpressions(
//         foldedIndex );
// 
//       if( unfoldedIndex == null ) {
//         List<Expression> thisIndex = new ArrayList<Expression>( 1 );
//         thisIndex.add( foldedIndex );
//         unfoldedIndexes.add( thisIndex );
//       } else {
//         unfoldedIndexes.add( unfoldedIndex );
//       }
//     }
// 
//     List<List<Expression>> expandedIndexes = this.expandIndexes(
//       unfoldedIndexes );

    // This method is to be extended in subclasses
    ArrayAccess foldedAccess = new ArrayAccess(
      (Expression)expr.getArrayName().clone(), foldedIndexes );
    foldedAccess.setParent( expr.getParent() );
    Expression folded = this.foldArrayValue( foldedAccess );
    if( folded != null ) {
      return folded;
    }

    foldedAccess.setParent( null );
    return foldedAccess;
  }

  protected List<List<Expression>> foldArrayIndexes( ArrayAccess expr,
    List<List<Expression>> indexes ) {

    List<List<Expression>> foldedIndexes = new ArrayList<List<Expression>>(
      indexes.size() );

    for( List<Expression> index: indexes ) {
      ArrayAccess virtual = new ArrayAccess(
        (Identifier)expr.getArrayName().clone(),
         index );
      virtual.setParent( expr.getParent() );
      foldedIndexes.add( this.foldArrayIndexes( virtual ) );
    }

    return foldedIndexes;
  }

  protected List<Expression> foldArrayIndexes( ArrayAccess expr ) {

    List<Expression> foldedIndexes = new ArrayList<Expression>(
      expr.getNumIndices() );

    for( int i = 0; i < expr.getNumIndices(); i++ ) {
      Expression foldedIndex = this.analyzeExpression( expr.getIndex( i ),
        knownSymbols );

      if( foldedIndex instanceof MultiExpression ) {
        for( int j = 0; j < foldedIndex.getChildren().size(); j++ ) {
          if( !(foldedIndex.getChildren().get( j ) instanceof
            IntegerLiteral) ) {

            return null;
          }
        }
      } else {
        if( !(foldedIndex instanceof IntegerLiteral) ) {
          return null;
        }
      }

      foldedIndexes.add( foldedIndex );
    }

    return foldedIndexes;
  }

  protected List<List<Expression>> convertArrayIndexes( ArrayAccess expr,
    List<List<Expression>> indexes ) {

    List<List<Expression>> convertedIndexes = new ArrayList<List<Expression>>(
      indexes.size() );

    long iter = 0;
    for( List<Expression> index: indexes ) {
      ArrayAccess virtual = new ArrayAccess(
        (Identifier)expr.getArrayName().clone(),
        index );
      virtual.setParent( expr.getParent() );
      convertedIndexes.add( this.convertArrayIndexes( virtual ) );
      iter++;
    }

    return convertedIndexes;
  }

  protected List<Expression> convertArrayIndexes( ArrayAccess expr ) {
    // This is equivalente to
    // return (List<Expression>)expr.getIndices().clone();
    // The problem is, ArrayAccess has no getIndices() method.
    List<Expression> convertedIndexes = new ArrayList<Expression>(
      expr.getNumIndices() );
    for( int i = 0; i < expr.getNumIndices(); i++ ) {
      convertedIndexes.add( expr.getIndex( i ) );
    }

    return convertedIndexes;
  }

  protected abstract Expression foldArrayValue( ArrayAccess expr );

  private List<ArrayAccess> unfoldAccess( ArrayAccess access ) {
    int totalSize = 1;
    for( int i = 0; i < access.getNumIndices(); i++ ) {
      if( access.getIndex(i) instanceof CommaExpression ) {
        totalSize *=
          ((CommaExpression)access.getIndex(i)).getChildren().size();
      }
    }

    List<ArrayAccess> virtualAccesses = new ArrayList<ArrayAccess>( totalSize );
    if( totalSize != 1 ) {
      for( int i = 0; i < access.getNumIndices(); i++ ) {
        Expression index = access.getIndex( i );
        if( index instanceof CommaExpression ) {
          List<Expression> virtualIndexes = new ArrayList<Expression>(
          access.getNumIndices() );

          for( int j = 0; j < access.getNumIndices(); j++ ) {
            virtualIndexes.add( access.getIndex( j ) );
          }

          for( Expression exprIndex: (List<Expression>)
            ((CommaExpression)index).getChildren() ) {

            virtualIndexes.set( i, exprIndex );
            ArrayAccess virtual = new ArrayAccess(
              (Identifier)access.getArrayName().clone(), virtualIndexes );
            virtualAccesses.addAll( this.unfoldAccess( virtual ) );
          }

          return virtualAccesses;
        }
      }
    }

    virtualAccesses.add( access );
    return virtualAccesses;
  }

  protected static List<UnfoldedExpression> unfoldMultiExpressions(
    Expression expr ) {

    DepthFirstIterator iter = new DepthFirstIterator( expr );
    Identifier var = null;
    MultiExpression mexpr = null;
    while( iter.hasNext() ) {
      try {
        mexpr = (MultiExpression)iter.next( MultiExpression.class );
        var = mexpr.getVar();
        break;
      } catch( NoSuchElementException e ) {}
    }

    if( var == null ) {
      return null;
    }

    List<UnfoldedExpression> returnList = new ArrayList<UnfoldedExpression>(
      mexpr.getChildren().size() );

    // Replace inner multiexpressions depending on the same variable than the
    // selected one with the list of possible values depending on the variable.
    // The algorithm first finds multiexpression which are to be swapped, and
    // stores them in the "swaps" list.
    Expression clone = (Expression)expr.clone();
    clone.setParent( expr.getParent() );
    iter = new DepthFirstIterator( clone );
    List<Expression> swaps = new ArrayList<Expression>();
    while( iter.hasNext() ) {
      try {
        MultiExpression innerMexpr = (MultiExpression)iter.next(
          MultiExpression.class );
        if( innerMexpr.getVar().equals( var ) ) {
          swaps.add( innerMexpr );
        }
      } catch( NoSuchElementException e ) {}
    }

    // Now, it is necessary to create a clone of the expression to be unfolded
    // for each possible value of the unfolding variable. However, if the clone
    // is created prior to the substitution, we will be cloning potentially
    // huge multiexpressions just to have them replaced with a scalar value
    // (once per each possible value of the unfolding variable). In order to
    // avoid this, we have previously created a master clone, and identified
    // the objects inside it to be substituted. For each possible value of the
    // unfolding variable, we swap the objects in the master clone, and create
    // a clone of it afterwards. Therefore, we are just cloning the scalar
    // values after the substitution. The scalar values in the master clone are
    // introduced in the "swaps" list instead of the old, discarded ones, so
    // that future copies are accurate.
    List<Expression> originalSwaps = new ArrayList<Expression>( swaps );
    List<Expression> newSwaps = new ArrayList<Expression>( swaps.size() );
    for( Expression key: mexpr.getValueSet() ) {
      for( int i = 0; i < originalSwaps.size(); i++ ) {
        Expression swapIn = (Expression)
          ((MultiExpression)originalSwaps.get(i)).getValue( key ).clone();
        Expression swap = swaps.get( i );
        if( clone == swap ) {
          // If clone and swap are the same, then the original expression
          // passed to this function was the very MultiExpression we are
          // unfolding. Clone must become each of the values in the return list
          // for the rest of the code to work.
          clone = swapIn;
        } else {
          swap.swapWith( swapIn );
        }
        newSwaps.add( swapIn );
      }

      swaps = newSwaps;
      newSwaps = new ArrayList<Expression>( swaps.size() );
      Traversable parent = clone.getParent();
      clone.setParent( null );
      UnfoldedExpression unfolded = new UnfoldedExpression( var, key,
        (Expression)clone.clone() );
      clone.setParent( parent );
      unfolded.setParent( parent );
      returnList.add( unfolded );
    }

    return returnList;
  }

  private void assignToList( List list, Literal value ) {
    for( int i = 0; i < list.size(); i++ ) {
      if( list.get( i ) instanceof List ) {
        this.assignToList( (List)list.get(i), value );
      } else {
        list.set( i, value );
      }
    }
  }

  private void fastArrayAssignment( ArrayAccess access,
    List<Expression> indexes, Literal rhsValue ) {

    List<List<? extends Expression>> unfoldedIndexes =
      new ArrayList<List<? extends Expression>>( indexes.size() );

    // Unfold the indexes
    for(Expression index: indexes ) {
      List<UnfoldedExpression> unfoldedIndex = this.unfoldMultiExpressions(
        index );
      if( unfoldedIndex == null ) {
        List<Expression> thisIndex = new ArrayList<Expression>( 1 );
        thisIndex.add( index );
        unfoldedIndexes.add( thisIndex );
      } else {
        unfoldedIndexes.add( unfoldedIndex );
      }
    }

    // Get the total indices number and build array for them
    int indicesSize = 1;
    for( List<? extends Expression> unfoldedIndex: unfoldedIndexes ) {
      indicesSize *= unfoldedIndex.size();
    }

    // Get total array size
    Expression totalSize = VariableSizeAnalizerFactory.getAnalizer().getSize(
      (Identifier)access.getArrayName(), access );
    int intSize = 0;
    if( totalSize == null ) {
      intSize = 1;
    } else {
      totalSize = this.analyzeExpression( totalSize, this.knownSymbols );
      if( totalSize instanceof IntegerLiteral ) {
        intSize = (int)((IntegerLiteral)totalSize).getValue();
      }
    }

    // If size of assignment and array match: total assignment. Skip index
    // generation phase and just assign rhs to EVERY position in the known
    // symbols list
    if( indicesSize == intSize ) {
      List values = (List)knownSymbols.get( (Identifier)access.getArrayName() );
      this.assignToList( values, rhsValue );
      return;
    }

    List<List<Expression>> indices = this.expandIndexes( unfoldedIndexes );

    // Fold and convert the indexes using the multi-index functions
    List<List<Expression>> foldedIndexes = this.convertArrayIndexes( access,
      indices );

    // Iterate through the indices, inserting the proper value into known
    // symbols
    List values = (List)knownSymbols.get( (Identifier)access.getArrayName() );
    for( List<Expression> foldedIndex: foldedIndexes ) {
      List innerValues = values;
      //FIXME: Would be faster if we got the innerValues list once for each
      // index only different for the last component
      for( int i = 0; i < foldedIndex.size()-1; i++ ) {
        int iindex = (int)((IntegerLiteral)foldedIndex.get(i)).getValue();
        innerValues = (List)innerValues.get( iindex );
      }

      int iindex = (int)
        ((IntegerLiteral)foldedIndex.get(foldedIndex.size()-1)).getValue();
      innerValues.set( iindex, rhsValue );
    }
  }

  protected Expression analyzeExpression( AssignmentExpression expr ) {
    // Fold RHS
    Expression rhsValue = this.analyzeExpression( expr.getRHS(), knownSymbols );
    if( !(rhsValue instanceof Literal) &&
      !(rhsValue instanceof MultiExpression)) {
      return (Expression)expr.getRHS().clone();
    }

    // if LHS is an array access, fold its indexes
    if( expr.getLHS() instanceof ArrayAccess ) {
      ArrayAccess access = (ArrayAccess)expr.getLHS();
      List<Expression> foldedIndexes = this.foldArrayIndexes( access );

      if( foldedIndexes == null ) {
        return (Expression)expr.clone();
      }

      if( !knownSymbols.containsKey( access.getArrayName() ) ) {
        // The array was not a known symbol. Initialize its value.
        if( !this.initializeArrayValue( access ) ) {
          return (Expression)expr.clone();
        }
      }

      // If the RHS is a literal value and the folded indexes contain
      // multiexpression, it can be computationally expensive to unfold each
      // one of them to make the assignment, due to all the context changes
      // when recursively calling this function. Optimization: detect this
      // situation and use loops instead of recursive calls.
      if( rhsValue instanceof Literal ) {
        for( Expression foldedIndex: foldedIndexes ) {
          if( foldedIndex instanceof MultiExpression ) {
            this.fastArrayAssignment( access, foldedIndexes,
            (Literal)rhsValue );
            return (Expression)expr.clone();
          }
        }
      }

      // If the assignment depends on multiexpressions, we should unfold.
      // Create a virtual assignment to unfold, using the folded indexes and
      // the folded RHS.
      ArrayAccess virtualAccess = new ArrayAccess(
        (Identifier)access.getArrayName().clone(), foldedIndexes );

      AssignmentExpression virtualAssignment = new AssignmentExpression(
        virtualAccess, AssignmentOperator.NORMAL, rhsValue );
      virtualAssignment.setParent( expr.getParent() );

      List<UnfoldedExpression> unfoldedExpressions =
        this.unfoldMultiExpressions( virtualAssignment );

      if( unfoldedExpressions == null ) {
        // Convert indexes
        foldedIndexes = this.convertArrayIndexes( virtualAccess );

        // Insert value into appropiate position at values list
        List values = (List)knownSymbols.get( access.getArrayName() );
        for( int i = 0; i < foldedIndexes.size() - 1; i++ ) {
          int iindex = (int) ((IntegerLiteral)foldedIndexes.get(
            i )).getValue();

          try {
            values = (List)values.get( iindex );
          } catch( ClassCastException e ) {
            return (Expression)expr.clone();
          }
        }
        int iindex = (int)((IntegerLiteral)foldedIndexes.get(
          foldedIndexes.size() - 1 ) ).getValue();
        values.set( iindex, rhsValue );
      } else {
        Identifier multiExpressionVar = unfoldedExpressions.get(0).getVar();
        MultiExpression mexpr = new MultiExpression( multiExpressionVar );

        for( UnfoldedExpression unfolded: unfoldedExpressions ) {
          mexpr.addExpression( unfolded.getVarValue(), this.analyzeExpression(
            (AssignmentExpression)unfolded.getExpression() ) );
        }

        if( multiExpressionVar.toString().equals( "NODE" ) ) {
          foldedIndexes = this.convertArrayIndexes( virtualAccess );
          List values = (List)knownSymbols.get( access.getArrayName() );
          for( int i = 0; i < foldedIndexes.size() - 1; i++ ) {
            int iindex = (int) ((IntegerLiteral)foldedIndexes.get(
              i )).getValue();
            values = (List)values.get( iindex );
          }
          int iindex = (int)((IntegerLiteral)foldedIndexes.get(
            foldedIndexes.size() - 1 ) ).getValue();
          values.set( iindex, mexpr );
        }
      }
    } else {
      // If LHS is an identifier: add to known symbols
      if( expr.getLHS() instanceof Identifier ) {
        // If there's a multi expression in the RHS, unfold and recursive calls
        AssignmentExpression virtualAssignment = new AssignmentExpression(
          (Expression)expr.getLHS().clone(), expr.getOperator(), rhsValue );
        virtualAssignment.setParent( expr.getParent() );
        List<UnfoldedExpression> unfoldedExpressions =
          this.unfoldMultiExpressions( virtualAssignment );

        if( unfoldedExpressions != null ) {
          MultiExpression mexpr = new MultiExpression(
            unfoldedExpressions.get(0).getVar() );

          for( UnfoldedExpression unfolded: unfoldedExpressions ) {
            mexpr.addExpression( unfolded.getVarValue(), this.analyzeExpression(
              (AssignmentExpression)unfolded.getExpression() ) );
          }

          List<Expression> values = new ArrayList<Expression>( 1 );
          values.add( mexpr );
          SymbolicAnalyzer.addKnownSymbol( knownSymbols,
            (Identifier)expr.getLHS(), values );
        } else {
          // Wouldn't do us much good to register array portions as constants
          List<Expression> values = new ArrayList<Expression>( 1 );
          values.add( rhsValue );
          SymbolicAnalyzer.addKnownSymbol( knownSymbols,
            (Identifier)expr.getLHS(), values );
        }
      }
    }

    rhsValue.setParent( null );
    return rhsValue;
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
    if( !(foldedLHS instanceof Literal ) ||
      !(foldedRHS instanceof Literal ) ) {

      return new BinaryExpression( foldedLHS, expr.getOperator(), foldedRHS );
    }

    // Else, apply the binary operator
    BinaryOperator operator = expr.getOperator();
    Expression newExpr = null;

    if( operator.equals( BinaryOperator.DIVIDE ) ) {
      double lhsValue = this.numericAsDouble( foldedLHS );
      double rhsValue = this.numericAsDouble( foldedRHS );
      double opValue = lhsValue / rhsValue;

      return this.binaryOperationResult( foldedLHS, foldedRHS, opValue );
    }

    if( operator.equals( BinaryOperator.MULTIPLY ) ) {
      double lhsValue = this.numericAsDouble( foldedLHS );
      double rhsValue = this.numericAsDouble( foldedRHS );
      double opValue = lhsValue * rhsValue;

      return this.binaryOperationResult( foldedLHS, foldedRHS, opValue );
    }

    if( operator.equals( BinaryOperator.COMPARE_EQ ) ) {
      // Fold equality
      newExpr = this.buildBooleanLiteral( foldedLHS.equals( foldedRHS ) );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.COMPARE_NE ) ) {
      // Fold inequality
      newExpr = this.buildBooleanLiteral( !foldedLHS.equals( foldedRHS ) );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.COMPARE_GE ) ) {
      // Fold GE
      double lhs = this.numericAsDouble( foldedLHS );
      double rhs = this.numericAsDouble( foldedRHS );

      newExpr = this.buildBooleanLiteral( lhs >= rhs );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.COMPARE_GT ) ) {
      // Fold GT
      double lhs = this.numericAsDouble( foldedLHS );
      double rhs = this.numericAsDouble( foldedRHS );

      newExpr = this.buildBooleanLiteral( lhs > rhs );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.COMPARE_LE ) ) {
      // Fold LE
      double lhs = this.numericAsDouble( foldedLHS );
      double rhs = this.numericAsDouble( foldedRHS );

      newExpr = this.buildBooleanLiteral( lhs <= rhs );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.COMPARE_LT ) ) {
      // Fold LT
      double lhs = this.numericAsDouble( foldedLHS );
      double rhs = this.numericAsDouble( foldedRHS );

      newExpr = this.buildBooleanLiteral( lhs < rhs );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.LOGICAL_AND ) ) {
      // Fold logical and
      newExpr = this.applyLogicalAnd( foldedLHS, foldedRHS );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.LOGICAL_OR ) ) {
      // Fold logical or
      newExpr = this.applyLogicalOr( foldedLHS, foldedRHS );
      return newExpr;
    }

    if( operator.equals( BinaryOperator.ADD ) ) {
      double lhsValue = this.numericAsDouble( foldedLHS );
      double rhsValue = this.numericAsDouble( foldedRHS );
      double opValue = lhsValue + rhsValue;

      return this.binaryOperationResult( foldedLHS, foldedRHS, opValue );
    }

    if( operator.equals( BinaryOperator.SUBTRACT ) ) {
      double lhsValue = this.numericAsDouble( foldedLHS );
      double rhsValue = this.numericAsDouble( foldedRHS );
      double opValue = lhsValue - rhsValue;

      return this.binaryOperationResult( foldedLHS, foldedRHS, opValue );
    }

    System.err.println( "WARNING: "+
      "cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression( "+
      "BinaryExpression ) not implemented for BinaryOperator: " +
      operator );
    return (Expression)expr.clone();
  }

  protected Expression analyzeExpression( CharLiteral charLiteral ) {
    return (Expression)charLiteral.clone();
  }

  protected Expression analyzeExpression( CommaExpression expr ) {
    List<Expression> foldedValues = new ArrayList<Expression>(
      expr.getChildren().size() );

    for( int i = 0; i < expr.getChildren().size(); i++ ) {
      foldedValues.add( this.analyzeExpression(
        (Expression)expr.getChildren().get( i ), knownSymbols ) );
    }

    return new CommaExpression( foldedValues );
  }

  protected Expression analyzeExpression( DoubleLiteral doubleLiteral ) {
    return (Expression)doubleLiteral.clone();
  }

  protected Expression analyzeExpression( EscapeLiteral escapeLiteral ) {
    return (Expression)escapeLiteral.clone();
  }

  protected Expression analyzeExpression( FloatLiteral floatLiteral ) {
    return (Expression)floatLiteral.clone();
  }

  protected Expression analyzeExpression( FunctionCall functionCall ) {
    // Function calls affect symbol folding in two ways:
    // 1. Function parameters can be folded if, and only if, they are passed by
    // value or, otherwise, not modified by the function code
    //
    // 2. Global variables must be removed from the known symbols list if they
    // are modified by the function code

    // If there is no procedure characterization: do not fold parameters. We
    // should also remove all global variables from the symbols list, but we
    // assume here that the compiler has access to all user-written code (which
    // should be the only code modifying global variables)

    if(!CppcRegisterManager.isRegistered((Identifier)functionCall.getName()) ||
      CppcRegisterManager.getCharacterization(
        (Identifier)functionCall.getName()).isNull() ) {

//       Procedure procedure = functionCall.getStatement().getProcedure();
//       for( Identifier id: (Set<Identifier>)constants.keySet() ) {
//         if( ObjectAnalizer.isGlobal( id, procedure ) ) {
//           constants.remove( id );
//         }
//       }

      return (Expression)functionCall.clone();
    }

    // Get procedure characterization
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)functionCall.getName() );

    // Remove generated parameters from known symbols list
    for( ProcedureParameter param: c.getGenerated() ) {
      Expression expr = functionCall.getArgument( param.getPosition() );
      if( expr instanceof Identifier ) {
        knownSymbols.remove( (Identifier)expr );
      }
    }

    // Remove global generated variables from constants list
    Set<Identifier> remove = ObjectAnalizer.globalDeclarationsToSet(
      c.getGlobalGenerated() );
    for( Identifier id: remove ) {
      knownSymbols.remove( id );
    }

    return (Expression)functionCall.clone();
  }

  protected Expression analyzeExpression( Identifier identifier ) {
    if( knownSymbols.containsKey( identifier ) ) {
      List values = (List)knownSymbols.get( identifier );
      Expression folded = null;

      if( values.size() == 1 ) {
        folded = (Expression)((Expression)values.get( 0 )).clone();
      } else {
        List<Expression> clonedValues = new ArrayList<Expression>(
          values.size() );
        for( Expression expr: (List<Expression>)values ) {
          clonedValues.add( (Expression)expr.clone() );
        }

        folded = new CommaExpression( clonedValues );
      }

      if( !SymbolicAnalyzer.idMask.isEmpty() ) {
        int depth = 0;
        while( (folded instanceof MultiExpression) &&
        (depth<SymbolicAnalyzer.idMask.size()) ) {
          MultiExpression mexpr = (MultiExpression)folded;
          if( mexpr.getVar().equals(
            SymbolicAnalyzer.idMask.elementAt( depth ) ) ) {

            folded = mexpr.getValue( SymbolicAnalyzer.valueMask.elementAt(
              depth ) );
          }

          depth++;
        }

        if( folded != null ) {
          return (Expression)folded.clone();
        }

        return (Expression)identifier.clone();
      }

      return folded;
    }

    return (Expression)identifier.clone();
  }

  protected Expression analyzeExpression( BooleanLiteral booleanLiteral ) {
    return (Expression)booleanLiteral.clone();
  }

  protected Expression analyzeExpression( IntegerLiteral integerLiteral ) {
    return (Expression)integerLiteral.clone();
  }

  protected Expression analyzeExpression( MultiExpression mexpr ) {
    MultiExpression folded = new MultiExpression( mexpr.getVar() );

    for( Expression key: mexpr.getValueSet() ) {
      folded.addExpression( key, this.analyzeExpression( mexpr.getValue( key ),
        knownSymbols ) );
    }

    return folded;
  }

  protected Expression analyzeExpression( StringLiteral stringLiteral ) {
    return (Expression)stringLiteral.clone();
  }

  protected Expression analyzeExpression( UnaryExpression expr ) {

    UnaryOperator operator = expr.getOperator();

    // Fold the inner expression
    Expression folded = this.analyzeExpression( expr.getExpression(),
      knownSymbols );

    UnaryExpression virtual = new UnaryExpression( operator, folded );
    virtual.setParent( expr.getParent() );

    List<UnfoldedExpression> unfoldedExpressions = this.unfoldMultiExpressions(
      virtual );

    if( unfoldedExpressions != null ) {
      MultiExpression mexpr = new MultiExpression(
        unfoldedExpressions.get(0).getVar() );
      for( UnfoldedExpression unfolded: unfoldedExpressions ) {
        mexpr.addExpression( unfolded.getVarValue(), this.analyzeExpression(
          (UnaryExpression)unfolded.getExpression() ) );
      }

      return mexpr;
    }

    // If the inner expression is not constant, return
    if( !(folded instanceof Literal) ) {
      return new UnaryExpression( expr.getOperator(), folded );
    }

    Expression newExpr = null;

    // Else, apply the corresponding unary operator
    if( operator.equals( UnaryOperator.LOGICAL_NEGATION ) ) {
      newExpr = this.applyLogicalNegation( folded );
      return newExpr;
    }

    if( operator.equals( UnaryOperator.MINUS ) ) {
      if( folded instanceof IntegerLiteral ) {
        IntegerLiteral safeOp = (IntegerLiteral)folded;
        newExpr = new IntegerLiteral( -safeOp.getValue() );
        return newExpr;
      }

      // Double literal comes before FloatLiteral since it is a subclass (would
      // pass the instanceof test)
      if( folded instanceof DoubleLiteral ) {
        DoubleLiteral safeOp = (DoubleLiteral)folded;
        newExpr = new DoubleLiteral( -safeOp.getValue() );
        return newExpr;
      }

      if( folded instanceof FloatLiteral ) {
        FloatLiteral safeOp = (FloatLiteral)folded;
        newExpr = new FloatLiteral( -safeOp.getValue() );
        return newExpr;
      }
    }

    if( operator.equals( UnaryOperator.DEREFERENCE ) ||
      operator.equals( UnaryOperator.PLUS ) ) {

      System.out.println( "Check how to fold with operator: " + operator +
        ". Line: " + expr.getStatement().where() );
      return (Expression)expr.clone();
    }

    System.err.println( "WARNING: "+
      "cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression( "+
      "UnaryExpression ) not implemented for UnaryOperator: " + operator );

    return new UnaryExpression( expr.getOperator(), folded );
  }

  protected Expression analyzeExpression( UnfoldedExpression expr ) {
    Expression innerAnalyzed = this.analyzeExpression( expr.getExpression(),
      knownSymbols );

    return new UnfoldedExpression( expr.getVar(), expr.getVarValue(),
      innerAnalyzed );
  }

  protected static int numericAsInteger( Expression expr ) {
    if( expr instanceof IntegerLiteral ) {
      return (int)((IntegerLiteral)expr).getValue();
    }

    if( expr instanceof DoubleLiteral ) {
      return (int)((DoubleLiteral)expr).getValue();
    }

    if( expr instanceof FloatLiteral ) {
      return (int)((FloatLiteral)expr).getValue();
    }

    return 0;
  }

  protected static double numericAsDouble( Expression expr ) {

    if( expr instanceof IntegerLiteral ) {
      IntegerLiteral safeExpr = (IntegerLiteral)expr;
      return (double)safeExpr.getValue();
    }

    if( expr instanceof DoubleLiteral ) {
      DoubleLiteral safeExpr = (DoubleLiteral)expr;
      return safeExpr.getValue();
    }

    if( expr instanceof FloatLiteral ) {
      FloatLiteral safeExpr = (FloatLiteral)expr;
      return (double)safeExpr.getValue();
    }

    return 0;
  }

  protected static Expression convertToFloat( Expression value ) {
    if( value == null ) {
      return null;
    }

    if( value instanceof IntegerLiteral ) {
      return new FloatLiteral( ((IntegerLiteral)value).getValue() );
    }

    if( value instanceof DoubleLiteral ) {
      return new FloatLiteral( ((DoubleLiteral)value).getValue() );
    }

    return (Expression)value.clone();
  }

  protected static List convertToFloat( List values ) {
    List convertedValues = new ArrayList( values.size() );

    for( int i = 0; i < values.size(); i++ ) {
      // If list -> recursive call
      if( values.get( i ) instanceof List ) {
        convertedValues.add( SymbolicExpressionAnalyzer.convertToFloat(
          (List)values.get( i ) ) );
        continue;
      }

      convertedValues.add( SymbolicExpressionAnalyzer.convertToFloat(
        (Expression)values.get(i) ) );
    }

    return convertedValues;
  }

  protected static Expression convertToDouble( Expression value ) {
    if( value == null ) {
      return null;
    }

    if( value instanceof IntegerLiteral ) {
      return new DoubleLiteral( ((IntegerLiteral)value).getValue() );
    }

    if( value instanceof FloatLiteral ) {
      return new DoubleLiteral( ((FloatLiteral)value).getValue() );
    }

    return (Expression)value.clone();
  }

  protected static List convertToDouble( List values ) {
    List convertedValues = new ArrayList( values.size() );

    for( int i = 0; i < values.size(); i++ ) {
      // If list -> recursive call
      if( values.get( i ) instanceof List ) {
        convertedValues.add( SymbolicExpressionAnalyzer.convertToDouble( (List)
          values.get( i ) ) );
        continue;
      }

      convertedValues.add( SymbolicExpressionAnalyzer.convertToDouble(
        (Expression)values.get( i ) ) );
    }

    return convertedValues;
  }

  protected Expression binaryOperationResult( Expression lhs, Expression rhs,
    double value ) {

    if( (lhs instanceof DoubleLiteral) || (rhs instanceof DoubleLiteral) ) {
      return new DoubleLiteral( value );
    }

    if( (lhs instanceof FloatLiteral) || (rhs instanceof FloatLiteral) ) {
      return new FloatLiteral( value );
    }

    return new IntegerLiteral( (int)value );
  }

  protected abstract Expression applyLogicalAnd( Expression lhs,
    Expression rhs );
  protected abstract Expression applyLogicalNegation( Expression expr );
  protected abstract Expression applyLogicalOr( Expression lhs,
    Expression rhs );
  protected abstract Expression buildBooleanLiteral( boolean b );
  protected abstract boolean initializeArrayValue( ArrayAccess access );
}
