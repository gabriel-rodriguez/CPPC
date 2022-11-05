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




package cppc.compiler;

import cetus.exec.Driver;

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
import cppc.compiler.transforms.syntactic.skel.DetectUserTypes;
import cppc.compiler.transforms.syntactic.skel.EnterPragmedProcedures;
import cppc.compiler.transforms.syntactic.skel.LanguageTransforms;
import cppc.compiler.transforms.syntactic.skel.ManualPragmasToCppcPragmas;
import cppc.compiler.transforms.syntactic.skel.PragmaDetection;
import cppc.compiler.transforms.syntactic.skel.RequireProcedureReturns;
import cppc.compiler.transforms.syntactic.skel.StatementsToCppcStatements;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;

public class Compiler extends Driver {

  private static final String C_ADDCPPCINITPRAGMA_CLASSNAME =
    "cppc.compiler.transforms.semantic.stub.c.AddCppcInitPragma";
  private static final String C_ADDFILESCONTROL_CLASSNAME =
    "cppc.compiler.transforms.semantic.stub.c.AddOpenFilesControl";
  private static final String C_ADDLOOPCONTEXTMANAGEMENT_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.c.AddLoopContextManagement";
  private static final String C_ADDRESTARTJUMPS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.c.AddRestartJumps";
  private static final String C_CODEOPTIMIZATIONS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.c.CodeOptimizations";
  private static final String C_COMMUNICATIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CCommunicationAnalyzer";
  private static final String C_EXPRESSIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CExpressionAnalyzer";
  private static final String C_GLOBALNAMES_CLASSNAME =
    "cppc.compiler.utils.globalnames.CGlobalNames";
  private static final String C_LANGUAGEANALYZER_CLASSNAME =
    "cppc.compiler.utils.language.CLanguageAnalyzer";
  private static final String C_LANGUAGETRANSFORMS_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.c.LanguageTransforms";
  private static final String C_PRAGMADETECTION_CLASSNAME =
    "cppc.compiler.transforms.syntactic.stub.c.PragmaDetection";
  private static final String C_SPECIFIERANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CSpecifierAnalyzer";
  private static final String C_STATEMENTANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CStatementAnalyzer";
  private static final String C_SYMBOLICANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CSymbolicAnalyzer";
  private static final String C_SYMBOLICEXPRESSIONANALYZER_CLASSNAME =
    "cppc.compiler.analysis.CSymbolicExpressionAnalyzer";
  private static final String C_VARIABLESIZEANALYZER_CLASSNAME =
    "cppc.compiler.utils.CVariableSizeAnalizer";

  static {
    ConfigurationManager.setOption(
      GlobalNames.ADD_CPPC_INIT_PRAGMA_CLASS_OPTION,
      C_ADDCPPCINITPRAGMA_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.ADD_LOOP_CONTEXT_MANAGEMENT_CLASS_OPTION,
      C_ADDLOOPCONTEXTMANAGEMENT_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.ADD_OPEN_FILES_CONTROL_CLASS_OPTION,
      C_ADDFILESCONTROL_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.ADD_RESTART_JUMPS_CLASS_OPTION,
      C_ADDRESTARTJUMPS_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.CLASS_OPTION,
      C_GLOBALNAMES_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.CODE_OPTIMIZATIONS_CLASS_OPTION,
      C_CODEOPTIMIZATIONS_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.COMMUNICATION_ANALYZER_CLASS_OPTION,
      C_COMMUNICATIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.EXPRESSION_ANALYZER_CLASS_OPTION,
      C_EXPRESSIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.LANGUAGE_ANALYZER_CLASS_OPTION,
      C_LANGUAGEANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.LANGUAGE_TRANSFORMS_CLASS_OPTION,
      C_LANGUAGETRANSFORMS_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.PRAGMA_DETECTION_CLASS_OPTION,
      C_PRAGMADETECTION_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.SPECIFIER_ANALYZER_CLASS_OPTION,
      C_SPECIFIERANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.STATEMENT_ANALYZER_CLASS_OPTION,
      C_STATEMENTANALYZER_CLASSNAME );
    ConfigurationManager.setOption( GlobalNames.SYMBOLIC_ANALYZER_CLASS_OPTION,
      C_SYMBOLICANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.SYMBOLIC_EXPRESSION_ANALYZER_CLASS_OPTION,
      C_SYMBOLICEXPRESSIONANALYZER_CLASSNAME );
    ConfigurationManager.setOption(
      GlobalNames.VARIABLE_SIZE_ANALYZER_CLASS_OPTION,
      C_VARIABLESIZEANALYZER_CLASSNAME );
  }

  protected Compiler() {
    super();
  }

  public void runPasses() {
    DetectUserTypes.run( this.program );
    ManualPragmasToCppcPragmas.run( this.program );
    AddCppcInitPragma.run( this.program ); // Non-pipelinable
    AddCppcShutdownPragma.run( this.program );  // Non-pipelinable
    AddCppcExecutePragmas.run( this.program ); // Pipelinable
    AddOpenFilesControl.run( this.program ); // Pipelinable
    StatementsToCppcStatements.run( this.program ); // Pipelinable
    CppcStatementFiller.run( this.program ); // ??
    CommunicationMatching.run( this.program );
    AddCheckpointPragmas.run( this.program );
    LanguageTransforms.run( this.program ); // Pipelinable
//     CodeOptimizations.run( this.program ); // ??
    CheckCheckpointedProcedures.run( this.program ); // ??
    CppcDependenciesAnalizer.run( this.program );
    CppcStatementsToStatements.run( this.program ); // Pipelinable
    CheckPragmedProcedures.run( this.program ); //Pipelinable
    EnterPragmedProcedures.run( this.program ); //Non-Pipelinable
    AddLoopContextManagement.run( this.program ); // Non-pipelinable?
    PragmaDetection.run( this.program ); // Non-Pipelinable?
    AddExitLabels.run( this.program ); // Pipelinable
    AddRestartJumps.run( this.program ); // Pipelinable
    RequireProcedureReturns.run( this.program ); // Pipelinable
  }

  public static void main( String args[] ) {
    args = ConfigurationManager.parseCommandLine( args );
    Compiler compiler = new Compiler();
    compiler.run( args );
  }

}
