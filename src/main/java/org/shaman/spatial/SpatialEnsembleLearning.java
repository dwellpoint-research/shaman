package org.shaman.spatial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.DecisionStump;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

public class SpatialEnsembleLearning {
	public static int DATASET_SIZE = 5000;
	public static int GRID_WIDTH = 5;
	public static int GRID_HEIGHT = 5;

	public static int EPOCHS = 100;
	public static double REPLACE_FRACTION = 0.2;
	public static double WEIGHT_RATIO = 0.0;

	private int epoch;

	private WraparoundGrid grid;
	private Random random;

	private Instances dataset, copyTrainingSet;
	private Instances testset;
	private AbstractClassifier classifier;

	private double firstClassError;
	private double minClassError;
	private int sizeAtMinError;

	private DistributionTracker distributionTracker;
	//private HashMap<Double, WeightInstanceTracker> instanceToWeightTracker;
	private transient TreeSet<Instance> pruneSetFinal;

	// *********************************************************\
	// * Weka dataset experiments *
	// *********************************************************/
	private void initWekaDataSet() throws Exception {
		DataSource source;

		// 'Digit' handwritten digits dataset: http://alex.seewald.at/digits/
		/*
		 * source = new DataSource("./data/train-digits.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/test-digits.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		// Spambase dataset: http://www.hakank.org/weka/
		/*
		 * source = new DataSource("./data//spambase_real.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/spambase_real.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		// CH dataset: http://www.hakank.org/weka/
		/*
		 * source = new DataSource("./data/ch.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/ch.arff"); this.testset = source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 * 
		 * 
		 *  // Adult income prediction dataset /* source = new
		 * DataSource("./data/adult.train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/adult.test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		// Cover type dataset
		/*
		 * source = new DataSource("./data/covtype.train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/covtype.test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		// W8A dataset
		/*
		 * source = new DataSource("./data/w8a.train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/w8a.test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 * 
		 *  // Codon RNA Splice dataset /*source = new
		 * DataSource("./data/cod-rna-train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/cod-rna-test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		/*
		 * source = new DataSource("./data/Magic.Train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/Magic.Test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		// kddcup
		/*
		 * source = new DataSource("./data/KDDCup99-Train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("label")); source = new
		 * DataSource("./data/KDDCup99-Test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("label"));
		 */

		/*
		 * source = new
		 * DataSource("C:\\Research\\TCBB\\Results\\SampleRunToProve\\C_Elegans_Acceptor\\ce_elegans-train.libsvm.arff");
		 * this.dataset = source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("C:\\Research\\TCBB\\Results\\SampleRunToProve\\C_Elegans_Acceptor\\ce_elegans-test.libsvm.arff");
		 * this.testset = source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		/*
		 * source = new DataSource("./data/nonlinear-circle.libsvm.arff");
		 * this.dataset = source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/nonlinear-circle.test.ibsvm.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		/*
		 * source = new DataSource("./data/XorData.libsvm-train.arff");
		 * this.dataset = source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/XorData.libsvm-test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		/*
		 * source = new DataSource("./data/anneal-train.arff"); this.dataset =
		 * source.getDataSet();
		 * this.dataset.setClass(dataset.attribute("class")); source = new
		 * DataSource("./data/anneal-test.arff"); this.testset =
		 * source.getDataSet();
		 * this.testset.setClass(dataset.attribute("class"));
		 */

		source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets//MultiGaussian.csv.arff");
		this.dataset = source.getDataSet();
		this.dataset.setClass(dataset.attribute("class"));
		source = new DataSource("/Users/johan/SoftDev/Papers/Spatial Ensemble Learning/datasets//MultiGaussian.csv.arff");
		this.testset = source.getDataSet();
		this.testset.setClass(dataset.attribute("class"));
		this.copyTrainingSet = new Instances(this.dataset);

	}

	private void initWekaClassifier() throws Exception {
		NaiveBayes classifier = new NaiveBayes();
		// DecisionStump classifier = new DecisionStump();
		List<String> options = new ArrayList<String>();
		// options.add("-D");
		// options.add("-K");
		classifier.setOptions(options.toArray(new String[0]));
		// IBk classifier = new IBk();
		this.classifier = classifier;
	}

	// *********************************************************\
	// * Circle Fit toy problem *
	// *********************************************************/
	private void initCircleDataSet() throws Exception {
		CircleDataGenerator generator;

		// Generate the dataset and classifier for CircleFit toy problem
		generator = new CircleDataGenerator();
		this.dataset = generator.generateInstances(DATASET_SIZE, 2);
		this.copyTrainingSet = new Instances(this.dataset);
		this.testset = generator.generateInstances(DATASET_SIZE, 2);
	}

