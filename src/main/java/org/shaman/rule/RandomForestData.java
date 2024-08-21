package org.shaman.rule;

/**
 * @author Johan Kaers
 */
public interface RandomForestData
{
    double getInstanceValueForAttribute(int instanceIndex, int attributeIndex);

    double []getInstance(int instanceIndex);

    int getInstanceClass(int instanceIndex);

    int getNumberOfInstances();
}
