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
 * <h2>Fuzzy</h2>
 * Extends the contiguous data property with some fuzyness.
 *
 * @author Johan Kaers
 * @version 2.0
 */
public abstract class AttributePropertyFuzzy implements AttributeProperty
{
   public static final String PROPERTY_FUZZY = "fuzzy";

   // **********************************************************\
   // *                   Data Fuzzyfication                   *
   // **********************************************************/
   private double   thres;

   public abstract FMF getFMF(double v) throws DataModelException;
   public abstract FMF getFMF(Object o) throws DataModelException;

   public abstract FMF getRandomFMF() throws DataModelException;

   public double getSimilarity(double v1, double v2) throws DataModelException
   {
     FMF    fmf;
     double fm1, fm2;

     fmf = getFMF(v1);
     fm1 = fmf.apply(v1);
     fm2 = fmf.apply(v2);

     return(Math.abs(fm1-fm2));
   }

   public double getSimilarity(Object o1, Object o2) throws DataModelException
   {
     FMF    fmf;
     double fm1, fm2;

     fmf = getFMF(o1);
     fm1 = fmf.apply(o1);
     fm2 = fmf.apply(o2);
     if   ((fm1 == 0) || (fm2 == 0)) return(0);
     else return(1 - Math.abs(fm1-fm2));
   }

   public double getThreshold()
   {
     return(thres);
   }

   public void setThreshold(double _thres)
   {
     thres = _thres;
   }
}
