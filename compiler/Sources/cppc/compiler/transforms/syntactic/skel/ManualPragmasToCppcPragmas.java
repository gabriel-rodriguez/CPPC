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

import cetus.hir.Annotation;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.TranslationUnit;

import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.util.NoSuchElementException;

public class ManualPragmasToCppcPragmas extends ProcedureWalker {

  private static String passName = "[ManualPragmasToCppcPragmas]";

  private ManualPragmasToCppcPragmas( Program program ) {
    super( program );
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    // Check if this pass is to be performed
    if( !ConfigurationManager.hasOption(
      ConfigurationManager.MANUAL_PRAGMAS_OPTION ) ) {

      return;
    }

    ManualPragmasToCppcPragmas transform = new ManualPragmasToCppcPragmas(
      program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    GlobalNames names = GlobalNamesFactory.getGlobalNames();
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    DepthFirstIterator iter = new DepthFirstIterator( procedure );
    iter.pruneOn( Expression.class );

    while( iter.hasNext() ) {
      try {
        Annotation annote = (Annotation)iter.next( Annotation.class );
        if( analyzer.annotationIsPragma( annote ) ) {
          String pragmaText = analyzer.getPragmaText( annote );

          // Execute pragma
          if( ObjectAnalizer.matchStringWithArray( pragmaText,
            names.BEGIN_EXECUTE_PRAGMA() ) ) {

            this.processBeginExecute( annote );
          }

          // Checkpoint pragma
          if( ObjectAnalizer.matchStringWithArray( pragmaText,
            names.CHECKPOINT_PRAGMA() ) ) {

            this.processCheckpoint( annote );
          }

          if( ObjectAnalizer.matchStringWithArray( pragmaText,
            names.CHECKPOINT_LOOP_PRAGMA() ) ) {

            this.processCheckpointLoop( annote );
          }
        }
      } catch( NoSuchElementException e ) {}
    }
  }

  private void processBeginExecute( Annotation annote ) {
    GlobalNames names = GlobalNamesFactory.getGlobalNames();
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();

    // Get procedure body
    CompoundStatement body =
      ((Statement)annote.getParent()).getProcedure().getBody();

    // Position iterator over current annote
    DepthFirstIterator iter = new DepthFirstIterator( body );
    iter.pruneOn( Expression.class );

    Annotation match = null;
    while( iter.hasNext() && (match != annote) ) {
      try {
        match = (Annotation)iter.next( Annotation.class );
      } catch( NoSuchElementException e ) {
        System.err.println( "BUG: cannot find current annote. In " +
          "ManualPragmasToCppcPragmas.processBeginExecute()" );
        System.exit( 0 );
      }
    }

    Statement executeBegin = null;
    try {
      executeBegin = (Statement)iter.next( Statement.class );
    } catch( NoSuchElementException e ) {
      System.err.println( "BUG: cannot find begin statement. In " +
        "ManualPragmasToCppcPragmas.processBeginExecute()" );
      System.exit( 0 );
    }

    // Find end execute annote
    Statement executeEnd = executeBegin;
    while( iter.hasNext() ) {
      try {
        Statement next = (Statement)iter.next( Statement.class );
        if( next instanceof DeclarationStatement ) {
          Declaration declaration =
            ((DeclarationStatement)next).getDeclaration();
          if( declaration instanceof Annotation ) {
            Annotation nextAnnote = (Annotation)declaration;
            if( analyzer.annotationIsPragma( nextAnnote ) ) {
              String pragmaText = analyzer.getPragmaText( nextAnnote );
              if( ObjectAnalizer.matchStringWithArray( pragmaText,
                names.END_EXECUTE_PRAGMA() ) ) {

                // Build CppcExecutePragma
                CppcExecutePragma pragma = new CppcExecutePragma( executeBegin,
                  executeEnd );

                // Swap begin annote with pragma
                Statement annoteParent = (Statement)annote.getParent();
                annoteParent.swapWith( pragma );

                // Detach end annote
                next.detach();

                return;
              }
            }
          }
        }

        // if this was not our match, continue processing
        executeEnd = next;
      } catch( NoSuchElementException e ) {
        Statement annoteParent = (Statement)annote.getParent();
        System.err.println( "warning: match not found for CPPC EXECUTE ON " +
          "RESTART manually placed on line " + annoteParent.where() +
          ". Ignoring directive." );
      }
    }
  }

  private void processCheckpoint( Annotation annote ) {
    // Build checkpoint pragma
    CppcCheckpointPragma pragma = new CppcCheckpointPragma();

    // Swap annote with pragma
    Statement annoteParent = (Statement)annote.getParent();
    annoteParent.swapWith( pragma );
  }

  private void processCheckpointLoop( Annotation annote ) {
    // Build checkpoint loop pragma
    CppcCheckpointLoopPragma pragma = new CppcCheckpointLoopPragma();

    // Get parent compound statement
    Statement annoteParent = (Statement)annote.getParent();
    CompoundStatement statementList = (CompoundStatement)
      annoteParent.getParent(); 

    // Find next children
    int index = statementList.getChildren().indexOf( annoteParent );
    Statement loop = (Statement)statementList.getChildren().get( index + 1 );

    // If it is not a loop: error
    if( !(loop instanceof Loop) ) {
      TranslationUnit tunit = (TranslationUnit)loop.getProcedure().getParent();
      System.err.println( tunit.getInputFilename() + ": " +
        annoteParent.where() + ": " +
        "checkpoint loop directive not before loop." );
      System.exit( 0 );
    }

    // Insert pragma at loop body beginning
    Loop safeLoop = (Loop)loop;
    Statement loopBody = safeLoop.getBody();

    if( !(loopBody instanceof CompoundStatement) ) {
      CompoundStatement newBody = new CompoundStatement();
      newBody.addStatement( loopBody );
      loopBody.swapWith( newBody );
      loopBody = newBody;
    }

    ((CompoundStatement)loopBody).getChildren().add( 0, pragma );
    pragma.setParent( loopBody );
    pragma.setLineNumber( loopBody.where() );

    // Detach annote
    annoteParent.detach();
  }
}
