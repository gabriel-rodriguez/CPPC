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




package cppc.compiler.fortran;

import cetus.hir.*;

import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.FormatStatement;
import cppc.compiler.cetus.IOCall;
import cppc.compiler.cetus.grammars.fortran77.FParser;
import cppc.compiler.utils.ObjectAnalizer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Stack;

public final class FortranPrintManager {

  private static final int FORTRAN_FIRST_COLUMN = 7;
  private static final int FORTRAN_LAST_COLUMN = 72;
  private static final int FORTRAN_LINE_SPAN = FORTRAN_LAST_COLUMN -
    FORTRAN_FIRST_COLUMN;
  private static final char[] FORTRAN_LINE_BREAKER_CHARS = { '=', ')', '*', '+',
    ',', '.', '/'};//, ' ' };

  private static String FORTRAN_COLUMN_FILLER;
  private static final String FORTRAN_LINE_CONTINUES = "+";

  private static boolean printing = true;
  private static Stack<String> includedFiles = new Stack<String>();

  private static Label lastSeenLabel = null;

  static {
    FORTRAN_COLUMN_FILLER = "";
    for( int i = 0; i < FORTRAN_FIRST_COLUMN-2; i++ ) {
      FORTRAN_COLUMN_FILLER += " ";
    }
  }

  private FortranPrintManager() {}

