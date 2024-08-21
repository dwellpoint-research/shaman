/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.dataflow;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.exceptions.ConfigException;


/**
 * Interface implemented by Transformations that support persistence.
 */
public interface Persister
{
     public void loadState(ObjectInputStream oin) throws ConfigException;
     
     public void saveState(ObjectOutputStream oout) throws ConfigException;
}
