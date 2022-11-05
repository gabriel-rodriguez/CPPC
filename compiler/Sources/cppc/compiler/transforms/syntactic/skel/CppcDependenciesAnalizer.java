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




package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.Tools;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.analysis.StatementAnalyzer;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcNonportableMark;
import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.CppcUnregisterPragma;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

// New version: the dependencies analyzer follows the execution flow
public class CppcDependenciesAnalizer {

  // Static
  private static final String passName = "[CppcDependenciesAnalizer]";
  private static final Set<Identifier> globalRegisters = new HashSet<Identifier>();
  private static final Set<Procedure> analyzed = new HashSet<Procedure>();

  // Private members
  private Set<Identifier> generated;
  private Set<Identifier> registered;
  private Set<Identifier> commitedRegisters;

  protected CppcDependenciesAnalizer() {
    generated = new HashSet<Identifier>();
    registered = new HashSet<Identifier>();
    commitedRegisters = new HashSet<Identifier>();
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    CppcDependenciesAnalizer transform = new CppcDependenciesAnalizer();
    transform.start( program );

    Tools.printlnStatus( passName + " end", 1 );
  }
  
  protected void start( Program program ) {
    Procedure main = ObjectAnalizer.findMainProcedure( program );
    this.walkOverProcedure( main );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    if( analyzed.contains( procedure ) ) {
      return;
    }
    
    // Upon entering a new procedure: add variables generated in an initializer
    generated.clear();
    registered.clear();
    commitedRegisters.clear();
    analyzed.add( procedure );

    Set<Identifier> generatedInInitializers = this.generatedInInitializers( procedure );
    generated.addAll( generatedInInitializers );

    // Instrument if statements so that appropriate registers are inserted to
    // correctly re-evaluate the condition
    DepthFirstIterator procIter = new DepthFirstIterator( procedure );
    procIter.pruneOn( Expression.class );
    while( procIter.hasNext() ) {
      IfStatement ifStatement = null;
      Statement thenStatement = null;
      Statement elseStatement = null;
      try {
        ifStatement = (IfStatement)procIter.next(IfStatement.class);
        thenStatement = ifStatement.getThenStatement();
        elseStatement = ifStatement.getElseStatement();
      } catch( NoSuchElementException e ) {
        continue;
      }

      try {
        DepthFirstIterator thenIter = new DepthFirstIterator( thenStatement );
        thenIter.pruneOn( Expression.class );

        CppcPragmaStatement pragma = (CppcPragmaStatement)thenIter.next( CppcPragmaStatement.class );
        CppcNonportableMark mark = new CppcNonportableMark( ifStatement.getControlExpression() );
        Statement ref = ifStatement;
        while( !(ref.getParent() instanceof CompoundStatement) ) {
          ref = (Statement)ref.getParent();
        }

        CompoundStatement list = (CompoundStatement)ref.getParent();
        list.addStatementBefore( ref, mark );
      } catch( NoSuchElementException e ) {
        try {
          if( elseStatement != null ) {
            DepthFirstIterator elseIter = new DepthFirstIterator( elseStatement );
            elseIter.pruneOn( Expression.class );

            CppcPragmaStatement pragma = (CppcPragmaStatement)elseIter.next( CppcPragmaStatement.class );
            CppcNonportableMark mark = new CppcNonportableMark( ifStatement.getControlExpression() );

            Statement ref = ifStatement;
            while( !(ref.getParent() instanceof CompoundStatement) ) {
              ref = (Statement)ref.getParent();
            }

            CompoundStatement list = (CompoundStatement)ref.getParent();
            list.addStatementBefore( ref, mark );
          }
        } catch( NoSuchElementException ex ) {}
      }
    }

    // Search for #pragma cppc execute on restart, #pragma cppc checkpoint and checkpointed procedure calls.
    // It is important to do this at the same time, to ensure that executed-on-restart elements are processed
    // in execution order. In this way, when processing e.g. a checkpointed function call we will have accurate
    // information about what has been generated prior to the call.
    procIter.reset();
    procIter.pruneOn( Expression.class );
    while( procIter.hasNext() ) {
      try {
        Statement s = (Statement)procIter.next( Statement.class );
        if( s instanceof CppcPragmaStatement ) {
          if( s instanceof CppcCheckpointPragma ) {
            generated.clear();
            generated.addAll( registered );
            generated.addAll( generatedInInitializers );
            this.processCheckpointBlock( (CppcCheckpointPragma)s );
            continue;
          }

          if( s instanceof CppcExecutePragma ) {
            this.processExecuteBlock( (CppcExecutePragma)s );
            continue;
          }

          continue;
        }

        if( s instanceof ExpressionStatement && ((ExpressionStatement)s).getExpression() instanceof FunctionCall ) {
          FunctionCall call = (FunctionCall)((ExpressionStatement)s).getExpression();
          if( CppcRegisterManager.isRegistered( (Identifier)call.getName() ) ) {
            ProcedureCharacterization c = CppcRegisterManager.getCharacterization( (Identifier)call.getName() );
            if( c.getCheckpointed() ) {
              this.processCheckpointBlock( call );
            }
            // In any case, analyze procedure (if not done yet)
            if( call.getProcedure() != null ) {
              Set<Identifier> generatedCopy = new HashSet<Identifier>(generated );
              Set<Identifier> registeredCopy = new HashSet<Identifier>( registered );
              Set<Identifier> commitedRegistersCopy = new HashSet<Identifier>( commitedRegisters );
              this.walkOverProcedure( call.getProcedure() );
              generated = generatedCopy;
              registered = registeredCopy;
              commitedRegisters = commitedRegistersCopy;
            }
          }
        }
      } catch( NoSuchElementException e ) {; }
    }
    
    // Before returning, unregister all local variables. In previous versions (up to 0.8.0) this was a responsibility
    // of the runtime library: upon popping a context all internal registers would be removed. However, sometimes the
    // internal registers are of global variables, and we cannot have that. Note that this can only happen when the
    // function containing the call to the one doing the registration does not have the global variable in scope (which
    // is not a very common issue).
    //FIXME: not implemented yet
  }

