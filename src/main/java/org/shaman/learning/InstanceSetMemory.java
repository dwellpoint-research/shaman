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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.shaman.dataflow.DataFlow;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


// **********************************************************\
// *  A set of weighted instances stored in a memory cache  *
// **********************************************************/
/**
 * <h2>Default CachingPresenter Implementation</h2>
 * Stores a number of instances in memory to allow
 * fast and random order data access by machine learning algorithms that need this.
 */
public class InstanceSetMemory extends Transformation implements CachingPresenter
{
    private String           name;               // The name of this Instance Set
    private DataModel        dataModel;          // The datamodel of the instances' attributes
    
    // Primitive Instances
    private DoubleMatrix1D []instance;
    private double         []goal;
    // Object instances
    private ObjectMatrix1D []oinstance;
    private Object         []ogoal;
    // Instance Weight
    private double         []weight;      // The instance weights. (for e.g. Case Based Reasoning, Boosting)
    // Class Balancing
    private int              pos = 0;
    
    // **********************************************************\
    // *    Flow Base Abstract Naming Methods Implementation    *
    // **********************************************************/
    public boolean areInputsAvailable(int port, int count) { return(false); }
    public Object  getInput(int port) { return(null); }
    public void    clearQueues()  {}
    public Object  pull(int port, DataFlow con) { return(null); }
    public void    setOutput(int port, Object out) { }
    
    public String getName()        { return("Instance Set Memory"); }
    public String getDescription() { return("A memory cache of instances"); }
    public void transform() {}
    
