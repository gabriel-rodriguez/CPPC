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




package cppc.compiler.analysis;

import cetus.hir.ArraySpecifier;
import cetus.hir.Identifier;

import java.util.Set;

public class CSpecifierAnalyzer extends SpecifierAnalyzer {

  CSpecifierAnalyzer() {}

  protected Set<Identifier> analyzeSpecifier( ArraySpecifier spec ) {
    Set<Identifier> ret = ExpressionAnalyzer.analyzeExpression(
      currentStatement, spec.getDimension( 0 ) );

    for( int i = 1; i < spec.getNumDimensions(); i++ ) {
      ret.addAll( ExpressionAnalyzer.analyzeExpression( currentStatement,
        spec.getDimension( i ) ) );
    }

    return ret;
  }
}
