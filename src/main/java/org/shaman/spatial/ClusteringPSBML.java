package org.shaman.spatial;

import weka.clusterers.AbstractClusterer;
import weka.clusterers.Clusterer;
import weka.core.*;
import weka.datagenerators.clusterers.BIRCHCluster;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;

public class ClusteringPSBML extends JFrame
{
    public static final String STATS_PATH = "/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/clustering/stats.txt";

    public static final String TITLE = "Parallel Spatial Boosting for Clustering";

    // PSBML parameters
    public static final int    GRID_WIDTH  = 3;
    public static final int    GRID_HEIGHT = 3;
    public static final int    EPOCHS = 30;
    public static final double REPLACE_FRACTION = .2;         // Apply proportionate selection on this fraction of each nodes dataset

    // Dataset parameter
    public static final int NUMBER_OF_CLUSTERS = 6;
    public static final int NUMBER_OF_CLUSTER_INSTANCES = 1000;

    // ----------

    private ClusteringView view;
    private boolean   nextDataSet = true;             // Flag set by pressing any key.

    private ClusterWraparoundGrid grid;               // PSBML Grid containing ClustererNodes

    private Random    random;

    // Current trained Clusterer and its dataset
    private BaseKMeans clusterer;
    private Instances  dataSet;
    private BaseKMeans startClusterer;

    private BaseKMeans minClusterer;
    private Instances  minDataSet;
    private String     minEpochSummary;

    public ClusteringPSBML()
    {
        this.random = new Random(System.currentTimeMillis());
    }

    public Instances initDataSet() throws Exception
    {
        Instances dataSet;

        // Create random BIRCH dataset. Gaussians with following parameters:
        BIRCHCluster dataGenerator = new BIRCHCluster();
        dataGenerator.setNumClusters(NUMBER_OF_CLUSTERS);          // number of clusters
        dataGenerator.setNumAttributes(6);                         // number of dimensions.
        dataGenerator.setMaxInstNum(NUMBER_OF_CLUSTER_INSTANCES);  // minimum / maximum number of instances per cluster
        dataGenerator.setMinInstNum(NUMBER_OF_CLUSTER_INSTANCES);
        dataGenerator.setMinRadius(0.3);                           // minimum / maximum radius of cluster
        dataGenerator.setMaxRadius(0.3);
        dataGenerator.setPattern(new SelectedTag(BIRCHCluster.RANDOM, BIRCHCluster.TAGS_PATTERN));   // Cluster centres are placed randomly
        //dataGenerator.setPattern(new SelectedTag(BIRCHCluster.GRID, BIRCHCluster.TAGS_PATTERN));   // Cluster centres are places on grid
        dataGenerator.setRandom(new Random());
        dataGenerator.setSeed((int)System.currentTimeMillis());
        //System.out.println("Random seed " + dataGenerator.getSeed());
        dataGenerator.defineDataFormat();
        dataSet = dataGenerator.generateExamples();
        this.dataSet = dataSet;

        return dataSet;
    }

    private void initLearner() throws Exception
    {
        // Create Clusterer prototype:
        // Weighted Object K-Means(++)
        BaseKMeans clusterer;
        clusterer = new BaseKMeans();
        clusterer.setNumClusters(NUMBER_OF_CLUSTERS);
        clusterer.setMaxIterations(20);
        clusterer.setInitializeUsingKMeansPlusPlusMethod(false);   // K-Means
        //clusterer.setInitializeUsingKMeansPlusPlusMethod(true);    // K-Means++ initialization method
        this.clusterer = clusterer;
    }

    private void initPSBML(Instances trainData) throws Exception
    {
        int i, numnodes;
        Instances nodeData;
        ClusterWraparoundGrid grid;

        // Install the base Clusterer algorithm in the wrap-around grid
        grid = new ClusterWraparoundGrid(GRID_WIDTH, GRID_HEIGHT, this.clusterer);
        this.grid = grid;

        // Distribute the dataset over the grid nodes
        numnodes = GRID_WIDTH * GRID_HEIGHT;
        trainData.randomize(this.random);

        i = 0;
        //System.out.println("Distributing train dataset over nodes:");
        for (ClustererNode node : this.grid.getNodes())
        {
            nodeData = trainData.testCV(numnodes, i++);
            node.setData(nodeData);
            //System.out.println("\tnode " + i + " dataset size " + nodeData.size());
        }
    }

