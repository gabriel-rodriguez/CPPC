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

import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.exceptions.TypeNotSupportedException;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public abstract class AddLoopContextManagement extends ProcedureWalker {

  private static String passName = "[AddLoopContextManagement]";
  private ArrayList<Statement> processedLoops;

  protected AddLoopContextManagement( Program program ) {
    super( program );
    processedLoops = new ArrayList<Statement>();
  }

  private static final AddLoopContextManagement getTransformInstance(
    Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.ADD_LOOP_CONTEXT_MANAGEMENT_CLASS_OPTION );
    try {
      Class theClass = Class.forName( className );
      Class [] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      AddLoopContextManagement theInstance =
        (AddLoopContextManagement)instancer.invoke( null, program );
      return theInstance;
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }

    return null;
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    AddLoopContextManagement transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {

    DepthFirstIterator iter = new DepthFirstIterator( procedure );

    try {
      while( iter.hasNext() ) {
        // Process only non-portable function marks. Checkpoints are handled by
        // the  parent HeapContext. So are registers.
        CppcNonportableFunctionMark mark =
          (CppcNonportableFunctionMark)iter.next(
            CppcNonportableFunctionMark.class );

        Statement ref = (Statement)ObjectAnalizer.getParentOfClass( mark,
          Statement.class );
        Statement loop = this.testInsideLoop( ref );
        while( ( loop != null ) && !processedLoops.contains( loop ) ) {
          processLoop( loop );
          processedLoops.add( loop );
          loop = testInsideLoop( loop );
        }
      }
    } catch( NoSuchElementException e ) {}
  }

  private void processLoop( Statement loop ) {

    // Get the compound statement containing the loop
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( loop,
        CompoundStatement.class );

    // Get the loop index variable
    Identifier loopIndex = this.getLoopIndex( loop );

    // Get the loop index type
    VariableDeclaration vd = null;
    try {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
          loop, loopIndex );
    } catch( SymbolIsNotVariableException e ) {
      System.err.println( "BUG: Symbol " + loopIndex + " is not a variable in current context.\n" );
      System.err.println( "\tUnable to analyze loop.\n" );
      System.err.println( "\tAt " + this.getClass() + ".processLoop" );
      return;
    } catch( SymbolNotDefinedException e ) {
      System.err.println( "BUG: Symbol " + loopIndex + " not defined in current context.\n" );
      System.err.println( "\tUnable to analyze loop.\n" );
      System.err.println( "\tAt " + this.getClass() + ".processLoop" );
      return;
    }

    Identifier cppcType = null;
    try {
      cppcType = (Identifier)
        TypeManager.getType( vd.getSpecifiers() ).getBaseType().clone();
    } catch( Exception e ) {
      System.err.println( "BUG: Type not supported by the CPPC framework.\n" );
      System.err.println( "\tUnable to analyze loop.\n" );
      System.err.println( "\tAt " + this.getClass() + ".processLoop" );
      return;
    }

    // First: add CPPC_Add_loop_index call before the loop
    FunctionCall addIndexCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().ADD_LOOP_INDEX_FUNCTION() ) );
    addIndexCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
        loopIndex.toString() ) );
    addIndexCall.addArgument( cppcType );
    statementList.addStatementBefore( loop,
      new ExpressionStatement( addIndexCall ) );

    // Second: add CPPC_Set_loop_index call after the loop header
    FunctionCall setIndexCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().SET_LOOP_INDEX_FUNCTION() ) );
    Declarator indexDeclarator = ObjectAnalizer.getDeclarator( vd, loopIndex );
    if( indexDeclarator == null ) {
      System.err.println( "BUG: Declarator not found for loop index.\n" );
      System.err.println( "\tUnable to analyze loop.\n" );
      System.err.println( "\tAt " + this.getClass() + ".processLoop" );
      return;
    }
    setIndexCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
        indexDeclarator ) );

    CompoundStatement loopBody = (CompoundStatement)((Loop)loop).getBody();
    loopBody.getChildren().add( 0, new ExpressionStatement( setIndexCall ) );

    // Third: add a CPPC_Remove_loop_index after the loop body
    FunctionCall removeIndexCall = new FunctionCall( new Identifier(
      GlobalNamesFactory.getGlobalNames().REMOVE_LOOP_INDEX_FUNCTION() ) );
    statementList.addStatementAfter( loop, new ExpressionStatement(
      removeIndexCall ) );
  }

  protected abstract Statement testInsideLoop( Statement ref );
  protected abstract Identifier getLoopIndex( Statement loop );
}
