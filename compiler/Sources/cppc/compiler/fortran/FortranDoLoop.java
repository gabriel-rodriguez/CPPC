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

import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Statement;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FortranDoLoop extends Statement implements Loop {

  private static Method classPrintMethod;

  static {

    Class[] params = { FortranDoLoop.class, OutputStream.class };

    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  public FortranDoLoop( Identifier label, Identifier loopVar, Expression
    start, Expression stop, Expression step, Statement body ) {

    object_print_method = classPrintMethod;

    children.add( null );
    children.add( null );
    children.add( null );
    children.add( null );
    children.add( null );
    children.add( null );

    this.setLabel( label );
    this.setLoopVar( loopVar );
    this.setStart( start );
    this.setStop( stop );
    this.setStep( step );
    this.setBody( body );
  }

  public static void defaultPrint( FortranDoLoop loop, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "DO " );
    if( loop.getLabel() != null ) {
      p.print( loop.getLabel() + ", " );
    }
    p.print( loop.getLoopVar() + " = " + loop.getStart() + ", " +
      loop.getStop() );

    if( loop.getStep() != null ) {
      p.print( ", " + loop.getStep() );
    }
    p.println("");

    loop.getBody().print( stream );

    //If the loop has a label it has to be ended as: LABEL CONTINUE
    if( loop.getLabel() != null ) {
      p.print( loop.getLabel() + " CONTINUE" );
    } else {
      p.println( "END DO" );
    }
  }

  public Identifier getLabel() {
    return (Identifier)children.get( 0 );
  }

  public void setLabel( Identifier label ) {
    if( this.getLabel() != null ) {
      this.getLabel().setParent( null );
    }

    children.set( 0, label );

    if( label != null ) {
      label.setParent( this );
    }
  }

  public Identifier getLoopVar() {
    return (Identifier)children.get( 1 );
  }

  public void setLoopVar( Identifier id ) {
    if( this.getLoopVar() != null ) {
      this.getLoopVar().setParent( null );
    }

    children.set( 1, id );

    if( id != null ) {
      id.setParent( this );
    }
  }

  public Expression getStart() {
    return (Expression)children.get( 2 );
  }

  public void setStart( Expression start ) {
    if( this.getStart() != null ) {
      this.getStart().setParent( null );
    }

    children.set( 2, start );

    if( start != null ) {
      start.setParent( this );
    }
  }

  public Expression getStop() {
    return (Expression)children.get( 3 );
  }

  public void setStop( Expression stop ) {
    if( this.getStop() != null ) {
      this.getStop().setParent( null );
    }

    children.set( 3, stop );

    if( stop != null ) {
      stop.setParent( this );
    }
  }

  public Expression getStep() {
    return (Expression)children.get( 4 );
  }

  public void setStep( Expression step ) {
    if( this.getStep() != null ) {
      this.getStep().setParent( null );
    }

    children.set( 4, step );

    if( step != null ) {
      step.setParent( this );
    }
  }

  public Statement getBody() {
    return (Statement)children.get( 5 );
  }

  public void setBody( Statement body ) {
    if( this.getBody() != null ) {
      this.getBody().setParent( null );
    }

    if( body == null ) {
      body = new CompoundStatement();
    } else {
      if( !(body instanceof CompoundStatement) ) {
        CompoundStatement cs = new CompoundStatement();
        cs.addStatement( body );
        body = cs;
      }
    }

    children.set( 5, body );
    body.setParent( this );
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }

  public Expression getCondition() {
    return new BinaryExpression( (Identifier)this.getLoopVar().clone(),
      BinaryOperator.COMPARE_LE, (Expression)this.getStop().clone() );
  }
}
