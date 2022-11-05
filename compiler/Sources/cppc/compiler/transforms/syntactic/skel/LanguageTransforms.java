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

import cetus.hir.Program;
import cetus.hir.Tools;

import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.ConfigurationManager;

import java.lang.reflect.Method;

public abstract class LanguageTransforms extends ProcedureWalker {

  private static String passName = "[LanguageTransforms]";

  protected LanguageTransforms( Program program ) {
    super( program );
  }

  private static final LanguageTransforms getTransformInstance(
    Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.LANGUAGE_TRANSFORMS_CLASS_OPTION );
    try {
      Class theClass = Class.forName( className );
      Class [] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      LanguageTransforms theInstance =
        (LanguageTransforms)instancer.invoke(null, program );
      return theInstance;
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }

    return null;
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    LanguageTransforms transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

}
