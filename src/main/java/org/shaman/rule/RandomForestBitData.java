package org.shaman.rule;

import java.io.Serializable;
import java.util.BitSet;

/**
 * @author Johan Kaers
 */
public class RandomForestBitData implements RandomForestData, Serializable
{
    private BitSet[]instances;
    private int classIndex;

    public RandomForestBitData(BitSet []instances, int classIndex)
    {
        this.instances = instances;
        this.classIndex = classIndex;
    }

    public double getInstanceValueForAttribute(int instanceIndex, int attributeIndex)
    {
        return this.instances[instanceIndex].get(attributeIndex)?1:0;
    }

    public double[] getInstance(int instanceIndex)
    {
        double []instance;

        instance = new double[this.classIndex+1];
        for(int i=0; i<instance.length; i++)
        {
            instance[i] = this.instances[instanceIndex].get(i)?1:0;
        }

        return instance;
    }

    public int getInstanceClass(int instanceIndex)
    {
        return this.instances[instanceIndex].get(this.classIndex)?1:0;
    }

    public int getNumberOfInstances()
    {
        return this.instances.length;
    }
}
