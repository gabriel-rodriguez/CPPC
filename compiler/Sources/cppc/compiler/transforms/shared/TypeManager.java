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
import cetus.hir.PointerSpecifier;
import cetus.hir.Specifier;

import cppc.compiler.fortran.ComplexSpecifier;
import cppc.compiler.fortran.DoubleComplexSpecifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class TypeManager {

  private static HashMap<List<Specifier>,DataType> types;

  // Register known base types
  static {
    TypeManager.types = new HashMap<List<Specifier>,DataType>();

    List<Specifier> specs;

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.CHAR );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_CHAR" ) ) );

    specs = new ArrayList<Specifier>( 2 );
    specs.add( Specifier.UNSIGNED );
    specs.add( Specifier.CHAR );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_UCHAR" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.SHORT );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_SHORT" ) ) );

    specs = new ArrayList<Specifier>( 2 );
    specs.add( Specifier.UNSIGNED );
    specs.add( Specifier.SHORT );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_USHORT" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.INT );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_INT" ) ) );

    specs = new ArrayList<Specifier>( 2 );
    specs.add( Specifier.UNSIGNED );
    specs.add( Specifier.INT );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_UINT" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.LONG );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_LONG" ) ) );

    specs = new ArrayList<Specifier>( 2 );
    specs.add( Specifier.UNSIGNED );
    specs.add( Specifier.LONG );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_ULONG" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.FLOAT );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_FLOAT" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.DOUBLE );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_DOUBLE" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( Specifier.BOOL );
    types.put( specs, new BasicDataType( new Identifier( "CPPC_UCHAR" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( ComplexSpecifier.instance() );
    types.put( specs, new ComplexDataType( new Identifier( "CPPC_FLOAT" ) ) );

    specs = new ArrayList<Specifier>( 1 );
    specs.add( DoubleComplexSpecifier.instance() );
    types.put( specs, new ComplexDataType( new Identifier( "CPPC_DOUBLE" ) ) );
  }

  private TypeManager() {}

  public static final boolean addType( List<Specifier> specs, DataType type ) {
    if( types.containsKey( specs ) ) {
      return false;
    }

    types.put( specs, type );

    return true;
  }

  public static final boolean isBasicType( List<Specifier> specs ) {
    if( !isRegistered( specs ) ) {
      return false;
    }

    DataType type = types.get( specs );
    return( type instanceof BasicDataType );
  }

  public static final boolean isRegistered( List<Specifier> specs ) {
    List<Specifier> copy = new ArrayList<Specifier>( specs.size() );
    for( Specifier s: specs ) {
      if( !(s instanceof PointerSpecifier) ) {
        copy.add( s );
      }
    }

    return types.containsKey( copy );
  }

  public static final DataType getType( List<Specifier> specs ) {
    List<Specifier> copy = new ArrayList<Specifier>( specs.size() );
    for( Specifier s: specs ) {
      if( !(s instanceof PointerSpecifier) ) {
        if( s != Specifier.CONST ) {
          copy.add( s );
        }
      }
    }

    return types.get( copy );
  }
}

