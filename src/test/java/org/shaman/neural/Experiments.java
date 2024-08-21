/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.neural;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.Statistics;
import org.shaman.learning.BatchPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.learning.Validation;
import org.shaman.neural.HopfieldNetwork;
import org.shaman.neural.MLP;
import org.shaman.neural.MLPClassifier;
import org.shaman.neural.NeuralNet;
import org.shaman.neural.Neuron;
import org.shaman.preprocessing.Normalization;
import org.shaman.preprocessing.PCA;
import org.shaman.util.FileUtil;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.random.Uniform;



/**
 * <h2>Experiments in Neural Network</h2>
 * Yaneer Bar-Yam, Irving R. Epstein : "Response of complex networks to stimuli"
 *    PNAS vol. 101, no. 13, March 30, 2004. p4341-4345
 */

// **********************************************************\
// *              Neural Network Experiments                *
// **********************************************************/
public class Experiments
{
    // **********************************************************\
    // *             Handwritten Digit Recognition              *
    // **********************************************************/
    private void digitRecognition() throws Exception
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
        //
        //  Training on 60K instances of the official train set. Tested on the 10K instances of test set also used above.
        //  Network          : 20 (Standardized PCA Input) -> 300 Hidden1 -> 0 Hidden2 -> 10 Output (one-of-n)
        //  957    0   7   0   1   2   7   2   2   2
        //   0 1126   5   2   0   1   0   0   1   0
        //   6    0 989  10   4   2   2   7  11   1
        //   1    1   4 967   1   7   2   9  12   6
        //   1    0   6   0 951   3   5   3   1  12
        //   5    0   1  19   4 841   9   1   6   6
        //   6    4   0   2   2   7 936   0   1   0
        //   1    5  18   8   8   0   0 965   2  21
        //   4    2  14  18   3  18   5   6 896   8
        //   2    4   0  11  16   9   1  18   9 939
        //  Classification Error 0.0433 ( LeCun:  0.047 )

        //  Training on 60K instances of the official train set. Tested on the 10K instances of test set also used above.
        //  Network          : 20 (Standardized PCA Input) -> 300 Hidden1 -> 100 Hidden2 -> 10 Output (one-of-n)
        // 970    0    2   1   0   1   2   1   3   0
        //   0 1125    3   2   0   1   2   1   1   0
        //   7    0 1006   5   2   0   2   6   4   0
        //   0    0    1 981   0   4   0   7  13   4
        //   0    0    3   0 962   1   4   0   1  11
        //   7    0    0  17   2 851   5   1   4   5
        //   4    3    0   0   3   9 938   1   0   0
        //   2    4    7   3   7   0   0 982   3  20
        //   5    0    5  12   6   8   4   4 922   8
        //  1    4    0   8  14   6   0  14   6 956
        // Classification Error 0.0307 ( LeCun: 0.0305 )

        // A MLP Classifier Flow for Handwritten Digit Recognition
        MemorySupplier msTrain = new MemorySupplier();
        MemorySupplier msTest  = new MemorySupplier();
        PCA              pca = new PCA();
        Normalization   norm = new Normalization();
        MLP              mlp = new MLP();
        MLPClassifier   nnet = new MLPClassifier();
        
        msTrain.registerConsumer(0, pca, 0);
        pca.registerSupplier(0,    msTrain, 0);
        pca.registerConsumer(0,  norm, 0);
        norm.registerSupplier(0,  pca, 0);
        norm.registerConsumer(0, nnet, 0);
        nnet.registerSupplier(0, norm, 0);
        
        InstanceSetMemory imTrain = new InstanceSetMemory();
        InstanceSetMemory imTest  = new InstanceSetMemory();
        InstanceSetMemory imprep = new InstanceSetMemory();
        
        // Load the Data-Sets
        TestSets.loadMNIST(msTrain, new String[]{"./src/main/resources/data/mnist/train-images-idx3-ubyte", "./src/main/resources/data/mnist/train-labels-idx1-ubyte"});
        //TestSets.loadMNIST(msTrain, new String[]{"./src/main/resources/data/mnist/t10k-images-idx3-ubyte",  "./src/main/resources/data/mnist/t10k-labels-idx1-ubyte"});
        TestSets.loadMNIST(msTest,  new String[]{"./src/main/resources/data/mnist/t10k-images-idx3-ubyte",  "./src/main/resources/data/mnist/t10k-labels-idx1-ubyte"});

