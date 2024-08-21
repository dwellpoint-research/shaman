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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Text Document Processing</h2>
 * Document processing class that supports Stemming, Stop-Word removal,
 * markup-removal (e.g. XML), sentence and word separation. Can be easily
 * extended to process other kinds of documents (e.g. Postscript, PDF).
 * Can extract a topic-map from a repository of documents.
 * Can find key-words of a document.
 */

// *********************************************************\
// *             A Knowledge from Text Extractor.          *
// *           Generates TopicMaps and/or Keywords         *
// *********************************************************/
public class Extractor
{
    // Text processing elements
    private TopicMap myTopicMap;                     // The topic map to be used
    private Stemmer  myStemmer;                      // The stemmer to be used
    private List     myStopWords;                    // The Stop Word Lists to be used
    private List     mySeparators;                   // The text separators applied before stop-list and stemming
    
    // Constrain the keywords to the most important ones
    private boolean   limitKeywords;                 // Limit all world the ones listed in the hashtable below
    private Hashtable keyList;                       // List of valid keywords
    
    // Document Repository Processing
    private String   topicName;                      // Name of the topic
    
    // *********************************************************\
    // *   Extract TopicProfile from the processed documents   *
    // *********************************************************/
    /**
     * Extract TopicProfile (ordered list of word/occurence frequency) from the current TopicMap.
     * @return The TopicProfile
     * @throws LearnerException If the topic profile cannot be made.
     */
    public TreeSet extractTopicProfile() throws LearnerException
    {
        // Create the Topic Profile
        TreeSet     woset;
        double      woval;
        WordCounter woco;
        Iterator    it, it2;
        Stem        currentstem;
        Word        currentword;
        Repository  myrep;
        double      nrofdocs;
        
        // Make Word/OccurenceFreq table
        myrep    = myTopicMap.getRepository();
        nrofdocs = (double)myrep.myDocuments.size();
        woset    = new TreeSet();
        it       = myTopicMap.getStems();
        while (it.hasNext())
        {
            currentstem = (Stem)(myTopicMap.stemList.get(it.next()));
            it2         = currentstem.wordCounter.keySet().iterator();
            while (it2.hasNext())
            {
                currentword = (Word)currentstem.wordCounter.get(it2.next());
                woval       = currentword.getNrOfOccurences() / nrofdocs;
                woco        = new WordCounter(currentword.getWord(), woval);
                woset.add(woco);
            }
        }
        
        return(woset);
    }
    
    
    /**
     * Specify if and to which set all words in the topic-map should be limited.
     * @param _limitKeywords <code>true</code> if the set of words should be limited to the given set.
     * @param _keyList The key-set is the set of words to which all words belong.
     */
    public void setLimitKeywords(boolean _limitKeywords, Hashtable _keyList)
    {
        limitKeywords = _limitKeywords;
        keyList       = _keyList;
    }
    
    /**
     * Extract a TopicMap from the document repository.
     * @param trainData All documents as Object instance set.
     * @param name The name of the current topic.
     * @throws LearnerException If something goes wrong.
     */
    public void extract(CachingPresenter trainData, String name) throws LearnerException
    {
        // Extract and Store the ResourceProfiles of the document
        // Create an Overall TopicProfile also
        Iterator    it, it2;
        Stem        currentstem;
        Word        currentword;
        Repository  myrepository;
        
        topicName = name;
        
        // Make a TopicMap extractor for English HTML documents
        myTopicMap = new TopicMap();
        
        // Extract TopicMap from the documents
        process(trainData);
        
        // Evaluate all words in the topicmap. Make keywords and all...
        it           = myTopicMap.getStems();
        myrepository = myTopicMap.getRepository();
        while (it.hasNext())
        {
            currentstem = (Stem)(myTopicMap.stemList.get(it.next()));
            myrepository.getLogIDF(currentstem);
            it2         = currentstem.wordCounter.keySet().iterator();
            while (it2.hasNext())
            {
                currentword = (Word)currentstem.wordCounter.get(it2.next());
                myrepository.evaluateWord(currentword);
            }
        }
    }
    
    
    // *********************************************************\
    // * Try extracting the Keywords from just a list of words *
    // *********************************************************/
    /**
     * Extract 'keysize' keywords based on the inverse document freaquency.
     * @param allwords The list of words
     * @param keysize The number of keywords to return
     * @return The keywords.
     */
    public String []extractKeywords(ArrayList allwords, int keysize)
    {
        int         i;
        String      []keys;
        Vector      wordco;   // Vector of WordCounter objects for each unique word in 'allWords'
        Iterator    itwords;
        String      wordnow;
        boolean     found;
        WordCounter woco;
        
        // Make a 'word/number of occurences' list
        wordco  = new Vector();
        itwords = allwords.iterator();
        while(itwords.hasNext())
        {
            wordnow = ((String)itwords.next()).toLowerCase();
            found = false; woco = null;
            for (i=0; (i<wordco.size()) && (!found); i++)
            {
                woco  = (WordCounter)wordco.elementAt(i);
                found = woco.word.equals(wordnow);
            }
            if (found) woco.increaseCounter();
            else
            {
                woco = new WordCounter(wordnow);
                wordco.add(woco);
            }
        }
        
        // Order the occurence list
        Collections.sort(wordco);
        
        // Derive a list of keywords
        if (wordco.size() < keysize) keysize = wordco.size();
        keys = new String[keysize];
        for (i=0; i<keysize; i++)
        {
            woco = (WordCounter)wordco.elementAt(i);
            keys[i] = woco.word;
        }
        
        return(keys);
    }
    
