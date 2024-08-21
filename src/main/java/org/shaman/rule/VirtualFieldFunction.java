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
package org.shaman.rule;


import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Virtual Field Function</h2>
 * 
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *         Virtual Field Function Interface               *
// **********************************************************/
public interface VirtualFieldFunction
{
   public Object calculate(ObjectMatrix1D profile, DataModelObject dmprof, String fieldname) throws ShamanException;
   
   public void   calculate(DoubleMatrix1D profile, DataModelDouble dmprof, String fieldname) throws ShamanException;
}
