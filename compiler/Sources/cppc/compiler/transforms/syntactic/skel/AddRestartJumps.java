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

import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.Traversable;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.ObjectAnalizer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AddRestartJumps extends ProcedureWalker {

  private static String passName = "[AddRestartJumps]";

  protected AddRestartJumps( Program program ) {
    super( program );
  }

  private static final AddRestartJumps getTransformInstance( Program program ) {

    String className = ConfigurationManager.getOption(
      GlobalNames.ADD_RESTART_JUMPS_CLASS_OPTION );

    try {
      Class theClass = Class.forName( className );
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod( "getTransformInstance", param );
      AddRestartJumps theInstance = (AddRestartJumps)instancer.invoke( null,
        program );
      return theInstance;
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }

    return null;
  }

  public static void run( Program program ) {

    Tools.printlnStatus( passName + " begin", 1 );

    AddRestartJumps transform = getTransformInstance( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected abstract void addCppcVariables( Procedure procedure,
    List<CppcLabel> orderedLabels );
  protected abstract void addConditionalJump( CppcConditionalJump jump,
    List<CppcLabel> orderedLabels );

  protected void walkOverProcedure( Procedure procedure ) {   
    // If this procedure does not contain CppcLabels, there's
    // nothing to do
    if( !ObjectAnalizer.isCppcProcedure( procedure ) ) {
      return;
    }

    // Filter the conditional jumps so we do not insert a jump and
    // a label in adjacent positions
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Expression.class );
    try {
      while( iter.hasNext() ) {
        // Find a conditional jump
        CppcConditionalJump jump = (CppcConditionalJump)
          iter.next( CppcConditionalJump.class );

        if( iter.hasNext() ) {
          // If next to the jump there's a label: two adjacent RECs. Remove the
          // jump after the first and the label at the beginning of the second.
          Statement stmt = (Statement)iter.next( Statement.class );
          if( stmt instanceof CppcLabel ) {
            // Detach both the jump and the target label
            jump.detach();
            stmt.detach();
          }
        }
      }
    } catch( NoSuchElementException e ) {}


    // Instrument if statements appropriately
    iter.reset();
    iter.next(); // Discharge compound statement
    while( iter.hasNext() ) {
      try {
        IfStatement ifStmt = (IfStatement)iter.next( IfStatement.class );
        this.instrumentIfStatement( ifStmt );
      } catch( NoSuchElementException e ) {}
    }
    
    // Instrument loops appropriately
    iter.reset();
    iter.next(); // Discharge compound statement
    while( iter.hasNext() ) {
      try {
        Loop loop = (Loop)iter.next( Loop.class );
        this.instrumentLoop( loop );
      } catch( NoSuchElementException e ) {}
    }

    // Get the CppcLabels orderly and store them in a list
    iter.reset();
    ArrayList<CppcLabel> orderedLabels = new ArrayList<CppcLabel>();

    try {
      while( iter.hasNext() ) {
        orderedLabels.add( (CppcLabel)iter.next( CppcLabel.class ) );
      }
    } catch( NoSuchElementException e ) {}

    // If no CppcLabels are found, the job is done
    if( orderedLabels.size() == 0 ) {
      return;
    }

    // Add needed variables
    addCppcVariables( procedure, orderedLabels );

    // Add the conditional jump where there are conditional jump marks
    iter.reset();
    try {
      while( iter.hasNext() ) {
        CppcConditionalJump jump = (CppcConditionalJump)
          iter.next( CppcConditionalJump.class );

        addConditionalJump( jump, orderedLabels );
      }
    } catch( NoSuchElementException e ) {}
  }

  private void instrumentIfStatement( IfStatement ifStmt ) {
    // Get then and else parts
    Statement thenPart = ifStmt.getThenStatement();
    Statement elsePart = ifStmt.getElseStatement();
    CompoundStatement thenList = null;
    CompoundStatement elseList = null;

    // Find how many jumps are there in each one
    int thenLabels = this.executesInList( thenPart );
    int elseLabels = this.executesInList( elsePart );

    // If no jumps in any of them: return
    if( (thenLabels==0) && (elseLabels==0) ) {
      return;
    }

    // Otherwise, begin instrumentation. Add a jump at the beginning of each
    // branch. Ensure that both branches are compound statements. If else
    // branch is null, create an empty compound statement and add a mark
    if( !(thenPart instanceof CompoundStatement) ) {
      CompoundStatement newThen = new CompoundStatement();
      ifStmt.setThenStatement( newThen );
      newThen.addStatement( thenPart );
      thenList = newThen;
    } else {
      thenList = (CompoundStatement)thenPart;
    }

    if( elsePart == null ) {
      elsePart = new CompoundStatement();
      ifStmt.setElseStatement( elsePart );
    }

    if( !(elsePart instanceof CompoundStatement) ) {
      CompoundStatement newElse = new CompoundStatement();
      ifStmt.setElseStatement( newElse );
      newElse.addStatement( elsePart );
      elseList = newElse;
    } else {
      elseList = (CompoundStatement)elsePart;
    }

    CppcConditionalJump thenJump = new CppcConditionalJump();
    CppcConditionalJump elseJump = new CppcConditionalJump();

    Statement firstExecutable = ObjectAnalizer.findFirstExecutable( thenList );
    if( firstExecutable == null ) {
      thenList.addStatement( thenJump );
    } else {
      thenList.addStatementBefore( firstExecutable, thenJump );
    }

    firstExecutable = ObjectAnalizer.findFirstExecutable( elseList );
    if( firstExecutable == null ) {
      elseList.addStatement( elseJump );
    } else {
      elseList.addStatementBefore( firstExecutable, elseJump );
    }

    // Find last jump in then part. Make its leap = elseLabels+1
    if( elseLabels != 0 ) {
      CppcConditionalJump lastJump = this.findLastJump( thenList );
      lastJump.setLeap( elseLabels + 1 );
    }

    // Find first jump in else part. Make its leap = thenLabels+1
    if( thenLabels != 0 ) {
      CppcConditionalJump firstJump = this.findFirstJump( elseList );
      firstJump.setLeap( thenLabels + 1 );
    }
  }
  
  private void instrumentLoop( Loop loop ) {
    // Check whether this loop contains CPPC code. Do not check whether there are CppcLabel's, as this can be tricky
    // (most checkpointed Fortran loops merge labels and jumps in such a way that labels inside the loop are removed).
    // Look for jumps instead, since it is guaranteed that an instrumented loop will have at least one jump.
    BreadthFirstIterator iter = new BreadthFirstIterator( loop.getBody() );
    try {
      // Use the iterator for looking for a jump
      iter.next( CppcConditionalJump.class );
    } catch( Exception e ) {
      return;
    }
    
    // Add a cppc jump after the loop. During a restart, depending on the iteration values,
    // control may not enter the loop and we need a workaround to get to the next label.
    // Add a cppc jump after the jump. During a restart, depending on the iteration values,
    CppcConditionalJump jump = new CppcConditionalJump();
    CompoundStatement statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass( (Traversable)loop, CompoundStatement.class );
    statementList.addStatementAfter( (Statement)loop, jump );
    
    // Make the jump skip all the labels inside the loop
    int loopLabels = this.executesInList( (Statement)loop );
    jump.setLeap( loopLabels + 1 );
  }

  private int executesInList( Statement statementList ) {
    if( statementList == null ) {
      return 0;
    }

    // This descends recursively
    int currentExecutes = 0;
    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    iter.next(); //Discharge the compound statement
    iter.pruneOn( Expression.class );

    while( iter.hasNext() ) {
      try {
        Statement next = (Statement)iter.next( Statement.class );
        if( next instanceof CppcLabel ) {
          currentExecutes++;
          continue;
        }

        if( next instanceof CompoundStatement ) {
          int insideCount = this.executesInList( (CompoundStatement)next );
          currentExecutes += insideCount;
        }
      } catch( NoSuchElementException e ) {}
    }

    return currentExecutes;
  }

  private CppcConditionalJump findFirstJump( Statement statementList ) {
    // This does not descend recursively
    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Statement.class );

    while( iter.hasNext() ) {
      try {
        CppcConditionalJump next = (CppcConditionalJump)
          iter.next( CppcConditionalJump.class );
        return next;
      } catch( NoSuchElementException e ) {}
    }

    return null;
  }

  private CppcConditionalJump findLastJump( Statement statementList ) {
    // This does not descend recursively
    DepthFirstIterator iter = new DepthFirstIterator( statementList );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Statement.class );

    CppcConditionalJump last = null;
    while( iter.hasNext() ) {
      try {
        last = (CppcConditionalJump)iter.next( CppcConditionalJump.class );
      } catch( NoSuchElementException e ) {}
    }

    return last;
  }
}
