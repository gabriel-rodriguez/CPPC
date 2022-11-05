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

import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;

import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.Hashtable;

public class AddOpenFilesControl extends
  cppc.compiler.transforms.semantic.skel.AddOpenFilesControl {

  private static final String LOGICAL_UNIT_PARAMETER = "LogicalUnit";
  private static final String PATH_PARAMETER = "Path";
  private static final String STATUS_PARAMETER = "Status";

  private AddOpenFilesControl( Program program ) {
    super( program );
  }

  public static final AddOpenFilesControl getTransformInstance(
    Program program ) {

    return new AddOpenFilesControl( program );
  }

  protected Statement getCppcOpenCall( FunctionCall call ) {

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );
    Hashtable<String,String> attributes = c.getSemantic( CPPC_IO_OPEN_ROLE );

    // Get parameters placement
    String unitParameterPlace = attributes.get( LOGICAL_UNIT_PARAMETER );
    String pathParameterPlace = attributes.get( PATH_PARAMETER );
    String statusParameterName = attributes.get( STATUS_PARAMETER );

    // Get expressions
    Expression unitParameter = call.getArgument( new Integer(
      unitParameterPlace ).intValue() - 1 );
    Expression pathParameter = call.getArgument( new Integer(
      pathParameterPlace ).intValue() - 1 );

    // Strip the UNIT= if necessary from the unitParameter
    if( unitParameter instanceof BinaryExpression ) {
      unitParameter = ((BinaryExpression)unitParameter).getRHS();
    }

    // Strip the FILE= if necessary from the fileParameter
    if( pathParameter instanceof BinaryExpression ) {
      pathParameter = ((BinaryExpression)pathParameter).getRHS();
    }

    // Add the string end mark to the path parameter
    pathParameter = concatCStringEnd( pathParameter );

    // Create the CPPC File Code parameter
    IntegerLiteral fileCode = new IntegerLiteral(
      GlobalNamesFactory.getGlobalNames().NEXT_FILE_CODE() );

    // Create the function call
    FunctionCall cppcOpenCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_DESCRIPTOR_FUNCTION() ) );

    // Add the arguments to the call
    cppcOpenCall.addArgument( (Expression)unitParameter.clone() );
    cppcOpenCall.addArgument( pathParameter );
    cppcOpenCall.addArgument( fileCode );

    // If the application checks the Status of the open, then we
    // use it for checking if we have to CPPCF_OPEN
    for( int i = 0; i < call.getNumArguments(); i++ ) {
      Expression expr = call.getArgument( i );
      if( expr instanceof AssignmentExpression ) {
        AssignmentExpression namedParam = (AssignmentExpression)expr;
	if( namedParam.getLHS().toString().equals( statusParameterName ) ) {

	  return new IfStatement(
	    new BinaryExpression( (Expression)namedParam.getRHS().clone(),
	      BinaryOperator.COMPARE_EQ, new IntegerLiteral( 0 ) ),
	    new ExpressionStatement( cppcOpenCall ) );
	}
      }
    }

    return new ExpressionStatement( cppcOpenCall );
  }

  private static final Expression concatCStringEnd( Expression e ) {
    FunctionCall zeroChar = new FunctionCall( new Identifier("CHAR") );
    zeroChar.addArgument( new IntegerLiteral( 0 ) );
    
    BinaryExpression concat = new BinaryExpression( e, BinaryOperator.F_CONCAT, zeroChar );
    concat.setParens( false );
    
    return concat;
  }

  protected Statement getCppcCloseCall( FunctionCall call ) {

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );
    Hashtable<String,String> attributes = c.getSemantic( CPPC_IO_CLOSE_ROLE );

    // Get parameters placement
    String unitParameterPlace = attributes.get( LOGICAL_UNIT_PARAMETER );

    // Get expressions
    Expression unitParameter = call.getArgument( new Integer(
      unitParameterPlace ).intValue() - 1 );

    // Create the function call
    FunctionCall cppcCloseCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().UNREGISTER_DESCRIPTOR_FUNCTION() ) );

    // Add the arguments to the call
    cppcCloseCall.addArgument( (Expression)unitParameter.clone() );

    return new ExpressionStatement( cppcCloseCall );
  }
}
