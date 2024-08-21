/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *              Artificial Immune Systems                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.immune.core;

import java.io.FileWriter;

import junit.framework.TestCase;

import org.shaman.dataflow.NetworkConnection;
import org.shaman.dataflow.NetworkNode;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.AttributePropertyFuzzyContinuous;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.FMFContinuous;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierNetwork;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.LearnerComparison;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.preprocessing.Discretization;
import org.shaman.preprocessing.Fuzzyfication;
import org.shaman.util.FileUtil;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.random.Uniform;


// **********************************************************\
// *        Artificial Immune Systems Package Tests         *
// **********************************************************/
public class TestBody extends TestCase
{
    // **********************************************************\
    // *             Repeat N-Fold Cross Validation             *
    // **********************************************************/
    private double []repeatCrossValidation(String name, int numrep, int numfold, Classifier bod, InstanceSetMemory im) throws LearnerException
    {
        Validation           val;
        ValidationClassifier valclas;
        int                  i;
        double             []err;
        double               mean, var;
        
        mean = 0;
        err  = new double[numrep];
        for (i=0; i<numrep; i++)
        {       
            val = new Validation(im, bod);
            val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{numfold});
            val.test();
            
            valclas = val.getValidationClassifier();
            err[i]  = valclas.getClassificationError();
            mean   += err[i];
        }
        mean /= numrep;
        
        var = 0;
        for (i=0; i<numrep; i++) var += (err[i]-mean)*(err[i]-mean);
        var = Math.sqrt(var);
        
        System.out.println(name+": average classification Error : "+mean+" variance "+var);
        for (i=0; i<numrep; i++)
        {
            System.out.println("Error run "+i+" = "+err[i]);
        }
        