  public static void configureClasses() {

    Class[] params = { Object.class, OutputStream.class };
    Method printMethod = null;

    try {
      printMethod = FortranPrintManager.class.getMethod( "fortranPrint",
        params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }

    Annotation.setClassPrintMethod( printMethod );
    ArrayAccess.setClassPrintMethod( printMethod );
    BinaryOperator.setClassPrintMethod( printMethod );
    BooleanLiteral.setClassPrintMethod( printMethod );
    CommonBlock.setClassPrintMethod( printMethod );
    CompoundStatement.setClassPrintMethod( printMethod );
    ComputedGotoStatement.setClassPrintMethod( printMethod );
    ContinueStatement.setClassPrintMethod( printMethod );
    DeclarationStatement.setClassPrintMethod( printMethod );
    Declarator.setClassPrintMethod( printMethod );
    DoubleLiteral.setClassPrintMethod( printMethod );
    ExpressionStatement.setClassPrintMethod( printMethod );
    FormatStatement.setClassPrintMethod( printMethod );
    FortranDoLoop.setClassPrintMethod( printMethod );
    GotoStatement.setClassPrintMethod( printMethod );
    IfStatement.setClassPrintMethod( printMethod );
    IOCall.setClassPrintMethod( printMethod );
    Label.setClassPrintMethod( printMethod );
    NullStatement.setClassPrintMethod( printMethod );
    Procedure.setClassPrintMethod( printMethod );
    ReturnStatement.setClassPrintMethod( printMethod );
    Specifier.setClassPrintMethod( printMethod );
    StringLiteral.setClassPrintMethod( printMethod );
    UnaryOperator.setClassPrintMethod( printMethod );
    VariableDeclaration.setClassPrintMethod( printMethod );
    VariableDeclarator.setClassPrintMethod( printMethod );
    WhileLoop.setClassPrintMethod( printMethod );
  }

  public static void fortranPrint( Object obj, OutputStream stream ) {

    Class[] params = { obj.getClass(), OutputStream.class };

    try {
      if( printing ) {
        FortranPrintManager.class.getMethod( "fortranPrint", params ).invoke(
          null, obj, stream );
      } else {
        if( obj instanceof DeclarationStatement ) {
          fortranPrint( (DeclarationStatement)obj, stream );
        }
      }
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  public static void fortranPrint( Annotation annote, OutputStream stream ) {

    if( annote.getText().startsWith( FParser.INCLUDE_ANNOTE_TEXT ) ) {
      String aux = annote.getText();
      aux = aux.replaceFirst( FParser.INCLUDE_ANNOTE_TEXT, "" ).trim();
      if( includedFiles.isEmpty() ) {
        includedFiles.push( aux );
        PrintStream p = new PrintStream( stream );
        p.print( "INCLUDE '" + aux + "'" );
        printing = false;
      } else {
        if( includedFiles.peek().equals( aux ) ) {
          includedFiles.pop();
          printing = includedFiles.empty();
        } else {
          includedFiles.push( aux );
        }
      }
    }
  }

  public static void fortranPrint( ArrayAccess access, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    access.getArrayName().print( stream );
    p.print( "( " );
    for( int i = 0; i < access.getNumIndices(); i++ ) {
      access.getIndex( i ).print( stream );
      if( i != access.getNumIndices() - 1 ) {
        p.print( ", " );
      }
    }
    p.print( " )" );
  }

  public static void fortranPrint( BinaryOperator op, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    if( op == BinaryOperator.ADD ) {
      p.print( "+" );
      return;
    }

    if( op == BinaryOperator.MULTIPLY ) {
      p.print( "*" );
      return;
    }

    if( op == BinaryOperator.F_CONCAT ) {
      p.print( "//" );
      return;
    }

    if( op == BinaryOperator.DIVIDE ) {
      p.print( "/" );
      return;
    }

    if( op == BinaryOperator.SUBTRACT ) {
      p.print( "-" );
      return;
    }

    if( op == BinaryOperator.F_POWER ) {
      p.print( "**" );
      return;
    }

    if( op == AssignmentOperator.NORMAL ) {
      p.print( "=" );
      return;
    }

    if( op == BinaryOperator.COMPARE_EQ ) {
      p.print( ".EQ." );
      return;
    }

    if( op == BinaryOperator.COMPARE_GE ) {
      p.print( ".GE." );
      return;
    }

    if( op == BinaryOperator.COMPARE_GT ) {
      p.print( ".GT." );
      return;
    }

    if( op == BinaryOperator.COMPARE_LE ) {
      p.print( ".LE." );
      return;
    }

    if( op == BinaryOperator.COMPARE_LT ) {
      p.print( ".LT." );
      return;
    }

    if( op == BinaryOperator.COMPARE_NE ) {
      p.print( ".NE." );
      return;
    }

    if( op == BinaryOperator.LOGICAL_AND ) {
      p.print( ".AND." );
      return;
    }

    if( op == BinaryOperator.LOGICAL_OR ) {
      p.print( ".OR." );
      return;
    }
  }

  public static void fortranPrint( BooleanLiteral bool, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    if( bool.getValue() ) {
      p.print( ".TRUE." );
    } else {
      p.print( ".FALSE." );
    }
  }

  public static void fortranPrint( CommonBlock block, OutputStream stream ) {

    for( VariableDeclaration vd: block.getDeclarations() ) {
      fortranPrint( vd, stream );
    }
  }

  public static void fortranPrint( CompoundStatement stmt,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );
    Iterator iter = stmt.getChildren().iterator();

    while( iter.hasNext() ) {
      ((Statement)iter.next()).print( stream );
    }
  }

  public static void fortranPrint( ComputedGotoStatement stmt,
    OutputStream stream ) {

    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    ip.print( "GOTO ( " );
    Iterator iter = stmt.getChildren().iterator();
    while( iter.hasNext() ) {
      ((Printable)iter.next()).print( internalStream );
      if( iter.hasNext() ) {
        ip.print( ", " );
      }
    }

    ip.print( " ) " );
    stmt.getIndexer().print( internalStream );

    fortranPrintln( new PrintStream( stream ), internalStream.toString(),
      false );
  }

  public static void fortranPrint( ContinueStatement stmt, OutputStream stream ) {
    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    ip.print( "CONTINUE" );

    fortranPrintln( new PrintStream( stream ), internalStream.toString(), false );
  }

  public static void fortranPrint( CppcLabel label, OutputStream stream ) {
    fortranPrint( (Label)label, stream );
  }

  public static void fortranPrint( DeclarationStatement stmt,
    OutputStream stream ) {

    if( printing ) {
      ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
      stmt.getDeclaration().print( internalStream );
      // Non-cosmetic declarations do not print anything. Therefore,
      // we don't want to print a blank line for each of these.
      if( internalStream.size() != 0 ) {
        fortranPrintln( new PrintStream( stream ), internalStream.toString(),
          false );
      }
    } else {
      if( stmt.getDeclaration() instanceof Annotation ) {
        fortranPrint( (Annotation)stmt.getDeclaration(), stream );
      }
    }
  }

  public static void fortranPrint( Declarator decl, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( decl.getSymbol() );
    if( decl.getArraySpecifiers().size() != 0 ) {
      p.print( "( " );
      Iterator iter = decl.getArraySpecifiers().iterator();
      while( iter.hasNext() ) {
        ((Printable)iter.next()).print( stream );
        if( iter.hasNext() ) {
          p.print( ", " );
        }
      }
      p.print( " )" );
    }
  }

  public static void fortranPrint( DoubleLiteral dl, OutputStream stream ) {

    String value = new Double( dl.getValue() ).toString();

    if( value.contains("E") ) {
      value = value.replaceAll( "E", "d" );
    } else {
      value += "d0";
    }

    PrintStream p = new PrintStream( stream );
    p.print( value );
  }

  public static void fortranPrint( ExpressionStatement stmt,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );
    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();

    if( stmt.getExpression() instanceof FunctionCall ) {
      if( !(stmt.getExpression() instanceof IOCall ) ) {
        PrintStream ip = new PrintStream( internalStream );
        ip.print( "CALL " );
      }
    }
    stmt.getExpression().print( internalStream );

    fortranPrintln( p, internalStream.toString(), false );
  }

  public static void fortranPrint( FormatStatement stmt, OutputStream stream ) {

    boolean firstLine = true;
    PrintStream p = new PrintStream( stream );
    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    // As g77 doesn't seem to cope well with formats, we separate parameters in
    // different lines
    ip.print( "FORMAT( " );
    Iterator iter = stmt.getChildren().iterator();
    while( iter.hasNext() ) {
      ((Printable)iter.next()).print( internalStream );
      if( iter.hasNext() ) {
        ip.print( ", " );
        fortranPrintln( p, internalStream.toString(), !firstLine );
        firstLine=false;
        try {
          internalStream.flush();
        } catch( Exception e ) {
          e.printStackTrace();
        }
        internalStream.reset();
      }
    }
    ip.print( " )" );

    fortranPrintln( p, internalStream.toString(), !firstLine );
  }

  public static void fortranPrint( FortranDoLoop stmt, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    fortranPrintln( p, "", false );

    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    ip.print( "DO " );
    if( stmt.getLabel() != null ) {
      stmt.getLabel().print( internalStream );
      ip.print( ", " );
    }
    stmt.getLoopVar().print( internalStream );
    ip.print( " = " );
    stmt.getStart().print( internalStream );
    ip.print( ", " );
    stmt.getStop().print( internalStream );
    if( stmt.getStep() != null ) {
      ip.print( ", " );
      stmt.getStep().print( internalStream );
    }
    fortranPrintln( p, internalStream.toString(), false );
    try {
      internalStream.flush();
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }
    internalStream.reset();


    stmt.getBody().print( stream );

    if( stmt.getLabel() != null ) {
//       if( lastSeenLabel != null ) {
//         System.err.println( "BUG: CANNOT PRINT CONTINUE-LOOP LABELS USING "+
//           "CURRENT SCHEME" );
//         System.exit( 0 );
//       }
      lastSeenLabel = new Label( new Identifier( stmt.getLabel().toString() ) );
      ip.print( "CONTINUE" );
    } else {
      ip.print( "END DO" );
    }

    fortranPrintln( p, internalStream.toString(), false );

    fortranPrintln( p, "", false );
  }

  public static void fortranPrint( FunctionCall call, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    // If this is a Procedure: CALL
    Procedure proc = FortranProcedureManager.query(
      (Identifier)call.getName() );

    if( proc != null ) {
      if( proc.getReturnType().isEmpty() ) {
        p.print( "CALL " );
        FunctionCall.defaultPrint( call, stream );
        return;
      }
    }

    FunctionCall.defaultPrint( call, stream );
  }

  public static void fortranPrint( GotoStatement stmt, OutputStream stream ) {
    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    ip.print( "GOTO " );
    stmt.getValue().print( internalStream );

    fortranPrintln( new PrintStream( stream ), internalStream.toString(),
      false );
  }

  public static void fortranPrint( IOCall call, OutputStream stream ) {

    //PrintStream p = new PrintStream( stream );
    IOCall.defaultPrint( call, stream );
  }

  public static void fortranPrint( IfStatement stmt, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    fortranPrintln( p, "", false );

    // First case: IF ( condition ) statement
    if( stmt.getElseStatement() == null ) {
      if( !(stmt.getThenStatement() instanceof CompoundStatement ) ||
          (stmt.getThenStatement().getChildren().size() == 1) ) {

        Statement child =
          (Statement)stmt.getThenStatement().getChildren().get(0);
        if( !(child instanceof Loop) &&
          !(child instanceof IfStatement) ) {

          // Only case where we print a statement and not on a line alone: store
          // the label, then restore it
          Label curLabel = lastSeenLabel;
          lastSeenLabel = null;
          String statementText = stmt.getThenStatement().toString().trim();
          lastSeenLabel = curLabel;
          fortranPrintln( p, "IF ( " + stmt.getControlExpression() + " ) " +
            statementText, false );
          return;
        }
      }
    }

    // Second case: IF ( condition) THEN statementList
    fortranPrintln( p, "IF ( " + stmt.getControlExpression() + " ) THEN",
      false );
    stmt.getThenStatement().print( stream );
    while( stmt.getElseStatement() != null ) {
      // Third case: IF ( condition ) THEN statementList ELSE statementList
      if( (stmt.getElseStatement().getChildren().size() != 1) ||
        !(stmt.getElseStatement().getChildren().get( 0 )
          instanceof IfStatement ) ) {

        fortranPrintln( p, "ELSE", false );
        stmt.getElseStatement().print( stream );
        fortranPrintln( p, "END IF", false );
        fortranPrintln( p, "", false );
        return;
      } else {
        // Fourth case: IF ( condition ) THEN statementList ELSEIF ( condition )
        // THEN statementList ...
        IfStatement elseStatement =
          (IfStatement)stmt.getElseStatement().getChildren().get( 0 );
        fortranPrintln( p, "ELSEIF ( " + elseStatement.getControlExpression() +
          " ) THEN", false );
        elseStatement.getThenStatement().print( stream );
        stmt = (IfStatement)stmt.getElseStatement().getChildren().get( 0 );
      }
    }

    fortranPrintln( p, "END IF", false );
    fortranPrintln( p, "", false );
  }

  public static void fortranPrint( Label label, OutputStream stream ) {
    lastSeenLabel = label;
  }

  public static void fortranPrint( NullStatement stmt, OutputStream stream ) {}

  public static void fortranPrint( Procedure proc, OutputStream stream ) {

    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );
    String type;
    boolean explicitDeclarations = true;
    boolean programBody = false;

    if( ObjectAnalizer.isMainProcedure( proc ) ) {
      type = "PROGRAM ";
    } else {
      if( !proc.getReturnType().isEmpty() ) {

        // You cannot type functions which contain a typed variable called like
        // the function itself
        Declaration declaration = proc.getBody().findSymbol( proc.getName() );
        if( declaration instanceof Procedure ) {
          ((Specifier)proc.getReturnType().get( 0 ) ).print( internalStream );
          type = " FUNCTION ";
        } else {
          type = "FUNCTION ";
        }
      } else {
        type = "SUBROUTINE ";
      }
    }

    ip.print( type + proc.getName() );

    // Print procedure parameters in procedure header
    if( !proc.getParameters().isEmpty() || !proc.getReturnType().isEmpty() ) {
      ip.print( "( " );
      Iterator iter = proc.getParameters().iterator();
      while( iter.hasNext() ) {
        VariableDeclaration vd = (VariableDeclaration)iter.next();
        vd.getDeclarator( 0 ).getSymbol().print( internalStream );
        if( iter.hasNext() ) {
          ip.print( ", " );
        }
      }
      ip.print( " )" );
    }

    PrintStream p = new PrintStream( stream );
    fortranPrintln( p, internalStream.toString(), false );
    fortranPrintln( p, "", false );

    internalStream.reset();

    // Now begin printing the INCLUDE part
    CompoundStatement statementList = proc.getBody();
    Iterator children = statementList.getChildren().iterator();
    while( children.hasNext() ) {
      Printable child = (Printable)children.next();

      // Print procedure parameters declarations
      if( explicitDeclarations && printing &&
        (child instanceof Statement) ) {

        if( !(child instanceof DeclarationStatement) ||
          !(((DeclarationStatement)child).getDeclaration() instanceof
          Annotation ) ) {

          Iterator iter = proc.getParameters().iterator();
          if( iter.hasNext() ) {
            fortranPrintln( p, "", false ); //Cosmetic line
          }
          while( iter.hasNext() ) {
            new DeclarationStatement( (VariableDeclaration)iter.next() ).print(
              internalStream );
          }
          fortranPrintln( p, "", false );

          explicitDeclarations = false;
        }
      }

      if( !explicitDeclarations && printing && !programBody && !(child
        instanceof DeclarationStatement ) ) {

        fortranPrintln( p, "", false ); // Cosmetic line
        programBody = true;
      }

      child.print( stream );
    }

    fortranPrintln( p, "", false );
    fortranPrintln( p, "END " + type, false );
  }

