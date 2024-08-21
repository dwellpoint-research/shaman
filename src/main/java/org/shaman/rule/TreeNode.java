/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2013 Shaman Research                   *
\*********************************************************/
package org.shaman.rule;

import java.io.Serializable;

public class TreeNode implements Serializable
{
    public static final int IS_EQUAL         = 0;
    public static final int IS_NOT_EQUAL     = 1;
    public static final int IS_GREATER       = 2;
    public static final int IS_SMALLER       = 3;
    public static final int IS_GREATER_EQUAL = 4;
    public static final int IS_SMALLER_EQUAL = 5;

    private int        attributeIndex;            // Index of instance value of apply condition on
    private int        condition;                 // Condition to apply
    private double     value;                     // Value to apply condition with
    private double   []classDistribution;         // Distribution of goal class of all instance passing through this node
    private TreeNode []branches;                  // Branches of this node

    public TreeNode()
    {
        this.condition = -1;
    }

    public TreeNode(int attributeIndex, int condition, double value)
    {
        this.attributeIndex = attributeIndex;
        this.condition = condition;
        this.value = value;
    }

    private boolean apply(double []instance)
    {
        boolean match;
        double attributeValue;

        // Does the condition of this node match the given instance?
        match = false;
        attributeValue = instance[this.attributeIndex];
        switch (this.condition)
        {
            case IS_EQUAL:     match = attributeValue == this.value; break;
            case IS_NOT_EQUAL: match = attributeValue != this.value; break;
            case IS_GREATER:   match = attributeValue > this.value; break;
            case IS_SMALLER:   match = attributeValue < this.value; break;
            case IS_GREATER_EQUAL: match = attributeValue >= this.value; break;
            case IS_SMALLER_EQUAL: match = attributeValue <= this.value; break;
        }

        return match;
    }

    private TreeNode findMatchingBranch(double []instance)
    {
        int matchIndex;

        matchIndex = -1;
        if (this.branches != null)
        {
            for(int i=0; i<this.branches.length && matchIndex == -1; i++)
            {
                if (this.branches[i].apply(instance)) matchIndex = i;
            }
        }

        if (matchIndex != -1) return this.branches[matchIndex];
        else                  return null;
    }

    public TreeNode findMatchingLeaf(double []instance)
    {
        TreeNode leafNode, matchNode, childNode;

        leafNode = null;

        // Only starting from the root of a tree...
        if (this.condition == -1)
        {
            // Recurse down the tree, matching with a branch at each level.
            matchNode = this;
            while(leafNode == null)
            {
                if (matchNode.branches != null)
                {
                    childNode = matchNode.findMatchingBranch(instance);
                    if (childNode != null)
                         matchNode = childNode;
                    else leafNode = matchNode;
                }
                // Leaf node found when there are no branches found.
                else leafNode = matchNode;
            }
        }

        return leafNode;
    }

    public void setBranches(TreeNode []branches)
    {
        this.branches = branches;
    }

    public void setClassDistribution(double []classDistribution)
    {
        this.classDistribution = classDistribution;
    }

    public double getValue()
    {
        return this.value;
    }

    public int getAttributeIndex()
    {
        return this.attributeIndex;
    }

    public int getCondition()
    {
        return this.condition;
    }

    public double []getClassDistribution()
    {
        return this.classDistribution;
    }

    public TreeNode []getBranches()
    {
        return this.branches;
    }
}
