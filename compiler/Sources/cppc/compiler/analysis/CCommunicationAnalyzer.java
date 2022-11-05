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

import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.SwitchStatement;

import cppc.compiler.cetus.CppcStatement;

public class CCommunicationAnalyzer extends CommunicationAnalyzer {

  CCommunicationAnalyzer() {}

  protected void analyzeStatement( ForLoop forLoop ) {
    this.analyzeStatement( (Loop)forLoop );
  }

  protected void analyzeStatement( SwitchStatement stmt ) {
    // Analyze body (direct access to children since this method returns a
    // compound statement, and we transformed it into a cppc statement.
    // Otherwise it would raise a class cast exception)
    this.analyzeStatement( (CppcStatement)stmt.getChildren().get( 1 ) );
  }
}
