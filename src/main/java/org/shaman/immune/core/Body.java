/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *              Artificial Immune Systems                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.immune.core;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.LearnerDataModels;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


// *********************************************************\
// *               Artificial Immune System                *
// *********************************************************/
public class Body extends ClassifierTransformation implements Classifier, Cloneable
{
    // *********************************************************\
    // *                        Constants                      *
    // *********************************************************/
    // Different Kinds of Matching Rules
    static public final int MATCH_CONTIGUOUS       = 0;    // r contiguous symbol matching rule
    static public final int MATCH_HAMMING          = 1;    // r hamming distance matching
    
    // Diffent Kinds of Representations
    static public final int DATA_FUZZY       = 0;    // Fuzzy internal data representation
    static public final int DATA_BIT         = 1;    // Bit string internal data representation
    
    // Detector generation algorithms
    static public final int DETECTOR_RANDOM  = 0;
    static public final int DETECTOR_TABULAR = 1;
    static public final int DETECTOR_GREEDY  = 2;
    
    // *********************************************************\
    // *                          Data                         *
    // *********************************************************/
    // Training Parameters
    protected int     detectorAlgorithm;      // Type of detector generation algorithm
    protected boolean numDetGuess;            // If TRUE then guess the number of detectors (usgin Pf)
    protected double  numDetPf;               // the desired detection probability
    protected int     numDet;                 // If the guess is FALSE then generate this # of detectors
    
    // Match rule parameters
    protected int        matchRule;                 // Type of match rule
    protected int        matchLength;               // Size of a match
    protected Morphology morpho;                    // The structure of the self/antigen/detector space
    
    // Structure of the Immune Space
    protected int         dataRepresentation;     // Type of internal data representation
    protected boolean     crisp;                  // Crisp fuzzy?
    
    // Self Particle Set
    private Self     []sel;                     // Self set (maxSelf self particles)
    private int      numSelf;                   // Number of active self particles
    
    // Non-self Detection algorithm
    private DetectorSet detset;                 // The set of detectors
    
    // Persistence Options
    private boolean     storeSelf;
    private boolean     storeDetectorSet;
    
    // **********************************************************\
    // *             AIS Classifier Implementation              *
    // **********************************************************/
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        Antigen agen;
        int     mp;
        int     cout;
        
        try
        {
            agen = createAntigen(morpho.getParticleLength());
            agen.compile(morpho, instance);
            
            mp = matchDetectors(agen);
            if (mp == -1) cout = 0;
            else          cout = 1;
            
            if (confidence != null) confidence[cout] = 1.0;
        }
        catch(AISException ex) { throw new LearnerException(ex); }
        
        return(cout);
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Cannot handle non-primitive data");
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModel dmsup;
        
