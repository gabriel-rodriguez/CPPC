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




package cppc.compiler.transforms.semantic.stub.c;

import cetus.hir.AccessExpression;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.PointerSpecifier;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.UnaryExpression;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.Hashtable;

public class AddOpenFilesControl extends
  cppc.compiler.transforms.semantic.skel.AddOpenFilesControl {

  private static final String FD_PARAMETER = "FileDescriptor";
  private static final String PATH_PARAMETER = "Path";
  private static final String DESCRIPTOR_TYPE_PARAMETER = "DescriptorType";
  private static final String TMP_PATH_PARAMETER = "CPPC_PATH_TMP_";
  private static int TMP_PATH_COUNT = 0;

  private AddOpenFilesControl( Program program ) {
    super( program );
  }

  public static final AddOpenFilesControl getTransformInstance(
    Program program ) {

    return new AddOpenFilesControl( program );
  }

  private void checkPathParameter( FunctionCall call ) {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );
    Hashtable<String,String> attributes = c.getSemantic( CPPC_IO_OPEN_ROLE );

    // Get path parameter placement and expression
    String pathParameterPlace = attributes.get( PATH_PARAMETER );
    int pos = new Integer( pathParameterPlace ).intValue() - 1;
    Expression pathParameter = call.getArgument( pos );

    // If it is not an identifier, transform call by adding tmp variable
    if( !(pathParameter instanceof Identifier) ) {
      Identifier newPath = new Identifier( TMP_PATH_PARAMETER +
        TMP_PATH_COUNT++ );
      call.setArgument( pos, newPath );

      // Create an assignment statement before the call
      AssignmentExpression expr = new AssignmentExpression(
        (Identifier)newPath.clone(), AssignmentOperator.NORMAL, pathParameter );
      ExpressionStatement stmt = new ExpressionStatement( expr );

      // Insert statement before call
      Statement ref = (Statement)call.getStatement();
      CompoundStatement statementList = (CompoundStatement)ref.getParent();
      statementList.addStatementBefore( ref, stmt );

      // Add variable declaration to compound statement
      VariableDeclarator vdecl = new VariableDeclarator(
        PointerSpecifier.UNQUALIFIED, (Identifier)newPath.clone() );
      VariableDeclaration vd = new VariableDeclaration( Specifier.CHAR, vdecl );
      statementList.addDeclaration( vd );
    }
  }

  protected Statement getCppcOpenCall( FunctionCall call ) {
    // Check path parameter for the call
    this.checkPathParameter( call );

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );
    Hashtable<String,String> attributes = c.getSemantic( CPPC_IO_OPEN_ROLE );

    // Get path parameter placement and expression
    String pathParameterPlace = attributes.get( PATH_PARAMETER );
    Expression pathParameter = call.getArgument( new Integer(
      pathParameterPlace ).intValue() - 1 );

    // Try to get file descriptor parameter and expression
    String fdParameterStr = attributes.get( FD_PARAMETER );
    Expression fdParameter = null;
    try {
      fdParameter = getFdParameter( fdParameterStr, call );
    } catch( Exception e ) {
      System.err.println( "WARNING: cannot find Expression for a descriptor "+
        "in call " + call );
      System.err.println( "'" + e.getMessage() + " is not a variable symbol, "+
        "or is not defined" );
      System.err.println( "Call ignored. The opened file will not be correctly "
        + "recovered" );
      return null;
    }

    // Get the descriptor type
    String descriptorType = attributes.get( DESCRIPTOR_TYPE_PARAMETER );

    // Create the CPPC File Code parameter
    IntegerLiteral fileCode = new IntegerLiteral(
      GlobalNamesFactory.getGlobalNames().NEXT_FILE_CODE() );

    // Create the function call
    FunctionCall cppcOpenCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_DESCRIPTOR_FUNCTION() ) );

    // Add the arguments to the call
    cppcOpenCall.addArgument( fileCode );
    cppcOpenCall.addArgument( fdParameter );
    cppcOpenCall.addArgument( new Identifier( descriptorType ) );
    cppcOpenCall.addArgument( (Expression)pathParameter.clone() );

    // If the parameter is a pointer: assign the result to it
    if( fdParameter instanceof UnaryExpression ) {
      // Assuming reference here: &parameter
      return new ExpressionStatement( cppcOpenCall );
    } else {
      // Assuming pointer here: need to assign
      AssignmentExpression assignment = new AssignmentExpression(
        (Expression)fdParameter.clone(), AssignmentOperator.NORMAL,
        cppcOpenCall );

      return new ExpressionStatement( assignment );
    }
  }

  private Expression getFdParameter( String stringValue, FunctionCall call )
    throws SymbolNotDefinedException, SymbolIsNotVariableException {

    Expression fdParam = null;
    // If the File Descriptor is returned by the call: get the left part of the
    // assignment
    if( stringValue.equals( "return" ) ) {
      // This could be an assignment or an initializer.

      // Try the assignment
      AssignmentExpression expr =
        (AssignmentExpression)ObjectAnalizer.getParentOfClass( call,
          AssignmentExpression.class );

      if( expr != null ) {
        fdParam = expr.getLHS();
      } else {
        // Else, this should be inside an initializer
        VariableDeclarator vdecl =
          (VariableDeclarator)ObjectAnalizer.getParentOfClass( call,
            VariableDeclarator.class );
        fdParam = vdecl.getSymbol();
      }
    } else{
      // If the File Descriptor is directly a parameter of the call, extract it
      try {
        fdParam = call.getArgument( new Integer( stringValue ).intValue() - 1 );
      } catch( NumberFormatException e ) {}
    }

    if( fdParam != null ) {
      // Obtain reference (pointer)
      if( !(fdParam instanceof Identifier ) &&
        !(fdParam instanceof AccessExpression) ) {

        System.err.println( "BUG: fdParam is not an instance of "+
          "cetus.hir.Identifier" );
        System.err.println( "in " + this.getClass() );
        System.err.println( "Method getFdParameter()" );
        System.exit( 0 );
      }

      Identifier fdIdentifier = null;
      if( fdParam instanceof AccessExpression ) {
        fdIdentifier = (Identifier)((AccessExpression)fdParam).getLHS();
      } else {
        fdIdentifier = (Identifier)fdParam;
      }

//       Identifier fdIdentifier = (Identifier)fdParam;
      SymbolTable table = ObjectAnalizer.getSymbolTable( fdIdentifier );
      Declaration decl = table.findSymbol( fdIdentifier );
      if( decl == null ) {
        throw new SymbolNotDefinedException( fdIdentifier.toString() );
      }

      if( !(decl instanceof VariableDeclaration) ) {
        throw new SymbolIsNotVariableException( fdIdentifier.toString() );
      }

      VariableDeclaration varDecl = (VariableDeclaration)decl;
      Declarator declarator = null;
      for( int i = 0; i < varDecl.getNumDeclarators(); i++ ) {
        if( varDecl.getDeclarator( i ).getSymbol().equals( fdIdentifier ) ) {
          declarator = varDecl.getDeclarator( i );
          break;
        }
      }

      return LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
        declarator );
    }

    System.err.println( "ERROR:  Unable to extract FD information for function"
      + " call " + call );
    System.err.println( "\tMethod cppc.compiler.transforms.semantic.stub.c."+
      "AddOpenFilesControl.getFdParameter needs to be extended for dealing"+
      " with " + call.getName() + "()" );
    System.exit( 0 );

    return null;
  }

  protected Statement getCppcCloseCall( FunctionCall call ) {

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );
    Hashtable<String,String> attributes = c.getSemantic( CPPC_IO_CLOSE_ROLE );

    // Get FD string & parameter
    String fdParameterStr = attributes.get( FD_PARAMETER );
    Expression fdParameter = null;
    try {
      fdParameter = getFdParameter( fdParameterStr, call );
    } catch( Exception e ) {
      System.err.println( "WARNING: cannot find Expression for a descriptor "+
        "in call " + call );
      System.err.println( "'" + e.getMessage() + " is not a variable symbol, "+
        "or is not defined" );
      System.err.println( "Call ignored. The opened file will not be correctly "
        + "recovered" );
      return null;
    }

    // Create the function call
    FunctionCall cppcCloseCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().UNREGISTER_DESCRIPTOR_FUNCTION() ) );

    // Add the arguments to the call
    cppcCloseCall.addArgument( fdParameter );

    return new ExpressionStatement( cppcCloseCall );
  }
}
