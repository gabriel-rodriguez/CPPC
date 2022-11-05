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




package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;

import cppc.compiler.utils.ConfigurationManager;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public final class CppcRegisterManager {

  private static HashMap<Identifier,ProcedureCharacterization> procedures;

  // Register families of known functions with no available code (e.g. MPI)
  static {
    CppcRegisterManager.procedures =
      new HashMap<Identifier, ProcedureCharacterization>();
    String indexFileName = ConfigurationManager.getOption(
      "FunctionGrimoire" );

    if( indexFileName != null ) {
      File file = new File( indexFileName );
      if( file == null ) {
        System.err.println( "WARNING: cannot access file " +
          indexFileName + " for reading" );
      } else {
        CppcRegisterParser.parse( file );
      }
    }
  }

  private CppcRegisterManager() {}

  public static final boolean addProcedure( Identifier procedure,
    ProcedureCharacterization characterization) {

    if( procedures.containsKey( procedure ) ) {
      return false;
    }

    procedures.put( procedure, characterization );

    return true;
  }

  public static final boolean isRegistered( Identifier procedure ) {
    return procedures.containsKey( procedure );
  }

  public static final ProcedureCharacterization getCharacterization(
    Identifier procedure ) {

    if( !isRegistered( procedure ) ) {
      return null;
    }

    return procedures.get( procedure );
  }

  public static final HashSet<Identifier> getProceduresWithRole( String role ) {

    HashSet<Identifier> procs = new HashSet<Identifier>();

    Iterator<ProcedureCharacterization> i = procedures.values().iterator();
    while( i.hasNext() ) {
      ProcedureCharacterization c = i.next();

      if( c.hasSemantic( role ) ) {
        procs.add( c.getName() );
      }
    }

    return procs;
  }

}

