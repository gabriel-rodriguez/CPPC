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

import cetus.hir.Declarator;
import cetus.hir.Identifier;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Specifier;
import cetus.hir.VariableDeclaration;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommonBlock extends VariableDeclaration {

  private static Method classPrintMethod;

  static {
    Class[] params = { CommonBlock.class, OutputStream.class };

    try {
      classPrintMethod = params[0].getMethod( "defaultPrint", params );
    } catch( NoSuchMethodException e ) {
      e.printStackTrace();
    }
  }

  private Identifier blockName;
  private List<VariableDeclaration> typeInfo;
  private String copiedFrom = null;

  public CommonBlock( Identifier blockName ) {

    super( (Specifier)null, new ArrayList<Declarator>() );

    object_print_method = classPrintMethod;

    this.blockName = blockName;
    typeInfo = new ArrayList<VariableDeclaration>();
  }

  public CommonBlock( Identifier blockName, List<Declarator> declaratorList ) {

    super( (Specifier)null, declaratorList );

    object_print_method = classPrintMethod;

    this.blockName = blockName;
    typeInfo = new ArrayList<VariableDeclaration>();
  }

  public void addDeclaration( VariableDeclaration vd ) {

    if( vd.getParent() != null ) {
      throw new NotAnOrphanException();
    }

    vd.setParent( this );
    typeInfo.add( vd );
  }

  public void removeDeclaration( Identifier id ) {
    for( VariableDeclaration vd: typeInfo ) {
      if( vd.getDeclarator( 0 ).getSymbol().equals( id ) ) {
        vd.setParent( null );
        typeInfo.remove( vd );
        return;
      }
    }
  }

  public VariableDeclaration getDeclaration( Identifier id ) {
    for( VariableDeclaration vd: typeInfo ) {
      if( vd.getDeclaredSymbols().contains( id ) ) {
        return vd;
      }
    }

    return null;
  }

  public List<VariableDeclaration> getDeclarations() {
    return typeInfo;
  }

  public Identifier getBlockName() {
    return blockName;
  }

  public String getCopiedFrom() {
    return this.copiedFrom;
  }

  public void setCopiedFrom( String copiedFrom ) {
    this.copiedFrom = copiedFrom;
  }

  public Object clone() {
    CommonBlock copy = (CommonBlock)super.clone();
    copy.blockName = blockName;
    copy.typeInfo = new ArrayList<VariableDeclaration>();

    for( VariableDeclaration vd: typeInfo ) {
      copy.addDeclaration( (VariableDeclaration)vd.clone() );
    }

    return copy;
  }

  public static void defaultPrint( CommonBlock block,
    OutputStream stream ) {

    PrintStream p = new PrintStream( stream );

    p.print( "COMMON/" + block.blockName + "/" );

    Iterator iter = block.children.iterator();
    while( iter.hasNext() ) {
      ((Declarator)iter.next()).print( stream );
      if( iter.hasNext() ) {
        p.print( ", " );
      }
    }

    // Now print the variable declarator (if it has one)
    if( !block.typeInfo.isEmpty() ) {
      p.println("");
      for( VariableDeclaration vd: block.typeInfo ) {
        vd.print( stream );
        p.println( "" );
      }
    }
  }

  public static void setClassPrintMethod( Method m ) {
    classPrintMethod = m;
  }
}
