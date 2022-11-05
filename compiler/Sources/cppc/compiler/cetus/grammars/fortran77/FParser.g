header {
    package cppc.compiler.cetus.grammars.fortran77;
}

{
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
}

class FParser extends Parser;

options {
  k=2;
  importVocab=FLexer;
}

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


}

// Program
// Translation Unit
translationUnit [TranslationUnit init_tunit] returns [TranslationUnit tunit] {

    if( init_tunit == null ) {
        tunit = new TranslationUnit( getLexer().originalSource );
    } else {
        tunit = init_tunit;
    }

    symtab = tunit;
}

: ( mainProgram [ tunit ]
  | subroutineSubprogram [ tunit ]
  | functionSubprogram [ tunit ]
  | Newline )*
;

mainProgram [ TranslationUnit tunit ] { Identifier name=null; CompoundStatement body = null; }

: name=programStatement body=subprogramBody endProgramStatement {

        Declarator mainName = new ProcedureDeclarator( new Identifier( "main" ), new ArrayList() );
        Procedure main = new Procedure( mainName, body );
        tunit.addDeclaration( main );
}
;

subroutineSubprogram [ TranslationUnit tunit ] { Identifier name = null; CompoundStatement body = null; List<Identifier> parameters = new ArrayList<Identifier>(); }

