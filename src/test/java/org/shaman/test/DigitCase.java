/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                     Technologies                      *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  $VER : DigitCase.java v1.0 ( Mar 2002 )              *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002 Shaman Research                   *
\*********************************************************/
package org.shaman.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.bayes.NaiveBayes;
import org.shaman.cbr.CBR;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.BatchPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.Estimator;
import org.shaman.learning.InstanceBatch;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.neural.KMER;
import org.shaman.neural.Lattice;
import org.shaman.neural.MLP;
import org.shaman.neural.MLPClassifier;
import org.shaman.neural.Neuron;
import org.shaman.preprocessing.Discretization;
import org.shaman.preprocessing.Normalization;
import org.shaman.preprocessing.PCA;
import org.shaman.rule.DecisionTree;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;


/**
 * <h2>MNIST Handwritten Digits Case</h2>
 * Test-Case using the famous MNIST handwritten digits data-set from
 * AT&T Research, Speech and Image Processing Services Research Lab.
 *
 * @author Johan Kaers
 * @version 2.0
 */

// Results as Error in 3-fold Cross-Validation:
// -------------------------------------------
// Discretization -> Naive Bayes                    : 0.1680
// Discretization -> Decision Tree                  : 0.2785
// Discretization -> Case-Based Reasoning           : 0.0851
// PCA -> Normalization -> 2 Layer MLP (100 epochs) : 0.1353
// PCA -> Normalization -> 2 Layer MLP (500 epochs) : 0.0777
// PCA -> Normalization -> 3 Layer MLP (500 epochs) : 0.0621

// **********************************************************\
// *            MNIST Handwritten Digits Test-Case          *
// **********************************************************/
public class DigitCase
{
   DataModel dataModel;

   private static final String FILE_DISCRETIZATION     = "./src/main/resources/data/mnist/digitcase_disc3.obj";
   private static final String FILE_PCA                = "./src/main/resources/data/mnist/digitcase_pca20.obj";
   private static final String FILE_PCA_DISCRETIZATION = "./src/main/resources/data/mnist/digitcase_pca_disc.obj";
   private static final String FILE_PCA_NORMALIZATION  = "./src/main/resources/data/mnist/digitcase_pca_norm.obj";
   private static final String FILE_PCA_NAIVE_BAYES    = "./src/main/resources/data/mnist/digitcase_pca_naivebayes.obj";
   private static final String FILE_PCA_DECISION_TREE  = "./src/main/resources/data/mnist/digitcase_pca_decision_tree.obj";
   private static final String FILE_NAIVE_BAYES        = "./src/main/resources/data/mnist/digitcase_naivebayes.obj";
   private static final String FILE_CBR                = "./src/main/resources/data/mnist/digitcase_cbr.obj";
   private static final String FILE_DECISION_TREE      = "./src/main/resources/data/mnist/digitcase_decision_tree.obj";
   private static final String FILE_MLP                = "./src/main/resources/data/mnist/digitcase_mlp.obj";
   private static final String FILE_KMER               = "./src/main/resources/data/mnist/digitcase_kmer.obj";

   // **********************************************************\
   // *       Low-Resolution Discretization of the digits      *
   // **********************************************************/
   private void trainDiscretization() throws ShamanException, IOException
   {
       // The MNIST Handwritten Letters DataSet
       MemorySupplier ms    = new MemorySupplier();
       Discretization disc  = new Discretization();
       InstanceSetMemory im = new InstanceSetMemory();
       disc.registerSupplier(0, ms, 0);
       ms.registerConsumer(0, disc, 0);
       
       TestSets.loadMNIST(ms);
       im.create(ms);
       
       // Low-Resolution is fine. The values are greyscale (0-255),
       // but in practice more like black and white.
       disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 3);
       disc.init();
       disc.trainTransformation(im);
       
