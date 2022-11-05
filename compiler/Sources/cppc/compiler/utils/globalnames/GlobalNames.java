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




package cppc.compiler.utils.globalnames;

public interface GlobalNames {

  // Class options
  public static final String CLASS_OPTION = "CPPC/Utils/GlobalNames/ClassName";
  public static final String ADD_CPPC_INIT_PRAGMA_CLASS_OPTION =
    "CPPC/Transforms/AddCppcInitPragma/ClassName";
  public static final String ADD_LOOP_CONTEXT_MANAGEMENT_CLASS_OPTION =
    "CPPC/Transforms/AddLoopContextManagement/ClassName";
  public static final String ADD_OPEN_FILES_CONTROL_CLASS_OPTION =
    "CPPC/Transforms/AddOpenFilesControl/ClassName";
  public static final String ADD_RESTART_JUMPS_CLASS_OPTION =
    "CPPC/Transforms/AddRestartJumps/ClassName";
  public static final String CODE_OPTIMIZATIONS_CLASS_OPTION =
    "CPPC/Transforms/CodeOptimizations/ClassName";
  public static final String COMMUNICATION_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/CommunicationAnalyzer/ClassName";
  public static final String EXPRESSION_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/ExpressionAnalyzer/ClassName";
  public static final String LANGUAGE_ANALYZER_CLASS_OPTION =
    "CPPC/Utils/LanguageAnalyzer/ClassName";
  public static final String LANGUAGE_TRANSFORMS_CLASS_OPTION =
    "CPPC/Utils/LanguageTransforms/ClassName";
  public static final String PRAGMA_DETECTION_CLASS_OPTION =
    "CPPC/Transforms/PragmaDetection/ClassName";
  public static final String SPECIFIER_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/SpecifierAnalyzer/ClassName";
  public static final String STATEMENT_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/StatementAnalyzer/ClassName";
  public static final String SYMBOLIC_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/SymbolicAnalyzer/ClassName";
  public static final String SYMBOLIC_EXPRESSION_ANALYZER_CLASS_OPTION =
    "CPPC/Analysis/SymbolicExpressionAnalyzer/ClassName";
  public static final String VARIABLE_SIZE_ANALYZER_CLASS_OPTION =
    "CPPC/Utils/VariableSizeAnalyzer/ClassName";

  // Pragmas
  public String[] BEGIN_EXECUTE_PRAGMA();
  public String[] CHECKPOINT_PRAGMA();
  public String[] CHECKPOINT_LOOP_PRAGMA();
  public String[] END_EXECUTE_PRAGMA();
  public String[] REGISTER_PRAGMA();
  public String[] SHUTDOWN_PRAGMA();
  public String[] UNREGISTER_PRAGMA();

  // Jump labels
  public String CHECKPOINT_LABEL();
  public int CURRENT_CHKPT_CODE();
  public String ENTER_FUNCTION_LABEL();
  public String EXECUTE_LABEL();
  public String EXIT_FUNCTION_LABEL();
  public String LOOP_UNROLLING_LABEL();
  public String REGISTER_LABEL();
  public String UNREGISTER_LABEL();
  public int NEXT_FILE_CODE();

  // CPPC Functions
  public String ADD_LOOP_INDEX_FUNCTION();
  public String CHECKPOINT_FUNCTION();
  public String COMMIT_CALL_IMAGE_FUNCTION();
  public String CONTEXT_POP_FUNCTION();
  public String CONTEXT_PUSH_FUNCTION();
  public String CREATE_CALL_IMAGE_FUNCTION();
  public String INIT_CONFIGURATION_FUNCTION();
  public String INIT_STATE_FUNCTION();
  public String JUMP_NEXT_FUNCTION();
  public String REGISTER_DESCRIPTOR_FUNCTION();
  public String REGISTER_FOR_CALL_IMAGE_FUNCTION();
  public String REGISTER_FUNCTION();
  public String REMOVE_LOOP_INDEX_FUNCTION();
  public String SET_LOOP_INDEX_FUNCTION();
  public String SHUTDOWN_FUNCTION();
  public String UNREGISTER_DESCRIPTOR_FUNCTION();
  public String UNREGISTER_FUNCTION();

  // Misc
  public String INCLUDE_FILE();
}
