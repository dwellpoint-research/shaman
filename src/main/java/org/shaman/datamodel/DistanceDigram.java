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

import java.util.HashSet;

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Diigram Matching Distance</h2>
 * Distance between 2 string, calculated using digram matching algorithm.<br>
 *
 * @author Johan Kaers
 * @version 2.0
 */
public class DistanceDigram implements Distance
{
    /**
     * The Distance between 2 pre-computed sets of di-grams.
     * @param dg1 Set of di-grams1
     * @param dg2 Set of di-grams2
     * @return The fuzzy match between them
     * @throws DataModelException
     */
    public double distance(HashSet hs1, HashSet hs2) throws DataModelException
    {
        HashSet inter, union;
        double  sin, sun;
        
        // Make intersection and union of the trigrams
        inter = (HashSet)hs2.clone(); inter.retainAll(hs1);
        union = (HashSet)hs2.clone(); union.addAll(hs1);
        
        // Calculate the similarity coeffecicent
        sin = inter.size();
        sun = union.size();
        
        double d;
        if (sin > 0) d = sin/sun;
        else         d = 0;
        
        return(d);
    }
    
    /**
     *  The distance between 2 strings using tri-gram matching.
     *  @param at The Attribute's structure
     *  @param o1 Time Object 1
     *  @param o2 Time Object 2
     *  @return The tri-gram match.
     */
    public double distance(AttributeObject at, Object o1, Object o2) throws DataModelException
    {
        double d;
        String s1, s2;
        
        d = Double.NaN;
        if ((o1 instanceof String) && (o2 instanceof String))
        {
            HashSet hs1, hs2;
            HashSet inter, union;
            double  sin, sun;
            
            // Add some white-spaces to the edges.
            s1 = (String)o1; s2 = (String)o2;
            s1 = " "+s1.trim()+" ";
            s2 = " "+s2.trim()+" ";
            
            // Make the trigrams for both words.
            hs1 = makeDigrams(s1);
            hs2 = makeDigrams(s2);
            
            // Make intersection and union of the trigrams
            inter = (HashSet)hs2.clone(); inter.retainAll(hs1);
            union = (HashSet)hs2.clone(); union.addAll(hs1);
            
            // Calculate the similarity coeffecicent
            sin = inter.size();
            sun = union.size();
            
            d = sin/sun;
        }
        else throw new DataModelException("Digram distance cannot be calculated for non-strings. Input objects are '"+o1.getClass().getName()+"' and '"+o2.getClass().getName()+"'");
        
        return(d);
    }
    
    public static HashSet makeDigrams(String s)
    {
        int     i;
        String  ssnow;
        HashSet hs;
        
        hs = new HashSet();
        for (i=0; i<s.length()-1; i++)
        {
            ssnow = s.substring(i, i+2);
            hs.add(ssnow);
        }
        return(hs);
    }
    
    /**
     * Nothing. The digram distance can only be calculated for Strings.
     * @param at The Attribute describing the structure of the values
     * @param d1 Value 1
     * @param d2 Value 1
     * @return -
     * @throws DataModelException Always.
     */
    public double distance(AttributeDouble at, double d1, double d2) throws DataModelException
    {
        throw new DataModelException("Digram Distance can only be calculated for Strings, not doubles.");
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        return(new DistanceDigram());
    }
    
    public DistanceDigram() {}
}