       // Save the PCA to file for further usage
       ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(FILE_DISCRETIZATION));
       disc.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *           Calculate a PCA for the Digits               *
   // **********************************************************/
   private void trainPCA() throws ShamanException, IOException
   {
       // The MNIST Handwritten Letters DataSet
       MemorySupplier ms = new MemorySupplier();
       PCA           pca = new PCA();
       InstanceSetMemory im = new InstanceSetMemory();
       pca.registerSupplier(0, ms, 0);
       ms.registerConsumer(0, pca, 0);
       ms.registerConsumer(0, im, 0);
       
       TestSets.loadMNIST(ms);
       im.create(ms);
       
       pca.setType(PCA.TYPE_LINEAR);
       pca.setNumberOfPC(20);
       pca.init();
       pca.trainTransformation(im);
       
       // Save the PCA to file for further usage
       ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(FILE_PCA));
       pca.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *           Just Read all Data to check DataModel Fit    *
   // **********************************************************/
   private void readData() throws ShamanException
   {
       int i;
       
           // The MNIST Handwritten Letters DataSet
           MemorySupplier ms = new MemorySupplier();
           Normalization norm = new Normalization();
           InstanceSetMemory im = new InstanceSetMemory();
           norm.registerSupplier(0, ms, 0);
           ms.registerConsumer(0, norm, 0);
           ms.registerConsumer(0, im, 0);
           TestSets.loadMNIST(ms);
           norm.setType(Normalization.TYPE_STANDARDIZE);
           norm.init();
           
           // Train the normalizer on the input instances
           norm.trainTransformation(im);
           im.create(ms);
           
           // Create a set of normalized instances
           im = InstanceSetMemory.estimateAll(im, norm);
           
           // Create a batch presenter for this training data
           InstanceBatch ib = new InstanceBatch();
           ib.create(im, InstanceBatch.BATCH_REORDER, InstanceBatch.GOAL_BALANCE_CLASS, 0.1);
           ib.nextBatch();
           System.out.println("\nbegin");
           for (i=0; i<100; i++) { ib.nextBatch(); }
           System.out.println("end "+i+" batches");
   }
   
   // **********************************************************\
   // *             Train a Naive Bayes Classifier             *
   // **********************************************************/
   private void trainNaiveBayesAfterPCA() throws ShamanException, IOException
   {
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A Naive Bayes Classifier Flow for Handwritten Digit Recognition
       MemorySupplier   ms  = new MemorySupplier();
       PCA             pca  = new PCA();
       Discretization disc  = new Discretization();
       NaiveBayes       nb  = new NaiveBayes();
       
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0, pca, 0);
       pca.registerSupplier(0, ms, 0);
       pca.registerConsumer(0, disc, 0);
       disc.registerSupplier(0, pca, 0);
       disc.registerConsumer(0, nb, 0);
       nb.registerSupplier(0, disc, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the PCA and Discretization
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA));
       pca.loadState(oin);
       oin.close();
       pca.init();
       
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA_DISCRETIZATION));
       disc.loadState(oin);
       oin.close();
       disc.init();
       
       // Initialize the NaiveBayes
       nb.setClassifierOutput(Classifier.OUT_CLASS);
       nb.init();
       
       // Prepare training data
       im.create(ms);         // Make the DataSet of the raw image data
       estimateAll(im, pca);  // Linear PCA Transformation
       estimateAll(im, disc); // Discretized PCAed data
       
       // Run a Cross-Validation for the Naive Bayes
       System.out.println("Cross-Validation Naive Bayes after PCA");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, nb);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix  "+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last Naive Bayes Model
       oout = new ObjectOutputStream(new FileOutputStream(FILE_PCA_NAIVE_BAYES));
       nb.saveState(oout);
       oout.close();
   }
   
   private void trainNaiveBayes() throws ShamanException, IOException
   {
       // RESULTS: 83% Accuracy. Trained in ... ms.
       // -------
       // Confusion Matrix     10 x 10
       // [863    0  13  11   4  47  18   1  23   0
       //    1 1048  33   5   1  20   5   3  19   0
       //   35    6 861  28  17   6  21  12  45   1
       //   13   12  39 835   4  30   6  16  35  20
       //   13    0  17   0 760  11  20   5  20 136
       //   22    5  11  95  22 654  18   8  34  23
       //   29   12  18   1   4  42 844   0   8   0
       //   36    8  16   3  17   3   2 867  19  57
       //   16   25  23  58  15  36   4   9 763  25
       //   16    6   7  10  70  10   0  42  23 825]
       //  Classification Error 0.168
       
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A Naive Bayes Classifier Flow for Handwritten Digit Recognition
       MemorySupplier ms    = new MemorySupplier();
       Discretization disc  = new Discretization();
       NaiveBayes       nb  = new NaiveBayes();
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0, disc, 0);
       disc.registerSupplier(0, ms, 0);
       disc.registerConsumer(0, nb, 0);
       disc.registerConsumer(0, im, 0);
       nb.registerSupplier(0, disc, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the Discretization
       oin = new ObjectInputStream(new FileInputStream(FILE_DISCRETIZATION));
       disc.loadState(oin);
       oin.close();
       disc.init();
       
       // Initialize the NaiveBayes
       nb.setClassifierOutput(Classifier.OUT_CLASS);
       nb.init();
       
       // Make the Set of Discretized Digits
       im.create(ms);
       estimateAll(im, disc);
       
       // Run a Cross-Validation for the Naive Bayes
       System.out.println("Cross-Validation Naive Bayes");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, nb);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix"+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last Naive Bayes Model
       oout = new ObjectOutputStream(new FileOutputStream(FILE_NAIVE_BAYES));
       nb.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *           Train a CBR Classifier for the Task          *
   // **********************************************************/
   private void trainCBR() throws ShamanException, IOException
   {
       // NEEDS : Discretization Flow
       // Confusion Matrix10 x 10 matrix
       // 963    1   1   1   0   6   7   1   0   0
       //   1 1126   1   4   0   0   2   1   0   0
       //  19   29 910  11   6   3   6  37   8   3
       //   1   10  12 918   1  35   2  15   5  11
       //   1   20   2   0 881   3   3  10   2  60
       //   9   10   1  52   3 787  13   3   7   7
       //  11    6   0   0   2   5 933   0   1   0
       //   0   40   5   2   9   2   1 947   0  22
       //  14   20  14  45  12  42   5  11 796  15
       //   6   10   1   8  42   1   0  49   4 888
       //  Classification Error 0.0851
       
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A CBR Classifier Flow for Handwritten Digit Recognition
       MemorySupplier   ms  = new MemorySupplier();
       Discretization disc  = new Discretization();
       CBR             cbr  = new CBR();
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0,  disc, 0);
       disc.registerSupplier(0,  ms, 0);
       disc.registerConsumer(0, cbr, 0);
       disc.registerConsumer(0,  im, 0);
       cbr.registerSupplier(0, disc, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the Discretization
       oin = new ObjectInputStream(new FileInputStream(FILE_DISCRETIZATION));
       disc.loadState(oin);
       disc.init();
       
       // Initialize the CBR Flow
       cbr.setClassifierOutput(Classifier.OUT_CLASS);
       cbr.setKNearest(5);
       cbr.init();
       
       // Make the Set of Discretized Digits
       im.create(ms);
       estimateAll(im, disc);
       
       // Run a Cross-Validation for the Naive Bayes
       System.out.println("Cross-Validation Case-Based Reasoning");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, cbr);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix"+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last Case-Based Reasoning Model
       oout = new ObjectOutputStream(new FileOutputStream(FILE_CBR));
       //cbr.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *                      Train a MLP                       *
   // **********************************************************/
   private void trainMLP() throws ShamanException, IOException
   {
       // NEEDS : PCA Flow, Normalization after PCA Flow
       // RESULTS for
       // -----------
       //   Network          : 20 (Standardized PCA Input) -> 300 Hidden -> 10 Output (one-of-n)
       //   Back-Propagation : 0.01, true, 0.9
       //   Neurons          : SIGMOID_EXP, .5
       //   Batch            : BATCH_REORDER, GOAL_BALANCE_NONE, 0.1
       //   Epochs           : 100
       //
       //   Confusion Matrix
       //   938    0   3   6   0   9  17   1   6   0
       //     0 1110   3   2   1   3   5   0  11   0
       //    18   11 841  27  23   5  36  26  40   5
       //     7    8  18 865   5  35   5  20  30  17
       //     6    9   5   3 871   5  23   2   7  51
       //    23   19   4  50  33 680  23  13  37  10
       //    24   13   5   1   8   8 889   0  10   0
       //    11   19  18   4  11   3   7 886   9  60
       //    27   25  12  44  15  50   9  10 769  13
       //    17    9   5  22  93  13   0  44   8 798
       //   Classification Error 0.1353
       //
       //   Epochs           : 500
       //   Confusion Matrix10 x 10 matrix
       //   946    0   3   3   1  10  10   4   2   1
       //     0 1114   4   4   1   3   3   3   3   0
       //     6    3 930  16  15   8  11  23  17   3
       //     0    2  13 938   1  18   0  13  13  12
       //     2    4   5   3 915   0  12   4   6  31
       //    10    2   4  34   9 792  18   6  13   4
       //     9    3   4   2   5  10 919   1   5   0
       //     3    8  16   7  10   4   0 943   1  36
       //     3    5  11  43  12  30   5  13 846   6
       //     7    7   2  19  48   7   0  31   8 880
       //   Classification Error 0.0777
       //
       //  Network          : 20 (Standardized PCA Input) -> 300 Hidden1 -> 100 Hidden2 -> 10 Output (one-of-n)
       //  Confusion Matrix10 x 10 matrix
       //  949    0   7   1   0  10   6   2   4   1
       //    0 1115   5   5   1   0   1   2   5   1
       //    7    2 971   6   3   7   8  11  13   4
       //    0    3  14 936   1  14   0  12  21   9
       //    1    1   9   0 912   1   7   2  10  39
       //    8    2   4  14   5 827   8   3  16   5
       //    7    4   5   0   5  11 918   2   6   0
       //    2    4  14   5   9   0   1 953   9  31
       //    6    5  15  20   6  22   3   6 884   7
       //    5    4   3   9  33   9   0  24   8 914
       //  Classification Error 0.0621
       
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A MLP Classifier Flow for Handwritten Digit Recognition
       MemorySupplier    ms = new MemorySupplier();
       PCA              pca = new PCA();
       Normalization   norm = new Normalization();
       MLPClassifier   mlpc = new MLPClassifier();
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0,   pca, 0);
       pca.registerSupplier(0,  ms, 0);
       pca.registerConsumer(0,  norm, 0);
       norm.registerSupplier(0, pca, 0);
       norm.registerConsumer(0, mlpc, 0);
       norm.registerConsumer(0, im, 0);
       mlpc.registerSupplier(0, norm, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the PCA and Normalization
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA));
       pca.loadState(oin);
       oin.close();
       pca.init();
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA_NORMALIZATION));
       norm.loadState(oin);
       oin.close();
       norm.init();
       
       // Initialize the MLP Flow
       MLP mlp = new MLP();
       mlp.setBackPropagationParameters(0.01, true, 0.9, 100);
       mlp.setBatchParameters(BatchPresenter.BATCH_REORDER, BatchPresenter.GOAL_BALANCE_NONE, 0.1);
       mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
       mlp.setNetworkParameters(300, 0, MLP.OUTPUT_ONE_OF_N);
       mlpc.setClassifierOutput(Classifier.OUT_CLASS);
       mlpc.setMLP(mlp);
       mlpc.init();
       
       // Make the instance set
       im.create(ms);
       estimateAll(im, pca);
       estimateAll(im, norm);
       
       // Run a Cross-Validation for the MLP
       System.out.println("Cross-Validation Multi-Layer Perceptron");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, mlpc);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix "+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last MLP
       oout = new ObjectOutputStream(new FileOutputStream(FILE_MLP));
       mlp.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *                  Train After PCA Stuff                 *
   // **********************************************************/
   private void trainAfterPCA() throws ShamanException, IOException
   {
       // NEEDS : PCA Flow
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // Train some Preprocessing Flows for after the PCA
       MemorySupplier   ms  = new MemorySupplier();
       PCA             pca  = new PCA();
       Discretization  disc = new Discretization();
       Normalization   norm = new Normalization();
       ms.registerConsumer(0,  pca, 0);
       pca.registerSupplier(0, ms, 0);
       pca.registerConsumer(0, disc, 0);
       pca.registerConsumer(0, norm, 0);
       disc.registerSupplier(0, pca, 0);
       norm.registerSupplier(0, pca, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the PCA
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA));
       pca.loadState(oin);
       oin.close();
       pca.init();
       
       // Configure the Flows
       norm.setType(Normalization.TYPE_STANDARDIZE);
       disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
       //disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 5);
       
       norm.init();
       disc.init();
       
       // Train the Flows
       InstanceSetMemory im = new InstanceSetMemory();
       im.create(ms);
       estimateAll(im, pca);
       
       norm.trainTransformation(im);
       disc.trainTransformation(im);
       
       // Save the Flows
       oout = new ObjectOutputStream(new FileOutputStream(FILE_PCA_NORMALIZATION));
       norm.saveState(oout);
       oout.close();
       oout = new ObjectOutputStream(new FileOutputStream(FILE_PCA_DISCRETIZATION));
       disc.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *                 Train a KMER Network                   *
   // **********************************************************/
   private void trainKMER() throws ShamanException, IOException
   {
       // NEEDS : PCA Flow, Normalization after PCA
       // QUICK AND DIRTY RESULT on TrainSet :
       //       Average Error by KMER 2.395563731545375 on average norm of 19.999999999999947
       
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A KMER Self-Organizing Map.
       MemorySupplier    ms = new MemorySupplier();
       PCA              pca = new PCA();
       Normalization   norm = new Normalization();
       KMER            kmer = new KMER();
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0,    pca, 0);
       pca.registerSupplier(0,    ms, 0);
       pca.registerConsumer(0,  norm, 0);
       norm.registerSupplier(0,  pca, 0);
       norm.registerConsumer(0, kmer, 0);
       norm.registerConsumer(0,   im, 0);
       kmer.registerSupplier(0, norm, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the PCA and Normalization
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA));
       pca.loadState(oin);
       oin.close();
       pca.init();
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA_NORMALIZATION));
       norm.loadState(oin);
       oin.close();
       norm.init();
       
       // Create the PCA and Normalized data
       im.create(ms);
       estimateAll(im, pca);
       estimateAll(im, norm);
       
       // Initialize the KMER Flow
       kmer.setBatchParameters(BatchPresenter.BATCH_REORDER, BatchPresenter.GOAL_BALANCE_NONE, 0.1);
       kmer.setLatticeParameters(15, 15, 0, Lattice.NEIGHBORHOOD_GAUSSIAN, true);
       kmer.setKMERParameters(0.001, 1, 500);
       kmer.init();
       kmer.trainTransformation(im);
       
       // Quick and Dirty Test.
       //       oin = new ObjectInputStream(new FileInputStream(FILE_KMER));
       //        os = (ObjectState)oin.readObject();
       //        kmer.setStructure(os);
       //       oin.close();
       //       kmer.init();
       
       int            i;
       DoubleMatrix1D innow;
       double         mag, err;
       double         amag, aerr;
       
       amag = 0; aerr = 0;
       for (i=0; i<im.getNumberOfInstances(); i++)
       {
           innow = im.getInstance(i);
           mag   = Algebra.DEFAULT.norm2(innow);
           err   = kmer.estimateError(innow);
           amag += mag; aerr += err;
           //System.out.println("Norm "+mag+" Error by KMER "+err);
       }
       amag /= im.getNumberOfInstances(); aerr /= im.getNumberOfInstances();
       System.out.println("Average Error by KMER "+aerr+" on "+amag);
       
       // Save the last KMER
       oout = new ObjectOutputStream(new FileOutputStream(FILE_KMER));
       //kmer.saveState(oout);
       oout.close();
   }
   
   // **********************************************************\
   // *           Train a Decision Tree Classifier             *
   // **********************************************************/
   private void trainDecisionTreeAfterPCA() throws ShamanException, IOException
   {
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A Decision Tree Classifier Flow for Handwritten Digit Recognition
       MemorySupplier   ms  = new MemorySupplier();
       PCA             pca  = new PCA();
       Discretization disc  = new Discretization();
       DecisionTree     dt  = new DecisionTree();
       
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0, pca, 0);
       pca.registerSupplier(0, ms, 0);
       pca.registerConsumer(0, disc, 0);
       disc.registerSupplier(0, pca, 0);
       disc.registerConsumer(0, dt, 0);
       dt.registerSupplier(0, disc, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the PCA and Discretization
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA));
       pca.loadState(oin);
       oin.close();
       pca.init();
       
       oin = new ObjectInputStream(new FileInputStream(FILE_PCA_DISCRETIZATION));
       disc.loadState(oin);
       oin.close();
       disc.init();
       
       // Initialize the Decision Tree Flow
       dt.setClassifierOutput(Classifier.OUT_CLASS);
       dt.setPruneType(DecisionTree.PRUNE_NONE);
       dt.init();
       
       // Prepare training data
       im.create(ms);         // Make the DataSet of the raw image data
       estimateAll(im, pca);  // Linear PCA Transformation
       estimateAll(im, disc); // Discretized PCAed data
       
       // Run a Cross-Validation for the Naive Bayes
       System.out.println("Cross-Validation Decision Tree after PCA");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, dt);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix  "+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last Naive Bayes Model
       oout = new ObjectOutputStream(new FileOutputStream(FILE_PCA_DECISION_TREE));
       dt.saveState(oout);
       oout.close();
   }
   
   private void trainDecisionTree() throws ShamanException, IOException
   {
       // NEEDS : Discretization Flow
       // RESULTS:
       // 849    1  21  12   7  27  39   9   8   7
       //   2 1058  20  12   2   7   8   8  14   4
       //  21   29 675  33  32  36  90  35  50  31
       //  19   24  42 696  20  94  15  13  55  32
       //  10    6  44  16 687  25  53  10  38  93
       //  24   11  35  88  28 541  63  21  43  38
       //  26    9  59  15  41  45 695  15  30  23
       //  10   20  54  10  19  17  15 816  17  50
       //  14   36  73  37  32  68  62  18 566  68
       //  19   10  32  42  97  49  24  56  48 632
       //  Classification Error 0.2785
       
       ObjectInputStream  oin;
       ObjectOutputStream oout;
       
       // A CBR Classifier Flow for Handwritten Digit Recognition
       MemorySupplier   ms  = new MemorySupplier();
       Discretization disc  = new Discretization();
       DecisionTree     dt  = new DecisionTree();
       InstanceSetMemory im = new InstanceSetMemory();
       ms.registerConsumer(0,  disc, 0);
       disc.registerSupplier(0,  ms, 0);
       disc.registerConsumer(0, dt, 0);
       disc.registerConsumer(0,  im, 0);
       dt.registerSupplier(0, disc, 0);
       
       // Load the Data-Set
       TestSets.loadMNIST(ms);
       
       // Load the Discretization
       oin = new ObjectInputStream(new FileInputStream(FILE_DISCRETIZATION));
       disc.loadState(oin);
       oin.close();
       disc.init();
       
       // Initialize the Decision Tree Flow
       dt.setClassifierOutput(Classifier.OUT_CLASS);
       dt.setPruneType(DecisionTree.PRUNE_NONE);
       dt.init();
       
       // Make the Set of Discretized Digits
       im.create(ms);
       estimateAll(im, disc);
       
       // Run a Cross-Validation for the Naive Bayes
       System.out.println("Cross-Validation Decision Tree Classifier");
       double       [][]cmraw;
       DoubleMatrix2D cm;
       Validation val = new Validation(im, dt);
       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
       val.test();
       cmraw = val.getValidationClassifier().getConfusionMatrix();
       cm = DoubleFactory2D.dense.make(cmraw);
       System.out.println("Confusion Matrix"+cm);
       System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError());
       System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
       
       // Save the last Decision Tree Model
       oout = new ObjectOutputStream(new FileOutputStream(FILE_DECISION_TREE));
       dt.saveState(oout);
       oout.close();
   }
   
   private void estimateAll(InstanceSetMemory im, Estimator est) throws ShamanException
   {
       int               i;
       DoubleMatrix1D  []insin;
       
       insin  = im.getInstances();
       for (i=0; i<insin.length; i++) insin[i] = est.estimate(insin[i]);
       im.setDataModel(((Transformation)est).getOutputDataModel(0));
   }

   // **********************************************************\
   // *                Handwritten Digit Test Case             *
   // **********************************************************/
   public static void main (String []arg)
   {
     System.out.println("MNIST Handwritten Digit Test Case");
     System.out.println("---------------------------------");

     DigitCase instance;

     try
     {
       instance = new DigitCase();
       //instance.readData();
       //instance.trainPCA();
       //instance.trainAfterPCA();
       //instance.trainDiscretization();
       //instance.trainKMER();
       //instance.trainNaiveBayes();   // Takes +- 5 minutes.
       //instance.trainMLP();          // Takes +- 1 hour.
       //instance.trainDecisionTree(); // Takes +- 30 minutes.
       //instance.trainCBR();          // TEST AT NIGHT. Very SLOW...
       //instance.trainNaiveBayesAfterPCA();
       instance.trainDecisionTreeAfterPCA();
     }
     catch(Exception ex) { ex.printStackTrace(); }
   }
}
