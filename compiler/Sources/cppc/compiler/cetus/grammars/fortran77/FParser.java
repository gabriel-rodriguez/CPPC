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



// $ANTLR 2.7.7 (20080506): "FParser.g" -> "FParser.java"$

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

import antlr.LexerSharedInputState;

import cetus.hir.*;

import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.FormatStatement;
import cppc.compiler.cetus.ImpliedDoLoop;
import cppc.compiler.cetus.IOCall;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.fortran.CommonDeclaration;
import cppc.compiler.fortran.ComplexSpecifier;
import cppc.compiler.fortran.ComputedGotoStatement;
import cppc.compiler.fortran.DataDeclaration;
import cppc.compiler.fortran.DimensionDeclaration;
import cppc.compiler.fortran.DoubleComplexSpecifier;
import cppc.compiler.fortran.ExternalDeclaration;
import cppc.compiler.fortran.FortranDoLoop;
import cppc.compiler.fortran.FormatExpression;
import cppc.compiler.fortran.FortranArraySpecifier;
import cppc.compiler.fortran.FortranProcedureManager;
import cppc.compiler.fortran.ImplicitDeclarationManager;
import cppc.compiler.fortran.ImplicitRegister;
import cppc.compiler.fortran.IntrinsicDeclaration;
import cppc.compiler.fortran.ParameterDeclaration;
import cppc.compiler.fortran.SaveDeclaration;
import cppc.compiler.fortran.StringSpecifier;
import cppc.compiler.fortran.SubstringExpression;
import cppc.compiler.fortran.TypeDeclaration;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FParser extends antlr.LLkParser       implements FParserTokenTypes
 {

    public static final String INCLUDE_ANNOTE_TEXT = "CPPC_INCLUDE_ANNOTE_TEXT";
    FLexer curLexer = null;
    SymbolTable symtab;
    SymbolTable curSymtab = null;

    public FLexer getLexer() {
        return curLexer;
    }

    public void setLexer( FLexer lexer ) {
      curLexer = lexer;
    }

    // Cetus-0.5: Declarator is now an abstract class
    public VariableDeclaration getImplicitDeclaration( Declarator decl ) {
      Specifier spec = ImplicitDeclarationManager.getType(
        (Identifier)decl.getSymbol() );
      if( spec == null ) {
        System.out.println( "ERROR: Variable " + decl.getSymbol() + " not " +
          "defined" );
        System.exit( 0 );
      }

      return new VariableDeclaration( spec, decl );
    }

    public VariableDeclarator getDeclarator( Declaration decl, Identifier id ) {

      if( !(decl instanceof VariableDeclaration) ) {
        return null;
      }

      if( decl instanceof CommonBlock ) {
        return getDeclarator( ((CommonBlock)decl).getDeclaration( id ), id );
      }

      VariableDeclaration declaration = (VariableDeclaration)decl;

      for( int i = 0; i < declaration.getNumDeclarators(); i++ ) {
        if( declaration.getDeclarator( i ).getSymbol().equals( id ) ) {
          return (VariableDeclarator)declaration.getDeclarator( i );
        }
      }

      return null;
    }

    public Declarator removeDeclarator( VariableDeclaration vd, Identifier id ) {

      for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
        if( vd.getDeclarator( i ).getSymbol().equals( id ) ) {

          Declarator retDecl = vd.getDeclarator( i );
          retDecl.setParent( null );
          vd.getChildren().remove( i );

          SymbolTable table = (SymbolTable)ObjectAnalizer.getParentOfClass( vd, SymbolTable.class );
          table.getTable().remove( id );

          return retDecl;
        }
      }

      return null;
    }

    public Declarator mixDeclarators( Declarator decl1, Declarator decl2 ) {

      if( decl1.getArraySpecifiers().size() != 0 ) {
        if( decl2.getArraySpecifiers().size() != 0 ) {
          System.err.println( "BUG: BOTH DECLARATORS HAVE ARRAY SPECIFIERS" );
          System.exit( 0 );
        }

        return decl1;
      }

      if( ( decl1.getArraySpecifiers().size() == 0 ) &&
          ( decl2.getArraySpecifiers().size() == 0 ) ) {

          return decl1;
      }

      return mixDeclarators( decl2, decl1 );
    }



protected FParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public FParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected FParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public FParser(TokenStream lexer) {
  this(lexer,2);
}

public FParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final TranslationUnit  translationUnit(
		TranslationUnit init_tunit
	) throws RecognitionException, TokenStreamException {
		TranslationUnit tunit;
		
		
		
		if( init_tunit == null ) {
		tunit = new TranslationUnit( getLexer().originalSource );
		} else {
		tunit = init_tunit;
		}
		
		symtab = tunit;
		
		
		try {      // for error handling
			{
			_loop3:
			do {
				switch ( LA(1)) {
				case PROGRAM:
				{
					mainProgram( tunit );
					break;
				}
				case SUBROUTINE:
				{
					subroutineSubprogram( tunit );
					break;
				}
				case CHARACTER:
				case COMPLEX:
				case DOUBLE:
				case FUNCTION:
				case INTEGER:
				case LOGICAL:
				case REAL:
				{
					functionSubprogram( tunit );
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
		return tunit;
	}
	
	public final void mainProgram(
		 TranslationUnit tunit 
	) throws RecognitionException, TokenStreamException {
		
		Identifier name=null; CompoundStatement body = null;
		
		try {      // for error handling
			name=programStatement();
			body=subprogramBody();
			endProgramStatement();
			
			
			Declarator mainName = new ProcedureDeclarator( new Identifier( "main" ), new ArrayList() );
			Procedure main = new Procedure( mainName, body );
			tunit.addDeclaration( main );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final void subroutineSubprogram(
		 TranslationUnit tunit 
	) throws RecognitionException, TokenStreamException {
		
		Identifier name = null; CompoundStatement body = null; List<Identifier> parameters = new ArrayList<Identifier>();
		
		try {      // for error handling
			name=subroutineStatement(parameters);
			body=subprogramBody();
			endSubroutineStatement();
			
			
			// Create procedure
			Declarator mainName = new ProcedureDeclarator( name, new ArrayList() );
			Procedure procedure = new Procedure( mainName, body );
			
			// Get subroutine parameters from body, add to the declaration and remove from body
			Iterator<Identifier> iter = parameters.iterator();
			while( iter.hasNext() ) {
			Identifier id = iter.next();
			VariableDeclaration vd = (VariableDeclaration)body.findSymbol( id );
			
			// If implicit declaration
			if( vd == null ) {
			// Cetus-0.5: Declarator is now abstract
			vd = getImplicitDeclaration( new VariableDeclarator( id ) );
			procedure.addDeclaration( vd );
			} else {
			// Else, Remove the Declarator from the VariableDeclaration
			//       Declarator oldDecl = removeDeclarator( vd, id );
			Declarator oldDecl = getDeclarator( vd, id );
			procedure.addDeclaration( new VariableDeclaration( vd.getSpecifiers(),
			(Declarator)oldDecl.clone() ) );
			
			// Remove the DeclarationStatement from the body if there are no more
			// declarators in it
			if( vd.getNumDeclarators() == 0 ) {
			Statement statement = (Statement)ObjectAnalizer.getParentOfClass( vd,
			Statement.class );
			statement.detach();
			}
			}
			}
			
			// Add procedure to the translation unit
			tunit.addDeclaration( procedure );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
	}
	
	public final void functionSubprogram(
		 TranslationUnit tunit 
	) throws RecognitionException, TokenStreamException {
		
		Identifier name = null;
		CompoundStatement body = null;
		List<Identifier> parameters = new
		ArrayList<Identifier>();
		ArrayList<Specifier> spec = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case CHARACTER:
			case COMPLEX:
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
			name=functionStatement(parameters);
			body=subprogramBody();
			endFunctionStatement();
			
			
			// Create procedure
			List<Specifier> returnType = new ArrayList<Specifier>( 1 );
			if( spec == null ) {
			returnType.add( ImplicitDeclarationManager.getType( name ) );
			} else {
			returnType.addAll( spec );
			}
			
			Declarator mainName = new ProcedureDeclarator( name, new ArrayList() );
			Procedure procedure = new Procedure( returnType, mainName, body );
			
			// Get subroutine parameters from body, add to the declaration and remove
			// from body
			Iterator<Identifier> iter = parameters.iterator();
			while( iter.hasNext() ) {
			Identifier id = iter.next();
			VariableDeclaration vd = (VariableDeclaration)body.findSymbol( id );
			
			// If the VariableDeclaration is null : implicit parameter
			if( vd == null ) {
			// Cetus-0.5: Declarator is now abstract
			vd = getImplicitDeclaration( new VariableDeclarator( id ) );
			procedure.addDeclaration( vd );
			} else {
			// Remove the Declarator from the VariableDeclaration
			Declarator oldDecl = removeDeclarator( vd, id );
			procedure.addDeclaration( new VariableDeclaration( vd.getSpecifiers(),
			oldDecl ) );
			
			// Remove the DeclarationStatement from the body if there are no more
			// declarators in it
			if( vd.getNumDeclarators() == 0 ) {
			Statement statement = (Statement)ObjectAnalizer.getParentOfClass( vd,
			Statement.class );
			statement.detach();
			}
			}
			}
			
			// Add a variable called like the function to the SymbolTable of the
			// Procedure (if there's not one already)
			if( procedure.getBody().findSymbol( name ) == null ) {
			// Cetus-0.5: Declarator is now abstract
			VariableDeclaration vd = new VariableDeclaration( returnType,
			new VariableDeclarator( (Identifier)name.clone() ) );
			TypeDeclaration cosmetic = new TypeDeclaration( returnType.get( 0 ) );
			cosmetic.addDeclarator( new VariableDeclarator(
			(Identifier)name.clone() ) );
			
			procedure.getBody().addDeclaration( vd );
			procedure.getBody().addDeclaration( cosmetic );
			}
			
			tunit.addDeclaration( procedure );
			
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
			
			ImplicitDeclarationManager.restartState( new Identifier( "main" ) );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return id;
	}
	
	public final CompoundStatement  subprogramBody() throws RecognitionException, TokenStreamException {
		 CompoundStatement body = new CompoundStatement() ;
		
		Statement statement=null; DeclarationStatement decl=null; curSymtab=body;
		body.setLineNumber( curLexer.getLine() );
		
		try {      // for error handling
			declarationPart( body );
			executablePart( body );
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
		return body;
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
	
	public final Identifier  subroutineStatement(
		 List<Identifier> parameters 
	) throws RecognitionException, TokenStreamException {
		 Identifier subroutineName=null ;
		
		List<Identifier> innerParams = null;
		
		try {      // for error handling
			match(SUBROUTINE);
			subroutineName=identifier();
			
			ImplicitDeclarationManager.restartState( subroutineName );
			
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				{
				switch ( LA(1)) {
				case ID:
				{
					innerParams=identifierList();
					
					Iterator<Identifier> iter = innerParams.iterator();
					while( iter.hasNext() ) {
					parameters.add( iter.next() );
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
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case COMPLEX:
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
		Token  num4 = null;
		
		try {      // for error handling
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
			else if ((LA(1)==DOUBLE) && (LA(2)==PRECISION)) {
				match(DOUBLE);
				match(PRECISION);
				spec.add( Specifier.DOUBLE );
			}
			else if ((LA(1)==LOGICAL)) {
				match(LOGICAL);
				spec.add( Specifier.BOOL );
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
				
				spec.add( new StringSpecifier( new IntegerLiteral( new Integer(
				num3.getText() ).intValue() ) ) );
				
			}
			else if ((LA(1)==COMPLEX) && (LA(2)==FUNCTION||LA(2)==LPAREN||LA(2)==ID)) {
				match(COMPLEX);
				
				spec.add( ComplexSpecifier.instance() );
				
			}
			else if ((LA(1)==COMPLEX) && (LA(2)==BY)) {
				match(COMPLEX);
				match(BY);
				num4 = LT(1);
				match(NUMBER);
				
				if( num4.getText().equals( "16" ) ) {
				spec.add( DoubleComplexSpecifier.instance() );
				} else {
				spec.add( ComplexSpecifier.instance() );
				}
				
			}
			else if ((LA(1)==DOUBLE) && (LA(2)==COMPLEX)) {
				match(DOUBLE);
				match(COMPLEX);
				
				spec.add( DoubleComplexSpecifier.instance() );
				
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
		}
		return spec;
	}
	
	public final Identifier  functionStatement(
		 List<Identifier> parameters 
	) throws RecognitionException, TokenStreamException {
		 Identifier functionName=null ;
		
		List<Identifier> innerParams = null;
		
		try {      // for error handling
			match(FUNCTION);
			functionName=identifier();
			
			ImplicitDeclarationManager.restartState( functionName );
			
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				{
				switch ( LA(1)) {
				case ID:
				{
					innerParams=identifierList();
					
					Iterator<Identifier> iter = innerParams.iterator();
					while( iter.hasNext() ) {
					parameters.add( iter.next() );
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
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case COMPLEX:
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
	
	public final List<Identifier>  identifierList() throws RecognitionException, TokenStreamException {
		 List<Identifier> identifiers = new ArrayList<Identifier>() ;
		
		Identifier id = null;
		
		try {      // for error handling
			id=identifier();
			identifiers.add( id );
			{
			_loop127:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					id=identifier();
					identifiers.add( id );
				}
				else {
					break _loop127;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_6);
		}
		return identifiers;
	}
	
	public final void declarationPart(
		 CompoundStatement body 
	) throws RecognitionException, TokenStreamException {
		
		Statement statement = null; curSymtab = body;
		
		try {      // for error handling
			{
			_loop27:
			do {
				if ((_tokenSet_7.member(LA(1))) && (_tokenSet_8.member(LA(2)))) {
					statement=declarationStatement( body );
					
					if( statement != null ) {
					body.addStatement( statement );
					}
				}
				else if ((LA(1)==Newline) && (_tokenSet_2.member(LA(2)))) {
					match(Newline);
				}
				else {
					break _loop27;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_9);
		}
	}
	
	public final void executablePart(
		 CompoundStatement body 
	) throws RecognitionException, TokenStreamException {
		
		Statement stmt = null;
		
		try {      // for error handling
			{
			_loop30:
			do {
				if ((_tokenSet_10.member(LA(1))) && (_tokenSet_11.member(LA(2)))) {
					stmt=executableStatement();
					match(Newline);
					body.addStatement( stmt );
				}
				else if ((LA(1)==Newline)) {
					match(Newline);
				}
				else {
					break _loop30;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
	}
	
	public final Statement  declarationStatement(
		 CompoundStatement body 
	) throws RecognitionException, TokenStreamException {
		 Statement statement = null ;
		
		Token  file = null;
		Token  common = null;
		ArrayList<Specifier> spec = null;
		List<VariableDeclarator> declarators=null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case CHARACTER:
			case COMPLEX:
			case DOUBLE:
			case INTEGER:
			case LOGICAL:
			case REAL:
			{
				spec=specifier();
				declarators=declaratorList();
				
				
				// If the declaration already exists, we add this info
				// COSMETIC SENTENCE: Just to print it correctly
				TypeDeclaration cosmetic = new TypeDeclaration( spec.get( 0 ) );
				VariableDeclaration vd = new VariableDeclaration( spec.subList( 0, 1 ) );
				for( Declarator decl: declarators ) {
				
				// If this is some kind of madness like CHARACTER*80 <identifier> we
				// change the declarator to reflect it
				if( spec.size() != 1 ) {
				decl = new VariableDeclarator( decl.getSymbol(), spec.get( 1 ) );
				}
				VariableDeclaration oldVd = (VariableDeclaration)curSymtab.findSymbol(
				decl.getSymbol() );
				if( oldVd != null ) {
				if( oldVd instanceof CommonBlock ) {
				CommonBlock commonBlock = (CommonBlock)oldVd;
				
				// We have to "mix" both declarators
				VariableDeclaration internalVd = commonBlock.getDeclaration(
				(Identifier)decl.getSymbol() );
				Declarator oldDecl = getDeclarator( internalVd,
				(Identifier)decl.getSymbol() );
				
				Declarator newDecl = mixDeclarators( oldDecl, decl );
				
				commonBlock.removeDeclaration( (Identifier)decl.getSymbol() );
				commonBlock.addDeclaration( new VariableDeclaration( spec.get(0),
				newDecl ) );
				} else {
				Declarator oldDecl = getDeclarator( oldVd,
				(Identifier)decl.getSymbol() );
				removeDeclarator( oldVd, (Identifier)oldDecl.getSymbol() );
				if( oldVd.getNumDeclarators() == 0 ) {
				DeclarationStatement stmt =
				(DeclarationStatement)ObjectAnalizer.getParentOfClass( oldVd,
				DeclarationStatement.class );
				stmt.detach();
				}
				
				Declarator newDecl = new VariableDeclarator(
				(Identifier)decl.getSymbol().clone(),
				oldDecl.getArraySpecifiers() );
				vd.addDeclarator( newDecl );
				statement = new DeclarationStatement( vd );
				}
				} else {
				vd.addDeclarator( decl );
				statement = new DeclarationStatement( vd );
				}
				
				// Add declarator to the cosmetic declaration
				cosmetic.addDeclarator( (Declarator)decl.clone() );
				}
				
				// Add cosmetic declaration to body
				Statement cosmeticStmt = new DeclarationStatement( cosmetic );
				cosmeticStmt.setLineNumber( curLexer.getLine() );
				if( statement != null ) {
				statement.setLineNumber( curLexer.getLine() );
				}
				body.addStatement( cosmeticStmt );
				
				break;
			}
			case DIMENSION:
			{
				match(DIMENSION);
				declarators=declaratorList();
				
				
				// For cosmetic purposes only
				DimensionDeclaration cosmetic = new DimensionDeclaration();
				Iterator<VariableDeclarator> iter = declarators.iterator();
				while( iter.hasNext() ) {
				VariableDeclarator decl = iter.next();
				VariableDeclaration vd = (VariableDeclaration)curSymtab.findSymbol(
				decl.getSymbol() );
				
				// It could be an IMPLICIT declaration, so if null we ask the manager
				if( vd == null ) {
				vd = getImplicitDeclaration( decl );
				body.addStatement( new DeclarationStatement( vd ) );
				} else {
				if( vd instanceof CommonBlock ) {
				CommonBlock commonBlock = (CommonBlock)vd;
				VariableDeclaration innerDeclaration = commonBlock.getDeclaration(
				(Identifier)decl.getSymbol() );
				if( innerDeclaration == null ) {
				commonBlock.addDeclaration( new VariableDeclaration( decl ) );
				} else {
				VariableDeclarator innerDecl = getDeclarator( innerDeclaration,
				(Identifier)decl.getSymbol() );
				innerDecl.getTrailingSpecifiers().clear();
				innerDecl.getTrailingSpecifiers().addAll(
				decl.getTrailingSpecifiers() );
				}
				} else {
				// If the declaration is explicit, we look for it and add the declarator
				for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
				if( vd.getDeclarator( i ).getSymbol().equals( decl.getSymbol() ) ) {
				vd.getChildren().set( i, decl );
				}
				}
				}
				}
				
				// For cosmetic purposes only
				cosmetic.addDeclarator( (Declarator)decl.clone() );
				}
				
				// For cosmetic purposes only
				body.addStatement( new DeclarationStatement( cosmetic ) );
				
				break;
			}
			case INCLUDE:
			{
				match(INCLUDE);
				file = LT(1);
				match(STRINGLITERAL);
				
				
				String fileName = file.getText();
				
				try {
				LexerSharedInputState lexerState = null;
				String [] includeDirs = ConfigurationManager.getOptionArray( "Include" );
				if( new File( fileName ).canRead() ) {
				lexerState = new LexerSharedInputState( new FileReader( new File(
				fileName ) ) );
				} else {
				// Look for the file in our include path
				for( String dir: includeDirs ) {
				if( new File( dir + File.separator + fileName ).canRead() ) {
				fileName = dir + File.separator + fileName;
				lexerState = new LexerSharedInputState( new FileReader( new File(
				fileName ) ) );
				break;
				}
				}
				}
				
				if( lexerState == null ) {
				lexerState = new LexerSharedInputState( new FileReader( new File(
				fileName ) ) );
				}
				
				FLexer lexer = new FLexer( lexerState );
				FParser parser = new FParser( lexer );
				parser.setLexer( lexer );
				body.addStatement( new DeclarationStatement( new Annotation(
				INCLUDE_ANNOTE_TEXT + " " + file.getText() ) ) );
				parser.declarationPart( body );
				body.addStatement( new DeclarationStatement( new Annotation(
				INCLUDE_ANNOTE_TEXT + " " + file.getText() ) ) );
				} catch( Exception e ) {
				throw new InternalError( e.getMessage() );
				}
				
				break;
			}
			case COMMON:
			{
				Identifier blockName = null;
				common = LT(1);
				match(COMMON);
				match(DIVIDE);
				blockName=identifier();
				match(DIVIDE);
				declarators=declaratorList();
				
				
				// For cosmetic purposes only
				CommonDeclaration cosmetic = new CommonDeclaration(
				(Identifier)blockName.clone() );
				
				// Process the declarators in the list, so if they are already in the symbol
				// table we can unify the declarations
				CommonBlock commonBlock = new CommonBlock( blockName );
				for( VariableDeclarator newDecl: declarators ) {
				VariableDeclaration vd = (VariableDeclaration)curSymtab.findSymbol(
				newDecl.getSymbol() );
				if( vd != null ) {
				VariableDeclarator oldDecl = getDeclarator( vd,
				(Identifier)newDecl.getSymbol() );
				removeDeclarator( vd, (Identifier)oldDecl.getSymbol() );
				
				if( vd.getNumDeclarators() == 0 ) {
				DeclarationStatement stmt =
				(DeclarationStatement)ObjectAnalizer.getParentOfClass( vd,
				DeclarationStatement.class );
				stmt.detach();
				}
				commonBlock.addDeclarator( newDecl );
				if( newDecl.getTrailingSpecifiers().size() != 0 ) {
				oldDecl.getTrailingSpecifiers().clear();
				oldDecl.getTrailingSpecifiers().addAll(
				newDecl.getTrailingSpecifiers() );
				//           newDecl.getTrailingSpecifiers().clear();
				}
				commonBlock.addDeclaration( new VariableDeclaration(
				(Specifier)vd.getSpecifiers().get( 0 ), oldDecl ) );
				//         cosmetic.addDeclarator( (Declarator)oldDecl.clone() );
				cosmetic.addDeclarator( (Declarator)newDecl.clone() );
				} else {
				commonBlock.addDeclarator( new VariableDeclarator(
				(Identifier)newDecl.getSymbol().clone() ) );
				commonBlock.addDeclaration( new VariableDeclaration(
				ImplicitDeclarationManager.getType( (Identifier)newDecl.getSymbol() ),
				newDecl ) );
				cosmetic.addDeclarator( (Declarator)newDecl.clone() );
				}
				}
				
				statement = new DeclarationStatement( commonBlock );
				statement.setLineNumber( common.getLine() );
				
				// For cosmetic purposes only
				body.addStatement( new DeclarationStatement( cosmetic ) );
				
				break;
			}
			case IMPLICIT:
			{
				List<ImplicitRegister> registers=null;
				match(IMPLICIT);
				{
				switch ( LA(1)) {
				case NONE:
				{
					match(NONE);
					
					ImplicitDeclarationManager.setImplicitNone();
					statement = new DeclarationStatement( ImplicitRegister.IMPLICIT_NONE );
					break;
				}
				case CHARACTER:
				case COMPLEX:
				case DOUBLE:
				case INTEGER:
				case LOGICAL:
				case REAL:
				{
					spec=specifier();
					match(LPAREN);
					registers=implicitList(spec.get( 0 ));
					match(RPAREN);
					
					for( ImplicitRegister reg: registers ) {
					ImplicitDeclarationManager.addRegister( reg );
					body.addStatement( new DeclarationStatement( reg ) );
					}
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
				List<Expression> parameters = null;
				match(PARAMETER);
				match(LPAREN);
				parameters=parameterList();
				match(RPAREN);
				
				
				// For cosmetic purposes only
				ParameterDeclaration cosmetic = new ParameterDeclaration();
				
				for( Expression expr: parameters ) {
				AssignmentExpression assignment = (AssignmentExpression)expr;
				Identifier lhs = (Identifier)assignment.getLHS();
				
				Expression rhs = assignment.getRHS();
				
				Declarator declarator = new VariableDeclarator( (Identifier)lhs.clone() );
				Initializer initializer = new Initializer( rhs );
				
				VariableDeclaration oldVd = (VariableDeclaration)curSymtab.findSymbol(
				lhs );
				
				if( oldVd == null ) {
				List<Specifier> specifiers = new ArrayList<Specifier>( 2 );
				specifiers.add( ImplicitDeclarationManager.getType( lhs ) );
				specifiers.add( Specifier.CONST );
				declarator.setInitializer( initializer );
				VariableDeclaration vd = new VariableDeclaration( specifiers,
				declarator );
				body.addStatement( new DeclarationStatement( vd ) );
				} else {
				getDeclarator( oldVd, lhs ).setInitializer( initializer );
				if( !oldVd.getSpecifiers().contains( Specifier.CONST ) ) {
				oldVd.getSpecifiers().add( Specifier.CONST );
				}
				}
				
				// For cosmetic purposes only
				Declarator cosmeticDeclarator = new VariableDeclarator(
				(Identifier)lhs.clone() );
				cosmeticDeclarator.setInitializer( new Initializer(
				(Expression)rhs.clone() ) );
				cosmetic.addDeclarator( cosmeticDeclarator );
				}
				
				// For cosmetic purposes only
				body.addStatement( new DeclarationStatement( cosmetic ) );
				
				break;
			}
			case DATA:
			{
				match(DATA);
				statement=dataDeclaration();
				body.addStatement( statement );
				{
				_loop35:
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
						statement=dataDeclaration();
						
						body.addStatement( statement );
					}
					else {
						break _loop35;
					}
					
				} while (true);
				}
				statement=null;
				break;
			}
			case INTRINSIC:
			{
				List<Identifier> names = null;
				match(INTRINSIC);
				names=identifierList();
				
				statement = new DeclarationStatement( new IntrinsicDeclaration( names ) );
				
				break;
			}
			case EXTERNAL:
			{
				List<Identifier> names = null;
				match(EXTERNAL);
				names=identifierList();
				
				statement = new DeclarationStatement( new ExternalDeclaration( names ) );
				
				break;
			}
			default:
				if ((LA(1)==SAVE) && (LA(2)==ID)) {
					List<Identifier> names = null;
					match(SAVE);
					names=identifierList();
					
					
					// For cosmetic purposes only
					SaveDeclaration cosmetic = new SaveDeclaration();
					for( Identifier name: names ) {
					
					Declaration declaration = curSymtab.findSymbol( name );
					
					if( ( declaration != null ) &&
					!(declaration instanceof VariableDeclaration) ) {
					
					throw new InternalError( "Bug in save declaration statement: saving " +
					"non variable-declarated identifier" );
					}
					
					VariableDeclaration vd = (VariableDeclaration)declaration;
					
					if( vd == null ) {
					Declarator decl = new VariableDeclarator( name );
					ArrayList<Specifier> specs = new ArrayList<Specifier>( 2 );
					specs.add( Specifier.STATIC );
					specs.add( ImplicitDeclarationManager.getType( name ) );
					vd = new VariableDeclaration( specs, decl );
					} else {
					Declarator decl = removeDeclarator( vd, name );
					
					if( vd.getNumDeclarators() == 0 ) {
					Statement stmt = (Statement)ObjectAnalizer.getParentOfClass( vd,
					Statement.class );
					stmt.detach();
					}
					
					ArrayList<Specifier> specs = new ArrayList<Specifier>( 2 );
					specs.add( Specifier.STATIC );
					specs.add( (Specifier)vd.getSpecifiers().get( 0 ) );
					vd = new VariableDeclaration( specs, decl );
					}
					
					body.addStatement( new DeclarationStatement( vd ) );
					cosmetic.addDeclarator( new VariableDeclarator(
					(Identifier)name.clone() ) );
					}
					
					body.addStatement( new DeclarationStatement( cosmetic ) );
					
				}
				else if ((LA(1)==SAVE) && (LA(2)==DIVIDE)) {
					List<Identifier> names = null;
					match(SAVE);
					match(DIVIDE);
					names=identifierList();
					match(DIVIDE);
					
					
					for( Identifier name: names ) {
					
					DepthFirstIterator iter = new DepthFirstIterator( body );
					while( iter.hasNext() ) {
					
					Declaration declaration = null;
					try {
					declaration = (Declaration)iter.next( Declaration.class );
					} catch( NoSuchElementException e ) {}
					
					if( declaration instanceof CommonBlock ) {
					CommonBlock cb = (CommonBlock)declaration;
					if( cb.getBlockName().equals( name ) ) {
					cb.getSpecifiers().add( 0, Specifier.STATIC );
					continue;
					}
					}
					}
					}
					
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
		return statement;
	}
	
	public final Statement  executableStatement() throws RecognitionException, TokenStreamException {
		 Statement statement = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case PRAGMA:
			{
				statement=pragmaStatement();
				break;
			}
			case CALL:
			case CLOSE:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case STOP:
			case WRITE:
			case ID:
			{
				statement=expressionStatement();
				break;
			}
			case IF:
			{
				statement=ifStatement();
				break;
			}
			case RETURN:
			{
				statement=returnStatement();
				break;
			}
			case LABEL:
			{
				statement=labeledStatement();
				break;
			}
			case GO:
			case GOTOTOKEN:
			{
				statement=gotoStatement();
				break;
			}
			default:
				if ((LA(1)==DO) && (LA(2)==ID||LA(2)==NUMBER)) {
					statement=doLoopStatement();
				}
				else if ((LA(1)==DO) && (LA(2)==WHILE)) {
					statement=whileStatement();
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
		return statement;
	}
	
	public final List<VariableDeclarator>  declaratorList() throws RecognitionException, TokenStreamException {
		 List<VariableDeclarator> declarators =
              new ArrayList<VariableDeclarator>() ;
		
		VariableDeclarator decl = null;
		
		try {      // for error handling
			decl=declarator();
			declarators.add( decl );
			{
			_loop105:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					decl=declarator();
					declarators.add( decl );
				}
				else {
					break _loop105;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return declarators;
	}
	
	public final List<ImplicitRegister>  implicitList(
		Specifier spec
	) throws RecognitionException, TokenStreamException {
		 List<ImplicitRegister> registers = new
    ArrayList<ImplicitRegister>() ;
		
		ImplicitRegister reg = null;
		
		try {      // for error handling
			reg=implicitRegister(spec);
			registers.add( reg );
			{
			_loop130:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					reg=implicitRegister(spec);
					registers.add( reg );
				}
				else {
					break _loop130;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
		return registers;
	}
	
	public final List<Expression>  parameterList() throws RecognitionException, TokenStreamException {
		 List<Expression> parameters = new ArrayList<Expression>() ;
		
		Expression expr = null;
		
		try {      // for error handling
			expr=parameter();
			parameters.add( expr );
			{
			_loop115:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					expr=parameter();
					parameters.add( expr );
				}
				else {
					break _loop115;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_17);
		}
		return parameters;
	}
	
	public final Statement  dataDeclaration() throws RecognitionException, TokenStreamException {
		 Statement s = null; ;
		
		List<Expression> expressions = null; List<Expression> initializers=null;
		
		try {      // for error handling
			expressions=dataDeclarationExpressionList();
			match(DIVIDE);
			initializers=dataDeclarationInitializerList();
			match(DIVIDE);
			
			
			s = new DeclarationStatement( new DataDeclaration( expressions,
			initializers ) );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_18);
		}
		return s;
	}
	
	public final List<Expression>  dataDeclarationExpressionList() throws RecognitionException, TokenStreamException {
		 List<Expression> expressions = new ArrayList<Expression>() ;
		
		Expression expr = null;
		
		try {      // for error handling
			expr=dataDeclarationExpression();
			expressions.add( expr );
			{
			_loop39:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					expr=dataDeclarationExpression();
					expressions.add( expr );
				}
				else {
					break _loop39;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
		}
		return expressions;
	}
	
	public final List<Expression>  dataDeclarationInitializerList() throws RecognitionException, TokenStreamException {
		 List<Expression> initializers = new ArrayList<Expression>() ;
		
		Expression expr=null;
		
		try {      // for error handling
			expr=dataDeclarationInitializer();
			initializers.add( expr );
			{
			_loop43:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					expr=dataDeclarationInitializer();
					initializers.add( expr );
				}
				else {
					break _loop43;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
		}
		return initializers;
	}
	
	public final Expression  dataDeclarationExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			if ((LA(1)==ID) && (LA(2)==COMMA||LA(2)==DIVIDE)) {
				expr=identifier();
			}
			else if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
				expr=arrayAccessOrFunctionCall();
			}
			else if ((LA(1)==LPAREN)) {
				expr=impliedDoLoop();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_20);
		}
		return expr;
	}
	
	public final Expression  arrayAccessOrFunctionCall() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Identifier symbol = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				symbol=identifier();
				match(LPAREN);
				
				
				Declaration decl = curSymtab.findSymbol( symbol );
				Declarator declarator = getDeclarator( decl, symbol );
				
				// An array must have been defined. Not applied to a procedure.
				VariableDeclaration vd = null;
				
				// First of all: if it is for sure a function: done
				Procedure queryResult = FortranProcedureManager.query( symbol );
				if( ( queryResult != null ) &&
				( queryResult.getReturnType() != null ) ) {
				expr = finishFunctionCall( symbol );
				}
				
				// If not, we should get the VariableDeclaration for it
				if( (expr == null) && (declarator != null) ) {
				vd = (VariableDeclaration)decl;
				if( vd instanceof CommonBlock ) {
				vd = ((CommonBlock)vd).getDeclaration( symbol );
				}
				
				// If this variable is a string (CHARACTER*xx) we may find a substring
				// expression
				if( ( vd != null ) && ( vd.getSpecifiers().get( 0 ) instanceof
				StringSpecifier ) ) {
				
				expr = finishSubstringExpression( symbol );
				} else {
				// If not, if this is an array, just finish the array access
				if( declarator.getArraySpecifiers().size() != 0 ) {
				expr = finishArrayAccess( symbol );
				}
				}
				}
				
				// If the declarator is not null, then this has to be a call for a not-known
				// function
				if( expr == null ) {
				expr = finishFunctionCall( symbol );
				}
				
				break;
			}
			case REAL:
			{
				symbol=builtInFunctionWithReservedName();
				match(LPAREN);
				
				expr = finishFunctionCall( symbol );
				
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
			recover(ex,_tokenSet_21);
		}
		return expr;
	}
	
	public final Expression  impliedDoLoop() throws RecognitionException, TokenStreamException {
		 Expression expr=null ;
		
		List<Expression> objects = new ArrayList<Expression>();
		Identifier doVar = null; Expression start = null; Expression stop = null;
		Expression step = null;
		
		try {      // for error handling
			match(LPAREN);
			expr=ioParameter();
			{
			switch ( LA(1)) {
			case COMMA:
			{
				objects.add( expr );
				match(COMMA);
				{
				_loop101:
				do {
					if ((_tokenSet_22.member(LA(1))) && (_tokenSet_23.member(LA(2)))) {
						expr=ioParameter();
						match(COMMA);
						objects.add( expr );
					}
					else {
						break _loop101;
					}
					
				} while (true);
				}
				doVar=identifier();
				match(EQUALS);
				start=expression();
				match(COMMA);
				stop=expression();
				{
				switch ( LA(1)) {
				case COMMA:
				{
					match(COMMA);
					step=expression();
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
				
				
				if( step == null ) {
				expr = new ImpliedDoLoop( objects, doVar, start, stop );
				} else {
				expr = new ImpliedDoLoop( objects, doVar, start, stop, step );
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
			recover(ex,_tokenSet_24);
		}
		return expr;
	}
	
	public final Expression  dataDeclarationInitializer() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			expr=expression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_20);
		}
		return expr;
	}
	
	public final Expression  expression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null;
		
		try {      // for error handling
			expr=conditionalExpression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_25);
		}
		return expr;
	}
	
	public final DeclarationStatement  pragmaStatement() throws RecognitionException, TokenStreamException {
		 DeclarationStatement statement = null ;
		
		Token  family = null;
		Token  value = null;
		
		try {      // for error handling
			family = LT(1);
			match(PRAGMA);
			value = LT(1);
			match(PRAGMAVALUE);
			
			
			Annotation annote = new Annotation( family.getText().trim() + " " +
			value.getText().trim() );
			annote.setPrintMethod( Annotation.print_as_pragma_method );
			statement = new DeclarationStatement( annote );
			statement.setLineNumber( curLexer.getLine() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return statement;
	}
	
	public final FortranDoLoop  doLoopStatement() throws RecognitionException, TokenStreamException {
		 FortranDoLoop stmt = null ;
		
		Token  doTok = null;
		Token  label = null;
		Identifier loopId = null; Expression start = null; Expression end = null;
		Expression step = null;
		CompoundStatement loopBody = new CompoundStatement();
		
		try {      // for error handling
			doTok = LT(1);
			match(DO);
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				label = LT(1);
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
			loopId=identifier();
			match(EQUALS);
			start=doLimit();
			match(COMMA);
			end=doLimit();
			{
			switch ( LA(1)) {
			case COMMA:
			{
				match(COMMA);
				step=expression();
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
			executablePart( loopBody );
			endDoLoopStatement( label );
			
			
			Identifier labelObj = null;
			
			if( label != null ) {
			labelObj = new Identifier( label.getText() );
			}
			
			stmt = new FortranDoLoop( labelObj, loopId, start, end, step, loopBody );
			stmt.setLineNumber( doTok.getLine() );
			loopBody.setLineNumber( doTok.getLine()+1 );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return stmt;
	}
	
	public final ExpressionStatement  expressionStatement() throws RecognitionException, TokenStreamException {
		 ExpressionStatement statement = null ;
		
		Expression expr = null; Expression rhs = null;
		List<Expression> varargs = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case REAL:
			case ID:
			{
				expr=assignmentExpression();
				
				statement = new ExpressionStatement( expr );
				statement.setLineNumber( curLexer.getLine() );
				
				break;
			}
			case CALL:
			{
				expr=procedureCallStatement();
				
				
				statement = new ExpressionStatement( expr );
				statement.setLineNumber( curLexer.getLine()-1 );
				
				break;
			}
			case CLOSE:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case STOP:
			case WRITE:
			{
				expr=predefinedFunctionStatement();
				
				
				statement = new ExpressionStatement( expr );
				statement.setLineNumber( curLexer.getLine()-1 );
				
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
		return statement;
	}
	
	public final IfStatement  ifStatement() throws RecognitionException, TokenStreamException {
		 IfStatement statement = null ;
		
		Token  ifTok = null;
		Expression condition=null; Statement trueClause=null;
		Statement falseClause=null;
		
		try {      // for error handling
			ifTok = LT(1);
			match(IF);
			match(LPAREN);
			condition=conditionalExpression();
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
				trueClause=executableStatement();
				
				statement = new IfStatement( condition, trueClause );
				break;
			}
			case THEN:
			{
				trueClause = new CompoundStatement();
				match(THEN);
				executablePart( (CompoundStatement)trueClause );
				
				
				statement = new IfStatement( condition, trueClause );
				
				{
				if ((_tokenSet_26.member(LA(1))) && (_tokenSet_27.member(LA(2)))) {
					falseClause = new CompoundStatement(); Expression innerCondition=null;
					{
					_loop72:
					do {
						if ((LA(1)==ELSEIF)) {
							match(ELSEIF);
							match(LPAREN);
							innerCondition=conditionalExpression();
							match(RPAREN);
							match(THEN);
							executablePart( (CompoundStatement)falseClause );
							
							
							IfStatement innerElse = statement;
							
							while( innerElse.getElseStatement() != null ) {
							innerElse =
							(IfStatement)innerElse.getElseStatement().getChildren().get( 0 );
							}
							
							CompoundStatement elseStatement = new CompoundStatement();
							elseStatement.addStatement( new IfStatement( innerCondition,
							falseClause ) );
							innerElse.setElseStatement( elseStatement );
							falseClause = new CompoundStatement();
							
						}
						else {
							break _loop72;
						}
						
					} while (true);
					}
					{
					switch ( LA(1)) {
					case ELSE:
					{
						falseClause = new CompoundStatement();
						match(ELSE);
						executablePart( (CompoundStatement)falseClause );
						
						
						IfStatement innerElse = statement;
						
						while( innerElse.getElseStatement() != null ) {
						innerElse = (IfStatement)
						innerElse.getElseStatement().getChildren().get( 0 );
						}
						
						innerElse.setElseStatement( falseClause );
						
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
			
			
			statement.setLineNumber( ifTok.getLine() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return statement;
	}
	
	public final ReturnStatement  returnStatement() throws RecognitionException, TokenStreamException {
		 ReturnStatement statement = null ;
		
		Token  rTok = null;
		
		try {      // for error handling
			rTok = LT(1);
			match(RETURN);
			
			statement = new ReturnStatement();
			statement.setLineNumber( rTok.getLine() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return statement;
	}
	
	public final Statement  labeledStatement() throws RecognitionException, TokenStreamException {
		 Statement statement = null ;
		
		Token  label = null;
		
		try {      // for error handling
			label = LT(1);
			match(LABEL);
			{
			switch ( LA(1)) {
			case CALL:
			case CLOSE:
			case OPEN:
			case PAUSE:
			case PRINT:
			case READ:
			case REAL:
			case STOP:
			case WRITE:
			case ID:
			{
				statement=expressionStatement();
				break;
			}
			case IF:
			{
				statement=ifStatement();
				break;
			}
			case FORMAT:
			{
				statement=formatStatement();
				break;
			}
			case RETURN:
			{
				statement=returnStatement();
				break;
			}
			default:
				if ((LA(1)==DO) && (LA(2)==ID||LA(2)==NUMBER)) {
					statement=doLoopStatement();
				}
				else if ((LA(1)==DO) && (LA(2)==WHILE)) {
					statement=whileStatement();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
			
			CompoundStatement noBody = new CompoundStatement();
			noBody.addStatement( new Label( new Identifier( label.getText() ) ) );
			noBody.addStatement( statement );
			statement = noBody;
			statement.setLineNumber( label.getLine() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return statement;
	}
	
	public final WhileLoop  whileStatement() throws RecognitionException, TokenStreamException {
		 WhileLoop stmt = null ;
		
		Token  doTok = null;
		Token  whileTok = null;
		Expression condition = null;
		CompoundStatement loopBody = new CompoundStatement();
		
		try {      // for error handling
			doTok = LT(1);
			match(DO);
			whileTok = LT(1);
			match(WHILE);
			match(LPAREN);
			condition=conditionalExpression();
			match(RPAREN);
			executablePart( loopBody );
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
			
			
			stmt = new WhileLoop( condition, loopBody );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return stmt;
	}
	
	public final Statement  gotoStatement() throws RecognitionException, TokenStreamException {
		 Statement stmt = null ;
		
		Token  label = null;
		
		try {      // for error handling
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
				label = LT(1);
				match(NUMBER);
				
				stmt = new GotoStatement( new Identifier( label.getText() ) );
				
			}
			else if ((LA(1)==GO||LA(1)==GOTOTOKEN) && (LA(2)==TO||LA(2)==LPAREN)) {
				List<IntegerLiteral> parameters = null; Expression expr = null;
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
				parameters=integerLiteralList();
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
				expr=expression();
				
				
				List<Label> labels = new ArrayList<Label>( parameters.size() );
				for( IntegerLiteral l: parameters ) {
				labels.add( new Label( new Identifier( l.toString() ) ) );
				}
				
				stmt = new ComputedGotoStatement( labels, expr );
				
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return stmt;
	}
	
	public final FormatStatement  formatStatement() throws RecognitionException, TokenStreamException {
		 FormatStatement statement = null ;
		
		List<Expression> parts = null;
		
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
			if ((LA(1)==FORMAT_WHITESPACE) && (_tokenSet_28.member(LA(2)))) {
				match(FORMAT_WHITESPACE);
			}
			else if ((_tokenSet_28.member(LA(1))) && (_tokenSet_29.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			parts=formatParameterList();
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
			
			
			statement = new FormatStatement( parts );
			curLexer.toggleFormat();
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return statement;
	}
	
	public final Expression  doLimit() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			expr=additiveExpression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_30);
		}
		return expr;
	}
	
	public final void endDoLoopStatement(
		 Token t 
	) throws RecognitionException, TokenStreamException {
		
		Token  l = null;
		
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
				l = LT(1);
				match(LABEL);
				match(CONTINUE);
				
				
				if( t!= null ) {
				if( !l.getText().equals( t.getText() ) ) {
				throw new InternalError( "line " + curLexer.getLine() + ": CONTINUE " +
				"label does not match DO LOOP label" );
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
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
	}
	
	public final Expression  conditionalExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			expr=logicalOrExpression();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_25);
		}
		return expr;
	}
	
	public final List<IntegerLiteral>  integerLiteralList() throws RecognitionException, TokenStreamException {
		 List<IntegerLiteral> list = new ArrayList<IntegerLiteral>() ;
		
		IntegerLiteral integer = null;
		
		try {      // for error handling
			integer=integerLiteral();
			list.add( integer );
			{
			_loop93:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					integer=integerLiteral();
					list.add( integer );
				}
				else {
					break _loop93;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
		return list;
	}
	
	public final Expression  additiveExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null; BinaryOperator op = null;
		
		try {      // for error handling
			expr=multiplicativeExpression();
			{
			_loop148:
			do {
				if ((LA(1)==ADD||LA(1)==CONCATENATION||LA(1)==DASH)) {
					op=additiveOperator();
					rhs=multiplicativeExpression();
					
					expr = new BinaryExpression( expr, op, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop148;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_31);
		}
		return expr;
	}
	
	public final Expression  assignmentExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null;
		
		try {      // for error handling
			expr=assignmentTerminal();
			{
			switch ( LA(1)) {
			case EQUALS:
			{
				match(EQUALS);
				rhs=expression();
				
				expr = new AssignmentExpression( expr, AssignmentOperator.NORMAL, rhs );
				
				break;
			}
			case COLON:
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
			recover(ex,_tokenSet_32);
		}
		return expr;
	}
	
	public final Expression  procedureCallStatement() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Identifier name = null;
		List<Expression> parameters = new ArrayList<Expression>();
		
		try {      // for error handling
			match(CALL);
			name=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				{
				switch ( LA(1)) {
				case REAL:
				case ADD:
				case BY:
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
					parameters=parameterList();
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
			
			expr = new FunctionCall( name, parameters );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return expr;
	}
	
	public final Expression  predefinedFunctionStatement() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Token  stop = null;
		Token  pause = null;
		Token  write = null;
		Token  read = null;
		Token  iop = null;
		Token  open = null;
		Token  close = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case STOP:
			{
				StringLiteral lit = null;
				ArrayList<Expression> param = new ArrayList<Expression>( 1 );
				stop = LT(1);
				match(STOP);
				{
				switch ( LA(1)) {
				case STRINGLITERAL:
				{
					lit=stringLiteral();
					param.add( lit );
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
				
				expr = new IOCall( new Identifier( stop.getText().toUpperCase() ), param );
				
				break;
			}
			case PAUSE:
			{
				StringLiteral lit = null;
				ArrayList<Expression> param = new ArrayList<Expression>( 1 );
				pause = LT(1);
				match(PAUSE);
				{
				switch ( LA(1)) {
				case STRINGLITERAL:
				{
					lit=stringLiteral();
					param.add( lit );
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
				
				expr = new IOCall( new Identifier( pause.getText() ), param );
				
				break;
			}
			case READ:
			case WRITE:
			{
				List<Expression> parameters = null; List<Expression> varargs = null;
				{
				switch ( LA(1)) {
				case WRITE:
				{
					write = LT(1);
					match(WRITE);
					break;
				}
				case READ:
				{
					read = LT(1);
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
				parameters=parameterList();
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
					varargs=ioParameterList();
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
				
				
				if( varargs == null ) {
				varargs = new ArrayList<Expression>( 0 );
				}
				
				Identifier fname = null;
				if( write != null ) {
				fname = new Identifier( write.getText().toUpperCase() );
				} else {
				fname = new Identifier( read.getText().toUpperCase() );
				}
				
				expr = new IOCall( fname, parameters, varargs );
				
				break;
			}
			case PRINT:
			{
				Expression format=null; List<Expression> varargs = null;
				iop = LT(1);
				match(PRINT);
				format=parameter();
				match(COMMA);
				varargs=ioParameterList();
				
				
				List<Expression> allParams = new ArrayList<Expression>(
				varargs.size() + 1 );
				allParams.add( format );
				allParams.addAll( varargs );
				expr = new IOCall( new Identifier( iop.getText().toUpperCase() ),
				allParams );
				
				break;
			}
			case OPEN:
			{
				List<Expression> parameters = null;
				open = LT(1);
				match(OPEN);
				match(LPAREN);
				parameters=parameterList();
				match(RPAREN);
				
				expr = new IOCall( new Identifier( open.getText().toUpperCase() ),
				parameters, new ArrayList<Expression>( 0 ) );
				
				break;
			}
			case CLOSE:
			{
				List<Expression> parameters = null;
				close = LT(1);
				match(CLOSE);
				match(LPAREN);
				parameters=parameterList();
				match(RPAREN);
				
				expr = new IOCall( new Identifier( close.getText().toUpperCase() ),
				parameters, new ArrayList<Expression>( 0 ) );
				
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
		return expr;
	}
	
	public final Expression  assignmentTerminal() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
				expr=arrayAccessOrFunctionCall();
			}
			else if ((LA(1)==ID) && (_tokenSet_33.member(LA(2)))) {
				expr=identifier();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_33);
		}
		return expr;
	}
	
	public final Expression  finishAssignmentStatement(
		 Expression expr 
	) throws RecognitionException, TokenStreamException {
		 Expression retExpr = null ;
		
		Expression rhs=null;
		
		try {      // for error handling
			match(EQUALS);
			rhs=expression();
			
			retExpr = new AssignmentExpression( expr, AssignmentOperator.NORMAL, rhs );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return retExpr;
	}
	
	public final void finishFunctionCallStatement(
		 FunctionCall call 
	) throws RecognitionException, TokenStreamException {
		
		List<Expression> varargs = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case REAL:
			case ADD:
			case BY:
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
				varargs=parameterList();
				match(Newline);
				
				
				for( Expression expr: varargs ) {
				call.addArgument( expr );
				}
				
				break;
			}
			case Newline:
			{
				match(Newline);
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
			recover(ex,_tokenSet_0);
		}
	}
	
	public final List<Expression>  formatParameterList() throws RecognitionException, TokenStreamException {
		 List<Expression> parameters = new ArrayList<Expression>() ;
		
		List<Expression> param = null;
		
		try {      // for error handling
			param=formatParameter();
			parameters.addAll( param );
			{
			_loop119:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					param=formatParameter();
					parameters.addAll( param );
				}
				else {
					break _loop119;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_34);
		}
		return parameters;
	}
	
	public final StringLiteral  stringLiteral() throws RecognitionException, TokenStreamException {
		 StringLiteral literal = null ;
		
		Token  value = null;
		
		try {      // for error handling
			value = LT(1);
			match(STRINGLITERAL);
			literal = new StringLiteral( value.getText() );
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_35);
		}
		return literal;
	}
	
	public final List<Expression>  ioParameterList() throws RecognitionException, TokenStreamException {
		 List<Expression> varargs=new ArrayList<Expression>() ;
		
		Expression expr = null;
		
		try {      // for error handling
			expr=ioParameter();
			varargs.add( expr );
			{
			_loop96:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					expr=ioParameter();
					varargs.add( expr );
				}
				else {
					break _loop96;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_15);
		}
		return varargs;
	}
	
	public final Expression  parameter() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression ubound=null;
		
		try {      // for error handling
			if ((_tokenSet_22.member(LA(1))) && (_tokenSet_36.member(LA(2)))) {
				expr=expression();
				expr.setParens( false );
			}
			else if ((LA(1)==REAL||LA(1)==ID) && (_tokenSet_37.member(LA(2)))) {
				expr=assignmentExpression();
				expr.setParens( false );
			}
			else if ((LA(1)==BY)) {
				match(BY);
				expr = new Identifier( "*" );
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_32);
		}
		return expr;
	}
	
	public final IntegerLiteral  integerLiteral() throws RecognitionException, TokenStreamException {
		 IntegerLiteral literal = null ;
		
		Token  value = null;
		
		try {      // for error handling
			value = LT(1);
			match(NUMBER);
			
			literal = new IntegerLiteral( new Integer( value.getText() ).intValue() );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
		return literal;
	}
	
	public final Expression  ioParameter() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		
		try {      // for error handling
			if ((LA(1)==LPAREN) && (_tokenSet_22.member(LA(2)))) {
				expr=impliedDoLoop();
			}
			else if ((_tokenSet_22.member(LA(1))) && (_tokenSet_39.member(LA(2)))) {
				expr=expression();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_40);
		}
		return expr;
	}
	
	public final VariableDeclarator  declarator() throws RecognitionException, TokenStreamException {
		 VariableDeclarator decl = null ;
		
		Identifier name = null; List<FortranArraySpecifier> dimensions = null;
		
		try {      // for error handling
			name=identifier();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				dimensions=dimensionList();
				match(RPAREN);
				break;
			}
			case CALL:
			case CHARACTER:
			case CLOSE:
			case COMMON:
			case COMPLEX:
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
			
			if( dimensions == null ) {
			decl = new VariableDeclarator( name );
			} else {
			decl = new VariableDeclarator( name, dimensions );
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_41);
		}
		return decl;
	}
	
	public final List<FortranArraySpecifier>  dimensionList() throws RecognitionException, TokenStreamException {
		 List<FortranArraySpecifier> dimensions =
    new ArrayList<FortranArraySpecifier>() ;
		
		FortranArraySpecifier spec = null;
		
		try {      // for error handling
			spec=dimension();
			dimensions.add( spec );
			{
			_loop110:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					spec=dimension();
					dimensions.add( spec );
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
		return dimensions;
	}
	
	public final FortranArraySpecifier  dimension() throws RecognitionException, TokenStreamException {
		 FortranArraySpecifier specifier = null ;
		
		Expression lBound = null; Expression uBound = null;
		
		try {      // for error handling
			lBound=parameter();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				uBound=parameter();
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
			
			
			if( uBound == null ) {
			specifier = new FortranArraySpecifier( lBound );
			} else {
			specifier = new FortranArraySpecifier( lBound, uBound );
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_42);
		}
		return specifier;
	}
	
	public final List<Expression>  formatParameter() throws RecognitionException, TokenStreamException {
		 List<Expression> params = new ArrayList<Expression>(); ;
		
		Token  text = null;
		Token  white = null;
		StringLiteral strLit = null; String str=""; String aux=null;
		List<Expression> innerParams=null;
		
		try {      // for error handling
			{
			int _cnt124=0;
			_loop124:
			do {
				switch ( LA(1)) {
				case FORMAT_NEWLINE:
				{
					match(FORMAT_NEWLINE);
					str += "/";
					break;
				}
				case STRINGLITERAL:
				{
					strLit=stringLiteral();
					str += "'" + strLit.getValue() + "'";
					break;
				}
				case FORMAT_TEXT:
				{
					{
					int _cnt123=0;
					_loop123:
					do {
						if ((LA(1)==FORMAT_TEXT) && (_tokenSet_29.member(LA(2)))) {
							text = LT(1);
							match(FORMAT_TEXT);
							str += text.getText();
						}
						else {
							if ( _cnt123>=1 ) { break _loop123; } else {throw new NoViableAltException(LT(1), getFilename());}
						}
						
						_cnt123++;
					} while (true);
					}
					break;
				}
				case LPAREN:
				{
					match(LPAREN);
					innerParams=formatParameterList();
					match(RPAREN);
					
					str += innerParams.toString().replace( "[", "(" ).replace( "]", ")");
					break;
				}
				default:
					if ((LA(1)==FORMAT_WHITESPACE) && (_tokenSet_29.member(LA(2)))) {
						white = LT(1);
						match(FORMAT_WHITESPACE);
						str += white.getText();
					}
				else {
					if ( _cnt124>=1 ) { break _loop124; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt124++;
			} while (true);
			}
			params.add( new FormatExpression( str ) );
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_43);
		}
		return params;
	}
	
	public final ImplicitRegister  implicitRegister(
		Specifier spec
	) throws RecognitionException, TokenStreamException {
		 ImplicitRegister register = null ;
		
		Token  beginChar = null;
		Token  endChar = null;
		
		try {      // for error handling
			beginChar = LT(1);
			matchNot(EOF);
			match(DASH);
			endChar = LT(1);
			matchNot(EOF);
			
			return new ImplicitRegister( beginChar.getText().toUpperCase().charAt( 0 ),
			endChar.getText().toUpperCase().charAt( 0 ), spec );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_42);
		}
		return register;
	}
	
	public final Expression  logicalOrExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null;
		
		try {      // for error handling
			expr=logicalAndExpression();
			{
			_loop136:
			do {
				if ((LA(1)==LOGICAL_OR)) {
					match(LOGICAL_OR);
					rhs=logicalAndExpression();
					
					expr = new BinaryExpression( expr, BinaryOperator.LOGICAL_OR, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop136;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_25);
		}
		return expr;
	}
	
	public final Expression  logicalAndExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null;
		
		try {      // for error handling
			expr=logicalNotExpression();
			{
			_loop139:
			do {
				if ((LA(1)==LOGICAL_AND)) {
					match(LOGICAL_AND);
					rhs=logicalNotExpression();
					
					expr = new BinaryExpression( expr, BinaryOperator.LOGICAL_AND, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop139;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_44);
		}
		return expr;
	}
	
	public final Expression  logicalNotExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		int negs = 0;
		
		try {      // for error handling
			{
			_loop142:
			do {
				if ((LA(1)==LOGICAL_NEGATION)) {
					match(LOGICAL_NEGATION);
					negs++;
				}
				else {
					break _loop142;
				}
				
			} while (true);
			}
			expr=logicalComparisonExpression();
			
			for( int i = 0; i < negs; i++ ) {
			expr = new UnaryExpression( UnaryOperator.LOGICAL_NEGATION, expr );
			expr.setParens( false );
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_45);
		}
		return expr;
	}
	
	public final Expression  logicalComparisonExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null; BinaryOperator op = null;
		
		try {      // for error handling
			expr=additiveExpression();
			{
			_loop145:
			do {
				if (((LA(1) >= COMPARE_EQ && LA(1) <= COMPARE_NE))) {
					op=comparisonOperator();
					rhs=additiveExpression();
					
					expr = new BinaryExpression( expr, op, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop145;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_45);
		}
		return expr;
	}
	
	public final BinaryOperator  comparisonOperator() throws RecognitionException, TokenStreamException {
		 BinaryOperator op = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case COMPARE_EQ:
			{
				match(COMPARE_EQ);
				op = BinaryOperator.COMPARE_EQ;
				break;
			}
			case COMPARE_GE:
			{
				match(COMPARE_GE);
				op = BinaryOperator.COMPARE_GE;
				break;
			}
			case COMPARE_GT:
			{
				match(COMPARE_GT);
				op = BinaryOperator.COMPARE_GT;
				break;
			}
			case COMPARE_LE:
			{
				match(COMPARE_LE);
				op = BinaryOperator.COMPARE_LE;
				break;
			}
			case COMPARE_LT:
			{
				match(COMPARE_LT);
				op = BinaryOperator.COMPARE_LT;
				break;
			}
			case COMPARE_NE:
			{
				match(COMPARE_NE);
				op = BinaryOperator.COMPARE_NE;
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
			recover(ex,_tokenSet_46);
		}
		return op;
	}
	
	public final Expression  multiplicativeExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null; BinaryOperator op = null;
		
		try {      // for error handling
			expr=powerExpression();
			{
			_loop151:
			do {
				if ((LA(1)==BY||LA(1)==DIVIDE) && (_tokenSet_46.member(LA(2)))) {
					op=multiplicativeOperator();
					rhs=powerExpression();
					
					expr = new BinaryExpression( expr, op, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop151;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_47);
		}
		return expr;
	}
	
	public final BinaryOperator  additiveOperator() throws RecognitionException, TokenStreamException {
		 BinaryOperator op = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ADD:
			{
				match(ADD);
				op = BinaryOperator.ADD;
				break;
			}
			case DASH:
			{
				match(DASH);
				op = BinaryOperator.SUBTRACT;
				break;
			}
			case CONCATENATION:
			{
				match(CONCATENATION);
				op = BinaryOperator.F_CONCAT;
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
			recover(ex,_tokenSet_46);
		}
		return op;
	}
	
	public final Expression  powerExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression rhs = null;
		
		try {      // for error handling
			expr=unaryExpression();
			{
			_loop154:
			do {
				if ((LA(1)==POWER)) {
					match(POWER);
					rhs=unaryExpression();
					
					expr = new BinaryExpression( expr, BinaryOperator.F_POWER, rhs );
					expr.setParens( false );
					
				}
				else {
					break _loop154;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_48);
		}
		return expr;
	}
	
	public final BinaryOperator  multiplicativeOperator() throws RecognitionException, TokenStreamException {
		 BinaryOperator op = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case BY:
			{
				match(BY);
				op = BinaryOperator.MULTIPLY;
				break;
			}
			case DIVIDE:
			{
				match(DIVIDE);
				op = BinaryOperator.DIVIDE;
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
			recover(ex,_tokenSet_46);
		}
		return op;
	}
	
	public final Expression  unaryExpression() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		UnaryOperator op = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ADD:
			case DASH:
			{
				op=prefixOperator();
				break;
			}
			case REAL:
			case DOT:
			case LOGICAL_FALSE:
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
			expr=expressionTerminal();
			
			
			if( op != null ) {
			// Workaround: it is difficult to distinguish literals starting with "-"
			// like "-8". The parser recognizes it as a unaryExpression. Trying to
			// insert parsing rules would introduce non-determinism and evil in
			// several forms yet to be known by men. Catch literals here and make a
			// little trick.
			if( expr instanceof IntegerLiteral ) {
			if( op.toString().equals( "+" ) ) {
			// Address bug that appears to have arisen in newly JDK versions, in
			// which a string like "+1" causes a number format exception
			expr = new IntegerLiteral( new Integer(
			expr.toString() ).intValue() );
			} else {
			expr = new IntegerLiteral( new Integer(
			op.toString() + expr.toString() ).intValue() );
			}
			} else {
			if( expr instanceof DoubleLiteral ) {
			double value = ((DoubleLiteral)expr).getValue();
			expr = new DoubleLiteral( new Double(
			op.toString() + value ).doubleValue() );
			} else {
			if( expr instanceof FloatLiteral ) {
			double value = ((FloatLiteral)expr).getValue();
			expr = new FloatLiteral( new Float(
			op.toString() + value ).floatValue() );
			} else {
			expr = new UnaryExpression( op, expr );
			expr.setParens( false );
			}
			}
			}
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
		return expr;
	}
	
	public final UnaryOperator  prefixOperator() throws RecognitionException, TokenStreamException {
		 UnaryOperator op = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case DASH:
			{
				match(DASH);
				op = UnaryOperator.MINUS;
				break;
			}
			case ADD:
			{
				match(ADD);
				op = UnaryOperator.PLUS;
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
			recover(ex,_tokenSet_49);
		}
		return op;
	}
	
	public final Expression  expressionTerminal() throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Identifier id = null; Expression start = null; Expression stop = null;
		IntegerLiteral step = null; boolean parens=false;
		List<Expression> list=null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case DOT:
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			case NUMBER:
			case STRINGLITERAL:
			{
				expr=literal();
				break;
			}
			case LPAREN:
			{
				match(LPAREN);
				list=parameterList();
				match(RPAREN);
				parens = true;
				break;
			}
			default:
				if ((LA(1)==ID) && (_tokenSet_38.member(LA(2)))) {
					expr=identifier();
				}
				else if ((LA(1)==REAL||LA(1)==ID) && (LA(2)==LPAREN)) {
					expr=arrayAccessOrFunctionCall();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
			if( list != null ) {
			if( list.size() == 1 ) {
			expr = (Expression)list.get(0);
			} else {
			expr = new CommaExpression( list );
			}
			}
			
			expr.setParens( parens );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
		return expr;
	}
	
	public final Literal  literal() throws RecognitionException, TokenStreamException {
		 Literal literal = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LOGICAL_FALSE:
			case LOGICAL_TRUE:
			{
				literal=booleanLiteral();
				break;
			}
			case STRINGLITERAL:
			{
				literal=stringLiteral();
				break;
			}
			default:
				if ((LA(1)==NUMBER) && (_tokenSet_38.member(LA(2)))) {
					literal=integerLiteral();
				}
				else if ((LA(1)==DOT||LA(1)==NUMBER) && (_tokenSet_50.member(LA(2)))) {
					literal=realLiteral();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
		return literal;
	}
	
	public final Identifier  builtInFunctionWithReservedName() throws RecognitionException, TokenStreamException {
		 Identifier fName = null ;
		
		
		try {      // for error handling
			match(REAL);
			fName = new Identifier( "REAL" );
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_51);
		}
		return fName;
	}
	
	public final Expression  finishSubstringExpression(
		 Identifier symbol 
	) throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		Expression ubound = null;
		
		try {      // for error handling
			expr=parameter();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				ubound=parameter();
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
			
			
			if( ubound == null ) {
			expr = new ArrayAccess( symbol, expr );
			} else {
			expr = new SubstringExpression( symbol, expr, ubound );
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return expr;
	}
	
	public final Expression  finishArrayAccess(
		 Identifier symbol 
	) throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		List<Expression> indices = null;
		
		try {      // for error handling
			indices=parameterList();
			match(RPAREN);
			expr = new ArrayAccess( symbol, indices );
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return expr;
	}
	
	public final Expression  finishFunctionCall(
		 Identifier symbol 
	) throws RecognitionException, TokenStreamException {
		 Expression expr = null ;
		
		List<Expression> parameters = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case REAL:
			case ADD:
			case BY:
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
				parameters=parameterList();
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
			
			
			if( parameters == null ) {
			parameters = new ArrayList<Expression>( 0 );
			}
			expr = new FunctionCall( symbol, parameters );
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return expr;
	}
	
	public final BooleanLiteral  booleanLiteral() throws RecognitionException, TokenStreamException {
		 BooleanLiteral literal = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LOGICAL_TRUE:
			{
				match(LOGICAL_TRUE);
				literal = new BooleanLiteral( true );
				break;
			}
			case LOGICAL_FALSE:
			{
				match(LOGICAL_FALSE);
				literal = new BooleanLiteral( false );
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
		return literal;
	}
	
	public final FloatLiteral  realLiteral() throws RecognitionException, TokenStreamException {
		 FloatLiteral literal = null ;
		
		Token  integerPart = null;
		Token  realPart = null;
		Token  separator = null;
		Token  expPartSignN = null;
		Token  expPartSignP = null;
		Token  expPart = null;
		String doubleStr = "";
		
		try {      // for error handling
			getLexer().toggleRealLiteral();
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				integerPart = LT(1);
				match(NUMBER);
				doubleStr += integerPart.getText();
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
			{
			match(DOT);
			doubleStr += ".";
			}
			{
			switch ( LA(1)) {
			case NUMBER:
			{
				realPart = LT(1);
				match(NUMBER);
				doubleStr += realPart.getText();
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
				{
				separator = LT(1);
				match(E);
				
				doubleStr += "E";
				if( separator.getText().equalsIgnoreCase( "e" ) ) {
				literal = new FloatLiteral( 0 );
				} else {
				literal = new DoubleLiteral( 0 );
				}
				}
				{
				switch ( LA(1)) {
				case DASH:
				{
					expPartSignN = LT(1);
					match(DASH);
					doubleStr += expPartSignN.getText();
					break;
				}
				case ADD:
				{
					expPartSignP = LT(1);
					match(ADD);
					doubleStr += expPartSignP.getText();
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
				{
				expPart = LT(1);
				match(NUMBER);
				doubleStr += expPart.getText();
				}
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
			if( literal != null ) {
			literal.setValue( new Double( doubleStr ) );
			} else {
			literal = new FloatLiteral( new Double( doubleStr ) );
			}
			
			getLexer().toggleRealLiteral();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_38);
		}
		return literal;
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
		"\"complex\"",
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
		long[] data = { 4742853959970L, 2147483648L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 74680350883312L, 2148663424L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 65536L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 2097152L, 131104L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { -3458830570957881870L, 2148663529L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 576535432654306800L, 2148663488L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 1380262096288L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 576742521082749216L, 16908320L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 73574966693968L, 2148663424L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 73574966628432L, 1179776L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 6053386463164239952L, 2200043574L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 507904L, 1048576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 1126174784749568L, 131104L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 2018879545335087104L, 19005494L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 0L, 2147483648L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 0L, 64L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 0L, 2147483712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 1200580257725936L, 2148663456L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 576460752303423488L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 577586652210266112L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { -3458831676342071214L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 1441292893124820992L, 19005494L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { -4612389430991257600L, 19005503L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 577586652210266112L, 2147483712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 578223177130512466L, 2148663488L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 376832L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 73574966956112L, 2148663456L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { 0L, 30081548320L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 1125899906842624L, 30081548384L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 1199474873667664L, 2148663424L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 720086565392683090L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 1688849860263936L, 2147483712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 4613374868287651840L, 2147483712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 0L, 8589934656L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { -8070517694769459118L, 32230211817L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { -4611826481037836288L, 2166489215L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { 4613374868287651840L, 2147483744L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	private static final long[] mk_tokenSet_38() {
		long[] data = { -8070517694769459118L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_38 = new BitSet(mk_tokenSet_38());
	private static final long[] mk_tokenSet_39() {
		long[] data = { -4612389430991257600L, 2166489215L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_39 = new BitSet(mk_tokenSet_39());
	private static final long[] mk_tokenSet_40() {
		long[] data = { 1125899906842624L, 2147483712L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_40 = new BitSet(mk_tokenSet_40());
	private static final long[] mk_tokenSet_41() {
		long[] data = { 1200580257725936L, 2148663424L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_41 = new BitSet(mk_tokenSet_41());
	private static final long[] mk_tokenSet_42() {
		long[] data = { 1125899906842624L, 64L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_42 = new BitSet(mk_tokenSet_42());
	private static final long[] mk_tokenSet_43() {
		long[] data = { 1125899906842624L, 8589934656L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_43 = new BitSet(mk_tokenSet_43());
	private static final long[] mk_tokenSet_44() {
		long[] data = { 578223177130512466L, 2148663496L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_44 = new BitSet(mk_tokenSet_44());
	private static final long[] mk_tokenSet_45() {
		long[] data = { 578223177130512466L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_45 = new BitSet(mk_tokenSet_45());
	private static final long[] mk_tokenSet_46() {
		long[] data = { 1441292893124820992L, 19005490L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_46 = new BitSet(mk_tokenSet_46());
	private static final long[] mk_tokenSet_47() {
		long[] data = { 1152572867108606034L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_47 = new BitSet(mk_tokenSet_47());
	private static final long[] mk_tokenSet_48() {
		long[] data = { 1152854342085316690L, 2148663497L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_48 = new BitSet(mk_tokenSet_48());
	private static final long[] mk_tokenSet_49() {
		long[] data = { 1152921779484753920L, 19005490L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_49 = new BitSet(mk_tokenSet_49());
	private static final long[] mk_tokenSet_50() {
		long[] data = { -4611753180948918190L, 2150760649L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_50 = new BitSet(mk_tokenSet_50());
	private static final long[] mk_tokenSet_51() {
		long[] data = { 0L, 32L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_51 = new BitSet(mk_tokenSet_51());
	
	}
