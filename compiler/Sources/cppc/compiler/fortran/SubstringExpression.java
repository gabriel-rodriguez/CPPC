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

import cetus.hir.Expression;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Printable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

public class SubstringExpression extends Expression {

  private static Method classPrintMethod;

  static {
    Class[] params = { SubstringExpression.class, OutputStream.class };

    try {
      classPrintMethod = SubstringExpression.class.getMethod( "defaultPrint",
        params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  public SubstringExpression( Expression string, Expression lbound,
    Expression ubound ) {

    super( 3 );

    object_print_method = classPrintMethod;

    if( string.getParent() != null ) {
      throw new NotAnOrphanException();
    }
    children.add( string );
    string.setParent( this );

    if( lbound.getParent() != null ) {
      throw new NotAnOrphanException();
    }
    children.add( lbound );
    lbound.setParent( this );

    if( ubound.getParent() != null ) {
      throw new NotAnOrphanException();
    }
    children.add( ubound );
    ubound.setParent( this );
  }

  public Object clone() {
    SubstringExpression copy = (SubstringExpression)super.clone();
    return copy;
  }

  public static void defaultPrint( SubstringExpression obj,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    ((Printable)obj.children.get( 0 )).print( stream );

    p.print( "( " );
    ((Printable)obj.children.get( 1 )).print( stream );
    p.print( " : " );
    ((Printable)obj.children.get( 2 )).print( stream );
    p.print( " )");
  }

  public Expression getStringName() {
    return (Expression)children.get( 0 );
  }

  public Expression getLBound() {
    return (Expression)children.get( 1 );
  }

  public Expression getUBound() {
    return (Expression)children.get( 2 );
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }
}
