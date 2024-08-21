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
package org.shaman.datamodel;

import org.shaman.exceptions.DataModelException;

/**
 * <h2>Fuzzy Membership Function</h2>
 * Base Interface of a Fuzzy Membership Function
 */
public interface FMF
{
  public double apply(double v) throws DataModelException;
  public double apply(Object o) throws DataModelException;
}
