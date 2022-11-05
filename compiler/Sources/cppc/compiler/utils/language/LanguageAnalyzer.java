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
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;

import java.util.List;

public interface LanguageAnalyzer {
    public VariableDeclaration addVariableDeclaration( VariableDeclaration d,
      Statement ref );
    public boolean annotationIsPragma( Annotation annote );
    public boolean beginsInclude( Annotation annote );
    public List<Traversable> buildInclude( String file );
    public Expression buildStringLiteral( String text );
    public void checkIncludes( Procedure proc );
    public VariableDeclaration cloneDeclaration( VariableDeclaration vd,
      Identifier id);
    public boolean endsInclude( Annotation annote, String file );
    public Statement getContainerLoopBody( Traversable t );
    public String getIncludeFile( Annotation annote );
    public String getPragmaText( Annotation annote );
    public Expression getReference( Declarator declarator );
    public VariableDeclaration getVariableDeclaration( Traversable ref,
      Identifier var ) throws SymbolNotDefinedException,
      SymbolIsNotVariableException;
    public boolean insideLoop( Traversable t );
}
