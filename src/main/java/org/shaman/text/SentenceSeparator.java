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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * <h2>Sentence Separator</h2>
 * Separates the given strings into (semi-)sentences by looked for punctuation marks.
 */
public class SentenceSeparator extends Separator {

    /**
     * Constructor for the SentenceSeparator object.
     */
    public SentenceSeparator() {
        mySeparatorTokens = new HashSet();
        mySeparatorTokens.add(".");
        mySeparatorTokens.add("?");
        mySeparatorTokens.add("!");
        mySeparatorTokens.add(":");
        mySeparatorTokens.add(";");
        mySeparatorTokens.add("(");
        mySeparatorTokens.add(")");
        mySeparatorTokens.add("\"");
        mySeparatorTokens.add("\t");
        mySeparatorTokens.add("\n");
//        mySeparatorTokens.add("\'");
    }

    /**
     * Transforms an input String into a List of Strings that were separated by
     * the defined tokens
     *
     * @param inputstring  description of Parameter
     * @return             Output List of Strings
     */
    public List[] separate(String inputstring) {
        List[] processlist = super.separate(inputstring, false);
        return coreSeparate(processlist);
    }

    /**
     * Transforms a List of Strings into a List of Strings that were separated
     * by the defined tokens
     *
     * @param inputlist  description of Parameter
     * @return           Output List of Strings
     */
    public List[] separate(List[] inputlist) {
        List[] processlist = super.separate(inputlist, false);
        return coreSeparate(processlist);
    }

    /**
     * Core separator for this Separator class
     *
     * @param inputlist  description of Parameter
     * @return           description of the Returned Value
     */
    public List[] coreSeparate(List[] inputlist) {
        List[] returnlist = new ArrayList[1];
        returnlist[0] = new ArrayList();
        Iterator it = inputlist[0].iterator();
        String currentstring;
        while (it.hasNext()) {
            currentstring = ((String) it.next());
            returnlist[0].add(fixString(currentstring));
        }
        return returnlist;
    }

}

