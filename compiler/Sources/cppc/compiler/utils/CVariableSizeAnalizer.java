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




package cppc.compiler.utils;

import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.SizeofExpression;
import cetus.hir.Specifier;
import cetus.hir.SymbolTable;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CVariableSizeAnalizer implements VariableSizeAnalizer {

  private static final String STRLEN_FUNCTION_NAME = "strlen";

  protected CVariableSizeAnalizer() {}

  public Expression getSize( Identifier id, Traversable reference ) {

    // Find a SymbolTable for this reference
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass(
      reference, SymbolTable.class );

    // Find the declaration for this identifier
    Declaration declaration = symbolTable.findSymbol( id );

    // Check the declaration exists
    if( declaration == null ) {
      System.out.println( "BUG: Symbol " + id + " not found in symbol table in "
        + CVariableSizeAnalizer.class + ". Method: getSize()" );
      System.exit( 0 );
    }

    // Check that is a VariableDeclaration
    if( !(declaration instanceof VariableDeclaration) ) {
      System.err.println( "BUG: Symbol " + id + " is not a variable. In " +
        CVariableSizeAnalizer.class + ". Method: getSize()" );
      System.exit( 0 );
    }

    VariableDeclaration vd = (VariableDeclaration)declaration;

    // Find the Declarator for our Identifier
    for( int i = 0; i < vd.getNumDeclarators(); i++ ) {

      Declarator declarator = vd.getDeclarator( i );
      Identifier declId = (Identifier)declarator.getSymbol();
      if( declId.equals( id ) ) {

        // Get the array and pointer specifiers for this declarator
        List arraySpecifiers = declarator.getArraySpecifiers();
        List pointerSpecifiers = declarator.getSpecifiers();

        if( ( arraySpecifiers.size() == 0 ) &&
          ( pointerSpecifiers.size() == 0 ) ) {

          return null; // Simple variable
        }

        if( pointerSpecifiers.size() == 0 ) {
          // This is an array
          return getSizeOfArray( arraySpecifiers );
        }

        if( arraySpecifiers.size() == 0 ) {
          // This is a pointer
          return getSizeOfPointer( id, declarator );
        }
      }
    }

    System.out.println( "WARNING: " +
      "cppc.compiler.utils.CVariableSizeAnalizer.getSize(): cannot handle " +
      "declaration " + declaration );
    return null;
  }

  private Expression getSizeOfArray( List arraySpecifiers ) {

    Iterator iter = arraySpecifiers.iterator();
    Expression expr = new IntegerLiteral( 1 );

    while( iter.hasNext() ) {
      ArraySpecifier spec = (ArraySpecifier)iter.next();
      for( int i = 0; i < spec.getNumDimensions(); i ++ ) {
        expr = new BinaryExpression( expr, BinaryOperator.MULTIPLY,
          spec.getDimension( i ) );
      }
    }

    return expr;
  }

  private Expression getSizeOfPointer( Identifier id, Declarator declarator ) {
    VariableDeclaration vd =
      (VariableDeclaration)ObjectAnalizer.getParentOfClass( declarator,
      VariableDeclaration.class );
    Procedure procedure = (Procedure)ObjectAnalizer.getParentOfClass( vd,
      Procedure.class );

    // Search for a malloc call that creates memory for this pointer
    if( procedure == null ) {
      // This declaration is probably global. Try to use the main procedure
      Program program = (Program)ObjectAnalizer.getParentOfClass( vd,
        Program.class );
      procedure = ObjectAnalizer.findMainProcedure( program );
    }
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator iter = new DepthFirstIterator( statementList );

    while( iter.hasNext() ) {

      FunctionCall functionCall = null;
      try {
        functionCall = (FunctionCall)iter.next( FunctionCall.class );
        Identifier procedureName = (Identifier)functionCall.getName();
        if( procedureName.equals( new Identifier( "malloc" ) ) ) {
          AssignmentExpression assignment =
            (AssignmentExpression)ObjectAnalizer.getParentOfClass( functionCall,
              AssignmentExpression.class );
          if( assignment != null ) {
            Identifier lhs = (Identifier)assignment.getLHS();
            if( lhs.equals( id ) ) {
              Expression mallocParameter = functionCall.getArgument( 0 );

              // Divide the malloc argument by the sizeof() the base argument
              // (obtained through its specifiers)
              SizeofExpression sizeofExpr = new SizeofExpression(
                vd.getSpecifiers() );
              return new BinaryExpression( (Expression)mallocParameter.clone(),
                BinaryOperator.DIVIDE, sizeofExpr );
            }
          }
        }
      } catch( NoSuchElementException e ) {}
    }

    if( vd.getSpecifiers().contains( Specifier.CHAR ) ) {
      // Represent size as a call to strlen
      FunctionCall functionCall = new FunctionCall( new Identifier(
        STRLEN_FUNCTION_NAME ) );
      functionCall.addArgument( (Identifier)id.clone() );

      return functionCall;
    }

    // If we have not been able to determine pointer size, then we assume that
    // this is a pointer to EXACTLY one element
    // This function requires a lot more work, as can be seen
    return new IntegerLiteral( 1 );
  }
}
