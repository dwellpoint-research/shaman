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
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Parser;
import javax.swing.text.html.parser.TagElement;

/**
 * <h2>Text Extractor</h2>
 * Extracts Words from HTML style string.
 */
public class TextExtractingParser extends Parser
{
    String    documentText;
    ArrayList docal;
    
    public void parse(String doc)
    {
        StringReader srdoc;
        
        try
        {
            srdoc = new StringReader(doc);
            parse(srdoc);
        }
        catch(IOException ex ) { ex.printStackTrace(); }
    }
    
    public ArrayList getTextArrayList()
    {
        return(docal);
    }
    
    public String getText()
    {
        return(documentText);
    }
    
    protected void handleStartTag(TagElement tag)
    {
        if (tag.getElement().getName().equals("p")) documentText += "\n";
    }
    
    protected void handleEndTag(TagElement tag)
    {
        if (tag.getElement().getName().equals("p")) documentText += "\n";
    }
    
    protected void handleText(char[] text)
    {
        int    i;
        String []apcodes = {"&nbsp", "&gt"};
        String txt;
        
        txt = new String(text);
        
        // Remove some common HTML strange character things
        for (i=0; i<apcodes.length; i++)
        {
            while ( txt.indexOf(apcodes[i]) != -1 )
            {
                txt = txt.substring(txt.indexOf(apcodes[i]), txt.lastIndexOf(apcodes[i]));
            }
        }
        
        // Remove whitespaces from the string
        txt = txt.trim();
        
        // If there is still something left, add it to the document.
        if (txt.length() >= 1)
        {
            documentText += txt;
            docal.add(txt);
        }
    }
    
    public TextExtractingParser(DTD dtd)
    {
        super(dtd);
        documentText = "";
        docal = new ArrayList();
    }
}
