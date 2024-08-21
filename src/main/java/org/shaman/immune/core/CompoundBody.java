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

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleMatrix1D;


// *********************************************************\
// *      Combined Body. Contains a number of Bodies       *
// *********************************************************/
public class CompoundBody extends Body
{
    private Body []bod;
    
    // **********************************************************\
    // *             AIS Classifier Implementation              *
    // **********************************************************/
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        int i, cout;
        int match;
        
        match = 0;
        for (i=0; (i<bod.length) && (match==0); i++)
        {
            match = bod[i].classify(instance);
        }
        
        if (match != 0) cout = 1;
        else            cout = 0;
        
        if (confidence != null) confidence[cout] = 1.0;
        
        return(cout);
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int             i;
        DataModel       dmsup;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        this.dataModel = dmsup;
        
        for (i=0; i<bod.length; i++) super.init(dmsup);
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void train() throws LearnerException
    {
        int i;
        
        for (i=0; i<bod.length; i++)
        {
            bod[i].setDataModel(this.dataModel);
            bod[i].train();
        } 
    }
    
    public void initializeTraining() throws LearnerException
    {
        
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        this.trainData = _trainData;
        for (int i=0; i<bod.length; i++) bod[i].setTrainSet(_trainData);
    }
    
    // *********************************************************\
    // *   Compile the self-set particles from the datasource  *
    // *********************************************************/
    public void compileSelfSet(Presenter selfDataSet) throws LearnerException
    {
        for (int i=0; i<bod.length; i++) compileSelfSet(selfDataSet);
    }
    
    // *********************************************************\
    // *         Match to Self-Set or Non-self Detectors       *
    // *********************************************************/
    public int matchSelf(Detector det) throws AISException
    {
        int i;
        int mind;
        
        mind = -1;
        for (i=0; (i<bod.length) && (mind == -1); i++) mind = bod[i].matchSelf(det);
        
        return(mind);
    }
    
    public int matchDetectors(Particle par) throws AISException
    {
        int i, match;
        
        match = -1;
        for (i=0; (i<bod.length) && (match == -1); i++) match = bod[i].matchDetectors(par);
        
        return(match);
    }
    
    
    // *********************************************************\
    // *         Non-Self Detectors Generation                 *
    // *********************************************************/
    public void generateDetectors() throws AISException
    {
        int      i;
        for (i=0; i<bod.length; i++) bod[i].generateDetectors();
    }
    
    // *********************************************************\
    // *               Initialization & Cleanup                *
    // *********************************************************/
    public CompoundBody()
    {
        super();
        name  = "coumpound body";
    }
    
    public void grow(int numbod)
    {
        super.grow(1,1);
        
        int i;
        this.bod = new Body[numbod];
        for (i=0; i<bod.length; i++) this.bod[i] = new Body(); 
    }
    
    
    // *********************************************************\
    // *                  API Implementation                   *
    // *********************************************************/
    
    //                  Particle Morphology
    //********************************************************/
    public DataModel getDataModel()
    {
        return(this.dataModel);
    }
    
    //             Matching and Detector Generation
    //********************************************************
    public void setMHC(int _mhc)
    {
        for(int i=0; i<bod.length; i++) bod[i].getMorphology().setMHC(_mhc);
    }
    
    public void setDetectorParameters(boolean _numDetGuess, double _numDetPf, int _numDet)
    {
        numDetGuess = _numDetGuess;
        numDetPf    = _numDetPf;
        numDet      = _numDet;
        
        // Every sub-body part of the work...
        int partdet = _numDet/bod.length;
        for(int i=0; i<bod.length; i++) bod[i].setDetectorParameters(_numDetGuess, _numDetPf, partdet);
    }
    
    public void setMatchRule(int rule) throws AISException
    {
        this.matchRule = rule;  
        for(int i=0; i<bod.length; i++) bod[i].setMatchRule(rule);
    }
    
    public int getMatchRule() { return(matchRule); }
    
    public void setDataRepresentation(int _data)
    {
        dataRepresentation = _data;
        for(int i=0; i<bod.length; i++) bod[i].setDataRepresentation(_data);
    }
    
    public int getDataRepresentation() { return(dataRepresentation); }
    
    public boolean getCrisp() { return(this.crisp); }
    
    public void setCrisp(boolean crisp)
    {
        this.crisp = crisp;
        for(int i=0; i<bod.length; i++) bod[i].setCrisp(crisp);
    }
    
    public void setMatchLength(int _ml)
    {
        matchLength = _ml;
        for(int i=0; i<bod.length; i++) bod[i].setMatchLength(_ml);
    }
    
    public int getMatchLength() { return(matchLength); }
    
    public void setMatchParameters(int rule, int ml) throws AISException
    {
        setMatchRule(rule);
        setMatchLength(ml);
        for(int i=0; i<bod.length; i++) bod[i].setMatchParameters(rule, ml);
    }
    
    public void setDetectorAlgorithm(int _detectorAlgorithm)
    {
        detectorAlgorithm = _detectorAlgorithm;
        for(int i=0; i<bod.length; i++) bod[i].setDetectorAlgorithm(_detectorAlgorithm);
    }
    public int  getDetectorAlgorithm()                       { return(detectorAlgorithm); }
}
