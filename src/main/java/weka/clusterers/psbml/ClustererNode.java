package weka.clusterers.psbml;

import java.io.Serializable;

import weka.clusterers.AbstractClusterer;
import weka.core.Instance;
import weka.core.Instances;

public class ClustererNode 
{
    private AbstractClusterer clusterer;
    private Instances dataSet;

    public double cluster(Instance instance) throws Exception
    {
        double []weights;
        double minWeight;

        // Return smallest (normalized) distance to any centroid.
        weights = this.clusterer.distributionForInstance(instance);
        minWeight = Double.MAX_VALUE;
        for(int i=0; i<weights.length; i++)
            if (weights[i] < minWeight) minWeight = weights[i];

        return minWeight;
    }

    public void train() throws Exception
    {
        this.clusterer.buildClusterer(this.dataSet);
    }

    public void setData(Instances dataSet)
    {
        this.dataSet = dataSet;
    }

    public Instances getData()
    {
        return this.dataSet;
    }

    public void setClusterer(AbstractClusterer clusterer) throws Exception
    {
        this.clusterer = (AbstractClusterer)AbstractClusterer.makeCopy(clusterer);
    }
}
