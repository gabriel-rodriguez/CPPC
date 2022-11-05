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



package cppc.compiler.transforms.semantic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.Identifier;
import cetus.hir.FunctionCall;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

public abstract class AddCppcInitPragma {

  private Program program;
  private static final String passName = "[AddCppcInitPragma]";
  private static final String CPPC_COMM_INITIALIZER_ROLE =
    "CPPC/Comm/Initializer";
  private static final HashSet<Identifier> commInitializerFunctions =
    initHashSet( CPPC_COMM_INITIALIZER_ROLE );

  private static final HashSet<Identifier> initHashSet( String role ) {
    return CppcRegisterManager.getProceduresWithRole( role );
  }

  private static final String initString( String [] i ) {

    String s = i[0];
    for( int j = 1; j < i.length; j++ ) {
      s += " " + i[ j ];
    }

    return s;
  }

  protected AddCppcInitPragma( Program program ) {
    this.program = program;
  }

  private static final AddCppcInitPragma getTransformInstance(
    Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.ADD_CPPC_INIT_PRAGMA_CLASS_OPTION );

    try {
      Class theClass = Class.forName( className );
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      AddCppcInitPragma theInstance = (AddCppcInitPragma)instancer.invoke(
        null, program );
      return theInstance;
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }

    return null;
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    AddCppcInitPragma transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  private void start() {

    // Insert the CPPC_Init_configuration call at the very beginning of the
    // program
    Statement ref = this.insertCppcInitConfiguration();

    // Is there a call in the program to a CPPC/Comm/Initializer function?
    // Find it.
    DepthFirstIterator iter = new DepthFirstIterator( program );
    ArrayList<FunctionCall> calls = new ArrayList<FunctionCall>( 1 );
    try {
      while( iter.hasNext() ) {
        FunctionCall call = (FunctionCall)iter.next( FunctionCall.class );
        if( commInitializerFunctions.contains( call.getName() ) ) {
          calls.add( call );
        }
      }
    } catch( NoSuchElementException e ) {}

    // Let's assume, at the moment, that we'll find only one
    // CPPC/Comm/Initializer role.
    if( calls.size() > 1 ) {
      System.err.println( "Error: Program has more than one call to a "+
        "CPPC/Comm/Initializer role." );
      System.err.println( "\tIn cppc.compiler.cetus.transforms.semantic.skel."+
        "AddCppcInitPragma.start()" );
      System.exit( 0 );
    }

    // If no such calls: insert after CPPC_Init_configuration
    if( calls.size() == 0 ) {
      this.insertCppcInitState( ref, false );
    } else {
      // Else, add the CPPC_Init_state after the call
      this.insertCppcInitState( (Statement)ObjectAnalizer.getParentOfClass(
        calls.get( 0 ), Statement.class ), true );
    }
  }

  protected abstract void checkMainProcedure( Procedure mainProc );
  protected abstract void addInitFunctionParameters( Procedure mainProc,
    FunctionCall call );

  private Statement insertCppcInitConfiguration() {

    // Get main procedure and insert the CPPC_Init_configuration as first stmt
    Procedure mainProc = ObjectAnalizer.findMainProcedure( program );
    CompoundStatement mainCode = mainProc.getBody();

    // Check the main procedure for (maybe) missing parameters
    this.checkMainProcedure( mainProc );

    // Create the Function Call
    FunctionCall call = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().INIT_CONFIGURATION_FUNCTION() ) );
    addInitFunctionParameters( mainProc, call );
    ExpressionStatement callStmt = new ExpressionStatement( call );
    Statement ref = ObjectAnalizer.findLastDeclaration( mainProc );
    if( ref != null ) {
      mainCode.addStatementAfter( ref, callStmt );
    } else {
      ref = (Statement)mainCode.getChildren().get( 0 );
      mainCode.addStatementBefore( ref, callStmt );
    }

    // Add a new Annotation to mark a conditional jump
    CppcConditionalJump jump = new CppcConditionalJump();
    mainCode.addStatementAfter( callStmt, jump );

    return jump;
  }

  private void insertCppcInitState( Statement ref, boolean foundComm ) {

    // Get CompoundStatement containing the call
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( ref,
        CompoundStatement.class );

    // Create the CPPC_Init_state call and insert it after the comm init
    FunctionCall call = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().INIT_STATE_FUNCTION() ) );
    ExpressionStatement callStmt = new ExpressionStatement( call );
    statementList.addStatementAfter( ref, callStmt );

    // Add label mark and jump mark after callStmt
    CppcLabel label = new CppcLabel( new Identifier(
      GlobalNamesFactory.getGlobalNames().CHECKPOINT_LABEL() ) );
    if( foundComm ) {
      // The ref statement is the communication initializer
      statementList.addStatementBefore( ref, label );
    } else {
      // The ref statement is prolly a jump mark
      statementList.addStatementBefore( callStmt, label );
    }

    // Add conditional jump
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter( callStmt, jump );
  }
}
