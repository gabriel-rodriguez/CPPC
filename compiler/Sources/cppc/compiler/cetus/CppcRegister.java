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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class CppcRegister {

  private Identifier name;
  private Expression size;

  private CppcRegister() {}

  public CppcRegister( Identifier name ) {
    this.name = name;
    this.size = null;
  }

  public CppcRegister( Identifier name, Expression size ) {
    this.name = name;
    this.size = size;
  }

  public Identifier getName() {
    return name;
  }

  public Expression getSize() {
    return size;
  }

  public void setSize( Expression size ) {
    this.size = size;
  }

  public void print( OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    name.print( stream );
    if( size != null ) {
      p.print( "[ " );
      size.print( stream );
      p.print( " ]" );
    }
  }

  public String toString() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    print( stream );
    return stream.toString();
  }

  public boolean equals( Object obj ) {

    if( !(obj instanceof CppcRegister) ) {
      return false;
    }

    CppcRegister rhs = (CppcRegister)obj;

    return( name.equals( rhs.name ) &&
      size.equals( rhs.size ) );
  }
}