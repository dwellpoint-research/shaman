package org.shaman.text;

/** DTDConsumer - interface for consuming parsed DTD data */

public interface DTDConsumer {
    // DTD Elements are passed to processElement()
    
    public boolean processElement(DTDElement e);
    
    // intervening data is passed to processString()
    
    public void processString(String s);
}
