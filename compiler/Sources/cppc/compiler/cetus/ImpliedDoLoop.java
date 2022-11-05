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




package cppc.compiler.cetus;

import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImpliedDoLoop extends Expression {

    private static Method classPrintMethod;

    static {
	Class[] params = { ImpliedDoLoop.class, OutputStream.class };

	try {
	    classPrintMethod = params[0].getMethod( "defaultPrint", params );
	} catch( NoSuchMethodException e ) {
	    e.printStackTrace();
	}
    }

    private int exprCount;

    public ImpliedDoLoop( List<Expression> objects, Identifier doVar, Expression start, Expression stop ) {

	super( 4 );

	object_print_method = classPrintMethod;
	exprCount = objects.size();

	for( Expression expr: objects ) {
	    addChild( expr );
	}
	
	addChild( doVar );
	addChild( start );
	addChild( stop );
    }
    
    public ImpliedDoLoop( List<Expression> objects, Identifier doVar, Expression start, Expression stop, Expression step ) {

	super( 5 );

	object_print_method = classPrintMethod;
	exprCount = objects.size();

	for( Expression expr: objects ) {
	    addChild( expr );
	}

	addChild( doVar );
	addChild( start );
	addChild( stop );
	addChild( step );
    }

    private void addChild( Traversable t ) {

	if( t.getParent() != null ) {
	    throw new NotAnOrphanException();
	}

	children.add( t );
	t.setParent( this );
    }

    public List<Expression> getExpressions() {

	ArrayList<Expression> exprList = new ArrayList<Expression>( exprCount );
	
	for( int i = 0; i < exprCount; i++ ) {
	    exprList.add( (Expression)children.get( i ) );
	}

	return exprList;
    }

    public Identifier getDoVar() {
	return (Identifier)children.get( exprCount );
    }

    public Expression getStart() {
	return (Expression)children.get( exprCount+1 );
    }

    public Expression getStop() {
	return (Expression)children.get( exprCount+2 );
    }

    public Expression getStep() {

	try {
	    return (IntegerLiteral)children.get( exprCount+3 );
	} catch( IndexOutOfBoundsException e ) {
	    return null;
	}
    }

    public static void defaultPrint( ImpliedDoLoop obj, OutputStream stream ) {

	PrintStream p = new PrintStream( stream );

	p.print( "( " );

	Iterator<Expression> iter = obj.getExpressions().iterator();
	while( iter.hasNext() ) {
	    iter.next().print( stream );
	    if( iter.hasNext() ) {
		p.print( ", " );
	    }
	}

	p.print( ", " );
	obj.getDoVar().print( stream );
	p.print( " = " );
	obj.getStart().print( stream );
	p.print( ", " );
	obj.getStop().print( stream );
	if( obj.getStep() != null ) {
	    p.print( ", " );
	    obj.getStep().print( stream );
	}
	p.print( " )" );
    }

    public static void setClassPrintMethod( Method m ) {
	classPrintMethod = m;
    }
}
