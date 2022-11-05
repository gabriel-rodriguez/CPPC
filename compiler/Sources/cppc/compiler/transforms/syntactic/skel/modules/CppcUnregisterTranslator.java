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

import cetus.hir.Annotation;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcUnregisterPragma;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;

import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;


public abstract class CppcUnregisterTranslator
  extends TranslationModule<CppcUnregisterPragma> {

  public CppcUnregisterTranslator() {
    super();
  }

  public Class getTargetClass() {
    return CppcUnregisterPragma.class;
  }

  public void translate( CppcUnregisterPragma pragma ) {
    // Add the CPPC_Unregister function calls
    Statement after = pragma;
    for( Identifier unregister: pragma.getUnregisters() ) {
      try {
        after = addCppcUnregisterCall( after, unregister.toString() );
      } catch( SymbolNotDefinedException e ) {
        String message = "Warning: symbol '" + unregister +
          "'not unregistered: it is not defined in this scope";
        printErrorInTranslation( System.out, pragma, message );
      } catch( SymbolIsNotVariableException e ) {
        String message = "Warning: symbol '" + unregister +
          "' not unregistered: it is not a variable symbol";
        printErrorInTranslation( System.out, pragma, message );
      }
    }

    pragma.detach();
  }

  private Statement addCppcUnregisterCall( Statement after, String unregister )
    throws SymbolNotDefinedException, SymbolIsNotVariableException {

    // Get the procedure body we are into (for symbol table inspecting)
    SymbolTable symbolTable = (SymbolTable)after.getParent();

    // Check if the variable to be registered is defined in this scope
    Declaration decl = symbolTable.findSymbol( new Identifier( unregister ) );
    if( decl == null ) {
      throw new SymbolNotDefinedException( unregister );
    }

    // Check if this symbol is a variable
    if( !(decl instanceof VariableDeclaration ) ) {
      throw new SymbolIsNotVariableException( unregister );
    }

    VariableDeclaration varDecl = (VariableDeclaration)decl;

    // Create the function call statement
    FunctionCall cppcUnregisterCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().UNREGISTER_FUNCTION() ) );

    cppcUnregisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
        unregister ) );
    ExpressionStatement cppcUnregisterCallStatement = new ExpressionStatement(
      cppcUnregisterCall );

    // Insert the ExpressionStatement
    CompoundStatement statementList = (CompoundStatement)after.getParent();
    statementList.addStatementAfter( after, cppcUnregisterCallStatement );

    return cppcUnregisterCallStatement;
  }
}