    /*
	private void initLinearDataSet() throws Exception {
		UnivariateGaussianDataGenerator generator;

		// Generate the dataset and classifier for CircleFit toy problem
		generator = new UnivariateGaussianDataGenerator();
		this.dataset = generator.generateInstances(DATASET_SIZE);
		this.testset = generator.generateInstances(DATASET_SIZE);
	}
	*/

	private void initCircleClassifier() throws Exception {
		this.classifier = new CircleFitClassifier();
	}
	
	private void initGaussianClassifier() throws Exception {
		this.classifier = new BivariateMultiGaussianSeparableClassifier();
	}

	private boolean measureCircle() {
		double ravg, setsize;
		int cnt;

		// Set that distinct instances
		TreeSet pruneset = new TreeSet(new DistinctInstanceComparator());

		// Calculate average radius of the instances and the number of unique
		// points in the instances
		ravg = 0;
		cnt = 0;
		setsize = 0;
		for (ClassifierNode node : this.grid.getNodes()) {
			for (Instance instance : node.getData()) {
				double radius = CircleFitClassifier.getRadius(instance);
				ravg += radius;
				cnt++;
				// distributionTracker.confidenceDistribution(radius,this.epoch);
				// WeightInstanceTracker tracker =
				// this.instanceToWeightTracker.get(CircleFitClassifier.getRadius(instance));

				// tracker.addWeight(instance.weight());
				pruneset.add(instance);
			}
		}
		setsize = pruneset.size();
		ravg /= cnt;

		System.out.println(this.epoch + "\t" + ravg + "\t" + setsize);

		this.epoch++;
		return (this.epoch != EPOCHS);
	}