: name=subroutineStatement[parameters] body=subprogramBody endSubroutineStatement {

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
;

functionSubprogram [ TranslationUnit tunit ] { Identifier name = null;
                                               CompoundStatement body = null;
                                               List<Identifier> parameters = new
                                                 ArrayList<Identifier>();
                                               ArrayList<Specifier> spec = null; }

: (spec=specifier)? name=functionStatement[parameters] body=subprogramBody
    endFunctionStatement {

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
;

programStatement returns [ Identifier id = null ]
: PROGRAM id=identifier Newline {
    ImplicitDeclarationManager.restartState( new Identifier( "main" ) );
  }
;

endProgramStatement { Identifier id = null; }
: END (PROGRAM (id=identifier)?)? Newline
;

subroutineStatement [ List<Identifier> parameters ]
  returns [ Identifier subroutineName=null ]

  { List<Identifier> innerParams = null; }

: SUBROUTINE subroutineName=identifier {
    ImplicitDeclarationManager.restartState( subroutineName );
    }

    ( LPAREN (innerParams=identifierList {
        Iterator<Identifier> iter = innerParams.iterator();
        while( iter.hasNext() ) {
          parameters.add( iter.next() );
        }
      } )? RPAREN  )?
;

endSubroutineStatement {Identifier id = null;}
: END (SUBROUTINE (id=identifier)?)? Newline;

functionStatement [ List<Identifier> parameters ]
    returns [ Identifier functionName=null ]

    { List<Identifier> innerParams = null; }

: FUNCTION functionName=identifier {
    ImplicitDeclarationManager.restartState( functionName );
  }

  ( LPAREN (innerParams=identifierList {
    Iterator<Identifier> iter = innerParams.iterator();
    while( iter.hasNext() ) {
      parameters.add( iter.next() );
    }
  } )? RPAREN )?
;

endFunctionStatement {Identifier id=null;}
: END (FUNCTION (id=identifier)?)? Newline
;

subprogramBody
  returns [ CompoundStatement body = new CompoundStatement() ]

  { Statement statement=null; DeclarationStatement decl=null; curSymtab=body;
    body.setLineNumber( curLexer.getLine() ); }

  // The parser can be unable to distinguish if he must follow the
  // declarationPart or the executablePart due to Newline's (as a Newline can
  // be in both parts. Which one it selects does not matter, so we add
  // warnWhenFollowAmbig=false in the declarationPart rule.
: declarationPart[ body ] executablePart[ body ]
;

declarationPart [ CompoundStatement body ]

  { Statement statement = null; curSymtab = body; }

: ( options{warnWhenFollowAmbig=false;}:
      statement=declarationStatement[ body ] {
        if( statement != null ) {
          body.addStatement( statement );
        } }
  | Newline )*
;

executablePart [ CompoundStatement body ] { Statement stmt = null; }

: ( stmt=executableStatement Newline { body.addStatement( stmt ); }
  | Newline )*
;

declarationStatement [ CompoundStatement body ]
  returns [ Statement statement = null ]

  { ArrayList<Specifier> spec = null;
    List<VariableDeclarator> declarators=null; }

: spec=specifier declarators=declaratorList {

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
  }
| DIMENSION declarators=declaratorList {

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
  }
| INCLUDE file:STRINGLITERAL {

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
  }
| { Identifier blockName = null; }
  common:COMMON DIVIDE blockName=identifier DIVIDE declarators=declaratorList {

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
  }
| { List<ImplicitRegister> registers=null; }
  IMPLICIT (
    NONE {
      ImplicitDeclarationManager.setImplicitNone();
      statement = new DeclarationStatement( ImplicitRegister.IMPLICIT_NONE ); }
    | spec=specifier LPAREN registers=implicitList[spec.get( 0 )] RPAREN {
      for( ImplicitRegister reg: registers ) {
        ImplicitDeclarationManager.addRegister( reg );
        body.addStatement( new DeclarationStatement( reg ) );
      } } )
| { List<Expression> parameters = null; }
  PARAMETER LPAREN parameters=parameterList RPAREN {

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
  }
| DATA statement=dataDeclaration { body.addStatement( statement ); }
  ( ( COMMA )?
    statement=dataDeclaration {
      body.addStatement( statement ); }
  )* { statement=null; }
| { List<Identifier> names = null; }
  INTRINSIC names=identifierList {
    statement = new DeclarationStatement( new IntrinsicDeclaration( names ) );
  }
| { List<Identifier> names = null; }
  EXTERNAL names=identifierList {
    statement = new DeclarationStatement( new ExternalDeclaration( names ) );
  }
| { List<Identifier> names = null; }
  SAVE names=identifierList {

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
| { List<Identifier> names = null; }
  SAVE DIVIDE names=identifierList DIVIDE {

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
;

dataDeclaration returns [ Statement s = null; ]

  { List<Expression> expressions = null; List<Expression> initializers=null;}

: expressions=dataDeclarationExpressionList DIVIDE
    initializers=dataDeclarationInitializerList DIVIDE {

    s = new DeclarationStatement( new DataDeclaration( expressions,
      initializers ) );
  }
;

dataDeclarationExpressionList
  returns [ List<Expression> expressions = new ArrayList<Expression>() ]

  { Expression expr = null; }

: expr=dataDeclarationExpression { expressions.add( expr ); }
  ( COMMA expr=dataDeclarationExpression { expressions.add( expr ); } )*
;

dataDeclarationExpression returns [ Expression expr = null ]

: expr = identifier
| expr = arrayAccessOrFunctionCall
| expr = impliedDoLoop
;

dataDeclarationInitializerList
  returns [ List<Expression> initializers = new ArrayList<Expression>() ]

  { Expression expr=null; }

: expr=dataDeclarationInitializer { initializers.add( expr ); }
  ( COMMA expr=dataDeclarationInitializer { initializers.add( expr ); } )*
;

dataDeclarationInitializer returns [ Expression expr = null ]

: expr = expression
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
| REAL    { spec.add( Specifier.FLOAT ); }
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
    spec.add( new StringSpecifier( new IntegerLiteral( new Integer(
      num3.getText() ).intValue() ) ) );
  }
| COMPLEX {
    spec.add( ComplexSpecifier.instance() );
}
| COMPLEX BY num4:NUMBER {
    if( num4.getText().equals( "16" ) ) {
      spec.add( DoubleComplexSpecifier.instance() );
    } else {
      spec.add( ComplexSpecifier.instance() );
    }
}
| DOUBLE COMPLEX {
    spec.add( DoubleComplexSpecifier.instance() );
}
;

executableStatement returns [ Statement statement = null ]

: statement=pragmaStatement
| statement=doLoopStatement
| statement=expressionStatement
| statement=ifStatement
| statement=returnStatement
| statement=labeledStatement
| statement=whileStatement
| statement=gotoStatement
;

labeledStatement returns [ Statement statement = null ]

: label:LABEL (
    statement=expressionStatement
    | statement=ifStatement
    | statement=formatStatement
    | statement=returnStatement
    | statement=doLoopStatement
    | statement=whileStatement ) {

      CompoundStatement noBody = new CompoundStatement();
      noBody.addStatement( new Label( new Identifier( label.getText() ) ) );
      noBody.addStatement( statement );
      statement = noBody;
      statement.setLineNumber( label.getLine() );
    }
;

doLoopStatement returns [ FortranDoLoop stmt = null ]

  { Identifier loopId = null; Expression start = null; Expression end = null;
    Expression step = null;
    CompoundStatement loopBody = new CompoundStatement(); }


: doTok:DO
  (label:NUMBER (COMMA)? )?
  loopId=identifier EQUALS start=doLimit COMMA end=doLimit
  ( COMMA step=expression )?
  executablePart[ loopBody ] endDoLoopStatement[ label ] {

    Identifier labelObj = null;

    if( label != null ) {
      labelObj = new Identifier( label.getText() );
    }

    stmt = new FortranDoLoop( labelObj, loopId, start, end, step, loopBody );
    stmt.setLineNumber( doTok.getLine() );
    loopBody.setLineNumber( doTok.getLine()+1 );
  }
;

endDoLoopStatement [ Token t ]

: END (DO)?
| ENDDO
| l:LABEL CONTINUE {

    if( t!= null ) {
      if( !l.getText().equals( t.getText() ) ) {
        throw new InternalError( "line " + curLexer.getLine() + ": CONTINUE " +
          "label does not match DO LOOP label" );
      }
    }
  }
;

whileStatement returns [ WhileLoop stmt = null ]

  { Expression condition = null;
    CompoundStatement loopBody = new CompoundStatement(); }

: doTok:DO whileTok:WHILE LPAREN condition = conditionalExpression RPAREN
  executablePart[ loopBody ]
  ( END DO | ENDDO ) {

    stmt = new WhileLoop( condition, loopBody );
  }
;

gotoStatement returns [ Statement stmt = null ]

: (GOTOTOKEN|GO TO) label:NUMBER {
  stmt = new GotoStatement( new Identifier( label.getText() ) );
}
| { List<IntegerLiteral> parameters = null; Expression expr = null; }

  (GOTOTOKEN|GO TO) LPAREN parameters = integerLiteralList RPAREN
    (COMMA) ? expr = expression {

    List<Label> labels = new ArrayList<Label>( parameters.size() );
    for( IntegerLiteral l: parameters ) {
      labels.add( new Label( new Identifier( l.toString() ) ) );
    }

    stmt = new ComputedGotoStatement( labels, expr );
  }
;

doLimit returns [ Expression expr = null ]

: expr = additiveExpression
;

expressionStatement returns [ ExpressionStatement statement = null ]

  { Expression expr = null; Expression rhs = null;
    List<Expression> varargs = null; }

: expr = assignmentExpression {
    statement = new ExpressionStatement( expr );
    statement.setLineNumber( curLexer.getLine() );
  }
| expr = procedureCallStatement {

    statement = new ExpressionStatement( expr );
    statement.setLineNumber( curLexer.getLine()-1 );
  }
| expr = predefinedFunctionStatement {

    statement = new ExpressionStatement( expr );
    statement.setLineNumber( curLexer.getLine()-1 );
  }
;

assignmentExpression returns [ Expression expr = null ]

  { Expression rhs = null; }

: expr = assignmentTerminal
    (EQUALS rhs=expression {
      expr = new AssignmentExpression( expr, AssignmentOperator.NORMAL, rhs );
    } )?
;

finishAssignmentStatement [ Expression expr ]
  returns [ Expression retExpr = null ]

  { Expression rhs=null; }
: EQUALS rhs=expression {
    retExpr = new AssignmentExpression( expr, AssignmentOperator.NORMAL, rhs );
  }
;

finishFunctionCallStatement [ FunctionCall call ]

  { List<Expression> varargs = null; }
: ( varargs=parameterList Newline {

    for( Expression expr: varargs ) {
      call.addArgument( expr );
    }
  }
  | Newline )
;

ifStatement returns [ IfStatement statement = null ]

  { Expression condition=null; Statement trueClause=null;
    Statement falseClause=null; }

: ifTok:IF LPAREN condition=conditionalExpression RPAREN
  ( trueClause = executableStatement {
      statement = new IfStatement( condition, trueClause ); }
  | { trueClause = new CompoundStatement(); }
    THEN executablePart[ (CompoundStatement)trueClause ] {

      statement = new IfStatement( condition, trueClause );
    }
    ( { falseClause = new CompoundStatement(); Expression innerCondition=null; }

      ( ELSEIF LPAREN innerCondition=conditionalExpression RPAREN THEN
        executablePart[ (CompoundStatement)falseClause ] {

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
        } )*
      ( { falseClause = new CompoundStatement(); }

          ELSE executablePart[ (CompoundStatement)falseClause ] {

            IfStatement innerElse = statement;

            while( innerElse.getElseStatement() != null ) {
              innerElse = (IfStatement)
                innerElse.getElseStatement().getChildren().get( 0 );
            }

            innerElse.setElseStatement( falseClause );
          } )? )?
    ( END (IF)? | ENDIF ) ) {

      statement.setLineNumber( ifTok.getLine() );
    }
;

formatStatement returns [ FormatStatement statement = null ]

{ List<Expression> parts = null; }

: { curLexer.toggleFormat(); }

  FORMAT (FORMAT_WHITESPACE)? LPAREN (FORMAT_WHITESPACE)?
  parts=formatParameterList (FORMAT_WHITESPACE)? RPAREN {

    statement = new FormatStatement( parts );
    curLexer.toggleFormat();
  }
;

returnStatement returns [ ReturnStatement statement = null ]

: rTok:RETURN {
    statement = new ReturnStatement();
    statement.setLineNumber( rTok.getLine() );
  }
;

pragmaStatement returns [ DeclarationStatement statement = null ]

: family:PRAGMA value:PRAGMAVALUE {

    Annotation annote = new Annotation( family.getText().trim() + " " +
      value.getText().trim() );
    annote.setPrintMethod( Annotation.print_as_pragma_method );
    statement = new DeclarationStatement( annote );
    statement.setLineNumber( curLexer.getLine() );
  }
;

procedureCallStatement returns [ Expression expr = null ]

  { Identifier name = null;
    List<Expression> parameters = new ArrayList<Expression>(); }

: CALL name=identifier (LPAREN (parameters = parameterList)? RPAREN)? {
    expr = new FunctionCall( name, parameters );
  }
;

predefinedFunctionStatement returns [ Expression expr = null ]
: { StringLiteral lit = null;
  ArrayList<Expression> param = new ArrayList<Expression>( 1 ); }

  stop:STOP (lit=stringLiteral { param.add( lit ); } )? {
    expr = new IOCall( new Identifier( stop.getText().toUpperCase() ), param );
  }
| { StringLiteral lit = null;
  ArrayList<Expression> param = new ArrayList<Expression>( 1 ); }

  pause:PAUSE ( lit=stringLiteral { param.add( lit ); } )? {
    expr = new IOCall( new Identifier( pause.getText() ), param );
  }
| { List<Expression> parameters = null; List<Expression> varargs = null; }

  (write:WRITE|read:READ) LPAREN parameters=parameterList RPAREN (COMMA)?
    (varargs = ioParameterList)? {

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
  }
| { Expression format=null; List<Expression> varargs = null; }

  iop:PRINT format=parameter COMMA varargs=ioParameterList {

    List<Expression> allParams = new ArrayList<Expression>(
      varargs.size() + 1 );
    allParams.add( format );
    allParams.addAll( varargs );
    expr = new IOCall( new Identifier( iop.getText().toUpperCase() ),
      allParams );
  }
| { List<Expression> parameters = null; }

  open:OPEN LPAREN parameters=parameterList RPAREN {
    expr = new IOCall( new Identifier( open.getText().toUpperCase() ),
      parameters, new ArrayList<Expression>( 0 ) );
  }
| { List<Expression> parameters = null; }
  close:CLOSE LPAREN parameters = parameterList RPAREN {
    expr = new IOCall( new Identifier( close.getText().toUpperCase() ),
      parameters, new ArrayList<Expression>( 0 ) );
  }
;

integerLiteralList
  returns [ List<IntegerLiteral> list = new ArrayList<IntegerLiteral>() ]

  { IntegerLiteral integer = null; }

: integer = integerLiteral { list.add( integer ); }
  ( COMMA integer = integerLiteral { list.add( integer ); } )*
;

ioParameterList
  returns [ List<Expression> varargs=new ArrayList<Expression>() ]

  { Expression expr = null; }

: expr = ioParameter { varargs.add( expr ); }
  ( COMMA expr=ioParameter { varargs.add( expr ); } )*
;

ioParameter returns [ Expression expr = null ]

: expr=impliedDoLoop // Implied do first to process LPAREN
| expr=expression // If changed, LPAREN would go through expressionTerminal
;

impliedDoLoop returns [ Expression expr=null ]

  { List<Expression> objects = new ArrayList<Expression>();
    Identifier doVar = null; Expression start = null; Expression stop = null;
    Expression step = null; }

: LPAREN expr=ioParameter
  ( { objects.add( expr ); } COMMA
    (expr=ioParameter COMMA { objects.add( expr ); } )*
    doVar=identifier
    EQUALS start=expression COMMA
    stop=expression (COMMA step=expression )? {

      if( step == null ) {
        expr = new ImpliedDoLoop( objects, doVar, start, stop );
      } else {
        expr = new ImpliedDoLoop( objects, doVar, start, stop, step );
      }
    } )? RPAREN
;


declaratorList
  returns [ List<VariableDeclarator> declarators =
              new ArrayList<VariableDeclarator>() ]

  { VariableDeclarator decl = null; }

: decl = declarator { declarators.add( decl ); }
  ( COMMA decl=declarator { declarators.add( decl ); } )*
;

declarator returns [ VariableDeclarator decl = null ]

  { Identifier name = null; List<FortranArraySpecifier> dimensions = null; }

: name=identifier ( LPAREN dimensions = dimensionList RPAREN )? {
    if( dimensions == null ) {
      decl = new VariableDeclarator( name );
    } else {
      decl = new VariableDeclarator( name, dimensions );
    }
  }
;

dimensionList
  returns [ List<FortranArraySpecifier> dimensions =
    new ArrayList<FortranArraySpecifier>() ]

  { FortranArraySpecifier spec = null; }

: spec = dimension { dimensions.add( spec ); }
  ( COMMA spec=dimension { dimensions.add( spec ); } )*
;

dimension returns [ FortranArraySpecifier specifier = null ]

  { Expression lBound = null; Expression uBound = null; }

: lBound = parameter ( COLON uBound = parameter )? {

    if( uBound == null ) {
      specifier = new FortranArraySpecifier( lBound );
    } else {
      specifier = new FortranArraySpecifier( lBound, uBound );
    }
  }
;

parameterList
  returns [ List<Expression> parameters = new ArrayList<Expression>() ]

  { Expression expr = null; }

: expr = parameter {parameters.add( expr );}
    ( COMMA expr=parameter {parameters.add( expr );} )*
;

parameter returns [ Expression expr = null ] { Expression ubound=null; }

: expr=expression { expr.setParens( false ); }
| expr=assignmentExpression { expr.setParens( false ); }
| BY { expr = new Identifier( "*" ); } // Fortran has this annoying things
;

formatParameterList
  returns [ List<Expression> parameters = new ArrayList<Expression>() ]

  { List<Expression> param = null; }

: param=formatParameter { parameters.addAll( param ); }
    ( COMMA param=formatParameter { parameters.addAll( param ); } )*
;

formatParameter
  returns [ List<Expression> params = new ArrayList<Expression>(); ]

  { StringLiteral strLit = null; String str=""; String aux=null;
    List<Expression> innerParams=null; }

: ( FORMAT_NEWLINE { str += "/"; }
  | strLit=stringLiteral { str += "'" + strLit.getValue() + "'"; }
  | ( text:FORMAT_TEXT { str += text.getText(); } )+
  | white:FORMAT_WHITESPACE { str += white.getText(); }
  | LPAREN innerParams=formatParameterList RPAREN {
      str += innerParams.toString().replace( "[", "(" ).replace( "]", ")");} )+

  { params.add( new FormatExpression( str ) ); }
;

identifierList
  returns [ List<Identifier> identifiers = new ArrayList<Identifier>() ]

  { Identifier id = null; }

: id = identifier {identifiers.add( id );}
    ( COMMA id=identifier {identifiers.add( id );} )*
;

implicitList [Specifier spec]
  returns [ List<ImplicitRegister> registers = new
    ArrayList<ImplicitRegister>() ]

  { ImplicitRegister reg = null; }

: reg=implicitRegister[spec] { registers.add( reg ); }
    ( COMMA reg=implicitRegister[spec] { registers.add( reg ); } )*
;

implicitRegister [Specifier spec] returns [ ImplicitRegister register = null ]

: beginChar:. DASH endChar:. {
    return new ImplicitRegister( beginChar.getText().toUpperCase().charAt( 0 ),
      endChar.getText().toUpperCase().charAt( 0 ), spec );
  }
;

expression returns [ Expression expr = null ]

  { Expression rhs = null; }

: expr=conditionalExpression
;

conditionalExpression returns [ Expression expr = null ]

  // Precedence infix parser (not efficient)
: expr=logicalOrExpression
;

logicalOrExpression returns [ Expression expr = null ]

  { Expression rhs = null; }

: expr=logicalAndExpression
    ( LOGICAL_OR^ rhs=logicalAndExpression {
      expr = new BinaryExpression( expr, BinaryOperator.LOGICAL_OR, rhs );
      expr.setParens( false );
    } )*
;

logicalAndExpression returns [ Expression expr = null ]

  { Expression rhs = null; }

: expr=logicalNotExpression
    ( LOGICAL_AND^ rhs=logicalNotExpression {
      expr = new BinaryExpression( expr, BinaryOperator.LOGICAL_AND, rhs );
      expr.setParens( false );
    } )*
;

logicalNotExpression returns [ Expression expr = null ]

  { int negs = 0; }

: (LOGICAL_NEGATION { negs++; })* expr=logicalComparisonExpression {
    for( int i = 0; i < negs; i++ ) {
      expr = new UnaryExpression( UnaryOperator.LOGICAL_NEGATION, expr );
      expr.setParens( false );
    }
  }
;

logicalComparisonExpression returns [ Expression expr = null ]

  { Expression rhs = null; BinaryOperator op = null; }

: expr=additiveExpression
    ( op=comparisonOperator rhs=additiveExpression {
      expr = new BinaryExpression( expr, op, rhs );
      expr.setParens( false );
    } )*
;

additiveExpression returns [ Expression expr = null ]

  { Expression rhs = null; BinaryOperator op = null; }

: expr=multiplicativeExpression
    ( op=additiveOperator rhs=multiplicativeExpression {
      expr = new BinaryExpression( expr, op, rhs );
      expr.setParens( false );
    } )*
;

multiplicativeExpression returns [ Expression expr = null ]

  { Expression rhs = null; BinaryOperator op = null; }

: expr=powerExpression
    ( op=multiplicativeOperator rhs=powerExpression {
      expr = new BinaryExpression( expr, op, rhs );
      expr.setParens( false );
    } )*
;

powerExpression returns [ Expression expr = null ]

  { Expression rhs = null; }

: expr=unaryExpression
    ( POWER rhs=unaryExpression {
      expr = new BinaryExpression( expr, BinaryOperator.F_POWER, rhs );
      expr.setParens( false );
    } )*
;

unaryExpression returns [ Expression expr = null ]

  { UnaryOperator op = null; }

// : (op=prefixOperator)? expr=expressionTerminal {
: (op=prefixOperator)? expr=expressionTerminal {

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
;

expressionTerminal returns [ Expression expr = null ]

  { Identifier id = null; Expression start = null; Expression stop = null;
    IntegerLiteral step = null; boolean parens=false;
    List<Expression> list=null; }

: ( expr=literal
  | expr=identifier
  | expr=arrayAccessOrFunctionCall
//   | LPAREN expr=expression RPAREN { parens=true; } ) {
//       expr.setParens( parens );
//     }
  | LPAREN list=parameterList RPAREN { parens = true; } ) {
    if( list != null ) {
      if( list.size() == 1 ) {
        expr = (Expression)list.get(0);
      } else {
        expr = new CommaExpression( list );
      }
    }

    expr.setParens( parens );
  }
;

additiveOperator returns [ BinaryOperator op = null ]

: ADD { op = BinaryOperator.ADD; }
| DASH { op = BinaryOperator.SUBTRACT; }
| CONCATENATION { op = BinaryOperator.F_CONCAT; }
;

multiplicativeOperator returns [ BinaryOperator op = null ]

: BY { op = BinaryOperator.MULTIPLY; }
| DIVIDE { op = BinaryOperator.DIVIDE; }
;

comparisonOperator returns [ BinaryOperator op = null ]

: COMPARE_EQ { op = BinaryOperator.COMPARE_EQ; }
| COMPARE_GE { op = BinaryOperator.COMPARE_GE; }
| COMPARE_GT { op = BinaryOperator.COMPARE_GT; }
| COMPARE_LE { op = BinaryOperator.COMPARE_LE; }
| COMPARE_LT { op = BinaryOperator.COMPARE_LT; }
| COMPARE_NE { op = BinaryOperator.COMPARE_NE; }
;

prefixOperator returns [ UnaryOperator op = null ]

: DASH { op = UnaryOperator.MINUS; }
| ADD { op = UnaryOperator.PLUS; }
;

arrayAccessOrFunctionCall returns [ Expression expr = null ]

  { Identifier symbol = null; }

: symbol=identifier LPAREN {

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
  }
| symbol=builtInFunctionWithReservedName LPAREN {
    expr = finishFunctionCall( symbol );
  }
;

builtInFunctionWithReservedName returns [ Identifier fName = null ]

: REAL { fName = new Identifier( "REAL" ); }
;

finishSubstringExpression [ Identifier symbol ]
  returns [ Expression expr = null ]

  { Expression ubound = null; }

: expr=parameter ( COLON ubound=parameter )? RPAREN {

    if( ubound == null ) {
      expr = new ArrayAccess( symbol, expr );
    } else {
      expr = new SubstringExpression( symbol, expr, ubound );
    }
  }
;

finishArrayAccess [ Identifier symbol ] returns [ Expression expr = null ]

  { List<Expression> indices = null; }

: indices=parameterList RPAREN { expr = new ArrayAccess( symbol, indices ); }
;

finishFunctionCall [ Identifier symbol ] returns [ Expression expr = null ]

  { List<Expression> parameters = null; }

: (parameters=parameterList)? RPAREN {

  if( parameters == null ) {
    parameters = new ArrayList<Expression>( 0 );
  }
  expr = new FunctionCall( symbol, parameters );
}
;

assignmentTerminal returns [ Expression expr = null ]

: expr = arrayAccessOrFunctionCall
| expr = identifier
;

// Identifier
identifier returns [ Identifier returnId=null ]

: id:ID {
    // We convert into UpperCase all identifiers so that Fortran behaviour is
    // correctly displayed
    returnId = new Identifier( id.getText().toUpperCase() );
  }
;

literal returns [ Literal literal = null ]

: literal=booleanLiteral
| literal=integerLiteral
| literal=realLiteral
| literal=stringLiteral
;

booleanLiteral returns [ BooleanLiteral literal = null ]

: LOGICAL_TRUE { literal = new BooleanLiteral( true ); }
| LOGICAL_FALSE { literal = new BooleanLiteral( false ); }
;

realLiteral returns [ FloatLiteral literal = null ] { String doubleStr = ""; }

: { getLexer().toggleRealLiteral(); }

  ( integerPart:NUMBER { doubleStr += integerPart.getText(); } )?

  ( DOT { doubleStr += "."; } )

  ( realPart:NUMBER { doubleStr += realPart.getText(); } )?

  ( ( separator:E {
    doubleStr += "E";
    if( separator.getText().equalsIgnoreCase( "e" ) ) {
      literal = new FloatLiteral( 0 );
    } else {
      literal = new DoubleLiteral( 0 );
    } } )
    ( expPartSignN:DASH { doubleStr += expPartSignN.getText(); }
    | expPartSignP:ADD { doubleStr += expPartSignP.getText(); } )?
    ( expPart:NUMBER { doubleStr += expPart.getText(); } ) )?

  { if( literal != null ) {
      literal.setValue( new Double( doubleStr ) );
    } else {
      literal = new FloatLiteral( new Double( doubleStr ) );
    }

    getLexer().toggleRealLiteral(); }
;

integerLiteral returns [ IntegerLiteral literal = null ]

: value:NUMBER {
    literal = new IntegerLiteral( new Integer( value.getText() ).intValue() );
  }
;

stringLiteral returns [ StringLiteral literal = null ]

: value:STRINGLITERAL { literal = new StringLiteral( value.getText() ); }
;
