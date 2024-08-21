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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.shaman.datamodel.AttributeObject;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;


/**
 * <h2> 'Discriminating Keyword'-Based Text Document Classifier</h2>
 */
// *********************************************************\
// *  'Discriminating Keyword'-Based Document Categorizer  *
// *********************************************************/
public class Categorizer
{
    // *********************************************************\
    // *               Document Categorizer Data               *
    // *********************************************************/
    private CachingPresenter trainData;         // The Document Repository as 1-element Object vectors
    private AttributeObject  atgoal;            // The goal attribute
    
    private String  []topics;                   // The names of the topics
    private Hashtable []keys;                   // The Text Categorization Model
    
    // Working Buffers
    private TreeSet []topicProfiles;            // Topic Profiles
    private Extractor ex;                       // The topicmap/keywords/topicprofile extractor
    
    // *********************************************************\
    // * Create the TopicProfiles :  Text Classification Model *
    // *********************************************************/
    /**
     * Create, for all topics, a list of words that occur often.
     * @param _trainData The Document repository
     * @param _atgoal The categorical attribute containing the topics as legal values.
     * @throws LearnerException If the profiles cannot be built.
     */
    public void createTopics(CachingPresenter _trainData, AttributeObject _atgoal) throws LearnerException
    {
        int     i,j;
        
        trainData = _trainData;
        atgoal    = _atgoal;
        
        // Make the Topic Profiles.
        topics        = (String [])atgoal.getLegalValues();
        topicProfiles = new TreeSet[topics.length];
        for (i=0; i<topics.length; i++)
        {
            // Disable all but the documents of the current topic.
            for (j=0; j<topics.length; j++)
            {
                if (i != j) trainData.setWeightWhereGoalIs(topics[j], 0.0);
                else        trainData.setWeightWhereGoalIs(topics[j], 1.0);
            }
            // Create the Topic Profile for the current topic
            topicProfiles[i] = createTopic(trainData, topics[i]);
        }
    }
    
    private TreeSet createTopic(CachingPresenter trainData, String topic) throws LearnerException
    {
        TreeSet   tp;
        
        // Extract a TopicMap from the Document Corpus
        ex.extract(trainData, topic);
        
        // Extract a preliminary TopicProfile from the TopicMap
        tp = ex.extractTopicProfile();
        
        return(tp);
    }
    
    // *********************************************************\
    // *   Make Topic Membership Values for the Given Words    *
    // *********************************************************/
    /**
     * Guess, for all topics, how well the given array of words fits.
     * @param words An array of words. e.g. a cut-up document, from a keyword extractor.
     * @return An array with topic membership values.
     */
    public double []categorize(String []words)
    {
        int    i,j;
        double []con;
        int    []cou;
        Double woval;
        double val;
        double max;
        int    maxpos;
        int    nummatch;
        
        con = new double[topics.length];
        cou = new int[topics.length];
        
        nummatch = 0;
        for(i=0; i<words.length; i++)
        {
            // Get category with the highest match for this keyword
            max = 0; maxpos = -1;
            for (j=0; j<topics.length; j++)
            {
                woval = (Double)keys[j].get(words[i]);
                if (woval != null) val = woval.doubleValue();
                else               val = 0;
                
                if (val > max) { max = val; maxpos = j; }
            }
            
            // If the keyword matched somewhere, add the keyword categorization strength at that position
            if (max > 0)
            {
                con[maxpos] += max; cou[maxpos]++;
                nummatch++;
            }
        }
        
        // Check if there's enough information to categorize
        if (nummatch > 0)
        {
            // Find the category which matches best with the given keywords
            max = 0; maxpos = -1;
            for (i=0; i<topics.length; i++)  { if (con[i] > max) { max = con[i]; maxpos = i; }
            }
            
            // Normalize the categorization data on maximum value
            for (i=0; i<topics.length; i++)  { con[i] /= max; }
        }
        else con = null;
        
        return(con);
    }
    