        dmsup = getSupplierDataModel(0);
        init(dmsup);
    }
    
    public void init(DataModel dmsup) throws ConfigException
    {
        DataModelDouble dmin, dmgo, dmout;
        AttributeDouble atgo;
        
        // Make sure the input is compatible with this transformation's data requirements
        checkDataModelFit(0, dmsup);
        dmin = (DataModelDouble)dmsup;
        
        // Make a Self/Non-Self Classifier's DataModel
        dmgo  = new DataModelDouble("", 1);
        atgo  = new AttributeDouble("self/non-self");
        atgo.initAsSymbolCategorical(new double[]{0.0,1.0});
        atgo.setValuesAsGoal();
        atgo.setIsActive(true);
        dmgo.setAttribute(0, atgo);
        dmgo.getLearningProperty().setGoal("self/non-self");
        dmout = (DataModelDouble)LearnerDataModels.getClassifierDataModel(dmgo, Classifier.OUT_CLASS);
        
        // Set and create the DataModels
        setInputDataModel(0,dmin);
        setOutputDataModel(0,dmout);
        this.dataModel = dmin;
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        Attribute        []actatt;
        int              i;
        Attribute        attnow;
        DataModelDouble  dmin;
        
        // Check the Primitiveness
        if (!dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new DataModelException("Cannot operate on non-primitive data... Please convert.");
        this.primitive = true;
        dmin   = (DataModelDouble)dataModel;
        actind = dmin.getActiveIndices();
        actatt = (Attribute [])dmin.getActiveAttributes();
        
        // Check if a classification goal is present
        if (dmin.getLearningProperty().getHasGoal())
        {
            // Check the goal type
            attnow = dmin.getAttribute(dmin.getLearningProperty().getGoalIndex());
            if (attnow.getGoalType() != Attribute.GOAL_CLASS) 
                throw new DataModelException("DataModel with a classification goal required.");
        }
        else ; // Everything is considered SELF
        
        // Check if the input is fuzzy when needed
        if (dataRepresentation == Body.DATA_FUZZY)
        {
            if (!this.crisp)
            {
                for (i=0; i<actatt.length; i++)
                {
                    if (!actatt[i].hasProperty(AttributePropertyFuzzy.PROPERTY_FUZZY))
                        throw new DataModelException("DataModel is not fuzzy while the Body uses Fuzzy AIS Algorithms.");
                    else
                    {
                        AttributePropertyFuzzy propfuz = (AttributePropertyFuzzy)actatt[i].getProperty(AttributePropertyFuzzy.PROPERTY_FUZZY);
                    }
                }
            }
            else
            {
                for (i=0; i<actatt.length; i++)
                {
                    if (!actatt[i].hasProperty(Attribute.PROPERTY_CATEGORICAL))
                        throw new DataModelException("Attribute '"+actatt[i].getName()+"' is not categorical.");
                }
            }
        }
        else
        {
            // Check attribute properties
            for (i=0; i<actatt.length; i++)
            {
                if (!actatt[i].hasProperty(Attribute.PROPERTY_CATEGORICAL))
                    throw new DataModelException("Categorical input data expected. Attribute '"+actatt[i].getName()+"' is not categorical.");
            }
        }
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void train() throws LearnerException
    {
        compileSelfSet(trainData);
        generateDetectors();
    }
    
    public void initializeTraining() throws LearnerException
    {
        
    }
    
    public Presenter getTrainSet()
    {
        return(trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        this.trainData = _trainData;
        //dataModel = trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *        Strucured Object Persistence Definition        *
    // *********************************************************/
    public void setPersistenceOptions(boolean _storeSelf, boolean _storeDetectorSet)
    {
        storeSelf        = _storeSelf;
        storeDetectorSet = _storeDetectorSet;
    }
    
    public boolean getStoreSelf()        { return(storeSelf); }
    public boolean getStoreDetectorSet() { return(storeDetectorSet); }
    
    // *********************************************************\
    // *                       Body Cloning                    *
    // *********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        Body bod;
        
        bod                    = new Body();
        bod.name               = name;
        
        // Persistence Options
        bod.storeSelf          = storeSelf;
        bod.storeDetectorSet   = storeDetectorSet;
        
        // Match Rule
        bod.matchRule          = matchRule;
        bod.matchLength        = matchLength;
        
        // Immune Space
        bod.dataRepresentation = dataRepresentation;
        bod.morpho             = (Morphology)morpho.clone();
        
        // Self Set?
        if (storeSelf)
        {
            bod.sel     = sel; // (Self [])sel.clone();
            bod.numSelf = numSelf;
        }
        else { bod.sel = null; bod.numSelf = 0; }
        
        // Detectors
        bod.detectorAlgorithm = detectorAlgorithm;
        if (storeDetectorSet)
        {
            bod.detset = detset; // (DetectorSet)detset.clone();
        }
        
        return(bod);
    }
    
    // ********************************************************\
    // *               Representational Abstraction            *
    // *********************************************************/
    public Detector createDetector() { return(createDetector(0)); }
    public Detector createDetector(int len)
    {
        Detector detnew;
        
        if (dataRepresentation == DATA_FUZZY) detnew = new DetectorFuzzy(this);
        else                                  detnew = new DetectorBit(this);
        
        if (len > 0) detnew.init(len, this);
        
        return(detnew);
    }
    
    public Antigen createAntigen() { return(createAntigen(0)); }
    public Antigen createAntigen(int len)
    {
        Antigen agnew;
        
        if (dataRepresentation == DATA_FUZZY) agnew = new AntigenFuzzy(this);
        else                                  agnew = new AntigenBit(this);
        
        if (len > 0) agnew.init(len, this);
        
        return(agnew);
    }
    
    public Self createSelf() { return(createSelf(0)); }
    public Self createSelf(int len)
    {
        Self selnew;
        
        if (dataRepresentation == DATA_FUZZY) selnew = new SelfFuzzy(this);
        else                                  selnew = new SelfBit(this);
        
        if (len > 0) selnew.init(len, this);
        
        return(selnew);
    }
    
    // *********************************************************\
    // *   Compile the self-set particles from the datasource  *
    // *********************************************************/
    public void compileSelfSet(Presenter selfDataSet) throws LearnerException
    {
        int              i,j,nor;
        int              sellen;
        CachingPresenter imself;
        
        // Determine the field order
        morpho.makeFieldPositions();
        sellen = morpho.getParticleLength();
        
        imself = (CachingPresenter)selfDataSet;
        if (this.dataModel.getLearningProperty().getHasGoal()) // If goal is defined
        {
            // Mark the records that conform to the goal condition as self.
            nor    = 0;
            for (i=0; i<imself.getNumberOfInstances(); i++)
            {
                if (imself.getGoalClass(i) == 1)   imself.setWeight(i, 0.0);
                else                             { imself.setWeight(i, 1.0); nor++; }
            }
        }
        else
        {
            // Mark Everything as Self
            nor = imself.getNumberOfInstances();
            for (i=0; i<nor; i++) imself.setWeight(i, 1.0);
        }
        
        // Commit set-size values to the body
        numSelf  = nor;
        
        // Construct the particles
        j   = 0;
        sel = new Self[nor];
        for (i=0; i<imself.getNumberOfInstances(); i++)
        {
            // If the record is active and self
            if (imself.getWeight(i) > 0.0)
            {
                // Fill the Self particle with the data in the right representation (fuzzy/bit, MHC, ...)
                sel[j] = createSelf(sellen);
                sel[j].compile(morpho, imself.getInstance(i));
                j++;
            }
        }
        
        System.out.println("Compiled Self Set of "+getNumberOfSelfs()+" particles.");
    }
    
    // *********************************************************\
    // *         Match to Self-Set or Non-self Detectors       *
    // *********************************************************/
    /*
     public int matchSelf(Particle par) throws AISException
     {
     int     i;
     double  mnow;
     boolean match;
     int     mind;
     
     match = false; mind = -1; mnow = 0;
     for (i=0; (i<sel.length) && (!match); i++)
     {
     if (sel[i] != null)
     {
     mnow = par.match(sel[i], matchLength);
     if (mnow >= matchLength) { match = true; mind = i; }
     }
     }
     
     return(mind);
     }*/
    
    public int matchSelf(Detector det) throws AISException
    {
        int     i;
        double  mnow;
        boolean match;
        int     mind;
        
        match = false; mind = -1;
        for (i=0; (i<sel.length) && (!match); i++)
        {
            if (sel[i] != null)
            {
                mnow = sel[i].match(det, matchLength);
                if (mnow >= matchLength) { match = true; mind = i; }
            }
        }
        
        return(mind);
    }
    
    public int matchDetectors(Particle par) throws AISException
    {
        int match;
        
        match = -1;
        if (detset != null) match = detset.match(par);
        
        return(match);
    }
    
    
    // *********************************************************\
    // *         Non-Self Detectors Generation                 *
    // *********************************************************/
    public void generateDetectors() throws AISException
    {
        // Do some sanity checking on the parameters.
        if (matchLength > morpho.getParticleLength()) matchLength = morpho.getParticleLength();
        
        // Only fuzzy contiguous matching is supported right now...
        if (matchRule == MATCH_CONTIGUOUS)
        {
            // Create a new detector set with the correct algorithms
            if      (detectorAlgorithm == DETECTOR_RANDOM)  detset = new DetectorSetRandom(this);
            else if (detectorAlgorithm == DETECTOR_TABULAR) detset = new DetectorSetTabular(this);
            //else if (detectorAlgorithm == DETECTOR_GREEDY)  detset = new DetectorSetGreedy(this);
            if (numDetGuess) detset.generate(numDetPf, numDet);
            else             detset.generate(numDet);
            
            System.out.println("**** Generated "+detset.numDet+" detectors");
        }
    }
    
    // *********************************************************\
    // *               Initialization & Cleanup                *
    // *********************************************************/
    public Body()
    {
        name              = "body";
        
        matchRule          = MATCH_CONTIGUOUS;
        dataRepresentation = DATA_FUZZY;
        detectorAlgorithm  = DETECTOR_RANDOM;
        //immuneResponse     = ImmuneResponse.IMMUNE_RESPONSE_DETECT;
        
        matchLength        = 1;
        morpho             = new Morphology(this);
        
        storeSelf        = false;
        storeDetectorSet = false;
    }
    
    public void clearBody()
    {
        sel     = null;
        numSelf = 0;
        detset  = null;
        //env     = null;
        //immres  = null;
    }
    
    
    // *********************************************************\
    // *                  API Implementation                   *
    // *********************************************************/
    
    //                 Self Definition
    // ********************************************************/
    public Self []getSelfs()       { return(sel); }
    public Self getSelf(int i)     { return(sel[i]); }
    public int getNumberOfSelfs()  { return(numSelf); }
    
    //                  Memory Particles
    // ********************************************************/
    
    
    //                  Particle Morphology
    //********************************************************/
    public DataModel getDataModel()
    {
        return(this.dataModel);
    }
    
    public Morphology getMorphology()
    {
        return(morpho);
    }
    
    public void setMorphology(Morphology _morpho)
    {
        morpho = _morpho;
    }
    
    //             Matching and Detector Generation
    //********************************************************
    public void setDetectorParameters(boolean _numDetGuess, double _numDetPf, int _numDet)
    {
        numDetGuess = _numDetGuess;
        numDetPf    = _numDetPf;
        numDet      = _numDet;
    }
    
    public void setMatchRule(int rule) throws AISException
    {
        if (rule == MATCH_CONTIGUOUS)
        {
            matchRule = rule;
        }
        else throw new AISException("Only contiguous matching is supported.");
    }
    
    public int getMatchRule() { return(matchRule); }
    
    public void setDataRepresentation(int _data)
    {
        dataRepresentation = _data;
    }
    
    public int getDataRepresentation() { return(dataRepresentation); }
    
    public boolean getCrisp() { return(this.crisp); }
    
    public void setCrisp(boolean crisp) { this.crisp = crisp; }
    
    public void setMatchLength(int _ml)
    {
        matchLength = _ml;
    }
    
    public int getMatchLength() { return(matchLength); }
    
    public void setMatchParameters(int rule, int ml) throws AISException
    {
        setMatchRule(rule);
        setMatchLength(ml);
    }
    
    public void setDetectorAlgorithm(int _detectorAlgorithm) {  detectorAlgorithm = _detectorAlgorithm; }
    public int  getDetectorAlgorithm()                       { return(detectorAlgorithm); }
    
    public DetectorSet getDetectorSet()                    { return(detset); }
    public void        setDetectorSet(DetectorSet _detset) { detset = _detset; }
    
    public void setDataModel(DataModel dm)
    {
        this.dataModel = dm;
    }
}
