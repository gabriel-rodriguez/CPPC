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

import cetus.hir.FloatLiteral;

import java.io.OutputStream;
import java.lang.reflect.Method;

public class DoubleLiteral extends FloatLiteral {

  private static Method class_print_method;

  static {
    Class [] params = new Class[2];

    try {
      params[0] = FloatLiteral.class;
      params[1] = OutputStream.class;
      class_print_method = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      throw new InternalError();
    }
  }

  // Just a subclass to mark that we need double precision when writing this
  public DoubleLiteral( double value ) {
    super( value );
    object_print_method = class_print_method;
  }

  public static void setClassPrintMethod( Method m ) {
    class_print_method = m;
  }

}