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


import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyLearning;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.DataModelException;

/**
 * <h2>Learner DataModels</h2>
 */
public class LearnerDataModels
{
    // **********************************************************\
    // *        Machine Learning DataModel Construction         *
    // **********************************************************/
    /**
     * Makes a DataModel that contains the output of a Classifier on the given DataModel.
     * This model has 1 primitive, categorical attribute, containing the goal class values
     * @param dmin The DataModel on which the Classifier works.
     *             Should have a categorical attribute as goal.
     * @return The DataModel of the output of a Classifier with as input DataModel <code>dmin</code>
     * @throws DataModelException If <code>dmin</code> does not comply with the required format.
     */
    public static DataModel getClassifierDataModel(DataModel dmin) throws DataModelException
    {
        return(getClassifierDataModel(dmin, Classifier.OUT_CLASS));
    }
    
    /**
     * Makes a DataModel that contains the output of a Classifier on the given DataModel.
     * This model contains the class and depending on the type of output wanted one or all confidence values.
     * @param dmin The DataModel on which the Classifier works.
     *             Should have a categorical attribute as goal.
     * @param classOut Type of output DataModel wanted
     * @return The DataModel of the output of a Classifier with as input DataModel <code>dmin</code>
     * @throws DataModelException If <code>dmin</code> does not comply with the required format.
     * @see org.shsman.learning.Classifier#OUT_CLASS
     * @see org.shaman.learning.Classifier#OUT_CLASS_AND_CONFIDENCE
     * @see org.shaman.learning.Classifier#OUT_CLASS_AND_CONFIDENCE_VECTOR
     */
    public static DataModel getClassifierDataModel(DataModel dmin, int classOut) throws DataModelException
    {
        int              i;
        DataModel        cldm;
        Attribute        attgoal;
        AttributeDouble  attcl;
        int              numclass;
        double           []classlegal;
        DataModelPropertyLearning learn;
        
        // Enforce strict datamodel and goal attribute requirements
        learn = dmin.getLearningProperty();
        if (!learn.getHasGoal()) throw new DataModelException("Cannot create Classification DataModel without a goal attribute.");
        attgoal = dmin.getAttribute(learn.getGoalIndex());
        if  (!attgoal.hasProperty(Attribute.PROPERTY_CATEGORICAL)) throw new DataModelException("Cannot create Classification DataModel for a non-categorical goal attribute '"+attgoal.getName()+"'");
        if  ( attgoal.getGoalType() != Attribute.GOAL_CLASS)       throw new DataModelException("Cannot create Classification DataModel based on a goal-attribute '"+attgoal.getName()+"' that is not a classification goal.");
        
        // Make a DataModel with 1 attribute, being the classification output.
        if      (classOut == Classifier.OUT_CLASS)
        {
            cldm       = new DataModelDouble(dmin.getName()+"-classification", 1);
            numclass   = attgoal.getNumberOfCategories();
            classlegal = new double[numclass];
            for (i=0; i<numclass; i++) classlegal[i] = i;
            attcl      = (AttributeDouble)cldm.getAttribute(0);
            attcl.initAsSymbolCategorical(classlegal);
            attcl.setIsActive(true);
            attcl.setName("class");
        }
        else if (classOut == Classifier.OUT_CLASS_AND_CONFIDENCE)
        {
            // Output has 2 attribute. The class and the confidence in this class.
            cldm       = new DataModelDouble(dmin.getName()+"-classification", 2);
            numclass   = attgoal.getNumberOfCategories();
            classlegal = new double[numclass];
            for (i=0; i<numclass; i++) classlegal[i] = i;
            attcl      = (AttributeDouble)cldm.getAttribute(0);
            attcl.initAsSymbolCategorical(classlegal);
            attcl.setIsActive(true);
            attcl.setName("class");
            attcl      = (AttributeDouble)cldm.getAttribute(1);
            attcl.initAsNumberContinuous();
            attcl.setIsActive(true);
            attcl.setName("confidence");
        }
        else if (classOut == Classifier.OUT_CLASS_AND_CONFIDENCE_VECTOR)
        {
            // Output has 1+number of classes  outputs : The class and the confidence in all these classes.
            numclass   = attgoal.getNumberOfCategories();
            cldm       = new DataModelDouble(dmin.getName()+"-classification", 1+numclass);
            classlegal = new double[numclass];
            for (i=0; i<numclass; i++) classlegal[i] = i;
            attcl      = (AttributeDouble)cldm.getAttribute(0);
            attcl.initAsSymbolCategorical(classlegal);
            attcl.setIsActive(true);
            attcl.setName("class");
            for (i=0; i<numclass; i++)
            {
                attcl = (AttributeDouble)cldm.getAttribute(i+1);
                attcl.initAsNumberContinuous();
                attcl.setIsActive(true);
                attcl.setName("confidence"+i);
            }
        }
        else throw new DataModelException("Cannot create classifier DataModel : unknown output type.");
        
        return(cldm);
    }
    
    /**
     * Make a DataModel for the ouput of an unsupervised classifier (clusterer).
     * @param numclus The number of clusters.
     * @return A LearnerDataModel with 1 categorical AttributeDouble, containing the cluster number.
     */
    public static DataModel getClustererDataModel(int numclus) //throws DataModelException
    {
        int              i;
        DataModel        cldm;
        AttributeDouble  attcl;
        double           []cluslegal;
        
        // Make a DataModel with 1 attribute, being the cluster assignment output.
        cldm      = new DataModelDouble(numclus+"-clusters", 1);
        cluslegal = new double[numclus];
        for (i=0; i<numclus; i++) cluslegal[i] = i;
        attcl     = (AttributeDouble)cldm.getAttribute(0);
        attcl.initAsSymbolCategorical(cluslegal);
        attcl.setIsActive(true);
        
        return(cldm);
    }
    
    /**
     * Make a DataModel for the output of a supervised estimator (function regression).
     * It contains 1 attribute corresponding to the continuous goal attribute of the input datamodel.
     * @param dmin The DataModel of the input of the estimator.
     * @return The output datamodel containing the single output attribute.
     * @throws DataModelException If the input datamodel does not have a continuous goal attribute.
     */
    public static DataModel getEstimatorDataModel(DataModel dmin) throws DataModelException
    {
        DataModel  dmes;
        Attribute  attes;
        DataModelPropertyLearning learn;
        
        // Check if the input datamodel has a estimator goal
        learn = dmin.getLearningProperty();
        if (!learn.getHasGoal()) throw new DataModelException("Cannot create Estimator DataModel without a goal attribute.");
        attes = dmin.getAttribute(learn.getGoalIndex());
        if  (!attes.hasProperty(Attribute.PROPERTY_CONTINUOUS)) throw new DataModelException("Cannot create Estimator DataModel for a non-continuous goal attribute '"+attes.getName()+"'");
        if  ( attes.getGoalType() != Attribute.GOAL_VALUE)      throw new DataModelException("Cannot create Estimator DataModel based on a goal-attribute '"+attes.getName()+"' that is not a value goal.");
        
        // Make an output datamodel containing only this goal
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
        {
            dmes  = new DataModelDouble(attes.getName()+"-estimation", 1);
            try
            {
                attes = (AttributeDouble)attes.clone();
            }
            catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        }
        else
        {
            dmes = new DataModelObject(attes.getName()+"-estimation", 1);
            try
            {
                attes = (AttributeObject)attes.clone();
            }
            catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        }
        attes.setIsGoal(false);
        attes.setIsActive(true);
        dmes.setAttribute(0, attes);
        
        return(dmes);
    }
    
    private LearnerDataModels()
    {
    }
}
