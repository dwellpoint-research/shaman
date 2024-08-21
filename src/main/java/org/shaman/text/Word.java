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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Word</h2>
 * Class containing a Word and some attributes (occurences, n-Grams, ...)
 */
public class Word implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * the word (as String)
     */
    public String myWord = null;

    /**
     * Occurence map containing (Document,nrOfOccurences)-pairs
     */
    public Map occurences = null;

    /**
     * the total number of occurences
     */
    public int totalNrOfOccurences = 0;

    /**
     * the length of the n-Grams
     */
    public int myNGramLength = 2;

    /**
     * Set of n-Grams
     */
    public Set myNGram = null;

    /**
     * Map of all the (String word, Word objects) in the application.
     */
    public static Map theWordList = new HashMap();

    /**
     * Constructor for the Word object.
     *
     * @param word  the word (as String)
     */
    public Word(String word) {
        this();
        setWord(word);
    }

    /**
     * Constructor for the Word object.
     */
    public Word() {
        occurences = new HashMap();
    }

    /**
     * Sets the word attribute of the Word object.
     *
     * @param word  the new word value
     */
    public void setWord(String word) {
        myWord = word;
        theWordList.put(word, this);

    }

    /**
     * Sets the length of the n-Grams
     *
     * @param length  new length of the n-Grams
     */
    public void setNGramLength(int length) {
        myNGramLength = length;
    }

    /**
     * Gets the total number of occurences for this word
     *
     * @return   the total number of occurences
     */
    public int getNrOfOccurences() {
        return totalNrOfOccurences;
    }

    /**
     * returns the word as String object
     *
     * @return   the word String
     */
    public String getWord() {
        return myWord;
    }

    /**
     * returns the Set of n-Grams for this word
     *
     * @return   the nGram value
     */
    public Set getNGram() {
        return extractNGram();
    }

    /**
     * returns the set of n-Grams for this word
     *
     * @param ngramlength  length of the n-Grams to bo returned
     * @return             Set containing the n-Grams
     */
    public Set getNGram(int ngramlength) {
        return extractNGram(ngramlength);
    }

    /**
     * returns the set of Document objects in which this word occurs
     *
     * @return   the Document Set
     */
    public Set getDocuments() {
        Set returnset = new HashSet(occurences.keySet());
        return returnset;
    }

    /**
     * Returns an occurence map or this
     *
     * @return   the occurences map
     */
    public Map getOccurences() {
        return occurences;
    }

    /**
     * Writes the word summary to a given PrintStream
     *
     * @param ps  PrintStream where to send the output to
     */
    public void print(PrintStream ps) {
        ps.println("Word Summary:");
        ps.println("Word: " + myWord);
        Iterator it = occurences.keySet().iterator();
        Document currentdoc;

        while (it.hasNext()) {
            currentdoc = (Document) it.next();
            ps.println("In " + currentdoc.getFileName() + " , " + ((Integer) occurences.get(currentdoc)).intValue() + " occurences");
        }
        ps.println();
    }


    /**
     * Writes the word summary in XML to the specified PrintStream
     *
     * @param ps  PrintStream where to send the output to
     */
    public void write2xml(PrintStream ps) {
        ps.println("<Word>");
        ps.println("<String>" + getWord() + "</String>");
        ps.println("<Count>" + getNrOfOccurences() + "</Count>");
        Iterator it = occurences.keySet().iterator();
        Document currentdoc;

        while (it.hasNext()) {
            currentdoc = (Document) it.next();
            ps.println("<Occurence>");
            ps.println("<Count>" + ((Integer) occurences.get(currentdoc)).intValue() + "</Count>");
            ps.println("<File>" + currentdoc.getFileName() + "</File>");
            ps.println("</Occurence>");
        }
        ps.println("</Word>");
    }

    /**
     * Writes the word summary in XML to System.out
     */
    public void write2xml() {
        write2xml(System.out);
    }


    /**
     * Writes the word summary to System.out
     */
    public void print() {
        print(System.out);
    }

    /**
     * adds an observation of the word in a specified document
     *
     * @param doc  The Document in whcih the word was observed
     */
    public void addObservation(Document doc) {
        Integer currentcount;

        if (occurences.containsKey(doc)) {
            currentcount = (Integer) occurences.get(doc);
            currentcount = new Integer(currentcount.intValue() + 1);
            occurences.put(doc, currentcount);
        }
        else
            occurences.put(doc, new Integer(1));
        totalNrOfOccurences++;
    }

    /**
     * returns a set of n-Grams for the word
     *
     * @param ngramlength  length of the n-Grams
     * @return             Set of n-Grams (as Strings)
     */
    public Set extractNGram(int ngramlength) {
        setNGramLength(ngramlength);
        return extractNGram();
    }

    /**
     * returns a set of n-Grams for the word
     *
     * @return   Set of n-Grams (as Stings)
     */
    public Set extractNGram() {
        int nrofngrams = myWord.length() - myNGramLength + 1;
        Set returnset = new HashSet();
        for (int i = 0; i < nrofngrams; i++) {
            returnset.add(myWord.toLowerCase().substring(i, i + myNGramLength));
        }
        myNGram = returnset;
        return returnset;
    }

    /**
     * Returns the word instance conforming to the specified String
     *
     * @param wordname  name of the word to be retrieved
     * @return          Instance of Word
     */
    public static Word getWord(String wordname) {
        Word returnword;
        returnword = (Word) theWordList.get(wordname);
        return returnword;
    }

    /**
     * Gets the nrOfWords attribute of the Word class.
     *
     * @return   the nrOfWords value
     */
    public static int getNrOfWords() {
        if (theWordList != null)
            return theWordList.size();
        else
            return 0;
    }

    /**
     * Returns a boolean indicating whether or not a specified word already
     * exists
     *
     * @param wordname  description of Parameter
     * @return          description of the Returned Value
     */
    public static boolean hasWord(String wordname) {
        boolean returnboolean = theWordList.containsKey(wordname);
        return returnboolean;
    }
}
