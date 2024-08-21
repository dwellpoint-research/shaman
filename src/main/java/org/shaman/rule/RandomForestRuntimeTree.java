package org.shaman.rule;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * @author Johan Kaers
 */
public class RandomForestRuntimeTree implements Serializable
{
    // Random Forest model
    private byte []conditions;                       // [i] Condition of TreeNode i
    private int  []attributeIdx;                     // [i] Index of instance value to apply condition on in TreeNode i
    private int  []valueIdx;                         // [i] Index in values[] of the value to apply condition with in TreeNode i
    private int  []classDistributionIdx;             // [i] Index in classDistributions[] of the distribution of goal class of all instance passing through node i
    private int  [][]branches;                       // [i] Indices of the branch nodes of node i
    private double []values;                         // Distinct condition values referenced by valueIdx[]
    private double [][]classDistributions;           // Distinct classDistributions referenced by classDistributionIdx[]

    public void saveState(ObjectOutputStream objectOutputStream) throws IOException
    {
        // Save the optimized Random Forest model.
        objectOutputStream.writeObject(this.conditions);
        objectOutputStream.writeObject(this.attributeIdx);
        objectOutputStream.writeObject(this.valueIdx);
        objectOutputStream.writeObject(this.classDistributionIdx);
        objectOutputStream.writeObject(this.branches);
        objectOutputStream.writeObject(this.values);
        objectOutputStream.writeObject(this.classDistributions);
    }

    public void loadState(ObjectInputStream objectInputStream) throws IOException
    {
        try
        {
            // Read the model arrays.
            this.conditions = (byte [])objectInputStream.readObject();
            this.attributeIdx = (int [])objectInputStream.readObject();
            this.valueIdx = (int [])objectInputStream.readObject();
            this.classDistributionIdx = (int [])objectInputStream.readObject();
            this.branches = (int [][])objectInputStream.readObject();
            this.values = (double [])objectInputStream.readObject();
            this.classDistributions = (double [][])objectInputStream.readObject();
        }
        catch(ClassNotFoundException ex) { throw new IOException(ex); }
    }

    /**
     * Traverse through the decision tree, matching conditions until a lead node is reached.
     * @param instance The instance to classify
     * @return The class distribution at the leaf node.
     */
    public double []findMatchingLeaf(double []instance)
    {
        int leafNode, matchNode, childNode;

        leafNode = -1;

        // Recurse down the tree, matching with a branch at each level.
        matchNode = 0;
        while(leafNode == -1)
        {
            if (this.branches[matchNode] != null)
            {
                childNode = findMatchingBranch(matchNode, instance);
                if (childNode != -1)
                    matchNode = childNode;
                else leafNode = matchNode;
            }
            // Leaf node found when there are no branches found.
            else leafNode = matchNode;
        }

        // Return class distribution in the leaf node
        return this.classDistributions[this.classDistributionIdx[leafNode]];
    }

    private int findMatchingBranch(int nodeIdx, double []instance)
    {
        int matchIndex;

        matchIndex = -1;
        if (this.branches[nodeIdx] != null)
        {
            // Try the conditions of each branch until one matched. Return its index.
            for(int i=0; i<this.branches[nodeIdx].length && matchIndex == -1; i++)
            {
                if (apply(this.branches[nodeIdx][i], instance)) matchIndex = i;
            }
        }

        if (matchIndex != -1) return this.branches[nodeIdx][matchIndex];
        else                  return -1;
    }

    private boolean apply(int nodeIdx, double []instance)
    {
        boolean match;
        double attributeValue, value;

        // Apply the condition of the given node with the given instance.
        match          = false;
        value          = this.values[this.valueIdx[nodeIdx]];
        attributeValue = instance[this.attributeIdx[nodeIdx]];
        switch ((int)this.conditions[nodeIdx])
        {
            case TreeNode.IS_EQUAL:     match = attributeValue == value; break;
            case TreeNode.IS_NOT_EQUAL: match = attributeValue != value; break;
            case TreeNode.IS_GREATER:   match = attributeValue > value; break;
            case TreeNode.IS_SMALLER:   match = attributeValue < value; break;
            case TreeNode.IS_GREATER_EQUAL: match = attributeValue >= value; break;
            case TreeNode.IS_SMALLER_EQUAL: match = attributeValue <= value; break;
        }

        return match;
    }

