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

import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CppcRegisterPragma extends CppcPragmaStatement {

  private List<CppcRegister> registers;

  public CppcRegisterPragma() {
    super();
    registers = new ArrayList<CppcRegister>();
    registerPrintMethod();
  }

  public CppcRegisterPragma( List<CppcRegister> registers ) {
    super();
    this.registers = registers;
    registerPrintMethod();
  }

  private void registerPrintMethod() {
    Class[] params = { CppcRegisterPragma.class, OutputStream.class };
    try {
      setPrintMethod( CppcRegisterPragma.class.getMethod( "classPrintMethod",
        params ) );
    } catch( NoSuchMethodException e ) {
      System.err.println( "BUG: NoSuchMethodException raised in " +
        CppcRegisterPragma.class + ". Method: registerPrintMethod()" );
      System.exit( 0 );
    }
  }


  public List<CppcRegister> getRegisters() {
    return registers;
  }

  public void addRegister( CppcRegister register ) {
    if( !registers.contains( register ) ) {
      registers.add( register );
    }
  }

  public static void classPrintMethod( CppcRegisterPragma statement,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "#pragma " );

    for( int i = 0;
      i < GlobalNamesFactory.getGlobalNames().REGISTER_PRAGMA().length;
      i++ ) {

      p.print( GlobalNamesFactory.getGlobalNames().REGISTER_PRAGMA()[i] + " " );
    }

    p.print( "( " );

    Iterator<CppcRegister> iter = statement.registers.iterator();
    while( iter.hasNext() ) {

      CppcRegister register = iter.next();
      register.print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }

    p.print( " )" );
  }

}