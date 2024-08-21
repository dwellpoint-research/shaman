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

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Topic Map</h2>
 * A Topic-Map
 */
public class TopicMap implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * List of stems present in the TopicMap.
     */
    public Map stemList = null;

    /**
     * Document repository from which this topicmap is generated
     */

    public Repository myRepository;

    /**
     * Constructor for the TopicMap object
     */
    public TopicMap() {
        stemList = new HashMap();
        myRepository = new Repository();
    }

    /**
     * Sets the repository of the TopicMap.
     *
     * @param repository  the new Repository
     */
    public void setRepository(Repository repository) {
        myRepository = repository;
    }


    /**
     * Gets the repository of the TopicMap.
     *
     * @return   the repository
     */
    public Repository getRepository() {
        return myRepository;
    }


    /**
     * returns the Topic instance with the given stem
     *
     * @param stemName  stem String
     * @return          Stem object
     */
    public Stem getTopic(String stemName) {
        return (Stem) stemList.get(stemName);
    }


    /**
     * checks if a Topic instance already exists for a given stem
     *
     * @param stemname  Stem for which presence has to be checked
     * @return          true if the stem is already present in the topicmap
     */
    public boolean isPresent(String stemname) {
        return stemList.containsKey(stemname);
    }


    /**
     * returns an Iterator over the different Stems present in the "TopicMap"
     *
     * @return   Iterator object containing all stems (as String)
     */

    public Iterator getTopics() {
        Iterator it = getStems();
        Set topicset = new HashSet();
        while (it.hasNext()) {
            topicset.add(getTopic((String) it.next()));
        }
        return topicset.iterator();
    }

    /**
     * returns an itterator over the stems present in the TopicMap
     *
     * @return   Iterator object containing all stems (as Topic)
     */
    public Iterator getStems() {
        return stemList.keySet().iterator();
    }


    /**
     * writes the entire TopicMap to System.out
     */
    public void print() {
        print(0.0, System.out);
    }

    /**
     * Writes the entire TopicMap to a given PrintStream
     *
     * @param ps  description of Parameter
     */
    public void print(PrintStream ps) {
        print(0.0, ps);
    }

    /**
     * Writes all Stems with nrOfOccurences > a given threshold
     *
     * @param threshold  minimum number of occurences
     */
    public void print(double threshold) {
        print(threshold, System.out);
    }

    /**
     * Writes all Stems with nrOfOccurences > a given threshold to a given
     * PrintStream
     *
     * @param threshold  minimum number of occurences
     * @param ps         PrintStream indicating where to write the output to
     */
    public void print(double threshold, PrintStream ps) {
        ps.println(" Topic Map summary: ");
        ps.println(" Nr of Stems: " + stemList.size());
        ps.println();
        Stem currenttopic;

        Iterator it = stemList.keySet().iterator();

        while (it.hasNext()) {
            currenttopic = getTopic((String) it.next());
            if (currenttopic.getTotalNrOfOccurences().doubleValue() > threshold) {
                currenttopic.print(ps);
            }
        }
        ps.println(" ");
    }

    /**
     * Writes all Stems with nrOfOccurences > a given threshold to a given
     * PrintStream in XML
     *
     * @param threshold  mimimum number of occurences
     * @param ps         PrintStream to write the output to
     */
    public void write2xml(double threshold, PrintStream ps) {
        Stem currenttopic;

        ps.println("<TopicMap>");
        ps.println("<NrOfStems>" + stemList.size() + "</NrOfStems>");

        Iterator it = stemList.keySet().iterator();
        while (it.hasNext()) {
            currenttopic = getTopic((String) it.next());
            if (currenttopic.getTotalNrOfOccurences().doubleValue() > threshold) {
                currenttopic.write2xml(ps);
            }
        }
        ps.println("</TopicMap>");
    }

    /**
     * Writes all Stems with nrOfOccurences > a given threshold to System.out in
     * XML
     *
     * @param threshold  description of Parameter
     */
    public void write2xml(double threshold) {
        write2xml(threshold, System.out);
    }

    /**
     * Writes all Stems to System.out in XML
     */
    public void write2xml() {
        write2xml(0.0, System.out);
    }

    /**
     * Adds an observation of a word with some stem occuring in some Document
     *
     * @param stemName  Stem of the observed word
     * @param word      observed word
     * @param doc       Document object in which the word was observed
     */
    public void addObservation(String stemName, String word, Document doc) {
        Stem currenttopic;
        if (!(stemName.length() < 3)) {
            if (isPresent(stemName)) {
                currenttopic = getTopic(stemName);
            }
            else {
                currenttopic = new Stem(stemName);
                stemList.put(stemName, currenttopic);

            }
            currenttopic.addObservation(word, doc);
            myRepository.addDocument(doc);
        }
    }

    /**
     * save the topicmap in binary format to a file
     *
     * @param filename  Filename
     */
    public void save(String filename) {
        System.out.println("saving file " + filename);
        try {
            FileOutputStream ofile = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(ofile);
            oos.writeObject(stemList);
            oos.flush();
            ofile.close();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * load the topicmap in binary format from a file
     *
     * @param filename  Filename
     */
    public void load(String filename) {
        System.out.println("loading file " + filename);
        try {
            FileInputStream ifile = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(ifile);
            stemList = (HashMap) ois.readObject();
            ifile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the TopicMap summary to a given file.
     *
     * @param filename                 Filename
     * @exception java.io.IOException
     */
    public void write2ascii(String filename) throws java.io.IOException {
        write2ascii(filename, 0.0);
    }

    /**
     * Writes all te stems with NrOfOccurences > threshold in XML to a given
     * file.
     *
     * @param filename                 name of the output file
     * @param threshold                minimum number of occurences
     * @exception java.io.IOException
     */
    public void write2xml(String filename, double threshold) throws java.io.IOException {
        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)));

        write2xml(threshold, ps);
        ps.flush();
        ps.close();
    }


    /**
     * Writes all te stems with NrOfOccurences > threshold in to a given file.
     *
     * @param filename                 description of Parameter
     * @param threshold                description of Parameter
     * @exception java.io.IOException
     */
    public void write2ascii(String filename, double threshold) throws java.io.IOException {
        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)));
        print(threshold, ps);
        ps.flush();
        ps.close();
    }

}
