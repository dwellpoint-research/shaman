package org.shaman.spatial;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import weka.core.Instance;
import weka.core.Instances;

public class NodeWorker implements Callable<NodeWorker>
{
    /**
     * Reorganize code for full distributed implementation
     * - Create interface merging logic from NodeWorker and ClassifierNode
     *   o training / testing / proportionate selection
     *   o communication between nodes
     *     + move local instances to neighbor for testing, test, return test results
     *     + get weighted instances from all nodes, perform weighted selection
     *     + synchronization of above operations for synchronous / epoched operation
     */

    public static final int WORK_TRAIN  = 1;    // Train the instances
    public static final int WORK_SELECT = 2;    // Test instances on neighbors. Select instances for next epoch.
    public static final int WORK_ASYNC  = 3;    // Train / Test on neighbors / Select instances / Replace instances

    private int            work;
    private ClassifierNode node;
    private List<ClassifierNode> neighbors;
    private SpatialEnsembleLearningThreaded selt;
    // ------
    private Random         random;
    private long           duration;

    public NodeWorker(ClassifierNode node, List<ClassifierNode> neighbors, SpatialEnsembleLearningThreaded selt)
    {
        this.node = node;
        this.neighbors = neighbors;
        this.random = new Random(System.currentTimeMillis() % this.hashCode());
        this.selt = selt;
    }

    public void setWork(int work)
    {
        this.work = work;
    }

    public NodeWorker call() throws Exception
    {
        long tbeg, tend, spent;

        tbeg = System.currentTimeMillis();

        try
        {
            // Execute either the Classification training or Instance selection step
            if      (this.work == WORK_TRAIN)  train();
            else if (this.work == WORK_SELECT) selectSpatialProportional();
                // Or do it all
            else if (this.work == WORK_ASYNC)
            {
                // Train the data-set on a other Classifier. Switch classifiers around when done.
                this.node.trainSafe();
                // Select instances
                selectSpatialProportional();
                // Switch instances to dataset
                this.node.setData(node.getSelectedInstances());
                // Re-schedule execution
                this.selt.recycleNodeWorker(this);
            }
        }
        catch(Exception ex)
        {
            // Debug... Multi-threading issues are tricky.
            ex.printStackTrace();

            // ? Re-throw to make sure the Threadpool kills off the failed thread.
            //throw(ex);
        }

        // Remember the time spent
        tend = System.currentTimeMillis();
        spent = tend-tbeg;
        this.duration += spent;

        return this;
    }

    public void train() throws Exception
    {
        this.node.train();
    }

    public long getTimeSpent()
    {
        Long spent = this.duration;
        this.duration = 0l;

        return(spent);
    }

    public ClassifierNode getNode() { return this.node; }

    // ***************************************************************\
    // * Replace with proportional selection from neighbor instances *
    // ***************************************************************/
    private void selectSpatialProportional() throws Exception
    {
        TreeSet<Instance>    weightOrderInstances;
        TreeMap<Double, Instance> sampleInstance;
        List<Instance>    zeroWeightInstances;
        double            weightSum;
        List<Instance>    replaceSelect;

        // Test the instances of this Node on the neighboring classifiers. Weight them accordingly.
        testInstancesOnNeighbors();

        // Collect all Instances from this Node and its neighboring nodes, ordered from high to low weight.
        weightOrderInstances = new TreeSet<Instance>(new InstanceWeightComparator());
        for(Instance instance: node.getData())
            weightOrderInstances.add(instance);
        for(ClassifierNode neighbor: neighbors)
            for(Instance instance: neighbor.getData())
                weightOrderInstances.add(instance);

        // Put instances in a TreeMap with as key the cumulative weight starting with highest weight instances
        sampleInstance      = new TreeMap<Double, Instance>();
        zeroWeightInstances = new LinkedList<Instance>();
        weightSum = 0;
        for(Instance instance: weightOrderInstances)
        {
            if (instance.weight() > 0)
            {
                weightSum += instance.weight();
                sampleInstance.put(weightSum, instance);
            }
            else
            {
                // Keep the (worst) Instances with weight 0 in a separate list... So they don't overwrite the sampleInstance of the last instances with weight > 0.
                zeroWeightInstances.add(instance);
            }
        }

        Instance selectedInstance;
        Instances selectedInstances;

        // Replace the given fraction of instances
        replaceSelect = new LinkedList<Instance>();
        for(Instance instance: node.getData())
        {
            // Replace with instance chosen with weight-proportionate selection.
            if (this.random.nextDouble() < SpatialEnsembleLearningThreaded.REPLACE_FRACTION)
                selectedInstance = selectWeightedInstance(sampleInstance, zeroWeightInstances);
            else selectedInstance = instance;

            // Make a copy with reset weight 
            selectedInstance = (Instance)selectedInstance.copy();
            selectedInstance.setWeight(1.0);

            replaceSelect.add(selectedInstance);
        }
        selectedInstances = new Instances(node.getData(), replaceSelect.size());
        selectedInstances.addAll(replaceSelect);
        selectedInstances.randomize(this.random);

        // Remember replacements for this Node. Don't replace yet because this Node is a neighbor for other Nodes and these still need the weight of this epoch.
        node.setSelectedInstances(selectedInstances);
    }

