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

import cetus.hir.Declaration;
import cetus.hir.Identifier;
import cetus.hir.Specifier;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ImplicitRegister extends Declaration {

  private static Method classPrintMethod;

  static {
    Class[] params = { ImplicitRegister.class, OutputStream.class };
    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  public static final ImplicitRegister IMPLICIT_NONE = new ImplicitRegister(
    (char)-1, (char)-1, (Specifier)null );

  private char beginChar;
  private char endChar;
  private Specifier specifier;

  private ImplicitRegister() {}

  public ImplicitRegister( char beginChar, char endChar, Specifier specifier ) {
    object_print_method = classPrintMethod;

    if( beginChar > endChar ) {
      throw new IllegalArgumentException();
    }

    this.beginChar = beginChar;
    this.endChar = endChar;
    this.specifier = specifier;
  }

  public boolean collides( ImplicitRegister rhs ) {
    if( rhs.endChar < beginChar ) { return false; }
    if( rhs.beginChar > endChar ) { return false; }

    return true;
  }

  public boolean overlaps( ImplicitRegister rhs ) {
    return( ( beginChar <= rhs.beginChar ) && ( endChar >= rhs.endChar ) );
  }

  public boolean contains( Identifier id ) {
    char firstChar = id.toString().charAt( 0 );
    return( ( firstChar >= beginChar ) && ( firstChar <= endChar ) );
  }

  public Specifier getSpecifier() {
    return specifier;
  }

  public char getBeginChar() {
    return beginChar;
  }

  public char getEndChar() {
    return endChar;
  }

  public List getDeclaredSymbols() {
    return new ArrayList();
  }

  public static void defaultPrint( ImplicitRegister obj, OutputStream stream ) {
    PrintStream p = new PrintStream( stream );

    if( obj == ImplicitRegister.IMPLICIT_NONE ) {
      p.print( "IMPLICIT NONE" );
    } else {
      p.print( "IMPLICIT " + obj.specifier.toString() + " ( " + obj.beginChar +
        "-" + obj.endChar + " )" );
    }
  }

  public String toString() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    print( stream );
    return stream.toString();
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }
}
