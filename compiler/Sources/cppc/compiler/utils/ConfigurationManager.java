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




package cppc.compiler.utils;

import java.util.HashMap;

public final class ConfigurationManager {

  private static HashMap<String,String[]> options =
    new HashMap<String,String[]>();
  private static final String CPPC_ARG_PREFIX = "-CPPC,";

  // Options
  public static final String DISABLE_CKPT_ANALYSIS = "DisableCkptAnalysis";
  public static final String DISABLE_COMM_ANALYSIS = "DisableCommAnalysis";
  public static final String MANUAL_PRAGMAS_OPTION = "ManualPragmas";
  public static final String PROCESS_NUMBER_OPTION = "ProcessNumber";

  private ConfigurationManager() {}

  public static final String[] parseCommandLine( String[] args ) {

    if( args.length == 0 ) {
      return args;
    }

    for( int i = 0; i < args.length; i++ ) {

      String option = args[i].trim();

      // Is this our option?
      if( !option.startsWith( CPPC_ARG_PREFIX ) ) {
        continue;
      }

      // If it is, remove the header
      option = option.replaceFirst( CPPC_ARG_PREFIX, "" );
      option.trim();

      // Split the remaining parts
      String[] subOptions = option.split( "," );

      for( int j = 0; j < subOptions.length; j++ ) {

        String subOption = subOptions[j].trim();

        String[] expr = subOption.split( "=" );

        if( expr.length != 2 ) {
          System.err.println( "WARNING: Ignoring command-line argument '" +
            subOption + "' : Syntax error" );
        } else {
          String key = expr[0].trim();
          String value = expr[1].trim();
          if( options.containsKey( key ) ) {
            String [] values = options.get( key );
            String [] newValues= new String[ values.length+1 ];
            System.arraycopy( values, 0, newValues, 0, values.length );
            newValues[ values.length ] = value;
            options.put( key, newValues );
          } else {
            String [] values = { value };
            options.put( key, values );
          }
        }
      }

      // Remove this option from the list
      String[] newArgs = new String[ args.length - 1 ];
      for( int j = 0; j < i; j++ ) {
        newArgs[ j ] = args[ j ];
      }
      for( int j = i; j < args.length-1; j++ ) {
        newArgs[j] = args[j+1];
      }

      return newArgs;
    }

    return args;
  }

  public static final boolean hasOption( String key ) {
    return options.containsKey( key );
  }

  public static final String getOption( String key ) {
    if( options.containsKey( key ) ) {
      return options.get( key )[ 0 ];
    } else {
      return null;
    }
  }

  public static final String[] getOptionArray( String key ) {
    if( options.containsKey( key ) ) {
      return options.get( key );
    } else {
      return new String[0];
    }
  }

  public static final void setOption( String key, String value ) {
    String [] values = { value.trim() };
    options.put( key.trim(), values );
  }
}