    private void runPSBML() throws Exception
    {
        this.view = null;  // No visuals

        BufferedWriter statsWriter;
        Instances trainData;
        final int REPEATS = 100;

        // Repeat the PSBML expirement on different random datasets. Log statistics.
        statsWriter = new BufferedWriter(new FileWriter(new File(STATS_PATH)));

        // Repeated PSBML experiment: generate random dataset / train / test / statistics logging
        for (int i=0; i<REPEATS; i++)
        {
            // Load / generate dataset
            trainData = initDataSet();
            this.dataSet = trainData;

            // Initialize PSBML on the dataset
            initPSBML(trainData);

            // Perform PSBML on the dataset. Output stats.
            repeatPSBML(i, trainData, statsWriter);
        }
        statsWriter.close();
    }

    private void repeatPSBML(int repeat, Instances trainData, BufferedWriter statsWriter) throws Exception
    {
        long tbeg, tend;
        Instances distinctInstances, instancesMin;
        BaseKMeans testClusterer;
        int epochMin, sizeMin;
        double []perfMin, perfStart, perf;
        String epochSummary;

        // Train / test the clustering on the given dataset before starting PSBML
        testClusterer = (BaseKMeans)AbstractClusterer.makeCopy(this.clusterer);
        perf = testPerformance(testClusterer, trainData, trainData);
        this.startClusterer = testClusterer;
        this.minDataSet = trainData;
        this.minClusterer = testClusterer;
        perfMin = perf;
        epochMin = 0;
        sizeMin = trainData.size();
        perfStart = perf;

        // Run algorithm for a number of epochs. Remember clusterer / dataset of the epoch with the best performance.
        tbeg = System.currentTimeMillis();
        for(int i=1; i<=EPOCHS; i++)
        {
            // Do one PSBML epoch: train nodes, test on neighboring nodes, proportionate selection of instances.
            trainEpoch();

            // Train: run the clusterer on the set of distinct instances present in the nodes.
            distinctInstances = collectDistinctInstances();
            testClusterer = (BaseKMeans)AbstractClusterer.makeCopy(this.clusterer);

            // Test: Determine quantization error on the original dataset
            perf = testPerformance(testClusterer, distinctInstances, trainData);

            epochSummary = i+"\t"+trainData.numInstances()+"\t"+perfStart[0]+"\t"+perf[0]+"\t"+perfMin[0];

            // Remember best epoch of them all
            if (perf[0] < perfMin[0])
            {
                perfMin = perf;
                epochMin = i;
                sizeMin = distinctInstances.numInstances();
                instancesMin = distinctInstances;
                this.minClusterer = testClusterer;
                this.minDataSet = instancesMin;

                epochSummary = i+"\t"+trainData.numInstances()+"\t"+perfStart[0]+"\t"+perf[0]+"\t"+perfMin[0];
                this.minEpochSummary = epochSummary;
            }

            // Display progress through epochs?
            if (this.view != null)
            {
                // Visualize!
                this.view.setEpochSummary(epochSummary);
                this.view.setDataSet(makeViewInstances(trainData, distinctInstances, testClusterer));
                this.view.drawInstances();
                //Thread.sleep(100); // Slow down to human speed...
            }
        }
        tend = System.currentTimeMillis();

        // Log stats to window / console / statistics output file...
        if (this.view != null) this.view.setEpochSummary(this.minEpochSummary);
        System.out.println(repeat+" Dataset start/best size: "+this.dataSet.size()+" / "+sizeMin+". Error start/best: "+perfStart[0]+" ("+perfStart[1]+") / "+perfMin[0]+"("+perfMin[1]+"). Best in epoch "+epochMin+". Time spent "+(tend-tbeg)+" ms");
        if (statsWriter != null)
        {
            StringBuilder statsBuilder = new StringBuilder();
            statsBuilder.append(repeat).append("\t");
            statsBuilder.append(this.dataSet.size()).append("\t");
            statsBuilder.append(sizeMin).append("\t");
            statsBuilder.append(epochMin).append("\t");
            statsBuilder.append(perfStart[0]).append("\t");
            statsBuilder.append(perfStart[1]).append("\t");
            statsBuilder.append(perfMin[0]).append("\t");
            statsBuilder.append(perfMin[1]).append("\t");
            statsBuilder.append((tend-tbeg)).append("\n");
            statsWriter.write(statsBuilder.toString());
            statsWriter.flush();
        }
    }

