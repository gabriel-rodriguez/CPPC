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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CommunicationBuffer implements Cloneable {

  private Queue<Communication> unmatchedSends;
  private Queue<Communication> unmatchedRecvs;
  private Queue<Communication> unwaitedComms;
  private Queue<Communication> unmatchedWaits;
  private boolean pendingCommunications;

  public CommunicationBuffer() {
    this.unmatchedSends = new LinkedList<Communication>();
    this.unmatchedRecvs = new LinkedList<Communication>();
    this.unwaitedComms = new LinkedList<Communication>();
    this.unmatchedWaits = new LinkedList<Communication>();

    pendingCommunications = false;
  }

  public CommunicationBuffer( CommunicationBuffer parent ) {
    this.unmatchedSends = new LinkedList<Communication>();
    this.unmatchedRecvs = new LinkedList<Communication>();
    this.unwaitedComms = new LinkedList<Communication>();
    this.unmatchedWaits = new LinkedList<Communication>();

    pendingCommunications = parent.getPendingCommunications();
  }

  public Queue<Communication> getUnmatchedSends() {
    return this.unmatchedSends;
  }

  public void setUnmatchedSends( Queue<Communication> unmatchedSends ) {
    this.unmatchedSends = unmatchedSends;
  }

  public Queue<Communication> getUnmatchedRecvs() {
    return this.unmatchedRecvs;
  }

  public void setUnmatchedRecvs( Queue<Communication> unmatchedRecvs ) {
    this.unmatchedRecvs = unmatchedRecvs;
  }

  public Queue<Communication> getUnwaitedComms() {
    return this.unwaitedComms;
  }

  public void setUnwaitedComms( Queue<Communication> unwaitedComms ) {
    this.unwaitedComms = unwaitedComms;
  }

  public Queue<Communication> getUnmatchedWaits() {
    return this.unmatchedWaits;
  }

  public void setUnmatchedWaits( Queue<Communication> unmatchedWaits ) {
    this.unmatchedWaits = unmatchedWaits;
  }

  public boolean getPendingCommunications() {
    return( this.pendingCommunications || !this.isEmpty() );
  }

  public void setPendingCommunications( boolean pendingCommunications ) {
    this.pendingCommunications = pendingCommunications;
  }

  public List<Communication> getAll() {
    ArrayList<Communication> allComms = new ArrayList<Communication>(
      this.unmatchedSends.size() + this.unmatchedRecvs.size() +
      this.unwaitedComms.size() + this.unmatchedWaits.size() );

    allComms.addAll( unmatchedSends );
    allComms.addAll( unmatchedRecvs );
    allComms.addAll( unwaitedComms );
    allComms.addAll( unmatchedWaits );

    return allComms;
  }

  public boolean remove( Communication comm ) {
    if( this.removeFromBuffer( comm, unmatchedSends ) ) {
      return true;
    }

    if( this.removeFromBuffer( comm, unmatchedRecvs ) ) {
      return true;
    }

    if( this.removeFromBuffer( comm, unwaitedComms ) ) {
      return true;
    }

    if( this.removeFromBuffer( comm, unmatchedWaits ) ) {
      return true;
    }

    return false;
  }

  private boolean removeFromBuffer( Communication comm,
    Queue<Communication> buffer ) {

    // We need to use ==, not equals, since we want to remove it if it is the
    // exact same object, not just any similar one.
    for( Communication c: buffer ) {
      if( c == comm ) {
        buffer.remove( c );
        return true;
      }
    }

    return false;
  }

  public boolean isEmpty() {
    return( this.unmatchedSends.isEmpty() && this.unmatchedRecvs.isEmpty() &&
      this.unwaitedComms.isEmpty() && this.unmatchedWaits.isEmpty() );
  }

  public Object clone() {
    CommunicationBuffer clone = new CommunicationBuffer();

    clone.unmatchedSends.addAll( this.unmatchedSends );
    clone.unmatchedRecvs.addAll( this.unmatchedRecvs );
    clone.unwaitedComms.addAll( this.unwaitedComms );
    clone.unmatchedWaits.addAll( this.unmatchedWaits );

    clone.pendingCommunications = this.pendingCommunications;

    return clone;
  }

  public String toString() {
    return( "Unmatched sends: " + unmatchedSends.size() + "\n" +
      "Unmatched recvs: " + unmatchedRecvs.size() + "\n" +
      "Unwaited  comms: " + unwaitedComms.size() + "\n" +
      "Unmatched waits: " + unmatchedWaits.size() );
  }
}