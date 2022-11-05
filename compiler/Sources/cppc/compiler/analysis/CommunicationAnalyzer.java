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




package cppc.compiler.analysis;

import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.TranslationUnit;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.WhileLoop;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.transforms.shared.comms.Communication;
import cppc.compiler.transforms.shared.comms.CommunicationBuffer;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

public abstract class CommunicationAnalyzer extends
  FunctionDispatcher<Statement> {

  private static Class instanceClass;
  private static CommunicationAnalyzer instance;
  protected static CppcStatement currentStatement = null;

  protected static boolean symbolicAnalysis = true;
  protected static boolean analyzedStatement = false;

  static {

    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
        GlobalNames.COMMUNICATION_ANALYZER_CLASS_OPTION ) );
      instance = (CommunicationAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  // Symbolic expression analysis
  private static Map<Identifier,List> knownSymbols = new
    HashMap<Identifier,List>();

  private static CommunicationBuffer commBuffer = new CommunicationBuffer();

  private static Map<Identifier,Set<Identifier>> commVariables = new
    HashMap<Identifier,Set<Identifier>>();
  private static Set<Identifier> currentCommVariables = null;
  public static Set<Identifier> getCurrentCommunicationVariables() {
    return currentCommVariables;
  }
  public static void setCommunicationVariables(
    Map<Identifier,Set<Identifier>> commVariables ) {

    CommunicationAnalyzer.commVariables = commVariables;
  }

  protected CommunicationAnalyzer() {}

  public static void analyzeStatement( CppcStatement cppcStatement ) {
    Method m = instance.dispatch( cppcStatement.getStatement(),
      "analyzeStatement" );

    if( m == null ) {
      return;
    }

    // Safe point is checked before analyzing. Why, because we define the
    // boolean attribute as safety *before* the considered statement.
    boolean safePoint = !commBuffer.getPendingCommunications();
    CppcStatement oldStatement = currentStatement;
    currentStatement = cppcStatement;
    try {
      m.invoke( instance, cppcStatement.getStatement() );
    } catch( Exception e ) {
      e.printStackTrace();
    } finally {
      currentStatement.setSafePoint( safePoint );
      currentStatement = oldStatement;
    }
  }

  public static void analyze( Procedure proc ) {

    // Avoid null pointer exceptions for non available codes
    if( proc == null ) {
      return;
    }

    // Try to find communication data in the register manager
    CommunicationBuffer procBuffer = null;
    ProcedureCharacterization c = null;
    if( CppcRegisterManager.isRegistered( (Identifier)proc.getName() ) ) {
      c = CppcRegisterManager.getCharacterization( (Identifier)proc.getName() );
      procBuffer = c.getCommunicationBuffer();
    }

    // Optimize means not to analyze the procedure twice. This can only be done
    // if this procedure does not modify any comm variable which is global, or
    // use any parameter as comm variable
    boolean optimize = true;
    if( commVariables.get( (Identifier)proc.getName() ) != null ) {
      // If any of its commVariables gets modified, don't optimize
      for( Identifier id: commVariables.get( (Identifier)proc.getName() ) ) {
        if( c.getVariableDependencies().containsKey( id ) ) {
          optimize = false;
          break;
        }
      }
    }

    if( !optimize || (procBuffer == null) ) {
      // Save old current comm variables and set them. Also, copy global
      // variables to current set.
      Set<Identifier> oldCommVariables = currentCommVariables;
      currentCommVariables = commVariables.get( (Identifier)proc.getName() );
      if( (oldCommVariables != null) && (currentCommVariables != null) ) {
        for( Identifier id: oldCommVariables ) {
          if( ObjectAnalizer.isGlobal( id, proc.getBody() ) ) {
            currentCommVariables.add( id );
          }
        }
      }

      // Save old buffer and reset it
      CommunicationBuffer oldBuffer = commBuffer;
      commBuffer = new CommunicationBuffer( oldBuffer );

      // Save old symbolicAnalysis state
      boolean oldSymbolicAnalysis = symbolicAnalysis;
      symbolicAnalysis = true;

      // Analyze
      instance.analyzeStatement( proc.getBody() );

      // Restore symbolicAnalysis
      symbolicAnalysis = oldSymbolicAnalysis;

      // Restore buffer
      procBuffer = commBuffer;
      commBuffer = oldBuffer;

      // Restore comm variables
      currentCommVariables = oldCommVariables;

      // Insert data into characterization
      if( c == null ) {
        c = new ProcedureCharacterization( (Identifier)proc.getName() );
        CppcRegisterManager.addProcedure( (Identifier)proc.getName(), c );
      }

      if( optimize ) {
        c.setCommunicationBuffer( procBuffer );
      }
    }

    // Mix buffers
    boolean safe = !commBuffer.getPendingCommunications();
    CommunicationBuffer oldBuffer = (CommunicationBuffer)commBuffer.clone();
    instance.mixBuffers( commBuffer, procBuffer );

    // If it was not safe before the call, but is safe now, we need to
    // internally mark statements as safe.
    if( !safe && !commBuffer.getPendingCommunications() ) {
      commBuffer = oldBuffer;
      instance.analyzeStatement( proc.getBody() );
    }
  }

  protected void analyzeStatement( CompoundStatement compoundStatement ) {
    List children = compoundStatement.getChildren();
    for( int i = 0; i < children.size(); i++ ) {
      CppcStatement cppcStatement = (CppcStatement)children.get( i );

      analyzedStatement = false;
      if( this.symbolicAnalysis ) {
        if( currentCommVariables != null ) {
          SetOperations<Identifier> setOps = new SetOperations<Identifier>();
          if( !setOps.setIntersection( cppcStatement.getPartialGenerated(),
            currentCommVariables ).isEmpty() ) {

            SymbolicAnalyzer.analyzeStatement( cppcStatement, knownSymbols );
            analyzedStatement = true;
          }
        }
      }

      this.analyzeStatement( cppcStatement );
    }
  }

  protected void analyzeStatement( ExpressionStatement s ) {
    DepthFirstIterator iter = new DepthFirstIterator( s.getExpression() );
    while( iter.hasNext() ) {
      try {
        FunctionCall call = (FunctionCall)iter.next( FunctionCall.class );

        if( Communication.sendFunctions.contains( call.getName() ) ) {
          Communication send = Communication.fromCall( call );
          matchSend( send, commBuffer );
          return;
        }

        if( Communication.recvFunctions.contains( call.getName() ) ) {
          Communication recv = Communication.fromCall( call );
          matchRecv( recv, commBuffer );
          return;
        }

        if( Communication.waitFunctions.contains( call.getName() ) ) {
          Communication wait = Communication.fromCall( call );
          matchWait( wait, commBuffer );
          return;
        }

        if( Communication.rankerFunctions.contains( call.getName() ) ) {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            (Identifier)call.getName() );
          Integer valuePos = new Integer(
            c.getSemantic( Communication.RANKER ).get( Communication.RANK ) );

          Set<Identifier> rankIdSet = ExpressionAnalyzer.analyzeExpression(
            currentStatement, call.getArgument( valuePos.intValue()-1 ) );
          if( rankIdSet.size() != 1 ) {
            System.err.println( "ERROR: RANK VARIABLE NOT IDENTIFIED" );
            System.exit( 0 );
          }
          Identifier rankId = rankIdSet.iterator().next();
          Expression expr = this.buildNodesExpression( rankId );
          List<Expression> values = new ArrayList<Expression>( 1 );
          values.add( expr );

          SymbolicAnalyzer.addKnownSymbol( knownSymbols, rankId, values );
          return;
        }

        if( Communication.sizerFunctions.contains( call.getName() ) ) {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            (Identifier)call.getName() );
          Integer valuePos = new Integer(
            c.getSemantic( Communication.SIZER ).get( Communication.SIZE ) );

          Set<Identifier> sizeIdSet = ExpressionAnalyzer.analyzeExpression(
            currentStatement, call.getArgument( valuePos.intValue()-1 ) );
          if( sizeIdSet.size() != 1 ) {
            System.err.println( "ERROR: SIZE VARIABLE NOT IDENTIFIED" );
            System.exit( 0 );
          }
          Identifier sizeId = sizeIdSet.iterator().next();
          Expression expr = this.buildSizeExpression();
          List<Expression> values = new ArrayList<Expression>( 1 );
          values.add( expr );

          SymbolicAnalyzer.addKnownSymbol( knownSymbols, sizeId, values );
          return;
        }

        // Get procedure
        Procedure proc = call.getProcedure();
        if( proc == null ) {
          return;
        }

        // Handle known symbols
        Map<Identifier,List> inCallKnown = new HashMap<Identifier,List>();


        // Copy known comm variables
        Set<Identifier> subCommVariables = commVariables.get( call.getName() );
        SymbolTable localTable = (SymbolTable)ObjectAnalizer.getParentOfClass(
          s, SymbolTable.class );
        SymbolTable subTable = (SymbolTable)proc.getBody();
        List parameters = proc.getParameters();

        if( subCommVariables != null ) {
          for( Identifier id: subCommVariables ) {
            // Global variables
            if( (ObjectAnalizer.isGlobal( id, subTable ) ||
              ObjectAnalizer.isGlobal( id, localTable ) ) &&
              knownSymbols.containsKey( id ) ) {

              inCallKnown.put( id, knownSymbols.get( id ) );
            } else {
              // Parameters
              ProcedureParameter param = ObjectAnalizer.idToProcParam(
                call.getProcedure().getParameters(), id, true );
              if( param != null ) {
                Expression expr = call.getArgument( param.getPosition() );
                Expression folded =
                  SymbolicExpressionAnalyzer.analyzeExpression( expr, 
                  knownSymbols );
                if( (folded instanceof Literal) ||
                  (folded instanceof MultiExpression) ) {

                  List values = new ArrayList( 1 );
                  values.add( folded );
                  SymbolicAnalyzer.addKnownSymbol( inCallKnown, id, values,
                    proc.getBody() );
                }
              }
            }
          }
        }

        // Replace known symbols for the analysis
        Map<Identifier,List> oldSymbols = knownSymbols;
        knownSymbols = inCallKnown;

        // Analyze
        this.analyze( proc );

        // Exit: remove generated parameters and generated global variables
        ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
          (Identifier)call.getName() );

        // Copy generated parameters
        for( ProcedureParameter param: c.getGenerated() ) {
          Expression expr = null;
          try {
            expr = call.getArgument( param.getPosition() );
          } catch( Exception e ) {
            // Someone screwed up with variable or procedure definitions
            continue;
          }
          if( expr instanceof Identifier ) {
            Identifier parameterName = (Identifier)
              ((VariableDeclaration)parameters.get(
                param.getPosition() )).getDeclarator( 0 ).getSymbol();
            if( knownSymbols.containsKey( parameterName ) ) {
              oldSymbols.put( (Identifier)expr, knownSymbols.get(
                parameterName ) );
            }
          }
        }

        // Copy modified global variables
        for( Identifier id: (Set<Identifier>)knownSymbols.keySet() ) {
          if( ObjectAnalizer.isGlobal( id, localTable ) ||
            ObjectAnalizer.isGlobal( id, subTable ) ) {

            if( knownSymbols.containsKey( id ) ) {
              oldSymbols.put( id, knownSymbols.get( id ) );
            }
          }
        }

        // Recover old symbols
        knownSymbols = oldSymbols;
      } catch( NoSuchElementException e ) {}
    }
  }

  protected void analyzeStatement( IfStatement s ) {
    CommunicationBuffer originalBuffer = commBuffer;
    Expression condition = s.getControlExpression();
    commBuffer = new CommunicationBuffer( originalBuffer );
    symbolicAnalysis = false;

    // Analyze then part
    this.analyzeStatement( (CppcStatement)s.getThenStatement() );
    for( Communication comm: commBuffer.getAll() ) {
      comm.addCondition( (Expression)condition.clone() );
    }

    // If there is an else statement, analyze it
    if( s.getElseStatement() != null ) {
      CommunicationBuffer thenBuffer = commBuffer;
      commBuffer = new CommunicationBuffer( originalBuffer );

      // Analyze else part
      this.analyzeStatement( (CppcStatement)s.getElseStatement() );
      for( Communication comm: commBuffer.getAll() ) {
        comm.addCondition( new UnaryExpression( UnaryOperator.LOGICAL_NEGATION,
          (Expression)condition.clone() ) );
      }

      // Mix both branches
      this.mixBuffers( thenBuffer, commBuffer );
      commBuffer = thenBuffer;
    }

    // Mix buffers
    this.mixBuffers( originalBuffer, commBuffer );
    commBuffer = originalBuffer;

    // Perform symbolic analysis
    SymbolicAnalyzer.analyzeStatement( currentStatement, knownSymbols );
    symbolicAnalysis = true;
  }

  protected void analyzeStatement( WhileLoop l ) {
    this.analyzeStatement( (Loop)l );
  }

  protected void analyzeStatement( Loop l ) {
    if( !this.analyzedStatement ) {
      SymbolicAnalyzer.enterLoop( l, knownSymbols );
      this.analyzeStatement( (CppcStatement)l.getBody() );
      SymbolicAnalyzer.exitLoop( l );
    }
  }

  private void matchSend( Communication send, CommunicationBuffer buffer ) {
    Communication match = null;

    // Fold the tag
    Expression tag = send.getCallValue( Communication.TAG );
    Expression foldedTag = SymbolicExpressionAnalyzer.analyzeExpression( tag,
      knownSymbols );

    if( foldedTag instanceof MultiExpression ) {
      MultiExpression mexpr = (MultiExpression)foldedTag;

      for( Expression key: mexpr.getValueSet() ) {
        Expression value = mexpr.getValue( key );
        Communication clone = (Communication)send.clone();
        Expression cloneExpr = clone.getCallValue( Communication.TAG );
        cloneExpr.swapWith( (Expression)value.clone() );
        this.matchSend( clone, buffer );
      }

      return;
    }

    send.setExpressionProperty( Communication.TAG, foldedTag );

    // Fold the request, if it exists
    try {
      Expression request = send.getCallValue( Communication.REQUEST );
      Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(
        request, knownSymbols );
      send.setExpressionProperty( Communication.REQUEST, foldedRequest );
    } catch( Exception e ) {
      //No request could be extracted
    }

    // Fold the target
    Expression target = send.getCallValue( Communication.DESTINATION );
    Expression foldedTarget = SymbolicExpressionAnalyzer.analyzeExpression(
      target, knownSymbols );
    send.setExpressionProperty( Communication.DESTINATION, foldedTarget );

    // Look for the first-in-queue matching recv
    for( Communication recv: buffer.getUnmatchedRecvs() ) {
      if( this.sendRecvMatch( send, recv ) ) {
        this.updateStatements( send, recv );
        match = recv;
        break;
      }
    }

    // If found, handle it
    if( match != null ) {
      boolean blocking = new Boolean( send.getProperty(
        Communication.BLOCKING ) ).booleanValue();
      boolean blockingMatch = new Boolean( match.getProperty(
        Communication.BLOCKING ) ).booleanValue();

      // Remove recv from buffer
      buffer.getUnmatchedRecvs().remove( match );

      // If send or receive are nonblocking, handle waits
      Queue<Communication> nonblocking = new LinkedList<Communication>();
      if( !blocking ) {
        nonblocking.add( send );
      }
      if( !blockingMatch ) {
        nonblocking.add( match );
      }
      this.processNonBlocking( nonblocking, buffer );

//       // If send is not blocking, handle waits
//       if( !blocking ) {
//         this.processNonBlocking( send, buffer, true );
//       }
// 
//       // If recv is not blocking, handle waits
//       if( !blockingMatch ) {
//         this.processNonBlocking( match, buffer, true );
//       }
    } else {
      // Add to unmatched sends
      buffer.getUnmatchedSends().add( send );
    }
  }

  private void matchRecv( Communication recv, CommunicationBuffer buffer ) {
    Communication match = null;

    // Fold the tag
    Expression tag = recv.getCallValue( Communication.TAG );
    if( tag.equals( new Identifier( "MPI_ANY_TAG" ) ) ) {
      System.err.println( "Error: line " +
        recv.getCall().getStatement().where() + " : " +
        "communication analysis does not yet support use of MPI_ANY_TAG." );
      System.exit( 0 );
    }
    Expression foldedTag = SymbolicExpressionAnalyzer.analyzeExpression( tag,
      knownSymbols );

    if( foldedTag instanceof MultiExpression ) {
      MultiExpression mexpr = (MultiExpression)foldedTag;

      for( Expression key: mexpr.getValueSet() ) {
        Expression value = mexpr.getValue( key );
        Communication clone = (Communication)recv.clone();
        Expression cloneExpr = clone.getCallValue( Communication.TAG );
        cloneExpr.swapWith( (Expression)value.clone() );
        this.matchRecv( clone, buffer );
      }

      return;
    }

    recv.setExpressionProperty( Communication.TAG, foldedTag );

    // Fold the request, if it exists
    try {
      Expression request = recv.getCallValue( Communication.REQUEST );
      Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(
        request, knownSymbols );
      recv.setExpressionProperty( Communication.REQUEST, foldedRequest );
    } catch( Exception e ) {
      //No request could be extracted
    }

    // Fold the target
    Expression source = recv.getCallValue( Communication.SOURCE );
    if( !source.equals( new Identifier( "MPI_ANY_SOURCE" ) ) ) {
      Expression foldedSource = SymbolicExpressionAnalyzer.analyzeExpression(
        source, knownSymbols );
      recv.setExpressionProperty( Communication.SOURCE, foldedSource );
    } else {
      recv.setExpressionProperty( Communication.SOURCE, source );
    }

    // Look for the first-in-queue matching send
    for( Communication send: buffer.getUnmatchedSends() ) {
      if( this.sendRecvMatch( send, recv ) ) {
        this.updateStatements( send, recv );
        match = send;
        break;
      }
    }

    // If found, handle it
    if( match != null ) {
      boolean blocking = new Boolean( recv.getProperty(
        Communication.BLOCKING ) ).booleanValue();
      boolean blockingMatch = new Boolean( match.getProperty(
        Communication.BLOCKING ) ).booleanValue();

      // Remove send from buffer
      buffer.getUnmatchedSends().remove( match );

      // If any is nonblocking, handle waits
      Queue<Communication> nonblocking = new LinkedList<Communication>();
      if( !blocking ) {
        nonblocking.add( recv );
      }
      if( !blockingMatch ) {
        nonblocking.add( match );
      }
      this.processNonBlocking( nonblocking, buffer );

//       // If recv is not blocking, handle waits
//       if( !blocking ) {
//         this.processNonBlocking( recv, buffer );
//       }
// 
//       // If send is not blocking, handle waits
//       if( !blockingMatch ) {
//         this.processNonBlocking( match, buffer );
//       }
    } else {
      // Add to unmatched recvs
      buffer.getUnmatchedRecvs().add( recv );
    }
  }

  private void matchWait( Communication wait, CommunicationBuffer buffer ) {
    // Fold the request
    Expression request = wait.getCallValue( Communication.REQUEST );
    Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(
      request, knownSymbols );
    wait.setExpressionProperty( Communication.REQUEST, foldedRequest );

    // A wait can be a collective operation, and therefore have more than one
    // match
    List<Communication> matches = new ArrayList<Communication>(
      buffer.getUnwaitedComms().size() );
    boolean collective = Communication.COLLECTIVE.equals( wait.getProperty(
      Communication.TYPE ) );

    // Look for matching unwaited communications
    for( Communication comm: buffer.getUnwaitedComms() ) {
      if( commWaitMatch( comm, wait, collective ) ) {
        matches.add( comm );
        this.updateStatements( comm, wait );
      }
    }

    // If no matches, add to unmatched waits
    if( matches.isEmpty() ) {
//       if( collective ) {
//         // At the moment, no waitalls allowed in unmatched waits
//         CppcStatement cppcWait = (CppcStatement)
//           wait.getCall().getStatement().getParent();
// 
//         TranslationUnit tunit = (TranslationUnit)
//           cppcWait.getProcedure().getParent();
//         System.err.println( tunit.getInputFilename() + ": " +
//           cppcWait.where() + ": " +
//           "error: could not match collective wait function." );
//         System.exit( 0 );
//       }

      buffer.getUnmatchedWaits().add( wait );
    } else {
      // Remove comms from unwaited ones queue
      for( Communication comm: matches ) {
        buffer.getUnwaitedComms().remove( comm );
      }
    }
  }

  private void processNonBlocking( Queue<Communication> comms,
    CommunicationBuffer buffer ) {

    Set<Communication> matches = new HashSet();
    for( Communication comm: comms ) {
      Communication match = this.processNonBlocking( comm, buffer );
      if( match != null ) {
        matches.add( match );
      }
    }

    for( Communication match: matches ) {
      buffer.getUnmatchedWaits().remove( match );
    }
  }

  private Communication processNonBlocking( Communication comm,
    CommunicationBuffer buffer ) {

    Communication match = null;
    for( Communication wait: buffer.getUnmatchedWaits() ) {
      boolean collective = Communication.COLLECTIVE.equals(
        wait.getProperty( Communication.TYPE ) );

      if( commWaitMatch( comm, wait, collective ) ) {
        this.updateStatements( comm, wait );
        match = wait;
        break;
      }
    }

    if( match == null ) {
      buffer.getUnwaitedComms().add( comm );
    }

    return match;
  }

  private boolean sendRecvMatch( Communication send, Communication recv ) {
    // First: communicators must be the same
    Expression sendCommunicator = send.getCallValue(Communication.COMMUNICATOR);
    Expression recvCommunicator = send.getCallValue(Communication.COMMUNICATOR);
    if( !sendCommunicator.equals( recvCommunicator ) ) {
      return false;
    }

    // Second: check TAG values
    Expression sendTag = send.getExpressionProperty( Communication.TAG );
    Expression recvTag = recv.getExpressionProperty( Communication.TAG );

    if( !sendTag.equals( recvTag ) ) {
      // Assume match to be false under these circumstances
      return false;
    }

    // Third: check source-destination values
    Expression target = send.getExpressionProperty( Communication.DESTINATION );
    Expression source = recv.getExpressionProperty( Communication.SOURCE );

    if( source.equals( new Identifier( "MPI_ANY_SOURCE" ) ) ) {
      // This matches, quite clearly
      return true;
    }

    // For each i in nodes, i = source(target(i)) : the source of a
    // communication in the target of that communication for process i must be
    // process i
    Expression nodes = this.buildNodesExpression( null );

    for( Expression i: ((MultiExpression)nodes).getValueSet() ) {
      try {
        Expression targetProcess =
          ((MultiExpression)target).getValue( i );
        Expression sourceProcess = ((MultiExpression)source).getValue(
          targetProcess );
        if( !i.equals( sourceProcess ) ) {
          return false;
        }
      } catch( ClassCastException e ) {
      } catch( Exception e ) {
        // If there's an exception, it means that some communication is not
        // issued for some process. As long as the Symbolic analysis is
        // correctly performed, this will only happen if a communication is not
        // issued due to conditionals. We are not checking for that right now,
        // but it should be safe to ignore the exception since the symbolic
        // analysis works fine.
      }
    }

    // If no problems were found, the communications match
    return true;
  }

  private boolean commWaitMatch( Communication comm, Communication wait,
    boolean collective ) {

    Expression waitRequest = wait.getExpressionProperty(
      Communication.REQUEST );
    Expression commRequest = comm.getExpressionProperty(
      Communication.REQUEST );

    if( !collective ) {
      return waitRequest.equals( commRequest );
    }

    waitRequest = ObjectAnalizer.getBaseIdentifier( waitRequest );
    commRequest = ObjectAnalizer.getBaseIdentifier( commRequest );
    return waitRequest.equals( commRequest );
  }

  private void removeRedundant( CommunicationBuffer oldBuffer,
    CommunicationBuffer newBuffer ) {

    // Build a cloned buffer retaining redundant comms
    CommunicationBuffer redundant = (CommunicationBuffer)oldBuffer.clone();
    redundant.getUnmatchedSends().retainAll( newBuffer.getUnmatchedSends() );
    redundant.getUnmatchedRecvs().retainAll( newBuffer.getUnmatchedRecvs() );
    redundant.getUnwaitedComms().retainAll( newBuffer.getUnwaitedComms() );
    redundant.getUnmatchedWaits().retainAll( newBuffer.getUnmatchedWaits() );

    // Remove from redundant buffer those which have no condition
    for( Communication comm: redundant.getAll() ) {
      if( comm.getConditions().size() == 0 ) {
        redundant.remove( comm );
      }
    }

    // Process redundant comms
    this.removeRedundant( redundant.getUnmatchedSends(),
      newBuffer.getUnmatchedSends() );
    this.removeRedundant( redundant.getUnmatchedRecvs(),
      newBuffer.getUnmatchedRecvs() );
    this.removeRedundant( redundant.getUnwaitedComms(),
      newBuffer.getUnwaitedComms() );
    this.removeRedundant( redundant.getUnmatchedWaits(),
      newBuffer.getUnmatchedWaits() );
  }

  private void removeRedundant( Queue<Communication> redundant,
    Queue<Communication> newBuffer ) {

    List<Communication> remove = new ArrayList<Communication>(redundant.size());

    for( Communication oldComm: redundant ) {
      for( Communication newComm: newBuffer ) {
        if( oldComm.equals( newComm ) ) {
          if( this.incompatible( oldComm, newComm ) ) {
            remove.add( newComm );
          }
        }
      }
    }

    redundant.removeAll( remove );
    newBuffer.removeAll( remove );
  }

  private boolean incompatible( Communication lhs, Communication rhs ) {
    for( Expression lhsCondition: lhs.getConditions() ) {
      for( Expression rhsCondition: rhs.getConditions() ) {
        if( this.incompatible( lhsCondition, rhsCondition ) ) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean incompatible( Expression lhs, Expression rhs ) {

    // If one of them is the other with a NOT, then it is true
    if( lhs instanceof UnaryExpression ) {
      UnaryExpression safeLhs = (UnaryExpression)lhs;
      if( safeLhs.getOperator() == UnaryOperator.LOGICAL_NEGATION ) {
        if( safeLhs.getExpression().equals( rhs ) ) {
          return true;
        }
      }
    }

    // Same, changing sides
    if( rhs instanceof UnaryExpression ) {
      UnaryExpression safeRhs = (UnaryExpression)rhs;
      if( safeRhs.getOperator() == UnaryOperator.LOGICAL_NEGATION ) {
        if( safeRhs.getExpression().equals( lhs ) ) {
          return true;
        }
      }
    }

    // Check if both are equalities over the same identifier
    if( (lhs instanceof BinaryExpression) &&
      (rhs instanceof BinaryExpression) ) {

      BinaryExpression safeLhs = (BinaryExpression)lhs;
      BinaryExpression safeRhs = (BinaryExpression)rhs;

      if( (safeLhs.getOperator() == BinaryOperator.COMPARE_EQ) &&
        (safeRhs.getOperator() == BinaryOperator.COMPARE_EQ) ) {

        Expression lhsValue = null;
        Expression rhsValue = null;
        if( safeLhs.getLHS().equals( safeRhs.getLHS() ) ) {
          lhsValue = safeLhs.getRHS();
          rhsValue = safeRhs.getRHS();
        }

        if( safeLhs.getLHS().equals( safeRhs.getRHS() ) ) {
          lhsValue = safeLhs.getRHS();
          rhsValue = safeRhs.getLHS();
        }

        if( safeLhs.getRHS().equals( safeRhs.getLHS() ) ) {
          lhsValue = safeLhs.getLHS();
          rhsValue = safeRhs.getRHS();
        }

        if( lhsValue != null ) {
          if( !lhsValue.equals( rhsValue ) ) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private void mixBuffers( CommunicationBuffer oldBuffer,
    CommunicationBuffer newBuffer ) {

    // If no new comms: return
    if( newBuffer.isEmpty() ) {
      return;
    }

    // Remove communications that are the same, just on incompatible execution
    // branches
    this.removeRedundant( oldBuffer, newBuffer );

    // Unmatched sends
    for( Communication newSend: newBuffer.getUnmatchedSends() ) {
      this.matchSend( newSend, oldBuffer );
    }

    // Unmatched recvs
    for( Communication newRecv: newBuffer.getUnmatchedRecvs() ) {
      this.matchRecv( newRecv, oldBuffer );
    }

    // Unwaited comms
//     for( Communication newComm: newBuffer.getUnwaitedComms() ) {
//       this.processNonBlocking( newComm, oldBuffer );
//     }
    this.processNonBlocking( newBuffer.getUnwaitedComms(), oldBuffer );

    // Unmatched waits
    for( Communication newWait: newBuffer.getUnmatchedWaits() ) {
      this.matchWait( newWait, oldBuffer );
    }
  }

  private void updateStatements( Communication source, Communication target ) {
    CppcStatement cppcSource = (CppcStatement)
      source.getCall().getStatement().getParent();
    CppcStatement cppcTarget = (CppcStatement)
      target.getCall().getStatement().getParent();

    cppcSource.getMatchingCommunications().add( target );
    cppcTarget.getMatchingCommunications().add( source );
  }

  private Expression buildNodesExpression( Identifier id ) {
    Integer procNumber = null;
    try {
      procNumber = new Integer( ConfigurationManager.getOption(
        ConfigurationManager.PROCESS_NUMBER_OPTION ) );
    } catch( Exception e ) {
      procNumber = new Integer( 1 );
    }

    MultiExpression mexpr = new MultiExpression( id );
    for( int i = 0; i < procNumber.intValue(); i++ ) {
      IntegerLiteral rank = new IntegerLiteral( i );
      mexpr.addExpression( rank, rank );
    }

    return mexpr;
  }

  private Expression buildSizeExpression() {
    Integer procNumber = null;
    try {
      procNumber = new Integer( ConfigurationManager.getOption(
        ConfigurationManager.PROCESS_NUMBER_OPTION ) );
    } catch( Exception e ) {
      procNumber = new Integer( 1 );
    }

    return new IntegerLiteral( procNumber.intValue() );
  }
}
