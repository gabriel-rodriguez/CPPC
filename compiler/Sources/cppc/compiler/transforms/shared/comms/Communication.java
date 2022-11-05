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




package cppc.compiler.transforms.shared.comms;

import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;

import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

public class Communication implements Cloneable {

  private Map<String,String> properties;
  private Map<String,Expression> expressionProperties;
  private FunctionCall call;
  private List<Expression> conditions;

  // Comm semantics
  public static final String RANKER = "CPPC/Comm/Ranker";
  public static final String RECV = "CPPC/Comm/Recv";
  public static final String SEND = "CPPC/Comm/Send";
  public static final String SIZER = "CPPC/Comm/Sizer";
  public static final String WAIT = "CPPC/Comm/Wait";

  // Implemented role
  public static final String ROLE = "Role";

  // Comm types
  public static final String P2P = "P2P";
  public static final String COLLECTIVE = "Collective";

  // Semantic attributes
  public static final String BLOCKING = "Blocking";
  public static final String BUFFER = "Buffer";
  public static final String COMMUNICATOR = "Communicator";
  public static final String COUNT = "Count";
  public static final String DATATYPE = "Datatype";
  public static final String DESTINATION = "Destination";
  public static final String RANK = "Rank";
  public static final String REQUEST = "Request";
  public static final String SIZE = "Size";
  public static final String SOURCE = "Source";
  public static final String TAG = "Tag";
  public static final String TYPE = "Type";

  // Communication functions
  public static final HashSet<Identifier> rankerFunctions =
    initHashSet( Communication.RANKER );
  public static final HashSet<Identifier> recvFunctions =
    initHashSet( Communication.RECV );
  public static final HashSet<Identifier> sendFunctions =
    initHashSet( Communication.SEND );
  public static final HashSet<Identifier> sizerFunctions =
    initHashSet( Communication.SIZER );
  public static final HashSet<Identifier> waitFunctions =
    initHashSet( Communication.WAIT );

  private static final HashSet<Identifier> initHashSet( String role ) {
    return CppcRegisterManager.getProceduresWithRole( role );
  }

  public Communication() {
    properties = new HashMap<String,String>();
    expressionProperties = new HashMap<String,Expression>();
    call = null;
    conditions = new ArrayList<Expression>();
  }

  public String getProperty( String key ) {
    return properties.get( key );
  }

  public Expression getExpressionProperty( String key ) {
    return expressionProperties.get( key );
  }

  public void setProperty( String key, String value ) {
    properties.put( key, value );
  }

  public void setExpressionProperty( String key, Expression value ) {
    expressionProperties.put( key, value );
  }

  public Map<String,String> getProperties() {
    return properties;
  }

  public Map<String,Expression> getExpressionProperties() {
    return expressionProperties;
  }

  public FunctionCall getCall() {
    return call;
  }

  public List<Expression> getConditions() {
    return this.conditions;
  }

  public void addCondition( Expression condition ) {
    this.conditions.add( condition );
  }

  public Expression getCallValue( String key ) {
    Integer valuePos = new Integer( properties.get( key ) );
    return call.getArgument( valuePos.intValue()-1 );
  }

  public static final Communication fromCall( FunctionCall call ) {

    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName() );

    Communication communication = new Communication();
    Hashtable<String,String> s = c.getSemantic( Communication.SEND );
    if( s == null ) {
      s = c.getSemantic( Communication.RECV );
      if( s == null ) {
        s = c.getSemantic( Communication.WAIT );
        communication.setProperty( Communication.ROLE, Communication.WAIT );
      } else {
        communication.setProperty( Communication.ROLE, Communication.RECV );
      }
    } else {
      communication.setProperty( Communication.ROLE, Communication.SEND );
    }

    for( Enumeration<String> e = s.keys(); e.hasMoreElements(); ) {
      String k = e.nextElement();

      communication.setProperty( k, s.get( k ) );
    }

    communication.call = call;

    return communication;
  }

  public String toString() {

    String txt = "Communication: " + call.getName() + "\n";

    for( String k: properties.keySet() ) {
      String t = properties.get(k);

      try {
        int pos = new Integer( t ).intValue();
        Expression v = call.getArgument( pos-1 );
        txt += "\t" + k + ": " + v.toString() + "\n";
      } catch( Exception e ) {
        txt += "\t" + k + ": " + t + "\n";
      }
    }

    return txt;
  }

  public Object clone() {
    Communication clone = new Communication();
    clone.properties.putAll( this.properties );
    clone.expressionProperties.putAll( this.expressionProperties );

    // The call needs to be cloned to ensure that variable values are
    // propagated correctly (see CommunicationAnalysis.fillCallParameters). Bad
    // part is, we are losing our reference to the original call in the code.
    // This could cause trouble later when working with checkpoint calls, but I
    // guess it will have to be workaround-ed.
    clone.call = (FunctionCall)call.clone();

    // Cloning leaves the call unassociated to a parent, which is bad for us
    clone.call.setParent( call.getParent() );

    return clone;
  }

  public boolean equals( Object obj ) {
    if( !(obj instanceof Communication) ) {
      return false;
    }

    Communication safeObj = (Communication)obj;
    return this.getCall().equals( safeObj.getCall() );
  }
}
