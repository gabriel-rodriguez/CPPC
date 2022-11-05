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

import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.exceptions.TypeNotSupportedException;
import cppc.compiler.transforms.shared.DataType;
import cppc.compiler.transforms.shared.TypedefDataType;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.util.Iterator;
import java.util.Vector;

public abstract class CppcRegisterTranslator
  extends TranslationModule<CppcRegisterPragma> {

  private static final String STRING_LENGTH =
    "cppc_string_length_";
  private static int stringsFound = 0;

  public CppcRegisterTranslator() {
    super();
  }

  public Class getTargetClass() {
    return CppcRegisterPragma.class;
  }

  public void translate( CppcRegisterPragma pragma ) {
    // Add jump label to the beginning of the register block
    CppcLabel jumpLabel = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_LABEL() ) );
    pragma.swapWith( jumpLabel );
    jumpLabel.setLineNumber( pragma.where() );

    // Add the CPPC_Register function calls
    Statement after = jumpLabel;
    for( CppcRegister register: pragma.getRegisters() ) {
      try {
        after = addCppcRegisterCall( after, register );
      } catch( SymbolNotDefinedException e ) {
      } catch( SymbolIsNotVariableException e ) {
      } catch( TypeNotSupportedException e ) {
        String message = "Warning: CPPC does not support registering " +
          "objects of type: " + e.getMessage() + "\n" +
          "\tPlease contact developers to issue a feature request";
        printErrorInTranslation( System.err, jumpLabel, message );
      }
    }

    // Put the conditional jump mark after the last register
    CompoundStatement statementList =
      (CompoundStatement)jumpLabel.getParent();
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter( after, jump );
  }

  protected abstract void furtherModifyRegisterCall( FunctionCall call,
    VariableDeclarator varDeclarator );
  protected abstract Statement getRegisterCallStatement(
    VariableDeclarator declarator, FunctionCall call );

  private Statement addCppcRegisterCall( Statement after,
    CppcRegister register ) throws SymbolNotDefinedException,
    SymbolIsNotVariableException, TypeNotSupportedException {

    // Get a variable declaration for this symbol
    VariableDeclaration varDecl =
      LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        after, register.getName() );
    VariableDeclarator varDeclarator = null;
    if( varDecl instanceof cppc.compiler.fortran.CommonBlock ) {
      cppc.compiler.fortran.CommonBlock block =
        (cppc.compiler.fortran.CommonBlock)varDecl;
      varDecl = block.getDeclaration( register.getName() );
    }

    // Get the statement list
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( after,
        CompoundStatement.class );

    // If this register's size depends on a function call (strlen) add temp
    // variable for holding its size and initialize.
    if( register.getSize() instanceof FunctionCall ) {
      FunctionCall call = (FunctionCall)register.getSize();

      // Add variable declaration to the symbol table
      Identifier sizeId = new Identifier( STRING_LENGTH + stringsFound++ );
      VariableDeclaration sizeDeclaration = new VariableDeclaration(
        Specifier.INT, new VariableDeclarator( sizeId ) );
      statementList.addDeclaration( sizeDeclaration );

      // Add "+1" to the size (for \0)
      BinaryExpression sizeExpr = new BinaryExpression( register.getSize(),
        BinaryOperator.ADD, new IntegerLiteral( 1 ) );
      sizeExpr.setParens( false );

      // Create the assignment
      AssignmentExpression sizeAssignment = new AssignmentExpression( sizeId,
        AssignmentOperator.NORMAL, sizeExpr );
      ExpressionStatement sizeStmt = new ExpressionStatement( sizeAssignment );
      statementList.addStatementAfter( after, sizeStmt );
      after = sizeStmt;

      // Add a register for the size variable
      CppcRegister sizeRegister = new CppcRegister( sizeId,
        new IntegerLiteral( 1 ) );
      after = this.addCppcRegisterCall( after, sizeRegister );

      // Modify the original register's size
      register.setSize( sizeId );

      // Initialize the variable to an empty string if it is not originally
      // initialized (to avoid SIGSEGV when restarting and executing strlen on
      // a non-initialized string)
      for( int i = 0; i < varDecl.getNumDeclarators(); i++ ) {
        if( varDecl.getDeclarator( i ).getSymbol().equals(
          register.getName() ) ) {

          varDeclarator = (VariableDeclarator)varDecl.getDeclarator( i );
          if( varDeclarator.getInitializer() == null ) {
            Initializer initializer = new Initializer(
              new StringLiteral( "" ) );
            varDeclarator.setInitializer( initializer );
          }

          break;
        }
      }
    }

    // Create the function call statement
    FunctionCall cppcRegisterCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_FUNCTION() ) );

    for( int i=0; i < varDecl.getNumDeclarators(); i++ ) {
      if( varDecl.getDeclarator( i ).getSymbol().equals(
        register.getName() ) ) {

        varDeclarator = (VariableDeclarator)varDecl.getDeclarator( i );
        break;
      }
    }

    cppcRegisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
        varDeclarator ) );

    DataType dataType = TypeManager.getType( varDecl.getSpecifiers() );
    if( dataType == null ) {
      return after;
    }

    Expression registerSize = null;
    if( register.getSize() == null ) {
      registerSize = new IntegerLiteral( dataType.size() );
    } else {
      registerSize = (Expression)register.getSize().clone();
      if( dataType.size() != 1 ) {
        registerSize = new BinaryExpression(
          new IntegerLiteral( dataType.size() ), BinaryOperator.MULTIPLY,
          registerSize );
      }
    }

    if( dataType instanceof TypedefDataType ) {
      if( ((TypedefDataType)dataType).size() != 1 ) {
        IntegerLiteral size = new IntegerLiteral(
          ((TypedefDataType)dataType).size() );
        registerSize = new BinaryExpression( registerSize,
          BinaryOperator.MULTIPLY, size );
      }
    }

    cppcRegisterCall.addArgument( registerSize );
    cppcRegisterCall.addArgument( (Identifier)dataType.getBaseType().clone() );
    cppcRegisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
        register.getName().toString() ) );
    furtherModifyRegisterCall( cppcRegisterCall, varDeclarator );
    Statement cppcRegisterCallStatement = getRegisterCallStatement(
      varDeclarator, cppcRegisterCall );


    // Insert the ExpressionStatement
    statementList.addStatementAfter( after, cppcRegisterCallStatement );

    return cppcRegisterCallStatement;
  }
}
