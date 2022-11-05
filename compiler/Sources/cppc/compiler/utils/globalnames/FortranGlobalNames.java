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

public class FortranGlobalNames implements GlobalNames {

  private static final String[] BEGIN_EXECUTE_PRAGMA = { "CPPC", "EXECUTE",
    "ON", "RESTART" };
  private static final String[] CHECKPOINT_PRAGMA = { "CPPC", "CHECKPOINT" };
  private static final String[] CHECKPOINT_LOOP_PRAGMA = { "CPPC",
    "CHECKPOINT", "LOOP" };
  private static final String[] END_EXECUTE_PRAGMA = { "CPPC", "END",
    "EXECUTE" };
  private static final String[] REGISTER_PRAGMA = { "CPPC", "REGISTER" };
  private static final String[] SHUTDOWN_PRAGMA = { "CPPC", "SHUTDOWN" };
  private static final String[] UNREGISTER_PRAGMA = { "CPPC", "UNREGISTER" };

  public String[] BEGIN_EXECUTE_PRAGMA() {
    return BEGIN_EXECUTE_PRAGMA;
  }

  public String[] CHECKPOINT_PRAGMA() {
    return CHECKPOINT_PRAGMA;
  }

  public String[] CHECKPOINT_LOOP_PRAGMA() {
    return CHECKPOINT_LOOP_PRAGMA;
  }

  public String[] END_EXECUTE_PRAGMA() {
    return END_EXECUTE_PRAGMA;
  }

  public String[] REGISTER_PRAGMA() {
    return REGISTER_PRAGMA;
  }

  public String[] SHUTDOWN_PRAGMA() {
    return SHUTDOWN_PRAGMA;
  }

  public String[] UNREGISTER_PRAGMA() {
    return UNREGISTER_PRAGMA;
  }

  private static int CURRENT_LABEL = resetCurrentLabel();
  private static int resetCurrentLabel() {
    return 9000;
  }

  private static int CHKPT_COUNT = 1;
  private static boolean CHECKPOINT_LABEL_CREATED = false;

  private String anyLabel() {
    return new Integer( CURRENT_LABEL++ ).toString();
  }

  public String CHECKPOINT_LABEL() {
    CHECKPOINT_LABEL_CREATED = true;
    return anyLabel();
  }


  public int CURRENT_CHKPT_CODE() {
    int retVal = CHKPT_COUNT;

    if( CHECKPOINT_LABEL_CREATED ) {
      CHKPT_COUNT++;
      CHECKPOINT_LABEL_CREATED = false;
    }

    return retVal;
  }

  public String ENTER_FUNCTION_LABEL() {
    resetCurrentLabel();
    return anyLabel();
  }

  public String EXECUTE_LABEL() {
    return anyLabel();
  }

  public String EXIT_FUNCTION_LABEL() {
    return anyLabel();
  }

  public String LOOP_UNROLLING_LABEL() {
    return anyLabel();
  }

  public String REGISTER_LABEL() {
    return anyLabel();
  }

  public String UNREGISTER_LABEL() {
    return anyLabel();
  }

  private static final String ADD_LOOP_INDEX_FUNCTION = "CPPC_ADD_LOOP_INDEX";
  private static final String CHECKPOINT_FUNCTION = "CPPCF_DO_CHECKPOINT";
  private static final String COMMIT_CALL_IMAGE_FUNCTION =
    "CPPC_COMMIT_CALL_IMAGE";
  private static final String CONTEXT_POP_FUNCTION = "CPPC_CONTEXT_POP";
  private static final String CONTEXT_PUSH_FUNCTION = "CPPC_CONTEXT_PUSH";
  private static final String CREATE_CALL_IMAGE_FUNCTION =
    "CPPC_CREATE_CALL_IMAGE";
  private static final String INIT_CONFIGURATION_FUNCTION =
    "CPPC_INIT_CONFIGURATION";
  private static final String INIT_STATE_FUNCTION = "CPPC_INIT_STATE";
  private static final String JUMP_NEXT_FUNCTION = "CPPC_JUMP_NEXT";
  private static final String REGISTER_DESCRIPTOR_FUNCTION = "CPPCF_OPEN";
  private static final String REGISTER_FOR_CALL_IMAGE_FUNCTION =
    "CPPC_REGISTER_FOR_CALL_IMAGE";
  private static final String REGISTER_FUNCTION = "CPPC_REGISTER";
  private static final String REMOVE_LOOP_INDEX_FUNCTION =
    "CPPC_REMOVE_LOOP_INDEX";
  private static final String SET_LOOP_INDEX_FUNCTION = "CPPC_SET_LOOP_INDEX";
  private static final String SHUTDOWN_FUNCTION = "CPPC_SHUTDOWN";
  private static final String UNREGISTER_DESCRIPTOR_FUNCTION = "CPPCF_CLOSE";
  private static final String UNREGISTER_FUNCTION = "CPPC_UNREGISTER";

  public String ADD_LOOP_INDEX_FUNCTION() {
    return ADD_LOOP_INDEX_FUNCTION;
  }

  public String CHECKPOINT_FUNCTION() {
    return CHECKPOINT_FUNCTION;
  }

  public String COMMIT_CALL_IMAGE_FUNCTION() {
    return COMMIT_CALL_IMAGE_FUNCTION;
  }

  public String CONTEXT_POP_FUNCTION() {
    return CONTEXT_POP_FUNCTION;
  }

  public String CONTEXT_PUSH_FUNCTION() {
    return CONTEXT_PUSH_FUNCTION;
  }

  public String CREATE_CALL_IMAGE_FUNCTION() {
    return CREATE_CALL_IMAGE_FUNCTION;
  }

  public String INIT_CONFIGURATION_FUNCTION() {
    return INIT_CONFIGURATION_FUNCTION;
  }

  public String INIT_STATE_FUNCTION() {
    return INIT_STATE_FUNCTION;
  }

  public String JUMP_NEXT_FUNCTION() {
    return JUMP_NEXT_FUNCTION;
  }

  public String REGISTER_DESCRIPTOR_FUNCTION() {
    return REGISTER_DESCRIPTOR_FUNCTION;
  }

  public String REGISTER_FUNCTION() {
    return REGISTER_FUNCTION;
  }

  public String REGISTER_FOR_CALL_IMAGE_FUNCTION() {
    return REGISTER_FOR_CALL_IMAGE_FUNCTION;
  }

  public String REMOVE_LOOP_INDEX_FUNCTION() {
    return REMOVE_LOOP_INDEX_FUNCTION;
  }

  public String SET_LOOP_INDEX_FUNCTION() {
    return SET_LOOP_INDEX_FUNCTION;
  }

  public String SHUTDOWN_FUNCTION() {
    return SHUTDOWN_FUNCTION;
  }

  public String UNREGISTER_DESCRIPTOR_FUNCTION() {
    return UNREGISTER_DESCRIPTOR_FUNCTION;
  }

  public String UNREGISTER_FUNCTION() {
    return UNREGISTER_FUNCTION;
  }

  private static int NEXT_FILE_CODE = 0;
  public int NEXT_FILE_CODE() {
    return NEXT_FILE_CODE++;
  }

  private static String INCLUDE_FILE = "cppcf.h";
  public String INCLUDE_FILE() {
    return INCLUDE_FILE;
  }
}
