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

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Document Repository</h2>
 * Used by the Extractor and TopicMap classes.
 */

public class Repository implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Map of documents in this repository
     */
    public Map myDocuments;

    /**
     * Constructor for the Repository object.
     */
    public Repository() {
        myDocuments = new HashMap();
    }

    /**
     * Calculates the entropy of a given Word against this repository
     *
     * @param word  Word object to be evaluated
     * @return      the entropy value
     */
    public double getEntropy(Word word) {
        double returndouble = getEntropy(word.getOccurences());
        return returndouble;
    }

    /**
     * Calculates the entropy of a given Stem against this repository
     *
     * @param stem  Topic object to be evaluated
     * @return      the entropy value
     */
    public double getEntropy(Stem stem) {
        double returndouble = getEntropy(stem.getOccurences());
        System.out.println(stem.myName + " " + returndouble);

        return returndouble;
    }


    /**
     * Calculates the entropy of a given occurence map
     *
     * @param occurences  occurence map (Document -- nrOfOccurences)-pairs
     * @return            the entropy value
     */
    public double getEntropy(Map occurences) {
        Set intersection = occurences.keySet();
        intersection.retainAll(getDocuments());
        Iterator it = intersection.iterator();
        Document currentdoc;
        double N_Total = 0.0;
        double currentvalue;
        double SumN_iLogN_i = 0.0;
        while (it.hasNext()) {
            currentdoc = (Document) it.next();
            currentvalue = ((Integer) occurences.get(currentdoc)).doubleValue();
            N_Total += currentvalue;
            SumN_iLogN_i += currentvalue * Math.log(currentvalue);
        }

        return (Math.log(N_Total) - SumN_iLogN_i / N_Total);
    }

    /**
     * Calculates the log(IDF) for a given Word object against this repository
     *
     * @param word  Word object
     * @return      the log(IDF) value
     */
    public double getLogIDF(Word word) {
        double returndouble = getLogIDF(word.getOccurences());
        return returndouble;
    }

    /**
     * Calculates the log(IDF) for a given Stem against this repository
     *
     * @param stem  Stem (as Topic)
     * @return      the log(IDF) value
     */
    public double getLogIDF(Stem stem) {
        double returndouble = getLogIDF(stem.getOccurences());
        return returndouble;
    }

    /**
     * Calculates the log(idf) of a given occurence map against this repository
     *
     * @param occurences  occurence map (Document -- nrOfOccurences)-pairs
     * @return            the entropy value
     */
    public double getLogIDF(Map occurences) {
        Set intersection = occurences.keySet();
        intersection.retainAll(getDocuments());
        return (Math.log(getDocuments().size() / intersection.size()));
    }

    /**
     * Returns the document object related to the specified filename
     *
     * @param filename  filename
     * @return          Document object
     */
    public Document getFile(String filename) {
        return (Document) myDocuments.get(filename);
    }

    /**
     * Returns a Set of Document objects present in this repository
     *
     * @return   Set of Documents
     */
    public Set getDocuments() {
        Set returnset = new HashSet();
        Iterator it = myDocuments.keySet().iterator();
        while (it.hasNext()) {
            returnset.add(myDocuments.get(it.next()));
        }

        return returnset;
    }

    /**
     * Evaluates a word as a keyword for the documents in this repository. This
     * method calls the Document::assertKeyWord
     *
     * @param word  Word object
     */
    public void evaluateWord(Word word) {
        Map occurences = word.getOccurences();
        Iterator it = occurences.keySet().iterator();
        double logidf = getLogIDF(occurences);
        Document currentdocument;
        while (it.hasNext()) {
            currentdocument = (Document) it.next();
            currentdocument.assertKeyWord(word, (logidf * ((Integer) occurences.get(currentdocument)).doubleValue()));
        }

    }

    /**
     * Writes a repository summary to System.out
     */
    public void print() {
        print(System.out);
    }


    /**
     * Writes a repository summary to a given PrintStream
     *
     * @param ps  PrintStream to write the output to
     */
    public void print(PrintStream ps) {
        ps.println("Document Repository index:");
        Iterator it = myDocuments.keySet().iterator();
        while (it.hasNext()) {
            ((Document) myDocuments.get(it.next())).print(ps);
            ps.println();
        }
    }


    /**
     * Adds a Document specified by its filename to the repository
     *
     * @param filename  the filename of the Document to be added
     */
    public void addFile(String filename) {
        Document doc = new Document(filename);
        addDocument(doc);
    }

    /**
     * Adds all files in a given directory to the repository
     *
     * @param dirname  directory name
     */
    public void addDirectory(String dirname) {
        File currentdir = new File(dirname);
        String[] filenames = currentdir.list();
        for (int i = 0; i < filenames.length; i++) {
            addFile(currentdir + "\\" + filenames[i]);
        }
    }

    /**
     * Adds a Document object to the repository
     *
     * @param doc  Document instance
     */
    public void addDocument(Document doc) {
        if (!myDocuments.containsKey(doc.getFileName())) {
            myDocuments.put(doc.getFileName(), doc);
        }
    }

    /**
     * Adds all the files related to the specified String to the repository. If
     * the String represents a directory, all the files in this directory will
     * be added. if the String represents a filename, only that file is added.
     *
     * @param name  description of Parameter
     */
    public void add(String name) {
        File currentfile = new File(name);
        if (currentfile.isDirectory()) {
            String[] filelist = currentfile.list();
            for (int i = 0; i < filelist.length; i++) {
                addFile(name + "\\" + filelist[i]);
            }
        }
        else {
            addFile(name);
        }
    }

    /**
     * The main program for the Repository class.
     *
     * @param args  the command line arguments
     */
    public static void main(String[] args) {
        //Repository repository1 = new Repository();
    }
}
