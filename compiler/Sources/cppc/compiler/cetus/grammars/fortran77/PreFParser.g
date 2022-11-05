header {
    package cppc.compiler.cetus.grammars.fortran77;
}

{
import cetus.hir.ArraySpecifier;
import cetus.hir.CompoundStatement;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.ProcedureDeclarator;
import cetus.hir.Specifier;
import cetus.hir.TranslationUnit;

import cppc.compiler.fortran.FortranProcedureManager;
import cppc.compiler.fortran.ImplicitDeclarationManager;

import java.util.ArrayList;
import java.util.List;
}

class PreFParser extends Parser;

options {
  k=2;
  importVocab=FLexer;
}

{
  FLexer curLexer = null;

  public FLexer getLexer() {
    return curLexer;
  }

  public void setLexer( FLexer lexer ) {
    curLexer = lexer;
  }
}

// Program
// Translation Unit
translationUnit [TranslationUnit init_tunit]
: ( mainProgram
  | subroutineSubprogram
  | functionSubprogram
  | Newline )*
;

mainProgram { Identifier name=null; }

: name=programStatement subprogramBody endProgramStatement {

  ProcedureDeclarator declarator = new ProcedureDeclarator( name,
    new ArrayList() );
  Procedure procedure = new Procedure( declarator, new CompoundStatement() );
  FortranProcedureManager.addRegister( name, procedure );
}
;

subroutineSubprogram { Identifier name = null; }

: name=subroutineStatement subprogramBody endSubroutineStatement {

  ProcedureDeclarator declarator = new ProcedureDeclarator( name,
    new ArrayList() );
  Procedure procedure = new Procedure( declarator, new CompoundStatement() );
  FortranProcedureManager.addRegister( name, procedure );
}
;

functionSubprogram { Identifier name = null; ArrayList<Specifier> spec = null; }

: (spec=specifier)? name=functionStatement subprogramBody endFunctionStatement {

  if( spec == null ) {
    spec = new ArrayList<Specifier>( 1 );
    spec.add( ImplicitDeclarationManager.getType( name ) );
  }
  ProcedureDeclarator declarator = new ProcedureDeclarator( name,
    new ArrayList() );
  Procedure procedure = new Procedure( (Specifier)spec.get( 0 ), declarator,
    new CompoundStatement() );
  FortranProcedureManager.addRegister( name, procedure );
}
;

programStatement returns [ Identifier id = null ]
: PROGRAM id=identifier Newline
;

endProgramStatement { Identifier id = null; }
: END (PROGRAM (id=identifier)?)? Newline
;

subroutineStatement returns [ Identifier subroutineName = null ]
: SUBROUTINE subroutineName=identifier ( LPAREN identifierList RPAREN )?
;

endSubroutineStatement {Identifier id = null;}
: END (SUBROUTINE (id=identifier)?)? Newline;

functionStatement returns [ Identifier functionName = null ]
: FUNCTION functionName=identifier ( LPAREN (identifierList)? RPAREN )?
;

endFunctionStatement {Identifier id=null;}
: END (FUNCTION (id=identifier)?)? Newline
;

subprogramBody 
// The parser can be unable to distinguish if he must follow the declarationPart
// or the executablePart due to Newline's (as a Newline can be in both parts.
// Which one it selects does not matter, so we add warnWhenFollowAmbig=false in
// the declarationPart rule.
: declarationPart executablePart
;

declarationPart
: ( options{warnWhenFollowAmbig=false;}: declarationStatement | Newline )*
;

executablePart
: ( executableStatement Newline | Newline )*
;

declarationStatement

  { List<Specifier> trash = null; Identifier trashId = null; }

: trash=specifier declaratorList
| DIMENSION declaratorList
| INCLUDE STRINGLITERAL
| COMMON DIVIDE trashId=identifier DIVIDE declaratorList
| IMPLICIT ( NONE | trash=specifier LPAREN implicitList RPAREN )
| PARAMETER LPAREN parameterList RPAREN
| DATA dataDeclaration ( ( COMMA )? dataDeclaration )*
| INTRINSIC identifierList
| EXTERNAL identifierList
| SAVE identifierList
| SAVE DIVIDE identifierList DIVIDE
;

dataDeclaration
: dataDeclarationExpressionList DIVIDE dataDeclarationInitializerList DIVIDE
;

