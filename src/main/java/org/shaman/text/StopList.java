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

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * <h2>Stop-Word List Base Class</h2>
 */
public class StopList
{
    /**
     * table of stopwords.
     */
    public Hashtable myStopWords = null;

    /**
     * Constructor for the StopList object.
     */
    public StopList() {
        myStopWords = new Hashtable();
    }

    /**
     * Returns a boolean indicating whether or not the given word is a stopword
     *
     * @param word  String containing the input word
     * @return      true is the given word is a stopword
     */
    public boolean isStopWord(String word) {

        return myStopWords.containsKey(word);
    }

    /**
     * Adds a word to the list of stopwords
     *
     * @param stopword  Word to be added to the list
     */
    public void addStopWord(String stopword) {
        Double dummy = new Double(0);
        myStopWords.put(stopword, dummy);
    }

    /**
     * Removes a word from the list of stopwords
     *
     * @param stopword  Word to be removed from the list
     */
    public void removeStopWord(String stopword) {
        myStopWords.containsKey(stopword);
        myStopWords.remove(stopword);
    }

    /**
     * Writes the list of stopwords to System.out
     */
    public void print() {
        print(System.out);
    }

    /**
     * Writes the list of stopwords to a given PrintStream
     *
     * @param ps  PrintStream to write the stopwords to
     */
    public void print(PrintStream ps) {
        Iterator it = myStopWords.keySet().iterator();
        while (it.hasNext()) {
            ps.println((String) it.next());
        }

    }

}