	// *********************************************************\
	// * Confusion Matrix Evaluation *
	// *********************************************************/
	class DistinctInstanceComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			if ((arg0 instanceof Instance) && (arg1 instanceof Instance)) {
				Instance a = (Instance) arg0;
				Instance b = (Instance) arg1;
				boolean diff = false;
				int i = 0;
				while (i < a.numAttributes() && !diff) {
					if (a.value(i) != b.value(i))
						diff = true;
					if (!diff)
						i++;
				}
				if (!diff)
					return (0);
				else
					return (new Double(a.value(i)).compareTo(new Double(b
							.value(i))));
			} else
				return 0;
		}
	}

	public boolean testPerformance() throws Exception {
		double[][] conf;
		double[][] confnode;

		// Find the total number of distinct Instances in the train sets
		TreeSet<Instance> pruneset = new TreeSet<Instance>(
				new DistinctInstanceComparator());

		int allsize;

		// Combine the confusion matrices of all Nodes in a single one
		conf = null;
		allsize = 0;
		for (ClassifierNode node : this.grid.getNodes()) {
			confnode = node.testConfusion(this.testset);
			if (conf == null)
				conf = confnode;
			else
				conf = combineConfusion(conf, confnode);

			for (Instance instance : node.getData()) {
				pruneset.add(instance);
				// distributionTracker.weightDistribution(instance.weight(),this.epoch);
			}

			allsize += node.getData().size();
		}

		double err;

		err = printPerformance(conf);
		// System.out.println(this.epoch+"\t"+err+"\t"+pruneset.size()+"\t"+allsize);

		// Train a Classifer on all distinct instances found in the nodes. These
		// should be the 'difficult' ones.
		// Measure performance on testset. This should get better
		double[][] errtotalconf;
		double errtotaltest;
		Instances trainset;

		trainset = new Instances(this.dataset, pruneset.size());
		trainset.clear();
		for (Instance instance : pruneset)
			trainset.add(instance);

		ClassifierNode testnode = new ClassifierNode();
		testnode.setClassifier((AbstractClassifier) AbstractClassifier
				.makeCopy(this.classifier));
		testnode.setData(trainset);
		testnode.train();
		errtotalconf = testnode.testConfusion(this.testset);
		errtotaltest = printPerformance(errtotalconf);

		// Print epoch, error on test-set and remaining number of distinct
		// (/difficult) instances left in all the nodes.
		System.out.println(this.epoch + "\t" + errtotaltest + "\t"
				+ pruneset.size());
		

		// Remember best performance
		if (this.epoch == 0)
			this.firstClassError = errtotaltest;
		if (errtotaltest <= this.minClassError) {
			this.minClassError = errtotaltest;
			this.sizeAtMinError = pruneset.size();
			this.pruneSetFinal = pruneset;
		}

		// Move on when the classification error is lower than twice it was it
		// the start of the experiment
		this.epoch++;
		// if (this.firstClassError*2 < errtotaltest) this.epoch = EPOCHS;

		boolean done = this.epoch == EPOCHS;

		if (done)
			System.out.println(this.minClassError + "\t" + this.sizeAtMinError);

		return (!done);
	}

	private double[][] combineConfusion(double[][] conf, double[][] confnode) {
		// Add the confusion matrix of the Node to the overall one
		for (int i = 0; i < conf.length; i++)
			for (int j = 0; j < conf[i].length; j++)
				conf[i][j] += confnode[i][j];

		return (conf);
	}

	private double printPerformance(double[][] conf) {
		StringBuffer sconf = new StringBuffer();
		double cntcorrect, cntall, err;

		sconf.append("Confusion Matrix:\n");
		cntcorrect = 0;
		cntall = 0;
		for (int i = 0; i < conf.length; i++) {
			for (int j = 0; j < conf[i].length; j++) {
				cntall += conf[i][j];
				if (i == j)
					cntcorrect += conf[i][j];

				sconf.append("\t" + conf[i][j]);
			}
			sconf.append("\n");
		}
		err = 1.0 - (cntcorrect / cntall);

		// System.out.println(sconf.toString());

		return (err);
	}

	// *********************************************************\
	// * Spatial Ensemble Learning *
	// *********************************************************/
	public void initialize() throws Exception {
		this.random = new Random(System.currentTimeMillis());
		this.epoch = 1;

		System.out.println("Train dataset size " + this.dataset.size());
		System.out.println("Test  dataset size " + this.testset.size());

		// Create the toroidal grid of classifiers
		this.grid = new WraparoundGrid(GRID_WIDTH, GRID_HEIGHT, this.classifier);

		int i, numnodes;
		Instances nodedata;

		// Distribute the dataset over the grid nodes
		numnodes = GRID_WIDTH * GRID_HEIGHT;
		this.dataset.randomize(this.random);
		this.dataset.stratify(numnodes);

		i = 0;
		System.out.println("Distributing train dataset over nodes:");
		for (ClassifierNode node : this.grid.getNodes()) {
			nodedata = dataset.testCV(numnodes, i++);
			node.setData(nodedata);

			System.out.println("\tnode " + i + " dataset size "
					+ nodedata.size());
		}

		// Initialize performance measurements
		this.epoch = 0;
		this.minClassError = Double.MAX_VALUE;
		this.sizeAtMinError = 0;
	}

	private int countDistinctInNodes() {
		TreeSet<Instance> dset = new TreeSet<Instance>(
				new DistinctInstanceComparator());
		for (ClassifierNode node : this.grid.getNodes())
			for (Instance instance : node.getData())
				dset.add(instance);

		return (dset.size());
	}

	public void train() throws Exception {
		List<ClassifierNode> nodes;
		List<ClassifierNode> neighbors;
		// Train all the Nodes
		nodes = this.grid.getNodes();
		// For all Nodes: move to next epoch.
		for (ClassifierNode node : nodes) {
			//Set the instance weights to 1.0 in order not to confuse
			Instances instances = node.getData();
			for (Instance instance : instances)
				instance.setWeight(1.0);
		}

		for (ClassifierNode node : nodes) {
			node.train();
		}

		// Test the data-sets of all Nodes on their neighbor nodes
		for (ClassifierNode node : nodes) {
			neighbors = this.grid.getNeighbors(node);
			testInstancesOnNeighbors(node, neighbors);
		}

		// Select instances for next epoch
		propagateSpatialProportional(nodes);
	}

	class InstanceWeightComparator implements Comparator {
		// Ordere from highest to lowest Instance weight. Break ties with the
		// hashCode of the Instances.
		public int compare(Object arg0, Object arg1) {
			Instance a = (Instance) arg0;
			Instance b = (Instance) arg1;

			if (b.weight() > a.weight())
				return (1);
			else if (b.weight() < a.weight())
				return (-1);
			else
				return new Integer(b.hashCode()).compareTo(new Integer(a
						.hashCode()));
		}
	}

	// ***************************************************************\
	// * Replace with proportional selection from neighbor instances *
	// ***************************************************************/
	private void propagateSpatialProportional(List<ClassifierNode> nodes) {
		List<ClassifierNode> neighbors;
		TreeSet<Instance> weightOrderInstances;
		TreeMap<Double, Instance> sampleInstance;
		List<Instance> zeroWeightInstances;
		double weightSum;
		Map<ClassifierNode, List<Instance>> replacements;
		List<Instance> replaceSelect;
		Instances instances;

		// Collect the Instances sets resulting from the replacement step for
		// all Nodes
		replacements = new HashMap<ClassifierNode, List<Instance>>();
		for (ClassifierNode node : nodes) {
			// Collect all Instances from this Node and its neighboring nodes,
			// ordered from high to low weight.
			weightOrderInstances = new TreeSet<Instance>(
					new InstanceWeightComparator());
			for (Instance instance : node.getData())
				weightOrderInstances.add(instance);
			neighbors = this.grid.getNeighbors(node);
			for (ClassifierNode neighbor : neighbors)
				for (Instance instance : neighbor.getData())
					weightOrderInstances.add(instance);

			// Put instances in a TreeMap with as key the cumulative weight
			// starting with highest weight instances
			sampleInstance = new TreeMap<Double, Instance>();
			zeroWeightInstances = new LinkedList<Instance>();
			weightSum = 0;
			for (Instance instance : weightOrderInstances) {
				if (instance.weight() > 0) {
					weightSum += instance.weight();
					sampleInstance.put(weightSum, instance);
				} else {
					// Keep the (worst) Instances with weight 0 in a separate
					// list... So they don't overwrite the sampleInstance of the
					// last instances with weight > 0.
					zeroWeightInstances.add(instance);
				}
			}

			// Weight proportionate selection
			replaceSelect = new LinkedList<Instance>();
			for (Instance instance : node.getData()) {
				// Don't replace all:
				// The weight of an instances is not a constant since it depends
				// on the data / classifier in its neighbor nodes.
				// Therefore, an instances needs to be evaluated multiple times
				// so its average weight is effectively used for propagation
				// Using a 'momentum' term in the weighting should be smooth out
				// this effect.
				if (this.random.nextDouble() < REPLACE_FRACTION) {
					replaceSelect.add(selectWeightedInstance(sampleInstance,
							zeroWeightInstances));
				} else
					replaceSelect.add(instance);
			}

			// Remember replacements for this Node. Don't replace yet because
			// this Node is a neighbor for other Nodes and these still need the
			// weight of the this epoch.
			replacements.put(node, replaceSelect);
		}

		// // For all Nodes: move to next epoch.
		for (ClassifierNode node : nodes) {
			// Set the instance weights to 1.0 in order not to confuse
			// Classifiers that use these in training.
			replaceSelect = replacements.get(node);
			// for(Instance instance: replaceSelect)
			// instance.setWeight(1.0);

			// Replace the Node's Instances with the ones selected and reshuffle
			// to avoid ordering effects in the Classifier.
			instances = node.getData();
			instances.clear();
			instances.addAll(replaceSelect);
			instances.randomize(this.random);
		}
	}

	private Instance selectWeightedInstance(
			TreeMap<Double, Instance> instances,
			List<Instance> zeroWeightInstances) {
		double ran;
		Double wkey;

		// Pick a random number in [0, sum of instance weights]
		ran = this.random.nextDouble() * instances.lastKey();

		// Find the Instance for which the sum of weight of all previous
		// instances is closest
		wkey = instances.higherKey(ran);

		// When beyond the highest weight, pick the last one when available. Or
		// one of the zero-weight instances when available.
		if (wkey == null) {
			if (zeroWeightInstances.size() == 0)
				return instances.get(instances.lastKey());
			else
				return zeroWeightInstances.get(this.random
						.nextInt(zeroWeightInstances.size()));
		}
		// Return instance selected by weight-proportionate selection.
		else
			return instances.get(wkey);
	}

	private void testInstancesOnNeighbors(ClassifierNode node,
			List<ClassifierNode> neighbors) throws Exception {
		Instances nodeset;
		int i;
		double[] weight;
		double conf, minconf;

		nodeset = node.getData();
		weight = new double[nodeset.size()];
		neighbors = this.grid.getNeighbors(node);

		// For all instance in this node's dataset
		i = 0;
		for (Instance instance : nodeset) {
			// Test on neighbors. Remember most difficult test
			minconf = Double.MAX_VALUE;
			for (ClassifierNode neighbor : neighbors) {
				conf = neighbor.classify(instance);
				if (conf < minconf)
					minconf = conf;
			}
			// ********** DISABLE WEIGHTING *******
			// minconf = 1.0;
			// ************************************
			weight[i++] = minconf;
		}

		double wdiff, wmax, wmin;

		// Normalize weight to [0,1] with 1 the most difficult instance
		wmax = Double.NEGATIVE_INFINITY;
		wmin = Double.POSITIVE_INFINITY;
		for (i = 0; i < weight.length; i++)
			if (weight[i] > wmax)
				wmax = weight[i];
		for (i = 0; i < weight.length; i++)
			if (weight[i] < wmin)
				wmin = weight[i];
		wdiff = (wmax - wmin);
		if (wdiff != 0) {
			for (i = 0; i < weight.length; i++) {
				weight[i] = 1.0 - ((weight[i] - wmin) / wdiff);

				if (WEIGHT_RATIO > 1.0)
					weight[i] = 1.0 + weight[i] * (WEIGHT_RATIO - 1.0);
			}
		} else {
			for (i = 0; i < weight.length; i++)
				weight[i] = 1.0;
		}

		// Update the Instances of this Node with their new weights
		i = 0;
		for (Instance ins : nodeset) {
			// TODO: Add momentum as suggested by Uday
			ins.setWeight(weight[i++]);
			this.distributionTracker.weightDistribution(ins.value(0), epoch, ins
					.weight());
		}

	}

    /*
	public void trackInitWeight(Instances data) {
		int numberOfInstances = data.size();
		for (int i = 0; i < numberOfInstances; i++) {
			Instance instance = data.get(i);
			double radius = CircleFitClassifier.getRadius(instance);
			this.instanceToWeightTracker.put(radius,
					new WeightInstanceTracker());
		}
	}

	public void printDistributionForMargin() throws Exception {
		Set radiusSet = this.instanceToWeightTracker.keySet();
		Iterator allRadius = radiusSet.iterator();
		while (allRadius.hasNext()) {
			double radius = (Double) allRadius.next();
			String fileName = "Margin-" + radius + ".txt";
			File f = new File(fileName);
			WeightInstanceTracker tr = this.instanceToWeightTracker.get(radius);
			ArrayList weights = tr.getWeights();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f, true)));
			if (weights != null) {
				for (int j = 0; j < weights.size(); j++) {
					writer.write(new String(weights.get(j).toString() + "\n"));
				}
			}
			writer.flush();
			writer.close();

		}
	}

	private void initUnivariateClassifier() throws Exception {
		this.classifier = new GaussianFitClassifier();
	}*/

	// *********************************************************\
	// * Experiments *
	// *********************************************************/
	public static void main(String[] args) {
		try {

			SpatialEnsembleLearning sal = new SpatialEnsembleLearning();

			// CircleDataGenerator.INTERIOR_RADIUS = 0.4;
			sal.distributionTracker = new DistributionTracker();
			// sal.instanceToWeightTracker = new
			// HashMap<Double,WeightInstanceTracker>();
			//            
			SpatialEnsembleLearning.DATASET_SIZE = 5000;
			SpatialEnsembleLearning.GRID_WIDTH = 3;
			SpatialEnsembleLearning.GRID_HEIGHT = 3;
			SpatialEnsembleLearning.REPLACE_FRACTION = 0.2;
			WraparoundGrid.NEIGHBORS = WraparoundGrid.NEIGHBORS_C9;

			// Circle toy problem
			// sal.initLinearDataSet();
			// sal.initUnivariateClassifier();
			// sal.initCircleDataSet();
			// sal.initCircleClassifier();
			// sal.trackInitWeight(sal.dataset);
			// sal.initialize();
			// sal.train();
			// while(sal.testPerformance()) sal.train();
			// while(sal.measureCircle()) sal.train();
			// sal.distributionTracker.printDistribution();
			// sal.printDistributionForMargin();

			// Weka dataset with real classifier
			sal.initWekaDataSet();
			sal.initGaussianClassifier();
			sal.initialize();
			sal.train();
			while (sal.testPerformance())
				sal.train();
			sal.distributionTracker.printDistribution();
			// sal.printDistributionForMargin();

//			int count = 0;
//			File f = new File("SupportVectorsCircle.txt");
//			for (int k = 0; k < sal.copyTrainingSet.size(); k++) {
//				Instance trainingInstance = sal.copyTrainingSet.get(k);
//				StringBuffer sv = new StringBuffer();
//				if (sal.pruneSetFinal.contains(trainingInstance)) {
//					sv.append("SV\n");
//					count++;
//				} else
//					sv.append("NSV\n");
//				writeStringToTextFile(f, sv.toString(), true);
//			}
//			System.out.println(count);

			// sal.testDigit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void writeStringToTextFile(File f, String text, boolean append)
			throws IOException {
		if (f.getParentFile() != null) {
			f.getParentFile().mkdirs();
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f.getAbsolutePath(), append)));

			if (text == null) {
				writer.write("");
			} else {
				writer.write(text);
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

}
