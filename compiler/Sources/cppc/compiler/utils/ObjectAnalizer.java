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




package cppc.compiler.utils;

import cetus.hir.Annotation;
import cetus.hir.ArrayAccess;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.TranslationUnit;
import cetus.hir.Traversable;
import cetus.hir.UnaryExpression;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.analysis.StatementAnalyzer;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.exceptions.TypeNotSupportedException;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public final class ObjectAnalizer {

  private static final String initString( String [] i ) {

    String s = i[0];
    for( int j = 1; j < i.length; j++ ) {
      s = s + " " + i[ j ];
    }

    return s;
  }

  private ObjectAnalizer() {}

  public static final Procedure getProcedure(Program program, Identifier name) {
    DepthFirstIterator iter = new DepthFirstIterator( program );
    while( iter.hasNext() ) {
      try {
        Procedure proc = (Procedure)iter.next( Procedure.class );
        if( proc.getName().equals( name ) ) {
          return proc;
        }
      } catch( NoSuchElementException e ) {}
    }

    return null;
  }

  public static final Procedure findMainProcedure( Program program ) {

    DepthFirstIterator iter = new DepthFirstIterator( program );
    Procedure mainProc = null;

    try {
      while( iter.hasNext() ) {
        Procedure proc = (Procedure)iter.next( Procedure.class );
        if( ObjectAnalizer.isMainProcedure( proc ) ) {
          if( mainProc == null ) {
            mainProc = proc;
          } else {
            System.err.println( "error: Duplicate entry procedures." );
            System.err.println( "Your application contains more than one "+
              "\"main\" procedure. Cannot perform analyses." );
            System.err.println( "Exiting..." );
            System.exit( 0 );
          }
        }
      }
    } catch( NoSuchElementException e ) {}

    if( mainProc != null ) {
      return mainProc;
    }

    System.err.println( "BUG: No main procedure found." );
    System.err.println( "\tIn cppc.compiler.utils.ObjectAnalizer" );
    System.exit( 0 );
    return null;
  }

  public static final boolean isCppcProcedure( Procedure procedure ) {

    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    try {
      CppcLabel cppcLabel = (CppcLabel)iter.next( CppcLabel.class );
    } catch( NoSuchElementException e ) {
      return false;
    }

    return true;
  }

  public static final boolean isMainProcedure( Procedure p ) {

    // Port from the SUIF-ipanalysis module
    String procName = p.getName().toString();
    return( procName.equals( "main" ) ||
      procName.equals( "__MAIN" ) ||
      procName.equals( "MAIN_" ) );
  }

  public static final ProcedureParameter idToProcParam( List parameters,
    Identifier parameter, boolean recursive ) {

    if( parameter.equals( new Identifier( "..." ) ) ) {
      return ProcedureParameter.VARARGS;
    } else {
      // Parameters is a List<Declaration>, so we need to extract Declaration
      // parameters, once at a time
      Iterator iter = parameters.iterator();

      for( int i = 0; iter.hasNext(); i++ ) {
        Declaration declaration = (Declaration)iter.next();
        Identifier id = (Identifier)declaration.getDeclaredSymbols().get(0);
        if( id.equals( parameter ) ) {
          return new ProcedureParameter( i );
        }
      }
    }

    // When called from outside, recursive will be true and thus this function
    // will search for "system" names
    // When called recursively, this if-statement will be skipped
    if( recursive ) {
      ProcedureParameter returnVal = idToProcParam( parameters, new Identifier(
        "__" + parameter.toString() ), false );
      if( returnVal != null ) {
        return returnVal;
      }

      returnVal = idToProcParam( parameters, new Identifier(
        parameter.toString() + "_" ), false );
      return returnVal;
    }

    return null;
  }

  public static final Set<ProcedureParameter> setIdToSetProcParam(
    Declaration procedureDeclaration, Set<Identifier> localParams ) {

    List declaredParams = null;

    // The declaration must be a Procedure (when it is a user-supplied
    // procedure, with available code)
    // or either a VariableDeclaration (when it is a prototype)
    if( procedureDeclaration instanceof Procedure ) {
      declaredParams = ((Procedure)procedureDeclaration).getParameters();
    }

    if( procedureDeclaration instanceof VariableDeclaration ) {
      declaredParams =
        ((VariableDeclaration)procedureDeclaration).getDeclarator(
          0 ).getParameters();
    }

    if( declaredParams == null ) {
      System.err.println( "ERROR: BUG found in " + ObjectAnalizer.class );
      System.err.println( "\tMethod: "+
          "setIdToSetProcParam( cetus.hir.Declaration, "+
          "java.util.Set<Identifier> )" );
      System.err.println( "\tCause : Passed Declaration not instanceof "+
        " VariableDeclaration or Procedure" );
      System.exit( 0 );
    }

    Iterator<Identifier> iter = localParams.iterator();
    Set<ProcedureParameter> returnSet = new HashSet<ProcedureParameter>();

    while( iter.hasNext() ) {
      ProcedureParameter procParam = idToProcParam( declaredParams, iter.next(),
        true );
      if( procParam != null ) {
        returnSet.add( procParam );
      }
    }

    return returnSet;
  }

  public static final SymbolTable getSymbolTable( Traversable t ) {

    if( t instanceof SymbolTable ) {
      return (SymbolTable)t;
    }

    if( t == null ) {
      return null;
    }

    return getSymbolTable( t.getParent() );
  }

  public static final boolean matchStringWithArray( String text,
    String[] parts ) {

    for( int i = 0; i < parts.length; i++ ) {

      if( !text.startsWith( parts[i] ) ) {
        return false;
      }

      text = text.replaceFirst( parts[i], "" );
      text = text.trim();
    }

    return( text.length() == 0 );
  }

  public static Traversable getParentOfClass( Traversable t, Class c ) {

    // When t is null c.isInstance( t ) is true
    while( (t != null) && (!c.isInstance( t )) ) {
      t = t.getParent();
    }

    return t;
  }

  public static Set<Identifier> globalDeclarationsToSet(
    Set<VariableDeclaration> globals ) {

    HashSet<Identifier> set = new HashSet<Identifier>( globals.size() );

    for( VariableDeclaration vd: globals ) {
      for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
        set.add( (Identifier)vd.getDeclarator( i ).getSymbol() );
      }
    }

    return set;
  }

  private static boolean characterizeBlock( CppcStatement stmt, Statement end,
    Set<Identifier> generated, Set<Identifier> consumed,
    Set<Identifier> initialized ) {

    boolean stop = false;

    // Find last cppcStatement
    DepthFirstIterator iter = new DepthFirstIterator( stmt );
    CppcStatement cppcStatement = null;
    while( iter.hasNext() ) {
      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );
      } catch( Exception e ) {}

      if( cppcStatement == end ) {
        stop = true;
        break;
      }
    }

    simpleBlockCharacterization( stmt, cppcStatement, generated, consumed,
      initialized );
    return stop;
  }

  private static boolean characterizeBlock( IfStatement ifStmt, Statement end,
    Set<Identifier> generated, Set<Identifier> consumed,
    Set<Identifier> initialized ) {

    boolean stop;

    // Then part
    Set<Identifier> localGenerated = new HashSet<Identifier>( generated );
    CppcStatement thenStmt = (CppcStatement)ifStmt.getThenStatement();
    stop = characterizeBlock( thenStmt, end, localGenerated, consumed,
      initialized );
    if( stop ) {
      return stop;
    }

    // Else part
    CppcStatement elseStmt = (CppcStatement)ifStmt.getElseStatement();
    if( elseStmt != null ) {
      localGenerated = new HashSet<Identifier>( generated );
      stop = characterizeBlock( elseStmt, end, localGenerated, consumed,
        initialized );
    }

    return stop;
  }

  private static void simpleBlockCharacterization( Statement begin,
    Statement end, Set<Identifier> generated, Set<Identifier> consumed,
    Set<Identifier> initialized ) {

    // Find the CompoundStatement containing this Statements
    // Use the Procedure body, since it cannot be assumed that both
    // statements will be on the same leaf StatementList
    Procedure proc = (Procedure)ObjectAnalizer.getParentOfClass( begin,
      Procedure.class );
    CompoundStatement statementList = proc.getBody();
    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();

    // Position the iterator over the "begin" statement
    CppcStatement cppcStatement = null;
    while( cppcStatement != begin ) {
      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {
        System.out.println( "BUG: Cannot find first Statement in "+
          "cppc.compiler.utils.ObjectAnalizer.simpleBlockCharacterization" );
        System.exit( 0 );
      }
    }

    // Initialize "consumed" and "generated" by processing the begin statement
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    consumed.addAll( setOps.setMinus( cppcStatement.getConsumed(),
      generated ) );
    consumed.addAll( setOps.setMinus( globalDeclarationsToSet(
      cppcStatement.getGlobalConsumed() ), generated ) );
    generated.addAll( cppcStatement.getGenerated() );
    generated.addAll( globalDeclarationsToSet(
      cppcStatement.getGlobalGenerated() ) );

    // Add declarations for global variables not defined in this scope
    SymbolTable table = (SymbolTable)ObjectAnalizer.getParentOfClass(
        cppcStatement, SymbolTable.class );
    if( !cppcStatement.getGlobalConsumed().isEmpty() ) {
      for( VariableDeclaration vd: cppcStatement.getGlobalConsumed() ) {
        List<Identifier> symbolsToAdd;
        // Encapsulation loss here: CommonBlock in generic code
        if( vd.getParent() instanceof CommonBlock ) {
          symbolsToAdd = (List<Identifier>)
            ((CommonBlock)vd.getParent()).getDeclaredSymbols();
        } else {
          symbolsToAdd = (List<Identifier>)vd.getDeclaredSymbols();
        }
        for( Identifier id: symbolsToAdd ) {
          // Add possibly necessary size-related declarations
          SymbolTable exTable =
            (SymbolTable)ObjectAnalizer.getParentOfClass( vd,
              SymbolTable.class );
          Expression sizeExpr =
            VariableSizeAnalizerFactory.getAnalizer().getSize( id,
              (Traversable)exTable  );
          if( sizeExpr != null ) {
            DepthFirstIterator exprIter = new DepthFirstIterator( sizeExpr );
            while( exprIter.hasNext() ) {
              try {
                Identifier sizeId = (Identifier)exprIter.next(
                  Identifier.class );
                VariableDeclaration sizeVd = analyzer.getVariableDeclaration(
                  (Traversable)exTable, sizeId );
                Identifier newSizeId = ObjectAnalizer.
                  addClonedVariableDeclaration( sizeVd, sizeId,
                    cppcStatement );
                if( newSizeId != sizeId ) {
                  sizeId.swapWith( newSizeId );
                }

                // If this declaration contains an initializer, add to
                // "generated" list
                if( ObjectAnalizer.getDeclarator( sizeVd,
                  sizeId ).getInitializer() != null ) {

                  initialized.add( newSizeId );
                }
              } catch( NoSuchElementException e ) {
              } catch( SymbolNotDefinedException e ) {
              } catch( SymbolIsNotVariableException e ) {}
            }
          }
        }

        for( Identifier id: (List<Identifier>)vd.getDeclaredSymbols() ) {
          Identifier newId = ObjectAnalizer.addClonedVariableDeclaration( vd,
            id, cppcStatement );
          if( (id != newId) && consumed.contains( id ) ) {
            consumed.remove( id );
            consumed.add( newId );
          }
        }
      }
    }

    iter.pruneOn( IfStatement.class );
    boolean stop = false;

    if( begin == end ) {
      stop = true;
    }

    if( cppcStatement.getStatement() instanceof IfStatement ) {
      stop = stop || ObjectAnalizer.characterizeBlock( (IfStatement)
        cppcStatement.getStatement(), end, generated, consumed, initialized );
    }

    if( stop ) {
      return;
    }

    // Iterate over the statements, generating characterization sets, until
    // "end" is reached
    while( iter.hasNext() ) {

      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {
        System.out.println( "BUG: Cannot find last Statement in "+
          "cppc.compiler.utils.ObjectAnalizer.characterizeBlock" );
        System.out.println( "I was seeking statement = " + end );
        System.exit( 0 );
      }

      consumed.addAll( setOps.setMinus( cppcStatement.getConsumed(),
        generated ) );
      consumed.addAll( setOps.setMinus( globalDeclarationsToSet(
        cppcStatement.getGlobalConsumed() ), generated ) );
      generated.addAll( cppcStatement.getGenerated() );
      generated.addAll( globalDeclarationsToSet(
        cppcStatement.getGlobalGenerated() ) );

      // Add declarations for global variables not defined in this scope
      table = (SymbolTable)ObjectAnalizer.getParentOfClass(
        cppcStatement, SymbolTable.class );
      VariableDeclaration clone = null;
      if( !cppcStatement.getGlobalConsumed().isEmpty() ) {
        for( VariableDeclaration vd: cppcStatement.getGlobalConsumed() ) {
          List<Identifier> symbolsToAdd;
          // Encapsulation loss here: CommonBlock in generic code
          if( vd.getParent() instanceof CommonBlock ) {
            symbolsToAdd = (List<Identifier>)
              ((CommonBlock)vd.getParent()).getDeclaredSymbols();
          } else {
            symbolsToAdd = (List<Identifier>)vd.getDeclaredSymbols();
          }
          for( Identifier id: symbolsToAdd ) {
            // Add possibly necessary size-related declarations
            SymbolTable exTable =
              (SymbolTable)ObjectAnalizer.getParentOfClass( vd,
                SymbolTable.class );
            Expression sizeExpr =
              VariableSizeAnalizerFactory.getAnalizer().getSize( id,
                (Traversable)exTable  );
            if( sizeExpr != null ) {
              DepthFirstIterator exprIter = new DepthFirstIterator(
                sizeExpr );
              while( exprIter.hasNext() ) {
                try {
                  Identifier sizeId = (Identifier)exprIter.next(
                    Identifier.class );
                  VariableDeclaration sizeVd =
                    analyzer.getVariableDeclaration( (Traversable)exTable,
                      sizeId );
                  Identifier newSizeId = ObjectAnalizer.
                    addClonedVariableDeclaration( sizeVd, sizeId,
                      cppcStatement );
                  if( newSizeId != sizeId ) {
                    sizeId.swapWith( newSizeId );
                  }

                  // If this declaration contains an initializer, add to
                  // "generated" list
                  if( ObjectAnalizer.getDeclarator( sizeVd,
                    sizeId ).getInitializer() != null ) {

                    initialized.add( newSizeId );
                  }
                } catch( NoSuchElementException e ) {
                } catch( SymbolNotDefinedException e ) {
                } catch( SymbolIsNotVariableException e ) {}
              }
            }
          }

          for( Identifier id: (List<Identifier>)vd.getDeclaredSymbols() ) {
            Identifier newId = ObjectAnalizer.addClonedVariableDeclaration( vd,
              id, cppcStatement );
            if( (id != newId) && consumed.contains( id ) ) {
              consumed.remove( id );
              consumed.add( newId );
            }
          }
        }
      }

      if( cppcStatement == end ) {
        stop = true;
      }

      if( cppcStatement.getStatement() instanceof IfStatement ) {
        // If statements do not generate for outside their block, so we don't
        // iterate through them normally. Notice that we have added the
        // consumed/generated items contained in the current CppcStatement,
        // since those are consumed/generated in the control expression, which
        // is always executed
        stop = stop || characterizeBlock(
          (IfStatement)cppcStatement.getStatement(), end, generated,
            consumed, initialized );
      }

      if( stop ) {
        return;
      }
    }
  }

  private static Identifier addClonedVariableDeclaration(
    VariableDeclaration vd, Identifier id, Traversable ref ) {

    SymbolTable table = (SymbolTable)ObjectAnalizer.getParentOfClass( ref,
      SymbolTable.class );
    VariableDeclaration localDecl = (VariableDeclaration)table.findSymbol( id );
    VariableDeclaration clone = null;
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();

    if( localDecl == null ) {
      clone = analyzer.cloneDeclaration( vd, id );
      VariableDeclaration ret = analyzer.addVariableDeclaration( clone,
        (Statement)ref );

      if( ret != clone ) {
        for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
          if( id.equals( vd.getDeclarator( i ).getSymbol() ) ) {
            return (Identifier)ret.getDeclarator( i ).getSymbol();
          }
        }
      }
    }

    return id;
  }

  public static void characterizeBlock( Statement begin, Statement end,
    Set<Identifier> generated, Set<Identifier> consumed,
    Set<Identifier> initialized) {

    // If we are not into a loop: trivial operation
    if( !LanguageAnalyzerFactory.getLanguageAnalyzer().insideLoop( begin ) ) {
      ObjectAnalizer.simpleBlockCharacterization( begin, end, generated,
        consumed, initialized );
      return;
    }

    // Get the loop body and the loop Statement
    Statement loopBody =
      LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody(
        begin );
    CppcStatement loopStatement =
      (CppcStatement)ObjectAnalizer.getParentOfClass( loopBody.getParent(),
        CppcStatement.class );

    // Get SetOps
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();

    // If the end is inside the same loop (maybe inside a different loop nested
    // in the big one): lucky day, it is trivial as well
    Statement endLoopBody =
      LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody( end );

    while( (endLoopBody != null) && (endLoopBody != loopBody) ) {
      // Trying to get the loop body of a loop body makes up for neverending fun
      endLoopBody = (Statement)endLoopBody.getParent();
      endLoopBody =
        LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody(
          endLoopBody.getParent() );
    }

    if( endLoopBody != null ) {
      ObjectAnalizer.simpleBlockCharacterization( begin, end, generated,
        consumed, initialized );

      // Add consumed/generated in the loop iteration
      consumed.addAll( setOps.setMinus( loopStatement.getConsumed(),
        generated ) );
      generated.addAll( loopStatement.getGenerated() );
      return;
    }

    // Else proceed to walk over the loop as the execution would

    // 1. Analyze the code from the begin up to the end of the loop
    // 1.1 Find the end of the loop
    CppcStatement loopEnd = null;
    DepthFirstIterator loopIter = new DepthFirstIterator( loopBody );
    try {
      while( loopIter.hasNext() ) {
        loopEnd = (CppcStatement)loopIter.next( CppcStatement.class );
      }
    } catch( NoSuchElementException e ) {}
    // 1.2 Analyze the begin->loopend bit
    ObjectAnalizer.simpleBlockCharacterization( begin, loopEnd, generated,
      consumed, initialized );
    // 1.3 Add the consumed/generated in the loop iteration
    consumed.addAll( setOps.setMinus( loopStatement.getConsumed(), generated ));
    generated.addAll( loopStatement.getGenerated() );

    // 2. Analyze the code from the beginning of the loop up to the requested
    // beginning. Take into account that the simpleBlockCharacterization
    // function automatically carries dependencies across calls, as it is called
    // using the same variables for storing generated and consumed variables
    // 2.1 Find the first loop statement
    loopIter.reset();
    CppcStatement loopBegin = null;
    try {
      loopBegin = (CppcStatement)loopIter.next( CppcStatement.class );
    } catch( NoSuchElementException e ) {
      System.err.println( "BUG: Loop doesn't contain any CppcStatement. In "+
        "cppc.compiler.utils.ObjectAnalizer.characterizeBlock()" );
      System.exit( 0 );
    }
    // 2.2 Analyze the code from the beginning of the loop up to the requested
    // beginning
    ObjectAnalizer.simpleBlockCharacterization( loopBegin, begin, generated,
      consumed, initialized );

    // 3. Analyze from the end of the loop up to the requested end
    ObjectAnalizer.simpleBlockCharacterization( loopEnd, end, generated,
      consumed, initialized );
  }

  public static void addRegisterPragmaBefore( Statement reference,
    Set<Identifier> registers ) {

    CppcRegisterPragma pragma = new CppcRegisterPragma();
    Iterator<Identifier> idIter = registers.iterator();
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( reference,
        CompoundStatement.class );
    Set<Identifier> sizeDependencies = new HashSet<Identifier>();

    while( idIter.hasNext() ) {
      Identifier id = idIter.next();
      Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize( id,
        statementList );
      pragma.addRegister( new CppcRegister( id, size ) );

      // Analyze the Expression for unregistered data
      if( size != null ) {
        ExpressionStatement statement = new ExpressionStatement( size );
        CppcStatement cppcStatement = new CppcStatement( statement );
        StatementAnalyzer.analyzeStatement( cppcStatement );
        Iterator<Identifier> iter = cppcStatement.getConsumed().iterator();
        while( iter.hasNext() ) {
          pragma.addRegister( new CppcRegister( iter.next(), null ) );
        }
      }
    }

    statementList.addStatementBefore( reference, pragma );
  }

  public static boolean isGlobal( Identifier id, SymbolTable table ) {

    Declaration vd = table.findSymbol( id );
    if( vd instanceof cppc.compiler.fortran.CommonBlock ) {
      return true;
    }

    SymbolTable parentTable = (SymbolTable)getParentOfClass( vd,
      SymbolTable.class );
    if( parentTable instanceof TranslationUnit ) {
      return true;
    }

    return false;
  }

  public static final Statement findFirstExecutable(
    CompoundStatement statementList ) {

    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Statement.class );

    while( iter.hasNext() ) {
      try {
        Statement stmt = (Statement)iter.next( Statement.class );
        if( !(stmt instanceof DeclarationStatement) ) {
          return stmt;
        }
      } catch( NoSuchElementException e ) {}
    }

    return null;
  }

  public static final Statement findFirstExecutable( Procedure procedure ) {
    return ObjectAnalizer.findFirstExecutable( procedure.getBody() );
  }

  public static final Statement findLastDeclaration( Procedure procedure ) {

    CompoundStatement statementList = procedure.getBody();

    int i = 0;
    List children = statementList.getChildren();

    // There are more children and (a CppcStatement contains a declaration) OR
    // (the child is a declaration)
    while( ( i < children.size() ) &&
      ( (children.get(i) instanceof CppcStatement) &&
        (((CppcStatement)children.get(i)).getStatement() instanceof
        DeclarationStatement)
      ||
        (children.get(i) instanceof DeclarationStatement) ) ) {

      DeclarationStatement decl;
      if( children.get(i) instanceof CppcStatement ) {
        decl = (DeclarationStatement)
          ((CppcStatement)children.get(i)).getStatement();
      } else {
        decl = (DeclarationStatement)children.get( i );
      }

      i++;
    }

    if( i == 0 ) {
      // If there are no declarations (maybe aside CPPC directives); return null
      return null;
    } else {
      return (Statement)children.get(--i);
    }
  }

  public static final Statement findLastStatement( Procedure procedure ) {
    int size = procedure.getBody().getChildren().size();
    return (Statement)procedure.getBody().getChildren().get(size-1);
  }

  public static final void encloseWithExecutes( Statement s ) {

    // Find the CompoundStatement
    CompoundStatement statementList = (CompoundStatement)getParentOfClass( s,
      CompoundStatement.class );

    CppcExecutePragma pragma = new CppcExecutePragma( s, s );
    statementList.addStatementBefore( s, pragma );
  }

  public static Declarator getDeclarator( VariableDeclaration vd,
    Identifier id ) {

    for( int i  = 0; i < vd.getNumDeclarators(); i++ ) {
      if( vd.getDeclarator( i ).getSymbol().equals( id ) ) {
        return vd.getDeclarator( i );
      }
    }

    return null;
  }

  public static Identifier getBaseIdentifier( Expression expr ) {

    if( expr instanceof Identifier ) {
      return (Identifier)expr;
    }

    if( expr instanceof ArrayAccess ) {
      return getBaseIdentifier( ((ArrayAccess)expr).getArrayName() );
    }

    if( expr instanceof UnaryExpression ) {
      return getBaseIdentifier( ((UnaryExpression)expr).getExpression() );
    }

    return null;
  }
}
