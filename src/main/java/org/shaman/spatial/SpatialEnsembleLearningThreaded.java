package org.shaman.spatial;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpatialEnsembleLearningThreaded
{
    public static int DATASET_SIZE = 2000;
    public static int GRID_WIDTH   = 3;
    public static int GRID_HEIGHT  = 3;

    public static int EPOCHS = 50;
    public static double REPLACE_FRACTION = 0.3;
    public static double WEIGHT_RATIO = 0.0;

    public static final int NUMBER_OF_THREADS = 4;
    public int numberOfThreads = NUMBER_OF_THREADS;
    public int gridWidth = GRID_WIDTH;
    public int gridHeight = GRID_HEIGHT;
    public double pr=REPLACE_FRACTION;

    private int epoch;

    private WraparoundGrid      grid;

    private Instances           dataset;         // Train set
    private Instances           validationset;   // Validation set
    private Instances           testset;         // Test set for final result
    private AbstractClassifier  classifier;
    private Random              random;

    private double              firstClassError;
    private double              minClassError;
    private int                 sizeAtMinError;

    // Multi-threading support
    private ExecutorService   executorService;
    private List<NodeWorker>  workers;
    private long              epochStart;

    private boolean useWeightedAUCForSelection=true;
    private Classifier bestSoFarClassifier;
    
    //public static final String DATA_DIR = "./data/";
    public static final String DATA_DIR = "/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets/";

    // *********************************************************\
    // *              Weka dataset experiments                 *
    // *********************************************************/
    private void initWekaDataSet() throws Exception
    {
        DataSource source;


        // 'Digit' handwritten digits dataset: http://alex.seewald.at/digits/
        /*source = new DataSource(DATA_DIR+"sinewave3Train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"sinewave3Test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));



        //  Spambase dataset: http://www.hakank.org/weka/
        /*source = new DataSource(DATA_DIR+"/spambase_real.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"spambase_real.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
        */


/*
        // CH dataset: http://www.hakank.org/weka/
        source = new DataSource(DATA_DIR+"ch.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"ch.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));
*/


        // Adult income prediction dataset
       /* source = new DataSource(DATA_DIR+"adult.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"adult.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));*/



        // Cover type dataset
        source = new DataSource(DATA_DIR+"covtype.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"covtype.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));


        // W8A dataset
        /*source = new DataSource(DATA_DIR+"w8a.train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"w8a.test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));


        // Codon RNA Splice dataset
        /*source = new DataSource(DATA_DIR+"cod-rna-train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"cod-rna-test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));   */

        /*source = new DataSource(DATA_DIR+"Magic.Train.arff");
        this.dataset = source.getDataSet();
        this.dataset.setClass(dataset.attribute("class"));
        source = new DataSource(DATA_DIR+"Magic.Test.arff");
        this.testset = source.getDataSet();
        this.testset.setClass(dataset.attribute("class"));  */

        Random r = new Random(1);
        Instances instances = source.getDataSet();
        instances.setClass(instances.attribute("class"));
        int splits = instances.numInstances() * (10 - 1) / 10;
		instances.randomize(r);
		// Select out a fold
		instances.stratify(10);
		this.dataset = new Instances(instances, 0, splits);
		this.validationset = new Instances(instances, splits, instances.numInstances()-splits);
		System.err.println("Building model on training split (" + splits+ " instances)...");

    }

    public Classifier getBestClassifier(){
    	return this.bestSoFarClassifier;
    }

    private void initWekaClassifier() throws Exception
    {
        NaiveBayes classifier = new NaiveBayes();
        List<String> options = new ArrayList<String>();
        options.add("-D");   // Discretize numeric attributes
        //options.add("-K"); // Kernel estimater instead of normal distribution for numeric attributes
        classifier.setOptions(options.toArray(new String[0]));

        //if want to set Decision Tree
//        J48 decisionTree = new J48();
//		this.classifier= decisionTree;
//    	REPTree classifier = new REPTree();


        // k-nearest neighbor
//    	IBk classifier = new IBk();
//    	classifier.setKNN(1);

        // SVM
//    	SMO classifier = new SMO();
//    	classifier.setKernel(new PolyKernel());

        this.classifier = classifier;
    }

    // *********************************************************\
    // *                 Logging about timing                  *
    // *********************************************************/
    public boolean testTiming() throws Exception
    {
        long epochEnd, spent;
        long spentByWorkers;
        double spentFraction;

        // Calculate time elapsed while this epoch started
        epochEnd = System.currentTimeMillis();
        spent    = epochEnd - this.epochStart;

        // Calculate total time spent inside the worker threads
        spentByWorkers = 0;
        for(NodeWorker worker: this.workers) spentByWorkers += worker.getTimeSpent();

        // Scalability: when close to 1, the workers could execute near parallel
        spentFraction = ((double)spentByWorkers) / (spent*NUMBER_OF_THREADS);
//        spentFraction = (double)spentByWorkers / spent;
        System.out.println(this.epoch+"\t"+spent + "\t"+spentByWorkers+"\t"+spentFraction);

        // Move on to next epoch?
        this.epoch++;
        boolean done = this.epoch >= EPOCHS;

        this.epochStart = System.currentTimeMillis();

        return(!done);
    }

    // *********************************************************\
    // *               Confusion Matrix Evaluation             *
    // *********************************************************/
    public synchronized boolean testPerformance() throws Exception
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
            allsize += node.getData().size();

            //confnode = node.testConfusion(this.testset);
            //if (conf == null) conf = confnode;
            //else              conf = combineConfusion(conf, confnode);

            for(Instance instance: node.getData())
            {
                pruneset.add(instance);
            }
        }

        double err;

        //err = printPerformance(conf);
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
        if(!this.useWeightedAUCForSelection){
			errtotalconf = testnode.testConfusion(this.validationset);
			errtotaltest = printPerformance(errtotalconf);

			// Print epoch, error on test-set and remaining number of distinct
			// (/difficult) instances left in all the nodes.
			System.err.println(this.epoch + "\t ErrorConfusion:" + errtotaltest + "\t"
				+ pruneset.size());
		}
		else {
			Evaluation eval = new Evaluation(trainset);
			eval.evaluateModel(testnode.getClassifier(), this.validationset);
			double weightedAreaUnderCurve = eval.weightedAreaUnderROC();
			errtotaltest = 1- weightedAreaUnderCurve;
			// Print epoch, error on test-set and remaining number of distinct
			// (/difficult) instances left in all the nodes.
			System.out.println(this.epoch + "\t Weighted Area Under Curve" + weightedAreaUnderCurve + "\tError " + errtotaltest + "\t"
				+ pruneset.size());
		}
		// Remember best performance
		if (errtotaltest <= this.minClassError) {
			this.minClassError = errtotaltest;
			this.sizeAtMinError = pruneset.size();
			this.bestSoFarClassifier = testnode.getClassifier();
		}

        // Move on when the classification error is lower than twice it was it the start of the experiment
        this.epoch++;
        //if (this.firstClassError*2 < errtotaltest) this.epoch = EPOCHS;

        boolean done = this.epoch == EPOCHS;

        return(!done);
    }

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
    public void learn() throws Exception
    {
        trainMT();

        // Log statistics about classification performance.
        while(testPerformance()) trainMT();

        // Log statistics about speed of execution
        //while(testTiming()) trainMT();

        // Log best result
        System.out.println(this.minClassError+"\t"+this.sizeAtMinError);
    }

    public void learnAsynchronous() throws Exception
    {
        // First make sure the nodes are trained for the first time
        for(NodeWorker worker: this.workers)
            worker.setWork(NodeWorker.WORK_TRAIN);
        this.executorService.invokeAll(this.workers);
        testPerformance();

        // Start the Nodes so they execute asynchronously from eachother and re-submit themselves by calling recycleNodeWorker()
        for(NodeWorker worker: this.workers)
        {
            worker.setWork(NodeWorker.WORK_ASYNC);
            this.executorService.submit(worker);
        }

        // Sleep until each node has lived through 'epoch' executions.
        while(this.epoch < (EPOCHS*GRID_WIDTH*GRID_HEIGHT)) Thread.sleep(10);

        // Log best results.
        System.out.println(this.minClassError+"\t"+this.sizeAtMinError);
    }

    // Called by NodeWorker.WORK_ASYNC right before it finishes
    public void recycleNodeWorker(NodeWorker nodeWorker) throws Exception
    {
        this.epoch++;

        // Log statistics once in a while
        if (this.epoch % (GRID_WIDTH*GRID_HEIGHT) == 0)
        {
            //testTiming();
            testPerformance();
        }

        // Re-submit node for execution in the Threadpool
        this.executorService.submit(nodeWorker);
    }

    public void trainMT() throws Exception
    {
        // First train each node on its dataset
        for(NodeWorker worker: this.workers)
            worker.setWork(NodeWorker.WORK_TRAIN);
        this.executorService.invokeAll(this.workers);

        // Then test instances on neighboring nodes and select the ones for the next epoch
        for(NodeWorker worker: this.workers)
            worker.setWork(NodeWorker.WORK_SELECT);
        this.executorService.invokeAll(this.workers);

        // Switch over this epoch's instances for the selected ones
        for(ClassifierNode node: this.grid.getNodes())
            node.setData(node.getSelectedInstances());
    }

    public void initialize() throws Exception
    {
        this.random = new Random(System.currentTimeMillis());
        this.epoch  = 1;

        System.out.println("Train dataset size "+this.dataset.size());
        System.out.println("Test  dataset size " + this.validationset.size());

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

        List<NodeWorker> workers;

        // Create multi-threading logic. One worker for each Node.
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
        workers = new LinkedList<NodeWorker>();
        for(ClassifierNode node: this.grid.getNodes())
            workers.add(new NodeWorker(node, this.grid.getNeighbors(node), this));
        this.workers = workers;
        this.epochStart = System.currentTimeMillis();
    }

    // *********************************************************\
    // *                     Experiments                       *
    // *********************************************************/
    public static void main(String[] args)
    {
        try
        {
        	String threads = args[0];


            SpatialEnsembleLearningThreaded sal = new SpatialEnsembleLearningThreaded();
            sal.numberOfThreads = new Integer(threads).intValue();

            CircleDataGenerator.INTERIOR_RADIUS = 0.4;

            SpatialEnsembleLearningThreaded.DATASET_SIZE     = 2000;
            SpatialEnsembleLearningThreaded.GRID_WIDTH       = 3;
            SpatialEnsembleLearningThreaded.GRID_HEIGHT      = 3;
            SpatialEnsembleLearningThreaded.REPLACE_FRACTION = 0.2;
            WraparoundGrid.NEIGHBORS = WraparoundGrid.NEIGHBORS_C9;

            // Weka dataset with real classifier
            sal.initWekaDataSet();
            sal.initWekaClassifier();
            sal.initialize();

            // Train and collect statistics
            sal.learn();

           //lets evaluate the best classifier
            Evaluation eval = new Evaluation(sal.dataset);
            //eval.useNoPriors();
			eval.evaluateModel(sal.getBestClassifier(), sal.testset);
			System.out.println(eval.toSummaryString(true));
			System.out.println(eval.weightedAreaUnderROC());
			eval.predictions();

            // Asynchronous training and statistics
            //sal.learnAsynchronous();

            System.exit(0);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}
