/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2013 Shaman Research                   *
 \*********************************************************/
package org.shaman.rule;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandomForest implements Serializable
{
    private int minObjects;                // Minimum number of instances in leaf nodes
    private int maxDepth;                  // Maximum number of levels in the tree
    private double trainFraction;          // Fraction of train data to use for each Tree
    private int numberOfVariables;         // Number of variables to consider at each tree-node
    private int numberOfTrees;             // The number of decision trees in the ensemble
    private int randomSeed;                // Seed for Random number generator

    private int      []attributeDescription;   // [i] = -1: inactive, [i] = 0 : continuous value, [i] = n > 0 : categorical value with n classes
    private int        goalClasses;            // Number of classes in the goal attribute
    private RandomForestData data;             // Set of instances to train tree on
    // ----------
    private double     outOfBagError;       // Estimate of classification error using the out-of-bag instances.
    private List<RandomForestTree> trees;   // The classification trees making up the random forest.

    // *********************************************************\
    // *                   Persistence                         *
    // *********************************************************/
    public void saveState(ObjectOutputStream objectOutputStream) throws IOException
    {
        // Save instance description but not the train data
        objectOutputStream.writeObject(this.attributeDescription);
        objectOutputStream.writeInt(this.goalClasses);
        // Save Tree parameters
        objectOutputStream.writeInt(this.numberOfTrees);
        objectOutputStream.writeDouble(this.trainFraction);
        objectOutputStream.writeInt(this.maxDepth);
        objectOutputStream.writeInt(this.minObjects);
        objectOutputStream.writeInt(this.numberOfVariables);
        objectOutputStream.writeInt(this.randomSeed);
        // Save Trees themselves
        for(RandomForestTree tree: this.trees) tree.saveState(objectOutputStream);
    }

    public void loadState(ObjectInputStream objectInputStream) throws IOException
    {
        try
        {
            // Read instance description and tree parameters
            this.attributeDescription = (int [])objectInputStream.readObject();
            this.goalClasses = objectInputStream.readInt();
            this.numberOfTrees = objectInputStream.readInt();
            this.trainFraction = objectInputStream.readDouble();
            this.maxDepth = objectInputStream.readInt();
            this.minObjects = objectInputStream.readInt();
            this.numberOfVariables = objectInputStream.readInt();
            this.randomSeed = objectInputStream.readInt();

            List<RandomForestTree> trees;
            RandomForestTree tree;

            // Read the trees themselves.
            trees = new LinkedList<RandomForestTree>();
            for(int i=0; i<this.numberOfTrees; i++)
            {
                tree = new RandomForestTree();
                tree.loadState(objectInputStream);
                trees.add(tree);
            }
            this.trees = trees;
        }
        catch (ClassNotFoundException ex) { throw new IOException(ex); }
    }

    // *********************************************************\
    // *             Random Forest Classification              *
    // *********************************************************/
    public void train()
    {
        List<RandomForestTree> randomForestTrees;
        Random random;

        // Collect the Decision Trees of the Random Forest.
        randomForestTrees = new LinkedList<RandomForestTree>();

        // Initialize the shared Random generator with the seed in case exact reproducability is wanted.
        if (this.randomSeed != 0)
            random = new Random(this.randomSeed);
        else random = new Random();

        int [][]outOfBagClass;

        // Calculate Out-of-bag error estimate during training
        outOfBagClass = new int[this.data.getNumberOfInstances()][this.goalClasses];

        // Create the Classification Trees
        for(int i=0; i<this.numberOfTrees; i++)
        {
            RandomForestTree randomForestTree;

            // Train another Tree with the given parameters and training data.
            randomForestTree = new RandomForestTree();
            randomForestTree.setAttributeDescription(this.attributeDescription);
            randomForestTree.setTrainInstances(this.data);
            randomForestTree.setGoalClasses(this.goalClasses);
            randomForestTree.setMinObjects(this.minObjects);
            randomForestTree.setNumberOfVariables(this.numberOfVariables);
            randomForestTree.setTrainFraction(this.trainFraction);
            randomForestTree.setMaxDepth(this.maxDepth);
            randomForestTree.setOutOfBagClass(outOfBagClass);
            randomForestTree.setRandom(random);
            randomForestTree.trainTree();

            System.out.println("Trained Tree "+(i+1)+" of "+this.numberOfTrees);

            randomForestTrees.add(randomForestTree);
        }

        // Done. Remember Trees for classification.
        this.trees = randomForestTrees;

        int  maxClassCount, maxClass;
        double cntError;
        boolean classTie;

        // Calculate the out-of-bag error estimate: fraction of instances where the most-frequently chosen class when out-of-bag is wrong.
        cntError = 0;
        for(int i=0; i<this.data.getNumberOfInstances(); i++)
        {
            // Find the class that was predicted most for this instance.
            maxClass      = -1;
            maxClassCount = Integer.MIN_VALUE;
            for(int j=0; j<outOfBagClass[j].length; j++)
            {
                if (outOfBagClass[i][j] > maxClassCount) { maxClass = j; maxClassCount = outOfBagClass[i][j]; }
            }

            // Are all classes predicted just as often? Then this doesn't count as an error.
            classTie = true;
            for(int j=0; j<outOfBagClass[i].length; j++)
            {
                if (j<outOfBagClass[i].length-1 && outOfBagClass[i][j] != outOfBagClass[i][j+1]) classTie = false;
            }

            if (!classTie)
            {
                if (this.data.getInstanceClass(i) != maxClass) cntError++;
            }
        }
        this.outOfBagError = cntError / outOfBagClass.length;
    }

    public int classify(double []instance, double []confidence)
    {
        int modeClass;

        modeClass = -1;

        double []sumConfidence;
        double []treeConfidence;
        int vote, cntVotes;
        double maxConfidence;

        // Calculate the average of the confidences over all Trees
        sumConfidence = new double[this.goalClasses];
        treeConfidence = new double[this.goalClasses];
        cntVotes = 0;

        // Classify the instance with all the trees in the forest. Sum the class distributions at leaf node where the instance ends up.
        for(RandomForestTree tree: this.trees)
        {
            vote = tree.classify(instance, treeConfidence);
            if (vote != -1)
            {
                for(int i=0; i<treeConfidence.length; i++) sumConfidence[i] += treeConfidence[i];
                cntVotes++;
            }
        }
        if (cntVotes > 0)
        {
            // Pick the class with highest occurence in the lead nodes as output.
            maxConfidence = Double.NEGATIVE_INFINITY;
            for(int i=0; i<sumConfidence.length; i++) sumConfidence[i] /= cntVotes;
            for(int i=0; i<sumConfidence.length; i++)
            {
                if (sumConfidence[i] >= maxConfidence)
                {
                    maxConfidence = sumConfidence[i];
                    modeClass = i;
                }
            }
            // Also copy the class-confidence over the entire forrest?
            if (confidence != null)
                for(int i=0; i<sumConfidence.length; i++) confidence[i] = sumConfidence[i];
        }

        return modeClass;
    }

    public double getOutOfBagError()
    {
        return this.outOfBagError;
    }

    // *********************************************************\
    // *                Parameter configuration                *
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

    public void setAttributeDescription(int []attributeDescription)
    {
        this.attributeDescription = attributeDescription;
    }

    public void setGoalClasses(int goalClasses)
    {
        this.goalClasses = goalClasses;
    }

    public void setTrainInstances(double [][]trainInstances)
    {
        this.data = new RandomForestDoubleData(trainInstances);
    }

    public void setTrainInstances(RandomForestData randomForestData)
    {
        this.data = randomForestData;
    }
}
