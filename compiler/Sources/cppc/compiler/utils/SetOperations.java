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

import java.util.HashSet;
import java.util.Set;

public final class SetOperations<T> {

  public SetOperations() {}

  public final Set<T> setIntersection( Set<T> lhs, Set<T> rhs ) {
    HashSet<T> returnSet = null;
    Set<T> otherSet = null;
    if( lhs.size() > rhs.size() ) {
      returnSet = new HashSet<T>( rhs );
      otherSet = lhs;
    } else {
      returnSet = new HashSet<T>( lhs );
      otherSet = rhs;
    }

    returnSet.retainAll( otherSet );
    return returnSet;
  }

  public final Set<T> setMinus( Set<T> lhs, Set<T> rhs ) {
    HashSet<T> returnSet = new HashSet<T>( lhs );
    returnSet.removeAll( rhs );
    return returnSet;
  }
}
