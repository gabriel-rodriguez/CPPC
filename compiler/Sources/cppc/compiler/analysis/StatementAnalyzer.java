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

import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.ContinueStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DoLoop;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.Label;
import cetus.hir.NullStatement;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.hir.WhileLoop;

import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class StatementAnalyzer extends
  FunctionDispatcher<Statement> {

  private static Class instanceClass;
  private static StatementAnalyzer instance;
  protected static CppcStatement currentStatement = null;

  static {

    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
        GlobalNames.STATEMENT_ANALYZER_CLASS_OPTION ) );
      instance = (StatementAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected StatementAnalyzer() {}

  public static void analyzeStatement( CppcStatement cppcStatement ) {
    cppcStatement.statementCount = 1;

    Method m = instance.dispatch( cppcStatement.getStatement(),
      "analyzeStatement" );

    if( m == null ) {
      System.err.println( "WARNING: cppc.compiler.analysis.StatementAnalyzer." +
        "analyzeStatement() not implemented for " +
        cppcStatement.getStatement().getClass() );

      return;
    }

    CppcStatement oldStatement = currentStatement;
    currentStatement = cppcStatement;
    try {
      m.invoke( instance, cppcStatement.getStatement() );
    } catch( Exception e ) {
      e.printStackTrace();
    } finally {
      currentStatement = oldStatement;
    }
  }

  protected void analyzeStatement( CppcLabel label ) {
    currentStatement.setWeight( 0 );
    currentStatement.statementCount = 0;
  }

  protected void analyzeStatement( CompoundStatement compoundStatement ) {
    // Analyze contained statements
    BreadthFirstIterator iter = new BreadthFirstIterator( compoundStatement );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Statement.class );

    long totalWeight = 0;
    int statementCount = 0;
    while( iter.hasNext() ) {
      try {
        CppcStatement next = (CppcStatement)iter.next( CppcStatement.class );
        this.analyzeStatement( next );
        totalWeight += next.getWeight();
        statementCount += next.statementCount;
      } catch( NoSuchElementException e ) {}
    }

    currentStatement.setWeight( totalWeight );
    currentStatement.statementCount = statementCount;
  }

  protected void analyzeStatement( ContinueStatement stmt ) {
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcCheckpointPragma pragma ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcCheckpointLoopPragma pragma ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcConditionalJump pragma ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcExecutePragma pragma ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcNonportableFunctionMark s ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  protected void analyzeStatement( CppcShutdownPragma pragma ) {
    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
  }

  private static void analyzeInitializer( Initializer init ) {

    // The initializer expression can generate/consume identifiers
    // // If the initializer is an expression: analyze it

    // New Cetus version: values are centralized in the children list
    List values = init.getChildren();
    if( values != null ) {
      Iterator children = values.iterator();
      while( children.hasNext() ) {
        Object obj = children.next();

        // A list can contain expressions
        if( obj instanceof Expression ) {
          currentStatement.getConsumed().addAll(
            ExpressionAnalyzer.analyzeExpression( currentStatement,
            (Expression)obj ) );
        } else {
          // ... or initializers, that must be analyzed
          analyzeInitializer( (Initializer)obj );
        }
      }
    }
  }

  protected void analyzeStatement( DeclarationStatement s ) {

    currentStatement.statementCount = 0;
    currentStatement.setWeight( 0 );
    Declaration declaration = s.getDeclaration();

    // Only processing VariableDeclaration's
    if( !(declaration instanceof VariableDeclaration) ) {
      return;
    }

    VariableDeclaration vd = (VariableDeclaration)declaration;

    // Iterator over declarators
    for( int i = 0; i < vd.getNumDeclarators(); i++ ) {

      // If this declarator has an Initializer, then it must be added to the
      // 'generated' set
      Declarator declarator = vd.getDeclarator( i );
      if( declarator instanceof VariableDeclarator ) {
        if( declarator.getInitializer() != null ) {
          currentStatement.getGenerated().add(
            (Identifier)declarator.getSymbol().clone() );
          currentStatement.getPartialGenerated().add(
            (Identifier)declarator.getSymbol().clone() );
          analyzeInitializer( declarator.getInitializer() );
        }

        // Also add the variables used in its trailing specs (array specs)
        List trailingSpecs =
          ((VariableDeclarator)declarator).getTrailingSpecifiers();
        for( int j = 0; j < trailingSpecs.size(); j++ ) {
          currentStatement.getConsumed().addAll(
            SpecifierAnalyzer.analyzeSpecifier( currentStatement,
            (Specifier)trailingSpecs.get( j ) ) );
        }

        // Also, add it to the list of "partial generated" variables of the
        // statement, since it depends on the consumed set (small workaround to
        // make this work, no harm done).
        currentStatement.getPartialGenerated().add(
          (Identifier)declarator.getSymbol() );
      }
    }
  }

  protected void analyzeStatement( DoLoop l ) {
    CppcStatement body = (CppcStatement)l.getBody();
    this.analyzeStatement( body );

    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
      l.getCondition() ) );

    currentStatement.setWeight( currentStatement.getWeight() +
      body.getWeight() );
    currentStatement.statementCount += body.statementCount;
  }

  protected void analyzeStatement( ExpressionStatement s ) {

    Expression expr = s.getExpression();

    currentStatement.getConsumed().addAll( ExpressionAnalyzer.analyzeExpression(
      currentStatement, expr ) );
  }

  protected void analyzeStatement( GotoStatement stmt ) {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
        stmt.getValue() ) );
  }

  protected void analyzeStatement( IfStatement s ) {
    currentStatement.getConsumed().addAll( ExpressionAnalyzer.analyzeExpression(
      currentStatement, s.getControlExpression() ) );

//     long thenWeight = 0;
//     long elseWeight = 0;
//     int thenStatements = 0;
//     int elseStatements = 0;
//    long weight = 0;
    long weight = currentStatement.getWeight();
    int statements = 0;

    this.analyzeStatement( (CppcStatement)s.getThenStatement() );
//     thenWeight = ((CppcStatement)s.getThenStatement()).getWeight();
//     thenStatements = ((CppcStatement)s.getThenStatement()).statementCount;
    weight += ((CppcStatement)s.getThenStatement()).getWeight();
    statements = ((CppcStatement)s.getThenStatement()).statementCount;

    if( s.getElseStatement() != null ) {
      this.analyzeStatement( (CppcStatement)s.getElseStatement() );
//       elseWeight = ((CppcStatement)s.getElseStatement()).getWeight();
//       elseStatements = ((CppcStatement)s.getElseStatement()).statementCount;
      weight += ((CppcStatement)s.getElseStatement()).getWeight();
      statements += ((CppcStatement)s.getElseStatement()).statementCount;
    }

//     currentStatement.setWeight( elseWeight>thenWeight?elseWeight:thenWeight
// );
//     currentStatement.statementCount = elseStatements>thenStatements?
//       elseStatements:thenStatements;
    currentStatement.setWeight( (int)Math.ceil( (double)weight/2.0 ) );
    currentStatement.statementCount = (int)Math.ceil( (double)statements/2.0 );
  }

  protected void analyzeStatement( WhileLoop l ) {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression( currentStatement,
        l.getCondition() ) );

    this.analyzeStatement( (CppcStatement)l.getBody() );
    currentStatement.setWeight( ((CppcStatement)l.getBody()).getWeight() );
    currentStatement.statementCount +=
      ((CppcStatement)l.getBody()).statementCount;
  }

  protected void analyzeStatement( Label label ) {
    currentStatement.setWeight( 0 );
    currentStatement.statementCount = 0;
  }

  protected void analyzeStatement( NullStatement nullStatement ) {}
}
