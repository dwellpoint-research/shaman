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

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.shaman.util.FileUtil;

/**
 * <h2>Text Document</h2>
 * This Class represents a document and keeps track of some properties (keywordlist, filename, ...)
 */
public class Document
{
    private String contents;         // The text Content of this document
    private DC     dublinCore;       // Dublin Core Meta-Data of this document
    
    /**
     * Name of the file to which the Document object refers
     */
    public String myFileName = null;
    
    /**
     * The orginal URL of this document
     */
    public String myURL;
    
    /**
     * Maximum number of keywords a Document should keep track of
     */
    public int myNrOFKeyWords = 10;
    
    /**
     * Map of Keywords and their TF*Log(IDF) value.
     */
    public Map myKeyWords;
    
    
    // *********************************************************\
    // *    Get the keywords from the Dublin Core metadata     *
    // *********************************************************/
    /**
     * Get the keywords from the Dublin-Core meta-data.
     * @return The list of Dublin-Core keywords.
     */
    public String []getDCKeywords()
    {
        String          []keys;
        java.util.StringTokenizer keytok;
        String          keystr;
        Vector          keyvec;
        String          keynow;
        
        keys = null;
        keystr = dublinCore.getFirstElementValue("subject");
        if (keystr != null)
        {
            keytok = new java.util.StringTokenizer(keystr, ",;");
            keyvec = new Vector();
            while(keytok.hasMoreTokens())
            {
                keynow = (String)keytok.nextToken();
                keynow = keynow.toLowerCase();
                keynow = keynow.trim();
                
                if ((keynow.length() > 0) && (!keyvec.contains(keynow))) keyvec.add(keynow);
            }
            if (keyvec.size() > 2)
            {
                keys = new String[keyvec.size()];
                keyvec.copyInto(keys);
            }
        }
        
        return(keys);
    }
    
    /**
     * Constructor for the Document object.
     *
     * @param fileName  initial filename
     */
    public Document(String fileName)
    {
        myFileName = fileName;
        myKeyWords = new HashMap();
    }
    
    /**
     * Constructor for the Document object if the original URL is known.
     *
     * @param fileName  initial filename
     * @param url the location of the file
     */
    public Document(String fileName, String url)
    {
        myFileName = fileName;
        myURL      = url;
        myKeyWords = new HashMap();
    }
    
    /**
     * Constructor for the Document object.
     */
    public Document()
    {
        myFileName = "";
        myURL      = "";
        myKeyWords = new HashMap();
    }
    
    
    /**
     * Sets the filename for this Document
     *
     * @param filename  description of Parameter
     */
    public void setFileName(String filename) {
        myFileName = filename;
    }
    
    /**
     * Sets the maximum number of keywords a Document object should keep track of
     *
     * @param nr  number of keywords
     */
    public void setNrOfKeyWords(int nr) {
        myNrOFKeyWords = nr;
    }
    
    /**
     * returns the maximum number of keywords a Document object keeps track of
     *
     * @return   number of keywords
     */
    public int getNrOfKeyWords() {
        return myNrOFKeyWords;
    }
    
    /**
     * Set the content of the document.
     * @param _contents The new contents.
     */
    public void setContents(String _contents)
    {
        contents = _contents;
    }
    
    /**
     * Get the contents of the document.
     * @return The contents string.
     */
    public String getContents()
    {
        if (contents == null)
        {
            File currentfile = new File(myFileName);
            contents = new String();
            if (!myFileName.endsWith(".ntm"))
            {
                try {  contents = FileUtil.readTextFileToString(currentfile, false); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }
        return contents;
    }
    
    /**
     * returns a string containing the enire contents of a file with the specified name
     *
     * @param filename  file to be parsed
     * @return          the contents
     */
    public String getContents(String filename) {
        setFileName(filename);
        return getContents();
    }
    
    /**
     * Get the Dublin-Core meta-data of this Document.
     * @return The Dublin-Core meta-data.
     */
    public DC getDublinCore()
    {
        return(dublinCore);
    }
    
    /**
     * Set the Dublin-Core meta-data of this Document.
     * @param _dublinCore The new Dublin-Core meta-data.
     */
    public void setDublinCore(DC _dublinCore)
    {
        dublinCore = _dublinCore;
    }
    
    
    
    /**
     * returns the filename
     *
     * @return   fileName
     */
    public String getFileName() {
        return myFileName;
    }
    
    /**
     * returns the original on-line URL of the page
     *
     * @return   url
     */
    public String getURL() {
        return myURL;
    }
    
    
    /**
     * updates the Document's keyword list when necessary.
     * This method is generally called by the class that calculated the quality factor
     *
     * @param word   The Word object that should be considered as keyword
     * @param TFIDF  Quality measure for the keyword (TF*Log(IDF) in this case.)
     */
    public void assertKeyWord(Word word, double TFIDF) {
        
        if (myKeyWords.size() < myNrOFKeyWords)
        {
            myKeyWords.put(word, new Double(TFIDF));
        }
        else
        {
            Map newKeywords = new HashMap();
            Iterator it = myKeyWords.keySet().iterator();
            double currenttfidf = TFIDF;
            Word currentword;
            while (it.hasNext()) {
                currentword = (Word) it.next();
                if (((Double) myKeyWords.get(currentword)).doubleValue() < currenttfidf)
                {
                    newKeywords.put(word, new Double(currenttfidf));
                    currenttfidf = ((Double) myKeyWords.get(currentword)).doubleValue();
                    word = currentword;
                }
                else newKeywords.put(currentword, (Double) myKeyWords.get(currentword));
            }
            myKeyWords = newKeywords;
        }
    }
    
    /**
     * Writes a summary to System.out
     */
    public void print() {
        print(System.out);
    }
    
    /**
     * Writes a summary to the specified PrintWriter
     *
     * @param ps  PrintWriter where to send the output to
     */
    public void print(PrintStream ps) {
        ps.println(" Document Name : " + myFileName);
        Iterator it = myKeyWords.keySet().iterator();
        Word currentword;
        while (it.hasNext()) {
            currentword = (Word) it.next();
            ps.println("  " + currentword.getWord() + " : " + ((Double) myKeyWords.get(currentword)).doubleValue());
        }
    }
}
