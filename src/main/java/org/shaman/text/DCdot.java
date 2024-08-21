/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                     Text Mining                       *
\*********************************************************/
package org.shaman.text;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;

/**
 * <h2>Dublin Core Meta-Data Extraction</h2>
 */
public class DCdot {
    
    public static DC processPage(String page) throws IOException
    {
        Reader is = new StringReader(page);
        // Need to look for MIME type and parse HTML or XML as required
        HTMLConsumer hc = new HTMLConsumer();
        DTDParser p = new DTDParser(hc, is);
        p.parse();
        is.close();
        DC dc = hc.getDC();
        if (dc.getFirstElement(DC.TITLE) == null &&
                ! hc.getTitle().equals(""))
            dc.add(new DCElement(DC.TITLE, hc.getTitle()));
        if (dc.getFirstElement(DC.FORMAT) == null)
            dc.add(new DCElement(DC.FORMAT, "", DC.FORMAT_SCHEMES[1], "", "text/html"));
        //if (dc.getFirstElement(DC.IDENTIFIER) == null)
        //   dc.add(new DCElement(DC.IDENTIFIER, tfidentifier.getText()));
        
        return dc;
    }
}

class HTMLConsumer implements DTDConsumer {
    boolean intitle = false;
    StringBuffer title = new StringBuffer("");
    DC dc = new DC();
    
    // Called for each tag
    public boolean processElement(DTDElement e) {
        String tag = e.getTag().toLowerCase();
        if (tag.equals("title")) {
            if (intitle) {
                intitle = false;
            }
            else {
                intitle = true;
            }
        }
        if (tag.equals("meta")) {
            String type = "";
            String scheme = "";
            String content = "";
            String name = "";
            Enumeration enumer = e.getParams().keys();
            while (enumer.hasMoreElements()) {
                String k = (String)enumer.nextElement();
                if (k.equalsIgnoreCase("name"))
                    name = (String)e.getParams().get(k);
                if (k.equalsIgnoreCase("content"))
                    content = (String)e.getParams().get(k);
                if (k.equalsIgnoreCase("scheme"))
                    scheme = (String)e.getParams().get(k);
            }
            if (name == "")
                return(false);
            name = name.toLowerCase();
            if (name.startsWith("dc.")) {
                name = name.substring(3);
                int i = name.indexOf(".");
                if (i > 0) {
                    type = name.substring(i + 1);
                    name = name.substring(0, i);
                }
                
                dc.add(new DCElement(name, type, scheme, "", content));
            }
            else if (name.equals ("description") && content.length() > 2) {
                dc.add(new DCElement(DC.DESCRIPTION, type, scheme, "", content));
            }
            else if (name.equals ("author") && content.length() >= 2 ) {
                dc.add(new DCElement(DC.CREATOR, type, scheme, "", content));
            }
            else if (name.equals ("keywords") && content.length() > 2 ) {
                dc.add(new DCElement(DC.SUBJECT, type, scheme, "", content));
            }
        }
        return(false);
    }
    
    // Called for each string between tags
    public void processString(String s) {
        if (intitle)
            title.append(s);
    }
    
    public String getTitle() {
        return(title.toString());
    }
    
    public DC getDC() {
        return(dc);
    }
}