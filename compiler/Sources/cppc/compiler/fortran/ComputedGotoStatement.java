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
import cetus.hir.Identifier;
import cetus.hir.Label;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Statement;
import cetus.hir.Traversable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class ComputedGotoStatement extends Statement {

  private static Method classPrintMethod;

  static {
    Class[] params = { ComputedGotoStatement.class, OutputStream.class };
    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  private Expression indexer;

  public ComputedGotoStatement() {
    object_print_method = classPrintMethod;
    this.indexer = null;
  }

  public ComputedGotoStatement( Expression indexer ) {
    object_print_method = classPrintMethod;
    this.indexer = indexer;
  }

  public ComputedGotoStatement( List<Label> labels, Expression indexer ) {
    object_print_method = classPrintMethod;

    for( Label label: labels ) {
      addChild( (Identifier)label.getName().clone() );
    }

    this.indexer = indexer;
  }

  private void addChild( Traversable t ) {
    if( t.getParent() != null ) {
      throw new NotAnOrphanException();
    }

    children.add( t );
    t.setParent( this );
  }

  public static void defaultPrint( ComputedGotoStatement obj,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "GOTO ( " );

    Iterator iter = obj.children.iterator();
    while( iter.hasNext() ) {
      Identifier label = (Identifier)iter.next();
      label.print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }

    p.print(" ) ");
    obj.indexer.print( stream );
  }

  public List getLabels() {
    return children;
  }

  public Expression getIndexer() {
    return indexer;
  }

  public void setIndexer( Expression indexer ) {
    this.indexer = indexer;
  }

  public void addLabel( Identifier label ) {
    addChild( label );
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }
}
