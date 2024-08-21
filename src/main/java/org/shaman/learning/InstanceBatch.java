/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.learning;

import java.util.Hashtable;
import java.util.Iterator;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>Default Batch Presentation Implementation</h2>
 * Batch Presenter using a Presenter to draw instances from. <br>
 * This presenter can be linked to a number of actual datasources.
 * E.g. In memory buffers, a DataBase Table, etc...
 * Supports Balancing of instances according to Goal Class or Goal Value.
 * Can take a sub-sample of the instances of the Presenter.
 * Can recycle the instances of the Presenter or re-read a new batch
 * of instances from the Presenter for every new batch.
 **/
public class InstanceBatch implements BatchPresenter
{
    private int[]     index;            // The Positions of the Source's Instances in the batch
    private double    sampleFraction;   // Subsampling Fraction (in [0,1])
    private int       balance;          // Instance balance w.r.t to classification goal attribute
    private int       batchType;        // Type of batch refresh
    private Presenter source;           // The instance source (e.g. memory cache, hardware device)
    
    // **********************************************************\
    // *            Produce a new batch of instances            *
    // **********************************************************/
    private boolean firstTime = true;
    
    public void nextBatch() throws LearnerException
    {
        int             i, j, numin;
        int             ind1, ind2;
        int             indbuf;
        int             goalind;
        DataModelPropertyLearning learn;
        AttributeDouble attgoal;
        Hashtable       hashgval;
        double          []igoals;
        double          []pgoals;
        int             [][]gclass;
        double          cd;
        double          pclass;
        Object          keynow;
        Iterator        itkeys;
        
        // Get a new batch of instances from the instance source
        if (firstTime || (batchType == BATCH_REFRESH))
        {
            source.getNewInstances();
        }
        
        // Create space for the instances if necessary
        numin = (int)(sampleFraction*source.getNumberOfInstances());
        if ((index == null) || (numin != index.length)) { index = new int[numin]; }
        
        // No goal balancing
        if (balance == BatchPresenter.GOAL_BALANCE_NONE)
        {
            // Create a new set of indices taking into account the sub-sampling and goal balancing parameters
            if (sampleFraction == 1.00) // Use them all.
            {
                // Don't do the sub-sampling then...
                for (i=0; i<index.length; i++) index[i] = i;
            }
            else
            {
                // Select the specified random fraction of the instances
                int     numinsrc = source.getNumberOfInstances();
                byte    []isel = new byte[numinsrc];
                
                for (i=0; i<isel.length; i++) isel[i] = 0;
                for (i=0; i<index.length; i++)
                {
                    ind1      = Uniform.staticNextIntFromTo(0, numinsrc-1);
                    while (isel[ind1] == 1) { ind1++; if (ind1 == numinsrc) ind1 = 0; }
                    index[i] = ind1;
                }
            }
        }
        else if (balance == BatchPresenter.GOAL_BALANCE_CLASS || balance == BatchPresenter.GOAL_BALANCE_CLASS_VALUE)
        {
            try
            {
                DataModelDouble dmdo;
                
                dmdo  = (DataModelDouble)source.getDataModel();
                learn = source.getDataModel().getLearningProperty();
                // Balance the instances according to their goal value or class.
                
                // Determine the probabilities of having a certain goal value or class.
                goalind  = learn.getGoalIndex();
                attgoal  = dmdo.getAttributeDouble(goalind);
                hashgval = new Hashtable();
                gclass   = dmdo.getAttributeDouble(goalind).getGoalClassIndices();
                pclass   = 1.0 / gclass.length;
                for (i=0; i<gclass.length; i++)
                {
                    if (balance == GOAL_BALANCE_CLASS_VALUE)
                    {
                        for (j=0; j<gclass[i].length; j++)
                        {
                            cd = attgoal.getCategoryDouble(gclass[i][j]);  // Balance according to goal value
                            hashgval.put(new Double(cd), new Double(pclass / gclass[i].length));
                        }
                    }
                    else if (balance == GOAL_BALANCE_CLASS)
                    {
                        hashgval.put(new Double(i), new Double(pclass));  // Balance according to goal class
                    }
                }
            }
            catch(DataModelException ex) { throw new LearnerException(ex); }
            
            // Make a PDF for selecting a goal class or value.
            itkeys   = hashgval.keySet().iterator();
            igoals   = new double[hashgval.size()];
            pgoals   = new double[hashgval.size()];
            i = 0;
            while (itkeys.hasNext())
            {
                keynow    = itkeys.next();
                igoals[i] = ((Double)keynow).doubleValue();
                pgoals[i] = ((Double)hashgval.get(keynow)).doubleValue();
                i++;
            }
            
            double rv, acc;
            for (i=0; i<index.length; i++)
            {
                // Select the goal value of the next instances according to the PDF
                rv = Uniform.staticNextDoubleFromTo(0.0,1.0);
                j = 0; acc = pgoals[0]; while (acc < rv) { acc += pgoals[++j]; }
                
                // Get an instance with the correct goal value
                if (balance == BatchPresenter.GOAL_BALANCE_CLASS)
                {
                    index[i] = source.getIndexWithGoalClass((int)igoals[j]);
                }
                else if (balance == BatchPresenter.GOAL_BALANCE_CLASS_VALUE)
                {
                    index[i] = source.getIndexWithGoalValue(igoals[j]);
                }
            }
        }
        
        // Shuffle them around a bit to re-gain the randomness.
        for (i=0; i<index.length; i++)
        {
            ind1   = Uniform.staticNextIntFromTo(0, index.length-1);
            ind2   = Uniform.staticNextIntFromTo(0, index.length-1);
            indbuf = index[ind1];
            index[ind1] = index[ind2];
            index[ind2] = indbuf;
        }
        
        firstTime = false;
    }
    
