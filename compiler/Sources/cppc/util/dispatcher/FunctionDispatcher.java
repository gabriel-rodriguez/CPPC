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




package cppc.util.dispatcher;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.util.Hashtable;

public abstract class FunctionDispatcher<T> {

  private Hashtable<String, Hashtable<Class,Method>> matches;

  public FunctionDispatcher() {
    matches = new Hashtable<String, Hashtable<Class,Method>>();
  }

  public Method dispatch( T t, String methodName ) {
    Method m;
    Hashtable<Class,Method> methodMatches = matches.get( methodName );
    if( methodMatches == null ) {
      try {
        m = getHierarchyDeclaredMethod( this.getClass(), methodName,
          t.getClass() );
        methodMatches = new Hashtable<Class,Method>();
        methodMatches.put( t.getClass(), m );
        matches.put( methodName, methodMatches );
      } catch( NoSuchMethodException e ) {
        return null;
      }
    } else {
      m = methodMatches.get( t.getClass() );
      if( m == null ) {
        try {
          m = getHierarchyDeclaredMethod( this.getClass(),
            methodName, t.getClass() );
          methodMatches.put( t.getClass(), m );
        } catch( NoSuchMethodException e ) {
          return null;
        }
      }
    }

    return m;
  }

  private static Method getHierarchyDeclaredMethod( Class subClass,
    String methodName, Class parameterType ) throws NoSuchMethodException,
    SecurityException {

    try {
      Method m = subClass.getDeclaredMethod( methodName, parameterType );
      return m;
    } catch( NoSuchMethodException e ) {

      try {
        subClass = (Class)subClass.getGenericSuperclass();

        if( subClass == null ) {
          throw e;
        }

        return getHierarchyDeclaredMethod( subClass, methodName,
          parameterType );
      } catch( ClassCastException ex ) {
        throw e;
      } catch( TypeNotPresentException ex ) {
        ex.printStackTrace();
      } catch( MalformedParameterizedTypeException ex ) {
        ex.printStackTrace();
      }
    }

    return null;
  }
}