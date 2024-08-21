package org.shaman.rule;

import java.io.Serializable;

/**
 * @author Johan Kaers
 */
public class RandomForestByteData implements RandomForestData, Serializable
{
    private byte [][]trainInstances;

    public RandomForestByteData(byte [][]trainInstances)
    {
        this.trainInstances = trainInstances;
    }

    public double []getInstance(int instanceIndex)
    {
        double []instance;

        instance = new double[this.trainInstances[instanceIndex].length];
        for(int i=0; i<this.trainInstances[instanceIndex].length; i++) instance[i] = (double)trainInstances[instanceIndex][i];

        return instance;
    }

    public double getInstanceValueForAttribute(int instanceIndex, int attributeIndex)
    {
        return (double)this.trainInstances[instanceIndex][attributeIndex];
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