    private void trainEpoch() throws Exception
    {
        List<ClustererNode> nodes;
        List<ClustererNode> neighbors;

        nodes = this.grid.getNodes();

        // Set the instance weights to 1.0 in order not to confuse any base-clusterers that support instance weighting
        for (ClustererNode node : nodes)
            for (Instance instance : node.getData()) instance.setWeight(1.0);

        // Train all Nodes on their (sub-)datasets
        for (ClustererNode node : nodes)
        {
            node.train();
        }

        // Test the data-sets of all Nodes on their neighbor nodes
        for (ClustererNode node : nodes)
        {
            neighbors = this.grid.getNeighbors(node);
            testInstancesOnNeighbors(node, neighbors);
        }

        // Select instances for next epoch through proportional selection.
        propagateSpatialProportional(nodes);
    }

    private double []testPerformance(BaseKMeans clusterer, Instances trainDataSet, Instances testDataSet) throws Exception
    {
        double []perf;
        double mean, stddev;
        final int REPEAT = 10;

        // Performance is quantization error on the testDataSet, calculated by the clusterer trained on the trainDataSet
        // Repeat train / test a number of times. Calculate mean / stddev of the quantization errors.
        mean = 0;
        perf = new double[REPEAT];
        for(int i=0; i<REPEAT; i++)
        {
            clusterer.buildClusterer(trainDataSet);
            perf[i] = clusterer.evaluateQuantizationError(testDataSet);
            mean += perf[i];
        }
        mean /= REPEAT;

        stddev = 0;
        for(int i=0; i<REPEAT; i++) stddev += (perf[i]-mean)*(perf[i]-mean);
        stddev /= REPEAT;
        stddev = Math.sqrt(stddev);

        return new double[]{mean, stddev};
    }

    private Instances collectDistinctInstances()
    {
        TreeSet<Instance> pruneSet;
        Instances prunedData;

        // Collect all distinct instances present in the nodes
        pruneSet = new TreeSet<Instance>(new DistinctInstanceComparator());
        for(ClustererNode node: this.grid.getNodes())
            for(Instance instance: node.getData()) pruneSet.add(instance);
        prunedData = new Instances(this.dataSet, pruneSet.size());
        prunedData.addAll(pruneSet);

        return prunedData;
    }

