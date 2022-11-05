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
import cetus.hir.Identifier;
import cetus.hir.Statement;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;


public class CppcExecuteTranslator
  extends TranslationModule<CppcExecutePragma> {

  public CppcExecuteTranslator() {
    super();
  }

  public Class getTargetClass() {
    return CppcExecutePragma.class;
  }

  public void translate( CppcExecutePragma pragma ) {
    // Add jump label to the beginning of the execute block
    CppcLabel jumpLabel = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().EXECUTE_LABEL() ) );
    Statement begin = pragma.getBegin();
    CompoundStatement beginList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass( pragma, CompoundStatement.class );
    beginList.addStatementBefore( begin, jumpLabel );

    // Add conditional jump mark after the end statement
    CppcConditionalJump jump = new CppcConditionalJump();
    Statement end = pragma.getEnd();
    CompoundStatement endList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass( end, CompoundStatement.class );
    endList.addStatementAfter( end, jump );

    // Detach pragma statement
    pragma.detach();
  }
}
