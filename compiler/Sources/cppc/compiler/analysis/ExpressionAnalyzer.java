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

import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BooleanLiteral;
import cetus.hir.CharLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.EscapeLiteral;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Specifier;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ExpressionAnalyzer extends
  FunctionDispatcher<Expression> {

  private static Class instanceClass;
  private static ExpressionAnalyzer instance;
  protected static CppcStatement currentStatement = null;

  static {

    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
        GlobalNames.EXPRESSION_ANALYZER_CLASS_OPTION ) );
      instance = (ExpressionAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected ExpressionAnalyzer() {}

  public static Set<Identifier> analyzeExpression( CppcStatement s,
    Expression expr ) {

    Method m = instance.dispatch( expr, "analyzeExpression" );

    if( m == null ) {
      System.err.println( "WARNING: "+
        "cppc.compiler.analysis.ExpressionAnalyzer.analyzeExpression not "+
        "implemented for " + expr.getClass() );

      return createEmptySet();
    }

    CppcStatement oldStatement = currentStatement;
    currentStatement = s;
    try {
      return (Set<Identifier>)m.invoke( instance, expr );
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    } finally {
      currentStatement = oldStatement;
    }

    return createEmptySet();
  }

  protected static Set<Identifier> createEmptySet() {
    return new HashSet<Identifier>( 0 );
  }

  protected Set<Identifier> analyzeExpression( ArrayAccess expr ) {

    // Do not count variables consumed in the index
    long currentWeight = currentStatement.getWeight();

    // Process the Expression childs of this ArrayAccess adding consumed
    // identifiers to the set
    Set<Identifier> consumed = analyzeExpression( currentStatement,
      expr.getIndex( 0 ) );

    for( int i = 1; i < expr.getNumIndices(); i++ ) {
      consumed.addAll( analyzeExpression( currentStatement,
        expr.getIndex( i ) ) );
    }

    // Add consumed set to cppcStatement.consumed
    currentStatement.setWeight( currentWeight );
    currentStatement.getConsumed().addAll( consumed );

    // Return the expression that names this array
    return analyzeExpression( currentStatement, expr.getArrayName() );
  }

  protected Set<Identifier> analyzeExpression( AssignmentExpression expr ) {

    // Register the analysis of the RHS as consumed
    currentStatement.getConsumed().addAll( analyzeExpression( currentStatement,
      expr.getRHS() ) );

    // CONSERVATIVE STRATEGY: ARRAYS AND POINTERS ARE NEVER GENERATED (WE
    // NEED A LOT MORE ANALYSIS TO DETERMINE IF THEY NEED TO BE REGISTERED)

    // Register the analysis of the LHS as generated
    long currentWeight = currentStatement.getWeight();
    Set<Identifier> generated = analyzeExpression( currentStatement,
      expr.getLHS() );
    // Reset weight (lhs of assignments do not count)
    currentStatement.setWeight( currentWeight );

    currentStatement.getPartialGenerated().addAll( generated );
    checkGeneratedArrays( generated );
    currentStatement.getGenerated().addAll( generated );

    // If the operator is not '=', add generated to cppcStatement.consumed
    if( !expr.getOperator().equals( AssignmentOperator.NORMAL ) ) {
      currentStatement.getConsumed().addAll( generated );
    }

    // Return nothing (all identifiers have been already processed and included
    // in a set)
    return createEmptySet();
  }

  private static void checkGeneratedArrays( Set<Identifier> gen ) {
    Set<Identifier> remove = new HashSet<Identifier>( gen.size() );

    for( Identifier id: gen ) {
      VariableDeclaration vd = null;
      try {
        vd =
          LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
            currentStatement, id );
      } catch( SymbolNotDefinedException e ) {
        remove.add( id );
        continue;
      } catch( SymbolIsNotVariableException e ) {
        remove.add( id );
      }

      Declarator declarator = ObjectAnalizer.getDeclarator( vd, id );
      if( (declarator.getArraySpecifiers().size() != 0 ) ||
        (declarator.getSpecifiers().size() != 0 ) ) { // Pointer specifiers

        if( !checkFullyGeneratedArray( id ) ) {
          if( !checkLoopGeneratedArray( id ) ) {
            // PRUNE
            remove.add( id );
            continue;
          }
        }
      }
    }

    gen.removeAll( remove );
  }

  private static boolean checkFullyGeneratedArray( Identifier id ) {
    ArrayAccess access = (ArrayAccess)ObjectAnalizer.getParentOfClass( id,
      ArrayAccess.class );

    return (access == null);
  }

  private static boolean checkLoopGeneratedArray( Identifier id ) {
    ArrayAccess access = (ArrayAccess)ObjectAnalizer.getParentOfClass( id,
      ArrayAccess.class );
    cetus.hir.Statement stmt =
      (cetus.hir.Statement)ObjectAnalizer.getParentOfClass( id,
      cetus.hir.Statement.class );
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass(
      stmt, SymbolTable.class );

    if(
    !cppc.compiler.utils.language.LanguageAnalyzerFactory.getLanguageAnalyzer().
      insideLoop ( stmt ) ) {
      return false;
    }

    cppc.compiler.fortran.FortranDoLoop doLoop =
      (cppc.compiler.fortran.FortranDoLoop)ObjectAnalizer.getParentOfClass(
        stmt, cppc.compiler.fortran.FortranDoLoop.class );

    if( doLoop == null ) {
      return false;
    }

    java.util.ArrayList<cppc.compiler.fortran.FortranDoLoop> loops = new
      java.util.ArrayList<cppc.compiler.fortran.FortranDoLoop>(
        access.getNumIndices() );
    while( doLoop != null ) {
      loops.add( doLoop );
      doLoop =
        (cppc.compiler.fortran.FortranDoLoop)ObjectAnalizer.getParentOfClass(
          doLoop.getParent(), cppc.compiler.fortran.FortranDoLoop.class );
    }

    if( loops.size() != access.getNumIndices() ) {
      return false;
    }

    int i = access.getNumIndices() - 1;
    for( cppc.compiler.fortran.FortranDoLoop l: loops ) {

      if( !l.getLoopVar().equals( access.getIndex( i ) ) ) {
        return false;
      }

      VariableDeclaration vd = (VariableDeclaration)symbolTable.findSymbol(
        id );
      Declarator declarator = ObjectAnalizer.getDeclarator( vd, id );

      if( declarator.getArraySpecifiers().size() == 0 ) {
        return false;
      }

      if( (l.getStep() != null) && 
        !l.getStep().equals( new IntegerLiteral( 1 ) ) ) {
        return false;
      }

      java.util.List arraySpecs = declarator.getArraySpecifiers();
      cppc.compiler.fortran.FortranArraySpecifier spec =
        (cppc.compiler.fortran.FortranArraySpecifier)arraySpecs.get( i-- );

      if( !spec.getLowerBound().equals( l.getStart() ) ) {
        return false;
      }

      if( !spec.getUpperBound().equals( l.getStop() ) ) {
        return false;
      }
    }

    return true;
  }

  protected Set<Identifier> analyzeExpression( BinaryExpression expr ) {

    // A BinaryExpression that is not an AssignmentExpression can generate
    // or consume in both sides of the operator, and must return what it gets
    // from them
    Set<Identifier> resultSet = analyzeExpression( currentStatement,
      expr.getLHS() );
    resultSet.addAll( analyzeExpression( currentStatement,
      expr.getRHS() ) );

    return resultSet;
  }

  protected Set<Identifier> analyzeExpression( CharLiteral charLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( CommaExpression expr ) {
    Set<Identifier> resultSet = createEmptySet();
    Iterator iter = expr.getChildren().iterator();

    while( iter.hasNext() ) {
      resultSet.addAll( analyzeExpression( currentStatement,
      (Expression)iter.next() ) );
    }

    return resultSet;
  }

  protected Set<Identifier> analyzeExpression( DoubleLiteral doubleLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( EscapeLiteral escapeLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( FloatLiteral floatLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( FunctionCall functionCall ) {
    // If the called procedure is already registered, then we use the "cache"
    // Else, we need to generated these data
    if( !CppcRegisterManager.isRegistered(
      (Identifier)functionCall.getName() ) ) {

      Procedure procedure = functionCall.getProcedure();
      if( procedure == null ) {
        // A null procedure means that no code exists for it.
        // Using a conservative approach
        registerNullFunctionCall( functionCall );
      } else {
        ProcedureAnalyzer.analyzeProcedure( procedure );
      }
    }

    // If we have the code for the callee, add it to the calls list
    if( functionCall.getProcedure() != null ) {
      CppcRegisterManager.getCharacterization( (Identifier)
        currentStatement.getProcedure().getName() ).addCall( (Identifier)
        functionCall.getName() );
    }

    // Get what is registered in the CppcRegisterManager and translate it to
    // parameters
    ProcedureCharacterization characterization =
      CppcRegisterManager.getCharacterization(
        (Identifier)functionCall.getName() );

    // Add called from trace
    if( !characterization.isNull() ) {
      characterization.addCalledFrom( (Identifier)
        currentStatement.getProcedure().getName() );
    }

    Set<ProcedureParameter> localConsumed = characterization.getConsumed();
    Set<ProcedureParameter> localGenerated = characterization.getGenerated();

    // Translate ProcedureParameters instances to effective parameters passed by
    // the function call
    for( int i = 0; i < functionCall.getNumArguments(); i++ ) {

      Expression argument = functionCall.getArgument( i );
      Set<Identifier> ids = analyzeExpression( currentStatement, argument );
      ProcedureParameter aux = new ProcedureParameter( i );
      boolean found = false;

      // Input, output, and input-output normal parameters are added here
      if( localConsumed.contains( aux ) ) {
        for( Identifier id: ids ) {
          if( !currentStatement.getGenerated().contains( id ) ) {
            currentStatement.getConsumed().add( id );
          }
        }
        found = true;
      }

      if( localGenerated.contains( aux ) ) {
        currentStatement.getPartialGenerated().addAll( ids );
        checkGeneratedArrays( ids );
        currentStatement.getGenerated().addAll( ids );
        found =true;
      }

      // If 'found' is true, then it is not a VARARGS parameter
      if( found ) {
        continue;
      }

      // VARARGS parameters, search for ProcedureParameter.VARARGS
      if( localConsumed.contains( ProcedureParameter.VARARGS ) ) {
        currentStatement.getConsumed().addAll( ids );
      }
      if( localGenerated.contains( ProcedureParameter.VARARGS ) ) {
        currentStatement.getGenerated().addAll( ids );
        currentStatement.getPartialGenerated().addAll( ids );
      }
    }

    // Now for the Global Declarations part
    currentStatement.getGlobalConsumed().addAll(
      characterization.getGlobalConsumed() );
    currentStatement.getGlobalGenerated().addAll(
      characterization.getGlobalGenerated() );

    // Computational cost of call
    if( characterization.getProcedure() != null ) {
      currentStatement.setWeight( characterization.getWeight() );
    }
    currentStatement.statementCount = characterization.statementCount;

    // Procedures are not generated nor consumed, only called
    return createEmptySet();
  }

  protected static void registerNullFunctionCall( FunctionCall functionCall ) {

    // Issue a warning so the user knows that this could be better
    if( !functionCall.getName().toString().startsWith( "_" ) ) {
      System.out.println( "WARNING: Taking a conservative approach over "+
        functionCall.getName() + " function." );
      System.out.println( "\tReason  : Code not found." );
      System.out.println( "\tApproach: All parameters are consumed, none "+
        "generated." );
      System.out.println( "\tSolution: Implement or issue a petition over a "+
        "semantic module for this function's family." );
    }

    // Conservative approach: all parameters are consumed, none generated
    HashSet<ProcedureParameter> generated =
      new HashSet<ProcedureParameter>( 0 );
    HashSet<ProcedureParameter> consumed = new HashSet<ProcedureParameter>( 1 );

    // Adding ProcedureParameter.VARARGS to consumed will effectively mean that
    // all parameters are consumed
    consumed.add( ProcedureParameter.VARARGS );

    ProcedureCharacterization characterization = new ProcedureCharacterization(
      (Identifier)functionCall.getName() );
    characterization.setGenerated( generated );
    characterization.setConsumed( consumed );
    characterization.setNull( true );
    characterization.statementCount = 1;
    characterization.setWeight( functionCall.getNumArguments() );

    CppcRegisterManager.addProcedure( (Identifier)functionCall.getName(),
      characterization );
  }

  static java.util.Map knownTrash = new java.util.HashMap();
  protected Set<Identifier> analyzeExpression( Identifier identifier ) {
    Set<Identifier> resultSet = new HashSet<Identifier>( 1 );
    resultSet.add( identifier );

    // FIXME
//     long weight = 1;
//     Expression size =
//       cppc.compiler.utils.VariableSizeAnalizerFactory.getAnalizer().getSize(
//       identifier, identifier );
//     if( size != null ) {
//       Expression foldedSize = SymbolicExpressionAnalyzer.analyzeExpression(
//         size, knownTrash );
//       if( foldedSize instanceof IntegerLiteral ) {
//         weight = (int)((IntegerLiteral)foldedSize).getValue();
//       } else {
//         boolean found = false;
//         int notn = 1950;
//         int lgaus = 64;
//         int nmax = 10;
//         int note = 620;
//         int ngaus = 32;
//         int mgaus = 32;
//         int lgaus2 = 64;
//         int nlgaus = 64;
//         int strdim = 100;
//         int irestrtmax = notn*3*5/100;
//         int notncf = 75;
//         if( identifier.toString().equals( "NATNO" ) ||
//           identifier.toString().equals( "X" ) ||
//           identifier.toString().equals( "Y" ) ||
//           identifier.toString().equals( "Z" ) ||
//           identifier.toString().equals( "XF" ) ||
//           identifier.toString().equals( "YF" ) ||
//           identifier.toString().equals( "ZF" ) ||
//           identifier.toString().equals( "NZ" )||
//           identifier.toString().equals( "SIGXX" )||
//           identifier.toString().equals( "SIGYY" )||
//           identifier.toString().equals( "SIGZZ" )||
//           identifier.toString().equals( "SIGXY" )||
//           identifier.toString().equals( "SIGYZ" )||
//           identifier.toString().equals( "SIGZX" )||
//           identifier.toString().equals( "QPNODES" )) {
// 
//           weight = notn;
//           found = true;
//         }
//         if( identifier.toString().equals( "A1" ) ||
//           identifier.toString().equals( "A2" ) ||
//           identifier.toString().equals( "A3" ) ||
//           identifier.toString().equals( "A11" ) ||
//           identifier.toString().equals( "A22" ) ||
//           identifier.toString().equals( "A33" ) ||
//           identifier.toString().equals( "A12" ) ||
//           identifier.toString().equals( "A23" ) ||
//           identifier.toString().equals( "A31" ) ||
//           identifier.toString().equals( "W" ) ||
//           identifier.toString().equals( "TRACT" ) ||
//           identifier.toString().equals( "BT" )||
//           identifier.toString().equals( "B1" )||
//           identifier.toString().equals( "B2" )||
//           identifier.toString().equals( "B3" )||
//           identifier.toString().equals( "FI" )||
//           identifier.toString().equals( "FO" )||
//           identifier.toString().equals( "AO" )||
//           identifier.toString().equals( "B11" )||
//           identifier.toString().equals( "B22" )||
//           identifier.toString().equals( "B33" )||
//           identifier.toString().equals( "B12" )||
//           identifier.toString().equals( "B23" )||
//           identifier.toString().equals( "B31" )||
//           identifier.toString().equals( "C11" )||
//           identifier.toString().equals( "C22" )||
//           identifier.toString().equals( "C33" )||
//           identifier.toString().equals( "C12" )||
//           identifier.toString().equals( "C23" )||
//           identifier.toString().equals( "C31" )||
//           identifier.toString().equals( "M" )||
//           identifier.toString().equals( "YM" )||
//           identifier.toString().equals( "XM" )||
//           identifier.toString().equals( "AX" )||
//           identifier.toString().equals( "VV" )||
//           identifier.toString().equals( "AV" )||
//           identifier.toString().equals( "XX" )) {
// 
//           weight = notn*3;
//           found = true;
//         }
//         if( identifier.toString().equals( "GPP" ) ||
//           identifier.toString().equals( "GWW" ) ) {
// 
//           weight = lgaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "IPIV" ) ||
//           identifier.toString().equals( "INDXR" ) ||
//           identifier.toString().equals( "INDXC" ) ||
//           identifier.toString().equals( "INDX" )) {
// 
//           weight = nmax;
//           found = true;
//         }
//         if( identifier.toString().equals( "A" ) ) {
// 
//           weight = (int)Math.pow(notn*3,2);
//           found = true;
//         }
//         if( identifier.toString().equals( "MATNO" ) ||
//           identifier.toString().equals( "NPI" ) ||
//           identifier.toString().equals( "NPJ" ) ||
//           identifier.toString().equals( "NPK" ) ||
//           identifier.toString().equals( "NPL" ) ||
//           identifier.toString().equals( "NPM" ) ||
//           identifier.toString().equals( "NPN" ) ||
//           identifier.toString().equals( "NPP" ) ||
//           identifier.toString().equals( "NPQ" ) ||
//           identifier.toString().equals( "NPR" ) ||
//           identifier.toString().equals( "MNEL" ) ||
//           identifier.toString().equals( "NC1" ) ||
//           identifier.toString().equals( "MCOND" )||
//           identifier.toString().equals( "NSUBDEX" )||
//           identifier.toString().equals( "NXF" )) {
// 
//           weight = note;
//           found = true;
//         }
//         if( identifier.toString().equals( "XG" ) ||
//           identifier.toString().equals( "CG" ) ) {
// 
//           weight = ngaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "XG8" ) ||
//           identifier.toString().equals( "CG8" ) ) {
// 
//           weight = mgaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "GPP2" ) ||
//           identifier.toString().equals( "GWW2" ) ) {
// 
//           weight = lgaus2;
//           found = true;
//         }
//         if( identifier.toString().equals( "XGN" ) ||
//           identifier.toString().equals( "CGN" ) ) {
// 
//           weight = nlgaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "STRND" ) ) {
// 
//           weight = strdim;
//           found = true;
//         }
//         if( identifier.toString().equals( "UPRES" ) ) {
// 
//           weight = note*9;
//           found = true;
//         }
//         if( identifier.toString().equals( "NELM" ) ||
//           identifier.toString().equals( "TPRES" ) ||
//           identifier.toString().equals( "JT" )||
//           identifier.toString().equals( "ICOND" )) {
// 
//           weight = note*3*9;
//           found = true;
//         }
//         if( identifier.toString().equals( "DG" ) ||
//           identifier.toString().equals( "DH" ) ||
//           identifier.toString().equals( "SF" )||
//           identifier.toString().equals( "SFF" )||
//           identifier.toString().equals( "SFD" )) {
// 
//           weight = 8*ngaus*ngaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "DG9" ) ||
//           identifier.toString().equals( "DH9" )||
//           identifier.toString().equals( "SF9" )||
//           identifier.toString().equals( "SFCF" )) {
// 
//           weight = 9*ngaus*ngaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "SSF" ) ||
//           identifier.toString().equals( "SDG" )||
//           identifier.toString().equals( "SDH" )) {
// 
//           weight = 4*mgaus*mgaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "SSFN" ) ||
//           identifier.toString().equals( "SDGN" )||
//           identifier.toString().equals( "SDHN" )) {
// 
//           weight = 4*nlgaus*nlgaus;
//           found = true;
//         }
//         if( identifier.toString().equals( "S" ) ) {
// 
//           weight = irestrtmax;
//           found = true;
//         }
//         if( identifier.toString().equals( "SIFNODE" ) ||
//           identifier.toString().equals( "CFN" )) {
// 
//           weight = notncf*2;
//           found = true;
//         }
//         if( identifier.toString().equals( "NORMV" ) ||
//           identifier.toString().equals( "TEMP2" )) {
// 
//           weight = notncf*3*3;
//           found = true;
//         }
//         if( identifier.toString().equals( "SIF1" ) ||
//           identifier.toString().equals( "SIF2" )||
//           identifier.toString().equals( "SIF" )||
//           identifier.toString().equals( "TEMP" )||
//           identifier.toString().equals( "SIFV" )||
//           identifier.toString().equals( "PRPVCT" )) {
// 
//           weight = notncf*3;
//           found = true;
//         }
//         if( identifier.toString().equals( "CFNS" ) ||
//           identifier.toString().equals( "PRPK" )) {
// 
//           weight = notncf;
//           found = true;
//         }
// 
//         if( !found ) {
//           System.out.println( "Non identified variable: " + identifier );
//         }
//       }
//     }

    //FIXME
    currentStatement.setWeight( currentStatement.getWeight()+1 );
//     currentStatement.setWeight( currentStatement.getWeight()+weight );

    return resultSet;
  }

  protected Set<Identifier> analyzeExpression( BooleanLiteral booleanLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( IntegerLiteral integerLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( StringLiteral stringLiteral ) {
    return createEmptySet();
  }

  protected Set<Identifier> analyzeExpression( UnaryExpression expr ) {

    Expression expression = expr.getExpression();
    UnaryOperator operator = expr.getOperator();

    if( operator.equals( UnaryOperator.ADDRESS_OF ) ||
      operator.equals( UnaryOperator.DEREFERENCE ) ||
      operator.equals( UnaryOperator.MINUS ) ||
      operator.equals( UnaryOperator.PLUS ) ||
      operator.equals( UnaryOperator.LOGICAL_NEGATION ) ||
      operator.equals( UnaryOperator.BITWISE_COMPLEMENT ) ) {

      return analyzeExpression( currentStatement, expression );
    }

    if( operator.equals( UnaryOperator.POST_INCREMENT ) ||
      operator.equals( UnaryOperator.POST_DECREMENT ) ||
      operator.equals( UnaryOperator.PRE_INCREMENT ) ||
      operator.equals( UnaryOperator.PRE_DECREMENT ) ) {

      Set<Identifier> resultSet = analyzeExpression( currentStatement,
        expression );
      currentStatement.getConsumed().addAll( resultSet );
      currentStatement.getGenerated().addAll( resultSet );
      currentStatement.getPartialGenerated().addAll( resultSet );
      return createEmptySet();
    }

    if( operator.equals( UnaryOperator.LOGICAL_NEGATION ) ) {
      return analyzeExpression( currentStatement, expression );
    }

    System.err.println( "WARNING: "+
      "cppc.compiler.analysis.ExpressionAnalyzer.analyzeExpression( "+
      "CppcStatement, UnaryExpression ) not " +
      "implemented for UnaryOperator: " + operator );

    return createEmptySet();
  }
}
