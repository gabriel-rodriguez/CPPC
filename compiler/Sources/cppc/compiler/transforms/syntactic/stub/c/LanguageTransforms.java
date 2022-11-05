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

import cetus.hir.Procedure;
import cetus.hir.Program;

public class LanguageTransforms extends
  cppc.compiler.transforms.syntactic.skel.LanguageTransforms {

  private LanguageTransforms( Program program ) {
    super( program );
  }

  protected void walkOverProcedure( Procedure procedure ) {}

  public static final LanguageTransforms getTransformInstance(
    Program program ) {

    return new LanguageTransforms( program );
  }
}
