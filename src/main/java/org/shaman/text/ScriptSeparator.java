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
import java.util.List;

/**
 * <h2>HTML Script Separator</h2>
 */
public class ScriptSeparator extends Separator
{
    public ScriptSeparator()
    {
    }
    
    public List[] separate(String inputstring) {
        
        return separate(inputstring, false);
    }
    
    /**
     * Transforms a String into a List of Strings that were separated by the defined tokens
     *
     * @param list       Input List of Strings
     * @param gettokens  Specifies wether or not to ouput the occurence of the separator tokens
     * @return           Output List of Strings
     *
     */
    public List[] separate(String inputstring, boolean gettokens) {
        List[] returnlist = new ArrayList[2];
        returnlist[0] = new ArrayList();
        returnlist[1] = new ArrayList();
        String workstring = new String(inputstring);
        int runningcount1 = 0;
        int runningcount2 = 0;
        runningcount1 = workstring.toLowerCase().indexOf("<script");
        
        while (runningcount1 != -1)
        {
            runningcount2 = workstring.toLowerCase().indexOf("</script",runningcount1);
            returnlist[0].add(workstring.substring(0,runningcount1));
            returnlist[1].add(workstring.substring(runningcount1, runningcount2 + 9) );
            workstring = workstring.substring(runningcount2 +  9);
            runningcount1 = workstring.toLowerCase().indexOf("<script");
            runningcount2 = workstring.toLowerCase().indexOf("</script");
        }
        if (runningcount1 == -1)
            returnlist[0].add(workstring);
        return returnlist;
    }
}