package weka.clusterers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import weka.classifiers.meta.spatial.WraparoundGrid;
import weka.clusterers.AbstractClusterer;
import weka.core.Capabilities;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformationHandler;
import weka.core.Capabilities.Capability;
import weka.clusterers.psbml.*;

/**
 * 
  <p/> <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are: <p/>
 * 
 * <pre>
 *  -B &lt;num&gt;
 *  Grid Width for Torrodidal Grid.
 *  (default 3)
 * </pre>
 * 
 * <pre>
 *  -H &lt;num&gt;
 *  Grid Height for Torrodidal Grid.
 *  (default 3)
 * </pre>
 * 
 * <pre>
 *  -R &lt;num&gt;
 *  Replacement Ratio in Fitness Proportionate Selection.
 *  (default 0.2)
 * </pre>
 * 
 * <pre>
 *  -P &lt;num&gt;
 *  Percentage of weight mass to base training on.
 *  (default 90)
 * </pre> *
 * 
 * <pre>
 *  -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 *  -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)
 * </pre>
 * 
 *
 */

public class ParallelBoostingClusterer extends RandomizableClusterer implements
		OptionHandler {
	public static final String STATS_PATH = "C:/Research/INFS-755/Clustering/stats.txt";

	private int numClusters = 2; // Number of clusters
	private int maxIterations = 20; // Maximum number of iterations. Stop when
	// reached or when last iteration did not cause
	// any cluster assignment changes.
	private DistanceFunction distanceFunction = new EuclideanDistance();
	private transient ClusterWraparoundGrid grid; // PSBML Grid containing
	// ClustererNodes

	private transient Instances trainData;
	private transient Instances validationSet;
	private int gridWidth = 3;

	private int gridHeight = 3;
	/** The random number seed. */
	public int epochs = 30;
	public double replacementRatio = .2; // Apply proportionate
	// selection on this
	// fraction of each
	// nodes dataset

	/** Random object used in this class */
	private Random m_Random = null;

	// Current trained Clusterer and its dataset
	private BaseKMeans clusterer;

	private BaseKMeans startClusterer;

	private BaseKMeans minClusterer;
	private Instances minDataSet;
	private String minEpochSummary;
	boolean reverseWeighting = false;

	private int folds = 10;

	private static int NEIGHBORS_L5 = 0;

	private static int NEIGHBORS_L9 = 1;
	private static int NEIGHBORS_C9 = 2;
	private static int NEIGHBORS_C13 = 3;

	/** combination rules */
	public static final Tag[] TAGS_NEIGHBORHOOD = {
			new Tag(NEIGHBORS_L5, "L5", "Linear 5 Neighborhood"),
			new Tag(NEIGHBORS_L9, "L9", "Linear 9 Neighborhood"),
			new Tag(NEIGHBORS_C9, "C9", "Compact 9 Neighborhood"),
			new Tag(NEIGHBORS_C13, "C13", "Compact 13 Neighborhood") };

	private int neighborhood = 2;

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {

		double[] d = new double[numberOfClusters()];
		d = this.minClusterer.distributionForInstance(instance);

		return d;
	}

	@Override
	public void buildClusterer(Instances instances) throws Exception {
		// make copies equal to grid size
		int i, numnodes;
		Instances nodeData;
		ClusterWraparoundGrid grid;

		// initialize
		initLearner();

		this.m_Random = instances.getRandomNumberGenerator(m_Seed);
		// Install the base Clusterer algorithm in the wrap-around grid
		grid = new ClusterWraparoundGrid(this.gridWidth, this.gridHeight,
				this.clusterer, this.getNeighbors());
		this.grid = grid;

		// Distribute the dataset over the grid nodes
		numnodes = this.gridWidth * this.gridHeight;
		int splits = instances.numInstances() * (this.folds - 1) / this.folds;
		instances.randomize(this.m_Random);
		// Select out a fold
		this.trainData = new Instances(instances, 0, splits);
		this.validationSet = new Instances(instances, splits, instances
				.numInstances()
				- splits);
		System.err.println("Building model on training split (" + splits
				+ " instances)...");

		i = 0;
		for (ClustererNode node : this.grid.getNodes()) {
			nodeData = trainData.testCV(numnodes, i++);
			node.setData(nodeData);
		}

		// run
		runPSBML(this.trainData);
	}

	/**
	 * Returns default capabilities of the clusterer.
	 * 
	 * @return the capabilities of this clusterer
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		result.enable(Capability.NO_CLASS);

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);

		return result;
	}

	private void runPSBML(Instances trainData) throws Exception {
		long tbeg, tend;
		Instances distinctInstances, instancesMin;
		BaseKMeans testClusterer;
		int epochMin, sizeMin;
		double[] perfMin, perfStart, perf;
		String epochSummary;

		// Train / test the clustering on the given dataset before starting
		// PSBML
		testClusterer = (BaseKMeans) AbstractClusterer.makeCopy(this.clusterer);
		perf = testPerformance(testClusterer, trainData, this.validationSet);
		this.startClusterer = testClusterer;
		this.minDataSet = trainData;
		this.minClusterer = testClusterer;
		perfMin = perf;
		epochMin = 0;
		sizeMin = trainData.size();
		perfStart = perf;

		// Run algorithm for a number of epochs. Remember clusterer / dataset of
		// the epoch with the best performance.
		tbeg = System.currentTimeMillis();
		for (int i = 1; i <= epochs; i++) {
			// Do one PSBML epoch: train nodes, test on neighboring nodes,
			// proportionate selection of instances.
			trainEpoch();

			// Train: run the clusterer on the set of distinct instances present
			// in the nodes.
			distinctInstances = collectDistinctInstances();
			testClusterer = (BaseKMeans) AbstractClusterer
					.makeCopy(this.clusterer);

			// Test: Determine quantization error on the original dataset
			perf = testPerformance(testClusterer, distinctInstances, trainData);

			epochSummary = i + "\t" + trainData.numInstances() + "\t"
					+ perfStart[0] + "\t" + perf[0] + "\t" + perfMin[0];

			// Remember best epoch of them all
			if (perf[0] < perfMin[0]) {
				perfMin = perf;
				epochMin = i;
				sizeMin = distinctInstances.numInstances();
				instancesMin = distinctInstances;
				this.minClusterer = testClusterer;
				this.minDataSet = instancesMin;

				epochSummary = i + "\t" + trainData.numInstances() + "\t"
						+ perfStart[0] + "\t" + perf[0] + "\t" + perfMin[0];
				this.minEpochSummary = epochSummary;
			}

		}
		tend = System.currentTimeMillis();

		System.out.println(" Dataset start/best size: " + this.trainData.size()
				+ " / " + sizeMin + ". Error start/best: " + perfStart[0]
				+ " (" + perfStart[1] + ") / " + perfMin[0] + "(" + perfMin[1]
				+ "). Best in epoch " + epochMin + ". Time spent "
				+ (tend - tbeg) + " ms");

	}

	private void trainEpoch() throws Exception {
		List<ClustererNode> nodes;
		List<ClustererNode> neighbors;

		nodes = this.grid.getNodes();

		// Set the instance weights to 1.0 in order not to confuse any
		// base-clusterers that support instance weighting
		for (ClustererNode node : nodes)
			for (Instance instance : node.getData())
				instance.setWeight(1.0);

		// Train all Nodes on their (sub-)datasets
		for (ClustererNode node : nodes) {
			node.train();
		}

		// Test the data-sets of all Nodes on their neighbor nodes
		for (ClustererNode node : nodes) {
			neighbors = this.grid.getNeighbors(node);
			testInstancesOnNeighbors(node, neighbors);
		}

		// Select instances for next epoch through proportional selection.
		propagateSpatialProportional(nodes);
	}

	private double[] testPerformance(BaseKMeans clusterer,
			Instances trainDataSet, Instances testDataSet) throws Exception {
		double[] perf;
		double mean, stddev;
		final int REPEAT = 1;

		// Performance is quantization error on the testDataSet, calculated by
		// the clusterer trained on the trainDataSet
		// Repeat train / test a number of times. Calculate mean / stddev of the
		// quantization errors.
		mean = 0;
		perf = new double[REPEAT];
		for (int i = 0; i < REPEAT; i++) {
			clusterer.buildClusterer(trainDataSet);
			perf[i] = clusterer.evaluateQuantizationError(testDataSet);
			mean += perf[i];
		}
		mean /= REPEAT;

		stddev = 0;
		for (int i = 0; i < REPEAT; i++)
			stddev += (perf[i] - mean) * (perf[i] - mean);
		stddev /= REPEAT;
		stddev = Math.sqrt(stddev);

		return new double[] { mean, stddev };
	}

	private Instances collectDistinctInstances() {
		TreeSet<Instance> pruneSet;
		Instances prunedData;

		// Collect all distinct instances present in the nodes
		pruneSet = new TreeSet<Instance>(new DistinctInstanceComparator());
		for (ClustererNode node : this.grid.getNodes())
			for (Instance instance : node.getData())
				pruneSet.add(instance);
		prunedData = new Instances(this.trainData, pruneSet.size());
		prunedData.addAll(pruneSet);

		return prunedData;
	}

	private void propagateSpatialProportional(List<ClustererNode> nodes) {
		List<ClustererNode> neighbors;
		TreeSet<Instance> weightOrderInstances;
		TreeMap<Double, Instance> sampleInstance;
		List<Instance> zeroWeightInstances;
		double weightSum;
		Map<ClustererNode, List<Instance>> replacements;
		List<Instance> replaceSelect;
		Instances instances;

		// Collect the Instances sets resulting from the replacement step for
		// all Nodes
		replacements = new HashMap<ClustererNode, List<Instance>>();
		for (ClustererNode node : nodes) {
			// Collect all Instances from this Node and its neighboring nodes,
			// ordered from high to low weight.
			weightOrderInstances = new TreeSet<Instance>(
					new InstanceWeightComparator());
			for (Instance instance : node.getData()) {
				weightOrderInstances.add(instance);
			}
			neighbors = this.grid.getNeighbors(node);
			for (ClustererNode neighbor : neighbors) {
				for (Instance instance : neighbor.getData())
					weightOrderInstances.add(instance);
			}

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
				if (this.m_Random.nextDouble() < replacementRatio) {
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
		for (ClustererNode node : nodes) {
			replaceSelect = replacements.get(node);

			// Replace the Node's Instances with the ones selected and reshuffle
			// to avoid ordering effects in the Classifier.
			instances = node.getData();
			instances.clear();
			instances.addAll(replaceSelect);
			instances.randomize(this.m_Random);
		}
	}

	private Instance selectWeightedInstance(
			TreeMap<Double, Instance> instances,
			List<Instance> zeroWeightInstances) {
		double ran;
		Double wkey;

		// Pick a random number in [0, sum of instance weights]
		ran = this.m_Random.nextDouble() * instances.lastKey();

		// Find the Instance for which the sum of weight of all previous
		// instances is closest
		wkey = instances.higherKey(ran);

		// When beyond the highest weight, pick the last one when available. Or
		// one of the zero-weight instances when available.
		if (wkey == null) {
			if (zeroWeightInstances.size() == 0)
				return instances.get(instances.lastKey());
			else
				return zeroWeightInstances.get(this.m_Random
						.nextInt(zeroWeightInstances.size()));
		}
		// Return instance selected by weight-proportionate selection.
		else
			return instances.get(wkey);
	}

	private void initLearner() throws Exception {
		// Create Clusterer prototype:
		// Weighted Object K-Means(++)
		BaseKMeans clusterer;
		clusterer = new BaseKMeans();
		clusterer.setNumClusters(numClusters);
		clusterer.setMaxIterations(20);
		clusterer.setInitializeUsingKMeansPlusPlusMethod(true); // K-Means++
		this.clusterer = clusterer;
	}

	private void testInstancesOnNeighbors(ClustererNode node,
			List<ClustererNode> neighbors) throws Exception {
		Instances nodeData;
		Instance instance;
		double neighborWeight, minWeight;
		double[] weights;

		// Determine instance weights.
		nodeData = node.getData();
		weights = new double[nodeData.numInstances()];
		for (int i = 0; i < nodeData.numInstances(); i++) {
			// Smaller weights are 'easier' because they're closer to a centroid
			// in one of the neighboring clusterers
			instance = nodeData.get(i);
			minWeight = Double.MAX_VALUE;
			for (int j = 0; j < neighbors.size(); j++) {
				neighborWeight = neighbors.get(j).cluster(instance);
				if (neighborWeight < minWeight)
					minWeight = neighborWeight;
			}
			weights[i++] = minWeight;
		}

		// Normalize the weights
		double wdiff, wmax, wmin;

		// Normalize weight to [0,1] with 1 the more preferred instance
		wmax = Double.NEGATIVE_INFINITY;
		wmin = Double.POSITIVE_INFINITY;
		for (int i = 0; i < weights.length; i++)
			if (weights[i] > wmax)
				wmax = weights[i];
		for (int i = 0; i < weights.length; i++)
			if (weights[i] < wmin)
				wmin = weights[i];
		wdiff = (wmax - wmin);
		if (wdiff != 0) {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1.0 - ((weights[i] - wmin) / wdiff);
			}
		} else {
			for (int i = 0; i < weights.length; i++)
				weights[i] = 1.0;
		}

		// Install the weight on the instances
		for (int i = 0; i < nodeData.numInstances(); i++)
			nodeData.instance(i).setWeight(weights[i]);
	}

	@Override
	public int numberOfClusters() throws Exception {
		return this.numClusters;
	}

	public void setNumClusters(int n) throws Exception {
		this.numClusters = n;
	}
	
	public int getNumClusters() throws Exception {
		return this.numClusters;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public int getMaxIterations() {
		return this.maxIterations;
	}

	public void setDistanceFunction(DistanceFunction d) {
		this.distanceFunction = d;
	}

	public void setFolds(int folds) {
		this.folds = folds;
	}

	public int getGridWidth() {
		return gridWidth;
	}

	public int getGridHeight() {
		return gridHeight;
	}

	public double getReplacementRatio() {
		return replacementRatio;
	}

	public void setGridWidth(int gw) {
		this.gridWidth = gw;
	}

	public void setGridHeight(int gh) {
		this.gridHeight = gh;
	}

	public void setReplacementRatio(double rr) {
		this.replacementRatio = rr;
	}

	public int getFolds() {
		return this.folds;
	}

	/**
	 * Sets the TAGS_NEIGHBORHOOD rule to use. Values other than
	 * 
	 * @param newRule
	 *            the TAGS_NEIGHBORHOOD rule method to use
	 */
	public void setCombinationRule(SelectedTag newRule) {
		if (newRule.getTags() == TAGS_NEIGHBORHOOD)
			this.neighborhood = newRule.getSelectedTag().getID();
	}

	/**
	 * Gets the TAGS_NEIGHBORHOOD rule used
	 * 
	 * @return the TAGS_NEIGHBORHOOD rule used
	 */
	public SelectedTag getCombinationRule() {
		return new SelectedTag(this.neighborhood, TAGS_NEIGHBORHOOD);
	}

	private int[][] getNeighbors() {
		int[][] neighborhood = WraparoundGrid.NEIGHBORS_C9;
		switch (this.neighborhood) {
		case 0:
			neighborhood = ClusterWraparoundGrid.NEIGHBORS_L5;
			break;
		case 1:
			neighborhood = ClusterWraparoundGrid.NEIGHBORS_L9;
			break;
		case 2:
			neighborhood = ClusterWraparoundGrid.NEIGHBORS_C9;
			break;
		case 3:
			neighborhood = ClusterWraparoundGrid.NEIGHBORS_C13;
			break;

		}

		return neighborhood;
	}

}
