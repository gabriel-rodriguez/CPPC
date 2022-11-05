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




package cppc.compiler.transforms.syntactic.stub.c.modules;

import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.Declaration;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;

public class CppcRegisterTranslator extends
  cppc.compiler.transforms.syntactic.skel.modules.CppcRegisterTranslator {

  public CppcRegisterTranslator() {
    super();
  }

  private Expression getSize( String sizeStr, SymbolTable symbolTable ) {

    // If the size is specified as an integer, we return an IntegerLiteral
    try {
      Integer sizeValue = new Integer( sizeStr );
      return new IntegerLiteral( sizeValue.intValue() );
    } catch( NumberFormatException e ) {}

    // If the size is specified as a variable, we return an Identifier
    Declaration decl = symbolTable.findSymbol( new Identifier( sizeStr ) );
    try {
      if( decl != null ) {
        // If the symbol is not a variable, error
        if( !(decl instanceof VariableDeclaration) ) {
          throw new SymbolIsNotVariableException( sizeStr );
        }

        return new Identifier( sizeStr );
      }

      throw new SymbolNotDefinedException( sizeStr );
    } catch( Exception e ) {
      e.printStackTrace();
      return null;
    }
  }

  protected void furtherModifyRegisterCall( FunctionCall call,
    VariableDeclarator varDeclarator ) {

    call.addArgument( getIsStatic( varDeclarator ) );
  }

  protected Statement getRegisterCallStatement( VariableDeclarator declarator,
    FunctionCall call ) {

    // If the variable is static, then the register function is called
    // Else, the result of the register call is assigned to the variable
    if( getIsStaticAsBoolean( declarator ) ) {
      return new ExpressionStatement( call );
    } else {
      // // variable = registerCall
      AssignmentExpression assignment = new AssignmentExpression(
        (Identifier)declarator.getSymbol().clone(), AssignmentOperator.NORMAL,
        call );
      return new ExpressionStatement( assignment );
    }
  }

  private Identifier getIsStatic( VariableDeclarator declarator ) {

    if( getIsStaticAsBoolean( declarator ) ) {
      return new Identifier( "CPPC_STATIC" );
    } else {
      return new Identifier( "CPPC_DYNAMIC" );
    }
  }

  private boolean getIsStaticAsBoolean( VariableDeclarator declarator ) {
    return( declarator.getSpecifiers().size() == 0 );
  }

}
