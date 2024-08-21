package org.shaman.text;

/** DTDParser - main DTD Parser */

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

public class DTDParser
{
    private DTDConsumer     consumer;
    private StreamTokenizer st;
    private Reader          stream;
    private boolean pre = false;
    private StringBuffer text;
    
    public DTDParser(DTDConsumer consumer, Reader stream)
    {
        this.consumer = consumer;
        this.stream = stream;
        
        text = new StringBuffer(200);
        
        st = new StreamTokenizer(stream);
        //st.lowerCaseMode(true);
        st.ordinaryChar('<');
        st.ordinaryChar('>');
        st.ordinaryChars('#', ';');
        st.ordinaryChars('?', '@');
        st.ordinaryChars('[', '~');
        st.wordChars('!', '!');
        st.wordChars('#', ';');
        st.wordChars('?', '@');
        st.wordChars('[', '~');
        st.ordinaryChar('&');
        st.ordinaryChar('\"');
    }
    
    public void parse() throws IOException
    {
        int c;
        boolean start = false, pstart = false, equals = false,
        intag = false, entity = false, quote = false;
        //String pname = null, pvalue = "";
        String pname = null;
        StringBuffer pvalue = new StringBuffer("");
        DTDElement e = null;
        
        out:
            for(;;)
            {
                c = st.nextToken();
                switch(c)
                {
                case StreamTokenizer.TT_EOF:
                    break out;
                case StreamTokenizer.TT_WORD:
                    if(entity)
                    {
                        pname = st.sval;
                        break;
                    }
                    else if(quote)
                    {
                        //pvalue = st.sval;
                        if (pvalue.length() > 0)
                            pvalue.append("\n");
                        pvalue.append(st.sval);
                        break;
                    }
                if(!intag)
                {
                    if(text.length() > 0) text.append(' ');
                    text.append(st.sval);
                }
                else
                {
                    if(start)
                    {
                        // tag part of DTD element
                        if(st.sval.charAt(0) == '/')
                        {
                            e.setEnd(true);
                            e.setTag(st.sval.substring(1));
                        }
                        else e.setTag(st.sval);
                        start = false;
                        pstart = true;
                    }
                    else
                    {
                        // parameter part of DTD element
                        
                        if(pstart)
                        {
                            // name part of name=value
                            
                            pstart = false;
                            pname = st.sval;
                        }
                        else
                        {
                            // value part of name=value
                            
                            if(equals)
                            {
                                e.addParam(pname, st.sval);
                                equals = false;
                                pname = null;
                                pstart = true;
                            }
                            else
                            {
                                // a valueless parameter
                                e.addParam(pname, "");
                                pname = st.sval;
                                pstart = false;
                            }
                        }
                    }
                }
                
                break;
                
                case '>':
                    if(pname != null)
                        e.addParam(pname, "");
                    start = false;
                    pstart = false;
                    intag = false;
                    sendElement(e);
                    st.wordChars('"', '"');
                    st.ordinaryChar('&');
                    st.ordinaryChar(';');
                    //st.lowerCaseMode(false);
                    break;
                    
                case '<':
                    flushText();
                    e = new DTDElement();
                    start = true;
                    intag = true;
                    st.ordinaryChar('\"');
                    st.wordChars('&', '&');
                    st.wordChars(';', ';');
                    //st.lowerCaseMode(true);
                    break;
                    
                case '\"':
                    if(pstart) continue;
                    if(!quote)
                    {
                        quote = true;
                        //pvalue = "";
                        pvalue = new StringBuffer("");
                        st.wordChars(' ', ' ');
                        st.wordChars('\t', '\t');
                    }
                    else
                    {
                        quote = false;
                        //e.addParam(pname, pvalue);
                        e.addParam(pname, pvalue.toString());
                        pname = null;
                        pstart = true;
                        st.whitespaceChars(' ', ' ');
                        st.whitespaceChars('\t', '\t');
                    }
                    break;
                    
                case '=':
                    equals = true;
                    break;
                    
                case '&':
                    if(!intag)
                    {
                        e = new DTDElement();
                        st.ordinaryChar(';');
                        entity = true;
                    }
                    break;
                    
                case ';':
                    if(entity)
                    {
                        if(pname != null) e.addParam("&" + pname, "");
                        st.wordChars(';', ';');
                        entity = false;
                        sendElement(e);
                    }
                    break;
                    
                default:
                    break;
                }
            }
        flushText();
    }
    
    private void flushText()
    {
        if(text.length() > 0)
        {
            consumer.processString(text.toString());
            text = new StringBuffer(200);
        }
    }
    
    private void sendElement(DTDElement e)
    {
        boolean r = consumer.processElement(e);
        
        if(r)
        {
            if(pre)
            {
                // turn formatting on
                
                st.whitespaceChars('\n', '\n');
                st.whitespaceChars('\f', '\f');
                st.whitespaceChars('\t', '\t');
                st.whitespaceChars(' ', ' ');
                
                pre = false;
            }
            else
            {
                // turn formatting off
                
                st.wordChars('\n', '\n');
                st.wordChars('\f', '\f');
                st.wordChars('\t', '\t');
                st.wordChars(' ', ' ');
                
                pre = true;
            }
        }
    }
}




// end of source
