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

import cetus.hir.BooleanLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.ReturnStatement;
import cetus.hir.Statement;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.FormatStatement;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.fortran.DataDeclaration;
import cppc.compiler.fortran.FortranDoLoop;
import cppc.compiler.fortran.ParameterDeclaration;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.VariableSizeAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class FortranSymbolicAnalyzer extends SymbolicAnalyzer {

  FortranSymbolicAnalyzer() {}

  protected void analyzeStatement( DeclarationStatement stmt ) {
    // Use the superclass version in case the declaration is a
    // VariableDeclaration
    super.analyzeStatement( stmt );

    Declaration declaration = stmt.getDeclaration();
    if( declaration instanceof DataDeclaration ) {
      // Else, if it is a Fortran DataDeclaration, process it
      DataDeclaration dd = (DataDeclaration)declaration;
      List<Expression> initializers = dd.getInitializers();

      // For size analysis purposes
      VariableSizeAnalizer sizeAnalizer =
        VariableSizeAnalizerFactory.getAnalizer();

      int nextPos = 0;
      for( Expression expr: dd.getExpressions() ) {

        // If the expression is not an identifier (i.e., it's an array access),
        // continue
        if( !(expr instanceof Identifier) ) {
          continue;
        }

        // Find size of this identifier
        Identifier id = (Identifier)expr;
        Expression sizeExpr = sizeAnalizer.getSize( id, stmt );

        long size;
        if( sizeExpr == null ) {
          size = 1;
        } else {
          sizeExpr = SymbolicExpressionAnalyzer.analyzeExpression( sizeExpr,
            knownSymbols );
          if( !(sizeExpr instanceof IntegerLiteral) ) {
            // Size MUST be constant
            continue;
          }
          size = ((IntegerLiteral)sizeExpr).getValue();
        }

        List<Expression> localInitializer =
          new ArrayList<Expression>( (int)size );
        for( int i = 0; (i < size) && (i < initializers.size()); i++ ) {
          // Get next initializer expression
          Expression next = (Expression)initializers.get( nextPos++ ).clone();
          next = SymbolicExpressionAnalyzer.analyzeExpression( next,
            knownSymbols );
          localInitializer.add( next );
        }

        this.addKnownSymbol( knownSymbols, id, localInitializer );
      }

      return;
    }

    if( declaration instanceof ParameterDeclaration ) {
      // Else if it is a Fortran ParameterDeclaration, process it
      ParameterDeclaration pd = (ParameterDeclaration)declaration;
      Iterator iter = pd.getDeclarators().iterator();
      while( iter.hasNext() ) {
        VariableDeclarator decl = (VariableDeclarator)iter.next();
        if( decl.getInitializer() != null ) {
          // There is an initializer. Fold its expressions.
          for( Expression expr:
            (List<Expression>)decl.getInitializer().getChildren() ) {

            SymbolicExpressionAnalyzer.analyzeExpression( expr, knownSymbols );
          }

          // If all expressions are constants, add to list
          for( Expression expr:
            (List<Expression>)decl.getInitializer().getChildren() ) {

            if( !(expr instanceof Literal) ) {
              return;
            }
          }

          // All expressions are constants: add to list
          this.addKnownSymbol( knownSymbols, (Identifier)decl.getSymbol(),
            decl.getInitializer().getChildren() );
        }
      }
    }
  }

  protected void analyzeStatement( FormatStatement formatStatement ) {}

  protected void enterLoop( FortranDoLoop fortranDoLoop ) {
    // Remove variables tagged in gotos to the loop label
    Identifier loopVar = fortranDoLoop.getLoopVar();
    Identifier label = fortranDoLoop.getLabel();
    if( (label != null) &&
      removeUponLabel.containsKey( label ) ) {

      for( Identifier id: removeUponLabel.get( label ) ) {
        knownSymbols.remove( id );
      }
    }

    Set<Identifier> commVariables =
      CommunicationAnalyzer.getCurrentCommunicationVariables();
    if( commVariables != null ) {
      if( commVariables.contains( loopVar ) ) {
        // Fold the start
        Expression foldedStart = SymbolicExpressionAnalyzer.analyzeExpression(
          fortranDoLoop.getStart(), knownSymbols );

        // Fold the stop
        Expression foldedStop = SymbolicExpressionAnalyzer.analyzeExpression(
          fortranDoLoop.getStop(), knownSymbols );

        // Fold the step
        Expression foldedStep = null;
        if( fortranDoLoop.getStep() != null ) {
          foldedStep = SymbolicExpressionAnalyzer.analyzeExpression(
            fortranDoLoop.getStep(), knownSymbols );
        }

        if( foldedStep == null ) {
          foldedStep = new IntegerLiteral( 1 );
        }

        List headerValues = new ArrayList( 3 );
        headerValues.add( foldedStart );
        headerValues.add( foldedStop );
        headerValues.add( foldedStep );
        CommaExpression loopExpression = new CommaExpression( headerValues );
        loopExpression.setParent( fortranDoLoop );

        List<UnfoldedExpression> unfoldedExpressions =
          SymbolicExpressionAnalyzer.unfoldMultiExpressions( loopExpression );
        if( unfoldedExpressions != null ) {
          MultiExpression indexMexpr = new MultiExpression(
            unfoldedExpressions.get(0).getVar() );
          for( UnfoldedExpression unfolded: unfoldedExpressions ) {
            CommaExpression subLoop = (CommaExpression)unfolded.getExpression();
            Expression subStart = (Expression)subLoop.getChildren().get( 0 );
            Expression subStop = (Expression)subLoop.getChildren().get( 1 );
            Expression subStep = (Expression)subLoop.getChildren().get( 2 );
            FortranDoLoop virtual = new FortranDoLoop( null,
              (Identifier)fortranDoLoop.getLoopVar().clone(), subStart, subStop,
              subStep, null );
            virtual.setParent( fortranDoLoop.getParent() );
            this.enterLoop( virtual );

            if( knownSymbols.containsKey( loopVar ) ) {
              // Add the current value of the index to the multiexpression,
              // using the current parent index value as constraint
              indexMexpr.addExpression( unfolded.getVarValue(), (Expression)
                ((Expression)((List)knownSymbols.get(loopVar )).get(
                  0)).clone());
            }
          }

          if( !indexMexpr.getChildren().isEmpty() ) {
            List values = new ArrayList( 1 );
            values.add( indexMexpr );
            knownSymbols.put( loopVar, values );
          }

          this.removeLoopCarriedSymbols( fortranDoLoop );
          return;
        }

        // If iteration values are known at compile time, add a vector of values
        // to the known symbols for the loop variable
        if( (foldedStart instanceof Literal) &&
          (foldedStop instanceof Literal) &&
          (foldedStep instanceof Literal) ) {

          int intStart = SymbolicExpressionAnalyzer.numericAsInteger(
            foldedStart );
          int intStop = SymbolicExpressionAnalyzer.numericAsInteger(
            foldedStop );
          int intStep = SymbolicExpressionAnalyzer.numericAsInteger(
            foldedStep );

          List<Expression> loopValues = new ArrayList<Expression>(
            (intStop-intStart) / intStep + 1 );

          if( intStep > 0 ) {
            for( int i = intStart; i <= intStop; i+= intStep ) {
              loopValues.add( new IntegerLiteral( i ) );
            }
          } else {
            for( int i = intStart; i >= intStop; i+= intStep ) {
              loopValues.add( new IntegerLiteral( i ) );
            }
          }

          if( !loopValues.isEmpty() ) {
            MultiExpression value = new MultiExpression(
              fortranDoLoop.getLoopVar() );
            for( Expression iter: loopValues ) {
              value.addExpression( iter, iter );
            }
            List<Expression> values = new ArrayList<Expression>( 1 );
            values.add( value );

            this.addKnownSymbol( knownSymbols, loopVar, values );
          }
        } else {
          // Else, all values generated inside the loop must be removed from the
          // known symbols list, since they can't be accurately calculated.

          // Find all generated variables inside the loop. We get the parent
          // because it is the statement holding data about the loop statement
          // itself.
          CppcStatement parent = (CppcStatement)fortranDoLoop.getParent();
          DepthFirstIterator iter = new DepthFirstIterator( parent );
          Set<Identifier> generated = new HashSet<Identifier>();
          while( iter.hasNext() ) {
            try {
              CppcStatement cppcStatement = (CppcStatement)iter.next(
                CppcStatement.class );

              // Add generated
              generated.addAll( cppcStatement.getGenerated() );
              generated.addAll( ObjectAnalizer.globalDeclarationsToSet(
                cppcStatement.getGlobalGenerated() ) );
            } catch( NoSuchElementException e ) {}
          }

          // Remove generated symbols from known symbols list.
          for( Identifier id: generated ) {
            knownSymbols.remove( id );
          }
        }
      }

      this.removeLoopCarriedSymbols( fortranDoLoop );
    }
  }

  private void removeMultiExpressions( List values, Identifier var ) {
    for( int i = 0; i < values.size(); i++ ) {
      Object value = values.get( i );

      if( value == null ) {
        continue;
      }

      if( value instanceof List ) {
        this.removeMultiExpressions( (List)value, var );
        continue;
      }

      if( value instanceof MultiExpression ) {
        MultiExpression mexpr = (MultiExpression)value;
        if( mexpr.getVar().equals( var ) ) {
          Expression lastChild = (Expression)mexpr.getChildren().get(
            mexpr.getChildren().size() - 1 );
          values.set( i, lastChild );
        }
      }
    }
  }

  protected void exitLoop( FortranDoLoop fortranDoLoop ) {
    // Remove again variables tagged in gotos to the loop label
    Identifier loopVar = fortranDoLoop.getLoopVar();
    Identifier label = fortranDoLoop.getLabel();
    if( (label != null) &&
      removeUponLabel.containsKey( label ) ) {

      for( Identifier id: removeUponLabel.get( label ) ) {
        knownSymbols.remove( id );
      }
      removeUponLabel.remove( label );
    }

    // Remove all multi expressions dependent on the loop var
    for( Identifier id: ((Map<Identifier,?>)knownSymbols).keySet() ) {
      List values = (List)knownSymbols.get( id );
      this.removeMultiExpressions( values, loopVar );
    }

//     // If the loop variable was added as a multi expression, remove it
//     Identifier loopVar = fortranDoLoop.getLoopVar();
//     if( knownSymbols.containsKey( loopVar ) ) {
//       List values = (List)knownSymbols.get( loopVar );
//       if( values.size() == 1 ) {
//         // Under normal circumstances, the variable value upon loop exit
// would
//         // be the last iteration's value
//         Expression value = (Expression)values.get( 0 );
//         if( value instanceof MultiExpression ) {
//           Expression lastChild = (Expression)value.getChildren().get(
//             value.getChildren().size() - 1 );
//           values.clear();
//           values.add( lastChild );
//         }
//       } else {
//         // The loop is over and we have to void the value of the loop
// variable.
//         // If we don't know what value will the loop variable have upon loop
//         // exit, we better just remove it from the known symbols list
//         knownSymbols.remove( loopVar );
//       }
//     }
  }

  protected void analyzeStatement( FortranDoLoop fortranDoLoop ) {
    // If we find a loop inside an If, the analysis won't trigger by itself
    this.enterLoop( fortranDoLoop );
    this.analyzeStatement( (CppcStatement)fortranDoLoop.getBody(),
      knownSymbols );
    this.exitLoop( fortranDoLoop );
  }

  protected void fold( GotoStatement s ) {
    // No need to fold its expression: Fortran only accepts numeric literals

    // Slightly different from super version: if s is inside a labeled Fortran
    // do loop matching its target label, we have to analyze up to its end
    Expression target = s.getValue();
    FortranDoLoop loop = (FortranDoLoop)ObjectAnalizer.getParentOfClass( s,
      FortranDoLoop.class );
    Set<Identifier> removeInc = new HashSet<Identifier>(
      knownSymbols.keySet().size() );

    while( loop != null ) {
      Identifier label = loop.getLabel();
      if( (label != null) && 
          target.equals( label ) ) {
        // Find goto statement inside loop
        DepthFirstIterator iter = new DepthFirstIterator( loop.getBody() );
        iter.pruneOn( Expression.class );
        while( iter.hasNext() ) {
          try {
            GotoStatement stmt = (GotoStatement)iter.next(
              GotoStatement.class );
            if( stmt == s ) {
              break;
            }
          } catch( NoSuchElementException e ) {
            // Error: unable to found goto inside loop
            return;
          }
        }

        // Now the iterator is positioned on the goto. Iterator until the end
        // of the loop body
        while( iter.hasNext() ) {
          try {
            CppcStatement cppcStatement = (CppcStatement)iter.next(
              CppcStatement.class );

            for( Identifier id: cppcStatement.getGenerated() ) {
              removeInc.add( id );
            }
          } catch( NoSuchElementException e ) {}
        }

        Set<Identifier> oldRemove = removeUponLabel.get( s.getValue() );
        if( oldRemove == null ) {
          removeUponLabel.put( s.getValue(), removeInc );
        } else {
          oldRemove.addAll( removeInc );
        }

        return;
      }

      loop = (FortranDoLoop)ObjectAnalizer.getParentOfClass( loop.getParent(),
        FortranDoLoop.class );
    }

    // If it was not inside its corresponding label fortran do loop: use super
    // version
    super.analyzeStatement( s );
  }

  protected void analyzeStatement( ReturnStatement returnStatement ) {}

  protected Set<Identifier> removeLoopCarriedSymbols( Loop l ) {
    Set<Identifier> carried = super.removeLoopCarriedSymbols( l );

    if( l instanceof FortranDoLoop ) {
      FortranDoLoop safeLoop = (FortranDoLoop)l;
      this.calculateCarriedSymbols( safeLoop.getLoopVar(), l.getBody(),
        carried );
      return carried;
    }

    System.err.println( "WARNING: " +
      "cppc.compiler.analysis.FortranSymbolicAnalyzer.removeLoopCarriedSymbols"+
      "() not implemented for class " + l.getClass() );
    return carried;
  }
}
