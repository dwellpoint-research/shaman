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
package org.shaman.datamodel;

import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Datamodel for Machine Learning Algorithms</h2>
 * The Structure of a Data Vector used in a machine learning flow. <br>
 * Can be used for primitive or <code>Object</code>based data vectors.
 * Selects the goal attribute.
 * Can make fixed-form datamodels for machine-learning algorithm support.
 */

// **********************************************************\
// *  The Structure of a Data Vector somewhere in a Flow    *
// **********************************************************/
public class DataModelPropertyLearning implements DataModelProperty
{
    // What to do to when compiling an instance with a missing value.
    public static final int MISSING_IGNORE  = 0;   // Make the instance containing the missing value
    public static final int MISSING_DISCARD = 1;   // Discard the instance containing the missing value
    
    /** The underlying datamodel */
    private DataModel       datamodel;
    private DataModelDouble dmdo;
    private DataModelObject dmob;
    /** Does this DataModel have an attribute that is currently assigned to be used as a machine-learning goal */
    private boolean   hasGoal;            // Is there a classification or regression goal?
    /** The name of the goal attribute */
    private String    goalName;           // Name of the goal attribute
    /** The index in <code>attribute</code> of the goal. */
    private int       goalIndex;          // Index of the goal attribute
    private int       missingHandling;    // What to do when a missing values is encountered
    
    // **********************************************************\
    // *                  Instance Construction                 *
    // **********************************************************/
    /**
     * Get the value corresponding to the goal attribute of this datamodel from the given double vector
     * @param omin The double vector.
     * @return the goal value of the given double vector
     * @throws DataModelException If this datamodel does not have a goal.
     */
    public double getInstanceGoal(DoubleMatrix1D omin) throws DataModelException
    {
        double    goal;
        
        goal = 0;
        if (hasGoal) { goal = this.dmdo.getAttributeDouble(this.goalIndex).getGoalValue(omin.getQuick(this.goalIndex)); }
        else throw new DataModelException("DataModel has no goal.");
        
        return(goal);
    }
    
    /**
     * Get the Object corresponding to the goal attribute of this datamodel from the given Object vector.
     * @param omin The Object vector
     * @return The goal Object of the given Object vector
     * @throws DataModelException If this datamodel does not have a gola
     */
    public Object getInstanceGoal(ObjectMatrix1D omin) throws DataModelException
    {
        Object goal;
        
        goal = null;
        if (this.hasGoal) { goal = this.dmob.getAttributeObject(this.goalIndex).getGoalValue(omin.getQuick(this.goalIndex)); }
        else throw new DataModelException("DataModel has no goal.");
        
        return(goal);
    }
    
    /**
     * Make an instance from the given double vector :
     * Strip the vector of all its non-active attributes,
     * convert all missing values to the unique missing value indicator.
     * @param ind The double vector
     * @return The instance
     * @throws DataModelException If something goes wrong while making the instance.
     */
    public DoubleMatrix1D getInstanceVector(DoubleMatrix1D ind) throws DataModelException
    {
        return(getInstanceVector(this.datamodel.getActiveIndices(), ind));
    }
    
    /**
     * Make an instance from the given double vector :
     * Strip the vector of all its non-active attributes,
     * convert all missing values to the unique missing value indicator.
     * @param ind The double vector
     * @param actind The indices of the active attributes
     * @return The instance
     * @throws DataModelException If something goes wrong while making the instance.
     */
    public DoubleMatrix1D getInstanceVector(int []actind, DoubleMatrix1D ind) throws DataModelException
    {
        int             i;
        Attribute     []attribute;
        AttributeDouble attnow;
        DoubleMatrix1D  vec;
        double          dnow;
        boolean         abort;
        
        attribute = this.datamodel.getAttributes();
        vec       = null;
        abort     = false;
        dnow  = 0;
        for (i=0; (i<actind.length) && (!abort); i++)
        {
            dnow   = ind.getQuick(actind[i]);
            attnow = (AttributeDouble)attribute[actind[i]];
            if (attnow.isMissingValue(dnow))
            {
                if (missingHandling == MISSING_DISCARD) abort = true;
                else ind.setQuick(actind[i], attnow.getMissingAsDouble());
            }
        }
        if (!abort) vec = ind.viewSelection(actind);
        
        return(vec);
    }
    
    /**
     * Make an instance from the given Object vector :
     * Strip the vector of all its non-active attributes,
     * convert all missing Objects to the unique missing Object indicator.
     * @param ind The Object vector
     * @return The instance
     * @throws DataModelException If something goes wrong while making the instance.
     */
    public ObjectMatrix1D getInstanceVector(ObjectMatrix1D ind) throws DataModelException
    {
        return(getInstanceVector(this.datamodel.getActiveIndices(), ind));
    }
    
