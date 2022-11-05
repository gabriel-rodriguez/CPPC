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




package cppc.compiler.fortran;

import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Printable;
import cetus.hir.Traversable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SaveDeclaration extends Declaration {

    private static Method classPrintMethod;

    static {
	Class[] params = { SaveDeclaration.class, OutputStream.class };
	try {
	    classPrintMethod = params[0].getMethod( "defaultPrint", params );
	} catch( NoSuchMethodException e ) {
	    e.printStackTrace();
	}
    }

    public SaveDeclaration() {

	super( 0 );

	object_print_method = classPrintMethod;
    }

    public SaveDeclaration( List<Declarator> names ) {

	super( names.size() );

	object_print_method = classPrintMethod;

	for( Declarator decl: names ) {
	    addChild( decl );
	}
    }

    public void addDeclarator( Declarator decl ) {
	addChild( decl );
    }

    /** As all these declarations are cosmetic ones (they do not
     * effectively store computation info, but printing one) they
     * do not need to declare anything. Variable declarations will
     * be inserted to hold that.
     **/
    public List getDeclaredSymbols() {
	return new ArrayList( 0 );
    }

    public List getDeclarators() {
	return children;
    }

    private void addChild( Traversable t ) {

	if( t.getParent() != null ) {
	    throw new NotAnOrphanException();
	}

	children.add( t );
	t.setParent( this );
    }

    public Object clone() {

	SaveDeclaration copy = (SaveDeclaration)super.clone();

	return copy;
    }

    public static void defaultPrint( SaveDeclaration obj, OutputStream stream ) {

	PrintStream p = new PrintStream( stream );

	p.print( "SAVE " );
	Iterator identifiers = obj.children.iterator();
	while( identifiers.hasNext() ) {
	    ((Printable)identifiers.next()).print( stream );
	    if( identifiers.hasNext() ) {
		p.print( ", " );
	    }
	}
    }

    public String toString() {

	ByteArrayOutputStream stream = new ByteArrayOutputStream();

	print( stream );

	return stream.toString();
    }
}
