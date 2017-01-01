/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 */

package org.jogamp.java3d.utils.geometry;

import org.jogamp.java3d.util.LongSparseIntArray;

class EdgeTable {

  //private HashMap edgeTable;
  private LongSparseIntArray edgeTable2;
  private static final int DEBUG = 0;


  //see http://stackoverflow.com/questions/12772939/java-storing-two-ints-in-a-long
  //long l = (((long)x) << 32) | (y & 0xffffffffL);
 // int x = (int)(l >> 32);
 // int y = (int)l;
  int get(int a, int b)
  {
	  long key = (((long)a) << 32) | (b & 0xffffffffL);
	  int newVal =  edgeTable2.get(key);
	  return  newVal ;
    //return (Integer)edgeTable.get(new Edge(a, b));
  } // End of get()


  //Integer get(Edge e)
  //{
  //  return (Integer)edgeTable.get(e);
  //} // End of get()



  // This function creates a table used to connect the triangles.
  // Here's how it works.  If a triangle is made of indices 12,
  // 40, and 51, then edge(12, 40) gets 51, edge(40, 51) gets 12,
  // and edge(51, 12) gets 40.  This lets us quickly move from
  // triangle to triangle without saving a lot of extra data.
  EdgeTable(int triangleIndices[])
  {
    // We'll have one edge for each vertex
   // edgeTable = new HashMap(triangleIndices.length * 2);
    edgeTable2 = new LongSparseIntArray(triangleIndices.length * 2);

    // Fill in table
    //Edge e;
    for (int t = 0 ; t < triangleIndices.length ; t += 3) {
      // Put all 3 edges of triangle into table
      for (int v = 0 ; v < 3 ; v++) {
    	  // e = new Edge(triangleIndices[t + v],
          //              triangleIndices[t + ((v + 1) % 3)]);
       	  int a = triangleIndices[t + v];
       	  int b = triangleIndices[t + ((v + 1) % 3)];
       	  long key = (((long)a) << 32) | (b & 0xffffffffL);
    if ((DEBUG & 1) != 0) {
       	  if (edgeTable2.get(key) != Integer.MAX_VALUE) {
	//if (edgeTable.get(e) != null) {
	 
	    System.out.println("EdgeTable Error: duplicate edge (" +
	    triangleIndices[t + v] + ", " +
	    triangleIndices[t + ((v + 1) % 3)] + ").");
	  }
    }
	
	  // Store index of 3rd vertex (across from edge)
		int val = t + ((v + 2) % 3);
		edgeTable2.put(key, val);
	  //edgeTable.put(e, new Integer(t + ((v + 2) % 3)));
	
      }
    }

    if ((DEBUG & 1) != 0) 
    {
      System.out.println("Edge Table:");
    /*  Iterator list = edgeTable.keySet().iterator();
      while (list.hasNext()) {
        Edge edge = (Edge)list.next();
        System.out.println("  (" + edge.v1 + ", " + edge.v2 + ") = " +
          get(edge.v1, edge.v2));
      }*/
      for (int i = 0; i < edgeTable2.size(); i++)
		{
      	long key = edgeTable2.keyAt(i);			
  		int v1 = (int)(key >> 32);
  		int v2 = (int)key;
  		System.out.println("  (" + v1 + ", " + v2 + ") = " + get(v1, v2));		
		}
    }
  } // End of constructor EdgeTable

} // End of class EdgeTable

// End of file EdgeTable.java


