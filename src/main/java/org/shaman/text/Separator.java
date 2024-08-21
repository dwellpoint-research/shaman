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
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <h2>Separator Base Class</h2>
 */
public abstract class Separator {

    /**
     * Set of tokens acting as separators
     */
    public Set mySeparatorTokens = new HashSet();

    /**
     * Constructor for the Separator object.
     */
    public Separator() { }

    /**
     * Returns the separatortokens as a String
     *
     * @return   the tokensAsString value
     */
    public String getTokensAsString() {
        String returnstring = new String();
        Iterator it = mySeparatorTokens.iterator();
        String currenttoken;
        while (it.hasNext()) {
            currenttoken = (String) it.next();
            returnstring = returnstring.concat(currenttoken);
        }
        return returnstring;
    }

    /**
     * Returns the separatortokens as a Set
     *
     * @return   the TokenSet
     */
    public Set getTokens() {
        return mySeparatorTokens;
    }

    /**
     * Adds a token to the Set of separatortokens
     *
     * @param token  Token to be added to the Set
     */
    public void addToken(String token) {
        mySeparatorTokens.add(token);
    }

    /**
     * cleans a String removes obsolete whitespace characters
     *
     * @param currentstring  description of Parameter
     * @return               description of the Returned Value
     */
    public String fixString(String currentstring) {
        StringTokenizer st = new StringTokenizer(currentstring, "\t \n", false);
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * The core of the separator class (to be implemented in extending classes)
     *
     * @param list  description of Parameter
     * @return      description of the Returned Value
     */
    public List[] coreSeparate(List[] list) {
        List[] returnlist = new List[2];

        return returnlist;
    }


    /**
     * Transforms a List of Strings into a List of Strings that were separated by the defined tokens
     *
     * @param list       Input List of Strings
     * @param gettokens  Specifies wether or not to ouput the occurence of the separator tokens
     * @return           Output List of Strings
     */
    public List[] separate(List[] list, boolean gettokens) {
        Iterator it = list[0].iterator();
        StringBuffer sb = new StringBuffer();
        String currentstring;
        while (it.hasNext()) {
            currentstring = (String) it.next();
            sb.append(currentstring);
            sb.append(" ");
        }

        List[] returnlist = separate(sb.toString(), gettokens);
        return returnlist;
    }

    /**
     * like separate(List[], boolean) which always returns the separator tokens
     *
     * @param list  description of Parameter
     * @return      description of the Returned Value
     */
    public List[] separate(List[] list)
    {
        //return separate(list, true);
        return separate(list, false);
    }


    /**
     * Same as Separate(List[]) but operates on an input string
     *
     * @param inputstring  description of Parameter
     * @return             description of the Returned Value
     */
    public List[] separate(String inputstring) {

        return separate(inputstring, false);
    }


    /**
     * same as separate(List[], boolean) but operates on an input String
     *
     * @param inputstring  description of Parameter
     * @param gettokens    description of Parameter
     * @return             description of the Returned Value
     */
    public List[] separate(String inputstring, boolean gettokens) {
        List[] returnlist = new ArrayList[1];
        returnlist[0] = new ArrayList();
        StringTokenizer st = new StringTokenizer(inputstring, getTokensAsString(), gettokens);
        String currenttoken;
        while (st.hasMoreTokens())
        {
            currenttoken = st.nextToken();
            currenttoken = currenttoken.trim();
            if (currenttoken.length() > 0) returnlist[0].add(currenttoken);
        }
        return returnlist;
    }

    /**
     * Removes the specified token from the set of separatortokens
     *
     * @param token  description of Parameter
     */
    public void removeToken(String token) {
        if (mySeparatorTokens.contains(token))
            mySeparatorTokens.remove(token);

    }

}
