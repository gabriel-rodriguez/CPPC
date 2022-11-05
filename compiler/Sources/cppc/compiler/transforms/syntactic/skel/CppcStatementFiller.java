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




package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;

import cppc.compiler.analysis.ProcedureAnalyzer;
import cppc.compiler.transforms.shared.ProcedureWalker;

public class CppcStatementFiller extends ProcedureWalker {

  private static final String passName = "[CppcStatementFiller]";

  private CppcStatementFiller( Program program ) {
    super( program );
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 2 );

    CppcStatementFiller transform = new CppcStatementFiller( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 2 );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    ProcedureAnalyzer.analyzeProcedure( procedure );
  }
}