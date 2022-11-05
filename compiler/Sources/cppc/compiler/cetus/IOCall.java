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
import cetus.hir.FunctionCall;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IOCall extends FunctionCall {

  private static Method classPrintMethod;
  private int numParams;

  static {
    Class[] params = { IOCall.class, OutputStream.class };

    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  public IOCall( Expression function ) {
    super( function );

    object_print_method = classPrintMethod;
  }

  public IOCall( Expression function, List<Expression> varargs ) {

    super( function );

    object_print_method = classPrintMethod;

    for( Expression expr: varargs ) {
      addChild( expr );
    }

    numParams = 0;
  }

  public IOCall( Expression function, List<Expression> parameters,
    List<Expression> varargs ) {

    super( function );

    object_print_method = classPrintMethod;

    for( Expression expr: parameters ) {
      addChild( expr );
    }
    for( Expression expr: varargs ) {
      addChild( expr );
    }

    numParams = parameters.size();
  }

  private void addChild( Traversable t ) {

    if( t.getParent() != null ) {
      throw new NotAnOrphanException();
    }

    children.add( t );
    t.setParent( this );
  }

  public void addArgument( Expression expr ) {
    children.add( numParams++, expr );
  }

  public int getNumArguments() {
    return numParams;
  }

  public List<Expression> getParameters() {

    ArrayList<Expression> params = new ArrayList<Expression>( numParams );

    for( int i = 0; i < numParams; i++ ) {
      params.add( getArgument( i ) );
    }

    return params;
  }

  public List<Expression> getVarargs() {

    // The first parameter in the children List is the function name
    ArrayList<Expression> varargs = new ArrayList<Expression>(
      children.size() - numParams - 1 );

    for( int i = numParams; i < children.size()-1; i++ ) {
      varargs.add( getArgument( i ) );
    }

    return varargs;
  }

  public static void defaultPrint( IOCall obj, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    obj.getName().print( stream );
    p.print( " " );
    if( obj.numParams != 0 ) {
      p.print( "( " );
      Iterator<Expression> iter = obj.getParameters().iterator();
      while( iter.hasNext() ) {
        iter.next().print( stream );
        if( iter.hasNext() ) {
          p.print( ", " );
        }
      }
      p.print( " )" );
    }

    Iterator<Expression> iter = obj.getVarargs().iterator();
    while( iter.hasNext() ) {
      iter.next().print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }
}
