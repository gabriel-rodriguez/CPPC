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



header {
    package cppc.compiler.cetus.grammars.fortran77;
}

class FLexer extends Lexer;

options {
  k = 3;
  caseSensitive=false;
  caseSensitiveLiterals=false;
  testLiterals=true;
  charVocabulary='\u0000'..'\uFFFE';
}

tokens {
  CALL             = "call";
  CHARACTER        = "character";
  CLOSE            = "close";
  COMMON           = "common";
  COMPLEX          = "complex";
  CONTINUE         = "continue";
  DATA             = "data";
  DIMENSION        = "dimension";
  DO               = "do";
  DOUBLE           = "double";
  ELSE             = "else";
  ELSEIF           = "elseif";
  END              = "end";
  ENDDO            = "enddo";
  ENDIF            = "endif";
  EXTERNAL         = "external";
  FORMAT           = "format";
  FUNCTION         = "function" ;
  GO               = "go";
  GOTOTOKEN        = "goto";
  IF               = "if" ;
  IMPLICIT         = "implicit";
  INCLUDE          = "include";
  INTEGER          = "integer";
  INTRINSIC        = "intrinsic";
  NONE             = "none";
  LOGICAL          = "logical";
  OPEN             = "open";
  PARAMETER        = "parameter";
  PAUSE            = "pause";
  PRECISION        = "precision";
  PRINT            = "print";
  PROGRAM          = "program" ;
  READ             = "read";
  REAL             = "real";
  RETURN           = "return";
  SAVE             = "save";
  STOP             = "stop";
  SUBROUTINE       = "subroutine" ;
  THEN             = "then";
  TO               = "to";
  WHILE            = "while";
  WRITE            = "write";
}

{
    String originalSource = "";
    boolean readingPragma = false;
    boolean readingRealLiteral = false;
    boolean readingFormat = false;
    boolean readingFormatText = false;

    public void initialize( String source ) {
        setOriginalSource( source );
    }

    public void setOriginalSource( String originalSource ) {
        this.originalSource = originalSource;
    }

    public void togglePragma() {
      readingPragma = !readingPragma;
    }

    public void toggleRealLiteral() {
      readingRealLiteral = !readingRealLiteral;
    }

    public boolean normalLine() {
      return( (getColumn() > 6) ) && !farColumn();
    }

    public boolean labelColumn() {
      return( getColumn() < 7 );
    }

    public boolean farColumn() {
      return( getColumn() > 72 );
    }

    public void toggleFormat() {
      readingFormat = !readingFormat;
    }

    public void toggleFormatText() {
      readingFormatText = !readingFormatText;
    }
}

ADD             : { normalLine() }? '+';
BY              : { normalLine() }? '*';
COLON           : { normalLine() }? ':';
COMMA           : { normalLine() }? ',';
COMPARE_EQ      : { normalLine() }? ".eq.";
COMPARE_GE      : { normalLine() }? ".ge.";
COMPARE_GT      : { normalLine() }? ".gt.";
COMPARE_LE      : { normalLine() }? ".le.";
COMPARE_LT      : { normalLine() }? ".lt.";
COMPARE_NE      : { normalLine() }? ".ne.";
CONCATENATION   : { normalLine() && !readingFormat }? "//";
DASH            : { normalLine() }? '-';
DIVIDE          : { normalLine() && !readingFormat }? '/';
DOT             : { normalLine() }? '.';
E               : { readingRealLiteral }? ( "e" | "d" );
EQUALS          : { normalLine() }? '=';
POWER           : { normalLine() }? "**";
LOGICAL_AND     : { normalLine() }? ".and.";
LOGICAL_FALSE   : { normalLine() }? ".false.";
LOGICAL_NEGATION: { normalLine() }? ".not.";
LOGICAL_OR      : { normalLine() }? ".or.";
LOGICAL_TRUE    : { normalLine() }? ".true.";
LPAREN          : { normalLine() }? '(';
RPAREN          : { normalLine() }? ')';
PRAGMA          : { normalLine() }? '!'! ID '$'! { togglePragma(); };
QUOTE           : { normalLine() }? ( '\'' | '"' );

// Other tokens that may appear in column 1 or 6
protected AMP   : { normalLine() }? '&';
protected BACKSLASH : { normalLine() }? '\\';
protected CIRCUMFLEX: { normalLine() }? '^';
protected LT    : { normalLine() }? '<';
protected GT    : { normalLine() }? '>';
protected QUERY : { normalLine() }? '?';
protected SEMICOLON : { normalLine() }? ';';
protected SHARP : { normalLine() }? '#';

ID: { normalLine() && !readingFormat }? ID_CHARACTER ( ID_CHARACTER | NUMBER_TOKEN )*
;

protected ID_CHARACTER: { normalLine() }?
        ( 'a'..'z' | '_' );

Whitespace: { !readingPragma && !readingFormat }?
        ( ' ' | '\t' | '\r' )+ { $setType( Token.SKIP ); }
;

// Labels are lexer worries
LABEL: { labelColumn() }? NUMBER;
NUMBER: { ( normalLine() && !readingFormat ) || labelColumn() }?(NUMBER_TOKEN)+;
protected NUMBER_TOKEN: { normalLine() || labelColumn() }? '0'..'9';
protected STRING_TOKEN: { normalLine() }? ~( '\'' | '\n' );

STRINGLITERAL: { normalLine() }? QUOTE!
  ( options{greedy=false;}: STRING_TOKEN )* QUOTE!;
PRAGMAVALUE: { readingPragma }? ( ~('\n') )+ { togglePragma(); };

// We skip comments. It would be a pain to distinguish between in-procedure ones and out-procedure,
// ones, so they simply get wiped.
COMMENT: { getColumn()==1 }? ~( '\n' | ' ' | '\t' | '\r' | '0'..'9' )
  ( ~('\n') )* { $setType( Token.SKIP ); } //Comments beginning in column 1
;

INLINECOMMENT: { normalLine() }? '!' ( ~('\n') )* { $setType( Token.SKIP ); };
FAR_COMMENT: { farColumn() }? ~('\n') { $setType( Token.SKIP ); };

protected NEWLINE: '\n' { newline(); };
protected CONTINUELINE: ( ~('\n'|' '|'\t'|'\r') );
Newline: { boolean oldFormat=readingFormat; readingFormat=false;} NEWLINE
          (Whitespace)?
          ({ getColumn() == 6 }? CONTINUELINE { $setType( Token.SKIP ); })?
          {readingFormat=oldFormat;};

FORMAT_NEWLINE: { readingFormat }? "/";
FORMAT_WHITESPACE: { readingFormat }? ( ' ' | '\t' | '\r' )+;
FORMAT_TEXT: { readingFormat }? ( ~( '(' | ')' | '\n' | '/' | ',' | '\'' ) )+;