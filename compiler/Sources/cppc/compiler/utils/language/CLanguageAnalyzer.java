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
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DoLoop;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.TranslationUnit;
import cetus.hir.Traversable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.hir.WhileLoop;

import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class CLanguageAnalyzer implements LanguageAnalyzer {

  public VariableDeclaration addVariableDeclaration( VariableDeclaration d,
    Statement ref ) {

    // Get the current scope body
    CompoundStatement statementList =
      (CompoundStatement)ObjectAnalizer.getParentOfClass( ref,
      CompoundStatement.class );

    statementList.addDeclaration( d );

    return d;
  }

  public boolean annotationIsPragma( Annotation annote ) {
    return annote.getText().startsWith( "#pragma" );
  }

  public boolean beginsInclude( Annotation annote ) {
    String txt = annote.getText();
    return txt.startsWith( "#pragma startinclude" );
  }

  public List<Traversable> buildInclude( String file ) {
    ArrayList<Traversable> ret = new ArrayList<Traversable>( 2 );
    ret.add( new Annotation( "#pragma startinclude #include <" + file + ">" ) );
    ret.add( new Annotation( "#pragma endinclude" ) );

    for( Traversable t: ret ) {
      ((Annotation)t).setPrintMethod( Annotation.print_raw_method );
    }

    return ret;
  }

  public Expression buildStringLiteral( String text ) {
    return new StringLiteral( text );
  }

  public void checkIncludes( Procedure proc ) {
    TranslationUnit tunit = (TranslationUnit)proc.getParent();
    BreadthFirstIterator iter = new BreadthFirstIterator( tunit );
    iter.pruneOn( Declaration.class );
    iter.pruneOn( Statement.class );

    // Check if this unit already has a cppc include
    while( iter.hasNext() ) {
      try {
        Declaration decl = (Declaration)iter.next( Declaration.class );
        if( decl instanceof Annotation ) {
          Annotation annote = (Annotation)decl;
          String file = this.getIncludeFile( annote );
          if( (file != null) && file.endsWith(
            GlobalNamesFactory.getGlobalNames().INCLUDE_FILE() ) ) {
            return;
          }
        }
      } catch( NoSuchElementException e ) {}
    }

    // If not: place the include as the last include in this unit
    iter.reset();
    iter.next(); // Discharge translation unit
    while( iter.hasNext() ) {
      try {
        Object obj = iter.next();
        if( obj instanceof Annotation ) {
          if( this.beginsInclude( (Annotation)obj ) ) {
            int incDepth = 1;
            String file = this.getIncludeFile( (Annotation)obj );

            // Cannot trust the file name, since endinclude pragmas
            // have no file associated. Do it over inc depth
            while( incDepth > 0 ) {
              obj = iter.next( Declaration.class );
              if( obj instanceof Annotation ) {
                if( this.beginsInclude( (Annotation)obj ) ) {
                  ++incDepth;
                }
                if( this.endsInclude( (Annotation)obj, file ) ) {
                  --incDepth;
                }
              }
            }
            continue;
          }
        }

        // Place new include
        List<Traversable> cppcInclude =
          this.buildInclude(GlobalNamesFactory.getGlobalNames().INCLUDE_FILE());
        if( obj instanceof Annotation ) {
          for( Traversable t: cppcInclude ) {
            tunit.addDeclarationAfter( (Declaration)obj, (Declaration)t );
            obj = t;
          }
        } else {
          try {
            Declaration decl = (Declaration)obj;
            for( Traversable t: cppcInclude ) {
              tunit.addDeclarationBefore( decl, (Declaration)t );
            }
          } catch( ClassCastException e ) {
            for( Traversable t: cppcInclude ) {
              tunit.addDeclaration( (Declaration)t );
            }
          }
        }

        return;
      } catch( NoSuchElementException e ) {}
    }
  }

  public VariableDeclaration cloneDeclaration( VariableDeclaration vd,
    Identifier id ) {

    return (VariableDeclaration)vd.clone();
  }

  public boolean endsInclude( Annotation annote, String file ) {
    String txt = annote.getText();
    return txt.startsWith( "#pragma endinclude" );
  }

  public String getIncludeFile( Annotation annote ) {
    String txt = annote.getText();

    if( !txt.startsWith( "#pragma startinclude" ) ) {
      return null;
    }

    txt = txt.replaceFirst( "#pragma startinclude #include \\\"", "" ).trim();
    txt = txt.replaceFirst( "#pragma startinclude #include <", "" ).trim();
    txt = txt.replaceAll( "\\\"", "" ).trim();
    return txt.replaceAll( ">", "" ).trim();
  }

  public String getPragmaText( Annotation annote ) {
    return annote.getText().trim();
  }

  public Expression getReference( Declarator declarator ) {

    if( ( declarator.getSpecifiers().size() == 0 ) &&
      ( declarator.getArraySpecifiers().size() == 0 ) ) {

      return new UnaryExpression( UnaryOperator.ADDRESS_OF,
        (Identifier)declarator.getSymbol().clone() );
    } else {
      return (Identifier)declarator.getSymbol().clone();
    }
  }

  public VariableDeclaration getVariableDeclaration( Traversable ref,
    Identifier var ) throws SymbolNotDefinedException,
    SymbolIsNotVariableException {

    // Get the correspondent symbol table>
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass( ref,
      SymbolTable.class );

    // Check if the variable to be registered is defined in this scope
    Declaration declaration = symbolTable.findSymbol( var );
    if( declaration == null ) {
      throw new SymbolNotDefinedException( var.toString() );
    }

    // Check if this symbol is a variable
    if( !(declaration instanceof VariableDeclaration) ) {
      throw new SymbolIsNotVariableException( var.toString() );
    }
    
    Declarator declarator = ObjectAnalizer.getDeclarator( (VariableDeclaration)declaration, var );
    if( !(declarator instanceof VariableDeclarator) ) {
      throw new SymbolIsNotVariableException( var.toString() );
    }

    return (VariableDeclaration)declaration;
  }

//   public boolean insideLoop( Statement stmt ) {
  public boolean insideLoop( Traversable t ) {
    ForLoop floop = (ForLoop)ObjectAnalizer.getParentOfClass( t,
      ForLoop.class );
    if( floop != null ) {
      return true;
    }

    WhileLoop wloop = (WhileLoop)ObjectAnalizer.getParentOfClass( t,
      WhileLoop.class );
    if( wloop != null ) {
      return true;
    }

    DoLoop dloop = (DoLoop)ObjectAnalizer.getParentOfClass( t,
      DoLoop.class );
    if( dloop != null ) {
      return true;
    }

    return false;
  }

  public Statement getContainerLoopBody( Traversable t ) {
    Loop loop = (Loop)ObjectAnalizer.getParentOfClass( t, Loop.class );
    return (Statement)loop;
  }

}
