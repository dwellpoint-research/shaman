package weka.classifiers.meta;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.classifiers.Sourcable;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import weka.classifiers.meta.spatial.*;

/**
 * <!-- globalinfo-start --> Class for boosting a nominal class classifier using
 * the Parallel Spatial Method. Only nominal class problems can be tackled.
 * Often dramatically improves performance, but sometimes overfits.<br/> <br/>
 * For more information, see<br/> <br/> A Spatial EA Framework for
 * Parallelizing Machine Learning Methods Uday Kamath, Johan Kaers, Amarda
 * Shehu, and Kenneth A. De Jong <p/> <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#064;inproceedings{kamathppsn2012,
 *    address = {San Francisco},
 *    author = {Uday Kamath, Johan Kaers, Amarda Shehu and Kenneth De Jong},
 *    booktitle = {12th International Conference on Parallel Problem Solving From Nature},
 *    pages = {148-156},
 *    publisher = {Springer},
 *    title = {A Spatial EA Framework for Parallelizing Machine Learning Methods},
 *    year = {2012}
 * }
 * </pre>
 * 
 * <p/> <!-- technical-bibtex-end -->
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
 * </pre>
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
 * <pre>
 *  -U &lt;num&gt;
 *  Fraction of instances whose label is removed before training.
 *  (default 0.0)
 * </pre>
 * 
 * <pre>
 *  -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 *  -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 *  
 * Options specific to classifier weka.classifiers.bayes.NaiveBayes:
 * </pre>
 * 
 * <pre>
 *  -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated classifier.
 * <p>
 * 
 * @author Uday Kamath (ukamath@gmu.edu)
 * @author Johan Kaers (johankaers@gmail.be)
 * @version $Revision: 5928 $
 */
