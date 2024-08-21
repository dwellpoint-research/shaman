/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *                                                       * 
 *  Copyright (c) 2001-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;


/**
 * <h2>Filter for Words containing digits</h2>
 * StopList that checks specifically for strange characters in words
 */

public class DigitWords extends StopList {
    /**
     * Constructor for the DigitWords object.
     */
    public DigitWords() { }
    
    /**
     * returns a boolean value indicating that the given words contains digits
     * or other strange tokens
     *
     * @param word  Word to be checked
     * @return      true if word is a stopword
     */
    
    public boolean isStopWord(String word) {
        boolean returnvalue = false;
        char[] letters = word.toCharArray();
        
        for (int i = 0; i < letters.length; i++) {
            returnvalue = returnvalue | (!Character.isJavaIdentifierStart(letters[i])) & Character.isDigit(letters[i]);
        }
        
        if (!returnvalue && (word.equals(">"))) returnvalue = true;
        
        return returnvalue;
    }
    
}