        return(new double[]{mean, var});
    }
    
    // **********************************************************\
    // *         Classifier Comparison and Reporting            *
    // **********************************************************/
    private int compareClassifiers(String name, int numfolds, int numrepeats, InstanceSetMemory im, Transformation cl1, Transformation cl2) throws LearnerException
    {
        LearnerComparison lc = new LearnerComparison();
        lc.init(im, (Classifier)cl1, (Classifier)cl2, numfolds, numrepeats);
        lc.testClassifiers();
        
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        System.out.println("And the winner is '"+((Transformation)lc.giveBestClassifier()).getName()+"'");
        System.out.println("Average classification error for '"+cl1.getName()+"' = "+lc.giveLearner1Error()+". Variance "+lc.giveLearner1Variance());
        System.out.println("Average classification error for '"+cl2.getName()+"' = "+lc.giveLearner2Error()+". Variance "+lc.giveLearner2Variance());
        
        if (lc.giveLearner1Error() > lc.giveLearner2Error()) return(2);
        else                                                 return(1);
    }
    
    // **********************************************************\
    // *                Random Bit String Modification          *
    // **********************************************************/
    public void doNot_testComparisonMHCRandom() throws Exception
    {
        int            i,j,k,l;
        int            selind, pos;
        int            numbits, matchlen, numchange, numself, numrep, numdet, match, match2, numtest;
        double         res, res2;
        DoubleMatrix1D []random;
        DoubleMatrix1D datnow;
        
        System.out.println("Random Bitstring Modification Experiment");
        
        // Set the Experiment Parameters
        numbits   = 32;
        numself   = 100;
        matchlen  = 8;
        numchange = 3;
        numdet    = 200;
        numtest   = 1000;
        
        // Make a Body with a Random Self Set.
        FunctionSupplier  fs  = new FunctionSupplier();
        Body             bod  = new Body();
        CompoundBody     bod2 = new CompoundBody();
        InstanceSetMemory im  = new InstanceSetMemory();
        Body            tbod;
        
        bod2.grow(4);
        
        fs.registerConsumer  (0, bod,  0);
        fs.registerConsumer  (0, bod2, 0);
        fs.registerConsumer  (0, im,   0);
        bod.registerSupplier (0, fs,   0);
        bod2.registerSupplier(0, fs,   0);
        
        fs.setParameters(FunctionSupplier.TYPE_RANDOM_BITSTRING, numself, numbits);
        fs.init();
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, numdet);
        bod.init();
        
        bod2.setDataRepresentation(Body.DATA_FUZZY);
        bod2.setCrisp(true);
        bod2.setMHC(Morphology.MHC_RANDOM);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, numdet);
        bod2.init();
        
        im.create(fs);
        
        // Do a hand-made comparison between the 2 classifiers
        numtest = 1000;
        numrep  = 10;
        
        LearnerComparison lc;
        double          []clerr1, clerr2;
        
        lc     = new LearnerComparison();
        lc.setNumberOfRepeats(numrep);
        clerr1 = new double[numrep];
        clerr2 = new double[numrep];
        
        // Repeat the experiment a number of times.
        for (i=0; i<numrep; i++)
        {
            res  = 0;
            res2 = 0;
            
            // Create new set of random strings
            fs.init();
            im.create(fs);
            random = im.getInstances();
            
            // Test whether a modified self string is detected.
            bod.trainTransformation(im);
            bod2.trainTransformation(im);
            
            numchange = 1;
            for (k=0; k<numtest; k++)
            {
                selind = Uniform.staticNextIntFromTo(0, numself-1);
                pos    = Uniform.staticNextIntFromTo(0, numbits-numchange-1);
                datnow = random[selind].copy();
                for (l=pos; l<pos+numchange; l++) datnow.setQuick(l, 1.0-datnow.getQuick(l));
                
                match  = bod.classify(datnow);
                match2 = bod2.classify(datnow);
                if (match  != 1) {  res++;  }
                if (match2 != 1) {  res2++; }
            }
            res  /= numtest;
            res2 /= numtest;
            
            clerr1[i] = res;
            clerr2[i] = res2;
            
            System.out.println("Random Bit-String modification "+res+" with MHC "+res2);
        }
        
        // Calculate the t-test statistics
        lc.prepareStatistics(clerr1, clerr2, numrep);
        
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        //System.out.println("And the winner is '"+((Transformation)lc.giveBestClassifier()).getName()+"'");
        System.out.println("Average classification error for '"+bod.getName()+"' = "+lc.giveLearner1Error()+". Variance "+lc.giveLearner1Variance());
        System.out.println("Average classification error for '"+bod2.getName()+"' = "+lc.giveLearner2Error()+". Variance "+lc.giveLearner2Variance());
    }
    
    // **********************************************************\
    // *                Hollow Sphere Testing                   *
    // **********************************************************/
    public void dont_testCrispSphere() throws Exception
    {
        FunctionSupplier  fs     = new FunctionSupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",             "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        fs.registerConsumer(0, bodnet, 0);
        fs.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, fs, 0);
        
        fs.setParameters(FunctionSupplier.TYPE_3D,0,0);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 1000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        fs.init();
        bodnet.init();
        
        // Create an Instance Set for Training
        im = fs.createHollowSphere(10, 1.0);
        //im = fs.createHollowCircle(1000, 0.6);
        
        double []res = repeatCrossValidation("Crisp Hollow Sphere", 3, 10, bodnet, im);
        System.out.println(res[0]+" "+res[1]);
        assertTrue(res[0] < 0.55);
    }
    
    // **********************************************************\
    // *                 MHC-Non MHC Comparisons                *
    // **********************************************************/
    public void doNot_testComparisonMHCBitCancer() throws Exception
    {
        DataModel  dm;
        int               i;
        
        // Load a Test Set.
        MemorySupplier     ms = new MemorySupplier();
        Body             bod1 = new Body();
        Body             bod2 = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, bod1, 0);
        ms.registerConsumer(0, bod2, 0);
        ms.registerConsumer(0, im, 0);
        bod1.registerSupplier(0, ms, 0);
        bod2.registerSupplier(0, ms, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        // Configure the Bodies
        bod1.setDataRepresentation(Body.DATA_BIT);
        bod1.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod1.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod1.setDetectorParameters(false, 0.0, 2000);
        bod1.getMorphology().setMHC(Morphology.MHC_NONE);
        bod1.init();
        
        bod2.setDataRepresentation(Body.DATA_BIT);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, 2000);
        bod2.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod2.init();
        
        bod1.setName("Body no MHC");
        bod2.setName("Body MHC");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Compare both Learners
        LearnerComparison lc = new LearnerComparison();
        lc.init(im, bod1, bod2, 3, 5);
        lc.testClassifiers();
        
        System.out.println("Can I be 95% sure that they are different? "+lc.areClassifiersDifferent(0.95));
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        System.out.println("And the winner is "+((Transformation)lc.giveBestClassifier()).getName());
    }
    
    public void doNot_testComparisonMHCBitZoo() throws Exception
    {
        DataModel  dm;
        int               i;
        
        // Load a Test Set.
        MemorySupplier     ms = new MemorySupplier();
        Body             bod1 = new Body();
        Body             bod2 = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, bod1, 0);
        ms.registerConsumer(0, bod2, 0);
        ms.registerConsumer(0, im, 0);
        bod1.registerSupplier(0, ms, 0);
        bod2.registerSupplier(0, ms, 0);
        
        TestSets.loadZoo(ms, true);
        
        // Configure the Bodies
        bod1.setDataRepresentation(Body.DATA_BIT);
        bod1.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod1.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod1.setDetectorParameters(false, 0.0, 100);
        bod1.getMorphology().setMHC(Morphology.MHC_NONE);
        bod1.init();
        
        bod2.setDataRepresentation(Body.DATA_BIT);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, 100);
        bod2.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod2.init();
        
        bod1.setName("Body no MHC");
        bod2.setName("Body MHC");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Compare both Learners
        LearnerComparison lc = new LearnerComparison();
        lc.init(im, bod1, bod2, 10, 10);
        lc.testClassifiers();
        
        System.out.println("Can I be 95% sure that they are different? "+lc.areClassifiersDifferent(0.95));
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        System.out.println("And the winner is "+((Transformation)lc.giveBestClassifier()).getName());
    }
    
    public void doNot_testComparisonMHCBitRandom() throws Exception
    {
        AntigenBit     agen;
        int            i,j,k,l;
        int            selind, pos;
        int            numbits, matchlen, numchange, numself, numdet;
        double         res;
        DoubleMatrix1D []random;
        DoubleMatrix1D datnow;
        FileWriter     fout;
        
        System.out.println("Random Bitstring Modification Experiment");
        
        // Set the Experiment Parameters
        numbits   = 32;
        numself   = 100;
        matchlen  = 8;
        numchange = 3;
        numdet    = 100;
        
        // Make a Body with a Random Self Set.
        FunctionSupplier  fs = new FunctionSupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        fs.registerConsumer(0, bod, 0);
        fs.registerConsumer(0, im, 0);
        bod.registerSupplier(0, fs, 0);
        
        fs.setParameters(FunctionSupplier.TYPE_RANDOM_BITSTRING, numself, numbits);
        fs.setGoal(true);
        fs.init();
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.setDetectorParameters(false, 0.0, numdet);
        bod.init();
        
        im.create(fs);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    public void doNot_testComparisonMHCCrispCancer() throws Exception
    {
        DataModel  dm;
        int               i;
        
        // 10 times repeated 10-fold cross validation classifier comparison
        // Can I be 95% sure that they are different? false
        // You can be 51.68651792253419% sure they're different
        
        // Load a Test Set.
        MemorySupplier     ms = new MemorySupplier();
        Body             bod1 = new Body();
        Body             bod2 = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, bod1, 0);
        ms.registerConsumer(0, bod2, 0);
        ms.registerConsumer(0, im, 0);
        bod1.registerSupplier(0, ms, 0);
        bod2.registerSupplier(0, ms, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        // Configure the Bodies
        bod1.setDataRepresentation(Body.DATA_FUZZY);
        bod1.setCrisp(true);
        bod1.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        bod1.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod1.setDetectorParameters(false, 0.0, 1000);
        bod1.getMorphology().setMHC(Morphology.MHC_NONE);
        bod1.init();
        
        bod2.setDataRepresentation(Body.DATA_FUZZY);
        bod2.setCrisp(true);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, 1000);
        bod2.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod2.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Compare both Learners
        LearnerComparison lc = new LearnerComparison();
        lc.init(im, bod1, bod2, 10, 10);
        lc.testClassifiers();
        
        System.out.println("Can I be 95% sure that they are different? "+lc.areClassifiersDifferent(0.95));
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        System.out.println("And the winner is "+((Transformation)lc.giveBestClassifier()).getName());
    }
    
    public void dont_testComparisonMHCCrispIris() throws Exception
    {
        MemorySupplier    ms       = new MemorySupplier();
        ClassifierNetwork net  = new ClassifierNetwork();
        ClassifierNetwork net2 = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Discretization    disc2;
        Body              bod, bod2;
        
        // 10 times repeated 10-fold cross validation classifier comparison
        // Can I be 95% sure that they are different? true
        // You can be 95.21330452643704% sure they're different
        // And the winner is MHC Body
        
        NetworkNode []crispnod = new NetworkNode[]
                                                 {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                                 };
        NetworkConnection []crispcon = new NetworkConnection[]
                                                             {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                             };
        NetworkNode []fuznod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                               };
        NetworkConnection []fuzcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        
        net.grow(1,1);
        net2.grow(1,1);
        net.populate(crispnod, crispcon);
        net2.populate(fuznod, fuzcon);
        disc     = (Discretization)net.getTransformation("Discretization");
        disc2    = (Discretization)net2.getTransformation("Discretization");
        bod      = (Body)net.getTransformation("Body");
        bod2     = (Body)net2.getTransformation("Body");
        
        ms.registerConsumer(0, net, 0);
        ms.registerConsumer(0, net2,   0);
        ms.registerConsumer(0, im,     0);
        net.registerSupplier(0, ms, 0);
        net2.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms);
        
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        disc2.setParameters(Discretization.TYPE_EQUAL_INTERVALS,10);
        
        // Crisp Data Representation
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 300);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        // MHC Crisp
        bod2.setDataRepresentation(Body.DATA_FUZZY);
        bod2.setCrisp(true);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, 300);
        bod2.getMorphology().setMHC(Morphology.MHC_RANDOM);
        
        net.init();
        net2.init();
        
        net.setName("Body");
        net2.setName("MHC Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Compare both learners
        LearnerComparison lc = new LearnerComparison();
        lc.init(im, net, net2, 10, 10);
        lc.testClassifiers();
        
        System.out.println("Can I be 95% sure that they are different? "+lc.areClassifiersDifferent(0.95));
        System.out.println("You can be "+lc.giveDifferentProbability()*100+"% sure they're different");
        System.out.println("And the winner is "+((Transformation)lc.giveBestClassifier()).getName());
    }
    
    // **********************************************************\
    // *             Mushroom Edibility AIS Test                *
    // **********************************************************/
    public void dont_testBitMushrooms() throws ShamanException
    {
        System.out.println("Mushrooms Edibility Classification");
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadMushrooms(ms);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 15);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 10000);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bod.init();
        
        im.create(ms);
        
        // Len=15, #det=10000, 2-fold x-val
        // 4195,0   12,0
        //  380,0 3536,0
        // Classification Error : 0.04825803274652222
        
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2.0});
        //val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.75});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
        
        assertTrue(valclas.getClassificationError() < 0.07);
    }
    
    public void dont_testCrispMushrooms() throws ShamanException
    {
        System.out.println("Mushrooms Edibility Classification");
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadMushrooms(ms);
        
        // len=8, #det=50000, 2fold x-val ->
        // Confusion
        // 4191,0   16,0
        //   84,0 3832,0
        // Classification Error : 0.012310722639418934
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 8);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 50000);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bod.init();
        
        im.create(ms);
        
        double []res = repeatCrossValidation("Mushrooms", 1, 10, bod, im);
        assertTrue(res[0] < 0.25);
    }
    
    
    // **********************************************************\
    // *                  Iris Classification                   *
    // **********************************************************/
    public void dont_testCrispCompoundIris() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im    = new InstanceSetMemory();
        Discretization    disc;
        CompoundBody      bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization","org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.CompoundBody",     new int[]{3}, "Artificial Immune System",         1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0,  "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (CompoundBody)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms);
        
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 8);
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 300);
        bod.setMHC(Morphology.MHC_RANDOM);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        double []res = repeatCrossValidation("Crisp MHC Iris", 10, 10, bodnet, im);
        System.out.println(res[0]+" "+res[1]);
        assertTrue(res[0] < 0.16);
    }
    
    public void dont_testComparisonMHCIris() throws Exception
    {
        // You can be 99.93038155412968% sure they're different
        // And the winner is 'MHC Body'
        // Average classification error for 'Body'     = 0.1630872483221476.  Variance 0.08444115966470657
        // Average classification error for 'MHC Body' = 0.10827740492170025. Variance 0.3123726712356199
        
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork nonnet = new ClassifierNetwork();
        ClassifierNetwork mhcnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Fuzzyfication     fuz1, fuz2;
        Body              nonbod;
        CompoundBody      mhcbod;
        
        NetworkNode []nonnod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication", "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.Body",          "Artificial Immune System",         1)
                                               };
        NetworkConnection []noncon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",          0),
                new NetworkConnection("Body",          0, null,            0)
                                                           };
        NetworkNode []mhcnod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication",  "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.CompoundBody",     new int[]{3}, "Artificial Immune System",         1)
                                               };
        NetworkConnection []mhccon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",          0),
                new NetworkConnection("Body",          0, null,            0)
                                                           };
        
        nonnet.grow(1,1);
        mhcnet.grow(1,1);
        nonnet.populate(nonnod, noncon);
        mhcnet.populate(mhcnod, mhccon);
        fuz1     = (Fuzzyfication)nonnet.getTransformation("Fuzzyfication");
        fuz2     = (Fuzzyfication)mhcnet.getTransformation("Fuzzyfication");
        nonbod   = (Body)nonnet.getTransformation("Body");
        mhcbod   = (CompoundBody)mhcnet.getTransformation("Body");
        
        ms.registerConsumer(0, nonnet, 0);
        ms.registerConsumer(0, mhcnet, 0);
        ms.registerConsumer(0, im,     0);
        nonnet.registerSupplier(0, ms, 0);
        mhcnet.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms, true);
        
        fuz1.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 9);
        fuz2.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 9);
        
        FMFContinuous.setOverlap(2.0);
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.01);
        
        // Non MHC body
        nonbod.setDataRepresentation(Body.DATA_FUZZY);
        nonbod.setCrisp(false);
        nonbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        nonbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        nonbod.setDetectorParameters(false, 0.0, 1000);
        nonbod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        // MHC Compound Body
        mhcbod.setDataRepresentation(Body.DATA_FUZZY);
        mhcbod.setCrisp(false);
        mhcbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        mhcbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        mhcbod.setDetectorParameters(false, 0.0, 1000);
        mhcbod.setMHC(Morphology.MHC_RANDOM);
        
        nonnet.init();
        mhcnet.init();
        
        nonnet.setName("Body");
        mhcnet.setName("MHC Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        int win = compareClassifiers("Iris Body vs. MHC Body", 10, 30, im, nonnet, mhcnet);
        assertEquals(2, win);
    }
    
    public void dont_testComparisonIris() throws Exception
    {
        // 10-Fold Cross-Validation. 30 Repeats.
        // You can be 91.30829744387539% sure they're different
        // And the winner is 'Fuzzy Body'
        // Average classification error for 'Crisp Body' = 0.14317673378076062. Variance 0.09924417455399921
        // Average classification error for 'Fuzzy Body' = 0.11991051454138704. Variance 0.14703961275306474
        
        MemorySupplier    ms       = new MemorySupplier();
        ClassifierNetwork crispnet = new ClassifierNetwork();
        ClassifierNetwork fuznet   = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Fuzzyfication     fuz;
        Body              crispbod;
        Body              fuzbod;
        
        NetworkNode []crispnod = new NetworkNode[]
                                                 {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                                 };
        NetworkConnection []crispcon = new NetworkConnection[]
                                                             {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                             };
        NetworkNode []fuznod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication", "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.Body",          "Artificial Immune System",         1)
                                               };
        NetworkConnection []fuzcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",           0),
                new NetworkConnection("Body",          0, null,             0)
                                                           };
        
        crispnet.grow(1,1);
        fuznet.grow(1,1);
        crispnet.populate(crispnod, crispcon);
        fuznet.populate(fuznod, fuzcon);
        disc     = (Discretization)crispnet.getTransformation("Discretization");
        fuz      = (Fuzzyfication)fuznet.getTransformation("Fuzzyfication");
        crispbod = (Body)crispnet.getTransformation("Body");
        fuzbod   = (Body)fuznet.getTransformation("Body");
        
        ms.registerConsumer(0, crispnet, 0);
        ms.registerConsumer(0, fuznet,   0);
        ms.registerConsumer(0, im,     0);
        crispnet.registerSupplier(0, ms, 0);
        fuznet.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 8);
        //fuz.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        fuz.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 8);
        
        // Crisp Data Representation
        crispbod.setDataRepresentation(Body.DATA_FUZZY);
        crispbod.setCrisp(true);
        crispbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        crispbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        crispbod.setDetectorParameters(false, 0.0, 300);
        crispbod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        // Fuzzyfied Data Representation
        fuzbod.setDataRepresentation(Body.DATA_FUZZY);
        fuzbod.setCrisp(false);
        fuzbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        fuzbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        fuzbod.setDetectorParameters(false, 0.0, 300);
        fuzbod.getMorphology().setMHC(Morphology.MHC_NONE);
        FMFContinuous.setOverlap(2.0);
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.01);
        
        crispnet.init();
        fuznet.init();
        
        crispnet.setName("Crisp Body");
        fuznet.setName("Fuzzy Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        int win = compareClassifiers("Iris Crisp vs. Fuzzy", 10, 50, im, crispnet, fuznet);
        assertEquals(2, win);
    }
    
    public void dont_testBitIris() throws Exception
    {
        // 49,0  1,0
        //  2,0 97,0
        // Classification Error : 0.020134228187919462
        
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",             "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 6);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 300);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        double []res = repeatCrossValidation("Crisp Iris", 10, 10, bodnet, im);
        System.err.println(res[0]+" "+res[1]);
        assertTrue(res[0] < 0.35);
    }
    
    public void dont_testCrispIris() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",             "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadIris(ms);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 8);
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 300);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        double []res = repeatCrossValidation("Crisp Iris", 10, 10, bodnet, im);
        System.err.println(res[0]+" "+res[1]);
        assertTrue(res[0] < 0.25);
    }
    
    public void dont_testFuzzyIris() throws ShamanException
    {
        // Fuzzy Iris: average classification Error : 0.1174496644295302 variance 0.03183501000169509
        // Make a Body for the Set
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        Fuzzyfication    fuz = new Fuzzyfication();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, fuz, 0);
        fuz.registerSupplier(0, ms, 0);
        fuz.registerConsumer(0, bod, 0);
        fuz.registerConsumer(0, im, 0);
        bod.registerSupplier(0, fuz, 0);
        
        TestSets.loadIris(ms);
        
        //fuz.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        fuz.setParameters(Fuzzyfication.TYPE_EQUAL_INTERVALS, 8);
        FMFContinuous.setOverlap(2.0);
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.01);
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(false);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 2);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 300);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        fuz.init();
        bod.init();
        
        im.create(ms);
        fuz.trainTransformation(im);
        
        double []res = repeatCrossValidation("Fuzzy Iris", 10, 10, bod, im);
        assertTrue(res[0] < 0.15);
    }
    
    
    
    // **********************************************************\
    // *            Animal Classification Test Case             *
    // **********************************************************/
    public void dont_testComparisonMHCZoo() throws ShamanException
    {
        // Load a Test Set.
        MemorySupplier     ms = new MemorySupplier();
        Body             bod1 = new Body();
        CompoundBody     bod2 = new CompoundBody();
        InstanceSetMemory im  = new InstanceSetMemory();
        bod2.grow(3);
        ms.registerConsumer(0, bod1, 0);
        ms.registerConsumer(0, bod2, 0);
        ms.registerConsumer(0, im, 0);
        bod1.registerSupplier(0, ms, 0);
        bod2.registerSupplier(0, ms, 0);
        
        TestSets.loadZoo(ms, true);
        
        // Configure the Bodies
        bod1.setDataRepresentation(Body.DATA_FUZZY);
        bod1.setCrisp(true);
        bod1.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod1.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod1.setDetectorParameters(false, 0.0, 100);
        bod1.getMorphology().setMHC(Morphology.MHC_NONE);
        bod1.init();
        bod1.setName("Body");
        
        bod2.setDataRepresentation(Body.DATA_FUZZY);
        bod2.setCrisp(true);
        bod2.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod2.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod2.setDetectorParameters(false, 0.0, 100);
        bod2.setMHC(Morphology.MHC_RANDOM);
        bod2.init();
        bod2.setName("MHC Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Compare both Learners
        int win = compareClassifiers("Zoo", 10, 30, im, bod1, bod2);
        assertTrue((win==1) || (win==2)); // No clear winner
    }
    
    public void dont_testBitZoo() throws ShamanException
    {
        // 37,0  4,0
        //  8,0 51,0
        // Classification Error : 0.06
        System.out.println("Zoo Animals Classification");
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadZoo(ms, true);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 100);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.init();
        
        im.create(ms);
        
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10.0});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
        assertTrue(valclas.getClassificationError() < 0.07);
    }
    
    public void dont_testCrispZoo() throws ShamanException
    {
        System.out.println("Zoo Animals Classification");
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadZoo(ms, true);
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 100);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bod.init();
        
        im.create(ms);
        
        double []res = repeatCrossValidation("Crisp Zoo", 10, 10, bod, im);
        assertTrue(res[0] < 0.15);
    }
    
    // **********************************************************\
    // *            Hepetitis Diagnosis AIS Test-Case           *
    // **********************************************************/
    public void doNot_testCrispHepatitis() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadHepatitis(ms);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 5);
        
        // Matchlength = 4, 3 det = 75000, 5-Fold x-val, no-continuous atts = 0.20129
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 4);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 75000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        repeatCrossValidation("Hepatitis Crisp", 5, 3, bodnet, im);
        
        // Do a cross-validation
        //       ValidationClassifier valclas;
        //       Validation val = new Validation(im, bodnet);
        //       val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{5});
        //       //val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.9});
        //       val.test();
        //
        //       // Get the Confusion Matrix
        //       valclas = val.getValidationClassifier();
        //       System.out.println("Confusion Matrix of cross-validation :");
        //       double [][]cmraw  = valclas.getConfusionMatrix();
        //       DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        //       System.out.println(cm);
        //       System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    public void doNot_testBitHepatitis() throws ShamanException
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",             "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadHepatitis(ms);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 5);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 4);    
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 10000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bodnet);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    // **********************************************************\
    // *     Wine Classification Application AIS Test-Case      *
    // **********************************************************/
    public void doNot_testFuzzyWine()
    {
        try
        {
            // Make a Body for the Set
            MemorySupplier    ms = new MemorySupplier();
            Body             bod = new Body();
            InstanceSetMemory im = new InstanceSetMemory();
            Fuzzyfication    fuz = new Fuzzyfication();
            
            ms.registerConsumer(0, bod, 0);
            ms.registerConsumer(0, fuz, 0);
            fuz.registerSupplier(0, ms, 0);
            fuz.registerConsumer(0, bod, 0);
            fuz.registerConsumer(0, im, 0);
            bod.registerSupplier(0, fuz, 0);
            
            TestSets.loadWine(ms);
            
            fuz.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 7);
            //fuz.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
            FMFContinuous.setOverlap(0.5);
            
            AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.001);
            bod.setDataRepresentation(Body.DATA_FUZZY);
            bod.setCrisp(false);
            bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 4);
            bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
            bod.setDetectorParameters(false, 0.0, 20000);
            //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
            bod.getMorphology().setMHC(Morphology.MHC_NONE);
            
            fuz.init();
            bod.init();
            
            im.create(ms);
            fuz.trainTransformation(im);
            
            // Do a cross-validation
            ValidationClassifier valclas;
            Validation val = new Validation(im, bod);
            val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
            val.test();
            
            // Get the Confusion Matrix
            valclas = val.getValidationClassifier();
            System.out.println("Confusion Matrix of cross-validation :");
            double [][]cmraw  = valclas.getConfusionMatrix();
            DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
            System.out.println(cm);
            System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public void doNot_testCrispWine() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadWine(ms, true);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 8);
        //disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 7);
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 50000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bodnet);
        //val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.25});
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    public void doNot_testBitWine() throws ShamanException
    {
        System.out.println("Wine Recognition Bit Representation AIS Experiment");
        
        // Make a Body for the Set
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization disc  = new Discretization();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, disc, 0);
        disc.registerSupplier(0, ms, 0);
        disc.registerConsumer(0, bod, 0);
        disc.registerConsumer(0, im, 0);
        bod.registerSupplier(0, disc, 0);
        
        TestSets.loadWine(ms);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 8);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 15);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 10000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        disc.init();
        bod.init();
        
        im.create(ms);
        disc.trainTransformation(im);
        
        im = InstanceSetMemory.estimateAll(im, disc);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    // **********************************************************\
    // *           Credit Card Application  AIS Test-Case       *
    // **********************************************************/
    public void doNot_testCrispCredit() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",                   "Artificial Immune System",          1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        disc = (Discretization)bodnet.getTransformation("Discretization");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadCredit(ms);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 7);
        //disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 7);
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 5);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 50000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bodnet);
        //val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.25});
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    public void doNot_testBitCredit() throws ShamanException
    {
        System.out.println("Credit-Card Application Bit Representation AIS Experiment");
        
        // Make a Body for the Set
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization disc  = new Discretization();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, disc, 0);
        disc.registerSupplier(0, ms, 0);
        disc.registerConsumer(0, bod, 0);
        disc.registerConsumer(0, im, 0);
        bod.registerSupplier(0, disc, 0);
        
        TestSets.loadCredit(ms);
        
        disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 8);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 9);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 150000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        disc.init();
        bod.init();
        
        im.create(ms);
        disc.trainTransformation(im);
        
        im = InstanceSetMemory.estimateAll(im, disc);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{2});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
    }
    
    
    // **********************************************************\
    // *        Cancer Diagnosis Data-set AIS Test-Case         *
    // **********************************************************/
    public void doNot_testComparisonMHCCancer() throws Exception
    {
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork nonnet = new ClassifierNetwork();
        ClassifierNetwork mhcnet = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Fuzzyfication     fuz1, fuz2;
        Body              nonbod;
        CompoundBody      mhcbod;
        
        NetworkNode []nonnod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication", "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.Body",          "Artificial Immune System",         1)
                                               };
        NetworkConnection []noncon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",          0),
                new NetworkConnection("Body",          0, null,            0)
                                                           };
        NetworkNode []mhcnod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication",            "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.CompoundBody", new int[]{3}, "Artificial Immune System",         1)
                                               };
        NetworkConnection []mhccon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",          0),
                new NetworkConnection("Body",          0, null,            0)
                                                           };
        
        nonnet.grow(1,1);
        mhcnet.grow(1,1);
        nonnet.populate(nonnod, noncon);
        mhcnet.populate(mhcnod, mhccon);
        fuz1     = (Fuzzyfication)nonnet.getTransformation("Fuzzyfication");
        fuz2     = (Fuzzyfication)mhcnet.getTransformation("Fuzzyfication");
        nonbod   = (Body)nonnet.getTransformation("Body");
        mhcbod   = (CompoundBody)mhcnet.getTransformation("Body");
        
        ms.registerConsumer(0, nonnet, 0);
        ms.registerConsumer(0, mhcnet, 0);
        ms.registerConsumer(0, im,     0);
        nonnet.registerSupplier(0, ms, 0);
        mhcnet.registerSupplier(0, ms, 0);
        TestSets.loadCancer(ms, true, true);
        
        fuz1.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        fuz2.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.001);
        FMFContinuous.setOverlap(3);
        
        // Non MHC body
        nonbod.setDataRepresentation(Body.DATA_FUZZY);
        nonbod.setCrisp(false);
        nonbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        nonbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        nonbod.setDetectorParameters(false, 0.0, 1000);
        nonbod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        // MHC Compound Body
        mhcbod.setDataRepresentation(Body.DATA_FUZZY);
        mhcbod.setCrisp(false);
        mhcbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        mhcbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        mhcbod.setDetectorParameters(false, 0.0, 1000);
        mhcbod.setMHC(Morphology.MHC_RANDOM);
        
        nonnet.init();
        mhcnet.init();
        
        nonnet.setName("Body");
        mhcnet.setName("MHC Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        compareClassifiers("Cancer Body vs. MHC Body", 10, 30, im, nonnet, mhcnet);
    }
    
    public void dont_testComparisonCancer() throws Exception
    {
        MemorySupplier    ms       = new MemorySupplier();
        ClassifierNetwork crispnet = new ClassifierNetwork();
        ClassifierNetwork fuznet   = new ClassifierNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        Discretization    disc;
        Fuzzyfication     fuz;
        Body              crispbod;
        Body              fuzbod;
        
        NetworkNode []crispnod = new NetworkNode[]
                                                 {
                new NetworkNode("Discretization", "org.shaman.preprocessing.Discretization", "Discretization of Continuous Data", 0),
                new NetworkNode("Body",           "org.shaman.immune.core.Body",             "Artificial Immune System",          1)
                                                 };
        NetworkConnection []crispcon = new NetworkConnection[]
                                                             {
                new NetworkConnection(null,             0, "Discretization", 0),
                new NetworkConnection("Discretization", 0, "Body",           0),
                new NetworkConnection("Body",           0, null,             0)
                                                             };
        NetworkNode []fuznod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication", "Fuzzyfication of Continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.Body",            "Artificial Immune System",         1)
                                               };
        NetworkConnection []fuzcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication",  0),
                new NetworkConnection("Fuzzyfication", 0, "Body",           0),
                new NetworkConnection("Body",          0, null,             0)
                                                           };
        
        crispnet.grow(1,1);
        fuznet.grow(1,1);
        crispnet.populate(crispnod, crispcon);
        fuznet.populate(fuznod, fuzcon);
        disc     = (Discretization)crispnet.getTransformation("Discretization");
        fuz      = (Fuzzyfication)fuznet.getTransformation("Fuzzyfication");
        crispbod = (Body)crispnet.getTransformation("Body");
        fuzbod   = (Body)fuznet.getTransformation("Body");
        
        ms.registerConsumer(0, crispnet, 0);
        ms.registerConsumer(0, fuznet,   0);
        ms.registerConsumer(0, im,     0);
        crispnet.registerSupplier(0, ms, 0);
        fuznet.registerSupplier(0, ms, 0);
        TestSets.loadCancer(ms, true, true);
        
        //disc.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        disc.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        //fuz.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        fuz.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        
        // Crisp Data Representation
        crispbod.setDataRepresentation(Body.DATA_FUZZY);
        crispbod.setCrisp(true);
        crispbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        crispbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        crispbod.setDetectorParameters(false, 0.0, 1000);
        crispbod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        // Fuzzyfied Data Representation
        fuzbod.setDataRepresentation(Body.DATA_FUZZY);
        fuzbod.setCrisp(false);
        fuzbod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        fuzbod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        fuzbod.setDetectorParameters(false, 0.0, 1000);
        fuzbod.getMorphology().setMHC(Morphology.MHC_NONE);
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.001);
        FMFContinuous.setOverlap(3);
        
        crispnet.init();
        fuznet.init();
        
        crispnet.setName("Crisp Body");
        fuznet.setName("Fuzzy Body");
        
        // Create an Instance Set for Training
        im.create(ms);
        
        int win = compareClassifiers("Cancer Crisp vs. Fuzzy", 3, 15, im, crispnet, fuznet);
        assertEquals(2, win);
    }
    
    public void dont_testCrispCancer() throws ShamanException
    {
        // Crisp Cancer: average classification Error : 0.08108882521489971 variance 0.014497548012233259
        
        // Load a Test Set.
        MemorySupplier    ms = new MemorySupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        // Configure and Train the Body
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 1000);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.init();
        
        im.create(ms);
        
        double []res = repeatCrossValidation("Crisp Cancer", 10, 10, bod, im);
        System.err.println(res[0]+" "+res[1]);
        assertTrue(res[0] < 0.15);
    }
    
    public void testFuzzyCancer() throws Exception
    {
        // Fuzzy Cancer: average classification Error : 0.03567335243553008 variance 0.005889628879969765
        MemorySupplier    ms     = new MemorySupplier();
        ClassifierNetwork bodnet = new ClassifierNetwork();
        InstanceSetMemory im    = new InstanceSetMemory();
        Fuzzyfication     fuz;
        Body              bod;
        
        NetworkNode []netnod = new NetworkNode[]
                                               {
                new NetworkNode("Fuzzyfication", "org.shaman.preprocessing.Fuzzyfication", "Fuzzyfication of continuous Data", 0),
                new NetworkNode("Body",          "org.shaman.immune.core.Body",            "Artificial Immune System",         1)
                                               };
        NetworkConnection []netcon = new NetworkConnection[]
                                                           {
                new NetworkConnection(null,            0, "Fuzzyfication", 0),
                new NetworkConnection("Fuzzyfication", 0, "Body",          0),
                new NetworkConnection("Body",           0, null,            0)
                                                           };
        bodnet.grow(1,1);
        bodnet.populate(netnod, netcon);
        fuz  = (Fuzzyfication)bodnet.getTransformation("Fuzzyfication");
        bod  = (Body)bodnet.getTransformation("Body");
        
        ms.registerConsumer(0, bodnet, 0);
        ms.registerConsumer(0, im,     0);
        bodnet.registerSupplier(0, ms, 0);
        TestSets.loadCancer(ms, true, true);
        
        //fuz.setParameters(Discretization.TYPE_HISTOGRAM_EQUALIZATION, 10);
        fuz.setParameters(Discretization.TYPE_EQUAL_INTERVALS, 10);
        AttributePropertyFuzzyContinuous.setFuzzyThreshold(0.001);
        FMFContinuous.setOverlap(3);
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(false);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 3);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 1000);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        
        bodnet.init();
        
        // Create an Instance Set for Training
        im.create(ms);
        
        double []res = repeatCrossValidation("Fuzzy Cancer", 10, 10, bodnet, im);
        assertTrue(res[0] < 0.05);
    }
    
    public void dont_testBitCancer() throws ShamanException
    {
        // 404,0  54,0
        //   2,0 238,0
        // Classification Error : 0.08022922636103152
        System.out.println("Cancer Diagnosis Bit Based AIS Experiment");
        
        // Make a Body for the Cancer Diagnosis Set
        MemorySupplier    ms = new FunctionSupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, bod, 0);
        ms.registerConsumer(0, im, 0);
        bod.registerSupplier(0, ms, 0);
        
        TestSets.loadCancer(ms, false, true);
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, 7);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, 2000);
        //bod.getMorphology().setMHC(Morphology.MHC_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.init();
        
        im.create(ms);
        
        // Do a cross-validation
        ValidationClassifier valclas;
        Validation val = new Validation(im, bod);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{10});
        val.test();
        
        // Get the Confusion Matrix
        valclas = val.getValidationClassifier();
        System.out.println("Confusion Matrix of cross-validation :");
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Classification Error : "+valclas.getClassificationError()+"\n");
        
        assertTrue(valclas.getClassificationError() < 0.10);
    }
    
    // **********************************************************\
    // *            Random Binary Noise Data Source             *
    // **********************************************************/
    public void dont_testBitRandom() throws Exception
    {
        bitRandomCore(10);
        bitRandomCore(3);
        bitRandomCore(1);
    }
    
    public void bitRandomCore(int numchange) throws Exception
    {
        AntigenBit     agen;
        int            i,k,l;
        int            selind, pos;
        int            numbits, matchlen, numself, numdet, match, numtest;
        double         res, selfmatch;
        DoubleMatrix1D []random;
        DoubleMatrix1D datnow;
        
        System.out.println("Random Bitstring Modification Experiment");
        
        // Set the Experiment Parameters
        numbits   = 32;
        numself   = 100;
        matchlen  = 8;
        numdet    = 100;
        numtest   = 200;
        
        // Make a Body with a Random Self Set.
        FunctionSupplier  fs = new FunctionSupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        fs.registerConsumer(0, bod, 0);
        fs.registerConsumer(0, im, 0);
        bod.registerSupplier(0, fs, 0);
        
        fs.setParameters(FunctionSupplier.TYPE_RANDOM_BITSTRING, numself, numbits);
        fs.init();
        
        bod.setDataRepresentation(Body.DATA_BIT);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.setDetectorParameters(false, 0.0, numdet);
        bod.init();
        
        im.create(fs);
        
        agen   = (AntigenBit)bod.createAntigen();
        agen.init(numbits, bod);
        random = im.getInstances();
        
        // Vary the number of detectors
        int      imin, imax, istep;
        double []ind;
        double []dat;
        int      cnt;
        
        imin  = 0;
        imax  = 500;
        istep = 1;
        cnt   = 0;
        ind = new double[(imax-imin)/istep];
        dat = new double[(imax-imin)/istep];
        for (i=imin; i<imax; i+=istep)
        {
            bod.setDetectorParameters(false, 0.0, i);
            bod.trainTransformation(im);
            
            res       = 0;
            selfmatch = 0;
            for (k=0; k<numtest; k++)
            {
                selind = Uniform.staticNextIntFromTo(0, numself-1);
                pos    = Uniform.staticNextIntFromTo(0, numbits-numchange-1);
                datnow = random[selind].copy();
                for (l=pos; l<pos+numchange; l++) datnow.setQuick(l, 1.0-datnow.getQuick(l));
                agen.compile(bod.getMorphology(), datnow);
                
                match = bod.matchDetectors(agen);
                if (match != -1) res++;
                
                datnow = random[selind].copy();
                agen.compile(bod.getMorphology(), datnow);
                match = bod.matchDetectors(agen);
                if (match != -1) selfmatch++;
            }
            res       /= numtest;
            selfmatch /= numtest;
            
            ind[cnt] = i;
            dat[cnt] = res;
            cnt++;
                        
            System.out.println(i+" Result non-self "+res+" self "+selfmatch);
        }
        FileUtil.logToMathematicaTableFile("ais_random_"+numchange+".txt", ind, dat);
    }
    
    public void doNot_testCrispRandom() throws Exception
    {
        AntigenFuzzy   agen;
        int            i,j,k,l;
        int            selind, pos;
        int            numbits, matchlen, numchange, numself, numdet, match, numtest;
        double         res;
        DoubleMatrix1D []random;
        DoubleMatrix1D datnow;
        FileWriter     fout;
        
        System.out.println("Random Bitstring Modification Experiment");
        
        // Set the Experiment Parameters
        numbits   = 32;
        numself   = 100;
        matchlen  = 8;
        numchange = 3;
        numdet    = 100;
        numtest   = 1000;
        
        // Make a Body with a Random Self Set.
        FunctionSupplier  fs = new FunctionSupplier();
        Body             bod = new Body();
        InstanceSetMemory im = new InstanceSetMemory();
        
        fs.registerConsumer(0, bod, 0);
        fs.registerConsumer(0, im, 0);
        bod.registerSupplier(0, fs, 0);
        
        fs.setParameters(FunctionSupplier.TYPE_RANDOM_BITSTRING, numself, numbits);
        fs.init();
        
        bod.setDataRepresentation(Body.DATA_FUZZY);
        bod.setCrisp(true);
        bod.getMorphology().setMHC(Morphology.MHC_NONE);
        bod.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
        bod.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
        bod.setDetectorParameters(false, 0.0, numdet);
        bod.init();
        
        im.create(fs);
        
        agen   = (AntigenFuzzy)bod.createAntigen();
        agen.init(numbits, bod);
        random = im.getInstances();
        
        // Vary the number of detectors
        fout = new FileWriter("./randomexp_vary_detectors.txt");
        numchange = 3;
        for (i=0; i<500; i+=5)
        {
            bod.setDetectorParameters(false, 0.0, i);
            bod.trainTransformation(im);
            
            res = 0;
            for (k=0; k<numtest; k++)
            {
                selind = Uniform.staticNextIntFromTo(0, numself-1);
                pos    = Uniform.staticNextIntFromTo(0, numbits-numchange-1);
                datnow = random[selind].copy();
                for (l=pos; l<pos+numchange; l++) datnow.setQuick(l, 1.0-datnow.getQuick(l));
                agen.setData(datnow.toArray());
                
                match = bod.matchDetectors(agen);
                if (match != -1)
                {
                    res++;
                }
            }
            res /= numtest;
            
            System.out.println(i+" Result "+res);
            fout.write(i+"  "+res+"\n");
            fout.flush();;
        }
        fout.close();
    }
    
    // **********************************************************\
    // *          Artificial Immune System JUnit Tests          *
    // **********************************************************/
    public TestBody(String name)
    {
        super(name);
    }
    
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
