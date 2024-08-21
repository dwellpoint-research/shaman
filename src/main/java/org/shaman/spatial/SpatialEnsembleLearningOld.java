package org.shaman.spatial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class SpatialEnsembleLearningOld
{
    public static int DATASET_SIZE = 2000;
    public static int GRID_WIDTH   = 5;
    public static int GRID_HEIGHT  = 5;
    
    public static int EPOCHS = 100;
    public static double REPLACE_FRACTION = 0.2;
    
    private int epoch;
    
    private WraparoundGrid      grid;
    private Random              random;
    
    private Instances           dataset;
    private Instances           testset;
    private AbstractClassifier  classifier;
    
    private double              firstClassError;
    private double              minClassError;
    private int                 sizeAtMinError;

    // *********************************************************\
    // *              Weka dataset experiments                 *
    // *********************************************************/
    private void initWekaDataSet() throws Exception
    {
        DataSource source;
        
        /*
        // 'Digit' handwritten digits dataset: http://alex.seewald.at/digits/
        source = new DataSource("./data/train-digits.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("./data/test-digits.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
        
        //  Spambase dataset: http://www.hakank.org/weka/
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/spambase_real.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/spambase_real.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));

        

        /*
        // CH dataset: http://www.hakank.org/weka/
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/ch.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/ch.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
        
        /*
        // Adult income prediction dataset
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/adult.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/adult.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
        
        /*
        // Cover type dataset
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/covtype.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/covtype.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
        
        
        /*
        // W8A dataset
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/w8a.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/w8a.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
        
        /*
        // Codon RNA Splice dataset
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/cod-rna-train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/cod-rna-test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */
   
    }
    
    private void initWekaClassifier() throws Exception
    {
        NaiveBayes classifier = new NaiveBayes();
        List<String> options = new ArrayList<String>();
        options.add("-D");
        //options.add("-K");
        classifier.setOptions(options.toArray(new String[0]));
        
        this.classifier = classifier;
    }
    
    // *********************************************************\
    // *                Circle Fit toy problem                 *
    // *********************************************************/
    private void initCircleDataSet() throws Exception
    {
        CircleDataGenerator generator;
        
        // Generate the dataset and classifier for CircleFit toy problem
        generator    = new CircleDataGenerator();
        this.dataset = generator.generateInstances(DATASET_SIZE, 2);
        this.testset = generator.generateInstances(DATASET_SIZE, 2);
    }
    
    private void initCircleClassifier() throws Exception
    {
        this.classifier = new CircleFitClassifier();
    }
    
    private boolean measureCircle()
    {
        double ravg, setsize;
        int    cnt;
        
        // Set that distinct instances
        TreeSet pruneset = new TreeSet(new DistinctInstanceComparator());
        
        // Calculate average radius of the instances and the number of unique points in the instances
        ravg    = 0;
        cnt     = 0;
        setsize = 0;
        for(ClassifierNode node: this.grid.getNodes())
        {
            for(Instance instance: node.getData())
            {
                ravg += CircleFitClassifier.getRadius(instance);
                cnt++;
                pruneset.add(instance);
            }
        }
        setsize = pruneset.size();
        ravg   /= cnt;
        
        System.out.println(this.epoch+"\t"+ravg+"\t"+setsize);
        
        this.epoch++;
        return(this.epoch != EPOCHS);
    }
    
    // *********************************************************\
    // *               Confusion Matrix Evaluation             *
    // *********************************************************/
    class DistinctInstanceComparator implements Comparator
    {
        public int compare(Object arg0, Object arg1)
            {
                if ((arg0 instanceof Instance) && (arg1 instanceof Instance))
                {
                    Instance a = (Instance)arg0;
                    Instance b = (Instance)arg1;
                    boolean  diff = false;
                    int      i = 0;
                    while(i<a.numAttributes() && !diff)
                    {
                        if (a.value(i) != b.value(i)) diff = true;
                        if (!diff) i++;
                    }
                    if (!diff) return(0);
                    else       return(new Double(a.value(i)).compareTo(new Double(b.value(i))));
                }
                else return 0;
            }
    }
    
    public boolean testPerformance() throws Exception
    {
        double [][]conf;
        double [][]confnode;
        
        // Find the total number of distinct Instances in the train sets
        TreeSet<Instance> pruneset = new TreeSet<Instance>(new DistinctInstanceComparator());
        
        int allsize;
        
        // Combine the confusion matrices of all Nodes in a single one
        conf    = null;
        allsize = 0;
        for(ClassifierNode node: this.grid.getNodes())
        {
            confnode = node.testConfusion(this.testset);
            if (conf == null) conf = confnode;
            else              conf = combineConfusion(conf, confnode);
            
            for(Instance instance: node.getData())
            {
                pruneset.add(instance);
            }
            
            allsize += node.getData().size();
        }
        
        double err;
        
        err = printPerformance(conf);
        //System.out.println(this.epoch+"\t"+err+"\t"+pruneset.size()+"\t"+allsize);
        
        // Train a Classifer on all distinct instances found in the nodes. These should be the 'difficult' ones.
        // Measure performance on testset. This should get better 
        double [][]errtotalconf;
        double     errtotaltest;
        Instances  trainset;
        
        trainset = new Instances(this.dataset, pruneset.size());
        trainset.clear();
        for(Instance instance: pruneset) trainset.add(instance);
        
        ClassifierNode testnode = new ClassifierNode();
        testnode.setClassifier((AbstractClassifier)AbstractClassifier.makeCopy(this.classifier));
        testnode.setData(trainset);
        testnode.train();
        errtotalconf = testnode.testConfusion(this.testset);
        errtotaltest = printPerformance(errtotalconf);
        
        // Print epoch, error on test-set and remaining number of distinct (/difficult) instances left in all the nodes.
        System.out.println(this.epoch+"\t"+errtotaltest+"\t"+pruneset.size());
        
        // Remember best performance
        if (this.epoch == 0) this.firstClassError = errtotaltest;
        if (errtotaltest < this.minClassError)
        {
            this.minClassError  = errtotaltest;
            this.sizeAtMinError = pruneset.size();
        }
        
        // Move on when the classification error is lower than twice it was it the start of the experiment
        this.epoch++;
        //if (this.firstClassError*2 < errtotaltest) this.epoch = EPOCHS;
        
        boolean done = this.epoch == EPOCHS;
        
        if (done)
            System.out.println(this.minClassError+"\t"+this.sizeAtMinError);
        
        return(!done);
    }
    
    private double [][]combineConfusion(double [][]conf, double [][]confnode)
    {
        // Add the confusion matrix of the Node to the overall one
        for(int i=0; i<conf.length; i++)
            for (int j=0; j<conf[i].length; j++) conf[i][j] += confnode[i][j];
        
        return(conf);
    }
    
    private double printPerformance(double [][]conf)
    {
        StringBuffer sconf = new StringBuffer();
        double cntcorrect, cntall, err;
        
        sconf.append("Confusion Matrix:\n");
        cntcorrect = 0;
        cntall     = 0;
        for(int i=0; i<conf.length; i++)
        {
            for(int j=0; j<conf[i].length; j++)
            {
                cntall += conf[i][j];
                if (i==j) cntcorrect += conf[i][j];
                
                sconf.append("\t"+conf[i][j]);
            }
            sconf.append("\n");
        }
        err = 1.0 - (cntcorrect / cntall);
        
        //System.out.println(sconf.toString());
        
        return(err);
    }
    
    // *********************************************************\
    // *              Spatial Ensemble Learning                *
    // *********************************************************/
    public void initialize() throws Exception
    {
        this.random = new Random(System.currentTimeMillis());
        this.epoch  = 1;
     
        System.out.println("Train dataset size "+this.dataset.size());
        System.out.println("Test  dataset size "+this.testset.size());
        
        // Create the toroidal grid of classifiers
        this.grid = new WraparoundGrid(GRID_WIDTH, GRID_HEIGHT, this.classifier);
        
        int      i, numnodes;
        Instances nodedata;
        
        // Distribute the dataset over the grid nodes
        numnodes = GRID_WIDTH * GRID_HEIGHT;
        this.dataset.randomize(this.random);
        this.dataset.stratify(numnodes);
        
        
        i = 0;
        System.out.println("Distributing train dataset over nodes:");
        for (ClassifierNode node: this.grid.getNodes())
        {
            nodedata = dataset.testCV(numnodes, i++);
            node.setData(nodedata);
            
            System.out.println("\tnode "+i+" dataset size "+nodedata.size());
        }
        
        // Initialize performance measurements
        this.epoch          = 0;
        this.minClassError  = Double.MAX_VALUE;
        this.sizeAtMinError = 0;
    }
    
    private int countDistinctInNodes()
    {
        TreeSet<Instance> dset = new TreeSet<Instance>(new DistinctInstanceComparator());
        for(ClassifierNode node: this.grid.getNodes())
           for(Instance instance: node.getData()) dset.add(instance);
        
        return(dset.size());
    }
    
    public void train() throws Exception
    {
        List<ClassifierNode> nodes;
        List<ClassifierNode> neighbors;
        
        // Train all the Nodes
        nodes = this.grid.getNodes();
        for(ClassifierNode node: nodes)
        {
            node.train();
        }
        
        // Test the data-sets of all Nodes on their neighbor nodes
        for(ClassifierNode node: nodes)
        {
            neighbors = this.grid.getNeighbors(node);
            testInstancesOnNeighbors(node, neighbors);
        }
        
        // Propagate instance through nodes.
        //propagateCombined(nodes);
        //propagateGetWeightedFromNeighbor(nodes);
        //propagatePushWeightedToNeighbor(nodes);
        propagateSpatialProportional(nodes);
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
    
    // ***************************************************************\
    // * Replace with proportional selection from neighbor instances *
    // ***************************************************************/
    private void propagateSpatialProportional(List<ClassifierNode> nodes)
    {
        List<ClassifierNode> neighbors;
        TreeSet<Instance>    weightOrderInstances;
        TreeMap<Double, Instance> sampleInstance;
        double            weightSum;
        Map<ClassifierNode, List<Instance>> replacements;
        List<Instance>    replaceSelect;
        Instances         instances;

        // Collect the Instances sets resulting from the replacement step for all Nodes
        replacements = new HashMap<ClassifierNode, List<Instance>>();
        for(ClassifierNode node: nodes)
        {
            // Collect all Instances from this Node and its neighboring nodes, ordered from high to low weight.
            weightOrderInstances = new TreeSet<Instance>(new InstanceWeightComparator());
            for(Instance instance: node.getData())
                weightOrderInstances.add(instance);
            neighbors = this.grid.getNeighbors(node);
            for(ClassifierNode neighbor: neighbors)
                for(Instance instance: neighbor.getData())
                    weightOrderInstances.add(instance);

            // Put instances in a TreeMap with as key the cumulative weight starting with highest weight instances
            sampleInstance = new TreeMap<Double, Instance>();
            weightSum = 0;
            for(Instance instance: weightOrderInstances)
            {
                weightSum += instance.weight();
                sampleInstance.put(weightSum, instance);
            }

            // Weight proportionate selection
            replaceSelect = new LinkedList<Instance>();
            for(Instance instance: node.getData())
            {
                // Don't replace all:
                // The weight of an instances is not a constant since it depends on the data / classifier in its neighbor nodes
                // Therefore, an instances needs to be evaluate multiple times so its average weight is effectively used for propagation 
                if (this.random.nextDouble() < REPLACE_FRACTION)
                {
                    replaceSelect.add(selectWeightedInstance(sampleInstance));
                }
                else replaceSelect.add(instance);
            }
            
            // Remember replacements for this Node. Don't replace yet because this Node is a neighbor for other Nodes and these still need the weight of the this epoch.
            replacements.put(node, replaceSelect);
        }

        // For all Nodes: move to next epoch.
        for(ClassifierNode node: nodes)
        {
            // Set the instance weights to 1.0 in order not to confuse Classifiers that use these in training.
            replaceSelect = replacements.get(node);
            for(Instance instance: replaceSelect)
                instance.setWeight(1.0);
            
            // Replace the Node's Instances with the ones selected and reshuffle to avoid ordering effects in the Classifier.
            instances     = node.getData();
            instances.clear();
            instances.addAll(replaceSelect);
            instances.randomize(this.random);
        }
    }
    
    // ************************************************************\
    // * Propagate: get weighted from combined set from neighbors *
    // ************************************************************/
    private void propagateCombined(List<ClassifierNode> nodes)
    {
        TreeMap<Double, Instance> sampleInstance;
        Map.Entry<Double, Instance> higherEntry;
        TreeSet<Instance> weightOrderInstances;
        double            weightSum, wSelect;
        Map<ClassifierNode, List<Instance>> replacements;
        List<Instance>    replaceSelect;
        int               replaceCount;
        Instances            nodeset;
        int               i;
        List<ClassifierNode> neighbors;
        int allrepcount = 0;
        
        // For all nodes in the network, pick highly weighted instance from the neighbors
        replacements = new HashMap<ClassifierNode, List<Instance>>();
        for(ClassifierNode node: nodes)
        {
            weightOrderInstances = new TreeSet<Instance>(new InstanceWeightComparator());
            
            int neiallcount = 0;
            
            // Add the instances of all neighbors to a set ordered from high to low weight
            neighbors = this.grid.getNeighbors(node);
            for(ClassifierNode neighbor: neighbors)
            {
                for (Instance instance: neighbor.getData())
                {
                     weightOrderInstances.add(instance);
                }
                neiallcount += neighbor.getData().size();
            }
            
            // Put instance in a TreeMap with as key the cumulative weight starting with highest weight instances
            sampleInstance = new TreeMap<Double, Instance>();
            weightSum = 0;
            for(Instance instance: weightOrderInstances)
            {
                weightSum += instance.weight();
                sampleInstance.put(weightSum, instance);
            }
            
            // Weighted sampling of the instances using the TreeMap.higherEntry()
            replaceSelect = new LinkedList<Instance>();
            replaceCount  = (int)Math.floor(node.getData().size() * REPLACE_FRACTION);
            for(i=0; i<replaceCount; i++)
            {
                wSelect     = (this.random.nextDouble() * weightSum) - 0.00001;
                higherEntry = sampleInstance.higherEntry(wSelect);
                
                // Don't allow duplicate replacements. Pick next one instead.
                if (higherEntry != null)
                {
                    while(replaceSelect.contains(higherEntry.getValue()) && (higherEntry != null))
                        higherEntry = sampleInstance.higherEntry(higherEntry.getKey());
                }
                if (higherEntry == null)
                {
                    higherEntry = sampleInstance.lastEntry();
                }
                replaceSelect.add(higherEntry.getValue());
            }
            allrepcount += replaceCount;
            replacements.put(node, replaceSelect);
        }
        
        
        //TreeSet allr = new TreeSet(new DistinctInstanceComparator());
        //for(List<Instance> repi: replacements.values())
        //    for (Instance instance: repi) allr.add(instance);
        //System.out.println("Distinct instance replacements in all nodes "+allr.size()+" expected "+allrepcount);
        //System.out.println("Distinct before replace "+countDistinctInNodes());
        
        
        // For all nodes in the network
        for(ClassifierNode node: nodes)
        {
            // Replace a fraction of the instances with the ones picked from the neighbors
            nodeset       = node.getData();
            nodeset.randomize(this.random);
            replaceSelect = replacements.get(node);
            
            //TreeSet<Instance> dset = new TreeSet<Instance>(new DistinctInstanceComparator());
            //for(Instance instance: nodeset) dset.add(instance);
            //System.err.println("nodeset before replace "+dset.size()+" of "+replaceSelect.size()); 
            
            for(i=0; i<replaceSelect.size(); i++)
            {
                nodeset.remove(0);
            }
            
            for(Instance instance: replaceSelect)
            {
                nodeset.add(instance);
            }
            
            //dset.clear();
            //for(Instance instance: nodeset) dset.add(instance);
            //System.err.println("nodeset after replace "+dset.size()); 
            
            
            // Reset all weights in order not to confuse Weka Classifiers that properly handle instance weights 
            for(Instance instance: nodeset) instance.setWeight(1.0);
        }
        
        //System.out.println("Distinct end "+countDistinctInNodes());
        
    }

    // *********************************************************\
    // *  Propagate: push weighted instance to random neighbor *
    // *********************************************************/
    private void propagatePushWeightedToNeighbor(List<ClassifierNode> nodes)
    {
        List<ClassifierNode> neighbors;
        TreeMap<Double, Instance> weightedInstances;
        Map<ClassifierNode, List<Instance>> toNodeInstances;
        Map<ClassifierNode, List<Instance>> fromNodeInstances;
        ClassifierNode neighbor;
        List<Instance> fromInstances, toInstances;
        Instance       sampledInstance;
        
        // Create the Map for weighted sampling of instances
        toNodeInstances   = new HashMap<ClassifierNode, List<Instance>>();
        fromNodeInstances = new HashMap<ClassifierNode, List<Instance>>();
        for(ClassifierNode node: nodes)
        {
            neighbors = this.grid.getNeighbors(node);
            
            // Create a Map to sample instances out according to their weight
            weightedInstances = makeWeightedInstances(node);
           
            // Select dataSet.size() * REPLACE_FRACTION instances
            fromInstances = getNodeInstances(node, fromNodeInstances);
            for(int i=0; i<node.getData().size(); i++)
            {
                if (this.random.nextDouble() < REPLACE_FRACTION)
                {
                    // Select an Instance according to the weighting
                    sampledInstance = selectWeightedInstance(weightedInstances);
                    
                    // Pick a random neighbor
                    neighbor        = selectRandomNeighbor(neighbors);
                    
                    // Remember the Instance moved FROM node TO neighbor.
                    fromInstances.add(sampledInstance);
                    toInstances = getNodeInstances(neighbor, toNodeInstances);
                    toInstances.add(sampledInstance);
                }
            }
        }
        
        Set<Instance> distinctInstances;
        Instances     newDataSet;
        
        distinctInstances = new TreeSet<Instance>(new DistinctInstanceComparator());
        for(ClassifierNode node: nodes)
        {
            // Lookup the Instances leaving and arriving at this Node
            fromInstances = fromNodeInstances.get(node);
            toInstances   = toNodeInstances.get(node);
            
            // Remove to leaving / add the arriving instances. Ignore duplicates
            distinctInstances.clear();
            distinctInstances.addAll(node.getData());
            if (fromInstances != null) distinctInstances.removeAll(fromInstances);
            if (toInstances != null)   distinctInstances.addAll(toInstances);
            
            // Create the updated DataSet for this Node. Reset instance weights.
            newDataSet = new Instances(node.getData(), distinctInstances.size());
            for(Instance instance: distinctInstances)
            {
                instance.setWeight(1.0);
                newDataSet.add(instance);
            }
            node.setData(newDataSet);
            
            //System.err.println("FROM "+fromInstances.size()+" to "+toInstances.size()+" newset "+newDataSet.size());
        }
    }
    
    private List<Instance> getNodeInstances(ClassifierNode node, Map<ClassifierNode, List<Instance>> movedInstances)
    {
        List<Instance> moved;
        
        moved = movedInstances.get(node);
        if (moved == null)
        {
            moved = new LinkedList<Instance>();
            movedInstances.put(node, moved);
        }
        
        return(moved);
    }
    
    // *********************************************************\
    // * Propagate: get weighted instance from random neighbor *
    // *********************************************************/
    private void propagateGetWeightedFromNeighbor(List<ClassifierNode> nodes)
    {
        List<ClassifierNode> neighbors;
        ClassifierNode       neighbor;
        Instances            nodeset;
        TreeMap<Double, Instance> weightedInstances;
        Map<ClassifierNode, TreeMap<Double, Instance>> allWeightedInstances;
        Map<ClassifierNode, Instances> allReplaceSets;
        List<Instance>       collectReplaced;
        Set<Instance>        uniqueReplaced;
        Instances            replaceSet;
        Instance             replaced;
        
        // First create weight instance lookup maps for all Nodes
        allWeightedInstances = new HashMap<ClassifierNode, TreeMap<Double, Instance>>();
        for(ClassifierNode node: nodes)
        {
            weightedInstances = makeWeightedInstances(node);
            allWeightedInstances.put(node, weightedInstances);
        }
        
        // For all Nodes
        allReplaceSets = new HashMap<ClassifierNode, Instances>();
        for(ClassifierNode node: nodes)
        {
            // Find the neighbor nodes
            neighbors = this.grid.getNeighbors(node);
            
            // Get the Node's data-set, create space for the replaced instances and the set of unique instances after replacement
            nodeset   = node.getData();
            collectReplaced = new LinkedList<Instance>();
            uniqueReplaced  = new TreeSet<Instance>(new DistinctInstanceComparator());
            
            // For all Instances
            for(Instance instance: nodeset)
            {
                // Replace this instance?
                if (this.random.nextDouble() < REPLACE_FRACTION)
                {
                    // Select a random neighbor
                    neighbor          = selectRandomNeighbor(neighbors);
                    weightedInstances = allWeightedInstances.get(neighbor);
                    
                    // Select an instance from the neighbor according to the PDF of their weights.
                    replaced          = selectWeightedInstance(weightedInstances);
                    
                    if (!uniqueReplaced.contains(replaced))
                    {
                        uniqueReplaced.add(replaced);
                    }
                }
                // Keep it.
                else
                {
                    // Remember the selected instance.
                    uniqueReplaced.add(instance);
                }
            }
            
            // Remove any duplicate instances received from the neighbors
            //uniqueReplaced.addAll(collectReplaced);
            
            // Create the replaces Instances dataset for the node
            replaceSet = new Instances(nodeset, uniqueReplaced.size());
            for(Instance instance: uniqueReplaced)
                replaceSet.add(instance);
            
            // Remember the new dataset. Wait with installing it until after all nodes have gone through this instance propagation step.
            allReplaceSets.put(node, replaceSet);
        }
        
        // For all nodes
        for(ClassifierNode node: nodes)
        {
            // Reset the weight of all Instances to 1.0 so classifier training algorithms do not use these instance weights. 
            replaceSet = allReplaceSets.get(node);
            for(Instance instance: replaceSet)
            {
                instance.setWeight(1.0);
            }
            
            // Install the dataset in the node
            node.setData(replaceSet);
        }
    }
    
    private TreeMap<Double, Instance> makeWeightedInstances(ClassifierNode node)
    {
        TreeMap<Double, Instance> weightedInstances;
        TreeSet<Instance>    orderedInstances;
        double sumWeight, instanceWeight;

        // Create Map indexed by sum-of-weight of previous Instances ordered from high to low weight
        orderedInstances  = new TreeSet<Instance>(new InstanceWeightComparator());
        orderedInstances.addAll(node.getData());            
        weightedInstances = new TreeMap<Double, Instance>();
        sumWeight         = 0;
        for(Instance instance: node.getData())
        {
            // Instance Weighting: TODO add momentum term
            instanceWeight = instance.weight();
            // **** DISABLE INSTANCE WEIGHTING ****
            //instanceWeight = 1.0;
            // *************************************
            sumWeight += instanceWeight;
            weightedInstances.put(sumWeight, instance);
        }

        return(weightedInstances);
    }
    
    private Instance selectWeightedInstance(TreeMap<Double, Instance> instances)
    {
        double   ran;
        Double   wkey;
        
        // Pick a random number in [0, sum of instance weights]
        ran  = this.random.nextDouble()*instances.lastKey();
        
        // Find the Instance for which the sum of weight of all previous instances is closest
        wkey = instances.higherKey(ran);
        
        // Pick the last one when out-of-bounds.
        if (wkey == null) wkey = instances.lastKey();
        
        return instances.get(wkey);
    }
    
    private int selectRandomNeighborIndex(int neighborsSize)
    {
        double nran;
        int    nidx;
        
        nran = this.random.nextDouble()*neighborsSize;
        nidx = (int)Math.floor(nran);
        if (nidx == neighborsSize) nidx = neighborsSize-1;
        
        return(nidx);
    }
    
    private ClassifierNode selectRandomNeighbor(List<ClassifierNode> neighbors)
    {
        int nidx = selectRandomNeighborIndex(neighbors.size());
        return(neighbors.get(nidx));
    }
    
    
    
    
    private void testInstancesOnNeighbors(ClassifierNode node, List<ClassifierNode> neighbors) throws Exception
    {
        Instances            nodeset;
        int                  i;
        double []weight;
        double   conf, minconf;

        nodeset   = node.getData();
        weight    = new double[nodeset.size()];
        neighbors = this.grid.getNeighbors(node);

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

        // Normalize weight to [0,1] with 1 the most difficult instance
        wmax = Double.NEGATIVE_INFINITY;
        wmin = Double.POSITIVE_INFINITY;
        for (i=0; i<weight.length; i++) if (weight[i] > wmax) wmax = weight[i];
        for (i=0; i<weight.length; i++) if (weight[i] < wmin) wmin = weight[i];
        wdiff = (wmax - wmin);
        if (wdiff != 0)
        {
            for (i=0; i<weight.length; i++)
                weight[i] = 1.0 - ((weight[i] - wmin) / wdiff);
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
    
    // *********************************************************\
    // *                     Experiments                       *
    // *********************************************************/
    public static void main(String[] args)
    {
        try
        {
            SpatialEnsembleLearningOld sal = new SpatialEnsembleLearningOld();
            
            CircleDataGenerator.INTERIOR_RADIUS = 0.4;
            
            SpatialEnsembleLearningOld.DATASET_SIZE     = 2000;
            SpatialEnsembleLearningOld.GRID_WIDTH       = 3;
            SpatialEnsembleLearningOld.GRID_HEIGHT      = 3;
            SpatialEnsembleLearningOld.REPLACE_FRACTION = 0.2;
            

            /*
            // Circle toy problem
            sal.initCircleDataSet();
            sal.initCircleClassifier();
            sal.initialize();           
            sal.train();
            while(sal.testPerformance()) sal.train();
            */
            
            // Weka dataset with real classifier
            sal.initWekaDataSet();
            sal.initWekaClassifier();
            sal.initialize();
            sal.train();
            while(sal.testPerformance()) sal.train();            
            
            //sal.testDigit();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public void testDigit() throws Exception
    {
        DataSource source = new DataSource("./data/train-digits.arff");
        Instances data = source.getDataSet();
        data.setClass(data.attribute("class"));
        
        
        NaiveBayes classifier = new NaiveBayes();
        List<String> options = new ArrayList<String>();
        options.add("-D");
        //options.add("-K");
        classifier.setOptions(options.toArray(new String[0]));
        
        //classifier.buildClassifier(data);
        //System.out.println(classifier.toString());
        
        
        ClassifierNode node = new ClassifierNode();
        node.setClassifier(classifier);
        node.setData(data);
        node.train();
        
        source = new DataSource("./data/test-digits.arff");
        Instances testset = source.getDataSet();
        testset.setClass(testset.attribute("class"));
        
        double [][]conf;
        conf = node.testConfusion(testset);
        printPerformance(conf);
        
    }


}
