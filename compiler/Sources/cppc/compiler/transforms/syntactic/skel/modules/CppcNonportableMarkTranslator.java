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




package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.CompoundStatement;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;

import cppc.compiler.analysis.ExpressionAnalyzer;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableMark;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

public abstract class CppcNonportableMarkTranslator
  extends TranslationModule<CppcNonportableMark> {

  private static String SUBSTITUTION_PREFIX = "CPPC_MARK_";
  private static int NONPORTABLE_INSTANCE = 0;

  public CppcNonportableMarkTranslator() {
    super();
  }

  public Class getTargetClass() {
    return CppcNonportableMark.class;
  }

  public void translate( CppcNonportableMark pragma ) {
    // Get mark target and detach pragma
    Expression target = pragma.getExpression();
    Statement stmt = target.getStatement();

    // Find the compound statement containing the target
    CompoundStatement statementList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass( stmt, CompoundStatement.class );

    // Calculation of what is consumed by the target. It does not matter what
    // has been recovered up to this point: the CallImages are treated as
    // independent data, so that it is enforced that each nonportable statement
    // is executed *exactly* with the same data as in the original run
    ExpressionStatement virtual = new ExpressionStatement(
      (Expression)target.clone() );
    CppcStatement cppcStatement = new CppcStatement( virtual );
    cppcStatement.setParent( stmt.getParent() );

    cppc.compiler.analysis.StatementAnalyzer.analyzeStatement( cppcStatement );
    cppcStatement.setParent( null );

    boolean registers = false;
    for( Identifier id: cppcStatement.getConsumed() ) {
      if( this.addCppcRegisterForCallImage( stmt, id, statementList ) ) {
        registers = true;
      }
    }

    // Add a jump label to the beginning of the block (switch with mark)
    CppcLabel jumpLabel = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().EXECUTE_LABEL() ) );
    pragma.swapWith( jumpLabel );

    // Put the conditional jump mark after the target
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter( stmt, jump );

    // If no registers could be added, just return
    if( !registers ) {
      return;
    }

    // Add a CPPC_Commit_call_image before the target
    FunctionCall commitCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().COMMIT_CALL_IMAGE_FUNCTION() ) );
    ExpressionStatement callStatement = new ExpressionStatement( commitCall );
    statementList.addStatementBefore( stmt, callStatement );

    // Add the CPPC_Create_call_image call after the jump label
    this.addCppcCreateCallImage( jumpLabel, stmt, statementList );
  }

  private void addCppcCreateCallImage( Statement ref, Statement stmt,
    CompoundStatement statementList ) {

    // Create the function call
    FunctionCall createCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().CREATE_CALL_IMAGE_FUNCTION() ) );

    // First parameter: tmp identifier
    createCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      SUBSTITUTION_PREFIX + NONPORTABLE_INSTANCE++ ) );

    // Second parameter: line where target is
    createCall.addArgument( new IntegerLiteral( stmt.where() ) );

    // Create the statement and place it after the reference
    ExpressionStatement createStatement = new ExpressionStatement( createCall );
    statementList.addStatementAfter( ref, createStatement );
  }

  private boolean addCppcRegisterForCallImage( Statement ref, Identifier id,
    CompoundStatement statementList ) {

    // Get the variable declaration for the register
    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        ref, id );

      // If the parameter is constant, return
      if( vd.getSpecifiers().contains( Specifier.CONST ) ) {
        return false;
      }
    } catch( SymbolNotDefinedException e ) {
      return false;
    } catch( SymbolIsNotVariableException e ) {
      return false;
    }

    // Create the function call
    FunctionCall call = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_FOR_CALL_IMAGE_FUNCTION() ) );

    // First four parameters are the same on both versions of the library
    // interface

    // First parameter: reference to the data
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
        ObjectAnalizer.getDeclarator( vd, id ) ) );

    // Second parameter: data size
    Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize(
      (Identifier)vd.getDeclarator( 0 ).getSymbol(), ref );
    if( size == null ) {
      size = new IntegerLiteral( 1 );
    }
    call.addArgument( size );

    // Third parameter: data type
    try {
      call.addArgument( (Identifier)
        cppc.compiler.transforms.shared.TypeManager.getType(
        vd.getSpecifiers() ).getBaseType().clone() );
    } catch( Exception e ) {
      String message = "Warning: CPPC does not support registering objects " +
          "of type: " + e.getMessage() + "\n" + "\tPlease contact developers " +
          "to issue a feature request";
      printErrorInTranslation( System.err, ref, message );
    }

    // Fourth parameter: register name
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
        id.toString() ) );

    // Add further parameters
    furtherModifyRegisterForCallImage( call, vd );

    // Create and insert the statement
    ExpressionStatement callStatement = new ExpressionStatement( call );
    statementList.addStatementBefore( ref, callStatement );

    return true;
  }

  protected abstract void furtherModifyRegisterForCallImage( FunctionCall call,
    VariableDeclaration vd );
}
