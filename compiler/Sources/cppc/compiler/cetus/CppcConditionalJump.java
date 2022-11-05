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

import java.io.OutputStream;
import java.io.PrintStream;

public class CppcConditionalJump extends CppcPragmaStatement {

  private int leap;

  public CppcConditionalJump() {
    super();
    registerPrintMethod();

    this.leap = 1;
  }

  public int getLeap() {
    return this.leap;
  }

  public void setLeap( int leap ) {
    this.leap = leap;
  }

  private void registerPrintMethod() {
    Class[] params = { CppcConditionalJump.class, OutputStream.class };
    try {
      setPrintMethod( CppcConditionalJump.class.getMethod( "classPrintMethod",
        params ) );
    } catch( NoSuchMethodException e ) {
      System.err.println( "BUG: NoSuchMethodException raised in " +
        CppcConditionalJump.class + ". Method: registerPrintMethod()" );
      System.exit( 0 );
    }
  }

  public static void classPrintMethod( CppcConditionalJump statement,
    OutputStream stream ) {}
}
