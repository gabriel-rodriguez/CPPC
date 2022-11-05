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




package cppc.compiler.utils;

import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.fortran.FortranArraySpecifier;
import cppc.compiler.fortran.StringSpecifier;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.Iterator;
import java.util.List;

public class FortranVariableSizeAnalizer implements VariableSizeAnalizer {

  protected FortranVariableSizeAnalizer() {}

  public Expression getSize( Identifier id, Traversable reference ) {

    // Find the variable declaration for this identifier
    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        reference, id );
    } catch( Exception e ) {
      return null;
    }

    if( vd instanceof CommonBlock ) {
      vd = ((CommonBlock)vd).getDeclaration( id );
    }

    // Find the declarator for our identifier
    for( int i = 0; i < vd.getNumDeclarators(); i++ ) {

      Declarator declarator = vd.getDeclarator( i );
      Identifier declId = (Identifier)declarator.getSymbol();
      if( declId.equals( id ) ) {

        // Get the ArraySpecifiers for this declarator
        List arraySpecifiers = declarator.getArraySpecifiers();

        if( arraySpecifiers.size() == 0 ) {

          // This may be a StringSpecifier (like CHARACTER*15)
          if( vd.getSpecifiers().get( 0 ) instanceof StringSpecifier ) {
            return ((StringSpecifier)vd.getSpecifiers().get( 0 )).getSize();
          }

          // Simple variable
          return null;
        }

        Iterator specsIter = arraySpecifiers.iterator();
        Expression totalSize = new IntegerLiteral( 1 );
        while( specsIter.hasNext() ) {
          FortranArraySpecifier arraySpec =
            (FortranArraySpecifier)specsIter.next();
          Expression addSizeMinusOne = new BinaryExpression(
            arraySpec.getUpperBound(), BinaryOperator.SUBTRACT,
            arraySpec.getLowerBound() );
          Expression addSize = new BinaryExpression( addSizeMinusOne,
            BinaryOperator.ADD, new IntegerLiteral( 1 ) );
          totalSize = new BinaryExpression( totalSize, BinaryOperator.MULTIPLY,
            addSize );
        }

        return totalSize;
      }
    }

    return null;
  }
}
