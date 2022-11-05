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
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;

import java.io.OutputStream;
import java.io.PrintStream;

public class FortranArraySpecifier extends Specifier {

  private Expression upperBound;
  private Expression lowerBound;

  public FortranArraySpecifier( Expression upperBound ) {
    // Cetus-0.5: constructor with int is private. -1 gets assigned
    // using the non-parameterized one.
    super();

    lowerBound = null;
    this.upperBound = upperBound;
  }

  public FortranArraySpecifier( Expression lowerBound,
    Expression upperBound ) {

    // Cetus-0.5: constructor with int is private. -1 gets assigned
    // using the non-parameterized one.
    super();

    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  public Expression getLowerBound() {

    if( lowerBound != null ) {
      return lowerBound;
    } else {
      return new IntegerLiteral( 1 );
    }
  }

  public Expression getUpperBound() {
    return upperBound;
  }

  public void print( OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    if( lowerBound != null ) {
      p.print( lowerBound );
      p.print( ":" );
    }

    p.print( upperBound );
  }

  public void setLowerBound( Expression lowerBound ) {
    this.lowerBound = lowerBound;
  }

  public void setUpperBound( Expression upperBound ) {
    this.upperBound = upperBound;
  }
}