    private void propagateSpatialProportional(List<ClustererNode> nodes)
    {
        List<ClustererNode> neighbors;
        TreeSet<Instance> weightOrderInstances;
        TreeMap<Double, Instance> sampleInstance;
        List<Instance> zeroWeightInstances;
        double weightSum;
        Map<ClustererNode, List<Instance>> replacements;
        List<Instance> replaceSelect;
        Instances instances;

        // Collect the Instances sets resulting from the replacement step for all Nodes
        replacements = new HashMap<ClustererNode, List<Instance>>();
        for (ClustererNode node : nodes)
        {
            // Collect all Instances from this Node and its neighboring nodes, ordered from high to low weight.
            weightOrderInstances = new TreeSet<Instance>(new InstanceWeightComparator());
            for (Instance instance : node.getData())
            {
                weightOrderInstances.add(instance);
            }
            neighbors = this.grid.getNeighbors(node);
            for (ClustererNode neighbor : neighbors)
            {
                for (Instance instance : neighbor.getData())
                    weightOrderInstances.add(instance);
            }

            // Put instances in a TreeMap with as key the cumulative weight starting with highest weight instances
            sampleInstance = new TreeMap<Double, Instance>();
            zeroWeightInstances = new LinkedList<Instance>();
            weightSum = 0;
            for (Instance instance : weightOrderInstances)
            {
                if (instance.weight() > 0)
                {
                    weightSum += instance.weight();
                    sampleInstance.put(weightSum, instance);
                }
                else
                {
                    // Keep the (worst) Instances with weight 0 in a separate
                    // list... So they don't overwrite the sampleInstance of the
                    // last instances with weight > 0.
                    zeroWeightInstances.add(instance);
                }
            }

            // Weight proportionate selection
            replaceSelect = new LinkedList<Instance>();
            for (Instance instance : node.getData())
            {
                // Don't replace all:
                // The weight of an instances is not a constant since it depends
                // on the data / classifier in its neighbor nodes.
                // Therefore, an instances needs to be evaluated multiple times
                // so its average weight is effectively used for propagation
                // Using a 'momentum' term in the weighting should be smooth out
                // this effect.
                if (this.random.nextDouble() < REPLACE_FRACTION)
                {
                    replaceSelect.add(selectWeightedInstance(sampleInstance, zeroWeightInstances));
                }
                else replaceSelect.add(instance);
            }

            // Remember replacements for this Node. Don't replace yet because
            // this Node is a neighbor for other Nodes and these still need the
            // weight of the this epoch.
            replacements.put(node, replaceSelect);
        }

        // // For all Nodes: move to next epoch.
        for (ClustererNode node : nodes)
        {
            replaceSelect = replacements.get(node);

            // Replace the Node's Instances with the ones selected and reshuffle
            // to avoid ordering effects in the Classifier.
            instances = node.getData();
            instances.clear();
            instances.addAll(replaceSelect);
            instances.randomize(this.random);
        }
    }

    private Instance selectWeightedInstance(TreeMap<Double, Instance> instances, List<Instance> zeroWeightInstances)
    {
        double ran;
        Double wkey;

        // Pick a random number in [0, sum of instance weights]
        ran = this.random.nextDouble() * instances.lastKey();

        // Find the Instance for which the sum of weight of all previous instances is closest
        wkey = instances.higherKey(ran);

        // When beyond the highest weight, pick the last one when available. Or one of the zero-weight instances when available.
        if (wkey == null)
        {
            if (zeroWeightInstances.size() == 0)
                return instances.get(instances.lastKey());
            else return zeroWeightInstances.get(this.random.nextInt(zeroWeightInstances.size()));
        }
        // Return instance selected by weight-proportionate selection.
        else return instances.get(wkey);
    }

    private void testInstancesOnNeighbors(ClustererNode node, List<ClustererNode> neighbors) throws Exception
    {
        Instances nodeData;
        Instance instance;
        double neighborWeight, minWeight;
        double []weights;

        // Determine instance weights.
        nodeData = node.getData();
        weights  = new double[nodeData.numInstances()];
        for(int i=0; i<nodeData.numInstances(); i++)
        {
            // Smaller weights are 'easier' because they're closer to a centroid in one of the neighboring clusterers
            instance = nodeData.get(i);
            minWeight = Double.MAX_VALUE;
            for(int j=0; j<neighbors.size(); j++)
            {
                neighborWeight = neighbors.get(j).cluster(instance);
                if (neighborWeight < minWeight) minWeight = neighborWeight;
            }
            weights[i++] = minWeight;
        }

        // Normalize the weights
        double wdiff, wmax, wmin;

        // Normalize weight to [0,1] with 1 the more preferred instance
        wmax = Double.NEGATIVE_INFINITY;
        wmin = Double.POSITIVE_INFINITY;
        for (int i = 0; i < weights.length; i++) if (weights[i] > wmax) wmax = weights[i];
        for (int i = 0; i < weights.length; i++) if (weights[i] < wmin) wmin = weights[i];
        wdiff = (wmax - wmin);
        if (wdiff != 0)
        {
            for (int i = 0; i < weights.length; i++)
            {
                weights[i] = 1.0 - ((weights[i] - wmin) / wdiff);
            }
        }
        else
        {
            for (int i = 0; i < weights.length; i++) weights[i] = 1.0;
        }

        // Install the weight on the instances
        for(int i=0; i<nodeData.numInstances(); i++)
            nodeData.instance(i).setWeight(weights[i]);
    }


