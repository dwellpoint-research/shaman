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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class RandomForestTree implements Serializable
{
    // How many (randomly picked) variables to consider at each tree noes
    public static final int NUMBER_OF_VARIABLES_ALL  = 0;        // All: Forest behaves as Bagger ensemble, Tree behaves al plain DecisionTree
    public static final int NUMBER_OF_VARIABLES_SQRT = 1;        // Random Forest: at each decision point, only look at the square root of the total number of attributes

    // Train data
    private int []attributeDescription;    // [i] = -1: inactive, [i] = 0 : continuous value, [i] = n > 0 : categorical value with n classes
    private int goalClasses;               // Number of classes in the goal attribute
    private RandomForestData data;         // Set of instances to train tree on
    private int [][]outOfBagClass;         // [i][j] = number of time class [j] is predicted when [i] is out-of-bag for a tree.
    // Tree parameters
    private int minObjects;                // Minimum number of instances in leaf node
    private int numberOfVariables;         // Number of variables to consider at each tree-node
    private double trainFraction;          // Fraction of train instances to sub-select for training each Tree
    private int maxDepth;                  // Maximum number of levels in the tree
    private Random random;                 // Random number generator
    // Runtime version of tree
    private RandomForestRuntimeTree tree;  // Efficient tree implementation only used for classification.

    // *********************************************************\
    // *                   Persistence                         *
    // *********************************************************/
    public void saveState(ObjectOutputStream objectOutputStream) throws IOException
    {
        this.tree.saveState(objectOutputStream);
    }

    public void loadState(ObjectInputStream objectInputStream) throws IOException
    {
        this.tree = new RandomForestRuntimeTree();
        this.tree.loadState(objectInputStream);
    }

    // *********************************************************\
    // *             Decision Tree Classification              *
    // *********************************************************/
    public int classify(double []instance, double []confidence)
    {
        return classify(null, instance, confidence);
    }

    private int classify(TreeNode root, double []instance, double []confidence)
    {
        TreeNode   matchingLeaf;
        double []classDistribution;

        // Traverse through the tree matching node conditions with the instance until a leaf node is reached.
        if (root != null)
        {
            // Use the given Tree to estimate out-of-bag error during training.
            matchingLeaf = root.findMatchingLeaf(instance);
            classDistribution = matchingLeaf.getClassDistribution();
        }
        else
        {
            // Or use the equivalent runtime version.
            classDistribution = this.tree.findMatchingLeaf(instance);
        }

        int    leafClass;
        double pMaxClass;

        // Return the class with highest probability in this leaf node.
        pMaxClass = Double.NEGATIVE_INFINITY;
        leafClass = 0;
        for (int i=0; i<classDistribution.length; i++) if (classDistribution[i] > pMaxClass) { leafClass = i; pMaxClass = classDistribution[i]; }

        // And optionally return the class distribution as confidence values.
        if (confidence != null)
        {
            for (int i=0; i<confidence.length; i++) confidence[i] = classDistribution[i];
        }

        return(leafClass);
    }

    // *********************************************************\
    // *                  Decision Tree Training               *
    // *********************************************************/
    public void trainTree()
    {
        int          i;
        List<Integer> instances, outOfBagInstances;
        List<Integer> attributes;

        // Make sure there's a Random Generator. Shared with the other trees when part of a Random Forest.
        if (this.random == null) this.random = new Random();

        // Minimum number of objects in leaf node
        if (this.minObjects == 0) this.minObjects = 1;

        // Fraction of instances in train-data to pick for training
        if (this.trainFraction == 0) this.trainFraction = 1.0;

        // Make a list the Attributes to choose from. To start, include all but ignore the inactive ones (like the class Attribute).
        attributes = new ArrayList<Integer>(this.attributeDescription.length);
        for (i=0; i<this.attributeDescription.length; i++)
            if (this.attributeDescription[i] >= 0) attributes.add(i);

        // Pick the instances to train on.
        instances         = new ArrayList<Integer>(this.data.getNumberOfInstances());
        outOfBagInstances = new ArrayList<Integer>(this.data.getNumberOfInstances());
        for (i=0; i<this.data.getNumberOfInstances(); i++)
        {
            // Bagging ensemble: select a fraction of all instances in the Train set. // Remember the other ones as out-of-bag instances.
            if (this.trainFraction > 0 && this.trainFraction < 1.0)
            {
                if (this.random.nextDouble() <= this.trainFraction)
                    instances.add(i);
                else outOfBagInstances.add(i);
            }
            else
            {
                // Or just include them all...
                instances.add(i);
            }
        }

        TreeNode tree;

        // Create root node of the Classification Tree.
        tree = new TreeNode();
        tree.setClassDistribution(getClassDistribution(instances));

        // Recursively derive the tree branches.
        findBranches(tree, 1, instances, attributes);

        // For all instances that weren't selected (the out-of-bag instances), predict and remember their classification for the out-of-bag error estimate of the Random Forest.
        if (this.outOfBagClass != null)
        {
            for(Integer oobInstance: outOfBagInstances)
            {
                // Increase the counter for the instance / classification combination.
                this.outOfBagClass[oobInstance][classify(tree, this.data.getInstance(oobInstance), null)]++;
            }
        }

        RandomForestRuntimeTree runtimeTree;

        // Convert the tree into a more efficient runtime version.
        runtimeTree = new RandomForestRuntimeTree();
        runtimeTree.make(tree);
        this.tree = runtimeTree;
    }

    private void findBranches(TreeNode treeNode, int depth, List<Integer> instances, List<Integer> attributes)
    {
        List<Integer>   selectedAttributes;

        // Pick the Attributes to consider in the information gain calculations
        selectedAttributes = new ArrayList<Integer>(attributes.size());

        // Use them all. Act like a plain decision tree or part of a Bagging ensemble
        if (this.numberOfVariables == NUMBER_OF_VARIABLES_ALL) selectedAttributes.addAll(attributes);
        else
        {
            int cntPick;
            boolean []picked;
            int pickIdx;

            // Random Forest tree: randomly pick sqrt() of the available number of attributes
            cntPick = (int)Math.ceil(Math.sqrt(attributes.size()));
            if (cntPick == attributes.size()) selectedAttributes.addAll(attributes);
            else
            {
                picked = new boolean[attributes.size()];
                for(int i=0; i<cntPick; i++)
                {
                    pickIdx = this.random.nextInt(picked.length);
                    while(picked[pickIdx]) { pickIdx++; pickIdx %= picked.length; }
                    picked[pickIdx] = true;
                }
                for(int i=0; i<picked.length; i++) if (picked[i]) selectedAttributes.add(attributes.get(i));
            }
        }

        int             branchAttributeIndex, maxIndex;
        double          branchInfoGain, maxInfoGain;
        List<TreeNode> branchConditions, maxBranchConditions;
        List<Integer>  []branchInstances;
        List<Integer>  []maxBranchInstances;

        // Find the attribute with the highest information gain when the instances are split-up accoring to its condition.
        maxBranchInstances  = null;
        maxBranchConditions = null;
        maxIndex = -1;
        maxInfoGain = Double.NEGATIVE_INFINITY;
        for (int i=0; i<selectedAttributes.size(); i++)
        {
            // Calculate information-gain of given Attribute
            branchAttributeIndex = selectedAttributes.get(i);
            branchConditions     = new LinkedList<TreeNode>();
            if      (this.attributeDescription[branchAttributeIndex] > 0)
            {
                // Infogain by splitting up on categorical value
                branchInstances = new List[this.attributeDescription[branchAttributeIndex]];
                branchInfoGain  = infoGainCategorical(instances, branchAttributeIndex, branchInstances, branchConditions);
            }
            else
            {
                // Infogain by splitting continuous values in 2 classes, over/under threshold value.
                branchInstances = new List[2];
                branchInfoGain = infoGainContinuous(instances, branchAttributeIndex, branchInstances, branchConditions);
            }
            if (branchInfoGain > maxInfoGain)
            {
                // Remember Attribute with highest gain. Branch on this Attribute
                maxIndex           = branchAttributeIndex;
                maxInfoGain        = branchInfoGain;
                maxBranchInstances = branchInstances;
                maxBranchConditions = branchConditions;
            }
        }

        TreeNode    []branches;
        List<Integer> attributesForBranches;
        boolean       recurse;
        double      []branchDistribution;

        // Remove the Attribute of this tree from the list of available Attributes
        attributesForBranches  = new ArrayList<Integer>(attributes);
        attributesForBranches.remove(attributesForBranches.indexOf(maxIndex));

        // Install the branches splitting up the most informative attribute.
        branches = new TreeNode[maxBranchConditions.size()];
        for (int i=0; i<branches.length; i++)
        {
            // Calculate the distribution of instances for the TreeNode
            branches[i] = maxBranchConditions.get(i);
            branchDistribution = getClassDistribution(maxBranchInstances[i]);
            branches[i].setClassDistribution(branchDistribution);

            // Continue down the branch if:
            // - the maximum tree depth is not exceeded
            // - the minimum number of instances is exceeded
            // - there are attributes to choose from left
            // - there are instances of different classes
            recurse =  (this.maxDepth == 0 || this.maxDepth > depth)
                    && maxBranchInstances[i].size() >= this.minObjects
                    && attributesForBranches.size() > 0;
            for (int j=0; (j<branchDistribution.length) && recurse; j++) if (branchDistribution[j] == 1.0) recurse = false;
            if (recurse)
            {
                findBranches(branches[i], depth+1, maxBranchInstances[i], attributesForBranches);
            }
        }
        treeNode.setBranches(branches);
    }

    private double []getClassDistribution(List<Integer> instances)
    {
        double []classDistribution;

        // Split the examples up according to their goal class. And calculate class probability distribution.
        classDistribution = new double[this.goalClasses];
        for (int i=0; i<instances.size(); i++)
        {
            // Add index of current example to list of its class.
            classDistribution[this.data.getInstanceClass(instances.get(i))]++;
        }
        for (int i=0; i<classDistribution.length; i++) classDistribution[i] = classDistribution[i] / instances.size();

        return classDistribution;
    }

    // *********************************************************\
    // *               Information Gain Criterion              *
    // *********************************************************/
    private double infoGainContinuous(List<Integer> instances, int attributeIndex, List<Integer>[] branchInstances, List<TreeNode> branchConditions)
    {
        HashSet<Double> distinctValues;
        Double []sortedValues;
        double infoGain, entropyAll;

        // Calculate the entropy of the entire instance set.
        entropyAll = entropy(instances);
        infoGain = 0;

        // Find all distinct values of the Attribute in the given instances
        distinctValues = new HashSet<Double>();
        for(Integer instanceIndex: instances)
            distinctValues.add(this.data.getInstanceValueForAttribute(instanceIndex, attributeIndex));

        // Continue when there's at least 2 value to split on...
        if (distinctValues.size() >= 2)
        {
            // Sort the distinct values.
            sortedValues = distinctValues.toArray(new Double[distinctValues.size()]);
            Arrays.sort(sortedValues);

            Integer instanceIndex;
            Double []splitValues;
            double instanceValue, split, maxSplit, maxInfoGain, infoGainSplit;
            List<Integer> []splitInstances, maxSplitInstances;

            // Try splitting up the instances. Remember the split with the highest infoGain.
            splitValues = makeSplitValues(sortedValues, instances, attributeIndex);
            maxInfoGain = Double.NEGATIVE_INFINITY;
            maxSplit = Double.NaN;
            maxSplitInstances = new List[2];
            splitInstances = new List[]{new ArrayList<Integer>(instances.size()), new ArrayList<Integer>(instances.size())};

            // For all split values.
            for(int i=0; i<splitValues.length; i++)
            {
                // Collect the instances lower / higher than the split.
                split = splitValues[i];
                splitInstances[0].clear();
                splitInstances[1].clear();
                for(int j=0; j<instances.size(); j++)
                {
                    instanceIndex = instances.get(j);
                    instanceValue = this.data.getInstanceValueForAttribute(instanceIndex, attributeIndex);
                    if (instanceValue < split)
                        splitInstances[0].add(instanceIndex);
                    else splitInstances[1].add(instanceIndex);
                }

                // Calculate the information gain obtained by splitting up the instances like this.
                infoGainSplit = entropyAll - ((entropy(splitInstances[0])*splitInstances[0].size())/instances.size() + (entropy(splitInstances[1])*splitInstances[1].size())/instances.size());

                // Remember the split value with the highest information gain.
                if (infoGainSplit > maxInfoGain)
                {
                    maxInfoGain = infoGainSplit;
                    maxSplit = split;
                    maxSplitInstances[0] = new ArrayList<Integer>(splitInstances[0]);
                    maxSplitInstances[1] = new ArrayList<Integer>(splitInstances[1]);
                }
            }

            if (!Double.isNaN(maxSplit))
            {
                // Create the 2 branches on the split-value with the highest information gain. Return the instances split-up like this as well.
                infoGain = maxInfoGain;
                branchInstances[0] = maxSplitInstances[0];
                branchInstances[1] = maxSplitInstances[1];
                branchConditions.add(new TreeNode(attributeIndex, TreeNode.IS_SMALLER, maxSplit));
                branchConditions.add(new TreeNode(attributeIndex, TreeNode.IS_GREATER_EQUAL, maxSplit));
            }
        }
        // All values are the same. So can't split up and gain information.
        else infoGain = 0;

        return infoGain;
    }

    private Double []makeSplitValues(Double []sortedValues, List<Integer> instances, int attributeIndex)
    {
        final int MAX_SPLIT = 20;
        List<Double> splitsList;

        splitsList = new LinkedList<Double>();
        if (sortedValues.length < MAX_SPLIT)
        {
            double value, nextValue, split;
            int distinctPos, step;

            step = 1;
            distinctPos = 1;
            while(distinctPos < sortedValues.length)
            {
                value = sortedValues[distinctPos-1];
                nextValue = sortedValues[distinctPos];
                split = (value+nextValue)/2;
                splitsList.add(split);

                distinctPos += step;
            }
        }
        else
        {
            TreeMap<Double, int []> distinctCount;
            double instanceValue;

            distinctCount = new TreeMap<Double, int []>();
            for(Double value: sortedValues) distinctCount.put(value, new int[]{0});
            for(Integer instanceIndex: instances)
            {
                instanceValue = this.data.getInstanceValueForAttribute(instanceIndex, attributeIndex);
                distinctCount.get(instanceValue)[0]++;
            }

            double step, stepLimit;
            double sumIns, value, nextValue;
            Iterator<Double> itValue;

            step = (double)instances.size() / MAX_SPLIT;
            sumIns = 0;
            stepLimit = step/2.0;
            itValue = distinctCount.keySet().iterator();
            while(itValue.hasNext() && splitsList.size() < MAX_SPLIT)
            {
                value = itValue.next();
                sumIns += distinctCount.get(value)[0];
                while(sumIns < stepLimit && itValue.hasNext())
                {
                    value = itValue.next();
                    sumIns += distinctCount.get(value)[0];
                }
                if (itValue.hasNext())
                {
                    stepLimit += step;
                    splitsList.add(value);
                }
            }
        }

        return splitsList.toArray(new Double[splitsList.size()]);
    }

    private double infoGainCategorical(List<Integer> instances, int attributeIndex, List<Integer>[] branchInstances, List<TreeNode> branchConditions)
    {
        int             instanceIndex;
        int             missingCategory, instanceCategory;
        double          attributeValue;
        int             cntAll;
        double        []cntBranch;
        double          infoGain, entropyAll, entropyPartitions;

        // Calculate the entropy of the entire instance set.
        entropyAll = entropy(instances);

        // Make empty sets that will contain the instances partitioned on their value at the given Attribute
        for (int i=0; i<branchInstances.length; i++)
        {
            branchInstances[i] = new ArrayList<Integer>(instances.size());

            // Create the conditions that select the various categories of the Attribute.
            branchConditions.add(new TreeNode(attributeIndex, TreeNode.IS_EQUAL, (double)i));
        }

        // Partition the instance in the branches according to the Attribute
        cntBranch = new double[branchInstances.length];
        cntAll    = 0;
        for (int i=0; i<instances.size(); i++)
        {
            instanceIndex = instances.get(i);

            // Find the category of the instance's value.
            attributeValue   = this.data.getInstanceValueForAttribute(instanceIndex, attributeIndex);
            instanceCategory = (int)attributeValue;

            // Add current example to it's branch's example list.
            if (instanceCategory != -1)
            {
                branchInstances[instanceCategory].add(instanceIndex);
                cntBranch[instanceCategory]++;
                cntAll++;
            }
        }

        // Calculate the entropy over the partitions
        entropyPartitions    = 0;
        for (int i=0; i<branchInstances.length; i++)
        {
            if (cntBranch[i] > 0)
            {
                entropyPartitions += (cntBranch[i] / cntAll) * entropy(branchInstances[i]);
            }
        }

        // Information gain is the difference between entropy of the entire set and the entropy over the partitions
        infoGain = entropyAll - entropyPartitions;

        return(infoGain);
    }

    private double entropy(List<Integer> instances)
    {
        int      instanceIndex, instanceClass;
        double   log2;
        double   cntAll;
        double []cntClass;
        double []pclass;
        double   entropy;

        log2 = Math.log(2.0);

        // Calculate the entropy of the given set of instances.
        cntClass = new double[this.goalClasses];
        pclass   = new double[this.goalClasses];
        for (int i=0; i<cntClass.length; i++) { pclass[i] = 0; cntClass[i] = 0; }
        cntAll = 0;
        for (int i=0; i<instances.size(); i++)
        {
            instanceIndex = instances.get(i);
            instanceClass = this.data.getInstanceClass(instanceIndex);
            cntClass[instanceClass]++;
            cntAll++;
        }
        for (int i=0; i<cntClass.length; i++) pclass[i] = cntClass[i] / cntAll;

        entropy = 0;
        for (int i=0; i<cntClass.length; i++)
        {
            if (pclass[i] != 0) entropy -= pclass[i]*(Math.log(pclass[i])/log2);
        }

        return(entropy);
    }

    // *********************************************************\
    // *                Parameter configuration                *
    // *********************************************************/
    /**
     * Set the minimum number of objects that need to be present for a branch to form.
     * @param minObjects Minimum number of Object needed for a branch.
     */
    public void setMinObjects(int minObjects)
    {
        this.minObjects = minObjects;
    }

    /**
     * Set the fraction of all training instances to use for training the tree.
     * @param trainFraction Fraction of instances to use. in ]0,1]
     */
    public void setTrainFraction(double trainFraction)
    {
        this.trainFraction = trainFraction;
    }

    /**
     * Set the variables to configure at each decision point. (0 = all, 1 = sqrt of available variables)
     * @param numberOfVariables
     */
    public void setNumberOfVariables(int numberOfVariables)
    {
        this.numberOfVariables = numberOfVariables;
    }

    /**
     * Set maximum levels in the decision tree.
     * @param maxDepth The maximum number of levels in the tree.
     */
    public void setMaxDepth(int maxDepth)
    {
        this.maxDepth = maxDepth;
    }

    /**
     * Set description of the instance / training vectors.
     * @param attributeDescription
     */
    public void setAttributeDescription(int []attributeDescription)
    {
        this.attributeDescription = attributeDescription;
    }

    /**
     * Set number of classes in the goal attribute.
     * @param goalClasses
     */
    public void setGoalClasses(int goalClasses)
    {
        this.goalClasses = goalClasses;
    }

    /**
     * Set the training data.
     * @param trainInstances
     */
    public void setTrainInstances(double [][]trainInstances)
    {
        this.data = new RandomForestDoubleData(trainInstances);
    }

    public void setTrainInstances(RandomForestData trainInstances)
    {
        this.data = trainInstances;
    }

    /**
     * Set the random number generator.
     * @param random
     */
    public void setRandom(Random random)
    {
        this.random = random;
    }

    public void setOutOfBagClass(int [][]outOfBagClass)
    {
        this.outOfBagClass = outOfBagClass;
    }
}
