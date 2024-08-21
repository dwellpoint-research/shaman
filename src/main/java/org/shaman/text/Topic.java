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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Topic</h2>
 * A Topic in a Topic-Map.
 */
public class Topic
{

    /**
     * The stem as a String
     */
    public String myName;

    /**
     * The total number of occurences
     */
    public int totalNrOfOccurences = 0;

    /**
     * Map containing the words derived from this stem (as String) and a
     * reference to the Word object
     */
    public Map wordCounter;

    /**
     * List of n-Grams contained in this stem
     */
    public List myNGrams = null;

    /**
     * Length of the n-Grams (default = 2)
     */
    public int myNGramLength = 2;

    /**
     * Distance matrix of the additive and multiplicative similarities between
     * all words derived from this stem
     */
    public double[][] myDistances = null;

    /**
     * Constructor for the Topic object.
     */
    public Topic() {
        myName = new String();
        totalNrOfOccurences = 0;
        wordCounter = new HashMap();
    }

    /**
     * Constructor for the Topic object.
     *
     * @param stemName  String containing the stem
     */
    public Topic(String stemName) {
        myName = new String(stemName);
        totalNrOfOccurences = 0;
        wordCounter = new HashMap();
    }

    /**
     * Sets the length of the n-Grams to be derived
     *
     * @param length  new length
     */
    public void setNGramLength(int length) {
        myNGramLength = length;
    }

    /**
     * Gets the current length of the n-Grams
     *
     * @return   Length of the n-Grams
     */
    public int getNGramLenth() {
        return myNGramLength;
    }


    /**
     * returns the totalNrOfOccurences of this stem
     *
     * @return   The totalNrOfOccurences
     */
    public Integer getTotalNrOfOccurences() {
        return new Integer(totalNrOfOccurences);
    }

    /**
     * returns an occurence Map containing (Document, nrOfOccurences)-pairs
     *
     * @return   the occurence Map
     */
    public Map getOccurences() {
        Map returnmap = new HashMap();
        Iterator it = wordCounter.values().iterator();
        Word currentword;
        Document currentdocument;
        int counter;

        while (it.hasNext()) {
            currentword = (Word) it.next();
            Iterator it2 = currentword.getDocuments().iterator();
            while (it2.hasNext()) {
                currentdocument = (Document) it2.next();
                if (returnmap.containsKey(currentdocument)) {
                    counter = ((Integer) returnmap.get(currentdocument)).intValue();
                }
                else {
                    counter = 0;
                }
                counter += ((Integer) currentword.getOccurences().get(currentdocument)).intValue();
                returnmap.put(currentdocument, new Integer(counter));
            }
        }
        return returnmap;
    }

    /**
     * Extracts the n-Grams
     */
    public void extractNGrams() {
        myNGrams = new ArrayList();
        Iterator it = wordCounter.keySet().iterator();
        String currentword;
        while (it.hasNext()) {
            currentword = (String) it.next();
            myNGrams.add(((Word) wordCounter.get(currentword)).getNGram(myNGramLength));
        }
    }

    /**
     * Constructs the distancematrix
     */
    public void constructMatrix() {
        if (myNGrams == null) {
            extractNGrams();
        }
        int l = wordCounter.size();
        myDistances = new double[l][l];
        Set intersection;
        Set s1;
        Set s2;
        for (int i = 0; i < l; i++) {
            myDistances[i][i] = 1;
            for (int j = i; j < l; j++) {
                s1 = (Set) myNGrams.get(i);
                s2 = (Set) myNGrams.get(j);
                intersection = new HashSet(s1);
                intersection.retainAll(s2);

                myDistances[i][j] = ((double) (2 * intersection.size())) / ((double) (s1.size() + s2.size()));

                myDistances[j][i] = ((double) (intersection.size() * intersection.size())) / ((double) (s1.size() * s2.size()));
            }

        }
    }

    /**
     * prints the distancematrix
     */
    public void printMatrix() {
        if (myDistances == null) {
            constructMatrix();
        }
        Iterator it = wordCounter.keySet().iterator();
        int l = wordCounter.size();
        int i = 0;

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        System.out.println(myName);
        while (it.hasNext()) {
            System.out.print("[\t");
            for (int j = 0; j < l; j++) {
                System.out.print(nf.format(myDistances[i][j]) + "\t");
            }
            System.out.println("]\t" + (String) it.next());
            i++;
        }
    }


    /**
     * adds an observation of a word (as String) that occured in some document
     *
     * @param word  the observed word
     * @param doc   the Document object in which the word was observed
     */
    public void addObservation(String word, Document doc) {
        Word currentword;
        if (wordCounter.containsKey(word)) {
            currentword = (Word) wordCounter.get(word);
        }
        else {
            currentword = new Word(word);
            wordCounter.put(word, currentword);
        }
        currentword.addObservation(doc);
        totalNrOfOccurences++;
    }


    /**
     * Writes a summary to a given PrintStream
     *
     * @param ps  PrintStream where to write the output to
     */
    public void print(PrintStream ps) {
        ps.println(" Topic Summary: ");
        ps.println(" Stem: " + myName);
        ps.println(" Nr of Occurences: " + totalNrOfOccurences);
        Iterator it = wordCounter.keySet().iterator();
        Word currentword;
        while (it.hasNext()) {
            currentword = (Word) wordCounter.get(it.next());
            currentword.print(ps);
        }
        ps.println();
    }

    /**
     * Writes a summary in XML to a given PrintStream
     *
     * @param ps  PrintStream where to write the output to
     */
    public void write2xml(PrintStream ps) {
        ps.println("<Stem>");
        ps.println("<String>" + myName + "</String>");
        ps.println("<Occurences>" + getTotalNrOfOccurences() + "</Occurences>");
        Iterator it = wordCounter.keySet().iterator();
        Word currentword;
        while (it.hasNext()) {
            currentword = (Word) wordCounter.get(it.next());
            currentword.write2xml(ps);
        }
        ps.println("</Stem>");
    }

    /**
     * Writes a summary in XML to System.out
     */
    public void write2xml() {
        write2xml(System.out);
    }


    /**
     * Writes a summary to System.out
     */
    public void print() {
        print(System.out);
    }

}
