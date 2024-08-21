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

import java.util.HashSet;
import java.util.List;

/**
 * <h2>Word Separator</h2>
 * Separates sentences into words.
 */
public class WordSeparator extends Separator {

    /**
     * Constructor for the WordSeparator object.
     */
    public WordSeparator() {
        mySeparatorTokens = new HashSet();
        mySeparatorTokens.add(" ");
        mySeparatorTokens.add("\n");
        mySeparatorTokens.add("\t");
        mySeparatorTokens.add("@");
        mySeparatorTokens.add("#");
        mySeparatorTokens.add("%");
        mySeparatorTokens.add("&");
        mySeparatorTokens.add("*");
        mySeparatorTokens.add("(");
        mySeparatorTokens.add(")");
        mySeparatorTokens.add(".");
        mySeparatorTokens.add(",");
        mySeparatorTokens.add("?");
        mySeparatorTokens.add("!");
        mySeparatorTokens.add(":");
        mySeparatorTokens.add(";");
        mySeparatorTokens.add("/");
        mySeparatorTokens.add("|");
        mySeparatorTokens.add("-");
        mySeparatorTokens.add("\"");
        mySeparatorTokens.add("\'");
    }

    /**
     * Transforms a String into a List of Strings that were separated by the defined tokens
     *
     * @param list       Input List of Strings
     * @param gettokens  Specifies wether or not to ouput the occurence of the separator tokens
     * @return           Output List of Strings
     */
    public List[] separate(String inputstring) {
        List[] processlist = super.separate(inputstring, false);
        return coreSeparator(processlist);
    }

    /**
     * Core separator for thes Separator class
     *
     * @param inputlist  description of Parameter
     * @return           description of the Returned Value
     */
    public List[] coreSeparator(List[] inputlist) {

        return inputlist;
    }


}