  public static void fortranPrint( Program program, OutputStream stream ) {

    Iterator iter = program.getChildren().iterator();
    while( iter.hasNext() ) {
      TranslationUnit tunit = (TranslationUnit)iter.next();
      tunit.print( stream );
    }
  }

  public static void fortranPrint( ReturnStatement stmt, OutputStream stream ) {
    fortranPrintln( new PrintStream( stream ), "RETURN", false );
  }

  public static void fortranPrint( Specifier spec, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    if( spec == Specifier.BOOL ) {
      p.print( "LOGICAL" );
      return;
    }

    if( spec == Specifier.CHAR ) {
      p.print( "CHARACTER" );
      return;
    }

    if( spec == Specifier.DOUBLE ) {
      p.print( "REAL*8" );
      return;
    }

    if( spec == Specifier.FLOAT ) {
      p.print( "REAL*4" );
      return;
    }

    if( spec == Specifier.INT ) {
      p.print( "INTEGER*4" );
      return;
    }

    if( spec == Specifier.LONG ) {
      p.print( "INTEGER*8" );
      return;
    }
  }

  public static void fortranPrint( StringLiteral lit, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "'" );
    p.print( lit.getValue() );
    p.print( "'" );
  }

  public static void fortranPrint( UnaryOperator op, OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    if( op == UnaryOperator.MINUS ) {
      p.print( "-" );
      return;
    }

    if( op == UnaryOperator.LOGICAL_NEGATION ) {
      p.print( ".NOT." );
      return;
    }

    if( op == UnaryOperator.PLUS ) {
      p.print( "+" );
      return;
    }

    System.err.print( "Method "+
      "cppc.compiler.fortran.FortranPrintManager.fortranPrint not "+
      "implemented for UnaryOperator " );
    UnaryOperator.defaultPrint( op, System.err );
    System.err.println("");
    System.exit( 0 );
  }

  public static void fortranPrint( VariableDeclaration vd,
    OutputStream stream ) {}

  public static void fortranPrint( VariableDeclarator decl,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( decl.getSymbol() );
    if( decl.getArraySpecifiers().size() != 0 ) {
      p.print( "( " );
      Iterator iter = decl.getArraySpecifiers().iterator();
      while( iter.hasNext() ) {
        ((Printable)iter.next()).print( stream );
        if( iter.hasNext() ) {
          p.print( ", " );
        }
      }
      p.print( " )" );
    }
  }

  public static void fortranPrint( WhileLoop loop, OutputStream stream ) {
    PrintStream p = new PrintStream( stream );

    ByteArrayOutputStream internalStream = new ByteArrayOutputStream();
    PrintStream ip = new PrintStream( internalStream );

    ip.print( "DO WHILE( " );
    loop.getCondition().print( internalStream );
    ip.print( " )" );

    fortranPrintln( p, internalStream.toString(), false );
    try {
      internalStream.flush();
    } catch( Exception e ) {
      e.printStackTrace();
    } finally {
      internalStream.reset();
    }

    loop.getBody().print( stream );

    ip.print( "END DO" );
    fortranPrintln( p, internalStream.toString(), false );
  }

  private static void fortranPrintln( PrintStream p, String line,
    boolean continueLine ) {

    String [] subLines = line.split( "\n" );
    for( int i = 0; i < subLines.length; i++ ) {

      // If this line has already been broken ('+' char at column 6) just print
      if( subLines[i].startsWith( FORTRAN_COLUMN_FILLER +
        FORTRAN_LINE_CONTINUES ) ) {

        p.println( subLines[i] );
        continue;
      }

      // If the line is greater than the max size: break it
      if( subLines[i].length() > FORTRAN_LINE_SPAN ) {

        // Find proper place to break
        String searchString = subLines[i].substring( 0, FORTRAN_LINE_SPAN );
        int maxSplitIndex = -1;
        for( int j = 0; j < FORTRAN_LINE_BREAKER_CHARS.length; j++ ) {
          int splitIndex = searchString.lastIndexOf(
            FORTRAN_LINE_BREAKER_CHARS[ j ] );
          if( splitIndex > maxSplitIndex ) {
            maxSplitIndex = splitIndex;
          }
        }

        // If cannot find it, just choose the max line size
        if( maxSplitIndex == -1 ) {
          maxSplitIndex = FORTRAN_LINE_SPAN-1;
        }

        String firstPart = subLines[i].substring( 0, maxSplitIndex+1 );
        String secondPart = "";

        // If the first part ends in an unfinished string, close and continue
        // on secondPart
        if( firstPart.contains( "'" ) ) {
          int matches=0, lastMatch=-1;
          for( int j = 0; j < firstPart.length(); j++ ) {
            if( firstPart.charAt( j ) == '\'' ) {
              ++matches;
              lastMatch = j;
            }
          }

          if( matches%2 != 0 ) {
            // This means the line ends in an open string
            maxSplitIndex = FORTRAN_LINE_SPAN-3;
            firstPart = subLines[i].substring( 0, maxSplitIndex+1 ) + "',";
            secondPart = "'";
          }
        }

        secondPart += subLines[i].substring( maxSplitIndex+1 );
        fortranPrintln( p, firstPart, continueLine );
        fortranPrintln( p, secondPart, true );
      } else {
        if( continueLine ) {
          // If this is already a continuation, print "+" in column 6
          String filler = FORTRAN_LINE_CONTINUES; // + " ";
          p.println( FORTRAN_COLUMN_FILLER + filler + subLines[ i ] );
        } else {
          // Else, print the line
          String filler = " ";
          if( (lastSeenLabel == null) || (subLines[ i ].length() == 0) ) {
            p.println( FORTRAN_COLUMN_FILLER + filler + subLines[ i ] );
          } else {
            int size = lastSeenLabel.getName().toString().length();
            p.print( lastSeenLabel.getName() );
            lastSeenLabel = null;
            for( int j = size; j < FORTRAN_FIRST_COLUMN-1; j++ ) {
              p.print( filler );
            }
            p.println( subLines[ i ] );
          }
        }
      }
    }
  }
}
