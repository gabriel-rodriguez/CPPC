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

import cetus.hir.CompoundStatement;
import cetus.hir.ContinueStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Label;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.NullStatement;
import cetus.hir.Procedure;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.hir.WhileLoop;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public abstract class SymbolicAnalyzer extends
  FunctionDispatcher<Statement> {

  private static Class instanceClass;
  private static SymbolicAnalyzer instance;
  protected static Map knownSymbols = null;
  private static List<CppcStatement> remove = new ArrayList<CppcStatement>();
  private static int callDepth = 0;
  protected static Map<Expression,Set<Identifier>> removeUponLabel =
    new Hashtable<Expression,Set<Identifier>>();
//   protected static Identifier idMask = null;
//   protected static Expression valueMask = null;
  protected static Stack<Identifier> idMask = new Stack<Identifier>();
  protected static Stack<Expression> valueMask = new Stack<Expression>();

  static {

    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
        GlobalNames.SYMBOLIC_ANALYZER_CLASS_OPTION ) );
      instance = (SymbolicAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected SymbolicAnalyzer() {}

  public static void analyzeStatement( CppcStatement cppcStatement,
    Map knownSymbols ) {

    Statement stmt = cppcStatement.getStatement();
    Method m = instance.dispatch( stmt,
      "analyzeStatement" );

    if( m == null ) {
      System.err.println( "WARNING: cppc.compiler.analysis.SymbolicAnalyzer." +
        "analyzeStatement() not implemented for " + stmt.getClass() );

      return;
    }

    Map oldSymbols = null;
    if( SymbolicAnalyzer.knownSymbols != knownSymbols ) {
      oldSymbols = SymbolicAnalyzer.knownSymbols;
      SymbolicAnalyzer.knownSymbols = knownSymbols;
    }
    try {
      callDepth++;
      m.invoke( instance, stmt );
    } catch( Exception e ) {
      e.printStackTrace();
    } finally {
      if( oldSymbols != null ) {
        SymbolicAnalyzer.knownSymbols = oldSymbols;
      }
      callDepth--;
      if( callDepth == 0 ) {
        for( CppcStatement s: remove ) {
          CompoundStatement parent = (CompoundStatement)s.getParent();
          if( parent != null ) {
            parent.removeChild( s );
          }
        }
        remove.clear();
      }
    }
  }

  public static void enterLoop( Loop l, Map knownSymbols ) {
    Method m = instance.dispatch( (Statement)l, "enterLoop" );

    if( m == null ) {
      System.err.println( "WARNING: cppc.compiler.analysis.SymbolicAnalyzer." +
        "enterLoop() not implemented for " + l.getClass() );

      return;
    }

    SymbolicAnalyzer.knownSymbols = knownSymbols;
    try {
      m.invoke( instance, l );
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  public static void exitLoop( Loop l ) {
    Method m = instance.dispatch( (Statement)l, "exitLoop" );

    if( m == null ) {
      System.err.println( "WARNING: cppc.compiler.analysis.SymbolicAnalyzer." +
        "exitLoop() not implemented for " + l.getClass() );

      return;
    }

    try {
      m.invoke( instance, l );
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected void analyzeStatement( CppcConditionalJump jump ) {}

  protected void analyzeStatement( CppcExecutePragma pragma ) {}

  protected void analyzeStatement( CppcLabel label ) {}

  protected void analyzeStatement( CppcShutdownPragma shutdown ) {}

  protected void analyzeStatement( CompoundStatement compoundStatement ) {
    // We use a list for removal scheduling to avoid
    // ConcurrentModificationException(s) trashing our day

    Set<Identifier> commVariables =
      CommunicationAnalyzer.getCurrentCommunicationVariables();

    if( (commVariables != null) && (!commVariables.isEmpty()) ) {
      SetOperations<Identifier> setOps = new SetOperations<Identifier>();
      Iterator iter = compoundStatement.getChildren().iterator();
      while( iter.hasNext() ) {
        CppcStatement next = (CppcStatement)iter.next();
        if( !setOps.setIntersection( next.getPartialGenerated(),
          commVariables ).isEmpty() ) {

          this.analyzeStatement( next, knownSymbols );
        }
      }
    }
  }

  protected void analyzeStatement( ContinueStatement stmt ) {}

  protected void analyzeStatement( DeclarationStatement stmt ) {
    Declaration declaration = stmt.getDeclaration();

    // Only variables can be constants (duh)
    if( declaration instanceof VariableDeclaration ) {
      VariableDeclaration vd = (VariableDeclaration)declaration;
      for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
        VariableDeclarator vdeclarator =
          (VariableDeclarator)vd.getDeclarator( i );
        if( vdeclarator.getInitializer() != null ) {
          List<Expression> initializers = (List<Expression>)
            vdeclarator.getInitializer().getChildren();
          List<Expression> folded = new ArrayList<Expression>(
            initializers.size() );

          // Analyze initializers
          for( int j = 0; j < initializers.size(); j++ ) {
            folded.add( SymbolicExpressionAnalyzer.analyzeExpression(
              initializers.get( j ), knownSymbols ) );
          }

          // Add folded values to list
          Identifier id = (Identifier)vdeclarator.getSymbol();
          this.addKnownSymbol( knownSymbols, id, folded );
        }
      }
    }
  }

  protected void analyzeStatement( ExpressionStatement s ) {
    SymbolicExpressionAnalyzer.analyzeExpression( s.getExpression(),
      knownSymbols );
  }

  protected void analyzeStatement( GotoStatement s ) {
    SymbolicExpressionAnalyzer.analyzeExpression( s.getValue(), knownSymbols );

    // If the destination label is located beyond the goto (forward jump), all
    // symbols being generated up to it should be removed. Else, we can
    // proceed as usual, since the symbols were removed when finding the label
    Expression target = s.getValue();
    DepthFirstIterator iter = new DepthFirstIterator(
      s.getProcedure().getBody() );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Expression.class );

    while( iter.hasNext() ) {
      Object obj = iter.next();

      // Label: check if it matches the target
      if( obj instanceof Label ) {
        if( target.equals( ((Label)obj).getName() ) ) {
          return;
        }
      }

      // Goto statement: begin processing until we find the label
      if( obj instanceof GotoStatement ) {
        if( obj == s ) {
          break;
        }
      }
    }

    // Now, iterate until finding the label, removing all generated symbols
    Set<Identifier> removeInc = new HashSet<Identifier>();
    while( iter.hasNext() ) {
      try {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class );
        if( cppcStatement.getStatement() instanceof Label ) {
          if(target.equals( ((Label)cppcStatement.getStatement()).getName()) ) {
            break;
          }
        }

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
  }

  protected void analyzeStatement( IfStatement s ) {
    if( CommunicationAnalyzer.symbolicAnalysis ) {
      return;
    }

    // Fold control expression
    Expression control = SymbolicExpressionAnalyzer.analyzeExpression(
      s.getControlExpression(), knownSymbols );

    if( control instanceof MultiExpression ) {
      MultiExpression safeControl = (MultiExpression)control;
//       SymbolicAnalyzer.idMask = safeControl.getVar();
      SymbolicAnalyzer.idMask.push( safeControl.getVar() );
      Statement thenBranch = s.getThenStatement();
      Statement elseBranch = s.getElseStatement();

      for( Expression key: safeControl.getValueSet() ) {
        Expression controlValue = safeControl.getValue( key );
//         this.valueMask = safeControl.getKeyOf( controlValue );
        SymbolicAnalyzer.valueMask.push( safeControl.getKeyOf( controlValue ) );

        IfStatement virtual = new IfStatement( (Expression)
          controlValue.clone(), new NullStatement() );
        virtual.setThenStatement( thenBranch );
        if( elseBranch != null ) {
          virtual.setElseStatement( elseBranch );
        }
        virtual.setParent( s.getParent() );

        this.analyzeStatement( virtual );
        SymbolicAnalyzer.valueMask.pop();
      }

      s.setThenStatement( thenBranch );
      if( elseBranch != null ) {
        s.setElseStatement( elseBranch );
      }
//       this.idMask = null;
//       this.valueMask = null;
      SymbolicAnalyzer.idMask.pop();
      return;
    }

    // If we reduced the control expression to a boolean literal, analyze the
    // corresponding branch as if there was no If
    if( control instanceof Literal ) {
      Expression trueLiteral =
        SymbolicExpressionAnalyzer.instance.buildBooleanLiteral( true );
      CppcStatement branch = null;
      if( control.equals( trueLiteral ) ) {
        branch = (CppcStatement)s.getThenStatement();
      } else {
        branch = (CppcStatement)s.getElseStatement();
      }

      // If control is false and there's no else branch, this will be null
      if( branch != null ) {
        this.analyzeStatement( branch, knownSymbols );
      }
      return;
    }

    // Else, create copies of the symbols map, since we do not want new symbols
    // to be added inside If branches for outside the If
    Map symbolsCopyThen = new HashMap( knownSymbols );
    Map symbolsCopyElse = new HashMap( knownSymbols );
    Map oldSymbols = knownSymbols;

    // Analyze THEN branch
    this.analyzeStatement( (CppcStatement)s.getThenStatement(),
      symbolsCopyThen );

    // Analyze ELSE branch
    if( s.getElseStatement() != null ) {
      this.analyzeStatement( (CppcStatement)s.getElseStatement(),
        symbolsCopyElse );
    }

    // Recover old symbols (overwritten by calling this.analyzeStatement)
    knownSymbols = oldSymbols;

    // Remove removed symbols (in any branch) from symbols pool
    Set<Identifier> thenSymbols = (Set<Identifier>)symbolsCopyThen.keySet();
    Set<Identifier> elseSymbols = (Set<Identifier>)symbolsCopyElse.keySet();

    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    Set<Identifier> thenRemoved = setOps.setMinus(
      (Set<Identifier>)knownSymbols.keySet(), thenSymbols );
    Set<Identifier> elseRemoved = setOps.setMinus(
      (Set<Identifier>)knownSymbols.keySet(), elseSymbols );

    Set<Identifier> remove = new HashSet<Identifier>( knownSymbols.size() );
    remove.addAll( thenRemoved );
    remove.addAll( elseRemoved );

    // Remove possibly modified constants from constants pool
    for( Identifier id: (Set<Identifier>)symbolsCopyThen.keySet() ) {
      if( knownSymbols.containsKey( id ) ) {
        if( !knownSymbols.get( id ).equals( symbolsCopyThen.get( id ) ) ) {
          // This symbol has been modified. Is it assigned the same value in
          // the else branch?
          if( symbolsCopyThen.get( id ).equals( symbolsCopyElse.get( id ) ) ) {
            // If so, swap value, and remove the symbol from the else symbols
            // list to avoid duplicate analysis
            knownSymbols.put( id, symbolsCopyThen.get( id ) );
            symbolsCopyElse.remove( id );
          } else {
            // Else, just remove the symbol from the known symbols list, and
            // also from the else symbols list to avoid duplicate analysis
            remove.add( id );
            symbolsCopyElse.remove( id );
          }
        }
      }
    }

    for( Identifier id: (Set<Identifier>)symbolsCopyElse.keySet() ) {
      if( knownSymbols.containsKey( id ) ) {
        if( !knownSymbols.get( id ).equals( symbolsCopyThen.get( id ) ) ) {
          // This symbol has been modified. It cannot have the same value as in
          // the THEN branch, or it would have been detected before. Therefore,
          // it needs to be removed.
          remove.add( id );
        }
      }
    }

    // Effectively remove keys
    for( Identifier id: remove ) {
      knownSymbols.remove( id );
    }
  }

  protected Set<Identifier> removeLoopCarriedSymbols( Loop l ) {
    Statement body = l.getBody();
    DepthFirstIterator iter = new DepthFirstIterator( body );
    iter.pruneOn( Loop.class ); // Loops are handled recursively
    iter.pruneOn( Expression.class ); // Only statements are analyzed

    CppcStatement cppcStatement = null;
    Set<Identifier> consumed = new HashSet<Identifier>();
    Set<Identifier> generated = new HashSet<Identifier>();
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    while( iter.hasNext() ) {
      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {
        continue;
      }

      Statement stmt = cppcStatement.getStatement();
      // If this is a loop: recursive call
      if( stmt instanceof Loop ) {
        // It will probably get analyzed in time?
        continue;
      }

      // If this CppcStatement contains a non-registered function call: remove
      // all identifier parameters from symbols list.
      DepthFirstIterator stmtIter = new DepthFirstIterator( stmt );
      stmtIter.next(); //Discharge statement
      stmtIter.pruneOn( CppcStatement.class ); // Do not descend into cppc stmts
      while( stmtIter.hasNext() ) {
        try {
          FunctionCall call = (FunctionCall)stmtIter.next( FunctionCall.class );
          if( !CppcRegisterManager.isRegistered((Identifier)call.getName())) {
            for( int i = 0; i < call.getNumArguments(); i++ ) {
              Expression arg = call.getArgument( i );
              if( arg instanceof Identifier ) {
                if( knownSymbols.containsKey( (Identifier)arg ) ) {
                  knownSymbols.remove( (Identifier)arg );
                }
              }
            }
          }
        } catch( NoSuchElementException e ) {}
      }

      consumed.addAll( setOps.setMinus( cppcStatement.getConsumed(),
        generated ) );
      generated.addAll( cppcStatement.getGenerated() );
    }

    Set<Identifier> carried = setOps.setIntersection(
      consumed, generated );
    Set<Identifier> keys = (Set<Identifier>)knownSymbols.keySet();
    Set<Identifier> carriedSymbols = setOps.setIntersection( carried, keys );

    return carriedSymbols;
  }

  protected void calculateCarriedSymbols( Identifier loopVar, IfStatement s,
    Set<Identifier> carriedSymbols ) {

    Expression control = SymbolicExpressionAnalyzer.analyzeExpression(
      s.getControlExpression(), knownSymbols );
    Set<Identifier> generatedInIf = new HashSet<Identifier>();

    // Check if any of the carried symbols is generated inside the statement
    DepthFirstIterator iter = new DepthFirstIterator( s );
    boolean relevant = false;
    while( iter.hasNext() && !relevant) {
      try {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class );
        for( Identifier id: cppcStatement.getGenerated() ) {
          if( carriedSymbols.contains( id ) ) {
            generatedInIf.add( id );
          }
        }
      } catch( NoSuchElementException e ) {}
    }

    if( generatedInIf.isEmpty() ) {
      return;
    }

    Expression trueLiteral =
      SymbolicExpressionAnalyzer.instance.buildBooleanLiteral( true );
    if( control instanceof MultiExpression ) {
      MultiExpression safeControl = (MultiExpression)control;
      Map generatedEvolution = new HashMap();
      for( Identifier id: generatedInIf ) {
        MultiExpression mexpr = new MultiExpression( safeControl.getVar() );
        if( knownSymbols.containsKey( id ) ) {
          Expression firstIterationValue = (Expression)
            safeControl.getKeyOf( (Expression)
            safeControl.getChildren().get(0) );
          mexpr.addExpression( firstIterationValue, (Expression)
            ((Expression)((List)knownSymbols.get( id )).get(0)).clone() );
          generatedEvolution.put( id, mexpr );
        } else {
          generatedInIf.remove( id );
        }
      }

      Map localKnown = new HashMap( knownSymbols );
      Map oldKnown = knownSymbols;
      knownSymbols = localKnown;

      for( int i = 0; i < control.getChildren().size() - 1; i++ ) {
        Expression controlValue = (Expression)control.getChildren().get(i);
        Statement branch = null;
        if( !(controlValue instanceof Literal) ) {
          return;
        }

        if( trueLiteral.equals( controlValue ) ) {
          branch = s.getThenStatement();
        } else {
          branch = s.getElseStatement();
        }

        if( branch != null ) {
          this.analyzeStatement( (CppcStatement)branch, localKnown );
        }

        for( Identifier id: generatedInIf ) {
          MultiExpression idEvolution = (MultiExpression)
            generatedEvolution.get( id );
          Expression value = (Expression)((List)localKnown.get( id )).get(0);
          Expression iterationValue = safeControl.getKeyOf( (Expression)
            control.getChildren().get( i+1 ) );
          idEvolution.addExpression( iterationValue, value );
        }
      }

      knownSymbols = oldKnown;
      for( Identifier id: generatedInIf ) {
        List values = new ArrayList(1);
        values.add( generatedEvolution.get(id) );
        knownSymbols.put( id, values );
        System.out.println( "Added induction variable " + id + ": " + values);
      }
    } else {
      if( !(control instanceof Literal) ) {
        return;
      }

      Statement branch = null;
      if( trueLiteral.equals( control ) ) {
        branch = s.getThenStatement();
      } else {
        branch = s.getElseStatement();
      }

      if( branch != null ) {
        this.calculateCarriedSymbols( loopVar, branch, generatedInIf );
      }
    }
  }

  protected void calculateCarriedSymbols( Identifier loopVar, Statement body,
    Set<Identifier> carriedSymbols ) {

//     if( !knownSymbols.containsKey( loopVar ) ) {
//       return;
//     }

    // For each carried identifier, find statements generating it
    for( Identifier carried: carriedSymbols ) {
      DepthFirstIterator bodyIter = new DepthFirstIterator( body );
      bodyIter.pruneOn( IfStatement.class );
      while( bodyIter.hasNext() ) {
        try {
          CppcStatement cppcStatement = (CppcStatement)bodyIter.next(
            CppcStatement.class );
          if( cppcStatement.getStatement() instanceof IfStatement ) {
            this.calculateCarriedSymbols( loopVar,
              (IfStatement)cppcStatement.getStatement(), carriedSymbols );
            continue;
          }

          if( cppcStatement.getGenerated().contains( carried ) ) {
            Map localKnown = new HashMap();
            for( Identifier consumed: cppcStatement.getConsumed() ) {
              if( knownSymbols.containsKey( consumed ) ) {
                localKnown.put( consumed, knownSymbols.get( consumed ) );
              }
            }

            // Only accept known scalar variables (no arrays)
            if( knownSymbols.containsKey( loopVar ) &&
              (((List)knownSymbols.get( carried )).size() == 1) ) {

//               MultiExpression mexpr = (MultiExpression)
//                 ((List)knownSymbols.get( loopVar )).get( 0 );
//               MultiExpression carriedMexpr = new MultiExpression( loopVar );
//               Expression firstIterationValue = (Expression)
//                 ((List)knownSymbols.get( carried )).get( 0 );
//               carriedMexpr.addExpression(
//                 mexpr.getKeyOf( (Expression)mexpr.getChildren().get(0) ),
//                 (Expression)firstIterationValue.clone() );
// 
//               for( int i = 1; i < mexpr.getChildren().size(); i++ ) {
//                 Expression expr = (Expression)mexpr.getChildren().get( i );
//                 List values = new ArrayList( 1 );
//                 values.add( expr );
//                 localKnown.put( loopVar, values );
//                 SymbolicAnalyzer.analyzeStatement( cppcStatement,
//                   localKnown );
//                 Expression finalValue = (Expression)
//                   ((List)localKnown.get( carried ) ).get( 0 );
// 
//                 carriedMexpr.addExpression( expr,
//                   (Expression)finalValue.clone() );
//               }
// 
//               List value = new ArrayList(1);
//               value.add( carriedMexpr );
//               knownSymbols.put( carried, value );
              Expression values = this.simulateIteration( (MultiExpression)
                ((List)knownSymbols.get( loopVar )).get( 0 ), localKnown,
                loopVar, carried, cppcStatement );
              List value = new ArrayList( 1 );
              value.add( values );
              knownSymbols.put( carried, value );
            } else {
              knownSymbols.remove( carried );
            }
          }
        } catch( NoSuchElementException e ) {}
      }
    }
  }

  private Expression simulateIteration( MultiExpression iterValues,
    Map localKnown, Identifier loopVar, Identifier carried,
    CppcStatement statement ) {

    MultiExpression carriedMexpr = new MultiExpression( loopVar );
    Expression firstIterationValue = (Expression)
      ((List)localKnown.get( carried )).get( 0 );

    carriedMexpr.addExpression( iterValues.getKeyOf(
      (Expression)iterValues.getChildren().get( 0 ) ),
      (Expression)firstIterationValue.clone() );

    for( int i = 1; i < iterValues.getChildren().size(); i++ ) {
      Expression expr = (Expression)iterValues.getChildren().get( i );

      if( expr instanceof MultiExpression ) {
        List values = new ArrayList( 1 );
        values.add( expr );
        localKnown.put( loopVar, values );
        SymbolicAnalyzer.analyzeStatement( statement, localKnown );

        Expression value = this.simulateIteration( (MultiExpression)expr,
          localKnown, loopVar, carried, statement );
        carriedMexpr.addExpression( expr, (Expression)value.clone() );
      } else {
        List values = new ArrayList( 1 );
        values.add( expr );
        localKnown.put( loopVar, values );
        SymbolicAnalyzer.analyzeStatement( statement, localKnown );
        Expression finalValue = (Expression)
          ((List)localKnown.get( carried )).get( 0 );

        carriedMexpr.addExpression( expr, (Expression)finalValue.clone() );
      }
    }

    return carriedMexpr;
  }

  protected void analyzeStatement( WhileLoop l ) {
    this.removeLoopCarriedSymbols( l );
    SymbolicExpressionAnalyzer.analyzeExpression( l.getCondition(),
      knownSymbols );
    this.analyzeStatement( (CppcStatement)l.getBody(), knownSymbols );
  }

  protected static void addKnownSymbol( Map knownSymbols, Identifier id,
    List values ) {

    SymbolicAnalyzer.addKnownSymbol( knownSymbols, id, values, id );
  }

  private static Expression convertToInt( Expression value ) {
    if( value == null ) {
      return null;
    }

    if( value instanceof MultiExpression ) {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression( safeValue.getVar() );

      for( Expression key: safeValue.getValueSet() ) {
        mexpr.addExpression( key, SymbolicAnalyzer.convertToInt(
          safeValue.getValue( key ) ) );
      }

      return mexpr;
    }

    if( value instanceof DoubleLiteral ) {
      return new IntegerLiteral( (int)((DoubleLiteral)value).getValue() );
    }

    if( value instanceof FloatLiteral ) {
      return new IntegerLiteral( (int)((FloatLiteral)value).getValue() );
    }

    return (Expression)value.clone();
  }

  private static List convertToInt( List values ) {
    List convertedValues = new ArrayList( values.size() );

    for( int i = 0; i < values.size(); i++ ) {
      // If list -> recursive call
      if( values.get( i ) instanceof List ) {
        convertedValues.add( SymbolicAnalyzer.convertToInt(
          (List)values.get( i ) ) );
        continue;
      }

      convertedValues.add( SymbolicAnalyzer.convertToInt(
        (Expression)values.get( i ) ) );
    }

    return convertedValues;
  }

  private static Expression convertToFloat( Expression value ) {
    if( value == null ) {
      return null;
    }

    if( value instanceof MultiExpression ) {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression( safeValue.getVar() );

      for( Expression key: safeValue.getValueSet() ) {
        mexpr.addExpression( key, SymbolicAnalyzer.convertToFloat(
          safeValue.getValue( key ) ) );
      }

      return mexpr;
    }

    if( value instanceof IntegerLiteral ) {
      return new FloatLiteral( ((IntegerLiteral)value).getValue() );
    }

    if( value instanceof DoubleLiteral ) {
      return new FloatLiteral( ((DoubleLiteral)value).getValue() );
    }

    return (Expression)value.clone();
  }

  private static List convertToFloat( List values ) {
    List convertedValues = new ArrayList( values.size() );

    for( int i = 0; i < values.size(); i++ ) {
      // If list -> recursive call
      if( values.get( i ) instanceof List ) {
        convertedValues.add( SymbolicAnalyzer.convertToFloat(
          (List)values.get( i ) ) );
        continue;
      }

      convertedValues.add( SymbolicAnalyzer.convertToFloat(
        (Expression)values.get(i) ) );
    }

    return convertedValues;
  }

  private static Expression convertToDouble( Expression value ) {
    if( value == null ) {
      return null;
    }

    if( value instanceof MultiExpression ) {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression( safeValue.getVar() );

      for( Expression key: safeValue.getValueSet() ) {
        mexpr.addExpression( key, SymbolicAnalyzer.convertToDouble(
          safeValue.getValue( key ) ) );
      }

      return mexpr;
    }

    if( value instanceof IntegerLiteral ) {
      return new DoubleLiteral( ((IntegerLiteral)value).getValue() );
    }

    if( value instanceof FloatLiteral ) {
      return new DoubleLiteral( ((FloatLiteral)value).getValue() );
    }

    return (Expression)value.clone();
  }

  private static List convertToDouble( List values ) {
    List convertedValues = new ArrayList( values.size() );

    for( int i = 0; i < values.size(); i++ ) {
      // If list -> recursive call
      if( values.get( i ) instanceof List ) {
        convertedValues.add( SymbolicAnalyzer.convertToDouble( (List)
          values.get( i ) ) );
        continue;
      }

      convertedValues.add( SymbolicAnalyzer.convertToDouble(
        (Expression)values.get( i ) ) );
    }

    return convertedValues;
  }

  protected static void addKnownSymbol( Map knownSymbols, Identifier id,
    List values, Traversable ref ) {

    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        ref, id );
    } catch( SymbolNotDefinedException e ) {
      return;
    } catch( SymbolIsNotVariableException e ) {
      return;
    }

    if( vd == null ) {
     return;
    }

    List symbolValues = values;

    if( vd.getSpecifiers().contains( Specifier.INT ) ) {
      symbolValues = SymbolicAnalyzer.convertToInt( values );
    }

    if( vd.getSpecifiers().contains( Specifier.FLOAT ) ) {
      symbolValues = SymbolicAnalyzer.convertToFloat( values );
    }

    if( vd.getSpecifiers().contains( Specifier.DOUBLE ) ) {
      symbolValues = SymbolicAnalyzer.convertToDouble( values );
    }

    SymbolicAnalyzer.putKnownSymbol( knownSymbols, id, symbolValues );
  }

  private static boolean checkRecurrency( Identifier id, List values ) {
    for( int i = 0; i < values.size(); i++ ) {
      if( values.get( i ) instanceof List ) {
        if( SymbolicAnalyzer.checkRecurrency( id, (List)values.get( i ) ) ) {
          return true;
        }

        continue;
      }

      Expression expr = (Expression)values.get( i );
      if( expr != null ) {
        DepthFirstIterator iter = new DepthFirstIterator( expr );
        while( iter.hasNext() ) {
          try {
            Identifier innerId = (Identifier)iter.next( Identifier.class );
            if( innerId.equals( id ) ) {
              System.out.println( "Avoiding register of " + id + " = " +
                values );
              return true;
            }
          } catch( NoSuchElementException e ) {}
        }
      }
    }

    return false;
  }

  private static void putMaskedSymbol( MultiExpression mexpr, Expression value,
    int depth ) {

    if(!mexpr.getVar().equals(SymbolicAnalyzer.idMask.elementAt(depth))) {
      System.err.println( "ERROR: IDMASK STACK BUG" );
      System.exit( 0 );
    }

    Expression key = SymbolicAnalyzer.valueMask.elementAt(depth);
    if( depth == SymbolicAnalyzer.idMask.size()-1 ) {
      mexpr.addExpression( key, value );
      return;
    } else {
      Expression nextLevel = mexpr.getValue( key );
      if( !(nextLevel instanceof MultiExpression) ) {
        nextLevel = new MultiExpression(
          SymbolicAnalyzer.idMask.elementAt( depth+1 ) );
        mexpr.addExpression( key, nextLevel );
      }

      SymbolicAnalyzer.putMaskedSymbol( (MultiExpression)nextLevel, value,
        ++depth );
    }
  }

  private static void putKnownSymbol( Map knownSymbols, Identifier id, List
    values ) {

//     if( SymbolicAnalyzer.idMask == null ) {
    if( SymbolicAnalyzer.idMask.isEmpty() ) {
      knownSymbols.put( id, values );
      return;
    }

    List currentValue = (List)knownSymbols.get( id );
    if( currentValue != null ) {
      if( currentValue.size() == 1 ) {
        Expression value = (Expression)currentValue.get( 0 );
        if( value instanceof MultiExpression ) {
//           MultiExpression mexpr = (MultiExpression)value;
//           if( !mexpr.getVar().equals( SymbolicAnalyzer.idMask ) ) {
//             System.err.println( "ERROR: TOO MANY MASKS." );
//             System.exit( 0 );
//           }

//           mexpr.addExpression( SymbolicAnalyzer.valueMask, (Expression)
//             ((Expression)values.get( 0 )).clone() );
          SymbolicAnalyzer.putMaskedSymbol( (MultiExpression)value,
            (Expression)((Expression)values.get(0)).clone(), 0 );
          return;
        }
      }
    }

//     MultiExpression mexpr = new MultiExpression( SymbolicAnalyzer.idMask );
    MultiExpression mexpr = new MultiExpression(
      SymbolicAnalyzer.idMask.elementAt( 0 ) );
    SymbolicAnalyzer.putMaskedSymbol( mexpr,
      (Expression)((Expression)values.get(0)).clone(), 0 );
//     mexpr.addExpression( SymbolicAnalyzer.valueMask, (Expression)
//       ((Expression)values.get( 0 )).clone() );
    values = new ArrayList<Expression>( 1 );
    values.add( mexpr );
    knownSymbols.put( id, values );
  }

  protected void analyzeStatement( Label label ) {
    // Remove all variables in remove upon label
    if( removeUponLabel.containsKey( label.getName() ) ) {
      for( Identifier id: removeUponLabel.get( label.getName() ) ) {
        knownSymbols.remove( id );
      }

      removeUponLabel.remove( label.getName() );
    }

    // A label is a potential jump destination. All constants being generated
    // from this label up to the last GOTO with it as a target should be
    // removed before continuing.
    Identifier target = label.getName();
    Procedure proc = label.getProcedure();
    DepthFirstIterator iter = new DepthFirstIterator( proc );
    iter.pruneOn( Expression.class );

    // Position iterator over analyzed label
    CppcStatement cppcStatement = null;
    boolean stop = false;
    while( !stop ) {
      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );

        if( cppcStatement.getStatement() == label ) {
          stop = true;
        }
      } catch( NoSuchElementException e ) {
        System.err.println( "ERROR: Matching label not found. At " +
          "cppc.compiler.analysis.SymbolicAnalyzer.analyzeStatement(" +
          " cetus.hir.Label )" );
        System.exit( 0 );
      }
    }

    Set<Identifier> remove = new HashSet<Identifier>();
    Set<Identifier> removeInc = new HashSet<Identifier>( knownSymbols.size() );
    while( iter.hasNext() ) {
      try {
        cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class );

        // Schedule generated symbols for removal
        for( Identifier id: cppcStatement.getGenerated() ) {
          if( knownSymbols.containsKey( id ) ) {
            removeInc.add( id );
          }
        }

        // If the CppcStatement contains a GotoStatement and its target is the
        // label being analyzed, commit current symbols for removal list
        if( cppcStatement.getStatement() instanceof GotoStatement ) {
          GotoStatement gotoStmt = (GotoStatement)cppcStatement.getStatement();
          if( gotoStmt.getValue().equals( target ) ) {
            remove.addAll( removeInc );
            removeInc.clear();
          }
        }
      } catch( NoSuchElementException e ) {}
    }

    for( Identifier id: remove ) {
      knownSymbols.remove( id );
    }
  }

  protected void analyzeStatement( NullStatement nullStatement ) {}
  protected void analyzeStatement( CppcNonportableFunctionMark s ) {}
}