    public int getIndexWithGoalClass(int gc) throws LearnerException
    {
        throw new LearnerException("Cannot get an index of specific goal class in a BatchPresenter");
    }
    
    public int getIndexWithGoalValue(double gv) throws LearnerException
    {
        throw new LearnerException("Cannot get an index of specific goal value in a BatchPresenter");
    }
    
    public int getIndexWithObjectGoalValue(Object ov) throws LearnerException
    {
        throw new LearnerException("Cannot get an index of specific Object goal value in a BatchPresenter");
    }
    
    public void getNewInstances() throws LearnerException
    {
        nextBatch();
    }
    
    public void reorder(int []ind) throws LearnerException
    {
        int              i;
        ObjectMatrix1D   []oinnew;
        DoubleMatrix1D   []innew;
        Object           []ogoalnew;
        double           []goalnew;
        double           []weightnew;
        CachingPresenter csrc;
        
        if (source instanceof CachingPresenter)
        {
            csrc      = (CachingPresenter)source;
            weightnew = csrc.getWeights();
            if (isPrimitive())
            {
                // Primitive Instances
                innew     = csrc.getInstances();
                goalnew   = csrc.getGoals();
                
                for (i=0; i<innew.length; i++)
                {
                    innew[i]     = csrc.getInstance(index[ind[i]]);
                    goalnew[i]   = csrc.getGoal(index[ind[i]]);
                    weightnew[i] = csrc.getWeight(index[ind[i]]);
                }
                csrc.setInstances(innew);
                csrc.setGoals(goalnew);
                csrc.setWeights(weightnew);
            }
            else
            {
                // Object Instances
                oinnew   = csrc.getObjectInstances();
                ogoalnew = csrc.getObjectGoals();
                
                for (i=0; i<oinnew.length; i++)
                {
                    oinnew[i]    = csrc.getObjectInstance(index[ind[i]]);
                    ogoalnew[i]  = csrc.getObjectGoal(index[ind[i]]);
                    weightnew[i] = csrc.getWeight(index[ind[i]]);
                }
                csrc.setObjectInstances(oinnew);
                csrc.setObjectGoals(ogoalnew);
                csrc.setWeights(weightnew);
            }
        }
        else throw new LearnerException("Can't reorder because the source Presenter is not a CachingPresenter.");
    }
    
    
    // **********************************************************\
    // *                  Presenter Interface                   *
    // **********************************************************/
    public int getNumberOfInstances()
    {
        if (index != null) return(index.length);
        else               return(-1);
    }
    
    public DataModel getDataModel()              { return(source.getDataModel()); }
    public DataModel getInputDataModel(int port) { return(source.getDataModel()); }
    public void setDataModel(DataModel dm) { source.setDataModel(dm); }
    
