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

import cetus.hir.CompoundStatement;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

public class CppcCheckpointTranslator extends
  TranslationModule<CppcCheckpointPragma> {

  public CppcCheckpointTranslator() {
    super();
  }

  public void translate( CppcCheckpointPragma pragma ) {
    // Add jump label to the beginning of the checkpoint block
    CppcLabel jumpLabel = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().CHECKPOINT_LABEL() ) );
    pragma.swapWith( jumpLabel );

    // Create the function call statement
    FunctionCall cppcCheckpointCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().CHECKPOINT_FUNCTION() ) );
    cppcCheckpointCall.addArgument( new IntegerLiteral(
      GlobalNamesFactory.getGlobalNames().CURRENT_CHKPT_CODE() ) );
    ExpressionStatement cppcCheckpointCallStatement = new ExpressionStatement(
      cppcCheckpointCall );

    // Insert the expression statement
    CompoundStatement statementList = (CompoundStatement)jumpLabel.getParent();
    statementList.addStatementAfter( jumpLabel, cppcCheckpointCallStatement );

    // Put the conditional jump mark after the call statement
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter( cppcCheckpointCallStatement, jump );
  }

  public Class getTargetClass() {
    return CppcCheckpointPragma.class;
  }
}
