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




package cppc.compiler.transforms.shared;

public class ProcedureParameter {

  public static final ProcedureParameter VARARGS = new ProcedureParameter( -1 );

  private int position;

  public ProcedureParameter( int position ) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  public boolean equals( Object obj ) {

    if( !(obj instanceof ProcedureParameter) ) {
      return false;
    }

    return( this.position == ((ProcedureParameter)obj).position );
  }

  public int hashCode() {
    return new Integer( position ).hashCode();
  }

  public String toString() {
    return( "ProcedureParameter in position: " + position );
  }
}