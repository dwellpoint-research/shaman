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
 * <h2>Continuous Fuzzy Membership Function</h2>
 */
public class FMFContinuous  implements FMF
{
     // **********************************************************\
     // *               Continuous Fuzzy Function                *
     // **********************************************************/
     private static double overlap;
     
     private double thres;
     
     private double []intMin;
     private double []intMax;
     private int    index;
     
     private double min;
     private double max;
     private double mid;
     
     private double fuzmin, fuzmax, fuzmid;
     
     public FMFContinuous(double min, double max) throws DataModelException
     {
        this.min = min;
        this.max = max;
        
        this.mid = (min+max)/2;
     }
     
     public FMFContinuous() throws DataModelException
     {
         
     }
     
     public void setBounds(double fuzmin, double fuzmax)
     {
         this.fuzmin = fuzmin;
         this.fuzmax = fuzmax;
         this.fuzmid = (fuzmin+fuzmax)/2;
     }
     
     public void init()
     {
         double fuzmin, fuzmax;
         double edge;
         int    indedge;
         double overlap;
         
         overlap = FMFContinuous.overlap;
         indedge = this.index - (int)(overlap);
         if (indedge < 0)
         {
            fuzmin = this.intMin[0];
         } 
         else
         {
            overlap = overlap - ((int)overlap);
            edge    = this.intMax[indedge];
            edge   -= (this.intMax[indedge] - this.intMin[indedge])*overlap;
            fuzmin  = edge;
         }
         
         overlap = FMFContinuous.overlap;
         indedge = this.index + (int)overlap;
         if (indedge >= this.intMax.length)
         {
            fuzmax = this.intMax[intMax.length-1];
         }
         else
         {
             overlap = overlap - ((int)overlap);
             edge    = this.intMin[indedge];
             edge   += (this.intMax[indedge] - this.intMin[indedge])*overlap;
             fuzmax  = edge;
         }
         
         this.fuzmin = fuzmin;
         this.fuzmax = fuzmax;
         this.fuzmid = (fuzmax+fuzmin)/2;
     }

     public double apply(double v) throws DataModelException
     {
         double m;
         double fuzmin, fuzmax, fuzmid;
         
         fuzmin = this.fuzmin;
         fuzmax = this.fuzmax;
         fuzmid = this.fuzmid;
         
         m = 0;
         if ((fuzmin != fuzmax))
         {
             if ((v > fuzmin) && (v < fuzmax))
             {
                 if (v <= fuzmid)
                 {
                     m = 1.0-((v-min)/(mid-min));
                 }
                 else
                 {
                     m = ((v-mid))/((max-mid));
                 }
             }
             else if ((v == fuzmin) || (v == fuzmax)) m = thres;
         }
         else
         {
             if (v == fuzmid) m = 1.0;
         }
         
         return(m);
     }

     public double apply(Object o) throws DataModelException
     {
         return(0);
     }
     
     public static void setOverlap(double overlap)
     {
         FMFContinuous.overlap = overlap;
     }
     
     public void setThreshold(double thres)
     {
         this.thres = thres;
     }
     
     public void setIntervals(double []intMin, double []intMax, double []intMid)
     {
         this.intMin = intMin;
         this.intMax = intMax;
     }
     
     public void setIntervalIndex(int index)
     {
         this.index = index;
     }
}