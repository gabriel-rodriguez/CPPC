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

import cetus.hir.Statement;

import java.io.OutputStream;
import java.io.PrintStream;

public class CppcExecutePragma extends CppcPragmaStatement {

  private Statement begin;
  private Statement end;

  public CppcExecutePragma( Statement begin, Statement end) {
    super();
    registerPrintMethod();

    this.begin = begin;
    this.end = end;
  }

  public Statement getBegin() {
    return this.begin;
  }

  public Statement getEnd() {
    return this.end;
  }

  private void registerPrintMethod() {
    Class[] params = { CppcExecutePragma.class, OutputStream.class };
    try {
      setPrintMethod( CppcExecutePragma.class.getMethod( "classPrintMethod",
        params ) );
    } catch( NoSuchMethodException e ) {
      System.err.println( "BUG: NoSuchMethodException raised in " +
        CppcExecutePragma.class + ". Method: registerPrintMethod()" );
      System.exit( 0 );
    }
  }


  public static void classPrintMethod( CppcExecutePragma statement,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "#pragma cppc execute" );
  }
}
