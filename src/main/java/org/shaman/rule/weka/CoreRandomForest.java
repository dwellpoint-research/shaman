package org.shaman.rule.weka;

import org.shaman.rule.RandomForest;
import org.shaman.rule.RandomForestBitData;
import org.shaman.rule.RandomForestByteData;
import org.shaman.rule.RandomForestData;
import org.shaman.rule.RandomForestDoubleData;
import org.shaman.rule.RandomForestTree;
import weka.classifiers.AbstractClassifier;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;

/**
 * WEKA wrapper for RandomForest algorithm.
 *
 * @author Johan Kaers
 */
public class CoreRandomForest extends AbstractClassifier implements OptionHandler, Randomizable, WeightedInstancesHandler, AdditionalMeasureProducer, TechnicalInformationHandler
{
    static final long serialVersionUID = 4216839470751428698L;

    private int minObjects = 2;                     // Minimum number of instances in leaf nodes
    private int maxDepth = 0;                       // Maximum number of levels in the tree. 0 = unlimited.
    private double trainFraction = 0.75;            // Fraction of train data to use for each Tree
    private int numberOfVariables = RandomForestTree.NUMBER_OF_VARIABLES_SQRT;         // Number of variables to consider at each tree-node
    private int numberOfTrees= 10;                  // The number of decision trees in the ensemble
    private int randomSeed = 1;                     // Random generator seed
    // --------
    private RandomForest randomForest;


    /**
     * Builds a classifier for a set of instances.
     *
     * @param data the instances to train the classifier with
     * @throws Exception if something goes wrong
     */
    public void buildClassifier(Instances data) throws Exception
    {
        // can classifier handle the data?
        getCapabilities().testWithFail(data);

        int   goalClasses;
        int []attributeDescription;
        RandomForestData trainInstances;
        RandomForest randomForest;

        // Make the training dataset and a description of the instances' Attributes.
        goalClasses = getGoalClasses(data);
        attributeDescription = makeAttributeDescription(data);
        trainInstances = makeTrainInstances(data, attributeDescription, goalClasses);

        // Create and configure the RandomForest with the given parameters and dataset.
        randomForest = new RandomForest();
        randomForest.setGoalClasses(goalClasses);
        randomForest.setAttributeDescription(attributeDescription);
        randomForest.setTrainInstances(trainInstances);
        randomForest.setNumberOfTrees(this.numberOfTrees);
        randomForest.setMinObjects(this.minObjects);
        randomForest.setNumberOfVariables(this.numberOfVariables);
        randomForest.setTrainFraction(this.trainFraction);
        randomForest.setMaxDepth(this.maxDepth);
        randomForest.setRandomSeed(this.randomSeed);
        randomForest.train();
        this.randomForest = randomForest;
    }

    private RandomForestData makeTrainInstances(Instances data, int []attributeDescription, int goalClasses)
    {
        RandomForestData instances;

        instances = null;
        if (goalClasses == 2)
        {
            // Can the data be put into bytes or even bits?
            boolean isCategorical = true;
            boolean isBinary = true;
            for(int i=0; i<attributeDescription.length-1; i++)
            {
                if (attributeDescription[i] > 0 && attributeDescription[i] < 256)
                {
                    if (attributeDescription[i] != 2) isBinary = false;
                }
                else { isCategorical = false; isBinary = false; }
            }

            /*
            // Binary data possible...
            if (isBinary)
            {
                BitSet []binaryInstances;
                int cntPos;

                // Create instances made out of bit-vectors...
                cntPos = 0;
                binaryInstances = new BitSet[data.numInstances()];
                for(int i=0; i<data.numInstances(); i++)
                {
                    binaryInstances[i] = new BitSet(attributeDescription.length);
                    for(int j=0; j<attributeDescription.length; j++)
                    {
                        if (data.instance(i).value(j) == 1.0) binaryInstances[i].set(j);
                    }
                    if (data.instance(i).classValue() == 1.0)
                    {
                        binaryInstances[i].set(attributeDescription.length-1);
                        cntPos++;
                    }
                }

                instances = new RandomForestBitData(binaryInstances, data.classIndex());
                System.out.println("Created BitData for "+instances.getNumberOfInstances()+" instances. Positive instances: "+cntPos);
            }
            // Categorical data possible.
            else */
            if (isCategorical)
            {
                byte [][]byteInstances;

                // Create instances made out of byte arrays.
                byteInstances = new byte[data.numInstances()][data.instance(0).numAttributes()];
                for(int i=0; i<data.numInstances(); i++)
                {
                    byteInstances[i] = new byte[data.instance(i).numAttributes()];
                    for(int j=0; j<data.instance(i).numAttributes(); j++)
                    {
                        byteInstances[i][j] = (byte)data.instance(i).value(j);
                    }
                }

                instances = new RandomForestByteData(byteInstances);
                System.out.println("Created ByteData for "+instances.getNumberOfInstances()+" instances");
            }
        }

        if (instances == null)
        {
            double[][] trainInstances;

            // Collect the instances in a double [][]. Convert the goal values to their corresponding goal class values.
            trainInstances = new double[data.numInstances()][];
            for (int i = 0; i < trainInstances.length; i++)
            {
                trainInstances[i] = data.instance(i).toDoubleArray();
                trainInstances[i][data.classIndex()] = data.instance(i).classValue();
            }
            instances = new RandomForestDoubleData(trainInstances);

            System.out.println("Created DoubleData for "+instances.getNumberOfInstances()+" instances");
        }

        return instances;
    }

