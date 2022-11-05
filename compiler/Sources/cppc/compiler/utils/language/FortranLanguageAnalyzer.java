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




package cppc.compiler.utils.language;

import cetus.hir.Annotation;
import cetus.hir.BreadthFirstIterator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.TranslationUnit;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.cetus.grammars.fortran77.FParser;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.fortran.CommonDeclaration;
import cppc.compiler.fortran.DimensionDeclaration;
import cppc.compiler.fortran.FortranDoLoop;
import cppc.compiler.fortran.ImplicitDeclarationManager;
import cppc.compiler.fortran.ImplicitRegister;
import cppc.compiler.fortran.ParameterDeclaration;
import cppc.compiler.fortran.TypeDeclaration;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FortranLanguageAnalyzer implements LanguageAnalyzer {

  private VariableDeclaration addVariableDeclaration( CommonBlock cb,
    Statement ref ) {

    // First: check that the common block is not already defined
    Procedure proc = ref.getProcedure();
    BreadthFirstIterator iter = new BreadthFirstIterator( proc.getBody() );
    iter.next(); //Discharge compound statement
    iter.pruneOn( Expression.class );
    while( iter.hasNext() ) {
      try {
        CommonBlock old = (CommonBlock)iter.next( CommonBlock.class );
        if( old.getBlockName().equals( cb.getBlockName() ) ) {
          return old;
        }
      } catch( NoSuchElementException e ) {}
    }

    // Create a cosmetic declaration
    CommonDeclaration cosmetic = new CommonDeclaration( cb.getBlockName() );
    for( int i = 0; i < cb.getNumDeclarators(); i++ ) {
      cosmetic.addDeclarator( (Declarator)cb.getDeclarator( i ).clone() );
    }

    // Add the declarations
    CompoundStatement body = proc.getBody();
    ref = ObjectAnalizer.findLastDeclaration( proc );

    if( ref == null ) {
      body.addDeclaration( cb );
      body.addDeclaration( cosmetic );
    } else {
      Statement stmt = new DeclarationStatement( cb );
      body.addStatementAfter( ref, stmt );
      body.addStatementAfter( stmt, new DeclarationStatement( cosmetic ) );
    }

    return cb;
  }

  public VariableDeclaration addVariableDeclaration( VariableDeclaration d,
    Statement ref ) {

    // If this is inside a common block, use the proper method
    if( d.getParent() instanceof CommonBlock ) {
      CommonBlock parent = (CommonBlock)d.getParent();
      CommonBlock cb = (CommonBlock)this.addVariableDeclaration( parent, ref );

      if( cb != d.getParent() ) {

        if( cb.getDeclarations().size() != parent.getDeclarations().size() ) {
          String parentFilename;
          String cbFilename;

          if( parent.getCopiedFrom() == null ) {
            TranslationUnit parentUnit = (TranslationUnit)
              ((Statement)parent.getParent()).getProcedure().getParent();
            parentFilename = parentUnit.getInputFilename() +
              ((Statement)parent.getParent()).where();
          } else {
            parentFilename = parent.getCopiedFrom();
          }

          if( cb.getCopiedFrom() == null ) {
            TranslationUnit cbUnit = (TranslationUnit)
              ((Statement)cb.getParent()).getProcedure().getParent();
            cbFilename = cbUnit.getInputFilename() + ":" +
              ((Statement)cb.getParent()).where();
          } else {
            cbFilename = cb.getCopiedFrom();
          }

          Statement stmt = (Statement)ObjectAnalizer.getParentOfClass( cb,
            Statement.class );
          System.err.println( "error: common block declaration for /" +
            cb.getBlockName() + "/ on " + parentFilename + ":" +
            stmt.where() +
            " substantially differs from the one on " +
            cbFilename + ". Please change them to declare the " +
            "same amount of variables." );
          System.exit( 0 );
        }
        // This means that a new block was not added, but an old one was used
        for( int i = 0; i < parent.getDeclarations().size(); i++ ) {
          if( d == parent.getDeclarations().get( i ) ) {
            return cb.getDeclarations().get( i );
          }
        }
      }

      // If the new block was created, we need cosmetic declarations for inner
      // variables
      for( VariableDeclaration vd: cb.getDeclarations() ) {
        TypeDeclaration cosmetic = null;
        DimensionDeclaration dimCosmetic = null;
        if( vd.getSpecifiers().size() != 0 ) {
          cosmetic = new TypeDeclaration( (Specifier)vd.getSpecifiers().get(
            0 ) );
        }

        for( int i = 0; i < vd.getNumDeclarators(); i++ ) {
          VariableDeclarator decl = (VariableDeclarator)vd.getDeclarator( i );
          if( cosmetic != null ) {
            cosmetic.addDeclarator( (Declarator)decl.clone() );
          } else {
            // If cosmetic is not null, the specifiers are already added to it
            if( decl.getTrailingSpecifiers().size() != 0 ) {
              if( dimCosmetic == null ) {
                dimCosmetic = new DimensionDeclaration();
              }

              dimCosmetic.addDeclarator( (Declarator)decl.clone() );
            }
          }
        }

        ref = (Statement)cb.getParent();
        if( cosmetic != null ) {
          ref.getProcedure().getBody().addStatementAfter( ref,
            new DeclarationStatement( cosmetic ) );
        }
        if( dimCosmetic != null ) {
          ref.getProcedure().getBody().addStatementAfter( ref,
            new DeclarationStatement( dimCosmetic ) );
        }
      }

      return d;
    }

    // Get the procedure body
    Procedure procedure = ref.getProcedure();
    CompoundStatement statementList = procedure.getBody();
    ref = ObjectAnalizer.findLastDeclaration( procedure );

    // Cosmetic type declaration
    TypeDeclaration cosmetic = new TypeDeclaration(
      (Specifier)d.getSpecifiers().get( 0 ) );
    cosmetic.addDeclarator( (Declarator)d.getDeclarator( 0 ).clone() );

    // If it has an initializer: cosmetic parameter declaration
    ParameterDeclaration parameter = new ParameterDeclaration();
    for( int i = 0; i < d.getNumDeclarators(); i++ ) {
      VariableDeclarator decl = (VariableDeclarator)d.getDeclarator( i );
      Initializer init = decl.getInitializer();
      if( init != null ) {
        parameter.addDeclarator( (VariableDeclarator)decl.clone() );
      }
    }

    if( ref != null ) {
      Statement declarationStatement = new DeclarationStatement( d );
      statementList.addStatementAfter( ref, declarationStatement );
      ref = declarationStatement;
      declarationStatement = new DeclarationStatement( cosmetic );
      statementList.addStatementAfter( ref, declarationStatement );
      if( parameter.getDeclarators().size() != 0 ) {
        statementList.addStatementAfter( declarationStatement,
          new DeclarationStatement( parameter ) );
      }
    } else {
      // If ref == null, no declarations and therefore we can just insert
      // as first statement
      statementList.addDeclaration( d );
      statementList.addDeclaration( cosmetic );
      statementList.addDeclaration( parameter );
    }

    return d;
  }

  public boolean annotationIsPragma( Annotation annote ) {
    return annote.getText().startsWith( "CPPC " );
  }

  public boolean beginsInclude( Annotation annote ) {
    String txt = annote.getText();
    return txt.startsWith( FParser.INCLUDE_ANNOTE_TEXT );
  }

  public List<Traversable> buildInclude( String file ) {
    ArrayList<Traversable> ret = new ArrayList<Traversable>( 2 );
    Annotation include = new Annotation( FParser.INCLUDE_ANNOTE_TEXT + " " +
      file );
    ret.add( new DeclarationStatement( include ) );
    ret.add( new DeclarationStatement( (Declaration)include.clone() ) );

    return ret;
  }

  public Expression buildStringLiteral( String text ) {
    FunctionCall zeroChar = new FunctionCall( new Identifier("CHAR") );
    zeroChar.addArgument( new IntegerLiteral( 0 ) );

    BinaryExpression concat = new BinaryExpression(
      new StringLiteral( text ),
      BinaryOperator.F_CONCAT,
      zeroChar );
    concat.setParens( false );

    return concat;
  }

  public void checkIncludes( Procedure proc ) {
    BreadthFirstIterator iter = new BreadthFirstIterator( proc.getBody() );
    iter.next(); // Discharge CompoundStatement
    iter.pruneOn( Statement.class );

    // Check if this Procedure already has an INCLUDE CPPC statement
    while( iter.hasNext() ) {
      Statement stmt = (Statement)iter.next();
      if( stmt instanceof DeclarationStatement ) {
        if( ((DeclarationStatement)stmt).getDeclaration() instanceof
          Annotation ) {

          Annotation annote =
            (Annotation)((DeclarationStatement)stmt).getDeclaration();
          String file = this.getIncludeFile( annote );
          if( (file != null) && file.endsWith(
            GlobalNamesFactory.getGlobalNames().INCLUDE_FILE() ) ) {
            return;
          }
        }
      }
    }

    // If not: place the include as the last include in this procedure
    int i = 0;
    List children = proc.getBody().getChildren();
    while( true ) {
      if( children.get( i ) instanceof DeclarationStatement ) {
        Declaration decl =
          ((DeclarationStatement)children.get(i)).getDeclaration();
        if( decl instanceof Annotation ) {
          if( this.beginsInclude( (Annotation)decl ) ) {
            String file =
              this.getIncludeFile( (Annotation)decl );
            for( int j = i+1; j < children.size(); j++ ) {
              if( children.get( j ) instanceof DeclarationStatement ) {
                decl = ((DeclarationStatement)children.get(j)).getDeclaration();
                if( decl instanceof Annotation ) {
                  if( this.endsInclude( (Annotation)decl, file ) ) {
                    i = j+1;
                    break;
                  }
                }
              }
            }
            continue;
          }
        }
      }

      List<Traversable> cppcInclude =
        this.buildInclude( GlobalNamesFactory.getGlobalNames().INCLUDE_FILE() );
      Statement ref = (Statement)children.get( i );
      if( ref instanceof DeclarationStatement ) {
        if( ((DeclarationStatement)ref).getDeclaration() instanceof
          ImplicitRegister ) {

          for( Traversable t: cppcInclude ) {
            proc.getBody().addStatementAfter( ref, (DeclarationStatement)t );
            ref = (DeclarationStatement)t;
          }

          return;
        }
      }

      for( Traversable t: cppcInclude ) {
        proc.getBody().addStatementBefore( ref, (DeclarationStatement)t );
      }
      return;
    }
  }

  public VariableDeclaration cloneDeclaration( VariableDeclaration vd,
    Identifier id ) {

    if( vd.getParent() instanceof CommonBlock ) {
      CommonBlock parent = (CommonBlock)vd.getParent();
      CommonBlock cb = (CommonBlock)parent.clone();
      cb.setParent( vd.getParent() );

      if( parent.getCopiedFrom() != null ) {
        cb.setCopiedFrom( parent.getCopiedFrom() );
      } else {
        TranslationUnit parentUnit = (TranslationUnit)
          ObjectAnalizer.getParentOfClass( parent, TranslationUnit.class );
        cb.setCopiedFrom( parentUnit.getInputFilename() + ":" +
          ((Statement)parent.getParent()).where() );
      }

      return cb.getDeclaration( id );
    }

    return (VariableDeclaration)vd.clone();
  }

  public boolean endsInclude( Annotation annote, String file ) {
    if( this.beginsInclude( annote ) ) {
      String includeFile = getIncludeFile( annote );
      return includeFile.equals( file );
    }

    return false;
  }

  public Statement getContainerLoopBody( Traversable t ) {
    Loop loop = (Loop)ObjectAnalizer.getParentOfClass( t.getParent(),
      Loop.class );

    if( loop != null ) {
      return loop.getBody();
    }

    return null;
  }

  public String getIncludeFile( Annotation annote ) {

    String txt = annote.getText();
    if( !txt.startsWith( FParser.INCLUDE_ANNOTE_TEXT ) ) {
      return null;
    }

    return txt.replaceFirst( FParser.INCLUDE_ANNOTE_TEXT, "" ).trim();
  }

  public String getPragmaText( Annotation annote ) {
    return annote.getText();
  }

  public Expression getReference( Declarator declarator ) {
    return (Identifier)declarator.getSymbol().clone();
  }

  public VariableDeclaration getVariableDeclaration( Traversable ref,
    Identifier var ) throws SymbolNotDefinedException,
    SymbolIsNotVariableException {

    // Get the correspondent symbol table
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass( ref,
      SymbolTable.class );
    Procedure proc =(Procedure)ObjectAnalizer.getParentOfClass(
      (Traversable)symbolTable,Procedure.class );

    // Check if the variable is defined in this scope
    Declaration decl = symbolTable.findSymbol( var );
    if( decl == null ) {
      // If it is not, use ImplicitDeclarations
      Specifier spec = ImplicitDeclarationManager.getType(
        (Identifier)proc.getName(), var );
      if( spec == null ) {
        throw new SymbolNotDefinedException( var.toString() );
      }

      return new VariableDeclaration( spec, new VariableDeclarator(
        (Identifier)var.clone() ) );
    }

    // Check if this symbol is a variable
    if( !(decl instanceof VariableDeclaration ) ) {
      throw new SymbolIsNotVariableException( var.toString() );
    }

    // If it is a CommonBlock, access the internal declaration
    if( decl instanceof CommonBlock ) {
      return ((CommonBlock)decl).getDeclaration( var );
    } else {
      return (VariableDeclaration)decl;
    }
  }

//   public boolean insideLoop( Statement stmt ) {
  public boolean insideLoop( Traversable t ) {

//     Traversable t = stmt.getParent();
//     Object fdloop = ObjectAnalizer.getParentOfClass( t,
//       FortranDoLoop.class );
    Object loop = ObjectAnalizer.getParentOfClass( t.getParent(),
      Loop.class );
//     if( fdloop != null ) {
    if( loop != null ) {
      return true;
    }

    return false;
  }

}

