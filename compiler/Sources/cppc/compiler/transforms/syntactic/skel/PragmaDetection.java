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

import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.NoSuchElementException;

public abstract class PragmaDetection extends ProcedureWalker {

  private static String passName = "[PragmaDetection]";
  private static HashMap<Class<? extends CppcPragmaStatement>,
    TranslationModule<? extends CppcPragmaStatement>> translators =
    new HashMap<Class<? extends CppcPragmaStatement>,
      TranslationModule<? extends CppcPragmaStatement>>();

  protected PragmaDetection( Program program ) {
    super( program );
    registerAllModules();
  }

  protected abstract void registerAllModules();

  private static final PragmaDetection getTransformInstance( Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.PRAGMA_DETECTION_CLASS_OPTION );
    try {
      Class theClass = Class.forName( className );
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      PragmaDetection theInstance = (PragmaDetection)instancer.invoke( null,
        program );
      return theInstance;
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }

    return null;
  }

  protected static void registerModule(
    TranslationModule<? extends CppcPragmaStatement> module ) {

    translators.put( module.getTargetClass(), module );
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    PragmaDetection transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    DepthFirstIterator iter = new DepthFirstIterator( procedure );
    iter.pruneOn( Expression.class );

    while( iter.hasNext() ) {
      try {
        CppcPragmaStatement pragma = (CppcPragmaStatement)iter.next(
          CppcPragmaStatement.class );
        TranslationModule<? extends CppcPragmaStatement> module =
          translators.get( pragma.getClass() );

        if( module != null ) {
          try {
            module.getMethod().invoke( module, pragma );
          } catch( Exception e ) {
            throw new InternalError( e.getMessage() );
          }
        }
      } catch( NoSuchElementException e ) {}
    }
  }
}
