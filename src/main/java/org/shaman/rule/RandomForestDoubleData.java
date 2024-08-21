package org.shaman.rule;

import java.io.Serializable;

/**
 * @author Johan Kaers
 */
public class RandomForestDoubleData implements RandomForestData, Serializable
{
    private double [][]trainInstances;     // Set of instances to train tree on

    public RandomForestDoubleData(double [][]trainInstances)
    {
        this.trainInstances = trainInstances;
    }

    public double []getInstance(int instanceIndex)
    {
        return this.trainInstances[instanceIndex];
    }

    public double getInstanceValueForAttribute(int instanceIndex, int attributeIndex)
    {
        return this.trainInstances[instanceIndex][attributeIndex];
    }

    public int getInstanceClass(int instanceIndex)
    {
        return (int)this.trainInstances[instanceIndex][this.trainInstances[0].length-1];
    }

    public int getNumberOfInstances()
    {
        return this.trainInstances.length;
    }
}
