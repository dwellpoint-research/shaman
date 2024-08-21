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
 * <h2>Tag Separator</h2>
 * Separates on XML/HTML Style tags.
 */
public class TagSeparator extends Separator {

    /**
     * Constructor for the TagSeparator object.
     */
    public TagSeparator() {
        mySeparatorTokens = new HashSet();
        mySeparatorTokens.add("<");
        mySeparatorTokens.add(">");
    }

    /**
     * Separator on String input
     *
     * @param inputstring  description of Parameter
     * @return             description of the Returned Value
     */
    public List[] separate(String inputstring) {
        List[] processlist = super.separate(inputstring, true);
        return coreSeparate(processlist);
    }

    /**
     * separate on List[] input
     *
     * @param inputlist  description of Parameter
     * @return           description of the Returned Value
     */
    public List[] separate(List[] inputlist) {
        List[] processlist = super.separate(inputlist, true);
        return coreSeparate(processlist);
    }

    /**
     * Core Separator returns an array of two Lists of Strings
     *
     * @param inputlist  description of Parameter
     * @return           List[0] contains the Strings between the tags
     *                   List[1] contains the tags
     */
    public List[] coreSeparate(List[] inputlist) {
        List[] returnlist = new ArrayList[2];
        returnlist[0] = new ArrayList();
        returnlist[1] = new ArrayList();
        Iterator it = inputlist[0].iterator();
        String currentstring;
        boolean intags = false;
        if (it.hasNext()) {
            currentstring = (String) it.next();
            if (!mySeparatorTokens.contains(currentstring)) {
                returnlist[0].add(fixString(currentstring));
                intags = false;
            }
            else
                intags = true;
        }
        while (it.hasNext()) {
            currentstring = (String) it.next();

            if (mySeparatorTokens.contains(currentstring))
                intags = !intags;
            else
                if (intags) {
                returnlist[1].add(currentstring);

            }
            else {
                if (currentstring.toLowerCase().equals("/br") | (currentstring.toLowerCase().equals("/td"))) {
                    intags = !intags;
                    System.out.println(" Unforeseen switchover occured, please validate html ");
                }
                else {
                    returnlist[0].add(currentstring);
                }
            }
        }
        return returnlist;
    }
}
