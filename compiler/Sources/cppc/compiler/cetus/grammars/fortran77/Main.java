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




package cppc.compiler.cetus.grammars.fortran77;

import antlr.LexerSharedInputState;

import cetus.exec.Driver;
import cetus.hir.Program;
import cetus.hir.TranslationUnit;

import cppc.compiler.fortran.FortranPrintManager;
import cppc.compiler.transforms.semantic.skel.AddCppcExecutePragmas;
import cppc.compiler.transforms.semantic.skel.AddCppcInitPragma;
import cppc.compiler.transforms.semantic.skel.AddOpenFilesControl;
import cppc.compiler.transforms.semantic.skel.CommunicationMatching;
import cppc.compiler.transforms.syntactic.skel.AddCheckpointPragmas;
import cppc.compiler.transforms.syntactic.skel.AddCppcShutdownPragma;
import cppc.compiler.transforms.syntactic.skel.AddExitLabels;
import cppc.compiler.transforms.syntactic.skel.AddLoopContextManagement;
import cppc.compiler.transforms.syntactic.skel.AddRestartJumps;
import cppc.compiler.transforms.syntactic.skel.CheckCheckpointedProcedures;
import cppc.compiler.transforms.syntactic.skel.CheckPragmedProcedures;
import cppc.compiler.transforms.syntactic.skel.CppcDependenciesAnalizer;
import cppc.compiler.transforms.syntactic.skel.CppcStatementFiller;
import cppc.compiler.transforms.syntactic.skel.CppcStatementsToStatements;
import cppc.compiler.transforms.syntactic.skel.EnterPragmedProcedures;
import cppc.compiler.transforms.syntactic.skel.LanguageTransforms;
import cppc.compiler.transforms.syntactic.skel.ManualPragmasToCppcPragmas;
import cppc.compiler.transforms.syntactic.skel.PragmaDetection;
import cppc.compiler.transforms.syntactic.skel.StatementsToCppcStatements;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Iterator;

public class Main extends cetus.exec.Driver {
  private static final String FORTRAN_ADDCPPCINITPRAGMA_CLASSNAME =
    "cppc.compiler.transforms.semantic.stub.fortran77.AddCppcInitPragma";
  private static final String FORTRAN_ADDLOOPCONTEXTMANAGEMENT_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.fortran77.AddLoopContextManagement";
  private static final String FORTRAN_ADDFILESCONTROL_CLASSNAME =
    "cppc.compiler.transforms.semantic.stub.fortran77.AddOpenFilesControl";
  private static final String FORTRAN_ADDRESTARTJUMPS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.fortran77.AddRestartJumps";
  private static final String FORTRAN_CODEOPTIMIZATIONS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.fortran77.CodeOptimizations";
  private static final String FORTRAN_COMMUNICATIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranCommunicationAnalyzer";
  private static final String FORTRAN_EXPRESSIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranExpressionAnalyzer";
  private static final String FORTRAN_GLOBALNAMES_CLASSNAME =
    "cppc.compiler.utils.globalnames.FortranGlobalNames";
  private static final String FORTRAN_LANGUAGEANALYZER_CLASSNAME =
    "cppc.compiler.utils.language.FortranLanguageAnalyzer";
  private static final String FORTRAN_LANGUAGETRANSFORMS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.fortran77.LanguageTransforms";
  private static final String FORTRAN_PRAGMADETECTION_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.fortran77.PragmaDetection";
  private static final String FORTRAN_SPECIFIERANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranSpecifierAnalyzer";
  private static final String FORTRAN_STATEMENTANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranStatementAnalyzer";
  private static final String FORTRAN_SYMBOLICANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranSymbolicAnalyzer";
  private static final String FORTRAN_SYMBOLICEXPRESSIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.FortranSymbolicExpressionAnalyzer";
  private static final String FORTRAN_VARIABLESIZEANALYZER_CLASSNAME =
    "cppc.compiler.utils.FortranVariableSizeAnalizer";

