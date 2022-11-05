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




package cppc.compiler.cetus;

import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.NotAChildException;
import cetus.hir.Statement;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.transforms.shared.comms.Communication;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CppcStatement extends Statement {

  private Set<Identifier> generated;
  private Set<Identifier> consumed;
  private Set<Identifier> partialGenerated;
  private Set<VariableDeclaration> globalGenerated;
  private Set<VariableDeclaration> globalConsumed;
  private Set<Communication> matchingCommunications;
  private boolean safePoint;
  private long weight;
  public int statementCount;

  private CppcStatement() {
    super();
  }

  public CppcStatement( Statement statement ) {
    super();
    children.add( statement );
    generated = new HashSet<Identifier>();
    consumed = new HashSet<Identifier>();
    partialGenerated = new HashSet<Identifier>();
    globalGenerated = new HashSet<VariableDeclaration>();
    globalConsumed = new HashSet<VariableDeclaration>();
    matchingCommunications = new HashSet<Communication>();
    safePoint = true;
    weight = 0;
  }

  public Set<Identifier> getGenerated() {
    return generated;
  }

  public Set<VariableDeclaration> getGlobalGenerated() {
    return globalGenerated;
  }

  public void setGenerated( Set<Identifier> generated ) {
    this.generated = generated;
  }

  public void setGlobalGenerated( Set<VariableDeclaration> globalGenerated ) {
    this.globalGenerated = globalGenerated;
  }

  public Set<Identifier> getConsumed() {
    return consumed;
  }

  public Set<VariableDeclaration> getGlobalConsumed() {
    return globalConsumed;
  }

  public Set<Identifier> getPartialGenerated() {
    return partialGenerated;
  }

  public Set<Communication> getMatchingCommunications() {
    return matchingCommunications;
  }

  public boolean getSafePoint() {
    return this.safePoint;
  }

  public void setSafePoint( boolean safePoint ) {
    this.safePoint = safePoint;
  }

  public long getWeight() {
    return this.weight;
  }

  public void setWeight( long weight ) {
    this.weight = weight;
  }

  public void setConsumed( Set<Identifier> consumed ) {
    this.consumed = consumed;
  }

  public void setGlobalConsumed( Set<VariableDeclaration> globalConsumed ) {
    this.globalConsumed = globalConsumed;
  }

  public void setPartialGenerated( Set<Identifier> partialGenerated ) {
    this.partialGenerated = partialGenerated;
  }

  public Statement getStatement() {
    return (Statement)children.get( 0 );
  }

  public void removeChild( Traversable child ) throws NotAChildException {

    if( !getStatement().equals( child ) ) {
      throw new NotAChildException();
    }

    children.remove( child );
  }


  public void print( OutputStream stream ) {
    this.getStatement().print( stream );
  }


  public void setLineNumber( int line ) {
    this.getStatement().setLineNumber( line );
  }

  public String toString() {
    return this.getStatement().toString();
  }

  public int where() {
    return this.getStatement().where();
  }

  public Object clone() {
    CppcStatement clone = new CppcStatement(
      (Statement)getStatement().clone() );
    clone.getStatement().setParent( clone );
    clone.generated.addAll( this.generated );
    clone.consumed.addAll( this.consumed );
    clone.globalGenerated.addAll( this.globalGenerated );
    clone.globalConsumed.addAll( this.globalConsumed );

    clone.matchingCommunications.addAll( this.matchingCommunications );
    clone.safePoint = this.safePoint;
    clone.weight = this.weight;

    return clone;
  }
}
