/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.graph;

/**
 * Graph Node Interface
 */
public interface GraphNode
{
    // Edge definition and Maintenance
    public GraphNode []getNeighbors() throws GraphException;
    public void        setNeighbors(GraphNode []nei) throws GraphException;
    public void        removeNeighbor(long id) throws GraphException;
    public void        addNeighbor(GraphNode nei) throws GraphException;
    public boolean     hasNeighbor(GraphNode nei) throws GraphException;
    
    // Topology and Structure Statistics
    public int         getDegree() throws GraphException;
    
    // Get the (unique ID) of this node
    public long        getID();
    public void        setID(long id);
}