    public double runRandom() throws Exception
    {
        Instances dataSet;

        // Generate / load the dataset
        dataSet = initDataSet();

        // Run the clusterer on the dataset
        this.clusterer.buildClusterer(dataSet);

        /*
        // WEKA Performance stats
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(clusterer);
        eval.evaluateClusterer(dataSet);
        System.err.println(eval.clusterResultsToString());
        */

        double quantError;

        // Quantization error: average distance to assigned cluster centroids
        if (this.clusterer instanceof BaseKMeans)
            quantError = ((BaseKMeans)this.clusterer).evaluateQuantizationError(dataSet);
        else quantError = 0.0;

        return quantError;
    }

    private void displayDataset(Instances dataSet, Instances minDataSet, BaseKMeans clusterer) throws Exception
    {
        // Draw (2D) cluster points and their assignments as colors
        this.view.setDataSet(makeViewInstances(dataSet, minDataSet, clusterer));
        this.view.drawInstances();
    }

    private Instances makeViewInstances(Instances dataSet, Instances minDataSet, Clusterer clusterer) throws Exception
    {
        Attribute goalAttribute;
        Instances viewData;
        List<String> classes;

        // Extend the dataSet with a class Attribute containing the possible cluster assignments.
        classes = new LinkedList<String>();
        for(int i=0; i<clusterer.numberOfClusters(); i++) classes.add("cluster"+i);
        goalAttribute = new Attribute("goal", classes);
        viewData = new Instances(dataSet, dataSet.numInstances());
        viewData.insertAttributeAt(goalAttribute, dataSet.numAttributes());
        viewData.setClassIndex(dataSet.numAttributes());

        int clusterClass;
        Instance viewInstance;

        // Create new instances that are extended with their cluster assignment value.
        for(Instance instance: dataSet)
        {
            viewInstance = extendWithClass(instance, viewData);
            if (minDataSet == null || containsInstance(minDataSet, instance))
                viewInstance.setClassValue(clusterer.clusterInstance(instance));
            else viewInstance.setClassValue(-2);
            viewData.add(viewInstance);
        }

        // Include the centroids as well. Mark them with class -1 to recognize during visualization
        Instance []centroids = ((BaseKMeans)clusterer).getCentroids();
        for(int i=0; i<centroids.length; i++)
        {
            viewInstance = extendWithClass(centroids[i], viewData);
            viewInstance.setClassValue(-1);
            viewData.add(viewInstance);
        }
        if (this.startClusterer != null)
        {
            // and the centroids at start, when PSBML has not been run yet.
            centroids = ((BaseKMeans)this.startClusterer).getCentroids();
            for(int i=0; i<centroids.length; i++)
            {
                viewInstance = extendWithClass(centroids[i], viewData);
                viewInstance.setClassValue(-3);
                viewData.add(viewInstance);
            }
        }

        return viewData;
    }

    private boolean containsInstance(Instances dataSet, Instance instance)
    {
        DistinctInstanceComparator comparator;

        comparator = new DistinctInstanceComparator();
        for(Instance elementInstance: dataSet)
        {
            if (comparator.compare(instance, elementInstance) == 0) return true;
        }
        return false;
    }

    private Instance extendWithClass(Instance instance, Instances viewData)
    {
        Instance viewInstance;

        // Extend the instance with the extra Attribute holding the cluster assignment (class).
        viewInstance = new DenseInstance(instance.numAttributes()+1);
        for(int i=0; i<instance.numAttributes(); i++) viewInstance.setValue(i, instance.value(i));
        viewInstance.setDataset(viewData);

        return viewInstance;
    }