    // *********************************************************\
    // *  Merge Topic Profiles into Text Classification Model  *
    // *********************************************************/
    /**
     * Merge the topic-profiles into 1 text-classification model.
     * Only keep the word (but not more than NUMKEYS per topic)
     * that are highly discrimination (> discThres) for the topics.
     * @param NUMKEYS Maximum number of keyword to remember for a topic.
     * @param discThres The discrimination threshold.
     * @return The Text-Categorization Model
     */
    public Hashtable []mergeTopics(int NUMKEYS, double discThres)
    {
        int               i,j,k, numrej;
        TreeSet           []woSet;
        Hashtable         []woDat;
        int               numtop;
        Iterator          it;
        WordCounter       woco;
        String            wonow;
        Double            frnow;
        double            []wofr;
        TreeSet           []woKey;
        double            frvar;
        double            frmax;
        double            frmean;
        
        numtop = topics.length;
        woSet  = topicProfiles;
        woDat  = new Hashtable[numtop];
        woKey  = new TreeSet[numtop];
        wofr   = new double[numtop];
        keys   = new Hashtable[numtop];
        
        // Put TopicProfiles (ordered word/frequency lists) into Hashtable
        for (i=0; i<topics.length; i++)
        {
            woDat[i] = new Hashtable();
            it       = woSet[i].iterator();
            while(it.hasNext())
            {
                woco = (WordCounter)it.next();
                woDat[i].put(woco.word, new Double(woco.count));
            }
        }
        
        // Retain the most important keywords per topic
        for (i=0; i<topics.length; i++)
        {
            woKey[i] = new TreeSet();
            keys[i]  = new Hashtable();
            it       = woSet[i].iterator();
            j        = 0;
            numrej   = 0;
            while ((j<NUMKEYS) && (it.hasNext()))
            {
                woco  = (WordCounter)it.next();
                wonow = woco.word;
                for (k=0; k<numtop; k++)
                {
                    frnow = (Double)woDat[k].get(wonow);
                    if (frnow == null) wofr[k] = 0;
                    else               wofr[k] = frnow.doubleValue();
                }
                
                // Calculate importance value from word frequency data
                // ---------------------------------------------------
                // Normalize the data
                frmax = 0; frmean = 0;
                for (k=0; k<numtop; k++)
                {
                    frmean += wofr[k];
                    if (wofr[k] > frmax) frmax = wofr[k];
                }
                frmean /= numtop;
                for (k=0; k<numtop; k++) wofr[k] /= frmax;
                frmean /= frmax;
                
                frvar   = 0;
                for (k=0; k<numtop; k++)
                {
                    if (k != i) frvar += Math.abs(wofr[i]-wofr[k]);
                }
                frvar /= numtop-1;
                
                // Only keep the ones who discriminate well between the topics.
                if (frvar > discThres)
                {
                    woKey[i].add(new WordCounter(wonow, frvar));
                    j++;
                }
                else numrej++;
            }
            
            // Create the categorization data for this topic.
            System.out.println("Found "+j+" keywords for "+topics[i]+" rejected "+numrej);
            
            it = woKey[i].iterator();
            for (k=0; k<j; k++)
            {
                woco = (WordCounter)it.next();
                keys[i].put(woco.word, new Double(woco.count));
            }
        }
        
        return(keys);
    }
    
    // *********************************************************\
    // *                      Data Access                      *
    // *********************************************************/
    /**
     * Set the list of topics.
     * @param _topics The array containing the topic names = legal values of the goal attribute.
     */
    public void setTopics(String []_topics)  { topics = _topics; }
    /**
     * Set the Text Classification Model data.
     * @param _keys The Text Classification Model
     */
    public void setCategorizationData(Hashtable []_keys) { keys = _keys; }
    /**
     * Set the main Text-Processing Component.
     * @param _ex The text-processing component.
     */
    public void setExtractor(Extractor _ex) { ex = _ex; }
    
    /**
     * Give the index of the topic with the given name.
     * @param name The name of the topic.
     * @return The index of the topic with the given name. -1 is not present.
     */
    public int giveCategoryIndex(String name)
    {
        int     i, ipos;
        boolean found;
        
        found = false; ipos = -1;
        for (i=0; (i<topics.length) && (!found); i++)
        {
            if (topics[i].equals(name)) { ipos = i; found = true; }
        }
        
        return(ipos);
    }
    
    /**
     * Return only those words that are known by the Text Classification model.
     * @param keys The list of words to filter.
     * @return The list of known words.
     */
    public String []filterKeywords(String []keys)
    {
        // Only retain the keywords that are known to the categorizer
        int    i;
        Vector keyvec;
        String []keyfil;
        
        keyvec = new Vector();
        for (i=0; i<keys.length; i++)
        {
            if (isKeywordKnown(keys[i])) keyvec.add(keys[i]);
        }
        keyfil = new String[keyvec.size()];
        keyvec.copyInto(keyfil);
        
        return(keyfil);
    }
    
    /**
     * Is the given word known by the text classification model?
     * @param word The word
     * @return <code>true</code> if the word is known.
     */
    public boolean isKeywordKnown(String word)
    {
        // Is this a known keyword in some category?
        int     i;
        boolean found;
        
        found = false;
        for (i=0; (i<keys.length) && (!found); i++)
        {
            if (keys[i].get(word) != null) found = true;
        }
        
        return(found);
    }
    
    
    public Categorizer()
    {
    }
}
