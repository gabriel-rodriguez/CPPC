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
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MultiExpression extends Expression {

  private static Method class_print_method;

  static {
    Class [] params = { MultiExpression.class, OutputStream.class };

    try {
      class_print_method = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      throw new InternalError();
    }
  }

  private Identifier var;
  Map<Expression,Expression> values;

  public MultiExpression( Identifier var ) {
    object_print_method = class_print_method;
    this.var = var;
    values = new TreeMap<Expression,Expression>();
  }

  public MultiExpression( Identifier var, Map<Expression, Expression> values ) {
    object_print_method = class_print_method;
    this.var = var;
    values = new TreeMap<Expression,Expression>();

    for( Expression key: values.keySet() ) {
      this.values.put( key, values.get( key ) );
      this.addChildren( values.get( key ) );
    }
  }

  private void addChildren( Traversable t ) {
    if( t.getParent() != null ) {
      throw new IllegalArgumentException();
    }

    children.add( t );
    t.setParent( this );
  }

  public void setChild( int index, Traversable t ) {
    if( t.getParent() != null ) {
      throw new NotAnOrphanException();
    }

    if( t instanceof Expression ) {
      Expression value = (Expression)children.get( index );
      for( Expression key: values.keySet() ) {
        if( values.get( key ) == value ) {
          values.put( key, (Expression)t );
          break;
        }
      }
      children.set( index, t );
      t.setParent( this );
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void addExpression( Expression key, Expression value ) {
    if( this.values.containsKey( key ) ) {
      Expression old = this.values.get( key );
      this.children.remove( old );
    }

    this.values.put( key, value );
    this.addChildren( value );
  }

  public Identifier getVar() {
    return this.var;
  }

  public Expression getValue( Expression key ) {
    if( values.containsKey( key ) ) {
      return values.get( key );
    }

    return null;
  }

  public Set<Expression> getValueSet() {
    return values.keySet();
  }

  public Expression getKeyOf( Expression value ) {
    if( values.containsValue( value ) ) {
      for( Expression key: values.keySet() ) {
        if( values.get( key ) == value ) {
          return key;
        }
      }
    }

    return null;
  }

  public static void defaultPrint( MultiExpression mexpr,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );
    p.print( "[[" );
    for( int i = 0; i < mexpr.children.size()-1; i++ ) {
      ((Expression)mexpr.children.get(i)).print( stream );
      p.print( ", " );
    }

    if( mexpr.children.size() > 0 ) {
      ((Expression)mexpr.children.get( mexpr.children.size()-1 )).print(
        stream );
    }
    p.print( "]]" );
  }

  public static void setClassPrintMethod( Method m ) {
    class_print_method = m;
  }

  public Object clone() {
    MultiExpression clone = new MultiExpression( (Identifier)var.clone() );

    for( Expression key: values.keySet() ) {
      clone.addExpression( key, (Expression)values.get( key ).clone() );
    }

    return clone;
  }
}