  static {
    FortranPrintManager.configureClasses();
    ConfigurationManager.setOption(
      GlobalNames.ADD_CPPC_INIT_PRAGMA_CLASS_OPTION,
      FORTRAN_ADDCPPCINITPRAGMA_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.ADD_LOOP_CONTEXT_MANAGEMENT_CLASS_OPTION,
      FORTRAN_ADDLOOPCONTEXTMANAGEMENT_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.ADD_OPEN_FILES_CONTROL_CLASS_OPTION,
      FORTRAN_ADDFILESCONTROL_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.ADD_RESTART_JUMPS_CLASS_OPTION,
      FORTRAN_ADDRESTARTJUMPS_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.CLASS_OPTION,
      FORTRAN_GLOBALNAMES_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.CODE_OPTIMIZATIONS_CLASS_OPTION,
      FORTRAN_CODEOPTIMIZATIONS_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.COMMUNICATION_ANALYZER_CLASS_OPTION,
      FORTRAN_COMMUNICATIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.EXPRESSION_ANALYZER_CLASS_OPTION,
      FORTRAN_EXPRESSIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.LANGUAGE_ANALYZER_CLASS_OPTION,
      FORTRAN_LANGUAGEANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.LANGUAGE_TRANSFORMS_CLASS_OPTION,
      FORTRAN_LANGUAGETRANSFORMS_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.PRAGMA_DETECTION_CLASS_OPTION,
      FORTRAN_PRAGMADETECTION_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.SPECIFIER_ANALYZER_CLASS_OPTION,
      FORTRAN_SPECIFIERANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.STATEMENT_ANALYZER_CLASS_OPTION,
      FORTRAN_STATEMENTANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.SYMBOLIC_ANALYZER_CLASS_OPTION,
      FORTRAN_SYMBOLICANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.SYMBOLIC_EXPRESSION_ANALYZER_CLASS_OPTION,
      FORTRAN_SYMBOLICEXPRESSIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.VARIABLE_SIZE_ANALYZER_CLASS_OPTION,
      FORTRAN_VARIABLESIZEANALYZER_CLASSNAME );
  }

  public void run( String[] args ) {

    args = ConfigurationManager.parseCommandLine( args );
    parseCommandLine( args );
    String[] fileNames = args;

    try {
      Program program = new Program( Arrays.asList( fileNames ) );


      Iterator iter = program.getChildren().iterator();
      while( iter.hasNext() ) {
        TranslationUnit tunit = (TranslationUnit)iter.next();
        File file = new File( tunit.getInputFilename() );
        LexerSharedInputState lexerState = new LexerSharedInputState(
          new FileReader( file ) );
        FLexer lexer = new FLexer( lexerState );
        PreFParser preParser = new PreFParser( lexer );
        preParser.setLexer( lexer );
        System.err.println( "Preparsing file: " + tunit.getInputFilename() );
        preParser.translationUnit( tunit );
      }

      iter = program.getChildren().iterator();
      while( iter.hasNext() ) {
        TranslationUnit tunit = (TranslationUnit)iter.next();
        File file = new File( tunit.getInputFilename() );
        LexerSharedInputState lexerState = new LexerSharedInputState(
          new FileReader( file ) );
        FLexer lexer = new FLexer( lexerState );
        FParser parser = new FParser( lexer );
        parser.setLexer( lexer );
        System.err.println( "Parsing file: " + tunit.getInputFilename() );
        parser.translationUnit( tunit );
      }

      // Run transformations
      runPasses( program );

      // Output
      iter = program.getChildren().iterator();
      while( iter.hasNext() ) {
        TranslationUnit tunit = (TranslationUnit)iter.next();
        String outputDir = ConfigurationManager.getOption( "OutputDir" );
        outputDir = (outputDir == null)? "./" : outputDir + "/";
        String outputFilename = tunit.getInputFilename().replaceAll( "^",
          outputDir );
        outputFilename = outputFilename.replaceAll( ".f$", ".cppc.f" );
        tunit.setOutputFilename( outputFilename );
        tunit.print();
        System.out.println( "Created file: " + outputFilename );
      }
    } catch( Exception e ) {
      e.printStackTrace();
    } catch( Error e ) {
      e.printStackTrace();
    }
  }


  private void runPasses( Program program ) {
    ManualPragmasToCppcPragmas.run( program );
    System.out.println( "Adding initialization directive..." );
    AddCppcInitPragma.run( program );
    System.out.println( "Adding finalization directive..." );
    AddCppcShutdownPragma.run( program );
    System.out.println( "Adding non-portable state recovery directives... " );
    AddCppcExecutePragmas.run( program );
    System.out.println( "Adding open files recovery directives..." );
    AddOpenFilesControl.run( program );
    StatementsToCppcStatements.run( program );
    System.out.println( "Analyzing data flow..." );
    CppcStatementFiller.run( program );
    System.out.println( "Analyzing communications..." );
    CommunicationMatching.run( program );
    System.out.println( "Placing checkpoint directives..." );
    AddCheckpointPragmas.run( program );
    System.out.println( "Performing language-dependent transforms..." );
    LanguageTransforms.run( program );
//     CodeOptimizations.run( program );
    CheckCheckpointedProcedures.run( program );
    System.out.println( "Placing register directives..." );
    CppcDependenciesAnalizer.run( program );
    CppcStatementsToStatements.run( program );
    System.out.println( "Adding restart flow control directives..." );
    CheckPragmedProcedures.run( program );
    EnterPragmedProcedures.run( program );
    System.out.println( "Adding context management directives..." );
    AddLoopContextManagement.run( program );
    System.out.println( "Generating code..." );
    PragmaDetection.run( program );
    AddExitLabels.run( program );
    AddRestartJumps.run( program );
  }

  public static void main( String[] args ) {
    new Main().run( args );
  }
}
