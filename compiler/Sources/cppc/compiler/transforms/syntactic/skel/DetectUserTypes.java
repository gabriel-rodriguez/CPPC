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




package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.ArraySpecifier;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Tools;
import cetus.hir.TranslationUnit;
import cetus.hir.UserSpecifier;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import cppc.compiler.transforms.shared.TypedefDataType;
import cppc.compiler.transforms.shared.TypeManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DetectUserTypes {

  private static String passName = "[DetectUserTypes]";
  private Program program;

  private DetectUserTypes( Program program ) {
    this.program = program;
  }

  public static void run( Program program ) {
    Tools.printlnStatus( passName + " begin", 1 );

    // Check if this pass is to be performed
    DetectUserTypes transform = new DetectUserTypes( program );
    transform.start();

    Tools.printlnStatus( passName + " end", 1 );
  }

  private void start() {
    DepthFirstIterator programIter = new DepthFirstIterator( program );
    programIter.pruneOn( TranslationUnit.class );
    programIter.next(); // Discharge program

    while( programIter.hasNext() ) {
      try {
        TranslationUnit tunit = (TranslationUnit)programIter.next(
          TranslationUnit.class );

        // Traverse declarations looking for typedefs
        DepthFirstIterator tunitIter = new DepthFirstIterator( tunit );
        tunitIter.pruneOn( Procedure.class );
        tunitIter.next(); // Discharge translation unit

        while( tunitIter.hasNext() ) {
          try {
            DeclarationStatement s = (DeclarationStatement)tunitIter.next(
              DeclarationStatement.class );
            if( s.getDeclaration() instanceof VariableDeclaration ) {
              VariableDeclaration decl =
                (VariableDeclaration)s.getDeclaration();
              if( decl.getSpecifiers().get( 0 ).equals( Specifier.TYPEDEF ) ) {
                int arraySize = decl.getSpecifiers().size();
                List<Specifier> specs = (List<Specifier>)
                  decl.getSpecifiers().subList( 1, arraySize );

                if( TypeManager.isRegistered( specs ) ) {
                  Identifier baseType =
                    TypeManager.getType( specs ).getBaseType();

                  // If this is not a variable declarator, we are not
                  // interested (it would be a function-based typedef)
                  if( decl.getDeclarator(0) instanceof VariableDeclarator ) {
                    // Get user specifier
                    VariableDeclarator vdecl = (VariableDeclarator)
                      decl.getDeclarator( 0 );

                    Identifier symbol = (Identifier)vdecl.getSymbol();
                    List arraySpecs = vdecl.getArraySpecifiers();
                    int size = 1;
                    if( !arraySpecs.isEmpty() ) {
                      Iterator iter = arraySpecs.iterator();
                      while( iter.hasNext() ) {
                        ArraySpecifier aspec = (ArraySpecifier)iter.next();
                        for( int i = 0; i < aspec.getNumDimensions(); i++ ) {
                          IntegerLiteral dimSize =
                            (IntegerLiteral)aspec.getDimension( i );
                          size *= dimSize.getValue();
                        }
                      }
                    }

                    UserSpecifier uspec = new UserSpecifier( (Identifier)
                      symbol.clone() );

                    // Register user specifier
                    specs = new ArrayList<Specifier>( 1 );
                    specs.add( uspec );
                    TypeManager.addType( specs,
                      new TypedefDataType( baseType, size ) );
                  }
                }
              }
            }
          } catch( NoSuchElementException e ) {}
        }
      } catch( NoSuchElementException e ) {}
    }
  }
}
