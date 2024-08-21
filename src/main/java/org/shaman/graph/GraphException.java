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

import java.io.Serializable;

import org.shaman.exceptions.ShamanException;


/**
 * Graph Package Exception
 */
public class GraphException extends ShamanException implements Serializable
{
    public final static long serialVersionUID = 0L;

    public GraphException(String message)
    {
        super(message);
    }
    
    public GraphException(Throwable throwable)
    {
        super(throwable);
    }
    
    public GraphException(String message, Throwable throwable)
    {
        super(message,throwable);
    }
}
