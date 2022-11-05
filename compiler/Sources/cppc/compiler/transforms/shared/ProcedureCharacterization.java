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
import cetus.hir.Procedure;
import cetus.hir.VariableDeclaration;

import cppc.compiler.transforms.shared.comms.CommunicationBuffer;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProcedureCharacterization {

  private Identifier name;
  private Procedure procedure;
  private Set<ProcedureParameter> generated;
  private Set<ProcedureParameter> consumed;

  //Global variables handling
  private Set<VariableDeclaration> globalGenerated;
  private Set<VariableDeclaration> globalConsumed;

  //Semantics handling
  private Hashtable<String,Hashtable<String,String>> semantics;

  //Enter pragmed procedures handling
  private boolean isPragmed;

  //Add registers for checkpoints inside procedures handling
  private boolean isCheckpointed;

  //Null registers are marked using this
  private boolean isNull;

  // Communication handling
  private CommunicationBuffer commBuffer;

  // Computational cost
  private long weight;
  public int statementCount;

  // Calling order
  private Set<Identifier> calledFrom;
  private Set<Identifier> calls;

  // Variable dependencies
  private Map<Identifier,Set<Identifier>> variableDependencies;

  public ProcedureCharacterization( Identifier name ) {
    this.name = name;
    this.procedure = null;
    this.generated = new HashSet<ProcedureParameter>( 0 );
    this.consumed = new HashSet<ProcedureParameter>( 0 );
    this.globalGenerated = new HashSet<VariableDeclaration>( 0 );
    this.globalConsumed = new HashSet<VariableDeclaration>( 0 );
    this.semantics = new Hashtable<String,Hashtable<String,String>>( 0 );
    this.isPragmed = false;
    this.isCheckpointed = false;
    this.isNull = false;
    this.commBuffer = null;
    this.weight = 1;
    this.calledFrom = new HashSet<Identifier>();
    this.calls = new HashSet<Identifier>();
    this.variableDependencies = null;
  }

  public Identifier getName() {
    return name;
  }

  public Procedure getProcedure() {
    return this.procedure;
  }

  public void setProcedure( Procedure procedure ) {
    this.procedure = procedure;
  }

  public Set<ProcedureParameter> getGenerated() {
    return generated;
  }

  public void setGenerated( Set<ProcedureParameter> generated ) {
    this.generated = generated;
  }

  public Set<ProcedureParameter> getConsumed() {
    return consumed;
  }

  public void setConsumed( Set<ProcedureParameter> consumed ) {
    this.consumed = consumed;
  }

  public void setGlobalGenerated( Set<VariableDeclaration> globalGenerated ) {
    this.globalGenerated = globalGenerated;
  }

  public Set<VariableDeclaration> getGlobalGenerated() {
    return globalGenerated;
  }

  public void setGlobalConsumed( Set<VariableDeclaration> globalConsumed ) {
    this.globalConsumed = globalConsumed;
  }

  public Set<VariableDeclaration> getGlobalConsumed() {
    return globalConsumed;
  }

  public void setSemantics(
    Hashtable<String,Hashtable<String,String>> semantics ) {

    this.semantics = semantics;
  }

  public boolean hasSemantic( String key ) {
    return (semantics.get( key ) != null);
  }

  public void addSemantic( String role, Hashtable<String,String> parameters ) {
    semantics.put( role, parameters );
  }

  public Hashtable<String,String> getSemantic( String key ) {
    return semantics.get( key );
  }

  public boolean getPragmed() {
    return isPragmed;
  }

  public void setPragmed( boolean isPragmed ) {
    this.isPragmed = isPragmed;
  }

  public boolean getCheckpointed() {
    return isCheckpointed;
  }

  public void setCheckpointed( boolean isCheckpointed ) {
    this.isCheckpointed = isCheckpointed;
  }

  public boolean isNull() {
    return this.isNull;
  }

  public void setNull( boolean b ) {
    this.isNull = b;
  }

  public CommunicationBuffer getCommunicationBuffer() {
    return this.commBuffer;
  }

  public void setCommunicationBuffer( CommunicationBuffer commBuffer ) {
    this.commBuffer = commBuffer;
  }

  public long getWeight() {
    return this.weight;
  }

  public void setWeight( long weight ) {
    this.weight = weight;
  }

  public Set<Identifier> getCalledFrom() {
    return this.calledFrom;
  }

  public Set<Identifier> getCalls() {
    return this.calls;
  }

  public void addCalledFrom( Identifier proc ) {
    this.calledFrom.add( proc );
  }

  public void addCall( Identifier proc ) {
    this.calls.add( proc );
  }

  public Map<Identifier,Set<Identifier>> getVariableDependencies() {
    return this.variableDependencies;
  }

  public Set<Identifier> getVariableDependencies( Identifier id ) {
    return this.variableDependencies.get( id );
  }

  public void setVariableDependencies( Map<Identifier,Set<Identifier>> vd ) {
    this.variableDependencies = vd;
  }
}