        //TestSets.loadWine(msTrain);
        //TestSets.loadWine(msTest);

        imTrain.create(msTrain);
        imTest.create(msTest);
        System.out.println("Read MNIST handwritten digit train/test data-sets of "+imTrain.getNumberOfInstances()+"/"+imTest.getNumberOfInstances()+" instances.");
        
        long tbeg, tend;

        System.out.println("Calculating Principal Components for training data-set.");
        // Find the 20 most principal components. Apply transformation on the data.
        tbeg = System.currentTimeMillis();
        pca.setType(PCA.TYPE_LINEAR);
        pca.setNumberOfPC(20);
        pca.init();
        pca.trainTransformation(imTrain);
        imprep = InstanceSetMemory.estimateAll(imTrain, pca);
        tend = System.currentTimeMillis();
        System.out.println("Calculated Principal Components in "+(tend-tbeg)+" ms");
        
        // Normalize principal component data-set.
        tbeg = System.currentTimeMillis();
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        norm.trainTransformation(imprep);
        imTrain = InstanceSetMemory.estimateAll(imprep, norm);
        tend = System.currentTimeMillis();
        System.out.println("Standardized PCA-ed data in "+(tend-tbeg)+" ms");

        System.out.println("Applying PCA and standardization on test data-set.");
        // Do the same pre-processing on the test dataset
        imprep = InstanceSetMemory.estimateAll(imTest, pca);
        imTest = InstanceSetMemory.estimateAll(imprep, norm);
        
        // Initialize the MLP Flow
        mlp.setBackPropagationParameters(0.01, true, 0.9, 1000);
        mlp.setBatchParameters(BatchPresenter.BATCH_REORDER, BatchPresenter.GOAL_BALANCE_NONE, 0.1);
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
        mlp.setNetworkParameters(300, 100, MLP.OUTPUT_ONE_OF_N);
        //mlp.setNetworkParameters(300, 0, MLP.OUTPUT_ONE_OF_N);
        nnet.setMLP(mlp);
        nnet.setClassifierOutput(Classifier.OUT_CLASS);
        nnet.init();
        