  private Set<Identifier> generatedInInitializers( Procedure procedure ) {
    Set<Identifier> declarationGenerated = new HashSet<Identifier>();

    DepthFirstIterator declIter = new DepthFirstIterator( procedure.getBody() );
    CppcStatement cppcStatement = null;
    while( declIter.hasNext() ) {

      try {
        cppcStatement = (CppcStatement)declIter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {
        generated.addAll( declarationGenerated );
      }

      if( cppcStatement.getStatement() instanceof DeclarationStatement ) {
        declarationGenerated.addAll( cppcStatement.getGenerated() );
      }
    }

    return declarationGenerated;
  }

  private void processExecuteBlock( CppcExecutePragma pragma ) {

    // Find beginning and ending cppc statement
    CppcStatement begin = (CppcStatement)ObjectAnalizer.getParentOfClass( pragma.getBegin(), CppcStatement.class );
    CppcStatement end = (CppcStatement)ObjectAnalizer.getParentOfClass( pragma.getEnd(), CppcStatement.class );

    // Calculation of what is generated/consumed within this execute block
    Set<Identifier> blockGenerated = new HashSet<Identifier>();
    Set<Identifier> blockConsumed = new HashSet<Identifier>();
    Set<Identifier> blockInitialized = new HashSet<Identifier>();

    blockGenerated.addAll( generated );
    ObjectAnalizer.characterizeBlock( begin, end, blockGenerated, blockConsumed, blockInitialized );
    generated.addAll( blockInitialized );

    // We build the "required" set as blockConsumed - generated: what we have
    // to register
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    Set<Identifier> required = setOps.setMinus( blockConsumed, generated );

    // Add a register pragma containing "required" before the block beginning
    if( !required.isEmpty() ) {
      List<CppcRegister> registerContent = this.buildRegisterList( begin, required );
      this.addRegisterPragmaBefore( begin, registerContent );
    }

    // Add blockGenerated and registeredVariables to current set of generated
    // data
    generated.addAll( blockGenerated );
    generated.addAll( required );
    registered.addAll( required );
  }

  private void processCheckpointBlock( Traversable t ) {
    // Find the last CppcStatement of this Procedure
    Procedure procedure = (Procedure)ObjectAnalizer.getParentOfClass( t.getParent(), Procedure.class );
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator procIter = new DepthFirstIterator( statementList );
    CppcStatement lastCppcStatement = null;

    while( procIter.hasNext() ) {
      try {
        lastCppcStatement = (CppcStatement)procIter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {}
    }

    Statement chkptStatement = (Statement)ObjectAnalizer.getParentOfClass(
      t, CppcStatement.class );

    // Calculation of what is generated/consumed from the checkpoint call to the
    // end of the procedure
    Set<Identifier> blockGenerated = new HashSet<Identifier>();
    Set<Identifier> blockConsumed = new HashSet<Identifier>();
    Set<Identifier> blockInitialized = new HashSet<Identifier>();

    // If the processed traversable is a function call (which means it is a
    // checkpointed function call), mark ALL its parameters as consumed.
    // Otherwise, we have problems with parameters which are generated
    // before the checkpoint but consumed AFTER the checkpoint.
    // FIXME: This can be improved by just analyzing the required code in an
    // interprocedural way (that is, from the call place when we find this).
    if( t instanceof FunctionCall ) {
      FunctionCall call = (FunctionCall)t;
      for( int i = 0; i < call.getNumArguments(); i++ ) {
        Expression expr = call.getArgument( i );
        if( expr instanceof Identifier ) {
          blockConsumed.add( (Identifier)expr );
        }
      }
    }

    // Characterize the checkpoint block
    ObjectAnalizer.characterizeBlock( chkptStatement, lastCppcStatement, blockGenerated, blockConsumed, blockInitialized );
    generated.addAll( blockInitialized );

    // We build the "required" set as blockConsumed - generated: what we have to
    // register
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    Set<Identifier> required = setOps.setMinus( blockConsumed, generated );

    // Add a register pragma containing "required" before the CHECKPOINT pragma
    Statement ref = chkptStatement; // Keeps track of where to unregister
    if( !required.isEmpty() ) {
      List<CppcRegister> registerContent = buildRegisterList( chkptStatement, required );
      ref = addRegisterPragmaBefore( chkptStatement, registerContent );
    }

    // Calculation of unregisters. It must be done previous to adding current
    // registers
    Set<Identifier> unrequired = setOps.setMinus( commitedRegisters,
      blockConsumed );
    registered.addAll( required );

    // We could unregister length info without unregistering dependent data!
    // Fill unrequired with required sizes
    if( !unrequired.isEmpty() ) {
      addUnregisterPragmaBefore( ref, unrequired );
      registered = setOps.setMinus( registered, unrequired );
      generated = setOps.setMinus( generated, unrequired );
    }

    // Add blockGenerated and registeredVariables to current set of generated
    // data
    generated.addAll( blockGenerated );
    generated.addAll( required );
    commitedRegisters.addAll( registered );
  }

  private List<CppcRegister> buildRegisterList( Statement reference, Set<Identifier> set ) {

    Iterator<Identifier> iter = set.iterator();
    Procedure procedure = reference.getProcedure();
    CompoundStatement statementList = procedure.getBody();
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    ArrayList<CppcRegister> returnList = new ArrayList<CppcRegister>();
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();

    while( iter.hasNext() ) {
      Identifier id = iter.next();

      // FIXME TEMPORARY: remove unscoped variables. This is needed while arrays
      // and pointers are not generated (as unscoped next-to-appear pointers
      // will appear as consumed, but not as generated)
      VariableDeclaration vd = null;
      try {
        vd = analyzer.getVariableDeclaration( procedure, id );
      } catch( Exception e ) {
        try {
          vd = analyzer.getVariableDeclaration( reference, id );
        } catch( Exception ex ) {}
      }

      if( vd == null ) {
        // Unscoped variable: remove
        iter.remove();
        continue;
      }

      // If this variable is a parameter, don't register it
      if( procedure.getParameters().contains( vd ) ) {
        continue;
      }

      // If this variable is global, register only if not already done
      SymbolTable table = (SymbolTable)ObjectAnalizer.getParentOfClass(
        reference, SymbolTable.class );
      if( ObjectAnalizer.isGlobal( id, table ) ) {
        if( globalRegisters.contains( id ) ) {
          continue;
        } else {
          globalRegisters.add( id );
        }
      }

      // Consistent part: build the register for this symbol
      Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize( id,
        reference );

      if( size != null ) {
        returnList.addAll( getSizeDependencies( size, reference, null ) );
      }

      returnList.add( new CppcRegister( id, size ) );
//       } catch( Exception e ) {
//         //Unscoped variable: remove
//         iter.remove();
//       }
    }

    return returnList;
  }

  private List<CppcRegister> getSizeDependencies( Expression size,
    Statement reference, List<CppcRegister> former ) {

    // This is a recursive function. Upon first call, former = null
    if( former == null ) {
      former = new ArrayList<CppcRegister>();
    }

    if( size instanceof FunctionCall ) {
      // If this happens, we are analyzing an strlen call and we do not want to
      // add anything
      return former;
    }

    // For any given identifier, we find its dependencies
    ExpressionStatement statement = new ExpressionStatement( size );
    CppcStatement cppcStatement = new CppcStatement( statement );
    StatementAnalyzer.analyzeStatement( cppcStatement );
    Set<Identifier> deps = cppcStatement.getConsumed();
    Iterator<Identifier> iter = deps.iterator();

    while( iter.hasNext() ) {

      Identifier id = iter.next();

      if( !generated.contains( id ) ) {
        Expression innerSize =
          VariableSizeAnalizerFactory.getAnalizer().getSize( id, reference );
        if( innerSize != null ) {
          getSizeDependencies( innerSize, reference, former );
        }
        former.add( new CppcRegister( id, innerSize ) );
        generated.add( id );
        // Size dependencies don't get added to the registered set, to avoid
        // further unregistration This could be OPTIMIZED though, managing some
        // kind of dependence-tree that unregisters this kind of symbols when
        // the master one gets unregistered, soooo FIXME FIXME FIXME FIXME
//              registered.add( id );
      }
    }

    return former;
  }

  protected CppcRegisterPragma addRegisterPragmaBefore( Statement reference,
    List<CppcRegister> registers ) {

    // Do not place registers inside a loop
    if( LanguageAnalyzerFactory.getLanguageAnalyzer().insideLoop(
      reference ) ) {

      Statement loop = (Statement)ObjectAnalizer.getParentOfClass( reference,
        Loop.class );
      Statement newRef = (Statement)loop.getParent();

      return this.addRegisterPragmaBefore( newRef, registers );
    }

    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( reference,
        CompoundStatement.class );

    // Do not place registers after a CPPC EXECUTE directive: it breaks the
    // label-conditionaljump scheme introduced by later passes, resulting in
    // two adjacent labels and incorrect conditional jump positioning.
    int index = statementList.getChildren().indexOf( reference );
    try {
      CppcStatement t = (CppcStatement)statementList.getChildren().get(
        index-1 );

      if( t.getStatement() instanceof CppcExecutePragma ) {
        return this.addRegisterPragmaBefore( t, registers );
      }
    } catch( ClassCastException e ) { } // No worries
      catch( ArrayIndexOutOfBoundsException e ) {} // No worries

    // Place the directive before the reference
    CppcRegisterPragma pragma = new CppcRegisterPragma( registers );
    statementList.addStatementBefore( reference, pragma );
    pragma.setLineNumber( reference.where() );

    return pragma;
  }

  private CppcUnregisterPragma addUnregisterPragmaBefore( Statement reference,
    Set<Identifier> unregisters ) {

    CppcUnregisterPragma pragma = new CppcUnregisterPragma();
    Iterator<Identifier> iter = unregisters.iterator();

    while( iter.hasNext() ) {
      pragma.addUnregister( iter.next() );
    }

    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( reference,
      CompoundStatement.class );
    statementList.addStatementBefore( reference, pragma );

    return pragma;
  }
}
