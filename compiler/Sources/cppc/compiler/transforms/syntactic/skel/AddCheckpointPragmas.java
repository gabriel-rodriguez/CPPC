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
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.TranslationUnit;
import cetus.hir.Traversable;

import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.shared.comms.Communication;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class AddCheckpointPragmas extends ProcedureWalker {

  private final class LoopNode {
    private CppcStatement loop;
    private Set<Identifier> calledProcedures;
    private Set<Identifier> callerProcedures;
    private boolean checkpoint;
    // FIXME : In the code's current state, it might be possible that manual
    // ckpts are not working
    private boolean manualCheckpoint;
    private LoopNode parent;
    private List<LoopNode> children;

    LoopNode( CppcStatement loop, Set<Identifier> calledProcedures,
       Set<Identifier> callerProcedures, //boolean tooHeavy,
      boolean manualCheckpoint ) {

      this.loop = loop;
      this.calledProcedures = calledProcedures;
      this.callerProcedures = callerProcedures;
      this.checkpoint = false;
      this.manualCheckpoint = manualCheckpoint;
      this.parent = null;
      this.children = new ArrayList<LoopNode>();
    }

    CppcStatement getLoop() {
      return this.loop;
    }

    Set<Identifier> getCalledProcedures() {
      return this.calledProcedures;
    }

    void setCalledProcedures( Set<Identifier> calledProcedures ) {
      this.calledProcedures = calledProcedures;
    }

    Set<Identifier> getCallerProcedures() {
      return this.callerProcedures;
    }

    boolean getCheckpoint() {
      return this.checkpoint;
    }

    void setCheckpoint( boolean checkpoint ) {
      this.checkpoint = checkpoint;
    }

    boolean getManualCheckpoint() {
      return this.manualCheckpoint;
    }

    void setManualCheckpoint( boolean manualCheckpoint ) {
      this.manualCheckpoint = manualCheckpoint;
    }

    double costFunction() {
      double sFactor = ((double)this.loop.statementCount /
        (double)programStatements);
      double wFactor = ((double)this.loop.getWeight() / (double)programSize);

      if( wFactor== 0 ) {
        return -1;
      }

      return -Math.log10( sFactor*wFactor );
    }

    LoopNode getParent() {
      return this.parent;
    }

    void setParent( LoopNode p ) {
      this.parent = p;
    }

    List<LoopNode> getChildren() {
      return this.children;
    }

    void add( LoopNode n ) {
      n.setParent( this );

      for( int i = 0; i < children.size(); i++ ) {
        if( children.get(i).costFunction() > n.costFunction() ) {
          children.add(i,n);
          return;
        }
      }

      children.add(n);
    }
  }

  private static final String passName = "[AddCheckpointPragmas]";
  private long programSize;
  private int programStatements;
  private List<LoopNode> loopCatalog = new
    ArrayList<LoopNode>();

  public AddCheckpointPragmas( Program program ) {
    super( program );


    // Find total program size
    Procedure main = ObjectAnalizer.findMainProcedure( program );
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)main.getName() );
    programSize = c.getWeight();
    programStatements = c.statementCount;
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    if( ConfigurationManager.hasOption(
      ConfigurationManager.DISABLE_CKPT_ANALYSIS ) ) {

      return;
    }

    AddCheckpointPragmas transform = new AddCheckpointPragmas( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  void printLoops( List<LoopNode> loops, int tabs ) {

    for( LoopNode n: loops ) {
      if( tabs == 0 ) {
        System.out.println();
      }
      System.out.println( n.costFunction() );
      this.printLoops( n.getChildren(), tabs+1 );
    }
  }

  protected void start() {
    // Analyze procedures
    super.start();

    List<LoopNode> orphanLoops = new ArrayList<LoopNode>();
    Map<CppcStatement,LoopNode> processedLoops = new
      HashMap<CppcStatement,LoopNode>();
    SetOperations<Identifier> setOps = new SetOperations<Identifier>();

    for( LoopNode l: loopCatalog ) {
      if( l.getParent() != null ) {
        // Already processed
        continue;
      }

      // Find direct or transitive parents in loop catalog
      boolean added = false;
      for( LoopNode ll: loopCatalog ) {
        if( l != ll ) {
          // Direct paret
          if( ll.getCalledProcedures().contains(
            l.getLoop().getProcedure().getName() ) ) {

            ll.add( l );
            added = true;
            break;
          }

          if( !setOps.setIntersection( l.getCallerProcedures(),
            ll.getCalledProcedures() ).isEmpty() ) {

            ll.add( l );
            added = true;
            // Don't break, continue looking for direct parents
          }
        }
      }

      if( !added ) {
        // Check that this is called at some point in the code
        if( (l.getCallerProcedures().size()==1) &&
          !ObjectAnalizer.isMainProcedure( l.getLoop().getProcedure() ) ) {

          // do not add to orphans or elsewhere
          continue;
        }
      }

      if( !added ) {
        for( int i = 0; i < orphanLoops.size(); i++ ) {
          if( orphanLoops.get(i).costFunction() > l.costFunction() ) {
            orphanLoops.add( i, l );
            added = true;
            break;
          }
        }
      }

      if( !added ) {
        orphanLoops.add( l );
      }
    }

    if( orphanLoops.isEmpty() ) {
      return;
    }

    // Cost function
    int n = orphanLoops.size();
    double f[] = new double[n];
    for( int i = 0; i < n; i++ ) {
      f[i] = orphanLoops.get(i).costFunction();
    }

    // Distances
    double a,b,c,d,D[] = new double[n];
    a = f[n-1]-f[0];
    b = 1-n; // 0-(n-1)
    c = -f[0]*b;
    d = Math.sqrt( a*a + b*b );
    int pos = 0;
    for( int i = 0; i < n; i++ ) {
      D[i] = Math.abs( a*i + b*f[i] + c ) / d;
      if( D[i] > D[pos] ) {
        pos = i;
      }
    }

    // 1st & 2nd derivatives
    int threshold = -1;
    if( pos != 0 ) {
      double df[] = new double[pos+1];
      double d2f[] = new double[pos+1];
      double sum[] = new double[pos+1];
      boolean ismax[] = new boolean[pos+1];
      df[0] = d2f[0] = sum[0] = 0;
      ismax[0] = false;
      for( int i = 1; i < pos+1; i++ ) {
        df[i] = f[i]-f[i-1];
        d2f[i] = df[i]-df[i-1];
        sum[i] = sum[i-1] + df[i];
      }

      for( int i = 1; i < pos; i++ ) {
        ismax[i] = (d2f[i]>d2f[i-1]) && (d2f[i]>d2f[i+1]);
      }
      ismax[pos] = d2f[pos] > d2f[pos-1];

      // Choose a local max to proceed
      for( int i = 0; i < pos+1; i++ ) {
        if( ismax[i] ) {
          if( sum[i]/sum[pos] > 0.5 ) {
            threshold = i-1;
            break;
          }
        }
      }

      if( threshold == -1 ) {
        // No threshold makes sum[i]/sum[pos] > 0.5
        // Choose last one
        for( int i=pos; i>=0; i-- ) {
          if( ismax[i] ) {
            threshold = i-1;
            break;
          }
        }
      }
    } else {
      threshold = 0;
    }

    // Process results
    for( int i = 0; i < threshold+1; i++ ) {
      this.placeCheckpoint( orphanLoops.get(i) );
    }
  }

  private double pushCheckpoint( LoopNode p, double sum, double sumThreshold,
    double costThreshold ) {
    List<LoopNode> children = p.getChildren();

    for( LoopNode n: children ) {
      // If we are beyond the cost threshold, then this branch is done
      if( n.costFunction() > costThreshold ) {
        // All further children have lower cost, and therefore cannot meet the
        // cost threshold
        return sum;
      }

      if( n.costFunction() < p.costFunction() ) {
        // This only happens when the children loop is inside an IF statement.
        // Since we prefer not to insert checkpoints at conditional statements,
        // we choose to entirely skip this branch.
        break;
      }

      // How does cost vary by pushing this chkpt down?
      double delta = n.costFunction() - p.costFunction();
      if( sum-delta > sumThreshold ) {
        // Push it down
        sum -= delta;
        p.setCheckpoint( false );
        n.setCheckpoint( true );
        sum = this.pushCheckpoint( n, sum, sumThreshold, costThreshold );
      } else {
        // All further children have lower cost, therefore delta will be
        // greater and it is imposible to meet the sumThreshold
        return sum;
      }
    }

    return sum;
  }

  protected void walkOverProcedure( Procedure procedure ) {
    DepthFirstIterator iter = new DepthFirstIterator( procedure.getBody() );
    iter.pruneOn( Expression.class );
    iter.pruneOn( Loop.class );

    while( iter.hasNext() ) {
      try {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class );

        if( cppcStatement.getStatement() instanceof Loop ) {
          this.processLoop( cppcStatement, null );
        }
      } catch( NoSuchElementException e ) {}
    }
  }

  private void processLoop( CppcStatement cppcStatement, LoopNode parent ) {
    Loop l = (Loop)cppcStatement.getStatement();
    DepthFirstIterator iter = new DepthFirstIterator( l.getBody() );
    iter.pruneOn( Expression.class );
    iter.pruneOn( Loop.class );
    Set<Identifier> calledFunctions = new HashSet<Identifier>();
    boolean manualCheckpoint = false;

    Identifier thisProc = (Identifier)cppcStatement.getProcedure().getName();
    Set<Identifier> thisCallers = this.getCallers( thisProc );

    LoopNode node = new LoopNode( cppcStatement, null, thisCallers, //false,
      false );
    if( node.costFunction() == -1 ) {
      return;
    }

    if( parent != null ) {
      node.setParent( parent );
    }

    while( iter.hasNext() ) {
      try {
        CppcStatement innerStatement = (CppcStatement)iter.next(
          CppcStatement.class );

        if( innerStatement.getStatement() instanceof Loop ) {
          this.processLoop( innerStatement, node );
        }

        if( innerStatement.getStatement() instanceof
          CppcCheckpointLoopPragma ) {

          innerStatement.detach();
          manualCheckpoint = true;
        }

        if( innerStatement.getStatement() instanceof ExpressionStatement ) {
          DepthFirstIterator stmtIter = new DepthFirstIterator(
            innerStatement );
          while( stmtIter.hasNext() ) {
            try {
              FunctionCall call = (FunctionCall)stmtIter.next(
                FunctionCall.class );
              calledFunctions.add( (Identifier)call.getName() );
            } catch( NoSuchElementException e ) {}
          }
        }
      } catch( NoSuchElementException e ) {}
    }

    node.setCalledProcedures( calledFunctions );
    node.setManualCheckpoint( manualCheckpoint );
    if( parent != null ) {
      parent.add( node );
    }

    loopCatalog.add( node );
  }

  private Set<Identifier> getCallers( Identifier proc ) {
    if( !CppcRegisterManager.isRegistered( proc ) ) {
      return null;
    }

    // Find all transitionally caller procedures
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      proc );
    Set<Identifier> callers = new HashSet<Identifier>();
    Set<Identifier> add = new HashSet<Identifier>( c.getCalledFrom() );
    callers.add( proc );

    while( add.size() > 0 ) {
      Set<Identifier> newAdd = new HashSet<Identifier>();

      for( Identifier name: add ) {
        callers.add( name );

        if( CppcRegisterManager.isRegistered( name ) ) {
          ProcedureCharacterization nc =
            CppcRegisterManager.getCharacterization( name );

          newAdd.addAll( nc.getCalledFrom() );
        }
      }

      newAdd.removeAll( callers );
      add = newAdd;
    }

    return callers;
  }

  private void placeCheckpoint( LoopNode ckpt ) {
    // Find first safe point
    BreadthFirstIterator iter = new BreadthFirstIterator(
      ((Loop)ckpt.getLoop().getStatement()).getBody() );
    iter.next(); // Discharge compound statement
    iter.pruneOn( Expression.class );

    while( iter.hasNext() ) {
      try {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class );

        // Insert into safe point and NOT declaration statement
        if( cppcStatement.getSafePoint() &&
          !(cppcStatement.getStatement() instanceof DeclarationStatement) ) {

          TranslationUnit tunit = (TranslationUnit)
            ckpt.getLoop().getProcedure().getParent();
          System.out.println( "Placing checkpoint at: " +
            tunit.getInputFilename() + ": " +
            cppcStatement.where() );

          this.addCheckpointPragma( cppcStatement );

          return;
        }
      } catch( NoSuchElementException e ) {
        TranslationUnit tunit = (TranslationUnit)
          ckpt.getLoop().getProcedure().getParent();
        System.err.println( tunit.getInputFilename() + ": " +
          ckpt.getLoop().where() + ": " +
          "no suitable safe point for inserting checkpoint" );
      }
    }
  }

  private void addCheckpointPragma( Statement stmt ) {
    CppcCheckpointPragma pragma = new CppcCheckpointPragma();
    CppcStatement cppcStatement = new CppcStatement( pragma );
    pragma.setParent( cppcStatement );

    CompoundStatement statementList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass( stmt, CompoundStatement.class );
    statementList.addStatementBefore( stmt, cppcStatement );
  }
}
