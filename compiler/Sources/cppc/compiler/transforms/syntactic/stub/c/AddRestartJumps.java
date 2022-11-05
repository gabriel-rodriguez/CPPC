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




package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.ArrayAccess;
import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.PointerSpecifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AddRestartJumps extends
  cppc.compiler.transforms.syntactic.skel.AddRestartJumps {

  private static final String CPPC_JUMP_POINTS_NAME = "cppc_jump_points";
  private static final String CPPC_JUMP_POINTS_SIZE_NAME =
    "cppc_jump_points_size";
  private static final String CPPC_JUMP_POINTS_CURRENT_NAME =
    "cppc_next_jump_point";

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

    // We add a variable 'void * cppc_jump_points[ jumpPoints ]'
    ArrayList cppcJumpPointsDeclarationSpecs = new ArrayList();
    cppcJumpPointsDeclarationSpecs.add( Specifier.VOID );
    ArrayList cppcJumpPointsDeclaratorLeadingSpecs = new ArrayList();
    cppcJumpPointsDeclaratorLeadingSpecs.add( PointerSpecifier.UNQUALIFIED );
    ArrayList cppcJumpPointsDeclaratorTrailingSpecs = new ArrayList();
    cppcJumpPointsDeclaratorTrailingSpecs.add( new ArraySpecifier(
      new IntegerLiteral( jumpPoints ) ) );
    Declarator cppcJumpPointsDeclarator = new VariableDeclarator(
      cppcJumpPointsDeclaratorLeadingSpecs,
      new Identifier( CPPC_JUMP_POINTS_NAME ),
      cppcJumpPointsDeclaratorTrailingSpecs );
    VariableDeclaration cppcJumpPointsDeclaration = new VariableDeclaration(
      cppcJumpPointsDeclarationSpecs, cppcJumpPointsDeclarator );
    statementList.addDeclaration( cppcJumpPointsDeclaration );

    // We add a variable 'const int cppc_jump_points_size'
    ArrayList cppcJumpPointsSizeDeclarationSpecs = new ArrayList();
    cppcJumpPointsSizeDeclarationSpecs.add( Specifier.CONST );
    cppcJumpPointsSizeDeclarationSpecs.add( Specifier.INT );

    Declarator cppcJumpPointsSizeDeclarator = new VariableDeclarator( new
      Identifier( CPPC_JUMP_POINTS_SIZE_NAME ) );

    cppcJumpPointsSizeDeclarator.setInitializer( new Initializer( new
      IntegerLiteral( jumpPoints ) ) );
    VariableDeclaration cppcJumpPointsSizeDeclaration =
      new VariableDeclaration( cppcJumpPointsSizeDeclarationSpecs,
        cppcJumpPointsSizeDeclarator );
    statementList.addDeclaration( cppcJumpPointsSizeDeclaration );

    // We add a variable 'int cppc_jump_points_current'
    Declarator cppcJumpPointsCurrentDeclarator = new VariableDeclarator(
      new Identifier( CPPC_JUMP_POINTS_CURRENT_NAME ) );
    cppcJumpPointsCurrentDeclarator.setInitializer( new Initializer(
      new IntegerLiteral( 0 ) ) );
    VariableDeclaration cppcJumpPointsCurrentDeclaration =
      new VariableDeclaration( Specifier.INT,
        cppcJumpPointsCurrentDeclarator );
    statementList.addDeclaration( cppcJumpPointsCurrentDeclaration );

    // Create a list of Expressions that are the directions of the CppcLabels
    ArrayList<UnaryExpression> orderedDirections =
      new ArrayList<UnaryExpression>( jumpPoints );
    Iterator<CppcLabel> labelsIter = orderedLabels.iterator();
    while( labelsIter.hasNext() ) {
      CppcLabel thisLabel = labelsIter.next();
      UnaryExpression labelDir = new UnaryExpression(
        UnaryOperator.LABEL_ADDRESS,
        (Identifier)thisLabel.getName().clone() );
      orderedDirections.add( labelDir );
    }

    // Put this list of directions as an Initializer for the
    // cppc_jump_points array
    Initializer initializer = new Initializer( orderedDirections );
    cppcJumpPointsDeclarator.setInitializer( initializer );
  }

  protected void addConditionalJump( CppcConditionalJump jump,
    List<CppcLabel> orderedLabels ) {

    final String CPPC_JUMP_INDEX_NAME = "jump_index";

    // Create the function call that is the precondition of the jump
    FunctionCall cppcJumpNextCall = new FunctionCall(
      new Identifier(
        GlobalNamesFactory.getGlobalNames().JUMP_NEXT_FUNCTION() ) );

    // Create the 'THEN' part of the IfStatement: the jump,
    // a CompoundStatement
    CompoundStatement jumpBody = new CompoundStatement();

    // Create the variable that indexes the jump array and initialize it,
    // as a VariableDeclaration
    ArrayList<Specifier> jumpIndexSpecs = new ArrayList<Specifier>( 2 );
    jumpIndexSpecs.add( Specifier.CONST );
    jumpIndexSpecs.add( Specifier.INT );
    Declarator jumpIndexDeclarator = new VariableDeclarator( new Identifier(
      CPPC_JUMP_INDEX_NAME ) );

    Expression initExpression = new BinaryExpression(
      new Identifier( CPPC_JUMP_POINTS_CURRENT_NAME ), BinaryOperator.ADD,
      new IntegerLiteral( jump.getLeap() - 1 ) );
    jumpIndexDeclarator.setInitializer( new Initializer( initExpression ) );
    VariableDeclaration jumpIndexDeclaration = new VariableDeclaration(
      jumpIndexSpecs, jumpIndexDeclarator );
    jumpBody.addDeclaration( jumpIndexDeclaration );

    // Increment the CPPC_JUMP_INDEX in k modulus the number of jump points

    // // Expression is: cppc_jump_next = ( cppc_jump_next + k ) %
    // // cppc_jump_points_size

    // // // First part: cppc_jump_next + l
    BinaryExpression increment = new BinaryExpression(
      new Identifier( CPPC_JUMP_INDEX_NAME ), BinaryOperator.ADD,
      new IntegerLiteral( 1 ) );

    // // // Second part: increment % cppc_jump_points_size
    BinaryExpression modulus = new BinaryExpression( increment,
      BinaryOperator.MODULUS, new Identifier( CPPC_JUMP_POINTS_SIZE_NAME ) );

    // // // Third part cppc_jump_next = modulus
    AssignmentExpression assignment = new AssignmentExpression(
      new Identifier( CPPC_JUMP_POINTS_CURRENT_NAME ),
      AssignmentOperator.NORMAL, modulus	);

    // // // Last thing: create an ExpressionStatement and add it to the
    // // // jump body
    ExpressionStatement assignmentStatement = new ExpressionStatement(
      assignment );
    jumpBody.addStatement( assignmentStatement );

    // Make the jump itself: the goto statement

    // // That is: goto * cppc_jump_points[jump_index];

    // // // First part: Build the array access: cppc_jump_points[jump_index]
    ArrayAccess arrayAccess = new ArrayAccess( new Identifier(
      CPPC_JUMP_POINTS_NAME ),
    new Identifier( CPPC_JUMP_INDEX_NAME ) );

    // // // Second part: dereference the void * contained in the array
    UnaryExpression dereference = new UnaryExpression(
      UnaryOperator.DEREFERENCE, arrayAccess );

    // // // Second and a half part: if this dereference is printed into
    // // // parens, then GCC will complain, as there cannot be a left paren
    // // // after a 'goto': do not print it between parens
    dereference.setParens( false );

    // // // Third part: insert the dereference as the 'goto' destination
    GotoStatement gotoStatement = new GotoStatement( dereference );

    // // // Last thing: add the gotoStatement to the jump body
    jumpBody.addStatement( gotoStatement );

    // Create the IfStatement (the conditional jump) using the condition and
    // the jump body
    IfStatement conditionalJump = new IfStatement( cppcJumpNextCall,
      jumpBody );

    // Swap the conditional jump mark with the conditional jump
    jump.swapWith( conditionalJump );
  }
}