    public DoubleMatrix1D getInstance(int ind) throws LearnerException       { return(source.getInstance(index[ind])); }
    public double         getGoal(int ind)   throws LearnerException         { return(source.getGoal(index[ind]));     }
    public double         getWeight(int ind)                               { return(source.getWeight(index[ind]));   }
    public ObjectMatrix1D getObjectInstance(int ind) throws LearnerException { return(source.getObjectInstance(index[ind])); }
    public Object         getObjectGoal(int ind) throws LearnerException     { return(source.getObjectGoal(index[ind])); }
    public int            getGoalClass(int ind) throws LearnerException      { return(source.getGoalClass(index[ind])); }
    public void           setInstance(int ind, DoubleMatrix1D in)          { source.setInstance(index[ind], in); }
    public void           setObjectInstance(int ind, ObjectMatrix1D in)    { source.setObjectInstance(index[ind], in); }
    public void           setGoal(int ind, double g)                { source.setGoal(index[ind],g);  }
    public void           setObjectGoal(int ind, Object g)          { source.setObjectGoal(index[ind], g); }
    public void           setWeight(int ind, double w)              { source.setWeight(index[ind], w);  }
    
    public void setWeights(double []weights)
    {
        for (int i=0; i<weights.length; i++) setWeight(i, weights[i]);
    }
    
    public void setInstances(DoubleMatrix1D []ins)
    {
        for (int i=0; i<ins.length; i++) setInstance(i, ins[i]);
    }
    
    public void setObjectInstance(ObjectMatrix1D []ins)
    {
        for (int i=0; i<ins.length; i++) setObjectInstance(i, ins[i]);
    }
    
    public void setGoals(double []goals)
    {
        for (int i=0; i<goals.length; i++) setGoal(i, goals[i]);
    }
    
    public void setObjectGoals(Object []goals)
    {
        for (int i=0; i<goals.length; i++) setObjectGoal(i, goals[i]);
    }
    
    public void push(int port, Object ob)
    {
        // Push into oblivion...
        ;
    }
    
    
    // **********************************************************\
    // *                      Parameter Access                  *
    // **********************************************************/
    public void setBalance(int _balance) { balance = _balance; }
    public int  getBalance()             { return(balance); }
    
    public void setBatchType(int _batchType) { batchType = _batchType; }
    public int  getBatchType()               { return(batchType); }
    
    public void   setSampleFraction(double _sampleFraction) { sampleFraction = _sampleFraction; }
    public double getSampleFraction()                       { return(sampleFraction); }
    
    // **********************************************************\
    // *                      Initialiazation                   *
    // **********************************************************/
    public InstanceBatch()
    {
    }
    
    /**
     * Create a BatchPresenter with as source the given Presenter and using the given batch parameters.
     * @param _source The Presenter where the actual data is stored in.
     * @param _batchType Reread or recycle the data in the source every batch.
     * @param _balance The type of goal balancing to use
     * @param _sampleFraction The amount (0..1) of sub-sampling to do
     * @throws LearnerException If the DataModel or source Presenter do not fit with the given parameters.
     */
    public void create(Presenter _source, int _batchType, int _balance, double _sampleFraction) throws LearnerException
    {
        this.source         = _source;
        this.batchType      = _batchType;
        this.balance        = _balance;
        this.sampleFraction = _sampleFraction;
        
        if (this.balance != BatchPresenter.GOAL_BALANCE_NONE)
        {
            DataModelPropertyLearning learn;
            learn = source.getDataModel().getLearningProperty();
            if (!learn.getHasGoal()) throw new LearnerException("Cannot balance batch instances if the datamodel does not have a goal.");
            Attribute attgoal = source.getDataModel().getAttribute(learn.getGoalIndex());
            if (attgoal.getGoalType() != Attribute.GOAL_CLASS) throw new LearnerException("Can only balance instances if the goal attribute is not a classification goal.");
        }
    }
    
    // **********************************************************\
    // *  Make a copy of this presenter. Don't touch the source *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        InstanceBatch clib  = new InstanceBatch();
        try
        {
            clib.create(source, batchType, balance, sampleFraction);
            clib.index          = (int [])index.clone(); // Otherwise super.clone() could be used
        }
        catch(LearnerException ex) { throw new CloneNotSupportedException("Cannot clone because of HCI exception"); }
        
        return(clib);
    }
    
    private boolean isPrimitive() { return(source.getDataModel() instanceof DataModelDouble); }
}
