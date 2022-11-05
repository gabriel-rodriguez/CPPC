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
import cetus.hir.Expression;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataDeclaration extends Declaration {

  private static Method classPrintMethod;

  static {
    Class[] params = { DataDeclaration.class, OutputStream.class };
    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  List<Expression> expressions;
  List<Expression> initializers;

  public DataDeclaration( List<Expression> expressions,
    List<Expression> initializers ) {

    super( expressions.size() + initializers.size() );

    object_print_method = classPrintMethod;

    for( Expression expr: expressions ) {
      addChild( expr );
    }

    for( Expression expr: initializers ) {
      addChild( expr );
    }

    this.expressions = expressions;
    this.initializers = initializers;
  }

  private void addChild( Traversable t ) {

    if( t.getParent() != null ) {
      throw new NotAnOrphanException();
    }

    children.add( t );
    t.setParent( this );
  }

  public Object clone() {

    DataDeclaration copy = (DataDeclaration)super.clone();
    copy.expressions = new ArrayList<Expression>();
    copy.initializers = new ArrayList<Expression>();

    for( Expression expr: expressions ) {
      copy.expressions.add( (Expression)expr.clone() );
    }

    for( Expression expr: initializers ) {
      copy.initializers.add( (Expression)expr.clone() );
    }

    return copy;
  }

  public List getDeclaredSymbols() {
    return new ArrayList();
  }

  public void removeChild( Traversable child ) {
    super.removeChild( child );

    if( expressions.contains( child ) ) {
      expressions.remove( child );
      return;
    }

    if( initializers.contains( child ) ) {
      initializers.remove( child );
      return;
    }
  }

  public List getChildren() {
    return initializers;
  }

  public void setChild( int index, Traversable t ) {
    Expression safeChild = (Expression)t;
    initializers.set( index, safeChild );
  }

  public static void defaultPrint( DataDeclaration obj, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "DATA " );
    Iterator<Expression> iter = obj.expressions.iterator();
    while( iter.hasNext() ) {
      iter.next().print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }

    p.print( "/" );
    iter = obj.initializers.iterator();
    while( iter.hasNext() ) {
      iter.next().print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }
    p.print( "/" );
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod=m;
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  public List<Expression> getInitializers() {
    return initializers;
  }
}