    /**
     * Make an instance from the given Object vector :
     * Strip the vector of all its non-active attributes,
     * convert all missing Objects to the unique missing Object indicator.
     * @param inob The Object vector
     * @param actind The indices of all active attributes
     * @return The instance
     * @throws DataModelException If something goes wrong while making the instance.
     */
    public ObjectMatrix1D getInstanceVector(int []actind, ObjectMatrix1D inob) throws DataModelException
    {
        int             i;
        Attribute     []attribute;
        ObjectMatrix1D  vec;
        AttributeObject attnow;
        Object          obnow;
        boolean         abort;
        
        attribute = this.datamodel.getAttributes();
        vec       = null;
        abort     = false;
        obnow     = null;
        for (i=0; (i<actind.length) && (!abort); i++)
        {
            obnow  = inob.getQuick(actind[i]);
            attnow = (AttributeObject)attribute[actind[i]];
            if (attnow.isMissingValue(obnow))
            {
                if (missingHandling == MISSING_DISCARD) abort = true;
                else inob.setQuick(actind[i], attnow.getMissingAsObject());
            }
        }
        if (!abort) vec = inob.viewSelection(actind);
        
        return(vec);
    }
    
    // **********************************************************\
    // *                 Goal Attribute selection               *
    // **********************************************************/
    /**
     * Get the index of the goal attribute.
     * @return The index of the goal attribute. -1 if no goal is defined.
     */
    public int getGoalIndex()
    {
        return(goalIndex);
    }
    
    /**
     * Get the name of the goal attribute.
     * @return The name of the goal attribute. <code>null</code> null if no goal is defined.
     */
    public String getGoalName()
    {
        return(goalName);
    }
    
    /**
     * Make the attribute at the specified index the goal of this datamodel.
     * @param ind The index of the new goal attribute.
     * @throws DataModelException if the specified attribute cannot be a goal.
     */
    public void setGoal(int ind) throws DataModelException
    {
        Attribute []attribute = this.datamodel.getAttributes();
        if (attribute[ind].isGoal)
        {
            this.hasGoal   = true;
            this.goalIndex = ind;
            this.goalName  = attribute[ind].name;
        }
        else throw new DataModelException("The attribute at index "+ind+" called "+attribute[ind].name+" is not a goal attribute");
    }
    
    /**
     * Set the attribute with the given name as goal attribute of this datamodel.
     * @param attname the name of the new goal attribute.
     * @throws DataModelException if an attribute with the given name does not existor
     *                            or if this attribute cannot be a goal.
     */
    public void setGoal(String attname) throws DataModelException
    {
        int ind;
        
        ind = this.datamodel.getAttributeIndex(attname);
        if (ind != -1) setGoal(ind);
        else throw new DataModelException("The attribute called "+attname+" does not exist.");
    }
    
    /**
     * Check if this datamodel has a goal attibute defined.
     * @return <code>true</code> if this datamodel has a goal attribute.
     */
    public boolean getHasGoal()
    {
        return(hasGoal);
    }
    
    /**
     * Get the indices of all attributes that can be made goal of this datamodel.
     * @return The indices of all possible goals.
     */
    public int []getPossibleGoals()
    {
        Attribute []attribute;
        int         i;
        int       []gi;
        int         gcount, pos;
        
        attribute = this.datamodel.getAttributes();
        gcount    = 0;
        for (i=0; i<attribute.length; i++) if (attribute[i].getIsGoal()) gcount++;
        
        pos = 0;
        gi  = new int[gcount];
        for (i=0; i<attribute.length; i++) if (attribute[i].getIsGoal()) gi[pos++] = i;
        
        return(gi);
    }
    
    // **********************************************************\
    // *                     Data Access                        *
    // **********************************************************/
    public void   setMissingHandling(int _missingHandling) { missingHandling = _missingHandling; }
    public int    getMissingHandling() { return(missingHandling); }
    
    // **********************************************************\
    // *                    Initialization                      *
    // **********************************************************/
    public Object clone(DataModel datamodel)
    {
        DataModelPropertyLearning learn;
        
        learn = new DataModelPropertyLearning(datamodel);
        learn.hasGoal         = this.hasGoal;
        learn.goalIndex       = this.goalIndex;
        learn.goalName        = this.goalName;
        learn.missingHandling = this.missingHandling;
        
        return(learn);
    }
    
    public DataModelPropertyLearning(DataModel datamodel)
    {
        // Remember datatype and vector format
        this.datamodel       = datamodel;
        if (datamodel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
        {
            this.dmdo       = (DataModelDouble)datamodel;
        }
        else
        {
            this.dmob       = (DataModelObject)datamodel;
        }
        
        // Set learning parameters defaults
        this.hasGoal         = false;
        this.goalIndex       = -1;
        this.goalName        = null;
        this.missingHandling = MISSING_IGNORE;
    }
}