public class ParallelSpatialBoosting extends
        RandomizableIteratedSingleClassifierEnhancer implements Sourcable,
        TechnicalInformationHandler {

    private transient WraparoundGrid grid;

    private int gridWidth = 3;

    private int gridHeight = 3;

    private double replacementRatio = 0.2;

    private int folds = 10;

    private int neighborhood = 2;
    
    private double unlabeledFraction = 0.0;

    private int epoch;

    private Instances trainSet;
    private Instances testSet;

    private double firstClassError;

    public static double WEIGHT_RATIO = 0.0;

    /** Random object used in this class */
    private Random m_Random = null;

    private static int NEIGHBORS_L5 = 0;

    private static int NEIGHBORS_L9 = 1;
    private static int NEIGHBORS_C9 = 2;
    private static int NEIGHBORS_C13 = 3;
    
    /** combination rules */
      public static final Tag[] TAGS_NEIGHBORHOOD = {
        new Tag(NEIGHBORS_L5, "L5", "Linear 5 Neighborhood"),
        new Tag(NEIGHBORS_L9, "L9", "Linear 9 Neighborhood"),
        new Tag(NEIGHBORS_C9, "C9", "Compact 9 Neighborhood"),
        new Tag(NEIGHBORS_C13, "C13", "Compact 13 Neighborhood")
      };
      
      
    /** Class value assigned to instances whose label is removed when the 'unlabeledFraction' parameter is set */
    public static final double UNLABELED_CLASS = -1;

    private Classifier bestSoFarClassifier;

    

    private double minClassError;
    private int sizeAtMinError;

    private boolean useWeightedAUCForSelection;

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public double getReplacementRatio() {
        return replacementRatio;
    }

    public int getFolds() {
        return this.folds;
    }

     /**
       * Gets the TAGS_NEIGHBORHOOD rule used
       *
       * @return        the TAGS_NEIGHBORHOOD rule used
       */
      public SelectedTag getCombinationRule() {
        return new SelectedTag(this.neighborhood, TAGS_NEIGHBORHOOD);
      }

      /**
       * Sets the TAGS_NEIGHBORHOOD rule to use. Values other than
       *
       * @param newRule     the TAGS_NEIGHBORHOOD rule method to use
       */
      public void setCombinationRule(SelectedTag newRule) {
        if (newRule.getTags() == TAGS_NEIGHBORHOOD)
            this.neighborhood = newRule.getSelectedTag().getID();
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

    public void setFolds(int folds) {
        this.folds = folds;
    }
    
    public double getUnlabeledFraction() {
        return this.unlabeledFraction;
    }
    
    public void setUnlabeledFraction(double uf) {
        this.unlabeledFraction = uf;
    }


    /**
     * Gets if estimation is based on weighted AUC.
     * 
     * @return Value of useWeightedAUCForSelection.
     */
    public boolean getUseWeightedAUCForSelection() {
        return useWeightedAUCForSelection;
    }
    
    /**
     * Gets if estimation is based on weighted AUC.
     * 
     * @return Value of useWeightedAUCForSelection.
     */
    public void setUseWeightedAUCForSelection(boolean val) {
        this.useWeightedAUCForSelection = val;
    }

    /**
     * Constructor.
     */
    public ParallelSpatialBoosting() {
        m_Classifier = new weka.classifiers.bayes.NaiveBayes();
        List<String> options = new ArrayList<String>();
        options.add("-D");

    }

    private int[][] getNeighbors() {
        int[][] neighborhood = WraparoundGrid.NEIGHBORS_C9;
        switch (this.neighborhood) {
        case 0:
            neighborhood = WraparoundGrid.NEIGHBORS_L5;
            break;
        case 1:
            neighborhood = WraparoundGrid.NEIGHBORS_L9;
            break;
        case 2:
            neighborhood = WraparoundGrid.NEIGHBORS_C9;
            break;
        case 3:
            neighborhood = WraparoundGrid.NEIGHBORS_C13;
            break;

        }

        return neighborhood;
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     * 
     * @param instance
     *            the instance to be classified
     * @return predicted class probability distribution
     * @throws Exception
     *             if instance could not be classified successfully
     */
    public double[] distributionForInstance(Instance instance) throws Exception {
        return this.bestSoFarClassifier.distributionForInstance(instance);
    }

    /**
     * Spatial Parallel Boosting method.
     * 
     * @param data
     *            the training data to be used for generating the boosted
     *            classifier.
     * @throws Exception
     *             if the classifier could not be built successfully
     */
    public void buildClassifier(Instances instances) throws Exception
    {   
        this.m_Random = instances.getRandomNumberGenerator(m_Seed);
        this.epoch = 1;
        
        // make copies equal to grid size
        int classifierCount = this.gridHeight * this.gridWidth;
        m_Classifiers = AbstractClassifier.makeCopies(m_Classifier, classifierCount);
        
        // Create the torroidal grid of classifiers
        this.grid = new WraparoundGrid(this.gridWidth, this.gridHeight, this.m_Classifiers, this.getNeighbors());
        int splits = instances.numInstances() * (this.folds - 1) / this.folds;
        instances.randomize(this.m_Random);
        // Select out a fold
        instances.stratify(this.folds);
        this.trainSet = new Instances(instances, 0, splits);
        this.testSet = new Instances(instances, splits, instances.numInstances() - splits);
        System.err.println("Building model on training split (" + splits + " instances)...");
        
        // remove fraction of the classification labels of the train data?
        if (this.unlabeledFraction > 0.0) this.trainSet = removeTrainSetLabels(this.trainSet);    

        // Distribute the dataset over the grid nodes
        int numnodes = this.gridWidth * this.gridHeight;
        this.trainSet.randomize(this.m_Random);
        this.trainSet.stratify(numnodes);

        int i = 0;
        Instances nodedata;
        System.out.println("Distributing train dataset over nodes:");
        for (ClassifierNode node : this.grid.getNodes()) {
            nodedata = trainSet.testCV(numnodes, i++);
            node.setData(nodedata);
            System.out.println("\tnode " + i + " dataset size "
                    + nodedata.size());
        }

        // Initialize performance measurements
        this.epoch = 0;
        this.minClassError = Double.MAX_VALUE;
        this.sizeAtMinError = 0;

        // iterate
        train();
        while (testPerformance())
            train();
        this.m_Classifier = this.bestSoFarClassifier;
    }
    
    private Instances removeTrainSetLabels(Instances trainSet)
    {
        Instances instances;
        DenseInstance copyInstance;
        
        instances = new Instances(trainSet, 0);

        // Remove fraction of the classification labels of the instances while copying.
        for(Instance instance: trainSet)
        {
            if (this.m_Random.nextDouble() <= this.unlabeledFraction)
            {
                copyInstance = new DenseInstance(instance);
                copyInstance.setDataset(instances);
                copyInstance.setClassValue(UNLABELED_CLASS);
                instances.add(copyInstance);
            }
            else instances.add(instance);
        }
        
        System.err.println("Removed label for "+(trainSet.numInstances()-instances.numInstances())+" instances. Training with "+instances.numInstances()+" out of "+trainSet.numInstances()+" instances.");
        
        
        return instances;
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
            confnode = node.testConfusion(this.testSet);
            if (conf == null)
                conf = confnode;
            else
                conf = combineConfusion(conf, confnode);

            for (Instance instance : node.getData()) {
                pruneset.add(instance);
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

        trainset = new Instances(this.trainSet, pruneset.size());
        trainset.clear();
        for (Instance instance : pruneset)
            trainset.add(instance);

        ClassifierNode testnode = new ClassifierNode();
        Classifier testClassifier = (AbstractClassifier) AbstractClassifier.makeCopy(this.m_Classifier);
        testnode.setClassifier(testClassifier);
        if (this.unlabeledFraction > 0.0) trainset = removeUnlabeledInstances(trainset);
        testnode.setData(trainset);
        testnode.train();
        if(!this.useWeightedAUCForSelection){
            errtotalconf = testnode.testConfusion(this.testSet);
            errtotaltest = printPerformance(errtotalconf);

            // Print epoch, error on test-set and remaining number of distinct
            // (/difficult) instances left in all the nodes.
            System.out.println(this.epoch + "\t ErrorConfusion:" + errtotaltest + "\t"
                + pruneset.size());
        }
        else {
            Evaluation eval = new Evaluation(trainset);
            eval.evaluateModel(testnode.getClassifier(), this.testSet);
            double weightedAreaUnderCurve = eval.weightedAreaUnderROC();
            errtotaltest = 1- weightedAreaUnderCurve;
            // Print epoch, error on test-set and remaining number of distinct
            // (/difficult) instances left in all the nodes.
            System.out.println(this.epoch + "\t Weighted Area Under Curve" + weightedAreaUnderCurve + "\tError " + errtotaltest + "\t"
                + pruneset.size());
        }
        // Remember best performance
        if (this.epoch == 0)
            this.firstClassError = errtotaltest;
        if (errtotaltest < this.minClassError) {
            this.minClassError = errtotaltest;
            this.sizeAtMinError = pruneset.size();
            this.bestSoFarClassifier = testnode.getClassifier();
        }

        // Move on when the classification error is lower than twice it was it
        // the start of the experiment
        this.epoch++;
        // if (this.firstClassError*2 < errtotaltest) this.epoch = EPOCHS;

        boolean done = this.epoch == this.m_NumIterations;

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

    public void train() throws Exception {
        List<ClassifierNode> nodes;
        List<ClassifierNode> neighbors;
        
        // Train all the Nodes
        nodes = this.grid.getNodes();
        // For all Nodes: move to next epoch.
        for (ClassifierNode node : nodes)
        {
            // Find the data-set to train the node on. Only keep the labeled instances where some were unlabeled during initialization.
            Instances instances = node.getData();
            Instances trainInstances = null;
            if (this.unlabeledFraction > 0.0) trainInstances = removeUnlabeledInstances(instances);
            else                              trainInstances = instances;
            
            // Set the instance weights to 1.0 in order not to confuse
            // Classifiers that use these in training.
            for (Instance instance : trainInstances)
                instance.setWeight(1.0);
            
            // Train classification on the (labeled) instances
            node.setData(trainInstances);
            node.train();
            node.setData(instances);
        }

        // Test the data-sets of all Nodes on their neighbor nodes
        for (ClassifierNode node : nodes) {
            neighbors = this.grid.getNeighbors(node);
            testInstancesOnNeighbors(node, neighbors);
        }

        // Select instances for next epoch
        propagateSpatialProportional(nodes);
    }
    
    private Instances removeUnlabeledInstances(Instances trainData)
    {
        Instances instances;
        
        // Copy (references) of all labeled instances into a new set. Ignore the unlabled ones for training.
        instances = new Instances(trainData, trainData.numInstances());
        for(Instance instance: trainData)
        {
            if (instance.classValue() != UNLABELED_CLASS) instances.add(instance);
        }
        
        return instances;
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
                if (m_Random.nextDouble() < replacementRatio) {
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

        // For all Nodes: move to next epoch.
        for (ClassifierNode node : nodes) {
            // Set the instance weights to 1.0 in order not to confuse
            // Classifiers that use these in training.
            replaceSelect = replacements.get(node);
            // Replace the Node's Instances with the ones selected and reshuffle
            // to avoid ordering effects in the Classifier.
            instances = node.getData();
            instances.clear();
            instances.addAll(replaceSelect);
            instances.randomize(this.m_Random);
        }
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

    // Weka interface impl

    /**
     * Returns an instance of a TechnicalInformation object, containing detailed
     * information about the technical background of this class, e.g., paper
     * reference or book this class is based on.
     * 
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;

        result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR,
                "Uday Kamath, Johan Kaers, Amarda Shehu and Kenneth De Jong");
        result
                .setValue(Field.TITLE,
                        "A Spatial EA Framework for Parallelizing Machine Learning Methods");
        result
                .setValue(Field.BOOKTITLE,
                        "12th International Conference on Parallel Problem Solving From Nature");
        result.setValue(Field.YEAR, "2012");
        result.setValue(Field.PAGES, "148-156");
        result.setValue(Field.PUBLISHER, "Springer");
        result.setValue(Field.ADDRESS, "San Francisco");

        return result;
    }

    /**
     * Returns the boosted model as Java source code.
     * 
     * @param className
     *            the classname of the generated class
     * @return the tree as Java source code
     * @throws Exception
     *             if something goes wrong
     */
    public String toSource(String className) throws Exception {

        return null;
    }

    /**
     * Returns description of the boosted classifier.
     * 
     * @return description of the boosted classifier as a string
     */
    public String toString() {
        if (this.bestSoFarClassifier != null ) return this.bestSoFarClassifier.toString();
        return "";
    }

    /**
     * Returns the revision string.
     * 
     * @return the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 5928 $");
    }

    /**
     * Returns an enumeration describing the available options.
     * 
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {

        Vector newVector = new Vector();

        newVector
                .addElement(new Option(
                        "\tPercentage of training data to do training, rest would be evaluation internaly, 90% is default",
                        "P", 1, "-P <num>"));

        newVector.addElement(new Option("\tWrap around grid width", "B", 1,
                "-B <num>"));

        newVector.addElement(new Option("\tWrap around grid height", "H", 1,
                "-H <num>"));

        newVector.addElement(new Option(
                "\tReplacement in Fitness Proportional Selection", "Pr", 1,
                "-R <num>"));
        newVector.addElement(new Option(
                "\tFraction of instances whose label is removed before training", "U", 1,
                "-U <num>"));
                

        Enumeration enu = super.listOptions();
        while (enu.hasMoreElements()) {
            newVector.addElement(enu.nextElement());
        }

        return newVector.elements();
    }

    public void setOptions(String[] options) throws Exception {

        String thresholdString = Utils.getOption('P', options);
        if (thresholdString.length() != 0) {
            setFolds(Integer.parseInt(thresholdString));
        } else {
            setFolds(10);
        }

        String gridW = Utils.getOption("B", options);
        if (gridW.length() != 0) {
            setGridWidth(Integer.parseInt(gridW));
        } else {
            setGridWidth(3);
        }

        String gridH = Utils.getOption("H", options);
        if (gridH.length() != 0) {
            setGridHeight(Integer.parseInt(gridH));
        } else {
            setGridHeight(3);
        }

        String pr = Utils.getOption("R", options);
        if (pr.length() != 0) {
            setReplacementRatio(Double.parseDouble(pr));
        } else {
            setReplacementRatio(0.2);
        }
        
        String uf = Utils.getOption("U", options);
        if (uf.length() > 0) {
            setUnlabeledFraction(Double.parseDouble(uf));
        } else {
            setUnlabeledFraction(0.0);
        }

         String tmpStr = Utils.getOption('N', options);
            if (tmpStr.length() != 0) 
              setCombinationRule(new SelectedTag(tmpStr, TAGS_NEIGHBORHOOD));
            else
              setCombinationRule(new SelectedTag(NEIGHBORS_L5, TAGS_NEIGHBORHOOD));

        super.setOptions(options);
    }

}