    // *********************************************************\
    // *  Separate and Filter Stop-Words for 1 HTML Document   *
    // *********************************************************/
    /**
     * Perform all text-processing algorithm on a HTML document.
     * Try to extract the Dublin-Core meta-data.
     * @param currentdoc The document
     * @return The list with words.
     */
    public ArrayList processOnline(Document currentdoc)
    {
        String    currenttoken;
        StopList  currentsw;
        Separator currentseparator;
        List      []currentlist = new ArrayList[1];
        ArrayList endlist = new ArrayList();
        String    contents;
        
        // Get meta-data from document
        contents = currentdoc.getContents();
        try
        {
            currentdoc.setDublinCore(DCdot.processPage(contents));
        }
        catch(java.io.IOException ex) { ex.printStackTrace(); }
        
        // Separate the document
        currentlist[0] = new ArrayList();
        currentlist[0].add(contents);
        Iterator itStopList;
        Iterator itSep;
        Iterator itString;
        
        itSep = mySeparators.iterator();
        while (itSep.hasNext())
        {
            currentseparator = (Separator)itSep.next();
            currentlist = currentseparator.separate(currentlist);
        }
        
        // Apply stopwords
        itString = currentlist[0].iterator();
        while (itString.hasNext())
        {
            itStopList   = myStopWords.iterator();
            currenttoken = (String) itString.next();
            
            boolean stopword = false;
            while (itStopList.hasNext())
            {
                currentsw = (StopList) itStopList.next();
                stopword = stopword || currentsw.isStopWord(currenttoken);
            }
            if (!stopword)
            {
                endlist.add(currenttoken);
            }
        }
        
        // All words that are not stopwords
        currentlist[0] = endlist;
        
        return(endlist);
    }
    
    // *********************************************************\
    // *       Process an entire repository of documents       *
    // *********************************************************/
    /**
     * processes the specified Repository
     *
     * @param currentrepository  Document Repository
     */
    public void process(Repository currentrepository)
    {
        Iterator it = currentrepository.getDocuments().iterator();
        Document currentdoc;
        while (it.hasNext()) {
            currentdoc = (Document) it.next();
            processDocument(currentdoc);
        }
    }
    
    
    // *********************************************************\
    // *    Extract the words from the given content string    *
    // *********************************************************/
    /**
     * Extract the words of the given contents string using the text-processing components.
     * @param contents The entire contents of a text document as String.
     * @return The words of the document.
     */
    public String []extractWords(String contents)
    {
        // Process the document
        String     currenttoken;
        StopList   currentsw;
        Separator  currentseparator;
        List[]     currentlist;
        Iterator   itStopList;
        Iterator   itSep;
        Iterator   itString;
        LinkedList llwords;
        boolean    stopword, wordpresent;
        
        llwords = new LinkedList();
        
        // Start with the entire document
        currentlist    = new ArrayList[1];
        currentlist[0] = new ArrayList();
        currentlist[0].add(contents);
        
        // Apply to separators (in the specified order)
        itSep = mySeparators.iterator();
        while (itSep.hasNext())
        {
            currentseparator = (Separator)itSep.next();
            currentlist = currentseparator.separate(currentlist);
        }
        
        // Remove the Words from the StopLists
        itString = currentlist[0].iterator();
        while (itString.hasNext())
        {
            itStopList   = myStopWords.iterator();
            currenttoken = (String) itString.next();
            currenttoken = currenttoken.toLowerCase();
            stopword    = false;
            while (itStopList.hasNext())
            {
                currentsw = (StopList) itStopList.next();
                stopword = stopword || currentsw.isStopWord(currenttoken);
            }
            if (!stopword)
            {
                if (limitKeywords) wordpresent = (keyList.get(currenttoken) != null);
                else               wordpresent = true;
                if (wordpresent)
                {
                    llwords.addLast(currenttoken);
                }
            }
        }
        
        // Make the output string array
        String []sout = (String [])llwords.toArray(new String[]{});
        
        return(sout);
    }
    