    /**
     * Convert a TreeNode trained by the RandomForestTree to this more efficient runtime version.
     * @param root The root of the decision tree.
     */
    public void make(TreeNode root)
    {
        Integer numClasses;
        LinkedHashMap<Double, Integer> conditionValues;
        LinkedHashMap<String, double []> classDistributionValues;
        LinkedHashMap<String, Integer> classDistributionLookup;
        LinkedHashMap<TreeNode, Integer> nodeIds;

        // Collect the distinct values in the conditions and distributions throughout the tree.
        conditionValues = new LinkedHashMap<Double, Integer>();
        classDistributionLookup = new LinkedHashMap<String, Integer>();
        classDistributionValues = new LinkedHashMap<String, double []>();
        nodeIds = new LinkedHashMap<TreeNode, Integer>();
        collectTreeData(root, conditionValues, classDistributionLookup, classDistributionValues, nodeIds);
        numClasses = root.getClassDistribution().length;

        //System.out.println("Tree with "+nodeIds.size()+" nodes, "+conditionValues.size()+" values and "+classDistributionLookup.size()+" distributions, "+numClasses+" classes.");

        byte []conditions;
        int  []valueIdx;
        int  []attributeIdx;
        int  []classDistributionIdx;
        int  [][]branches;
        double []values;
        double [][]classDistributions;

        // Put data in primitive arrays
        conditions = new byte[nodeIds.size()];
        attributeIdx = new int[nodeIds.size()];
        valueIdx   = new int[nodeIds.size()];
        branches   = new int[nodeIds.size()][];
        classDistributionIdx = new int[nodeIds.size()];
        values     = new double[conditionValues.size()];
        classDistributions = new double[classDistributionLookup.size()][];

        int idx;

        // Conditions, class distributions and branches
        idx = 0;
        for(TreeNode node: nodeIds.keySet())
        {
            conditions[idx] = (byte)node.getCondition();
            attributeIdx[idx] = node.getAttributeIndex();
            valueIdx[idx]   = conditionValues.get(node.getValue());
            classDistributionIdx[idx] = classDistributionLookup.get(makeClassDistributionKey(node.getClassDistribution()));
            if (node.getBranches() != null)
            {
                branches[idx] = new int[node.getBranches().length];
                for(int i=0; i<branches[idx].length; i++) branches[idx][i] = nodeIds.get(node.getBranches()[i]);
            }
            else branches[idx] = null;
            idx++;
        }

        // Condition values
        idx = 0;
        for(Double value: conditionValues.keySet()) values[idx++] = value;

        // Class distributions
        idx = 0;
        for(double []classDistributionValue: classDistributionValues.values()) classDistributions[idx++] = classDistributionValue;

        this.conditions = conditions;
        this.valueIdx = valueIdx;
        this.attributeIdx = attributeIdx;
        this.branches = branches;
        this.values = values;
        this.classDistributions = classDistributions;
        this.classDistributionIdx = classDistributionIdx;
    }

    private void collectTreeData(TreeNode node, LinkedHashMap<Double, Integer> conditionValues, LinkedHashMap<String, Integer> classDistributions, LinkedHashMap<String, double []> classDistributionValues, LinkedHashMap<TreeNode, Integer> nodeIds)
    {
        // Assign an index to each node in the Tree.
        nodeIds.put(node, nodeIds.size());

        // And to each distinct value occurring in a condition.
        if (!conditionValues.containsKey(node.getValue())) conditionValues.put(node.getValue(), conditionValues.size());

        // Also keep track of the distinct class-distributions.
        String cdKey = makeClassDistributionKey(node.getClassDistribution());
        if (!classDistributions.containsKey(cdKey))
        {
            classDistributions.put(cdKey, classDistributions.size());
            classDistributionValues.put(cdKey, node.getClassDistribution());
        }

        // Recurse down the tree collecting more data.
        if (node.getBranches() != null)
        {
            for(TreeNode branch: node.getBranches())
                if (branch != null) collectTreeData(branch, conditionValues, classDistributions, classDistributionValues, nodeIds);
        }
    }

    private String makeClassDistributionKey(double []classDistribution)
    {
        StringBuilder cdKey;

        // Append the values in the class distribution of the TreeNode to get a unique key. The leaf nodes (almost half of the nodes) have just a few distinct distributions.
        cdKey = new StringBuilder();
        for(int i=0; i<classDistribution.length; i++) cdKey.append("#").append(classDistribution[i]);

        return cdKey.toString();
    }
}