    public void buildLayout()
    {
        Container      contentPane;

        // Make empty frame listering to any key typed to move on to the next dataset.
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        setSize(800,700);
        setTitle(TITLE);
        contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.BLACK);
        KeyAdapter ka = new KeyAdapter()
        {
            public void keyTyped(KeyEvent e)
            {
                nextDataSet = true;
            }
        };
        addKeyListener(ka);

        ClusteringView view;

        // Display points in the current dataset, their cluster assignments (as colors) and the centroids found by the clustered
        view = new ClusteringView();
        contentPane.add(view, BorderLayout.CENTER);
        view.setDimensions(0,1);
        this.view = view;
    }

    public void showFrame()
    {
        validate();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        if (frameSize.height > screenSize.height) { frameSize.height = screenSize.height; }
        if (frameSize.width > screenSize.width)   { frameSize.width  = screenSize.width;  }
        setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void runVisualsPSBML() throws Exception
    {
        // Display results.
        showFrame();

        ClusterWraparoundGrid grid;

        // Install the base Clusterer algorithm in the wrap-around grid
        grid = new ClusterWraparoundGrid(GRID_WIDTH, GRID_HEIGHT, this.clusterer);
        this.grid = grid;

        while(true)
        {
            // Wait for a key to be pressed
            if (this.nextDataSet)
            {
                Instances trainData;

                // Make new dataset
                trainData = initDataSet();

                // Initialize PSBML on the dataset
                initPSBML(trainData);

                // Perform PSBML on the dataset. Output stats.
                repeatPSBML(0, trainData, null);

                // Draw the points in the dataset, their cluster assignments and the cluster centroids
                displayDataset(trainData, this.minDataSet, this.minClusterer);

                this.nextDataSet = false;
            }
            Thread.sleep(100);
        }
    }


    private void runVisuals() throws Exception
    {
        // Display results.
        showFrame();

        while(true)
        {
            // Wait for a key to be pressed
            if (this.nextDataSet)
            {
                double quantError;

                // Generate random dataset and run the clusterer
                quantError = runRandom();
                System.err.println("Quantization Error: "+quantError);

                // Draw the points in the dataset, their cluster assignments and the cluster centroids
                displayDataset(this.dataSet, null, this.clusterer);

                // Wait for next key-stroke
                this.nextDataSet = false;
            }
            Thread.sleep(100);
        }

    }

    private void compareKMeans() throws Exception
    {
        final int MAX_ITERATORS = 20;
        BaseKMeans kMeans, kMeansPP;

        // Plain K-Means
        kMeans = new BaseKMeans();
        kMeans.setNumClusters(NUMBER_OF_CLUSTERS);
        kMeans.setMaxIterations(MAX_ITERATORS);
        kMeans.setInitializeUsingKMeansPlusPlusMethod(false);

        // K-Means++ with optimized centroid initialization step
        kMeansPP = new BaseKMeans();
        kMeansPP.setNumClusters(NUMBER_OF_CLUSTERS);
        kMeansPP.setMaxIterations(MAX_ITERATORS);
        kMeansPP.setInitializeUsingKMeansPlusPlusMethod(true);

        Instances dataSet;
        double []perfKM, perfPP;

        // Compare performance on random datasets
        for(int i=0; i<100; i++)
        {
            dataSet = initDataSet();
            perfKM  = testPerformance(kMeans, dataSet, dataSet);
            perfPP  = testPerformance(kMeansPP, dataSet, dataSet);

            System.out.println(i+" "+perfKM[0]+" "+perfKM[1]+" "+perfPP[0]+" "+perfPP[1]+" ");
        }
    }

    public static void main(String []args)
    {
        ClusteringPSBML app = new ClusteringPSBML();
        try
        {
            app.buildLayout();

            // Initialize the base clusterer to use throughout the experiments
            app.initLearner();

            // Run parallel spatial boosted clustering.
            //app.runPSBML();

            // Visualize K-Means clusterer.
            //app.runVisuals();

            // Visualize PSBML running on the K-Means clusterer.
            app.runVisualsPSBML();

            // Compare K-Means and K-Means++
            //app.compareKMeans();

            System.exit(0);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(5);
        }
    }
}
