package org.shaman.spatial.weka;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * This class is the utility for a) Reading any file format like arff,csv,
 * libsvm etc, b) Partitioning the data into smaller subsets, keeping the same
 * distribution as original c) Getting a Naive Bayes Classifier instance d)
 * An example of reweighting
 * @author udaykamath
 * 
 */

public class WekaUtil {

	/**
	 * String arff,csv, libsvm file names
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static Instances readInstancesFromFile(String fileName)
			throws Exception {
		DataSource source = new DataSource(fileName);
		Instances data = source.getDataSet();
		return data;
	}

	

	public static Instances[] partitionDataSetToSamples(Instances totalSet,
			int samples) throws Exception {
		Instances[] resampledInstances = new Instances[samples];
		// User has provided a random number seed.
		totalSet.randomize(new Random(1));
		// Select out a fold
		totalSet.stratify(samples);
		for (int i = 0; i < samples; i++) {
			resampledInstances[i] =totalSet.testCV(samples, i );
		}
		return resampledInstances;
	}

	/**
	 * Create an instance of NaiveBayes Classifier
	 * 
	 * @param useDiscretization
	 * @param useKernelEstimator
	 * @return
	 * @throws Exception
	 */
	public static NaiveBayes createNaiveBayesClassifier(
			boolean useDiscretization, boolean useKernelEstimator)
			throws Exception {
		NaiveBayes classifier = new NaiveBayes();
		List<String> options = new ArrayList<String>();
		// check for useDiscreatization
		if (useDiscretization) {
			options.add("-D");
		} else if (useKernelEstimator) {
			options.add("-K");
		}
		classifier.setOptions(options.toArray(new String[0]));
		return classifier;
	}

	public static J48 createDecisionTreeClassifier() throws Exception {
		J48 decisionTree = new J48();
		return decisionTree;
	}

	public static void main(String[] args) {
		try {
			// read the data set
			String arffFile = "./data/iris.arff";
			Instances totalSet = WekaUtil.readInstancesFromFile(arffFile);
			// set the class
			Attribute classAttribute = totalSet.attribute("class");
			totalSet.setClass(classAttribute);
			// lets divide the dataset into 3 parts , 2 neighbours
			Instances[] samples = WekaUtil.partitionDataSetToSamples(totalSet,
					3);
			for (int i = 0; i < samples.length; i++) {
				Instances sample = samples[i];
				System.out.println(samples[i].size());
			}
			// create a NaiveBayes classifier
			NaiveBayes classifier = WekaUtil.createNaiveBayesClassifier(true,
					false);
			// train the classifier
			classifier.buildClassifier(samples[0]);
			// lets output the model for display
			System.out.println(classifier.toString());
			// lets get the classifier tested on separate sample
			for (int i = 0; i < samples[1].size(); i++) {
				// confidence of prediction
				double[] confidence = classifier
						.distributionForInstance(samples[1].get(i));
				System.out.println("Confidences are " + confidence[0] + " "
						+ confidence[1]);
			}
			// weight adjustment and normalization
			Instances training = samples[0];
			double oldSumOfWeights = training.sumOfWeights();
			Enumeration enu = training.enumerateInstances();
			while (enu.hasMoreElements()) {
				Instance instance = (Instance) enu.nextElement();
				double[] confidence = classifier
						.distributionForInstance(instance);
				// which one has more confidence
				double reweight = (confidence[0] > confidence[1]) ? confidence[0]
						: confidence[1];
				// new weight
				instance.setWeight(instance.weight() * reweight);
			}

			// Renormalize weights
			double newSumOfWeights = training.sumOfWeights();
			enu = training.enumerateInstances();
			while (enu.hasMoreElements()) {
				Instance instance = (Instance) enu.nextElement();
				instance.setWeight(instance.weight() * oldSumOfWeights
						/ newSumOfWeights);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