    // *********************************************************\
    // *  Process and update topicmap with the given document  *
    // *********************************************************/
    /**
     * Processes the specified Document instance using the text-processing algorithms.
     * @param currentdoc  Document instance
     */
    public void processDocument(Document currentdoc)
    {
        // Start new document analysis
        String contents = currentdoc.getContents();
        
        // Process the document
        String currenttoken;
        String currentstem;
        StopList currentsw;
        Separator currentseparator;
        List[] currentlist = new ArrayList[1];
        currentlist[0] = new ArrayList();
        currentlist[0].add(contents);
        Iterator itStopList;
        Iterator itSep;
        Iterator itString;
        boolean  stopword, wordpresent;
        
        itSep = mySeparators.iterator();
        while (itSep.hasNext())
        {
            currentseparator = (Separator)itSep.next();
            currentlist = currentseparator.separate(currentlist);
        }
        
        itString = currentlist[0].iterator();
        while (itString.hasNext())
        {
            itStopList   = myStopWords.iterator();
            currenttoken = (String) itString.next();
            currenttoken = currenttoken.toLowerCase();
            
            currentstem = myStemmer.stem(currenttoken);
            stopword    = false;
            while (itStopList.hasNext())
            {
                currentsw = (StopList) itStopList.next();
                stopword = stopword || currentsw.isStopWord(currenttoken);
            }
            if (!stopword)
            {
                if (limitKeywords) wordpresent = (keyList.get(currenttoken) != null);
                else               wordpresent = true;
                
                //System.err.println(currenttoken);
                
                if (wordpresent)
                    myTopicMap.addObservation(currentstem, currenttoken, currentdoc);
            }
        }
    }
    
    /**
     * Process the Documents with positive weights in the data.
     * @param trainData The repository
     */
    public void process(CachingPresenter trainData) throws LearnerException
    {
        int            i, numins;
        ObjectMatrix1D innow;
        Document       docnow;
        
        numins = trainData.getNumberOfInstances();
        for (i=0; i<numins; i++)
        {
            if (trainData.getWeight(i) == 1.0)
            {
                // First Element of the Object matrix is the Document String.
                innow  = trainData.getObjectInstance(i);
                docnow = new Document("document"+i, "url");
                docnow.setContents((String)innow.get(0));
                processDocument(docnow);
            }
        }
    }
    
    
    /**
     * Resets the internal topic map
     */
    public void reset() {
        myTopicMap = new TopicMap();
    }
    
    /**
     * Is the given a stop word?
     *
     * @param word  description of Parameter
     * @return      the stopWord value
     */
    public boolean isStopWord(String word)
    {
        boolean returnboolean = false;
        Iterator it;
        it = myStopWords.iterator();
        while (it.hasNext()) {
            returnboolean |= ((StopList) it.next()).isStopWord(word);
        }
        return returnboolean;
    }
    
    
    // *********************************************************\
    // *           Constructors and Initialization             *
    // *********************************************************/
    /**
     * Constructor for the Extractor object.
     */
    public Extractor()
    {
    }
    
    
    /**
     * Sets the topicMap attribute of the Extractor object.
     *
     * @param topicmap  the new topicMap value
     */
    public void setTopicMap(TopicMap topicmap) {
        myTopicMap = topicmap;
    }
    
    /**
     * Sets the stemmer attribute of the Extractor object.
     *
     * @param stemmer  the new stemmer value
     */
    public void setStemmer(Stemmer stemmer) {
        myStemmer = stemmer;
    }
    
    /**
     * returns the TopicMap of the Extractor.
     *
     * @return   TopicMap of this extractor
     */
    public TopicMap getTopicMap() {
        return myTopicMap;
    }
    
    
    /**
     * returns the stemmer object of the Extractor.
     *
     * @return   the stemmer value
     */
    public Stemmer getStemmer() {
        return myStemmer;
    }
    
    /**
     * Adds a StopList instance to the Set of StopLists
     *
     * @param stoplist  StopList instance or some daughter
     */
    public void addToStopWords(StopList stoplist)
    {
        if (myStopWords == null) myStopWords = new ArrayList();
        myStopWords.add(stoplist);
    }
    
    public List getStopwords()
    {
        return myStopWords;
    }
    
    
    /**
     * Add a Sepator the ordered list of separators.
     * @param sep The new separator.
     */
    public void addToSeparators(Separator sep){
        if (mySeparators == null) mySeparators = new ArrayList();
        mySeparators.add(sep);
    }
    
    /**
     * Get the ordered list of separators.
     * @return The list of separators.
     */
    public List getSeparators()
    {
        return mySeparators;
    }
    
    /**
     * Removes a StopList instance from the Set of StopLists
     *
     * @param stoplist  StopList instance or some daughter
     */
    public void removeFromStopWords(StopList stoplist) {
        myStopWords.remove(stoplist);
    }
}

// *********************************************************\
// *   Little ordered class for counting word occurences   *
// *********************************************************/
/** Ordered utility class for counting word occurences */
class WordCounter implements Comparable, Serializable
{
    String word;       // The word
    double count;      // Number of times the word occured
    
    public int compareTo(Object ob2)
    {
        int res;
        
        if (ob2 instanceof WordCounter)
        {
            WordCounter wc2 = (WordCounter)ob2;
            
            if (wc2.count < count) res = -1;
            else                   res = 1;
        }
        else res = 0;
        
        return(res);
    }
    
    public void increaseCounter() { count++; }
    
    public String toString()
    {
        String out;
        
        out = word+"  "+count;
        
        return(out);
    }
    
    public WordCounter(String _word)
    {
        word  = _word;
        count = 1;
    }
    
    public WordCounter(String _word, double _count)
    {
        word  = _word;
        count = _count;
    }
}
