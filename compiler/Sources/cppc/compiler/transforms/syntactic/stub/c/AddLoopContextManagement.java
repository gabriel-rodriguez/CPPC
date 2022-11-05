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

import cetus.hir.AssignmentExpression;
import cetus.hir.ExpressionStatement;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.Program;
import cetus.hir.Statement;

import cppc.compiler.utils.ObjectAnalizer;

public class AddLoopContextManagement extends
  cppc.compiler.transforms.syntactic.skel.AddLoopContextManagement {

  private AddLoopContextManagement( Program program ) {
    super( program );
  }

  public static final AddLoopContextManagement getTransformInstance(
    Program program ) {

    return new AddLoopContextManagement( program );
  }

  protected Statement testInsideLoop( Statement ref ) {

    ForLoop loop = (ForLoop)ObjectAnalizer.getParentOfClass( ref,
      ForLoop.class );

    return loop;
  }

  protected Identifier getLoopIndex( Statement loop ) {

    // Downcast to a DoLoop
    ForLoop forLoop = (ForLoop)loop;

    // Get init, condition and step
    Statement initStmt = forLoop.getInitialStatement();

    // Assume initializer is of the type VAR=VALUE
    AssignmentExpression assignment = (AssignmentExpression)
      ((ExpressionStatement)initStmt).getExpression();
    Identifier lhs = (Identifier)assignment.getLHS();

    // Return the identifier
    return lhs;
  }
}
