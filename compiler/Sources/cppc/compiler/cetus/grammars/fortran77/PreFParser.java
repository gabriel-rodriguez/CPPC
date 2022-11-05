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



// $ANTLR 2.7.5 (20070115): "PreFParser.g" -> "PreFParser.java"$

    package cppc.compiler.cetus.grammars.fortran77;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

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

public class PreFParser extends antlr.LLkParser       implements PreFParserTokenTypes
 {

  FLexer curLexer = null;

  public FLexer getLexer() {
    return curLexer;
  }

  public void setLexer( FLexer lexer ) {
    curLexer = lexer;
  }

protected PreFParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public PreFParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected PreFParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public PreFParser(TokenStream lexer) {
  this(lexer,2);
}

public PreFParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final void translationUnit(
		TranslationUnit init_tunit
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop3:
			do {
				switch ( LA(1)) {
				case PROGRAM:
				{
					mainProgram();
					break;
				}
				case SUBROUTINE:
				{
					subroutineSubprogram();
					break;
				}
				case CHARACTER:
				case DOUBLE:
				case FUNCTION:
				case INTEGER:
				case LOGICAL:
				case REAL:
				{
					functionSubprogram();
					break;
				}
				case Newline:
				{
					match(Newline);
					break;
				}
				default:
				{
					break _loop3;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
	}
	
	public final void mainProgram() throws RecognitionException, TokenStreamException {
		
		Identifier name=null;
		
		try {      // for error handling
			name=programStatement();
			subprogramBody();
			endProgramStatement();
			
			
			ProcedureDeclarator declarator = new ProcedureDeclarator( name,
			new ArrayList() );
			Procedure procedure = new Procedure( declarator, new CompoundStatement() );
			FortranProcedureManager.addRegister( name, procedure );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final void subroutineSubprogram() throws RecognitionException, TokenStreamException {
		
		Identifier name = null;
		
		try {      // for error handling
			name=subroutineStatement();
			subprogramBody();
			endSubroutineStatement();
			
			
			ProcedureDeclarator declarator = new ProcedureDeclarator( name,
			new ArrayList() );
			Procedure procedure = new Procedure( declarator, new CompoundStatement() );
			FortranProcedureManager.addRegister( name, procedure );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final void functionSubprogram() throws RecognitionException, TokenStreamException {
		
		Identifier name = null; ArrayList<Specifier> spec = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case CHARACTER:
			case DOUBLE:
			case INTEGER:
			case LOGICAL:
			case REAL:
			{
				spec=specifier();
				break;
			}
			case FUNCTION:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			name=functionStatement();
			subprogramBody();
			endFunctionStatement();
			
			
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
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final Identifier  programStatement() throws RecognitionException, TokenStreamException {
		 Identifier id = null ;
		
		
		try {      // for error handling
			match(PROGRAM);
			id=identifier();
			match(Newline);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return id;
	}
	
	public final void subprogramBody() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			declarationPart();
			executablePart();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
	}
	
	public final void endProgramStatement() throws RecognitionException, TokenStreamException {
		
		Identifier id = null;
		
		try {      // for error handling
			match(END);
			{
			switch ( LA(1)) {
			case PROGRAM:
			{
				match(PROGRAM);
				{
				switch ( LA(1)) {
				case ID:
				{
					id=identifier();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(Newline);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final Identifier  subroutineStatement() throws RecognitionException, TokenStreamException {
		 Identifier subroutineName = null ;
		
		
		try {      // for error handling
			match(SUBROUTINE);
			subroutineName=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				identifierList();
				match(RPAREN);
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case DATA:
			case DIMENSION:
			case DO:
			case DOUBLE:
			case END:
			case EXTERNAL:
			case GO:
			case GOTOTOKEN:
			case IF:
			case IMPLICIT:
			case INCLUDE:
			case INTEGER:
			case INTRINSIC:
			case LOGICAL:
			case OPEN:
			case PARAMETER:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case SAVE:
			case STOP:
			case WRITE:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return subroutineName;
	}
	
	public final void endSubroutineStatement() throws RecognitionException, TokenStreamException {
		
		Identifier id = null;
		
		try {      // for error handling
			match(END);
			{
			switch ( LA(1)) {
			case SUBROUTINE:
			{
				match(SUBROUTINE);
				{
				switch ( LA(1)) {
				case ID:
				{
					id=identifier();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(Newline);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final ArrayList<Specifier>  specifier() throws RecognitionException, TokenStreamException {
		 ArrayList<Specifier> spec = new ArrayList<Specifier>( 2 ) ;
		
		Token  num = null;
		Token  num2 = null;
		Token  num3 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case DOUBLE:
			{
				match(DOUBLE);
				match(PRECISION);
				spec.add( Specifier.DOUBLE );
				break;
			}
			case LOGICAL:
			{
				match(LOGICAL);
				spec.add( Specifier.BOOL );
				break;
			}
			default:
				if ((LA(1)==INTEGER) && (LA(2)==FUNCTION||LA(2)==LPAREN||LA(2)==ID)) {
					match(INTEGER);
					spec.add( Specifier.INT );
				}
				else if ((LA(1)==INTEGER) && (LA(2)==BY)) {
					match(INTEGER);
					match(BY);
					num = LT(1);
					match(NUMBER);
					
					if( num.getText().equals( "4" ) ) {
					spec.add( Specifier.INT );
					} else {
					spec.add( Specifier.LONG );
					}
					
				}
				else if ((LA(1)==REAL) && (LA(2)==FUNCTION||LA(2)==LPAREN||LA(2)==ID)) {
					match(REAL);
					spec.add( Specifier.FLOAT );
				}
				else if ((LA(1)==REAL) && (LA(2)==BY)) {
					match(REAL);
					match(BY);
					num2 = LT(1);
					match(NUMBER);
					
					if( num2.getText().equals( "4" ) ) {
					spec.add( Specifier.FLOAT );
					} else {
					spec.add( Specifier.DOUBLE );
					}
					
				}
				else if ((LA(1)==CHARACTER) && (LA(2)==FUNCTION||LA(2)==LPAREN||LA(2)==ID)) {
					match(CHARACTER);
					spec.add( Specifier.CHAR );
				}
				else if ((LA(1)==CHARACTER) && (LA(2)==BY)) {
					match(CHARACTER);
					match(BY);
					num3 = LT(1);
					match(NUMBER);
					
					
					spec.add( Specifier.CHAR );
					
					if( !num3.getText().equals( "1" ) ) {
					spec.add( new ArraySpecifier( new IntegerLiteral( new Integer(
					num3.getText() ).intValue() ) ) );
					}
					
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
		}
		return spec;
	}
	
	public final Identifier  functionStatement() throws RecognitionException, TokenStreamException {
		 Identifier functionName = null ;
		
		
		try {      // for error handling
			match(FUNCTION);
			functionName=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				{
				switch ( LA(1)) {
				case ID:
				{
					identifierList();
					break;
				}
				case RPAREN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(RPAREN);
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case DATA:
			case DIMENSION:
			case DO:
			case DOUBLE:
			case END:
			case EXTERNAL:
			case GO:
			case GOTOTOKEN:
			case IF:
			case IMPLICIT:
			case INCLUDE:
			case INTEGER:
			case INTRINSIC:
			case LOGICAL:
			case OPEN:
			case PARAMETER:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case SAVE:
			case STOP:
			case WRITE:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return functionName;
	}
	
	public final void endFunctionStatement() throws RecognitionException, TokenStreamException {
		
		Identifier id=null;
		
		try {      // for error handling
			match(END);
			{
			switch ( LA(1)) {
			case FUNCTION:
			{
				match(FUNCTION);
				{
				switch ( LA(1)) {
				case ID:
				{
					id=identifier();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(Newline);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final Identifier  identifier() throws RecognitionException, TokenStreamException {
		 Identifier returnId=null ;
		
		Token  id = null;
		
		try {      // for error handling
			id = LT(1);
			match(ID);
			
			// We convert into UpperCase all identifiers so that Fortran behaviour is
			// correctly displayed
			returnId = new Identifier( id.getText().toUpperCase() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_5);
		}
		return returnId;
	}
	
	public final void identifierList() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			trashId=identifier();
			{
			_loop122:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					trashId=identifier();
				}
				else {
					break _loop122;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_6);
		}
	}
	
	public final void declarationPart() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop26:
			do {
				if ((_tokenSet_7.member(LA(1))) && (_tokenSet_8.member(LA(2)))) {
					declarationStatement();
				}
				else if ((LA(1)==Newline) && (_tokenSet_2.member(LA(2)))) {
					match(Newline);
				}
				else {
					break _loop26;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_9);
		}
	}
	
	public final void executablePart() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop29:
			do {
				if ((_tokenSet_10.member(LA(1))) && (_tokenSet_11.member(LA(2)))) {
					executableStatement();
					match(Newline);
				}
				else if ((LA(1)==Newline)) {
					match(Newline);
				}
				else {
					break _loop29;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
	}
	
	public final void declarationStatement() throws RecognitionException, TokenStreamException {
		
		List<Specifier> trash = null; Identifier trashId = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case CHARACTER:
			case DOUBLE:
			case INTEGER:
			case LOGICAL:
			case REAL:
			{
				trash=specifier();
				declaratorList();
				break;
			}
			case DIMENSION:
			{
				match(DIMENSION);
				declaratorList();
				break;
			}
			case INCLUDE:
			{
				match(INCLUDE);
				match(STRINGLITERAL);
				break;
			}
			case COMMON:
			{
				match(COMMON);
				match(DIVIDE);
				trashId=identifier();
				match(DIVIDE);
				declaratorList();
				break;
			}
			case IMPLICIT:
			{
				match(IMPLICIT);
				{
				switch ( LA(1)) {
				case NONE:
				{
					match(NONE);
					break;
				}
				case CHARACTER:
				case DOUBLE:
				case INTEGER:
				case LOGICAL:
				case REAL:
				{
					trash=specifier();
					match(LPAREN);
					implicitList();
					match(RPAREN);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case PARAMETER:
			{
				match(PARAMETER);
				match(LPAREN);
				parameterList();
				match(RPAREN);
				break;
			}
			case DATA:
			{
				match(DATA);
				dataDeclaration();
				{
				_loop34:
				do {
					if ((_tokenSet_13.member(LA(1))) && (_tokenSet_14.member(LA(2)))) {
						{
						switch ( LA(1)) {
						case COMMA:
						{
							match(COMMA);
							break;
						}
						case REAL:
						case LPAREN:
						case ID:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						dataDeclaration();
					}
					else {
						break _loop34;
					}
					
				} while (true);
				}
				break;
			}
			case INTRINSIC:
			{
				match(INTRINSIC);
				identifierList();
				break;
			}
			case EXTERNAL:
			{
				match(EXTERNAL);
				identifierList();
				break;
			}
			default:
				if ((LA(1)==SAVE) && (LA(2)==ID)) {
					match(SAVE);
					identifierList();
				}
				else if ((LA(1)==SAVE) && (LA(2)==DIVIDE)) {
					match(SAVE);
					match(DIVIDE);
					identifierList();
					match(DIVIDE);
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
	}
	
	public final void executableStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case PRAGMA:
			{
				pragmaStatement();
				break;
			}
			case CALL:
			case CLOSE:
			case GO:
			case GOTOTOKEN:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case STOP:
			case WRITE:
			case ID:
			{
				expressionStatement();
				break;
			}
			case IF:
			{
				ifStatement();
				break;
			}
			case RETURN:
			{
				returnStatement();
				break;
			}
			case LABEL:
			{
				labeledStatement();
				break;
			}
			default:
				if ((LA(1)==DO) && (LA(2)==ID||LA(2)==NUMBER)) {
					doLoopStatement();
				}
				else if ((LA(1)==DO) && (LA(2)==WHILE)) {
					whileStatement();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void declaratorList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			declarator();
			{
			_loop100:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					declarator();
				}
				else {
					break _loop100;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
	}
	
	public final void implicitList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			implicitRegister();
			{
			_loop125:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					implicitRegister();
				}
				else {
					break _loop125;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void parameterList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			parameter();
			{
			_loop110:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					parameter();
				}
				else {
					break _loop110;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void dataDeclaration() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			dataDeclarationExpressionList();
			match(DIVIDE);
			dataDeclarationInitializerList();
			match(DIVIDE);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_17);
		}
	}
	
	public final void dataDeclarationExpressionList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			dataDeclarationExpression();
			{
			_loop38:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					dataDeclarationExpression();
				}
				else {
					break _loop38;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_18);
		}
	}
	
	public final void dataDeclarationInitializerList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			dataDeclarationInitializer();
			{
			_loop42:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					dataDeclarationInitializer();
				}
				else {
					break _loop42;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_18);
		}
	}
	
	public final void dataDeclarationExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			if ((LA(1)==ID) && (LA(2)==COMMA||LA(2)==DIVIDE)) {
				identifier();
			}
			else if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
				arrayAccessOrFunctionCall();
			}
			else if ((LA(1)==LPAREN)) {
				impliedDoLoop();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
		}
	}
	
	public final void arrayAccessOrFunctionCall() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				trashId=identifier();
				match(LPAREN);
				{
				if ((_tokenSet_20.member(LA(1))) && (_tokenSet_21.member(LA(2)))) {
					parameterList();
				}
				else if ((_tokenSet_20.member(LA(1))) && (_tokenSet_22.member(LA(2)))) {
					parameter();
					match(COLON);
					parameter();
				}
				else if ((LA(1)==RPAREN)) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				match(RPAREN);
				break;
			}
			case REAL:
			{
				builtInFunctionWithReservedName();
				match(LPAREN);
				{
				if ((_tokenSet_20.member(LA(1))) && (_tokenSet_21.member(LA(2)))) {
					parameterList();
				}
				else if ((_tokenSet_20.member(LA(1))) && (_tokenSet_22.member(LA(2)))) {
					parameter();
					match(COLON);
					parameter();
				}
				else if ((LA(1)==RPAREN)) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				match(RPAREN);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void impliedDoLoop() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			match(LPAREN);
			ioParameter();
			{
			switch ( LA(1)) {
			case COMMA:
			{
				match(COMMA);
				{
				_loop96:
				do {
					if ((_tokenSet_24.member(LA(1))) && (_tokenSet_25.member(LA(2)))) {
						ioParameter();
						match(COMMA);
					}
					else {
						break _loop96;
					}
					
				} while (true);
				}
				trashId=identifier();
				assignmentBinaryOperator();
				expression();
				match(COMMA);
				expression();
				{
				switch ( LA(1)) {
				case COMMA:
				{
					match(COMMA);
					expression();
					break;
				}
				case RPAREN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_26);
		}
	}
	
	public final void dataDeclarationInitializer() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			expression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
		}
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			expressionTerminal();
			{
			if ((_tokenSet_27.member(LA(1))) && (_tokenSet_24.member(LA(2)))) {
				binaryOperator();
				expression();
			}
			else if ((_tokenSet_23.member(LA(1))) && (_tokenSet_28.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void pragmaStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(PRAGMA);
			match(PRAGMAVALUE);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void doLoopStatement() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			match(DO);
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				match(NUMBER);
				{
				switch ( LA(1)) {
				case COMMA:
				{
					match(COMMA);
					break;
				}
				case ID:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			trashId=identifier();
			match(EQUALS);
			doLimit();
			match(COMMA);
			doLimit();
			{
			switch ( LA(1)) {
			case COMMA:
			{
				match(COMMA);
				expression();
				break;
			}
			case CALL:
			case CLOSE:
			case DO:
			case END:
			case ENDDO:
			case GO:
			case GOTOTOKEN:
			case IF:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case STOP:
			case WRITE:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			executablePart();
			endDoLoopStatement();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void expressionStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case REAL:
			case ID:
			{
				assignmentTerminal();
				{
				switch ( LA(1)) {
				case EQUALS:
				{
					match(EQUALS);
					expression();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case CALL:
			{
				procedureCallStatement();
				break;
			}
			case CLOSE:
			case GO:
			case GOTOTOKEN:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case STOP:
			case WRITE:
			{
				predefinedFunctionStatement();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void ifStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(IF);
			match(LPAREN);
			conditionalExpression();
			match(RPAREN);
			{
			switch ( LA(1)) {
			case CALL:
			case CLOSE:
			case DO:
			case GO:
			case GOTOTOKEN:
			case IF:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case STOP:
			case WRITE:
			case PRAGMA:
			case ID:
			case LABEL:
			{
				executableStatement();
				break;
			}
			case THEN:
			{
				match(THEN);
				executablePart();
				{
				if ((_tokenSet_29.member(LA(1))) && (LA(2)==IF||LA(2)==LPAREN||LA(2)==Newline)) {
					{
					_loop62:
					do {
						if ((LA(1)==ELSEIF)) {
							match(ELSEIF);
							match(LPAREN);
							conditionalExpression();
							match(RPAREN);
							match(THEN);
							executablePart();
						}
						else {
							break _loop62;
						}
						
					} while (true);
					}
					{
					switch ( LA(1)) {
					case ELSE:
					{
						match(ELSE);
						match(Newline);
						executablePart();
						break;
					}
					case END:
					case ENDIF:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				else if ((LA(1)==END||LA(1)==ENDIF) && (LA(2)==IF||LA(2)==Newline)) {
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				{
				switch ( LA(1)) {
				case END:
				{
					match(END);
					{
					switch ( LA(1)) {
					case IF:
					{
						match(IF);
						break;
					}
					case Newline:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					break;
				}
				case ENDIF:
				{
					match(ENDIF);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void returnStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(RETURN);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void labeledStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			if ((LA(1)==LABEL) && (_tokenSet_30.member(LA(2)))) {
				match(LABEL);
				expressionStatement();
			}
			else if ((LA(1)==LABEL) && (LA(2)==IF)) {
				match(LABEL);
				ifStatement();
			}
			else if ((LA(1)==LABEL) && (LA(2)==FORMAT)) {
				match(LABEL);
				formatStatement();
			}
			else if ((LA(1)==LABEL) && (LA(2)==RETURN)) {
				match(LABEL);
				returnStatement();
			}
			else if ((LA(1)==LABEL) && (LA(2)==DO)) {
				match(LABEL);
				doLoopStatement();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void whileStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(DO);
			match(WHILE);
			match(LPAREN);
			conditionalExpression();
			match(RPAREN);
			executablePart();
			{
			switch ( LA(1)) {
			case END:
			{
				match(END);
				match(DO);
				break;
			}
			case ENDDO:
			{
				match(ENDDO);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void formatStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			curLexer.toggleFormat();
			match(FORMAT);
			{
			switch ( LA(1)) {
			case FORMAT_WHITESPACE:
			{
				match(FORMAT_WHITESPACE);
				break;
			}
			case LPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LPAREN);
			{
			if ((LA(1)==FORMAT_WHITESPACE) && (_tokenSet_31.member(LA(2)))) {
				match(FORMAT_WHITESPACE);
			}
			else if ((_tokenSet_31.member(LA(1))) && (_tokenSet_32.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			formatParameterList();
			{
			switch ( LA(1)) {
			case FORMAT_WHITESPACE:
			{
				match(FORMAT_WHITESPACE);
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			
			curLexer.toggleFormat();
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void doLimit() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ADD:
			case DASH:
			case LOGICAL_NEGATION:
			{
				unaryExpression();
				break;
			}
			case REAL:
			case DOT:
			case LPAREN:
			case ID:
			case NUMBER:
			{
				arithmeticBinaryExpression();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_33);
		}
	}
	
	public final void endDoLoopStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case END:
			{
				match(END);
				{
				switch ( LA(1)) {
				case DO:
				{
					match(DO);
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case ENDDO:
			{
				match(ENDDO);
				break;
			}
			case LABEL:
			{
				match(LABEL);
				match(CONTINUE);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void conditionalExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			expression();
			{
			switch ( LA(1)) {
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case LOGICAL_AND:
			case LOGICAL_OR:
			{
				logicalBinaryOperator();
				conditionalExpression();
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void unaryExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			unaryOperator();
			expression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void arithmeticBinaryExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			arithmeticTerminal();
			{
			switch ( LA(1)) {
			case ADD:
			case BY:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case POWER:
			{
				arithmeticBinaryOperator();
				expression();
				break;
			}
			case CALL:
			case CLOSE:
			case DO:
			case END:
			case ENDDO:
			case GO:
			case GOTOTOKEN:
			case IF:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case STOP:
			case WRITE:
			case COMMA:
			case RPAREN:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_34);
		}
	}
	
	public final void assignmentTerminal() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
				arrayAccessOrFunctionCall();
			}
			else if ((LA(1)==ID) && (LA(2)==EQUALS||LA(2)==Newline)) {
				trashId=identifier();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_35);
		}
	}
	
	public final void procedureCallStatement() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			match(CALL);
			trashId=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				parameterList();
				match(RPAREN);
				break;
			}
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void predefinedFunctionStatement() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case STOP:
			{
				match(STOP);
				{
				switch ( LA(1)) {
				case STRINGLITERAL:
				{
					stringLiteral();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case PAUSE:
			{
				match(PAUSE);
				{
				switch ( LA(1)) {
				case STRINGLITERAL:
				{
					stringLiteral();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case READ:
			case WRITE:
			{
				{
				switch ( LA(1)) {
				case WRITE:
				{
					match(WRITE);
					break;
				}
				case READ:
				{
					match(READ);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(LPAREN);
				parameterList();
				match(RPAREN);
				{
				switch ( LA(1)) {
				case COMMA:
				{
					match(COMMA);
					break;
				}
				case REAL:
				case ADD:
				case DASH:
				case DOT:
				case LOGICAL_FALSE:
				case LOGICAL_NEGATION:
				case LOGICAL_TRUE:
				case LPAREN:
				case ID:
				case NUMBER:
				case STRINGLITERAL:
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case REAL:
				case ADD:
				case DASH:
				case DOT:
				case LOGICAL_FALSE:
				case LOGICAL_NEGATION:
				case LOGICAL_TRUE:
				case LPAREN:
				case ID:
				case NUMBER:
				case STRINGLITERAL:
				{
					ioParameterList();
					break;
				}
				case Newline:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case PRINT:
			{
				match(PRINT);
				parameter();
				match(COMMA);
				ioParameterList();
				break;
			}
			case OPEN:
			{
				match(OPEN);
				match(LPAREN);
				parameterList();
				match(RPAREN);
				break;
			}
			case CLOSE:
			{
				match(CLOSE);
				match(LPAREN);
				parameterList();
				match(RPAREN);
				break;
			}
			default:
				if ((LA(1)==GO||LA(1)==GOTOTOKEN) && (LA(2)==TO||LA(2)==NUMBER)) {
					{
					switch ( LA(1)) {
					case GOTOTOKEN:
					{
						match(GOTOTOKEN);
						break;
					}
					case GO:
					{
						match(GO);
						match(TO);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					match(NUMBER);
				}
				else if ((LA(1)==GO||LA(1)==GOTOTOKEN) && (LA(2)==TO||LA(2)==LPAREN)) {
					{
					switch ( LA(1)) {
					case GOTOTOKEN:
					{
						match(GOTOTOKEN);
						break;
					}
					case GO:
					{
						match(GO);
						match(TO);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					match(LPAREN);
					integerLiteralList();
					match(RPAREN);
					{
					switch ( LA(1)) {
					case COMMA:
					{
						match(COMMA);
						break;
					}
					case REAL:
					case ADD:
					case DASH:
					case DOT:
					case LOGICAL_FALSE:
					case LOGICAL_NEGATION:
					case LOGICAL_TRUE:
					case LPAREN:
					case ID:
					case NUMBER:
					case STRINGLITERAL:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					expression();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void formatParameterList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			formatParameter();
			{
			_loop114:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					formatParameter();
				}
				else {
					break _loop114;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_36);
		}
	}
	
	public final void stringLiteral() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(STRINGLITERAL);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_37);
		}
	}
	
	public final void integerLiteralList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			integerLiteral();
			{
			_loop85:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					integerLiteral();
				}
				else {
					break _loop85;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void ioParameterList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			ioParameter();
			{
			_loop88:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					ioParameter();
				}
				else {
					break _loop88;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final void parameter() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case REAL:
			case ADD:
			case DASH:
			case DOT:
			case LOGICAL_FALSE:
			case LOGICAL_NEGATION:
			case LOGICAL_TRUE:
			case LPAREN:
			case ID:
			case NUMBER:
			case STRINGLITERAL:
			{
				expression();
				break;
			}
			case BY:
			{
				match(BY);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
	}
	
	public final void integerLiteral() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(NUMBER);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void ioParameter() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			ioParameterTerminal();
			{
			switch ( LA(1)) {
			case ADD:
			case BY:
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case POWER:
			case LOGICAL_AND:
			case LOGICAL_OR:
			{
				ioParameterOperator();
				ioParameter();
				break;
			}
			case COMMA:
			case RPAREN:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_39);
		}
	}
	
	public final void ioParameterTerminal() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case DOT:
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			case NUMBER:
			case STRINGLITERAL:
			{
				literal();
				break;
			}
			case ADD:
			case DASH:
			case LOGICAL_NEGATION:
			{
				unaryExpression();
				break;
			}
			case LPAREN:
			{
				impliedDoLoop();
				break;
			}
			default:
				if ((LA(1)==ID) && (_tokenSet_26.member(LA(2)))) {
					trashId=identifier();
				}
				else if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
					arrayAccessOrFunctionCall();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_26);
		}
	}
	
	public final void ioParameterOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ADD:
			case BY:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case POWER:
			{
				arithmeticBinaryOperator();
				break;
			}
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case LOGICAL_AND:
			case LOGICAL_OR:
			{
				logicalBinaryOperator();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	public final void arithmeticBinaryOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ADD:
			{
				match(ADD);
				break;
			}
			case BY:
			{
				match(BY);
				break;
			}
			case CONCATENATION:
			{
				match(CONCATENATION);
				break;
			}
			case DIVIDE:
			{
				match(DIVIDE);
				break;
			}
			case DASH:
			{
				match(DASH);
				break;
			}
			case POWER:
			{
				match(POWER);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	public final void logicalBinaryOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case COMPARE_EQ:
			{
				match(COMPARE_EQ);
				break;
			}
			case COMPARE_GE:
			{
				match(COMPARE_GE);
				break;
			}
			case COMPARE_GT:
			{
				match(COMPARE_GT);
				break;
			}
			case COMPARE_LE:
			{
				match(COMPARE_LE);
				break;
			}
			case COMPARE_LT:
			{
				match(COMPARE_LT);
				break;
			}
			case COMPARE_NE:
			{
				match(COMPARE_NE);
				break;
			}
			case LOGICAL_AND:
			{
				match(LOGICAL_AND);
				break;
			}
			case LOGICAL_OR:
			{
				match(LOGICAL_OR);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	public final void literal() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			{
				booleanLiteral();
				break;
			}
			case STRINGLITERAL:
			{
				stringLiteral();
				break;
			}
			default:
				if ((LA(1)==NUMBER) && (_tokenSet_23.member(LA(2)))) {
					integerLiteral();
				}
				else if ((LA(1)==DOT||LA(1)==NUMBER) && (_tokenSet_40.member(LA(2)))) {
					realLiteral();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void assignmentBinaryOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(EQUALS);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	public final void declarator() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			trashId=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				dimensionList();
				match(RPAREN);
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case DATA:
			case DIMENSION:
			case DO:
			case DOUBLE:
			case END:
			case EXTERNAL:
			case GO:
			case GOTOTOKEN:
			case IF:
			case IMPLICIT:
			case INCLUDE:
			case INTEGER:
			case INTRINSIC:
			case LOGICAL:
			case OPEN:
			case PARAMETER:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case SAVE:
			case STOP:
			case WRITE:
			case COMMA:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_41);
		}
	}
	
	public final void dimensionList() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			dimension();
			{
			_loop105:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					dimension();
				}
				else {
					break _loop105;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void dimension() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			parameter();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				parameter();
				break;
			}
			case COMMA:
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_42);
		}
	}
	
	public final void formatParameter() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt119=0;
			_loop119:
			do {
				switch ( LA(1)) {
				case FORMAT_NEWLINE:
				{
					match(FORMAT_NEWLINE);
					break;
				}
				case STRINGLITERAL:
				{
					stringLiteral();
					break;
				}
				case FORMAT_TEXT:
				{
					{
					int _cnt118=0;
					_loop118:
					do {
						if ((LA(1)==FORMAT_TEXT) && (_tokenSet_32.member(LA(2)))) {
							match(FORMAT_TEXT);
						}
						else {
							if ( _cnt118>=1 ) { break _loop118; } else {throw new NoViableAltException(LT(1), getFilename());}
						}
						
						_cnt118++;
					} while (true);
					}
					break;
				}
				case LPAREN:
				{
					match(LPAREN);
					formatParameterList();
					match(RPAREN);
					break;
				}
				default:
					if ((LA(1)==FORMAT_WHITESPACE) && (_tokenSet_32.member(LA(2)))) {
						match(FORMAT_WHITESPACE);
					}
				else {
					if ( _cnt119>=1 ) { break _loop119; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt119++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_43);
		}
	}
	
	public final void implicitRegister() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			matchNot(EOF);
			match(DASH);
			matchNot(EOF);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_42);
		}
	}
	
	public final void expressionTerminal() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case DOT:
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			case NUMBER:
			case STRINGLITERAL:
			{
				literal();
				break;
			}
			case ADD:
			case DASH:
			case LOGICAL_NEGATION:
			{
				unaryExpression();
				break;
			}
			case LPAREN:
			{
				match(LPAREN);
				expression();
				match(RPAREN);
				break;
			}
			default:
				if ((LA(1)==ID) && (_tokenSet_23.member(LA(2)))) {
					identifier();
				}
				else if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
					arrayAccessOrFunctionCall();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void binaryOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ADD:
			case BY:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case POWER:
			{
				arithmeticBinaryOperator();
				break;
			}
			case EQUALS:
			{
				assignmentBinaryOperator();
				break;
			}
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case LOGICAL_AND:
			case LOGICAL_OR:
			{
				logicalBinaryOperator();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	public final void builtInFunctionWithReservedName() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(REAL);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_44);
		}
	}
	
	public final void assignmentExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			assignmentTerminal();
			match(EQUALS);
			expression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
	}
	
	public final void arithmeticTerminal() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
				arrayAccessOrFunctionCall();
			}
			else if ((LA(1)==ID) && (_tokenSet_45.member(LA(2)))) {
				trashId=identifier();
			}
			else if ((LA(1)==NUMBER) && (_tokenSet_45.member(LA(2)))) {
				integerLiteral();
			}
			else if ((LA(1)==DOT||LA(1)==NUMBER) && (_tokenSet_46.member(LA(2)))) {
				realLiteral();
			}
			else if ((LA(1)==LPAREN)) {
				match(LPAREN);
				arithmeticBinaryExpression();
				match(RPAREN);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_45);
		}
	}
	
	public final void logicalBinaryExpression() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			logicalTerminal();
			logicalBinaryOperator();
			expression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
	}
	
	public final void logicalTerminal() throws RecognitionException, TokenStreamException {
		
		Identifier trashId = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				trashId=identifier();
				break;
			}
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			{
				booleanLiteral();
				break;
			}
			case ADD:
			case DASH:
			case LOGICAL_NEGATION:
			{
				unaryExpression();
				break;
			}
			case LPAREN:
			{
				match(LPAREN);
				logicalBinaryExpression();
				match(RPAREN);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_47);
		}
	}
	
	public final void realLiteral() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			getLexer().toggleRealLiteral();
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				match(NUMBER);
				break;
			}
			case DOT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(DOT);
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				match(NUMBER);
				break;
			}
			case EOF:
			case CALL:
			case CLOSE:
			case DO:
			case END:
			case ENDDO:
			case GO:
			case GOTOTOKEN:
			case IF:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case STOP:
			case WRITE:
			case ADD:
			case BY:
			case COLON:
			case COMMA:
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case E:
			case EQUALS:
			case POWER:
			case LOGICAL_AND:
			case LOGICAL_OR:
			case RPAREN:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case E:
			{
				match(E);
				{
				switch ( LA(1)) {
				case DASH:
				{
					match(DASH);
					break;
				}
				case ADD:
				{
					match(ADD);
					break;
				}
				case NUMBER:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(NUMBER);
				break;
			}
			case EOF:
			case CALL:
			case CLOSE:
			case DO:
			case END:
			case ENDDO:
			case GO:
			case GOTOTOKEN:
			case IF:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case RETURN:
			case STOP:
			case WRITE:
			case ADD:
			case BY:
			case COLON:
			case COMMA:
			case COMPARE_EQ:
			case COMPARE_GE:
			case COMPARE_GT:
			case COMPARE_LE:
			case COMPARE_LT:
			case COMPARE_NE:
			case CONCATENATION:
			case DASH:
			case DIVIDE:
			case EQUALS:
			case POWER:
			case LOGICAL_AND:
			case LOGICAL_OR:
			case RPAREN:
			case PRAGMA:
			case ID:
			case LABEL:
			case Newline:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
			getLexer().toggleRealLiteral();
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void booleanLiteral() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LOGICAL_TRUE:
			{
				match(LOGICAL_TRUE);
				break;
			}
			case LOGICAL_FALSE:
			{
				match(LOGICAL_FALSE);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_23);
		}
	}
	
	public final void unaryOperator() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LOGICAL_NEGATION:
			{
				match(LOGICAL_NEGATION);
				break;
			}
			case DASH:
			{
				match(DASH);
				break;
			}
			case ADD:
			{
				match(ADD);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_24);
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"call\"",
		"\"character\"",
		"\"close\"",
		"\"common\"",
		"\"continue\"",
		"\"data\"",
		"\"dimension\"",
		"\"do\"",
		"\"double\"",
		"\"else\"",
		"\"elseif\"",
		"\"end\"",
		"\"enddo\"",
		"\"endif\"",
		"\"external\"",
		"\"format\"",
		"\"function\"",
		"\"go\"",
		"\"goto\"",
		"\"if\"",
		"\"implicit\"",
		"\"include\"",
		"\"integer\"",
		"\"intrinsic\"",
		"\"none\"",
		"\"logical\"",
		"\"open\"",
		"\"parameter\"",
		"\"pause\"",
		"\"precision\"",
		"\"print\"",
		"\"program\"",
		"\"read\"",
		"\"real\"",
		"\"return\"",
		"\"save\"",
		"\"stop\"",
		"\"subroutine\"",
		"\"then\"",
		"\"to\"",
		"\"while\"",
		"\"write\"",
		"ADD",
		"BY",
		"COLON",
		"COMMA",
		"COMPARE_EQ",
		"COMPARE_GE",
		"COMPARE_GT",
		"COMPARE_LE",
		"COMPARE_LT",
		"COMPARE_NE",
		"CONCATENATION",
		"DASH",
		"DIVIDE",
		"DOT",
		"E",
		"EQUALS",
		"POWER",
		"LOGICAL_AND",
		"LOGICAL_FALSE",
		"LOGICAL_NEGATION",
		"LOGICAL_OR",
		"LOGICAL_TRUE",
		"LPAREN",
		"RPAREN",
		"PRAGMA",
		"QUOTE",
		"AMP",
		"BACKSLASH",
		"CIRCUMFLEX",
		"LT",
		"GT",
		"QUERY",
		"SEMICOLON",
		"SHARP",
		"ID",
		"ID_CHARACTER",
		"Whitespace",
		"LABEL",
		"NUMBER",
		"NUMBER_TOKEN",
		"STRING_TOKEN",
		"STRINGLITERAL",
		"PRAGMAVALUE",
		"COMMENT",
		"INLINECOMMENT",
		"FAR_COMMENT",
		"NEWLINE",
		"CONTINUELINE",
		"Newline",
		"FORMAT_NEWLINE",
		"FORMAT_WHITESPACE",
		"FORMAT_TEXT"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2371426979874L, 1073741824L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 37340175441648L, 1074331712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 32768L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 1048576L, 65552L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { -1729415285478940942L, 1074331764L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 288267716327153392L, 1074331744L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 690131048096L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 288371260541374496L, 8454160L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 36787483347024L, 1074331712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 36787483314256L, 589888L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 3026693231588411472L, 1100021787L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 253952L, 524288L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 563087392374784L, 65552L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 1009439772667543552L, 9502747L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 0L, 1073741824L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 0L, 32L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 600290128862960L, 1074331728L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 288230376151711744L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 288793326105133056L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 720787184050765824L, 9502747L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { -351706281934848L, 9502783L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { -633181258645504L, 9502751L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { -1729415838171035566L, 1074331748L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 720646446562410496L, 9502747L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { -2306194715495628800L, 9502751L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { -4035577109844852736L, 1073741860L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { -1730297050584580096L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { -1152923746849259534L, 1100546175L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 188416L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 36512597016656L, 65536L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 0L, 15040774160L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 562949953421312L, 15040774192L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 599737436833872L, 1074331712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 599737436833872L, 1074331744L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { 2305843009213693952L, 1073741824L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { 0L, 4294967328L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { -1729415838171035566L, 16115105908L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	private static final long[] mk_tokenSet_38() {
		long[] data = { 844424930131968L, 32L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_38 = new BitSet(mk_tokenSet_38());
	private static final long[] mk_tokenSet_39() {
		long[] data = { 562949953421312L, 1073741856L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_39 = new BitSet(mk_tokenSet_39());
	private static final long[] mk_tokenSet_40() {
		long[] data = { -33581260765102L, 1075380324L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_40 = new BitSet(mk_tokenSet_40());
	private static final long[] mk_tokenSet_41() {
		long[] data = { 600290128862960L, 1074331712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_41 = new BitSet(mk_tokenSet_41());
	private static final long[] mk_tokenSet_42() {
		long[] data = { 562949953421312L, 32L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_42 = new BitSet(mk_tokenSet_42());
	private static final long[] mk_tokenSet_43() {
		long[] data = { 562949953421312L, 4294967328L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_43 = new BitSet(mk_tokenSet_43());
	private static final long[] mk_tokenSet_44() {
		long[] data = { 0L, 16L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_44 = new BitSet(mk_tokenSet_44());
	private static final long[] mk_tokenSet_45() {
		long[] data = { 5116900020362250320L, 1074331744L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_45 = new BitSet(mk_tokenSet_45());
	private static final long[] mk_tokenSet_46() {
		long[] data = { 6846282277272520784L, 1075380320L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_46 = new BitSet(mk_tokenSet_46());
	private static final long[] mk_tokenSet_47() {
		long[] data = { -9152440342723690496L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_47 = new BitSet(mk_tokenSet_47());
	
	}