        // Run a Cross-Validation for the MLP
        System.out.println("Training and testing Cross-Validation Multi-Layer Perceptron");
        double       [][]cmraw;
        DoubleMatrix2D cm;
        Validation val = new Validation(imTrain, nnet);
        //val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.create(Validation.SPLIT_TRAIN_TEST, new double[]{0.0});
        val.setTrainTestSet(imTrain, imTest);
        val.test();
        cmraw = val.getValidationClassifier().getConfusionMatrix();
        cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println("Confusion Matrix"+cm);
        System.out.println("Classification Error "+val.getValidationClassifier().getClassificationError()+"\nError: "+errorFromConfusionMatrix(cmraw));
        System.out.println("Error classifying "+val.getValidationClassifier().getErrorOfClassification());
    }

    private double errorFromConfusionMatrix(double [][]conf)
    {
        int cntAll, cntRight;
        double error;

        // Count the total number of instances and the ones on the diagonal.
        error = 0;
        cntAll = cntRight = 0;
        for(int i=0; i<conf.length; i++)
            for(int j=0; j<conf[i].length; j++)
            {
                if (i==j) cntRight += conf[i][j];
                cntAll += conf[i][j];
            }

        // Error is 1.0 - percentage right
        error = cntAll - cntRight;
        error = error / cntAll;

        return error;
    }
    
    // **********************************************************\
    // *        Response of Complex Networks to Stimuli         *
    // **********************************************************/
    private void networkStimuli() throws ShamanException
    {
        // Make Attractor (Hopfield) Neural Network
        MemorySupplier    ms = new MemorySupplier();
        HopfieldNetwork  net = new HopfieldNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        ms.registerConsumer(0, im, 0);
        ms.registerConsumer(0, net, 0);
        net.registerSupplier(0, ms, 0);
        
        // Create the Graph connecting the network nodes
        Graph      graph;
        Statistics graphstat;
        graph = GraphFactory.makeRandom(1000, 0.01); // p4342 / Numerical Simulations : 1000-nodes with avg. 20 links per node
        //graph = GraphFactory.makeScaleFree(1000);
        graphstat = new Statistics(graph);
        System.out.println("Created graph of "+graph.getNumberOfNodes()+" with average degree of "+graphstat.getAverageDegree());
        
        // Create the datamodel for the 2 random network states
        DataModelDouble dm;
        dm     = makeStateDataModel(graph, 2);
        
        // Make 2 random states and put then in an instance-set to train the network with
        DoubleMatrix1D  state1, state2;
        List            lstate;
        state1 = makeRandomState(dm, 0);
        state2 = makeRandomState(dm, 1);
        lstate = new LinkedList();
        lstate.add(state1);
        lstate.add(state2);
        im.create(lstate, dm);
        System.out.println("Created "+lstate.size()+" random network states");
        
        // Configure the Hopfield network to use the Graph as network connection structure
        ms.setOutputDataModel(0, dm);
        net.setGraph(graph);
        net.init();
        
        // Train the Hopfield network using Hebbian Imprinting rule
        net.setTrainSet(im);
        net.initializeTraining();
        net.train();
        System.out.println("Trained Attractor Network using Hebbian Imprinting");
        
        // Find the order from the highest to lowest connected neurons for directed stimuli
        int []dirorder;
        dirorder = getDirectedStimuliOrder(net);
        
        // Measure the response of the network to stimuli. Make histogram of basin of attraction sizes
        int      i, numneu;
        double []basinsize;
        double []basincount;
        numneu     = graph.getNumberOfNodes();
        basinsize  = new double[numneu];
        basincount = new double[numneu];
        for (i=0; i<basinsize.length; i++)
        {
            basinsize[i]  = i;
            basincount[i] = 0.0;
        }
        
        final int  numtest = 1000;
        for (i=0; i<numtest; i++)
        {
            int            ranpos;
            DoubleMatrix1D state;
            int            basin;
            
            // Pick a random state to start from
            ranpos = Uniform.staticNextIntFromTo(0, lstate.size()-1);
            state  = (DoubleMatrix1D)lstate.get(ranpos);
            
            // Determine the basin of attraction w.r.t. the given type of stimulus. Update histogram.
            basin  = findBasinOfAttraction(state, false, net, dirorder);
            if (basin != -1) basincount[basin]++;
            
            System.out.println(i+"/"+numtest+" basin size "+basin);
        }
        try
        {
            FileUtil.logToMathematicaTableFile("./basin_histogram.txt", basinsize, basincount);
        }
        catch(IOException ex) { throw new LearnerException(ex); }
    }
    
    private int findBasinOfAttraction(DoubleMatrix1D state, boolean randomStimulus, HopfieldNetwork net, int []dirorder) throws LearnerException
    {
        DoubleMatrix1D stimstate;
        int            statesize, stateclass;
        int            i, basin;
        boolean      []stimflip;
        
        // Remember the classification index of the state we start from
        stateclass = (int)state.getQuick(0);
        
        // Flip neuron states until the attractor network fails to converge back to the original state
        basin      = -1;
        statesize  = state.size()-1;
        stimstate  = state.copy(); 
        stimstate  = stimstate.viewPart(1, state.size()-1);
        stimflip   = new boolean[statesize];
        for (i=0; i<stimflip.length; i++) stimflip[i] = false;
        
        for (i=0; (i<statesize) && (basin == -1); i++)
        {
            int  flippos, stimclass;
            
            // Flip another state. Enlarge the stimulus.
            if (randomStimulus)
            {
                // Pick a random neuron
                flippos = Uniform.staticNextIntFromTo(0, statesize-1);
                while(stimflip[flippos])
                {
                    if (flippos == stimflip.length-1) flippos = 0;
                    else                              flippos++;
                }
            }
            else
            {
                // Follow the directed stimulus order, from high to low connected neurons
                flippos = dirorder[i];
            }
            
            // Flip the selected neuron state
            if (stimstate.getQuick(flippos) == -1.0) stimstate.setQuick(flippos,  1.0);
            else                                     stimstate.setQuick(flippos, -1.0);
            stimflip[flippos] = true;
            
            // Let the Hopfield network classify the state + stimulus
            stimclass = net.classify(stimstate);
            
            // If it's different from the class of the original state, the basin of attraction limit is reached
            if (stimclass != stateclass) basin = i;
        }        
        
        return(basin);
    }
    
    private DataModelDouble makeStateDataModel(Graph graph, int numstates) throws DataModelException
    {
        int             i;
        DataModelDouble dm;
        AttributeDouble att;
        int             numnodes;
        double        []state;
        
        numnodes = graph.getNumberOfNodes();
        dm       = new DataModelDouble("State DataModel", numnodes+1);
        
        // The attribute containing the state index goes first
        att      = new AttributeDouble("state");
        state    = new double[numstates];
        for (i=0; i<numstates; i++) state[i] = i;
        att.initAsSymbolCategorical(state);
        att.setIsActive(false);
        att.setValuesAsGoal();
        dm.setAttribute(0, att);
        dm.getLearningProperty().setGoal("state");
        
        // All the other attributes contain one neuron state
        for (i=0; i<numnodes; i++)
        {
            att = new AttributeDouble("neuron"+i);
            att.initAsSymbolCategorical(new double[]{-1,1});
            att.setIsActive(true);
            dm.setAttribute(i+1, att);
        }
        
        return(dm);
    }
    
    private DoubleMatrix1D makeRandomState(DataModelDouble dm, int state) throws DataModelException
    {
        int            i;
        DoubleMatrix1D ranvec;
        double         ran;
        
        // Create vector containing random neuron states.
        ranvec = DoubleFactory1D.dense.make(dm.getAttributeCount());
        ranvec.setQuick(0, state);
        for (i=1; i<ranvec.size(); i++)
        {
            ran = Uniform.staticNextDouble();
            if (ran < 0.5) ran = -1.0;
            else           ran =  1.0;
            ranvec.setQuick(i, ran);
        }
        
        return(ranvec);
    }
    
    private int []getDirectedStimuliOrder(HopfieldNetwork net)
    {
        NeuralNet nnet;
        int       i, numneu;
        Integer   degree;
        Neuron    neuron;
        TreeMap   ordmap;
        List      neulist;
        
        // Put the neurons ordered from high to low degree in a map
        ordmap = new TreeMap();
        nnet   = net.getNeuralNet();
        numneu = nnet.getNumberOfNeurons();
        for (i=0; i<numneu; i++)
        {
            neuron  = nnet.getNeuron(i);
            degree  = new Integer(-neuron.getInputConnections().length);
            neulist = (List)ordmap.get(degree);
            if (neulist == null) { neulist = new LinkedList(); ordmap.put(degree, neulist); }
            neulist.add(new Integer(i));
        }
        
        int    []dirorder;
        int      pos;
        Iterator itdegree, itlist;
        Integer  neuind;
        
        // Convert the map to an integer array of indices of neurons
        pos      = 0;
        dirorder = new int[numneu];
        itdegree = ordmap.values().iterator();
        while(itdegree.hasNext())
        {
            neulist = (List)itdegree.next();
            itlist  = neulist.iterator();
            while(itlist.hasNext())
            {
                neuind          = (Integer)itlist.next();
                dirorder[pos++] = neuind.intValue();
            }
        }
        
        //for (i=0; i<dirorder.length; i++)
        //    System.err.println(dirorder[i]+ " has "+nnet.getNeuron(dirorder[i]).getInputConnections().length);
        
        return(dirorder);
    }
    
    // **********************************************************\
    // *           Execute a Neural Network Experiment          *
    // **********************************************************/
    public static void main(String []args)
    {
        Experiments app;
        
        app = new Experiments();
        try
        {
            // Run one of the experiments
            //app.networkStimuli();
            app.digitRecognition();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}