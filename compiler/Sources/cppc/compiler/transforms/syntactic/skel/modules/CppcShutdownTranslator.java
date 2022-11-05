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




package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;

import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

public class CppcShutdownTranslator
  extends TranslationModule<CppcShutdownPragma> {

  public CppcShutdownTranslator() {
    super();
  }

  public void translate( CppcShutdownPragma pragma ) {
    // Get CPPC_Shutdown function Identifier and build the function call
    // expression
    Identifier cppcShutdownIdentifier = new Identifier(
      GlobalNamesFactory.getGlobalNames().SHUTDOWN_FUNCTION() );
    FunctionCall functionCall = new FunctionCall( cppcShutdownIdentifier );

    // Build the statement for replacing the pragma and replace it
    ExpressionStatement functionCallStatement = new ExpressionStatement(
      functionCall );
    pragma.swapWith( functionCallStatement );
  }

  public Class getTargetClass() {
    return CppcShutdownPragma.class;
  }
}