dataDeclarationExpressionList
: dataDeclarationExpression ( COMMA dataDeclarationExpression )*
;

dataDeclarationExpression
: identifier
| arrayAccessOrFunctionCall
| impliedDoLoop
;

dataDeclarationInitializerList
: dataDeclarationInitializer ( COMMA dataDeclarationInitializer )*
;

dataDeclarationInitializer
: expression
;

specifier returns [ ArrayList<Specifier> spec = new ArrayList<Specifier>( 2 ) ]

: INTEGER { spec.add( Specifier.INT ); }
| INTEGER BY num:NUMBER {
    if( num.getText().equals( "4" ) ) {
      spec.add( Specifier.INT );
    } else {
      spec.add( Specifier.LONG );
    }
  }
| REAL	  { spec.add( Specifier.FLOAT ); }
| REAL BY num2:NUMBER {
    if( num2.getText().equals( "4" ) ) {
      spec.add( Specifier.FLOAT );
    } else {
      spec.add( Specifier.DOUBLE );
    }
  }
| DOUBLE PRECISION { spec.add( Specifier.DOUBLE ); }
| LOGICAL { spec.add( Specifier.BOOL ); }
| CHARACTER { spec.add( Specifier.CHAR ); }
| CHARACTER BY num3:NUMBER {

    spec.add( Specifier.CHAR );

    if( !num3.getText().equals( "1" ) ) {
      spec.add( new ArraySpecifier( new IntegerLiteral( new Integer(
        num3.getText() ).intValue() ) ) );
    }
  }
;

executableStatement
: pragmaStatement
| doLoopStatement
| expressionStatement
| ifStatement
| returnStatement
| labeledStatement
| whileStatement
//| continueStatement
;

labeledStatement

: LABEL expressionStatement
| LABEL ifStatement
| LABEL formatStatement
| LABEL returnStatement
| LABEL doLoopStatement
//| LABEL continueStatement
;

doLoopStatement { Identifier trashId = null; }
: DO (NUMBER (COMMA)? )? trashId=identifier EQUALS doLimit COMMA doLimit
  ( COMMA expression )? executablePart endDoLoopStatement
;

endDoLoopStatement
: END (DO)?
| ENDDO
| LABEL CONTINUE
;

whileStatement
: DO WHILE LPAREN conditionalExpression RPAREN executablePart 
  ( END DO | ENDDO )
;

doLimit
: unaryExpression
| arithmeticBinaryExpression
;

expressionStatement
: assignmentTerminal ( EQUALS expression )?
| procedureCallStatement
| predefinedFunctionStatement
;

ifStatement
: IF LPAREN conditionalExpression RPAREN
  ( executableStatement
  | THEN executablePart
      ( (ELSEIF LPAREN conditionalExpression RPAREN THEN executablePart)*
      ( ELSE Newline executablePart )? )?
  ( END (IF)? | ENDIF )
  )
;

formatStatement
: { curLexer.toggleFormat(); }

  FORMAT (FORMAT_WHITESPACE)? LPAREN
    (FORMAT_WHITESPACE)? formatParameterList (FORMAT_WHITESPACE)? RPAREN {
      curLexer.toggleFormat();
    }
;

returnStatement
: RETURN
;

pragmaStatement
: PRAGMA PRAGMAVALUE
;

procedureCallStatement { Identifier trashId = null; }
: CALL trashId=identifier ( LPAREN parameterList RPAREN )?
;

predefinedFunctionStatement
: STOP ( stringLiteral )?
| PAUSE ( stringLiteral )?
| (GOTOTOKEN|GO TO) NUMBER
| (GOTOTOKEN|GO TO) LPAREN integerLiteralList RPAREN (COMMA)? expression
| (WRITE|READ) LPAREN parameterList RPAREN (COMMA)? ( ioParameterList )?
| PRINT parameter COMMA ioParameterList
| OPEN LPAREN parameterList RPAREN
| CLOSE LPAREN parameterList RPAREN
;

integerLiteralList
: integerLiteral ( COMMA integerLiteral )*
;

ioParameterList
: ioParameter ( COMMA ioParameter )*
;

ioParameter
: ioParameterTerminal ( ioParameterOperator ioParameter )?
;

