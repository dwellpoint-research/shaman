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
 * <h2>Categorical Data Property</h2>
 * Requires that all data equals one value of a predefined
 * list of distinct values.
 */
public class AttributePropertyCategorical implements AttributeProperty 
{
   /**
    *  Check if the given Object is found the in the legal values list.
    *  @param at The Attribute's structure
    *  @param o The Object
    *  @return <code>true</code> if the Object is found. <code>false</code> if not.
    *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeObject at, Object o) throws DataModelException
   {
     int     i;
     boolean foundlegal;
     Object  []obLegal;

     obLegal    = at.getLegalValues();
     foundlegal = false;
     if (obLegal != null)
     {
       for (i=0; (i<obLegal.length) && (!foundlegal); i++)
       {
         if   (o == null) foundlegal = (obLegal[i] == null);
         else             foundlegal = o.equals(obLegal[i]);
       }
     }
     else throw new DataModelException("Cannot check Categorical Attribute Property because legal values list is missing.");

     return(foundlegal);
   }

   /**
    * Check if the given value is found in the legal values list.
    * @param at The Attribute describing the structure of the values
    * @param d The value
    * @return <code>true</code> if the value is found. <code>false</code> if not.
    * @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeDouble at, double d) throws DataModelException
   {
     int     i;
     boolean foundlegal;
     double  []doLegal;

     doLegal    = at.getLegalValues();
     foundlegal = false;
     if (doLegal != null)
     {
       for (i=0; (i<doLegal.length) && (!foundlegal); i++)
       {
         if (Double.isNaN(d)) foundlegal = Double.isNaN(doLegal[i]);
         else                 foundlegal = (d == doLegal[i]);
       }
     }
     else throw new DataModelException("Cannot check Categorical Attribute Property because legal values list is missing.");

     return(foundlegal);
   }
}
