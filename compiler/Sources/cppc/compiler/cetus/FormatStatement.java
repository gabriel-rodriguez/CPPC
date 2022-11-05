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
import cetus.hir.NotAnOrphanException;
import cetus.hir.Statement;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FormatStatement extends Statement {

    private static Method classPrintMethod;

    static {

	Class[] params = { FormatStatement.class, OutputStream.class };

	try {
	    classPrintMethod = params[0].getMethod( "defaultPrint", params );
	} catch( NoSuchMethodException e ) {
	    e.printStackTrace();
	}
    }

    public FormatStatement() {
	object_print_method = classPrintMethod;
    }
    
    public FormatStatement( List<Expression> parameters ) {

	object_print_method = classPrintMethod;

	Iterator<Expression> iter = parameters.iterator();
	while( iter.hasNext() ) {
	    Expression expr = iter.next();
	    children.add( expr );
	    expr.setParent( this );
	}
    }

    public static void defaultPrint( FormatStatement obj, OutputStream stream ) {

	PrintStream p = new PrintStream( stream );

	p.print( "       FORMAT( " );

	Iterator iter = obj.children.iterator();
	while( iter.hasNext() ) {
	    Expression expr = (Expression)iter.next();
	    expr.print( stream );
	    if( iter.hasNext() ) {
		p.print( ", " );
	    }
	}

	p.println(" )");
    }

    public List getParameters() {
	return children;
    }

    public void addParameter( Expression parameter ) {
	
	if( parameter.getParent() != null ) {
	    throw new NotAnOrphanException();
	}

	children.add( parameter );
	parameter.setParent( this );
    }

    public void addParameterBefore( Expression reference, Expression parameter ) {

	if( parameter.getParent() != null ) {
	    throw new NotAnOrphanException();
	}

	for( int i = 0; i < children.size(); i++ ) {
	    if( children.get( i ).equals( reference ) ) {
		children.add( i, parameter );
		parameter.setParent( this );
		return;
	    }
	}

	throw new NoSuchElementException();
    }

    public void addParameterAfter( Expression reference, Expression parameter ) {

	if( parameter.getParent() != null ) {
	    throw new NotAnOrphanException();
	}

	for( int i = 0; i < children.size(); i++ ) {
	    if( children.get( i ).equals( reference ) ) {
		children.add( i+1, parameter );
		parameter.setParent( this );
		return;
	    }
	}

	throw new NoSuchElementException();
    }

    public static void setClassPrintMethod( Method m ) {
	classPrintMethod = m;
    }
}