    private Instance selectWeightedInstance(TreeMap<Double, Instance> instances, List<Instance> zeroWeightInstances)
    {
        double   ran;
        Double   wkey;

        // Pick a random number in [0, sum of instance weights]
        ran  = this.random.nextDouble()*instances.lastKey();

        // Find the Instance for which the sum of weight of all previous instances is closest
        wkey = instances.higherKey(ran);

        // When beyond the highest weight, pick the last one when available. Or one of the zero-weight instances when available.
        if (wkey == null)
        {
            if (zeroWeightInstances.size() == 0) return instances.get(instances.lastKey());
            else                                 return zeroWeightInstances.get(this.random.nextInt(zeroWeightInstances.size()));
        }
        // Return instance selected by weight-proportionate selection.
        else return instances.get(wkey);
    }

    private void testInstancesOnNeighbors() throws Exception
    {
        Instances            nodeset;
        int                  i;
        double []weight;
        double   conf, minconf;

        nodeset   = node.getData();
        weight    = new double[nodeset.size()];

        // For all instance in this node's dataset
        i = 0;
        for(Instance instance: nodeset)
        {
            // Test on neighbors. Remember most difficult test
            minconf = Double.MAX_VALUE;
            for(ClassifierNode neighbor: neighbors)
            {
                conf = neighbor.classify(instance);
                if (conf < minconf) minconf = conf;
            }
            // ********** DISABLE WEIGHTING *******
            //minconf = 1.0;
            // ************************************
            weight[i++] = minconf;
        }

        double wdiff, wmax, wmin;

        // Normalize weight with the highest weight as the most difficult instance
        wmax = Double.NEGATIVE_INFINITY;
        wmin = Double.POSITIVE_INFINITY;
        for (i=0; i<weight.length; i++) if (weight[i] > wmax) wmax = weight[i];
        for (i=0; i<weight.length; i++) if (weight[i] < wmin) wmin = weight[i];
        wdiff = (wmax - wmin);
        if (wdiff != 0)
        {
            for (i=0; i<weight.length; i++)
            {
                // Normalize between 0 and 1
                weight[i] = 1.0 - ((weight[i] - wmin) / wdiff);

                // Or... Normalize between 1 and WEIGHT_RATIO
                if (SpatialEnsembleLearningThreaded.WEIGHT_RATIO > 1.0) weight[i] = 1.0 + weight[i]*(SpatialEnsembleLearningThreaded.WEIGHT_RATIO-1.0);
            }
        }
        else
        {
            for (i=0; i<weight.length; i++) weight[i] = 1.0;
        }

        // Update the Instances of this Node with their new weights
        i = 0;
        for(Instance ins: nodeset)
        {
            // TODO: Add momentum as suggested by Uday
            ins.setWeight(weight[i++]);
        }
    }

    class InstanceWeightComparator implements Comparator
    {
        // Ordere from highest to lowest Instance weight. Break ties with the hashCode of the Instances.
        public int compare(Object arg0, Object arg1)
        {
            Instance a = (Instance)arg0;
            Instance b = (Instance)arg1;

            if      (b.weight() > a.weight()) return(1);
            else if (b.weight() < a.weight()) return(-1);
            else return new Integer(b.hashCode()).compareTo(new Integer(a.hashCode()));
        }
    }
}
