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




package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.ReturnStatement;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.transforms.shared.ProcedureWalker;

import java.util.List;

public class RequireProcedureReturns extends ProcedureWalker {

    private static String passName = "[RequireProcedureReturns]";

    private RequireProcedureReturns( Program program ) {
	super( program );
    }

    public static void run( Program program ) {

	Tools.printlnStatus( passName + " begin", 1 );

	RequireProcedureReturns transform = new RequireProcedureReturns( program );
	transform.start();

	Tools.printlnStatus( passName + " end", 1 );
    }

    protected void walkOverProcedure( Procedure procedure ) {

	// Get the last Statement of this procedure
	CompoundStatement statementList = procedure.getBody();
	List children = statementList.getChildren();
	Statement lastStatement = (Statement)children.get( children.size()-1 );

	// If the last Statement is already a ReturnStatement, we are done
	if( lastStatement instanceof ReturnStatement ) {
	    return;
	}

	// Get the return type for this procedure
	List returnType = procedure.getReturnType();
	
	// If the returnType is void, then no return is needed
	if( returnType.contains( Specifier.VOID ) ) {
	    return;
	}

	// C should accept '0' as a valid return for any non-void type
	ReturnStatement returnStatement = new ReturnStatement( new IntegerLiteral( 0 ) );
	statementList.addStatementAfter( lastStatement, returnStatement );
    }
}
	