ioParameterOperator
: arithmeticBinaryOperator
| logicalBinaryOperator
;

ioParameterTerminal { Identifier trashId = null; }
: literal
| trashId = identifier
| unaryExpression
| arrayAccessOrFunctionCall
| impliedDoLoop
;

impliedDoLoop { Identifier trashId = null; }
: LPAREN ioParameter ( COMMA (ioParameter COMMA )* trashId=identifier
  assignmentBinaryOperator expression COMMA expression ( COMMA expression )? )?
  RPAREN
;

declaratorList
: declarator ( COMMA declarator )*
;

declarator { Identifier trashId = null; }
: trashId=identifier ( LPAREN dimensionList RPAREN )?
;

dimensionList
: dimension ( COMMA dimension )*
;

dimension
: parameter ( COLON parameter )?
;

parameterList
: parameter ( COMMA parameter )*
;

parameter
: expression
| BY
;

formatParameterList
: formatParameter ( COMMA formatParameter )*
;

formatParameter
: ( FORMAT_NEWLINE
  | stringLiteral
  | ( FORMAT_TEXT ) +
  | FORMAT_WHITESPACE
  | LPAREN formatParameterList RPAREN )+
;

identifierList { Identifier trashId = null; }
: trashId=identifier ( COMMA trashId=identifier )*
;

implicitList
: implicitRegister ( COMMA implicitRegister )*
;

implicitRegister
: . DASH .
;

expression
: expressionTerminal ( options{warnWhenFollowAmbig=false;}: binaryOperator
    expression )?
;

expressionTerminal
: ( literal
  | identifier
  | unaryExpression
  | arrayAccessOrFunctionCall
  | LPAREN expression RPAREN )
;

conditionalExpression
: expression ( logicalBinaryOperator conditionalExpression )?
;

arrayAccessOrFunctionCall { Identifier trashId = null; }

: trashId=identifier LPAREN (parameterList|parameter COLON parameter)? RPAREN
| builtInFunctionWithReservedName LPAREN
    ( parameterList|parameter COLON parameter)? RPAREN
;

builtInFunctionWithReservedName
: REAL
;

assignmentExpression
: assignmentTerminal EQUALS expression
;

assignmentTerminal { Identifier trashId = null; }
: arrayAccessOrFunctionCall
| trashId = identifier
;

arithmeticBinaryExpression
: arithmeticTerminal ( arithmeticBinaryOperator expression )?
;

logicalBinaryExpression
: logicalTerminal logicalBinaryOperator expression
;

arithmeticTerminal { Identifier trashId = null; }
: arrayAccessOrFunctionCall
| trashId = identifier
| integerLiteral
| realLiteral
| LPAREN arithmeticBinaryExpression RPAREN
;

logicalTerminal { Identifier trashId = null; }
: trashId = identifier
| booleanLiteral
| unaryExpression
| LPAREN logicalBinaryExpression RPAREN
;

binaryOperator
: arithmeticBinaryOperator
| assignmentBinaryOperator
| logicalBinaryOperator
;

arithmeticBinaryOperator
: ADD
| BY
| CONCATENATION
| DIVIDE
| DASH
| POWER
;

assignmentBinaryOperator
: EQUALS
;

logicalBinaryOperator
: COMPARE_EQ
| COMPARE_GE
| COMPARE_GT
| COMPARE_LE
| COMPARE_LT
| COMPARE_NE
| LOGICAL_AND
| LOGICAL_OR
;

identifier returns [ Identifier returnId=null ]
: id:ID {
    // We convert into UpperCase all identifiers so that Fortran behaviour is
    // correctly displayed
    returnId = new Identifier( id.getText().toUpperCase() );
  }
;

literal
: booleanLiteral
| integerLiteral
| realLiteral
| stringLiteral
;

unaryExpression
: unaryOperator expression
;

unaryOperator
: LOGICAL_NEGATION
| DASH
| ADD
;

booleanLiteral
: LOGICAL_TRUE
| LOGICAL_FALSE
;

realLiteral
: { getLexer().toggleRealLiteral(); }

  ( NUMBER )? DOT ( NUMBER )? ( E ( DASH | ADD )? NUMBER )? {
    getLexer().toggleRealLiteral();
  }
;

integerLiteral
: NUMBER
;

stringLiteral
: STRINGLITERAL
;
