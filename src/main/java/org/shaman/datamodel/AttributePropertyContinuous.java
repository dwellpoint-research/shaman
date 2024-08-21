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
 * <h2>Continuous Data Property</h2>
 * Requires all data to lie within the intervals beginning with the
 * even- and ending with the odd-indexed values in the legal value
 * array of the attribute. Uses the attribute's order relation to
 * determine if the given value lies in these intervals.
 */
public class AttributePropertyContinuous implements AttributeProperty
{
   /**
    *  Check if the given Object lies within the legal continuous value intervals.
    *  @param at The Attribute's structure
    *  @param o The Object
    *  @return <code>true</code> if the Object is legal. <code>false</code> if not.
    *  @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeObject at, Object o) throws DataModelException
   {
     int     i;
     Order   atorder;
     boolean foundlegal;
     Object  []obLegal;

     foundlegal = false;
     if (o != null)
     {
       atorder    = at.getOrder();
       obLegal    = at.getLegalValues();
       if ((atorder != null) && (obLegal != null))
       {
         for (i=0; (i<obLegal.length) && (!foundlegal); i+=2)
         {
            if ((at.order(o, obLegal[i])   >= 0) &&
                (at.order(obLegal[i+1], o) >= 0)) foundlegal = true;
         }
       }
       else throw new DataModelException("Cannot check Continuous Attribute Property because the AttributeObject's structure is wrong.");
     }

     return(foundlegal);
   }

   /**
    * Check if the given value within the legal continuous value intervals.
    * @param at The Attribute describing the structure of the values
    * @param d The value
    * @return <code>true</code> if the value is legal. <code>false</code> if not.
    * @throws DataModelException If something is wrong with the Attribute's structure w.r.t this property.
    */
   public boolean isLegal(AttributeDouble at, double d) throws DataModelException
   {
     int     i;
     Order   atorder;
     boolean foundlegal;
     double  []doLegal;

     atorder    = at.getOrder();
     doLegal    = at.getLegalValues();
     foundlegal = false;
     if ((atorder != null) && (doLegal != null))
     {
       for (i=0; (i<doLegal.length) && (!foundlegal); i+=2)
       {
          if ((at.order(d, doLegal[i])   >= 0) &&
              (at.order(doLegal[i+1], d) >= 0)) foundlegal = true;
       }
     }
     else throw new DataModelException("Cannot check Continuous Attribute Property because the AttributeDouble's structure is wrong.");

     return(foundlegal);
   }
}
