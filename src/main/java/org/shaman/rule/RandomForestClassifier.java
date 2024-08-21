/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2013 Shaman Research                   *
 \*********************************************************/
package org.shaman.rule;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

/**
 * Shaman wrapper for RandomForest algorithm.
 */
public class RandomForestClassifier  extends ClassifierTransformation implements Classifier
{

    private int minObjects;                // Minimum number of instances in leaf nodes
    private int maxDepth;                  // Maximum number of levels in the tree
    private double trainFraction;          // Fraction of train data to use for each Tree
    private int numberOfVariables;         // Number of variables to consider at each tree-node
    private int numberOfTrees;             // The number of decision trees in the ensemble
    private int randomSeed;                // Seed of the Random number generator
    // -----------
    private RandomForest randomForest;

    // *********************************************************\
    // *             Random Forest Classification              *
    // *********************************************************/
    public void train() throws LearnerException
    {
        try
        {
            int   goalClasses;
            int []attributeDescription;
            double [][]trainInstances;
            RandomForest randomForest;

            // Make the training dataset and a description of the instances' Attributes.
            goalClasses = getGoalClasses();
            attributeDescription = makeAttributeDescription();
            trainInstances = makeTrainInstances();

            // Create and configure the RandomForest with the given parameters and dataset.
            randomForest = new RandomForest();
            randomForest.setGoalClasses(goalClasses);
            randomForest.setAttributeDescription(attributeDescription);
            randomForest.setTrainInstances(trainInstances);
            randomForest.setNumberOfTrees(this.numberOfTrees);
            randomForest.setMinObjects(this.minObjects);
            randomForest.setNumberOfVariables(this.numberOfVariables);
            randomForest.setTrainFraction(this.trainFraction);
            randomForest.setMaxDepth(this.maxDepth);
            randomForest.setRandomSeed(this.randomSeed);

            // Train the Trees of the RandomForest.
            randomForest.train();

            this.randomForest = randomForest;
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }

    private double [][]makeTrainInstances() throws LearnerException, DataModelException
    {
        double [][]trainInstances;
        double []instanceGoal;
        AttributeDouble goalAttribute;
        int instanceLength;

        // Collect the instances in a double [][]. Convert the goal values to their corresponding goal class values.
        goalAttribute = (AttributeDouble)this.dataModel.getAttribute(this.dataModel.getLearningProperty().getGoalIndex());
        instanceLength = this.trainData.getInstance(0).size();
        trainInstances = new double[this.trainData.getNumberOfInstances()][];
        for(int i=0; i<trainInstances.length; i++)
        {
            trainInstances[i] = this.trainData.getInstance(i).toArray();
            instanceGoal      = new double[instanceLength+1];
            System.arraycopy(trainInstances[i], 0, instanceGoal, 0, trainInstances[i].length);
            instanceGoal[instanceLength] = goalAttribute.getGoalClass(this.trainData.getGoal(i));
            trainInstances[i] = instanceGoal;
        }

        return trainInstances;
    }

    private int getGoalClasses() throws DataModelException
    {
        return this.attgoal.getNumberOfGoalClasses();
    }

    private int []makeAttributeDescription() throws DataModelException
    {
        int []attributeDescription;
        AttributeDouble attribute;

        // For each Attribute, determine if its categorical / continuous or not active.
        attributeDescription = new int[this.dataModel.getAttributeCount()];
        for(int i=0; i<this.dataModel.getAttributeCount(); i++)
        {
            attribute = (AttributeDouble)this.dataModel.getAttribute(i);
            if (attribute.getIsActive())
            {
                if      (attribute.hasProperty(Attribute.PROPERTY_CATEGORICAL)) attributeDescription[i] = attribute.getNumberOfCategories();
                else if (attribute.hasProperty(Attribute.PROPERTY_CONTINUOUS))  attributeDescription[i] = 0;
                else                                                            attributeDescription[i] = -1;
            }
            else attributeDescription[i] = -1;
        }

        return  attributeDescription;
    }

    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        return this.randomForest.classify(instance.toArray(), confidence);
    }

    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Object instances not supported.");
    }

    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        if (this.numberOfVariables == 0)
        {
            this.numberOfVariables = (int)Math.round(Math.sqrt(this.dataModel.getNumberOfActiveAttributes()));
        }
    }

    public Presenter getTrainSet()
    {
        return(this.trainData);
    }

    public void setTrainSet(Presenter _instances)
    {
        this.trainData = _instances;
        this.dataModel = _instances.getDataModel();
    }

    public boolean isSupervised()
    {
        return(true);
    }

    // *********************************************************\
    // *               Parameter and Model Access              *
    // *********************************************************/
    public void setTrainFraction(double trainFraction)
    {
        this.trainFraction = trainFraction;
    }

    public void setMinObjects(int minObjects)
    {
        this.minObjects = minObjects;
    }

    public void setNumberOfTrees(int numberOfTrees)
    {
        this.numberOfTrees = numberOfTrees;
    }

    public void setNumberOfVariables(int numberOfVariables)
    {
        this.numberOfVariables = numberOfVariables;
    }

    public void setMaxDepth(int maxDepth)
    {
        this.maxDepth = maxDepth;
    }

    public void setRandomSeed(int randomSeed)
    {
        this.randomSeed = randomSeed;
    }

    public double getOutOfBagError()
    {
        return this.randomForest.getOutOfBagError();
    }

    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        // Set input/output datamodel of the right type
        super.init();
    }

    public void cleanUp() throws DataFlowException
    {
        this.randomForest = null;
    }

    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        checkClassifierDataModelFit(dataModel, false, true, false);
    }

    // *********************************************************\
    // *          Decision Tree Classifier Creation            *
    // *********************************************************/
    public RandomForestClassifier()
    {
        super();
        this.name        = "Random Forest";
        this.description = "Random Forest ensemble of decision trees";
    }
}