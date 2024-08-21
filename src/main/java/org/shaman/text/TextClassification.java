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

import java.util.Hashtable;

import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Text Classifier</h2>
 * Classifies a String into a number of pre-defined subject categories.
 */

// *********************************************************\
// *                     Text Classifier                   *
// *********************************************************/
public class TextClassification extends ClassifierTransformation implements Classifier
{
    // Text Processing Paramters.
    // -------------------------
    private String stemmer;                // Name of the Stemming class
    private String []stopwords;            // Name of the Stop-Word classes
    private String []separators;           // Name of the Separator classes
    private int    maxKey;                 // Maximum number of keywords per Topic
    private double discThres;              // Discrimination threshold
    
    private Hashtable []keys;              // The topic-profiles
    
    // Text Categorization
    private Categorizer cat;
    private Extractor   ex;
    
    // Classification Buffers
    private double []pcl;                  // Confidence buffer
    private AttributeObject attgoal;       // Goal Attribute
    
    // **********************************************************\
    // *            Text Classification Learning                *
    // **********************************************************/
    /**
     * Train the Text Classification Model using the Categorizer class.
     * @throws LearnerException If not possible
     */
    public void train() throws LearnerException
    {
        // Use text-processing to train the Text Classification
        System.out.println("Training Text Classification");
        
        // Make the separarte Topic-Profiles
        cat.createTopics((CachingPresenter)trainData, attgoal);
        
        // Merge the Topic Profiles the Categorization data
        keys = cat.mergeTopics(maxKey, discThres);
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    /**
     * Set class names of the Text-Processing algorithms to use.
     * @param _stemmer  The Stemmer algorithm class to use
     * @param _stopWords  The StopList algortihm classes to use
     * @param _separators The ordered Separator classes to use
     */
    public void setTextProcessingComponents(String _stemmer, String []_stopwords, String []_separators)
    {
        stemmer    = _stemmer;
        stopwords  = _stopwords;
        separators = _separators;
    }
    
    /**
     * Set the parameter for the Text Classification training algorithm.
     * @param _maxKey The maximum number of key-words for every topic
     * @param _discThres The discriminating threshold for the keywords in the model
     */
    public void setTrainParameters(int _maxKey, double _discThres)
    {
        maxKey    = _maxKey;
        discThres = _discThres;
    }
    
    public void init() throws ConfigException
    {
        // Initialize classifier datamodels etc...
        super.init();
        
        // Make some helper variables
        this.attgoal = this.dmob.getAttributeObject(this.dataModel.getLearningProperty().getGoalIndex());
        this.pcl     = new double[this.attgoal.getNumberOfGoalClasses()];
        
        try
        {
            // Create the Text-Processing components
            create();
        }
        catch(LearnerException ex) { throw new ConfigException(ex); }
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Classified Text Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Text Input");
        else return(null);
    }
    
    // *********************************************************\
    // *                  Text Classification                  *
    // *********************************************************/
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        int    i, numclass, cl;
        double max;
        String content;
        String []words;
        
        numclass = attgoal.getNumberOfGoalClasses();
        
        // Get the separate words from the text-document
        content = (String)instance.getQuick(0);
        words   = ex.extractWords(content);
        
        // Categorize the document into one of the topics.
        pcl = cat.categorize(words);
        
        // Find the most probable topic if enough data was available to classify.
        max = -1; cl = -1;
        if (this.pcl != null)
        {
            for (i=0; i<numclass; i++)
            {
                if (this.pcl[i] > max) { cl = i; max = pcl[i]; }
            }
            if (confidence != null)
            {
                for (i=0; i<this.pcl.length; i++) confidence[i] = this.pcl[i];
            }
        }
        
        if (max > -1) return(cl);
        else          return(-1);
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Cannot classify primitive data");
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        // Check if train set is the correct type.
        if (!(this.trainData instanceof CachingPresenter))
            throw new LearnerException("Text Classification needs a Caching Presenter to train on.");
        create();
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        this.trainData = _trainData;
        this.dataModel = this.trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *             Text Classification DataModel Fit         *
    // *********************************************************/
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        checkClassifierDataModelFit(dataModel, false, false, true);
    }
    
    public void create() throws LearnerException
    {
        // Create the Categorizer
        cat = new Categorizer();
        cat.setTopics((String[])attgoal.getLegalValues());
        cat.setCategorizationData(keys);
        
        try
        {
            // Create the Text processing components.
            int       i;
            Stemmer   stem;
            StopList  stopnow;
            Separator sepnow;
            
            ex = new Extractor();
            stem = (Stemmer)Class.forName(stemmer).newInstance();
            ex.setStemmer(stem);
            for (i=0; i<stopwords.length; i++)
            {
                stopnow = (StopList)Class.forName(stopwords[i]).newInstance();
                ex.addToStopWords(stopnow);
            }
            for (i=0; i<separators.length; i++)
            {
                sepnow = (Separator)Class.forName(separators[i]).newInstance();
                ex.addToSeparators(sepnow);
            }
            ex.setLimitKeywords(false, null);
            
            // Connect the two
            cat.setExtractor(ex);
        }
        catch(ClassNotFoundException ex) { throw new LearnerException(ex); }
        catch(IllegalAccessException ex) { throw new LearnerException(ex); }
        catch(InstantiationException ex) { throw new LearnerException(ex); }
    }
    
    public TextClassification()
    {
        super();
        name        = "Text Classification";
        description = "Text Categorization";
    }
}