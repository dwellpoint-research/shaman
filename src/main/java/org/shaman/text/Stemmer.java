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

/**
 * <h2>Stemming Algorithm Interface</h2>
 * @version 1.0
 */
public interface Stemmer {

    /**
     * Stems a given String
     *
     * @param word  Input word
     * @return      stemmed word
     */
    public String stem(String word);

}
