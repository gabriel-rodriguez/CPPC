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




package cppc.compiler.transforms.syntactic.stub.c.modules;

import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;



public class CppcUnregisterTranslator extends
  cppc.compiler.transforms.syntactic.skel.modules.CppcUnregisterTranslator {

  public CppcUnregisterTranslator() {
    super();
  }

  protected Expression getReference( Declarator declarator ) {

//     if( (declarator.getPointerSpecifiers() == null) &&
    if( (declarator.getSpecifiers().size() == 0) &&
      (declarator.getArraySpecifiers().size() == 0) ) {

      return new UnaryExpression( UnaryOperator.ADDRESS_OF,
        (Identifier)declarator.getSymbol().clone() );
    } else {
      return (Identifier)declarator.getSymbol().clone();
    }
  }
}