    public String getOutputName(int port)
    {
        return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Data Input");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(0); }
    
    // **********************************************************\
    // *     Interface for Batch Presenter Class Balancing      *
    // **********************************************************/
    public int getIndexWithGoalClass(int gc) throws LearnerException
    {
        int                       begpos;
        DataModelPropertyLearning learn;
        DataModelDouble           dmdo = null;
        DataModelObject           dmob = null;
        
        begpos = pos;
        learn  = this.dataModel.getLearningProperty();
        if (isPrimitive()) dmdo = (DataModelDouble)this.dataModel;
        else               dmob = (DataModelObject)this.dataModel;
        
        try
        {
            if (isPrimitive())
            {
                AttributeDouble attgoal = dmdo.getAttributeDouble(learn.getGoalIndex());
                while (attgoal.getGoalClass(goal[pos]) != gc)
                {
                    if (pos == (goal.length-1)) pos = 0;
                    else                        pos++;
                    if (pos == begpos) throw new LearnerException("Cannot find an instance with primitive goal class "+gc);
                }
            }
            else
            {
                AttributeObject attob = dmob.getAttributeObject(learn.getGoalIndex());
                while(attob.getGoalClass(ogoal[pos]) != gc)
                {
                    if (pos == (ogoal.length-1)) pos = 0;
                    else                         pos++;
                    if (pos == begpos) throw new LearnerException("Cannot find an instance with Object goal class "+gc);
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(pos);
    }
    
    public int getIndexWithGoalValue(double gv) throws LearnerException
    {
        int begpos;
        
        begpos = pos;
        while (this.goal[pos] != gv)
        {
            if (pos == (this.goal.length-1)) pos = 0;
            else                             pos++;
            if (pos == begpos) throw new LearnerException("Cannot find an instance with goal value "+gv);
        }
        
        return(pos);
    }
    
    public int getIndexWithObjectGoalValue(Object gv) throws LearnerException
    {
        int begpos;
        
        begpos = pos;
        while (!this.ogoal[pos].equals(gv))
        {
            if (pos == (ogoal.length-1)) pos = 0;
            else                         pos++;
            if (pos == begpos) throw new LearnerException("Cannot find an instance with Object goal value "+gv.toString());
        }
        
        return(pos);
    }
    
    // **********************************************************\
    // *                  Instance Weighting                    *
    // **********************************************************/
    public void setWeightWhereGoalIs(double goalValue, double w)
    {
        for (int i=0; i<this.goal.length; i++)
        {
            if (this.goal[i] == goalValue) this.weight[i] = w;
        }
    }
    
    public void setWeightWhereGoalIs(Object goalValue, double w)
    {
        for (int i=0; i<this.ogoal.length; i++)
        {
            if (this.ogoal[i].equals(goalValue)) this.weight[i] = w;
        }
    }
    
    // **********************************************************\
    // *                  Raw Random Data Access                *
    // **********************************************************/
    public int getNumberOfInstances()
    {
        if (isPrimitive())
        {
            if (instance != null) return(instance.length);
            else                  return(-1);
        }
        else
        {
            if (oinstance != null) return(oinstance.length);
            else                   return(-1);
        }
    }
    
    public void           getNewInstances() { }
    public DoubleMatrix1D []getInstances()       { return(instance);  }
    public double         []getGoals()           { return(goal);      }
    public ObjectMatrix1D []getObjectInstances() { return(oinstance); }
    public Object         []getObjectGoals()     { return(ogoal);     }
    public double         []getWeights()         { return(weight);   }
    public void           setGoals(double []_goal)                        { goal      = _goal; }
    public void           setInstances(DoubleMatrix1D []_instance)        { instance  = _instance; }
    public void           setObjectGoals(Object []_ogoal)                 { ogoal     = _ogoal; }
    public void           setObjectInstances(ObjectMatrix1D []_oinstance) { oinstance = _oinstance; }
    public void           setWeights(double []_weight)                    { weight    = _weight; }
    
    public DataModel getInputDataModel(int port)   { return(dataModel); }
    public DataModel getDataModel()    { return(dataModel); }
    public void setDataModel(DataModel dm) { dataModel = (DataModel)dm; }
    
    public double         getWeight(int ind)                            { return(weight[ind]);   }
    public DoubleMatrix1D getInstance(int ind) throws LearnerException  { return(instance[ind]); }
    public double         getGoal(int ind)     throws LearnerException  { return(goal[ind]);     }
    public ObjectMatrix1D getObjectInstance(int ind) throws LearnerException { return(oinstance[ind]); }
    public Object         getObjectGoal(int ind)     throws LearnerException { return(ogoal[ind]);     }
    
    public int            getGoalClass(int ind) throws LearnerException
    {
        int       gc;
        Attribute attgoal;
        double    goal;
        Object   ogoal;
        
        gc = -1;
        attgoal = dataModel.getAttribute(dataModel.getLearningProperty().getGoalIndex());
        if (attgoal.getGoalType() == Attribute.GOAL_CLASS)
        {
            try
            {
                if (isPrimitive())
                {
                    goal = getGoal(ind);
                    gc   = ((AttributeDouble)attgoal).getGoalClass(goal);
                }
                else
                {
                    ogoal = getObjectGoal(ind);
                    gc    = ((AttributeObject)attgoal).getGoalClass(ogoal);
                }
            }
            catch(DataModelException ex) { throw new LearnerException(ex); }
        }
        else throw new LearnerException("Cannot get goal class for regression goal attribute '"+attgoal.getName()+"'");
        
        return(gc);
    }
    
    public void setWeight(int ind, double w)            { weight[ind]   = w;  }
    public void setInstance(int ind, DoubleMatrix1D in) { instance[ind] = in; }
    public void setGoal(int ind, double g)              { goal[ind]     = g;  }
    public void setObjectInstance(int ind, ObjectMatrix1D in) { oinstance[ind] = in; }
    public void setObjectGoal(int ind, Object g) { ogoal[ind] = g; }
    
    public void makeInstances(int size)
    {
        int i;
        
        if (isPrimitive())
        {
            instance = new DoubleMatrix1D[size];
            goal     = new double[size];
        }
        else
        {
            oinstance = new ObjectMatrix1D[size];
            ogoal     = new Object[size];
        }
        weight    = new double[size];
        for (i=0; i<weight.length; i++) weight[i] = 1.0;
    }
    
    // **********************************************************\
    // *              Data Set Resizing and reordering          *
    // **********************************************************/
    public void reorder(int []ind)
    {
        int            i;
        DoubleMatrix1D []innew;
        double         []goalnew;
        ObjectMatrix1D []oinnew;
        Object         []ogoalnew;
        double         []weightnew;
        
        if (isPrimitive())
        {
            innew     = new DoubleMatrix1D[ind.length];
            goalnew   = new double[ind.length];
            weightnew = new double[ind.length];
            for (i=0; i<innew.length; i++)
            {
                innew[i]     = instance[ind[i]];
                goalnew[i]   = goal[ind[i]];
                weightnew[i] = weight[ind[i]];
            }
            instance = innew;
            goal     = goalnew;
            weight   = weightnew;
        }
        else
        {
            oinnew    = new ObjectMatrix1D[ind.length];
            ogoalnew  = new Object[ind.length];
            weightnew = new double[ind.length];
            for (i=0; i<ind.length; i++)
            {
                oinnew[i]    = oinstance[ind[i]];
                ogoalnew[i]  = ogoal[ind[i]];
                weightnew[i] = weight[ind[i]];
            }
            oinstance = oinnew;
            ogoal     = ogoalnew;
            weight    = weightnew;
        }
    }
    
    // **********************************************************\
    // *    Clone the presenter. Do not clone the actual data   *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        // Clone the instance and goal and weight arrays. Do not touch the instances themselves.
        InstanceSetMemory clism;
        
        clism           = new InstanceSetMemory();
        clism.name      = name;
        clism.dataModel = dataModel;
        if (isPrimitive())
        {
            clism.instance  = (DoubleMatrix1D [])instance.clone();
            clism.goal      = (double [])goal.clone();
            clism.oinstance = null;
            clism.ogoal     = null;
        }
        else
        {
            clism.instance  = null;
            clism.goal      = null;
            clism.oinstance = (ObjectMatrix1D [])oinstance.clone();
            clism.ogoal     = (Object [])ogoal.clone();
        }
        clism.weight    = (double [])weight.clone();
        
        return(clism);
    }
    
    public void push(int port, Object ob)
    {
        // Push into oblivion...
        ;
    }
    
    // **********************************************************\
    // *                     Construction                       *
    // **********************************************************/
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void init() throws ConfigException
    {
    }
    
    /**
     * Create a CachingPresenter containing all instances from the specified source Presenter.
     * @param src The source presenter to get all instances from.
     * @throws LearnerException If the instances, their goals or weights cannot be read from the source presenter.
     */
    public void create(Presenter src) throws LearnerException
    {
        int i;
        int numins;
        
        numins    = src.getNumberOfInstances();
        dataModel = src.getDataModel();
        if (isPrimitive())
        {
            // Primitive Instances
            instance  = new DoubleMatrix1D[numins];
            goal      = new double[numins];
            weight    = new double[numins];
            for (i=0; i<numins; i++)
            {
                instance[i] = src.getInstance(i);
                goal[i]     = src.getGoal(i);
                weight[i]   = src.getWeight(i);
            }
        }
        else
        {
            // Object Instances
            oinstance = new ObjectMatrix1D[numins];
            ogoal     = new Object[numins];
            weight    = new double[numins];
            for (i=0; i<numins; i++)
            {
                oinstance[i] = src.getObjectInstance(i);
                ogoal[i]     = src.getObjectGoal(i);
                weight[i]    = src.getWeight(i);
            }
        }
    }
    
    
    public void create(DataFlow df) throws LearnerException
    {
        DataFlow          sup;
        InstanceSetMemory im;
        
        // Works for typical MemorySupplier -> Estimator -> ... Estimator ... -> Machine Learner  setup.
        im = new InstanceSetMemory();
        
        
        try
        {
            // Get the InstanceSet as output by the Supplier
            sup = df.getSupplier(0);
            if      (sup instanceof MemorySupplier) im.create((MemorySupplier)sup);
            else if (sup instanceof Estimator)      im.create(sup);
            
            // Transform the data with whatever this learner is
            if      (this instanceof Estimator)  im = estimateAll(this, (Estimator)df);
            //else if (this instanceof Classifier) im = classifyAll(this, (Classifier)df);
        }
        catch(DataFlowException ex) { throw new LearnerException(ex); }
        
        // Install the DataModel and Instances
        this.setDataModel(im.getDataModel());
        if   (isPrimitive()) this.instance  = im.getInstances();
        else                 this.oinstance = im.getObjectInstances();
    }
    
    public static InstanceSetMemory estimateAll(InstanceSetMemory im, Estimator est) throws LearnerException
    {
        InstanceSetMemory imout;
        int               i;
        DoubleMatrix1D  []insin;
        DoubleMatrix1D  []insest;
        
        imout = null;
        try { imout = (InstanceSetMemory)im.clone(); } catch(CloneNotSupportedException ex) { throw new LearnerException(ex); }
        
        insin  = im.getInstances();
        insest = imout.getInstances();
        for (i=0; i<insest.length; i++) insest[i] = est.estimate(insin[i]);
        
        try
        {
            imout.setDataModel(((Transformation)est).getOutputDataModel(0));
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
        
        return(imout);
    }
    
    public void create(MemorySupplier ms) throws LearnerException
    {
        DataModel dm;
        List      liin;
        
        try
        {
            dm = ms.getOutputDataModel(0);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
        
        if (dm instanceof DataModelDouble)
        {
            DoubleMatrix1D []dins = ms.getDoubleInstances();
            liin                  = Arrays.asList(dins);
        }
        else
        {
            ObjectMatrix1D []oins = ms.getObjectInstances();
            liin                  = Arrays.asList(oins);
        }
        createFromList(liin, dm);
    }
    
    /**
     * Create a CachingPresenter with the given datamodel and instances created from the given list of data vectors.
     * @param lins A list of data vectors used to create the instances.
     * @param dm The datamodel of the instance.
     * @throws LearnerException If the instances cannot be created.
     */
    public void create(List lins, DataModel dm) throws LearnerException
    {
        createFromList(lins, dm);
    }
    
    private void createFromList(List lins, DataModel dm) throws LearnerException
    {
        int             i;
        int             numins;
        Object          ogoalnow;
        double          goalnow;
        LinkedList      linsleg;
        Iterator        liit;
        DataModelObject dmob = null;
        DataModelDouble dmdo = null;
        ObjectMatrix1D  ovnow;
        DoubleMatrix1D  dvnow;
        AttributeObject atob;
        AttributeDouble atdo;
        int           []actind;
        boolean                   prim;
        DataModelPropertyLearning learn;
        
        // Filter the list. Only retain instances that have a goal (if present)
        this.dataModel = dm;
        learn  = dm.getLearningProperty();
        if (dm instanceof DataModelDouble) { prim = true;  dmdo = (DataModelDouble)dm; }
        else                               { prim = false; dmob = (DataModelObject)dm; }
        
        try
        {
            actind = dataModel.getActiveIndices();
            if (learn.getHasGoal()) // Is there a goal?
            {
                // Get goal attribute.
                atdo = null; atob = null;
                if (prim) atdo = dmdo.getAttributeDouble(learn.getGoalIndex());
                else      atob = dmob.getAttributeObject(learn.getGoalIndex());
                
                // Iterate over input list. Throw away instances with missing value for their goal.
                linsleg = new LinkedList();
                liit    = lins.iterator();
                while (liit.hasNext())
                {
                    if (prim)
                    {
                        // Filter primtive vector if goal is missing
                        dvnow   = (DoubleMatrix1D)liit.next();
                        goalnow = learn.getInstanceGoal(dvnow);
                        if (!atdo.isMissingAsDouble(goalnow)) linsleg.addLast(dvnow);
                    }
                    else
                    {
                        // Filter object vector if goal is missing
                        ovnow    = (ObjectMatrix1D)liit.next();
                        ogoalnow = learn.getInstanceGoal(ovnow);
                        if (!atob.isMissingAsObject(ogoalnow)) linsleg.addLast(ovnow);
                    }
                }
            }
            else linsleg = new LinkedList(lins); // No goal, then just take all of them.
            
            if (prim)
            {
                // Create the instance data from the double vector list
                numins    = linsleg.size();
                instance  = new DoubleMatrix1D[numins];
                goal      = new double[numins];
                weight    = new double[numins];
                liit      = linsleg.iterator();
                for (i=0; i<numins; i++)
                {
                    dvnow       = (DoubleMatrix1D)liit.next();
                    weight[i]   = 1.0;
                    if (learn.getHasGoal()) goal[i] = learn.getInstanceGoal(dvnow);
                    instance[i] = learn.getInstanceVector(actind, dvnow);
                }
            }
            else
            {
                // Create the instance data from the object vector list
                numins     = linsleg.size();
                oinstance  = new ObjectMatrix1D[numins];
                ogoal      = new Object[numins];
                weight     = new double[numins];
                liit       = linsleg.iterator();
                actind     = dataModel.getActiveIndices();
                for (i=0; i<numins; i++)
                {
                    ovnow       = (ObjectMatrix1D)liit.next();
                    weight[i]   = 1.0;
                    if (learn.getHasGoal()) ogoal[i] = learn.getInstanceGoal(ovnow);
                    oinstance[i] = learn.getInstanceVector(actind, ovnow);
                }
            }
        }
        catch(DataModelException ex)  { throw new LearnerException(ex); }
    }
    
    // Use Primitive or Object vectors.
    private boolean isPrimitive() { return(this.dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }
}
