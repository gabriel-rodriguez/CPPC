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
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.fortran.ComputedGotoStatement;
import cppc.compiler.fortran.ParameterDeclaration;
import cppc.compiler.fortran.TypeDeclaration;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.ObjectAnalizer;

import java.util.List;

public class AddRestartJumps extends
  cppc.compiler.transforms.syntactic.skel.AddRestartJumps {

  private static final String CPPC_JUMP_TEST_NAME = "CPPC_JUMP_TEST";
  private static final String CPPC_JUMP_INDEX_NAME = "CPPC_JUMP_INDEX";
  private static final String CPPC_JUMP_LABELS_COUNT_NAME =
    "CPPC_JUMP_LABELS_COUNT";
  private static final String FORTRAN_MODULUS_FUNCTION = "MOD";

  private AddRestartJumps( Program program ) {
    super( program );
  }

  public static final AddRestartJumps getTransformInstance( Program program ) {
    return new AddRestartJumps( program );
  }

  protected void addCppcVariables( Procedure procedure,
    List<CppcLabel> orderedLabels ) {

    // Get the jump points count
    int jumpPoints = orderedLabels.size();

    // Get the procedure body (for adding all the flow-control stuff)
    CompoundStatement statementList = procedure.getBody();
    Statement ref = ObjectAnalizer.findLastDeclaration( procedure );

    // Add a variable INTEGER CPPC_JUMP_TEST
    VariableDeclaration jumpTestDeclaration = new VariableDeclaration(
      Specifier.INT,
      new VariableDeclarator( new Identifier( CPPC_JUMP_TEST_NAME ) ) );
    DeclarationStatement jumpTestStatement = new DeclarationStatement(
      jumpTestDeclaration );
    if( ref == null ) {
      statementList.getChildren().add( 0, jumpTestStatement );
    } else {
      statementList.addStatementAfter( ref, jumpTestStatement );
    }
    ref = jumpTestStatement;

    // Add a variable INTEGER CPPC_JUMP_INDEX
    Declarator jumpIndexDeclarator = new VariableDeclarator( new Identifier(
      CPPC_JUMP_INDEX_NAME ) );
    VariableDeclaration jumpIndexDeclaration = new VariableDeclaration(
      Specifier.INT, jumpIndexDeclarator );
    DeclarationStatement jumpIndexStatement = new DeclarationStatement(
        jumpIndexDeclaration );
    statementList.addStatementAfter( ref, jumpIndexStatement );
    ref = jumpIndexStatement;

    // Add a variable INTEGER CPPC_JUMP_LABELS_COUNT, PARAMETER = jumpPoints
    Declarator jumpLabelsCountDeclarator = new VariableDeclarator(
      new Identifier( CPPC_JUMP_LABELS_COUNT_NAME ) );
    jumpLabelsCountDeclarator.setInitializer( new Initializer(
      new IntegerLiteral( jumpPoints ) ) );
    VariableDeclaration jumpLabelsCountDeclaration = new VariableDeclaration(
      Specifier.INT, jumpLabelsCountDeclarator );
    DeclarationStatement jumpLabelsCountStatement = new DeclarationStatement(
      jumpLabelsCountDeclaration );
    statementList.addStatementAfter( ref, jumpLabelsCountStatement );
    ref = jumpLabelsCountStatement;

    // Add Cosmetic Declarations
    TypeDeclaration integersCosmetic = new TypeDeclaration( Specifier.INT );
    integersCosmetic.addDeclarator( new VariableDeclarator( new Identifier(
      CPPC_JUMP_TEST_NAME ) ) );
    integersCosmetic.addDeclarator( new VariableDeclarator( new Identifier(
      CPPC_JUMP_INDEX_NAME ) ) );
    integersCosmetic.addDeclarator( new VariableDeclarator( new Identifier(
      CPPC_JUMP_LABELS_COUNT_NAME ) ) );
    ParameterDeclaration parameterCosmetic = new ParameterDeclaration();
    parameterCosmetic.addDeclarator(
      (Declarator)jumpLabelsCountDeclarator.clone() );
    DeclarationStatement integersCosmeticStatement = new DeclarationStatement(
      integersCosmetic );
    DeclarationStatement parameterCosmeticStatement = new DeclarationStatement(
      parameterCosmetic );
    statementList.addStatementAfter( ref, integersCosmeticStatement );
    ref = integersCosmeticStatement;
    statementList.addStatementAfter( ref, parameterCosmeticStatement );
    ref = parameterCosmeticStatement;

    // Add CPPC_JUMP_INDEX initialization (=0)
    AssignmentExpression assignmentExpression = new AssignmentExpression(
      new Identifier( CPPC_JUMP_INDEX_NAME ), AssignmentOperator.NORMAL,
      new IntegerLiteral( 0 ) );
    ExpressionStatement assignmentStatement = new ExpressionStatement(
      assignmentExpression );
    ref = ObjectAnalizer.findFirstExecutable( procedure );

    // If no declaration is found: add statement at the beginning of the block
    if( ref == null ) {
      statementList.getChildren().add( 0, assignmentStatement );
      assignmentStatement.setParent( statementList );
    } else {
      statementList.addStatementAfter( ref, assignmentStatement );
    }
  }

  protected void addConditionalJump( CppcConditionalJump jump,
    List<CppcLabel> orderedLabels ) {

    // Create the function call that is the precondition of the jump, and insert
    // it instead of the conditional jump mark
    FunctionCall cppcJumpNextCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().JUMP_NEXT_FUNCTION() ) );
    cppcJumpNextCall.addArgument( new Identifier( CPPC_JUMP_TEST_NAME ) );
    ExpressionStatement callStatement = new ExpressionStatement(
      cppcJumpNextCall );
    jump.swapWith( callStatement );

    // Create the 'THEN' part of the IfStatement: the jump, a CompoundStatement
    CompoundStatement jumpBody = new CompoundStatement();

    // Increment the CPPC_JUMP_INDEX variable accordingly
    FunctionCall modulusCall = new FunctionCall( new Identifier(
      FORTRAN_MODULUS_FUNCTION ) );
    BinaryExpression indexPlusOne = new BinaryExpression( new Identifier(
      CPPC_JUMP_INDEX_NAME ), BinaryOperator.ADD,
      new IntegerLiteral( jump.getLeap() ) );
    modulusCall.addArgument( indexPlusOne );
    BinaryExpression jumpLabelsCountPlusOne = new BinaryExpression(
      new Identifier( CPPC_JUMP_LABELS_COUNT_NAME ), BinaryOperator.ADD,
      new IntegerLiteral( 1 ) );
    modulusCall.addArgument( jumpLabelsCountPlusOne );
    AssignmentExpression jumpIndexIncrement = new AssignmentExpression(
      new Identifier( CPPC_JUMP_INDEX_NAME ), AssignmentOperator.NORMAL,
      modulusCall );
    jumpBody.addStatement( new ExpressionStatement( jumpIndexIncrement ) );

    // If jump index is 0 => jump_index = 1
    AssignmentExpression jumpIndexReset = new AssignmentExpression( new
      Identifier( CPPC_JUMP_INDEX_NAME ), AssignmentOperator.NORMAL,
      new IntegerLiteral( 1 ) );
    BinaryExpression jumpIndexIsZero = new BinaryExpression( new Identifier(
      CPPC_JUMP_INDEX_NAME ), BinaryOperator.COMPARE_EQ,
      new IntegerLiteral( 0 ) );
    IfStatement resetIndexIfStmt = new IfStatement( jumpIndexIsZero,
      new ExpressionStatement( jumpIndexReset ) );
    jumpBody.addStatement( resetIndexIfStmt );

    // Add the computed goto statement
    ComputedGotoStatement computedGoto = new ComputedGotoStatement(
      new Identifier( CPPC_JUMP_INDEX_NAME ) );
    for( CppcLabel label: orderedLabels ) {
      computedGoto.addLabel( (Identifier)label.getName().clone() );
    }
    jumpBody.addStatement( computedGoto );

    // Create the conditional jump
    BinaryExpression conditionalJumpsActive = new BinaryExpression(
      new Identifier( CPPC_JUMP_TEST_NAME ), BinaryOperator.COMPARE_EQ,
      new IntegerLiteral( 1 ) );
    IfStatement conditionalJump = new IfStatement( conditionalJumpsActive,
      jumpBody );
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( callStatement,
        CompoundStatement.class );
    statementList.addStatementAfter( callStatement, conditionalJump );
  }
}
