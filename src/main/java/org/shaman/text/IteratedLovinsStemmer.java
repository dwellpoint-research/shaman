/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers (johankaers@gmail.com)                *
 *                                                       * 
 *  Copyright (c) 2001-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;

public class IteratedLovinsStemmer extends LovinsStemmer
{    
    /**
     * Iterated stemming of the given word. Expects word to be lower case.
     *
     * @param str  description of Parameter
     * @return     description of the Returned Value
     */
    public String stem(String str) {
        
        if (str.length() <= 2) {
            return str;
        }
        String stemmed = super.stem(str);
        while (!stemmed.equals(str)) {
            str = stemmed;
            stemmed = super.stem(stemmed);
        }
        return stemmed;
    }
}

