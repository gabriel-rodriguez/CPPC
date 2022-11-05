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
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;

import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

public abstract class AddOpenFilesControl extends ProcedureWalker {

  private static String passName = "[AddOpenFilesControl]";
  protected static final String CPPC_IO_OPEN_ROLE = "CPPC/IO/Open";
  protected static final String CPPC_IO_CLOSE_ROLE = "CPPC/IO/Close";
  private static final HashSet<Identifier> ioOpenFunctions = initHashSet(
    CPPC_IO_OPEN_ROLE);
  private static final HashSet<Identifier> ioCloseFunctions = initHashSet(
    CPPC_IO_CLOSE_ROLE );

  private static final HashSet<Identifier> initHashSet( String role ) {
    return CppcRegisterManager.getProceduresWithRole( role );
  }

  protected AddOpenFilesControl( Program program ) {
    super( program );
  }

  private static final AddOpenFilesControl getTransformInstance(
    Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.ADD_OPEN_FILES_CONTROL_CLASS_OPTION );
    try {
      Class theClass = Class.forName( className );
      Class [] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      AddOpenFilesControl theInstance = (AddOpenFilesControl)instancer.invoke(
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

    AddOpenFilesControl transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " begin", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    DepthFirstIterator iter = new DepthFirstIterator( procedure );
    ArrayList<FunctionCall> openingFunctions = new ArrayList<FunctionCall>();
    ArrayList<FunctionCall> closingFunctions = new ArrayList<FunctionCall>();

    // Get function calls which fulfill the CPPC/IO/Open and CPPC/IO/Close
    // roles
    try {
      while( iter.hasNext() ) {
        FunctionCall call = (FunctionCall)iter.next( FunctionCall.class );

        if( ioOpenFunctions.contains( call.getName() ) ) {
          openingFunctions.add( call );
        }

        if( ioCloseFunctions.contains( call.getName() ) ) {
          closingFunctions.add( call );
        }
      }
    } catch( NoSuchElementException e ) {}

    // Process CPPC/IO/Open roles
    for( FunctionCall call: openingFunctions ) {
      Statement cppcCall = getCppcOpenCall( call );
      if( cppcCall != null ) {
        CompoundStatement statementList =
          (CompoundStatement)ObjectAnalizer.getParentOfClass( call,
          CompoundStatement.class );
        Statement s = (Statement)ObjectAnalizer.getParentOfClass( call,
          Statement.class );
        statementList.addStatementAfter( s, cppcCall );
        ObjectAnalizer.encloseWithExecutes( cppcCall );
      }
    }

    // Process CPPC/IO/Close roles
    for( FunctionCall call: closingFunctions ) {
      Statement cppcCall = getCppcCloseCall( call );
      if( cppcCall != null ) {
        CompoundStatement statementList =
          (CompoundStatement)ObjectAnalizer.getParentOfClass( call,
          CompoundStatement.class );
        Statement s = (Statement)ObjectAnalizer.getParentOfClass( call,
          Statement.class );
        statementList.addStatementAfter( s, cppcCall );
        // It is NOT needed to re-execute the close function at restart: the
        // file will not be opened by the opening function.
      }
    }
  }

  protected abstract Statement getCppcOpenCall( FunctionCall call );
  protected abstract Statement getCppcCloseCall( FunctionCall call );
}
