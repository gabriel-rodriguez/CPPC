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
import cetus.hir.Identifier;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class IntrinsicDeclaration extends Declaration {

    private static Method classPrintMethod;

    static {
      Class[] params = { IntrinsicDeclaration.class, OutputStream.class };
      try {
        classPrintMethod = params[0].getMethod( "defaultPrint", params );
      } catch( NoSuchMethodException e ) {
        e.printStackTrace();
      }
    }

    public IntrinsicDeclaration( List<Identifier> names ) {

      super( names.size() );

      object_print_method = classPrintMethod;

      for( Identifier name: names ) {
        addChild( name );
      }
    }

    private void addChild( Traversable t ) {

      if( t.getParent() != null ) {
        throw new NotAnOrphanException();
      }

      children.add( t );
      t.setParent( this );
    }

    public Object clone() {
      IntrinsicDeclaration copy = (IntrinsicDeclaration)super.clone();
      return copy;
    }

    public List getDeclaredSymbols() {
      return children;
    }

    public static void defaultPrint( IntrinsicDeclaration obj, OutputStream stream ) {

      PrintStream p = new PrintStream( stream );

      p.print( "INTRINSIC " );

      Iterator iter = obj.children.iterator();
      while( iter.hasNext() ) {
        ((Identifier)iter.next()).print( stream );
        if( iter.hasNext() ) {
          p.print( ", " );
        }
      }
    }

    public static void setClassPrintMethod( Method m ) {
      classPrintMethod = m;
    }
}