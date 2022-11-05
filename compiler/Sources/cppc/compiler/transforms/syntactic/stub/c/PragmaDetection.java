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




package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.Annotation;
import cetus.hir.Program;

import cppc.compiler.transforms.syntactic.skel.modules.CppcCheckpointTranslator;
import cppc.compiler.transforms.syntactic.skel.modules.CppcExecuteTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.
  CppcNonportableMarkTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.
  CppcNonportableFunctionMarkTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.CppcRegisterTranslator;
import cppc.compiler.transforms.syntactic.skel.modules.CppcShutdownTranslator;
import cppc.compiler.transforms.syntactic.stub.c.modules.
  CppcUnregisterTranslator ;
// import cppc.compiler.utils.language.LanguageAnalyzerFactory;

public class PragmaDetection extends
  cppc.compiler.transforms.syntactic.skel.PragmaDetection {

  private PragmaDetection( Program program ) {
    super( program );
  }

  protected void registerAllModules() {
    super.registerModule( new CppcCheckpointTranslator() );
    super.registerModule( new CppcExecuteTranslator() );
    super.registerModule( new CppcNonportableMarkTranslator() );
    super.registerModule( new CppcNonportableFunctionMarkTranslator() );
    super.registerModule( new CppcRegisterTranslator() );
    super.registerModule( new CppcShutdownTranslator() );
    super.registerModule( new CppcUnregisterTranslator() );
  }

  public static final PragmaDetection getTransformInstance(
    Program program ) {
      return new PragmaDetection( program );
  }
}
