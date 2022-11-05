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



// $ANTLR 2.7.7 (20080806): "FLexer.g" -> "FLexer.java"$

    package cppc.compiler.cetus.grammars.fortran77;

import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

public class FLexer extends antlr.CharScanner implements FLexerTokenTypes, TokenStream
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
public FLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public FLexer(Reader in) {
	this(new CharBuffer(in));
}
public FLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public FLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = false;
	setCaseSensitive(false);
	literals = new Hashtable();
	literals.put(new ANTLRHashString("pause", this), new Integer(33));
	literals.put(new ANTLRHashString("external", this), new Integer(19));
	literals.put(new ANTLRHashString("read", this), new Integer(37));
	literals.put(new ANTLRHashString("to", this), new Integer(44));
	literals.put(new ANTLRHashString("character", this), new Integer(5));
	literals.put(new ANTLRHashString("dimension", this), new Integer(11));
	literals.put(new ANTLRHashString("call", this), new Integer(4));
	literals.put(new ANTLRHashString("open", this), new Integer(31));
	literals.put(new ANTLRHashString("complex", this), new Integer(8));
	literals.put(new ANTLRHashString("parameter", this), new Integer(32));
	literals.put(new ANTLRHashString("print", this), new Integer(35));
	literals.put(new ANTLRHashString("integer", this), new Integer(27));
	literals.put(new ANTLRHashString("enddo", this), new Integer(17));
	literals.put(new ANTLRHashString("common", this), new Integer(7));
	literals.put(new ANTLRHashString("format", this), new Integer(20));
	literals.put(new ANTLRHashString("intrinsic", this), new Integer(28));
	literals.put(new ANTLRHashString("end", this), new Integer(16));
	literals.put(new ANTLRHashString("implicit", this), new Integer(25));
	literals.put(new ANTLRHashString("go", this), new Integer(22));
	literals.put(new ANTLRHashString("data", this), new Integer(10));
	literals.put(new ANTLRHashString("none", this), new Integer(29));
	literals.put(new ANTLRHashString("continue", this), new Integer(9));
	literals.put(new ANTLRHashString("precision", this), new Integer(34));
	literals.put(new ANTLRHashString("endif", this), new Integer(18));
	literals.put(new ANTLRHashString("do", this), new Integer(12));
	literals.put(new ANTLRHashString("write", this), new Integer(46));
	literals.put(new ANTLRHashString("function", this), new Integer(21));
	literals.put(new ANTLRHashString("logical", this), new Integer(30));
	literals.put(new ANTLRHashString("elseif", this), new Integer(15));
	literals.put(new ANTLRHashString("while", this), new Integer(45));
	literals.put(new ANTLRHashString("close", this), new Integer(6));
	literals.put(new ANTLRHashString("real", this), new Integer(38));
	literals.put(new ANTLRHashString("include", this), new Integer(26));
	literals.put(new ANTLRHashString("return", this), new Integer(39));
	literals.put(new ANTLRHashString("subroutine", this), new Integer(42));
	literals.put(new ANTLRHashString("stop", this), new Integer(41));
	literals.put(new ANTLRHashString("if", this), new Integer(24));
	literals.put(new ANTLRHashString("double", this), new Integer(13));
	literals.put(new ANTLRHashString("program", this), new Integer(36));
	literals.put(new ANTLRHashString("save", this), new Integer(40));
	literals.put(new ANTLRHashString("else", this), new Integer(14));
	literals.put(new ANTLRHashString("then", this), new Integer(43));
	literals.put(new ANTLRHashString("goto", this), new Integer(23));
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				if (((LA(1)=='.') && (LA(2)=='e') && (LA(3)=='q'))&&( normalLine() )) {
					mCOMPARE_EQ(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='g') && (LA(3)=='e'))&&( normalLine() )) {
					mCOMPARE_GE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='g') && (LA(3)=='t'))&&( normalLine() )) {
					mCOMPARE_GT(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='l') && (LA(3)=='e'))&&( normalLine() )) {
					mCOMPARE_LE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='l') && (LA(3)=='t'))&&( normalLine() )) {
					mCOMPARE_LT(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='n') && (LA(3)=='e'))&&( normalLine() )) {
					mCOMPARE_NE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='a') && (LA(3)=='n'))&&( normalLine() )) {
					mLOGICAL_AND(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='f') && (LA(3)=='a'))&&( normalLine() )) {
					mLOGICAL_FALSE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='n') && (LA(3)=='o'))&&( normalLine() )) {
					mLOGICAL_NEGATION(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='o') && (LA(3)=='r'))&&( normalLine() )) {
					mLOGICAL_OR(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (LA(2)=='t') && (LA(3)=='r'))&&( normalLine() )) {
					mLOGICAL_TRUE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='!') && (_tokenSet_0.member(LA(2))) && (_tokenSet_1.member(LA(3))))&&( normalLine() )) {
					mPRAGMA(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='/') && (LA(2)=='/') && (true))&&( normalLine() && !readingFormat )) {
					mCONCATENATION(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='*') && (LA(2)=='*') && (true))&&( normalLine() )) {
					mPOWER(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='"'||LA(1)=='\'') && (_tokenSet_2.member(LA(2))) && (true))&&( normalLine() )) {
					mSTRINGLITERAL(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='+') && (true) && (true))&&( normalLine() )) {
					mADD(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='*') && (true) && (true))&&( normalLine() )) {
					mBY(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)==':') && (true) && (true))&&( normalLine() )) {
					mCOLON(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)==',') && (true) && (true))&&( normalLine() )) {
					mCOMMA(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='-') && (true) && (true))&&( normalLine() )) {
					mDASH(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='/') && (true) && (true))&&( normalLine() && !readingFormat )) {
					mDIVIDE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='.') && (true) && (true))&&( normalLine() )) {
					mDOT(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='d'||LA(1)=='e') && (true) && (true))&&( readingRealLiteral )) {
					mE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='=') && (true) && (true))&&( normalLine() )) {
					mEQUALS(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='(') && (true) && (true))&&( normalLine() )) {
					mLPAREN(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)==')') && (true) && (true))&&( normalLine() )) {
					mRPAREN(true);
					theRetToken=_returnToken;
				}
				else if (((_tokenSet_0.member(LA(1))) && (true) && (true))&&( normalLine() && !readingFormat )) {
					mID(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='"'||LA(1)=='\'') && (true) && (true))&&( normalLine() )) {
					mQUOTE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='\t'||LA(1)=='\r'||LA(1)==' ') && (true) && (true))&&( !readingPragma && !readingFormat )) {
					mWhitespace(true);
					theRetToken=_returnToken;
				}
				else if ((((LA(1) >= '0' && LA(1) <= '9')) && (true) && (true))&&( labelColumn() )) {
					mLABEL(true);
					theRetToken=_returnToken;
				}
				else if ((((LA(1) >= '0' && LA(1) <= '9')) && (true) && (true))&&( ( normalLine() && !readingFormat ) || labelColumn() )) {
					mNUMBER(true);
					theRetToken=_returnToken;
				}
				else if (((_tokenSet_2.member(LA(1))) && (true) && (true))&&( readingPragma )) {
					mPRAGMAVALUE(true);
					theRetToken=_returnToken;
				}
				else if (((_tokenSet_3.member(LA(1))) && (true) && (true))&&( getColumn()==1 )) {
					mCOMMENT(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='!') && (true) && (true))&&( normalLine() )) {
					mINLINECOMMENT(true);
					theRetToken=_returnToken;
				}
				else if (((_tokenSet_2.member(LA(1))) && (true) && (true))&&( farColumn() )) {
					mFAR_COMMENT(true);
					theRetToken=_returnToken;
				}
				else if ((LA(1)=='\n')) {
					mNewline(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='/') && (true) && (true))&&( readingFormat )) {
					mFORMAT_NEWLINE(true);
					theRetToken=_returnToken;
				}
				else if (((LA(1)=='\t'||LA(1)=='\r'||LA(1)==' ') && (true) && (true))&&( readingFormat )) {
					mFORMAT_WHITESPACE(true);
					theRetToken=_returnToken;
				}
				else if (((_tokenSet_4.member(LA(1))) && (true) && (true))&&( readingFormat )) {
					mFORMAT_TEXT(true);
					theRetToken=_returnToken;
				}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_ttype = testLiteralsTable(_ttype);
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mADD(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ADD;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('+');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mBY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BY;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('*');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COLON;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(':');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMMA;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(',');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_EQ(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_EQ;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".eq.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_GE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_GE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".ge.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_GT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_GT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".gt.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_LE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_LE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".le.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_LT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_LT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".lt.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMPARE_NE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMPARE_NE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".ne.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCONCATENATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CONCATENATION;
		int _saveIndex;
		
		if (!( normalLine() && !readingFormat ))
		  throw new SemanticException(" normalLine() && !readingFormat ");
		match("//");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDASH(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DASH;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('-');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDIVIDE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DIVIDE;
		int _saveIndex;
		
		if (!( normalLine() && !readingFormat ))
		  throw new SemanticException(" normalLine() && !readingFormat ");
		match('/');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('.');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = E;
		int _saveIndex;
		
		if (!( readingRealLiteral ))
		  throw new SemanticException(" readingRealLiteral ");
		{
		switch ( LA(1)) {
		case 'e':
		{
			match("e");
			break;
		}
		case 'd':
		{
			match("d");
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQUALS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQUALS;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPOWER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = POWER;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match("**");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOGICAL_AND(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOGICAL_AND;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".and.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOGICAL_FALSE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOGICAL_FALSE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".false.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOGICAL_NEGATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOGICAL_NEGATION;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".not.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOGICAL_OR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOGICAL_OR;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".or.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOGICAL_TRUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOGICAL_TRUE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(".true.");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLPAREN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LPAREN;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('(');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRPAREN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RPAREN;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPRAGMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PRAGMA;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		_saveIndex=text.length();
		match('!');
		text.setLength(_saveIndex);
		mID(false);
		_saveIndex=text.length();
		match('$');
		text.setLength(_saveIndex);
		togglePragma();
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ID;
		int _saveIndex;
		
		if (!( normalLine() && !readingFormat ))
		  throw new SemanticException(" normalLine() && !readingFormat ");
		mID_CHARACTER(false);
		{
		_loop39:
		do {
			switch ( LA(1)) {
			case '_':  case 'a':  case 'b':  case 'c':
			case 'd':  case 'e':  case 'f':  case 'g':
			case 'h':  case 'i':  case 'j':  case 'k':
			case 'l':  case 'm':  case 'n':  case 'o':
			case 'p':  case 'q':  case 'r':  case 's':
			case 't':  case 'u':  case 'v':  case 'w':
			case 'x':  case 'y':  case 'z':
			{
				mID_CHARACTER(false);
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				mNUMBER_TOKEN(false);
				break;
			}
			default:
			{
				break _loop39;
			}
			}
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mQUOTE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QUOTE;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		{
		switch ( LA(1)) {
		case '\'':
		{
			match('\'');
			break;
		}
		case '"':
		{
			match('"');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mAMP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AMP;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('&');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mBACKSLASH(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BACKSLASH;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('\\');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCIRCUMFLEX(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CIRCUMFLEX;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('^');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('<');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mGT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = GT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('>');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mQUERY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QUERY;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('?');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSEMICOLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SEMICOLON;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match(';');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSHARP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SHARP;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('#');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mID_CHARACTER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ID_CHARACTER;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		{
		switch ( LA(1)) {
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			matchRange('a','z');
			break;
		}
		case '_':
		{
			match('_');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNUMBER_TOKEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER_TOKEN;
		int _saveIndex;
		
		if (!( normalLine() || labelColumn() ))
		  throw new SemanticException(" normalLine() || labelColumn() ");
		matchRange('0','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWhitespace(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Whitespace;
		int _saveIndex;
		
		if (!( !readingPragma && !readingFormat ))
		  throw new SemanticException(" !readingPragma && !readingFormat ");
		{
		int _cnt44=0;
		_loop44:
		do {
			switch ( LA(1)) {
			case ' ':
			{
				match(' ');
				break;
			}
			case '\t':
			{
				match('\t');
				break;
			}
			case '\r':
			{
				match('\r');
				break;
			}
			default:
			{
				if ( _cnt44>=1 ) { break _loop44; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt44++;
		} while (true);
		}
		_ttype =  Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLABEL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LABEL;
		int _saveIndex;
		
		if (!( labelColumn() ))
		  throw new SemanticException(" labelColumn() ");
		mNUMBER(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNUMBER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER;
		int _saveIndex;
		
		if (!( ( normalLine() && !readingFormat ) || labelColumn() ))
		  throw new SemanticException(" ( normalLine() && !readingFormat ) || labelColumn() ");
		{
		int _cnt48=0;
		_loop48:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				mNUMBER_TOKEN(false);
			}
			else {
				if ( _cnt48>=1 ) { break _loop48; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt48++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSTRING_TOKEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRING_TOKEN;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		{
		match(_tokenSet_5);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSTRINGLITERAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRINGLITERAL;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		_saveIndex=text.length();
		mQUOTE(false);
		text.setLength(_saveIndex);
		{
		_loop54:
		do {
			// nongreedy exit test
			if ((LA(1)=='"'||LA(1)=='\'') && (true)) break _loop54;
			if ((_tokenSet_5.member(LA(1))) && (_tokenSet_2.member(LA(2)))) {
				mSTRING_TOKEN(false);
			}
			else {
				break _loop54;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		mQUOTE(false);
		text.setLength(_saveIndex);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPRAGMAVALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PRAGMAVALUE;
		int _saveIndex;
		
		if (!( readingPragma ))
		  throw new SemanticException(" readingPragma ");
		{
		int _cnt58=0;
		_loop58:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				{
				match(_tokenSet_2);
				}
			}
			else {
				if ( _cnt58>=1 ) { break _loop58; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt58++;
		} while (true);
		}
		togglePragma();
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMMENT;
		int _saveIndex;
		
		if (!( getColumn()==1 ))
		  throw new SemanticException(" getColumn()==1 ");
		{
		match(_tokenSet_3);
		}
		{
		_loop63:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				{
				match(_tokenSet_2);
				}
			}
			else {
				break _loop63;
			}
			
		} while (true);
		}
		_ttype =  Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mINLINECOMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INLINECOMMENT;
		int _saveIndex;
		
		if (!( normalLine() ))
		  throw new SemanticException(" normalLine() ");
		match('!');
		{
		_loop67:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				{
				match(_tokenSet_2);
				}
			}
			else {
				break _loop67;
			}
			
		} while (true);
		}
		_ttype =  Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFAR_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FAR_COMMENT;
		int _saveIndex;
		
		if (!( farColumn() ))
		  throw new SemanticException(" farColumn() ");
		{
		match(_tokenSet_2);
		}
		_ttype =  Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNEWLINE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NEWLINE;
		int _saveIndex;
		
		match('\n');
		newline();
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCONTINUELINE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CONTINUELINE;
		int _saveIndex;
		
		{
		{
		match(_tokenSet_6);
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNewline(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Newline;
		int _saveIndex;
		
		boolean oldFormat=readingFormat; readingFormat=false;
		mNEWLINE(false);
		{
		if ((LA(1)=='\t'||LA(1)=='\r'||LA(1)==' ')) {
			mWhitespace(false);
		}
		else {
		}
		
		}
		{
		if (((_tokenSet_6.member(LA(1))))&&( getColumn() == 6 )) {
			mCONTINUELINE(false);
			_ttype =  Token.SKIP;
		}
		else {
		}
		
		}
		readingFormat=oldFormat;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFORMAT_NEWLINE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FORMAT_NEWLINE;
		int _saveIndex;
		
		if (!( readingFormat ))
		  throw new SemanticException(" readingFormat ");
		match("/");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFORMAT_WHITESPACE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FORMAT_WHITESPACE;
		int _saveIndex;
		
		if (!( readingFormat ))
		  throw new SemanticException(" readingFormat ");
		{
		int _cnt80=0;
		_loop80:
		do {
			switch ( LA(1)) {
			case ' ':
			{
				match(' ');
				break;
			}
			case '\t':
			{
				match('\t');
				break;
			}
			case '\r':
			{
				match('\r');
				break;
			}
			default:
			{
				if ( _cnt80>=1 ) { break _loop80; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt80++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFORMAT_TEXT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FORMAT_TEXT;
		int _saveIndex;
		
		if (!( readingFormat ))
		  throw new SemanticException(" readingFormat ");
		{
		int _cnt84=0;
		_loop84:
		do {
			if ((_tokenSet_4.member(LA(1)))) {
				{
				match(_tokenSet_4);
				}
			}
			else {
				if ( _cnt84>=1 ) { break _loop84; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt84++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[1025];
		data[1]=576460745860972544L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[1025];
		data[0]=287948969894477824L;
		data[1]=576460745860972544L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[2048];
		data[0]=-1025L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[2048];
		data[0]=-287948905469978113L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[2048];
		data[0]=-162177965097985L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = new long[2048];
		data[0]=-549755814913L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = new long[2048];
		data[0]=-4294977025L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
