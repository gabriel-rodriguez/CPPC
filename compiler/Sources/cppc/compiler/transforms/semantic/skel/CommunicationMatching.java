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

import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.NullStatement;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;

import cppc.compiler.analysis.CommunicationAnalyzer;
import cppc.compiler.analysis.ExpressionAnalyzer;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.shared.comms.Communication;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class CommunicationMatching extends ProcedureWalker {

//   private Program program;
  private static final String passName = "[CommunicationMatching]";
  private static Map<Identifier,Set<Identifier>> communicationVariables =
    new HashMap<Identifier,Set<Identifier>>();
  private static boolean modified = true;

  protected CommunicationMatching( Program program ) {
    super( program );
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    if( ConfigurationManager.hasOption(
      ConfigurationManager.DISABLE_COMM_ANALYSIS ) ) {

      return;
    }

    // First, for each procedure, find comm-relevant variables
    CommunicationMatching transform = new CommunicationMatching( program );
    transform.start();

//     // Find which variables are global. Add them to all procedures in the
//     // communicationVariables map.
//     Set<Identifier> globalVariables = new HashSet<Identifier>();
//     for( Identifier procName: communicationVariables.keySet() ) {
//       Set<Identifier> localVariables = communicationVariables.get( procName
// );
//       Procedure proc = ObjectAnalizer.getProcedure( program, procName );
//       for( Identifier id: localVariables ) {
//         if( ObjectAnalizer.isGlobal( id, proc.getBody() ) ) {
//           globalVariables.add( id );
//         }
//       }
//     }
// 
//     Set<Identifier> remove = new HashSet<Identifier>();
//     for( Identifier procName: communicationVariables.keySet() ) {
//       Set<Identifier> localVariables = communicationVariables.get( procName
// );
//       for( Identifier global: globalVariables ) {
//         if( !localVariables.contains( global ) ) {
//           // Only add if this procedure modifies the variable
//           ProcedureCharacterization c =
// CppcRegisterManager.getCharacterization(
//             procName );
//           if( c.getVariableDependencies().containsKey( global ) ) {
//             localVariables.add( global );
//           }
// 
//           Procedure proc = ObjectAnalizer.getProcedure( program, procName );
//           CommunicationMatching.addVariableDependencies( proc, localVariables
// );
//         }
//       }
// 
//       // Finally, if this procedure has no variables to track, just remove it
//       // from the list
//       if( localVariables.isEmpty() ) {
//         remove.add( procName );
//       }
//     }

    // Remove from list procedures which have no variables to track
    Set<Identifier> remove = new HashSet<Identifier>();
    for( Identifier procName: communicationVariables.keySet() ) {
      if( communicationVariables.get( procName ).isEmpty() ) {
        remove.add( procName );
      }
    }

    for( Identifier procName: remove ) {
      communicationVariables.remove( procName );
    }

    // Now, find main procedure and begin actual comm analysis
    Procedure main = ObjectAnalizer.findMainProcedure( program );
    CommunicationAnalyzer.setCommunicationVariables( communicationVariables );
    CommunicationAnalyzer.analyze( main );

    Tools.printlnStatus( passName + " end", 1 );
  }

  protected void walkOverProcedure( Procedure procedure ) {
    if(communicationVariables.containsKey( (Identifier)procedure.getName())) {
      return;
    }

    Set<Identifier> commVariables = new HashSet<Identifier>();

    // First: analyze function calls fulfilling send, recv or wait roles.
    // Analyze their source, target and tag parameters and add consumed
    // identifiers to the commVariables list
    DepthFirstIterator iter = new DepthFirstIterator( procedure );
    while( iter.hasNext() ) {
      FunctionCall call = null;
      try {
        call = (FunctionCall)iter.next( FunctionCall.class );
      } catch( NoSuchElementException e ) {
        break;
      }

      if( Communication.sendFunctions.contains( call.getName() ) ||
        Communication.recvFunctions.contains( call.getName() ) ||
        Communication.waitFunctions.contains( call.getName() ) ) {

        Communication comm = Communication.fromCall( call );
        String sourceString = comm.getProperty( Communication.SOURCE );
        String targetString = comm.getProperty( Communication.DESTINATION );
        String tagString = comm.getProperty( Communication.TAG );
        String requestString = comm.getProperty( Communication.REQUEST );

        int sourcePos = 0, targetPos = 0, tagPos = 0, requestPos = 0;
        CppcStatement helper = null;
        if( sourceString != null ) {
          sourcePos = new Integer( sourceString ).intValue();
          helper = new CppcStatement( new NullStatement() );
          commVariables.addAll( ExpressionAnalyzer.analyzeExpression( helper,
            call.getArgument( sourcePos-1 ) ) );
          commVariables.addAll( helper.getConsumed() );
        }
        if( targetString != null ) {
          targetPos = new Integer( targetString ).intValue();
          helper = new CppcStatement( new NullStatement() );
          commVariables.addAll( ExpressionAnalyzer.analyzeExpression( helper,
            call.getArgument( targetPos-1 ) ) );
          commVariables.addAll( helper.getConsumed() );
        }
        if( tagString != null ) {
          tagPos = new Integer( tagString ).intValue();
          helper = new CppcStatement( new NullStatement() );
          commVariables.addAll( ExpressionAnalyzer.analyzeExpression( helper,
            call.getArgument( tagPos-1 ) ) );
          commVariables.addAll( helper.getConsumed() );
        }
        if( requestString != null ) {
          requestPos = new Integer( requestString ).intValue();
          helper = new CppcStatement( new NullStatement() );
//           commVariables.addAll( ExpressionAnalyzer.analyzeExpression( helper,
//             call.getArgument( requestPos-1 ) ) );
          // We add just possible indexes to a theoretical requests array. No
          // point adding the array itself since it is an output parameter.
          ExpressionAnalyzer.analyzeExpression( helper, call.getArgument(
            requestPos-1 ) );
          commVariables.addAll( helper.getConsumed() );
        }
      } else {
        // If this is a call to another function, we must:
        // 1. Get internal communication variables
        // 2. See which of them are parameters or global variables
        // 3. Add those to the communication variables
        Set<Identifier> internalVariables = communicationVariables.get(
          (Identifier)call.getName() );
        if( internalVariables == null ) {
          if( call.getProcedure() != null ) {
            this.walkOverProcedure( call.getProcedure() );
            internalVariables = communicationVariables.get(
              (Identifier)call.getName() );
          }
        }

        if( internalVariables != null ) {
          if( !internalVariables.isEmpty() ) {
            Set<Identifier> dependencies = new HashSet<Identifier>();
            for( Identifier id: internalVariables ) {
              ProcedureParameter param = ObjectAnalizer.idToProcParam(
                call.getProcedure().getParameters(), id, true );
              if( param != null ) {
                Expression expr = call.getArgument( param.getPosition() );
                CppcStatement nullStmt =
                  new CppcStatement( new NullStatement() );
                commVariables.addAll( ExpressionAnalyzer.analyzeExpression(
                  nullStmt, expr ) );
                commVariables.addAll( nullStmt.getConsumed() );
              } else {
                if( ObjectAnalizer.isGlobal( id,
                  call.getProcedure().getBody() ) ) {

                  commVariables.add( id );
                }
              }
            }
          }
        }
      }
    }

    // Second: for each called procedure, if it modifies a global commvariable
    // or a commvariable passed by parameter, infuse them into the callee
    // commvariables list
    ProcedureCharacterization procedureChar =
      CppcRegisterManager.getCharacterization( (Identifier)
      procedure.getName() );
    for( Identifier calleeName: procedureChar.getCalls() ) {
      Procedure callee = CppcRegisterManager.getCharacterization( calleeName
        ).getProcedure();


      for( Identifier id: commVariables ) {
        // If is global
        if( ObjectAnalizer.isGlobal( id, callee.getBody() ) ) {
          // If callee modifies it
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            (Identifier)callee.getName() );
//           if( c.getVariableDependencies().containsKey( id ) ) {
            communicationVariables.get( callee.getName() ).add( id );
//           }
        }
      }

      this.addVariableDependencies( callee, communicationVariables.get(
        callee.getName() ) );

      for( Identifier id: communicationVariables.get( callee.getName() ) ) {
        if( ObjectAnalizer.isGlobal( id, procedure.getBody() ) ) {
//           if( procedureChar.getVariableDependencies
          commVariables.add( id );
        }
      }
    }

    // Second: analyze the variable dependencies and iteratively add new ones
    // to the list, until no more are added.
    this.addVariableDependencies( procedure, commVariables );
    communicationVariables.put((Identifier)procedure.getName(), commVariables);
  }

  private static Set<Identifier> addVariableDependencies( Procedure procedure,
   Set<Identifier> vars ) {

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)procedure.getName() );

    int formerSize = 0;
    while( formerSize != vars.size() ) {
      formerSize = vars.size();
      Set<Identifier> add = new HashSet<Identifier>();
      for( Identifier id: vars ) {
        Set<Identifier> dependencies = c.getVariableDependencies( id );
        if( dependencies != null ) {
          add.addAll( dependencies );
        }
      }

      vars.addAll( add );
    }

    return vars;
   }

//   private void start() {
//     // Find main procedure
//     Procedure main = ObjectAnalizer.findMainProcedure( program );
//     this.analyzeProcedure( main );
//   }

//   private void analyzeProcedure( Procedure proc ) {
//     CommunicationAnalyzer.analyze( proc );
//   }
}
