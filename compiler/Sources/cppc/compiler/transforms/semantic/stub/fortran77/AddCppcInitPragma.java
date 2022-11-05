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




package cppc.compiler.transforms.semantic.stub.fortran77;

import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.fortran.TypeDeclaration;
import cppc.compiler.utils.ObjectAnalizer;

public class AddCppcInitPragma extends
  cppc.compiler.transforms.semantic.skel.AddCppcInitPragma {

  private static final String IERROR_VARIABLE_NAME = "CPPC_IERROR_VAR";

  private AddCppcInitPragma( Program program ) {
    super( program );
  }

  public static final AddCppcInitPragma getTransformInstance(
    Program program ) {

    return new AddCppcInitPragma( program );
  }

  protected void checkMainProcedure( Procedure mainProc ) {

    // Add IERROR declaration to main procedure
    Identifier id = new Identifier( IERROR_VARIABLE_NAME );
    Declarator declarator = new VariableDeclarator( id );
    VariableDeclaration vd = new VariableDeclaration( Specifier.INT,
      declarator );

    // The new variables must be added just *before* the last declaration
    // statement found in the procedure. If they were added *after* it,
    // and it were the CPPC INIT directive: declaration after executable
    // statement
    DeclarationStatement declaration = new DeclarationStatement( vd );
    Statement ref = ObjectAnalizer.findLastDeclaration( mainProc );
    if( ref == null ) {
      mainProc.getBody().getChildren().add( 0, declaration );
      declaration.setParent( mainProc.getBody() );
    } else {
      mainProc.getBody().addStatementAfter( ref, declaration );
    }

    // Add COSMETIC DECLARATION just after the functional one
    TypeDeclaration cosmetic = new TypeDeclaration( Specifier.INT );
    cosmetic.addDeclarator( (Declarator)declarator.clone() );
    DeclarationStatement cosmeticStatement =
      new DeclarationStatement( cosmetic );
    mainProc.getBody().addStatementAfter( declaration, cosmeticStatement );
  }

  protected void addInitFunctionParameters( Procedure mainProc,
    FunctionCall call ) {

    // Add IERROR parameter to the call
    call.addArgument( new Identifier( IERROR_VARIABLE_NAME ) );
  }
}