    private int getGoalClasses(Instances data)
    {
        return data.classAttribute().numValues();
    }

    private int []makeAttributeDescription(Instances data)
    {
        int []attributeDescription;
        Attribute attribute;

        // For each Attribute, determine if its categorical / continuous or not active.
        attributeDescription = new int[data.numAttributes()];
        for(int i=0; i<data.numAttributes(); i++)
        {
            attribute = data.attribute(i);
            if (attribute != data.classAttribute())
            {
                if      (attribute.isNominal()) attributeDescription[i] = attribute.numValues();
                else if (attribute.isNumeric()) attributeDescription[i] = 0;
                else                            attributeDescription[i] = -1;
            }
            else attributeDescription[i] = -1;
        }

        return  attributeDescription;
    }

    /**
     * Returns the class probability distribution for an instance.
     *
     * @param instance the instance to be classified
     * @return the distribution the forest generates for the instance
     * @throws Exception if computation fails
     */
    public double[] distributionForInstance(Instance instance) throws Exception
    {
        double []confidence;

        confidence = new double[getGoalClasses(instance.dataset())];
        this.randomForest.classify(instance.toDoubleArray(), confidence);

        return confidence;
    }

    public String globalInfo()
    {
        return "Class for constructing a forest of decision trees.\n\nFor more information see: \n\n"+ getTechnicalInformation().toString();
    }

    public TechnicalInformation getTechnicalInformation()
    {
        TechnicalInformation 	result;

        result = new TechnicalInformation(TechnicalInformation.Type.ARTICLE);
        result.setValue(TechnicalInformation.Field.AUTHOR, "Leo Breiman");
        result.setValue(TechnicalInformation.Field.YEAR, "2001");
        result.setValue(TechnicalInformation.Field.TITLE, "Random Forests");
        result.setValue(TechnicalInformation.Field.JOURNAL, "Machine Learning");
        result.setValue(TechnicalInformation.Field.VOLUME, "45");
        result.setValue(TechnicalInformation.Field.NUMBER, "1");
        result.setValue(TechnicalInformation.Field.PAGES, "5-32");

        return result;
    }

    public String numTreesTipText()
    {
        return "The number of trees to be generated.";
    }

    public int getNumTrees()
    {
        return this.numberOfTrees;
    }

    public void setNumTrees(int newNumTrees)
    {

        this.numberOfTrees = newNumTrees;
    }

    public String minObjectsTipText()
    {
        return "The minimum number of instances in a node that is not a leaf.";
    }

    public int getMinObjects()
    {
        return this.minObjects;
    }

    public void setMinObjects(int minObjects)
    {
        this.minObjects = minObjects;
    }

    public String numFeaturesTipText()
    {
        return "The number of attributes to be used in random selection. 0 = all, 1 = ceil(sqrt(number of attributes))";
    }

    public int getNumFeatures()
    {
        return this.numberOfVariables;
    }

    public void setNumFeatures(int newNumFeatures)
    {
        this.numberOfVariables = newNumFeatures;
    }

    public String trainFractionTip()
    {
        return "Fraction of train data to use for each tree.";
    }

    public void setTrainFraction(double trainFraction)
    {
        this.trainFraction = trainFraction;
    }

    public double getTrainFraction()
    {
        return this.trainFraction;
    }

    public String seedTipText()
    {
        return "The random number seed to be used.";
    }

    public void setSeed(int seed)
    {
        this.randomSeed = seed;
    }

    public int getSeed()
    {
        return this.randomSeed;
    }

    public String maxDepthTipText()
    {
        return "The maximum depth of the trees, 0 for unlimited.";
    }

    public int getMaxDepth()
    {
        return this.maxDepth;
    }

    public void setMaxDepth(int value)
    {
        this.maxDepth = value;
    }

    public RandomForest getRandomForest()
    {
        return this.randomForest;
    }

    public void setRandomForest(RandomForest randomForest)
    {
        this.randomForest = randomForest;
    }

    public String printTreesTipText()
    {
        return "Print the individual trees in the output";
    }

    public double measureOutOfBagError()
    {
        if (this.randomForest != null) return this.randomForest.getOutOfBagError();
        else return Double.NaN;
    }

    public Enumeration enumerateMeasures()
    {
        Vector newVector = new Vector(1);
        newVector.addElement("measureOutOfBagError");
        return newVector.elements();
    }

    public double getMeasure(String additionalMeasureName)
    {
        if (additionalMeasureName.equalsIgnoreCase("measureOutOfBagError"))
        {
            return measureOutOfBagError();
        }
        else throw new IllegalArgumentException(additionalMeasureName+ " not supported (RandomForest)");
    }

