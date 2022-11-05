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




package cppc.compiler.analysis;

import cetus.hir.Identifier;
import cetus.hir.Specifier;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.util.dispatcher.FunctionDispatcher;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public abstract class SpecifierAnalyzer extends
  FunctionDispatcher<Specifier> {

  private static Class instanceClass;
  private static SpecifierAnalyzer instance;
  protected static CppcStatement currentStatement = null;

  static {
    try {
      instanceClass = Class.forName( ConfigurationManager.getOption(
        GlobalNames.SPECIFIER_ANALYZER_CLASS_OPTION ) );
      instance = (SpecifierAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

  protected SpecifierAnalyzer() {}

  public static Set<Identifier> analyzeSpecifier( CppcStatement s,
    Specifier spec ) {

    Method m = instance.dispatch( spec, "analyzeSpecifier" );

    if( m == null ) {
      System.err.println( "WARNING: " +
        "cppc.compiler.analysis.ExpressionAnalyzer.analyzeSpecifier not " +
        "implemented for " + spec.getClass() );

      return createEmptySet();
    }

    CppcStatement oldStatement = currentStatement;
    currentStatement = s;
    try {
      return (Set<Identifier>)m.invoke( instance, spec );
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    } finally {
      currentStatement = oldStatement;
    }

    return createEmptySet();
  }

  protected static Set<Identifier> createEmptySet() {
    return new HashSet<Identifier>( 0 );
  }
}
