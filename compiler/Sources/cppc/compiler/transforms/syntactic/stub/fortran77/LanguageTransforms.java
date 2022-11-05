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




package cppc.compiler.transforms.syntactic.stub.fortran77;

import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.fortran.FortranDoLoop;
import cppc.compiler.fortran.TypeDeclaration;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.ObjectAnalizer;

import java.util.NoSuchElementException;

public class LanguageTransforms extends
  cppc.compiler.transforms.syntactic.skel.LanguageTransforms {

  private static int loopLevel = 0;
  private static final String ITERATION_INIT_VAR = "CPPC_INIT_IT";

  private static final String initializeString( String [] i ) {

    String s = i[0];

    for( int j = 1; j < i.length; j++ ) {
      s = s + " " + i[j];
    }

    return s;
  }

  private LanguageTransforms( Program program ) {
    super( program );
  }

  public static final LanguageTransforms getTransformInstance(
    Program program ) {

    return new LanguageTransforms( program );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    loopLevel = 0;
    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    while( iter.hasNext() ) {
      try {
        CppcCheckpointPragma ckpt = (CppcCheckpointPragma)
          iter.next( CppcCheckpointPragma.class );
        this.testExecuteLoopHeaders( ckpt );
      } catch( NoSuchElementException e ) {}
    }
  }

  private void testExecuteLoopHeaders( CppcCheckpointPragma ckpt ) {
    FortranDoLoop loop = (FortranDoLoop)ObjectAnalizer.getParentOfClass(
      ckpt.getParent(), FortranDoLoop.class );

    if( loop != null ) {
      addExecuteLoopHeaders( ckpt, loop );
    }
  }

  private void addExecuteLoopHeaders( CppcCheckpointPragma ckpt, FortranDoLoop loop ) {
    // Add the CPPC_INIT_IT variable for the loop to procedure declarations
    Procedure procedure = ckpt.getProcedure();
    CompoundStatement statementList = procedure.getBody();
    String cppcVarName = ITERATION_INIT_VAR + "_" + loopLevel++;

    Declarator declarator = new VariableDeclarator( new Identifier(
      cppcVarName ) );
    VariableDeclaration vd = new VariableDeclaration( Specifier.INT,
      declarator );
    Statement lastDeclaration = ObjectAnalizer.findLastDeclaration(
      procedure );

    TypeDeclaration cosmetic = new TypeDeclaration( Specifier.INT );
    cosmetic.addDeclarator( (Declarator)declarator.clone() );
    if( lastDeclaration == null ) {
      statementList.addDeclaration( vd );
      statementList.addDeclaration( cosmetic );
    } else {
      statementList.addStatementAfter( lastDeclaration,
        new DeclarationStatement( vd ) );
      statementList.addStatementAfter( lastDeclaration, new
        DeclarationStatement( cosmetic ) );
    }

    // Before the loop, make CPPC_INIT_IT = loop_init (this makes the loop
    // execute correctly on a normal run)
    AssignmentExpression assignExpression = new AssignmentExpression(
      new Identifier( cppcVarName ), AssignmentOperator.NORMAL,
      (Expression)loop.getStart().clone() );
    ExpressionStatement assignStatement = new ExpressionStatement(
      assignExpression );
    statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass( loop,
      CompoundStatement.class );
    CppcStatement cppcLoop = (CppcStatement)loop.getParent();
    CppcStatement cppcAssignment = new CppcStatement( assignStatement );
    cppcAssignment.getGenerated().add( new Identifier( cppcVarName ) );
    assignStatement.setParent( cppcAssignment );
    statementList.addStatementBefore( cppcLoop, cppcAssignment );

    // Change the loop start for the newly created variable. Add it to the
    // consumed set of the loop. 
    loop.setStart( new Identifier( cppcVarName ) );
    cppcLoop.getConsumed().add( new Identifier( cppcVarName ) );

    // Add the loop variable IT to the generated set of the checkpoint pragma.
    // While this is not strictly true, it will help even out the analysis,
    // avoiding register of the loop variable
    CppcStatement cppcCkpt = (CppcStatement)ckpt.getParent();
    cppcCkpt.getGenerated().add( (Identifier)loop.getLoopVar() );

    // Inside the loop, make CPPC_INIT_IT = IT, so that the correct restart
    // value gets saved when checkpointing
    assignExpression = new AssignmentExpression(
      new Identifier( cppcVarName ), AssignmentOperator.NORMAL,
      (Expression)loop.getLoopVar() );
    assignStatement = new ExpressionStatement( assignExpression );
    statementList = (CompoundStatement)
      ((CppcStatement)loop.getBody()).getStatement();
    statementList.addStatementBefore(
      (Statement)statementList.getChildren().get(0), assignStatement );

    // Add a cppc execute directive before the loop header, and comprising up
    // to the last assignment expression
    CppcExecutePragma pragma = new CppcExecutePragma( loop, assignStatement );
    CppcStatement cppcPragma = new CppcStatement( pragma );
    pragma.setParent( cppcPragma );
    statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass( loop,
      CompoundStatement.class );
    statementList.addStatementBefore( cppcLoop, pragma );
    
    // If this loop is a nested one, do the same backwards
    FortranDoLoop parentLoop =
      (FortranDoLoop)ObjectAnalizer.getParentOfClass( loop.getParent(),
        FortranDoLoop.class );
    if( parentLoop != null ) {
      addExecuteLoopHeaders( ckpt, parentLoop );
    }
  }
}
