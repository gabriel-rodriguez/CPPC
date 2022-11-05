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

import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.globalnames.GlobalNames;

public final class LanguageAnalyzerFactory {

  private static LanguageAnalyzer instance;

  static {
    String className = ConfigurationManager.getOption(
      GlobalNames.LANGUAGE_ANALYZER_CLASS_OPTION );
    try {
      Class instanceClass = Class.forName( className );
      instance = (LanguageAnalyzer)instanceClass.newInstance();
    } catch( Exception e ) {
      e.printStackTrace();
      System.exit( 0 );
    }
  }

  public static final LanguageAnalyzer getLanguageAnalyzer() {
    return instance;
  }
}
