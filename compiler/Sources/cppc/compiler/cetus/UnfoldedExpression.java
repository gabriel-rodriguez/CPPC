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
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class UnfoldedExpression extends Expression {

  private static Method class_print_method;

  static {
    Class [] params = { UnfoldedExpression.class, OutputStream.class };

    try {
      class_print_method = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      throw new InternalError();
    }
  }

  private Identifier var;
  private Expression varValue;

  public UnfoldedExpression( Identifier var, Expression varValue,
    Expression inner ) {

    object_print_method = class_print_method;

    this.var = var;
    this.varValue = varValue;

    this.addChildren( inner );
  }

  public Identifier getVar() {
    return var;
  }

  public Expression getVarValue() {
    return varValue;
  }

  private void addChildren( Traversable t ) {
    if( t.getParent() != null ) {
      throw new IllegalArgumentException();
    }

    children.add( t );
    t.setParent( this );
  }

  public Expression getExpression() {
    return (Expression)children.get( 0 );
  }

  public static void defaultPrint( UnfoldedExpression mexpr,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );
    p.print( "[[" );
    p.print( "(" );
    mexpr.var.print( stream );
    p.print( "=" );
    mexpr.varValue.print( stream );
    p.print( ")" );
    mexpr.getExpression().print( stream );
    p.print( "]]" );
  }

  public static void setClassPrintMethod( Method m ) {
    class_print_method = m;
  }

  public Object clone() {
    UnfoldedExpression clone = new UnfoldedExpression( var, varValue,
      (Expression)this.getExpression().clone() );
    return clone;
  }
}