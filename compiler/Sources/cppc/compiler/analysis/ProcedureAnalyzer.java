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
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.NullStatement;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public final class ProcedureAnalyzer {

  private ProcedureAnalyzer() {}

  public static final void analyzeProcedure( Procedure procedure ) {
    // If the procedure has already been registered, just skip it
    if( !CppcRegisterManager.isRegistered( (Identifier)procedure.getName() ) ) {
      CompoundStatement statementList = procedure.getBody();
      DepthFirstIterator iter = new DepthFirstIterator( statementList );
      iter.next(); // Discharge the CompoundStatement
      iter.pruneOn( Statement.class );

      // Add empty register to allow for the lower level analyzers to insert
      // data beforehand
      ProcedureCharacterization c = new ProcedureCharacterization( (Identifier)
        procedure.getName() );
      CppcRegisterManager.addProcedure( (Identifier)procedure.getName(), c );

      long totalWeight = 0;
      int statementCount = 0;
      while( iter.hasNext() ) {
        try {
          CppcStatement cppcStatement = (CppcStatement)
            iter.next( CppcStatement.class );
          StatementAnalyzer.analyzeStatement( cppcStatement );
          totalWeight += cppcStatement.getWeight();
          statementCount += cppcStatement.statementCount;
        } catch( NoSuchElementException e ) {}
      }

      // If the last statement is a CppcStatement, the 'catch' won't work
      c.statementCount = statementCount;
      c.setWeight( totalWeight );
      addProcedureRegister( procedure );
    }
  }

  private static void addProcedureRegister( Procedure procedure ) {

    ProcedureCharacterization characterization = null;
    if( CppcRegisterManager.isRegistered( (Identifier)procedure.getName() ) ) {
      // If this procedure is already registered, then it was done for
      // array/function discrimination purposes. Therefore, it must be updated
      // with the consumed/generated info now available.
      characterization = CppcRegisterManager.getCharacterization(
        (Identifier)procedure.getName() );
    } else {
      characterization = new ProcedureCharacterization(
        (Identifier)procedure.getName() );
    }

    characterization.setProcedure( procedure );

    // Process the CPPC statements contained in this procedure
    HashSet<Identifier> consumed = new HashSet<Identifier>();
    HashSet<Identifier> generated = new HashSet<Identifier>();
    HashSet<VariableDeclaration> globalConsumed =
      new HashSet<VariableDeclaration>();
    HashSet<VariableDeclaration> globalGenerated =
      new HashSet<VariableDeclaration>();
    Map<Identifier,Set<Identifier>> variableDependencies =
      new HashMap<Identifier,Set<Identifier>>();
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    SetOperations<VariableDeclaration> vdSetOps =
      new SetOperations<VariableDeclaration>();
    Stack<IfStatement> conditionals = new Stack<IfStatement>();
    Set<Identifier> conditionalDependencies = new HashSet<Identifier>();
    Set<Identifier> calls = new HashSet<Identifier>();
    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    while( iter.hasNext() ) {
      CppcStatement cppcStatement = null;
      try {
        cppcStatement = (CppcStatement)iter.next( CppcStatement.class );
      } catch( NoSuchElementException e ) {
        Set<Identifier> generatedParameters = filterProcedureParameters(
          procedure, generated );
        Set<Identifier> consumedParameters = filterProcedureParameters(
          procedure, consumed );

        Set<ProcedureParameter> generatedPositions =
          ObjectAnalizer.setIdToSetProcParam( procedure, generatedParameters );
        Set<ProcedureParameter> consumedPositions =
          ObjectAnalizer.setIdToSetProcParam( procedure, consumedParameters );

        globalConsumed.addAll( vdSetOps.setMinus( filterGlobalVariables(
          procedure, consumed ), globalGenerated ) );
        globalGenerated.addAll( filterGlobalVariables( procedure, generated ) );

        characterization.setGenerated( generatedPositions );
        characterization.setConsumed( consumedPositions );
        characterization.setGlobalGenerated( globalGenerated );
        characterization.setGlobalConsumed( globalConsumed );
        characterization.setVariableDependencies( variableDependencies );

        if( !CppcRegisterManager.isRegistered(
          (Identifier)procedure.getName() ) ) {

            CppcRegisterManager.addProcedure( (Identifier)procedure.getName(),
              characterization );
        }

        return;
      }

      // Check if this statement is still inside the latest found conditional
      boolean peekParent = false;
      while( !conditionals.isEmpty() && !peekParent ) {
        IfStatement ifStmt = (IfStatement)ObjectAnalizer.getParentOfClass(
          cppcStatement, IfStatement.class );
        if( ifStmt != conditionals.peek() ) {
          conditionals.pop();
          // Update conditional dependencies
          conditionalDependencies =
            ProcedureAnalyzer.calculateConditionalDependencies(
            conditionals );
        } else {
          peekParent = true;
        }
      }

      // If this statement is a conditional, update dependencies
      if( cppcStatement.getStatement() instanceof IfStatement ) {
        conditionals.push( (IfStatement)cppcStatement.getStatement() );
        conditionalDependencies =
          ProcedureAnalyzer.calculateConditionalDependencies(
          conditionals );
      }

      // consumed = consumed + { cppcStatement.consumed - generated }
      consumed.addAll( setOps.setMinus( cppcStatement.getConsumed(),
        generated ) );
      globalConsumed.addAll( vdSetOps.setMinus(
        cppcStatement.getGlobalConsumed(), globalGenerated ) );

      // generated = generated + cppcStatement.generated
      generated.addAll( cppcStatement.getGenerated() );
      globalGenerated.addAll( cppcStatement.getGlobalGenerated() );

      // Variable dependencies
      if( !cppcStatement.getConsumed().isEmpty() ) {
        for( Identifier genId: cppcStatement.getPartialGenerated() ) {
          Set<Identifier> dependencies = variableDependencies.get( genId );
          if( dependencies == null ) {
            dependencies = new HashSet<Identifier>();
            variableDependencies.put( genId, dependencies );
          }

          dependencies.addAll( cppcStatement.getConsumed() );

          // Also, add conditional dependencies
          dependencies.addAll( conditionalDependencies );
        }
      }
    }

    // If the last statement is a CppcStatement, the 'catch' won't work
    Set<Identifier> generatedParameters = filterProcedureParameters( procedure,
      generated );
    Set<Identifier> consumedParameters = filterProcedureParameters( procedure,
      consumed );

    Set<ProcedureParameter> generatedPositions =
      ObjectAnalizer.setIdToSetProcParam( procedure, generatedParameters );
    Set<ProcedureParameter> consumedPositions =
      ObjectAnalizer.setIdToSetProcParam( procedure, consumedParameters );

    globalConsumed.addAll( vdSetOps.setMinus( filterGlobalVariables(
      procedure, consumed ), globalGenerated ) );
    globalGenerated.addAll( filterGlobalVariables( procedure, generated ) );

    characterization.setGenerated( generatedPositions );
    characterization.setConsumed( consumedPositions );
    characterization.setGlobalGenerated( globalGenerated );
    characterization.setGlobalConsumed( globalConsumed );
    characterization.setVariableDependencies( variableDependencies );

    if( !CppcRegisterManager.isRegistered( (Identifier)procedure.getName() ) ) {
      CppcRegisterManager.addProcedure( (Identifier)procedure.getName(),
        characterization );
    }
  }

  private static Set<Identifier> calculateConditionalDependencies(
    Stack<IfStatement> conditionals ) {

    Set<Identifier> conditionalDependencies = new HashSet<Identifier>();

    for( IfStatement ifStmt: conditionals ) {
      conditionalDependencies.addAll( ExpressionAnalyzer.analyzeExpression(
        (CppcStatement)ifStmt.getParent(), ifStmt.getControlExpression() ) );
    }

    return conditionalDependencies;
  }

  private static Set<Identifier> filterProcedureParameters( Procedure procedure,
    Set<Identifier> set ) {

    Iterator iter = procedure.getParameters().iterator();
    HashSet<Identifier> parameters = new HashSet<Identifier>();

    while( iter.hasNext() ) {

      Object obj = iter.next();

      if( !(obj instanceof VariableDeclaration) ) {
        System.err.println( "WARNING: "+
          "cppc.compiler.analysis.ProcedureAnalyzer.filterProcedureParameters()"
          +" not implemented for " + obj.getClass() );
        System.exit( 0 );
      }

      VariableDeclaration declaration = (VariableDeclaration)obj;

      // Iterator over the declarators of this declaration
      for( int i = 0; i < declaration.getNumDeclarators(); i++ ) {
        Declarator declarator = declaration.getDeclarator( i );
        parameters.add( (Identifier)declarator.getSymbol().clone() );
      }
    }

    // Build a suitable SetOperations and return the intersection
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    return setOps.setIntersection( parameters, set );
  }

  // First : see note inside code
  // Second: A less conservative approach would be to consider CommonBlocks
  // generated if ALL their contained variables are generated. This is not
  // completely true, as a subroutine can declare ONLY a subset of the same
  // CommonBlock, thus tricking the compiler and generating only that declared
  // subset. However, it could be used as an option, perhaps to inspire some
  // discipline into the hearts of Fortran programmers :)
  private static Set<VariableDeclaration> filterGlobalVariables(
    Procedure procedure, Set<Identifier> set ) {

    HashSet<VariableDeclaration> globals = new HashSet<VariableDeclaration>(
      set.size() );
    SymbolTable table = procedure.getBody();

    for( Identifier id: set ) {
      if( ObjectAnalizer.isGlobal( id, table ) ) {
        try {
          globals.add(
           LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
            (Statement)procedure.getBody().getChildren().get(0), id ));
        } catch( Exception e ) {}
      }
    }

    return globals;
  }
}
