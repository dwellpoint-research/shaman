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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.parser.DTD;

/**
 * <h2>HTML Text Separator</h2>
 * Uses Swing's HTML parser to split up the content,
 * then extracts only the text data from the parsed HTML.
 */

public class HTMLSeparator extends Separator
{
    private DTD  dtdHTML4;
    
    public HTMLSeparator()
    {
        try
        {
            dtdHTML4 = DTD.getDTD("texts/loose.dtd");
        }
        catch(IOException ex) { ex.printStackTrace(); }
    }
    
    public List[] separate(List []inlist)
    {
        String               doc;
        ArrayList            sepdoc;
        TextExtractingParser tap;
        List               []lout;
        
        doc = (String)inlist[0].get(0);
        tap = new TextExtractingParser(dtdHTML4);
        tap.parse(doc);
        sepdoc = tap.getTextArrayList();
        
        lout = new List[1];
        lout[0] = sepdoc;
        
        return(lout);
    }
}