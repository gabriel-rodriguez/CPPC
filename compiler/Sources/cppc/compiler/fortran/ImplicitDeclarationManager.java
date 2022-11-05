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




package cppc.compiler.fortran;

import cetus.hir.Identifier;
import cetus.hir.Specifier;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class ImplicitDeclarationManager {

  private static Map<Identifier,List<ImplicitRegister>> declarations =
    new Hashtable<Identifier,List<ImplicitRegister>>();
  private static Identifier lastProc =
    new Identifier( "__ORIGINAL_FORTRAN_STATE__" );

  static {
    // If asking for function return types during preparsing, we need the
    // original Fortran state
    restartState();
  }

  private ImplicitDeclarationManager() {}

  public static final void restartState() {
    ImplicitDeclarationManager.restartState( lastProc );
  }
  public static final void restartState( Identifier procedure ) {
    lastProc = procedure;

    // Remove possible old values
    declarations.remove( procedure );

    List<ImplicitRegister> list = new ArrayList<ImplicitRegister>();
    list.add( new ImplicitRegister( 'A', 'G', Specifier.FLOAT ) );
    list.add( new ImplicitRegister( 'I', 'N', Specifier.INT ) );
    list.add( new ImplicitRegister( 'O', 'Z', Specifier.FLOAT ) );

    declarations.put( procedure, list );
  }

  public static final void addRegister( ImplicitRegister newReg ) {
    ImplicitDeclarationManager.addRegister( lastProc, newReg );
  }
  public static final void addRegister( Identifier procedure,
    ImplicitRegister newReg ) {

    lastProc = procedure;

    List<ImplicitRegister> list = declarations.get( procedure );
    List<ImplicitRegister> collisions = new ArrayList<ImplicitRegister>();
    for( ImplicitRegister reg: list ) {
      if( reg.collides( newReg ) ) {
        collisions.add( reg );
      }
    }

    for( ImplicitRegister reg: collisions ) {
      list.remove( reg );
      ImplicitDeclarationManager.removeCollisionRange( newReg, reg, list );
    }

    list.add( newReg );
  }

  public static final Specifier getType( Identifier var ) {
    return ImplicitDeclarationManager.getType( lastProc, var );
  }
  public static final Specifier getType( Identifier procedure,
    Identifier var ) {

    lastProc = procedure;

    if( declarations.containsKey( procedure ) ) {
      for( ImplicitRegister reg: declarations.get( procedure ) ) {
        if( reg.contains( var ) ) {
          return reg.getSpecifier();
        }
      }
    }

    return null;
  }

  public static final void setImplicitNone() {
    ImplicitDeclarationManager.setImplicitNone( lastProc );
  }
  public static final void setImplicitNone( Identifier procedure ) {
    lastProc = procedure;
    declarations.remove( procedure );
  }

  private static final void removeCollisionRange( ImplicitRegister newReg,
    ImplicitRegister oldReg, List<ImplicitRegister> list ) {

    // If the new register overlaps the old one, nothing to do
    if( newReg.overlaps( oldReg ) ) {
      return;
    }

    // Else, we have to create a new, shorter register, with the info of
    // the old one
    if( newReg.getBeginChar() <= oldReg.getBeginChar() ) {
      list.add( new ImplicitRegister( (char)(newReg.getEndChar()+1),
        oldReg.getEndChar(), oldReg.getSpecifier() ) );
      return;
    } else {
      list.add( new ImplicitRegister( oldReg.getBeginChar(),
        (char)(newReg.getBeginChar()-1), oldReg.getSpecifier() ) );
      if( newReg.getEndChar() <= oldReg.getEndChar() ) {
        list.add( new ImplicitRegister( (char)(newReg.getEndChar()+1),
          oldReg.getEndChar(), oldReg.getSpecifier() ) );
      }
    }
  }
}
