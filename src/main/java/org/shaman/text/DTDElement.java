package org.shaman.text;

/** DTDElement - DTD markup element */

import java.util.Enumeration;
import java.util.Hashtable;

public class DTDElement extends Object
{
    private String tag;
    private Hashtable params;
    private boolean end;
    
    public DTDElement(String tag, boolean end)
    {
        this.tag = tag;
        params = new Hashtable();
        this.end = end;
    }
    
    public DTDElement()
    {
        this("", false);
    }
    
    public boolean isEnd()
    {
        return(end);
    }
    
    public void setEnd(boolean end)
    {
        this.end = end;
    }
    
    public void setTag(String tag)
    {
        this.tag = tag;
    }
    
    public String getTag()
    {
        return(tag);
    }
    
    public void addParam(String name, String value)
    {
        params.put(name, (value == null ? "" : value));
    }
    
    public Hashtable getParams()
    {
        return(params);
    }
    
    public String toString()
    {
        StringBuffer s = new StringBuffer(50);
        
        s.append('<');
        if(end) s.append('/');
        s.append(tag);
        
        for(Enumeration e = params.keys(); e.hasMoreElements();)
        {
            String key = (String)e.nextElement();
            
            s.append(' ');
            s.append(key);
            s.append('=');
            s.append('"');
            s.append((String)params.get(key));
            s.append('"');
        }
        s.append('>');
        return(s.toString());
    }
}
