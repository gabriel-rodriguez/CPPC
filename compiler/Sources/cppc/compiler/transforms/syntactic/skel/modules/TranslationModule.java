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




package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.Statement;
import cetus.hir.TranslationUnit;

import cppc.compiler.cetus.CppcPragmaStatement;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

public abstract class TranslationModule <T extends CppcPragmaStatement> {

  private Method translationMethod = null;

  protected TranslationModule() {}

  public static void printErrorInTranslation( PrintStream stream, Statement s,
    String message ) {

    TranslationUnit tunit = (TranslationUnit)s.getProcedure().getParent();
    stream.println( tunit.getInputFilename() + ": " + s.where() + ": " +
      message );
  }

  public abstract void translate( T pragma );

  public abstract Class<T> getTargetClass();

  public Method getMethod() {
    if( this.translationMethod == null ) {
      try {
        translationMethod = this.getClass().getMethod( "translate",
          this.getTargetClass() );
      } catch( NoSuchMethodException e ) {
        throw new InternalError( "No such method: " + e.getMessage() );
      }
    }

    return translationMethod;
  }
}
