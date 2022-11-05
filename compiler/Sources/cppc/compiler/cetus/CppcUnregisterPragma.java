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

import cetus.hir.Identifier;

import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CppcUnregisterPragma extends CppcPragmaStatement {

  private List<Identifier> unregisters;

  public CppcUnregisterPragma() {
    super();
    unregisters = new ArrayList<Identifier>();
    registerPrintMethod();
  }

  public CppcUnregisterPragma( List<Identifier> unregisters ) {
    super();
    this.unregisters = unregisters;
    registerPrintMethod();
  }

  private void registerPrintMethod() {

    Class[] params = { CppcUnregisterPragma.class, OutputStream.class };
    try {
      setPrintMethod( CppcUnregisterPragma.class.getMethod(
        "classPrintMethod", params ) );
    } catch( NoSuchMethodException e ) {
      System.err.println( "BUG: NoSuchMethodException raised in " +
        CppcUnregisterPragma.class + ". Method: registerPrintMethod()" );
      System.exit( 0 );
    }
  }

  public List<Identifier> getUnregisters() {
    return unregisters;
  }

  public void addUnregister( Identifier id ) {

    if( !unregisters.contains( id ) ) {
      unregisters.add( id );
    }
  }

  public static void classPrintMethod( CppcUnregisterPragma statement,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "#pragma " );

    for( int i = 0;
      i < GlobalNamesFactory.getGlobalNames().UNREGISTER_PRAGMA().length;
      i++ ) {

      p.print( GlobalNamesFactory.getGlobalNames().UNREGISTER_PRAGMA()[i] +
        " " );
    }

    p.print( "( " );

    Iterator<Identifier> iter = statement.unregisters.iterator();

    while( iter.hasNext() ) {
      p.print( iter.next() );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }

    p.print( " )" );
  }
}