    public Enumeration listOptions()
    {
        Vector newVector = new Vector();

        newVector.addElement(new Option(
                "\tNumber of trees to build.",
                "I", 1, "-I <number of trees>"));

        newVector.addElement(new Option(
                "\tNumber of features to consider (0=all, 1=ceil(sqrt(number of available)).",
                "K", 1, "-K <number of features>"));

        newVector.addElement(new Option(
                "\tMinimum number of instances in a node that's not a leaf.\n\t(default 2)",
                "M", 1, "-M <number of instnances>"));

        newVector.addElement(new Option(
                "\tThe maximum depth of the trees, 0 for unlimited.\n"
                        + "\t(default 0)",
                "depth", 1, "-depth <num>"));

        newVector.addElement(new Option(
                "\tFraction of training data to use for each tree.\n"
                        + "\t(default 1.0)",
                "F", 1, "-F <num>"));

        newVector.addElement(new Option(
                "\tSeed for random number generator.\n"
                        + "\t(default 1)",
                "S", 1, "-S"));

        Enumeration enu = super.listOptions();
        while (enu.hasMoreElements())
        {
            newVector.addElement(enu.nextElement());
        }

        return newVector.elements();
    }

    public String[] getOptions()
    {
        Vector        result;
        String[]      options;
        int           i;

        result = new Vector();

        result.add("-I");
        result.add("" + getNumTrees());

        result.add("-K");
        result.add("" + getNumFeatures());

        result.add("-M");
        result.add(""+this.minObjects);

        result.add("-F");
        result.add(""+this.trainFraction);

        if (getMaxDepth() > 0)
        {
            result.add("-depth");
            result.add("" + getMaxDepth());
        }

        result.add("-S");
        result.add("" + getSeed());

        options = super.getOptions();
        for (i = 0; i < options.length; i++) result.add(options[i]);

        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
     * Parses a given list of options. <p/>
     *
     * Valid options are: <p/>
     *
     * <pre> -I &lt;number of trees&gt;
     *  Number of trees to build.</pre>
     *
     * <pre> -K &lt;number of features&gt;
     *  Number of features to consider (&lt;1=int(logM+1)).</pre>
     *
     * <pre> -S
     *  Seed for random number generator.
     *  (default 1)</pre>
     *
     *  <pre> -F
     *      Fraction to training data to use for each Tree.
     *      (default 1.0);
     *  </pre>
     *
     * <pre> -depth &lt;num&gt;
     *  The maximum depth of the trees, 0 for unlimited.
     *  (default 0)</pre>
     *
     * <pre> -D
     *  If set, classifier is run in debug mode and
     *  may output additional info to the console</pre>
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception
    {
        String	tmpStr;

        tmpStr = Utils.getOption('I', options);
        if (tmpStr.length() != 0) {
            this.numberOfTrees = Integer.parseInt(tmpStr);
        } else {
            this.numberOfTrees = 10;
        }

        tmpStr = Utils.getOption('K', options);
        if (tmpStr.length() != 0) {
            this.numberOfVariables = Integer.parseInt(tmpStr);
        } else {
            this.numberOfVariables = RandomForestTree.NUMBER_OF_VARIABLES_SQRT;
        }

        tmpStr = Utils.getOption('M', options);
        if (tmpStr.length() != 0) {
            this.minObjects = Integer.parseInt(tmpStr);
        } else {
            this.minObjects = 2;
        }

        tmpStr = Utils.getOption('F', options);
        if (tmpStr.length() != 0) {
            this.trainFraction = Double.parseDouble(tmpStr);
        } else {
            this.trainFraction = 1.0;
        }

        tmpStr = Utils.getOption("depth", options);
        if (tmpStr.length() != 0) {
            setMaxDepth(Integer.parseInt(tmpStr));
        } else {
            setMaxDepth(0);
        }

        tmpStr = Utils.getOption('S', options);
        if (tmpStr.length() != 0) {
            setSeed(Integer.parseInt(tmpStr));
        } else {
            setSeed(1);
        }
        super.setOptions(options);

        Utils.checkForRemainingOptions(options);
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return the capabilities of this classifier
     */
    public Capabilities getCapabilities()
    {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);

        // class
        result.enable(Capabilities.Capability.NOMINAL_CLASS);

        return result;
    }

    public String toString()
    {
        if (this.randomForest == null) {
            return "Random forest not built yet";
        }
        else
        {
            StringBuffer temp = new StringBuffer();

            temp.append("Random forest of " + this.numberOfTrees
                    + " trees, each constructed while considering "
                    + (this.numberOfVariables==1?"sqrt of ":" all") + " attributes.\n"
                    + "Out of bag error: "
                    + Utils.doubleToString(this.randomForest.getOutOfBagError(), 4) + "\n"
                    + (getMaxDepth() > 0 ? ("Max. depth of trees: " + getMaxDepth() + "\n") : (""))
                    + "\n");

            return temp.toString();
        }
    }

    public String getRevision()
    {
        return RevisionUtils.extract("$Revision: 8892 $");
    }

    public static void main(String[] argv)
    {
        runClassifier(new CoreRandomForest(), argv);
    }
}
