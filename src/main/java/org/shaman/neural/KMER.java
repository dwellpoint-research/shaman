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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.GraphException;
import org.shaman.learning.BatchPresenter;
import org.shaman.learning.Estimator;
import org.shaman.learning.EstimatorTransformation;
import org.shaman.learning.InstanceBatch;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Statistic;
import cern.jet.random.Uniform;


/**
 * <h2>Kernel-Based Maximum Entropy Learning Network</h2>
 * Basic KMER algorithm. Can be used as a dimensionality reduction technique.
 * Outputs the (categorical) index of the neuron with highest activation or
 * the (continuous) coordinates of the winning neuron in the low-dimensional
 * lattice of neurons.<p>
 *
 * <i>Van Hulle M. (2001), Faithful Representations and Topographic Maps p139</i>
 */

// **********************************************************\
// *     Kernel-Based Maximum Entropy Learning Network      *
// **********************************************************/
public class KMER extends EstimatorTransformation implements Estimator, Persister
{
    // Lattice Parameters
    private int           n1,n2,n3;      // Lattice dimensions.
    private int           neighborhood;  // Neighborhood function
    private boolean       stratify;      // Stratify the lattice dimension in the output.
    
    // KMER Training Parameters
    private double        eta;           // Learning Rate
    private double        rho;           // Scale Factor
    private int           maxt;          // Number of epochs to train
    
    // Batch Parameters
    private int           batchType;
    private int           balance;
    private double        sampleFraction;
    
    // Network Structure
    private int      numneu;      // The number of neurons
    private Lattice  lattice;     // A low-dimensional (1D-3D) lattice of neurons
    
    // The Neural Network
    private NeuralNet net;
    private int       neuBeg;     // Index of the first neuron of the lattice
    
    // Training Buffers
    private int     epoch;         // Current epoch
    private int     inputSize;     // The number of input attributes
    private double  []inbuf;
    private double  [][]weibuf;
    private double  [][]dwei;
    private double  [][]sigmabuf;
    private double  []dsigma;
    private boolean []actbuf;
    
    // Disentanglement and Monitoring Heuristics
    //private double  ov;         // Overlap Variability Heuristic.
    private double  meanna;     // Mean # of active neurons
    private double  stdevna;    // Standard Deviation of # active neurons
    
    // Work buffers
    private double  []insbuf;
    private double  []outbuf;
    
    // **********************************************************\
    // *    Estimator Implementation : Output = Active Neuron   *
    // **********************************************************/
    public DoubleMatrix1D estimate(DoubleMatrix1D instance, double []conf) throws LearnerException
    {
        int            i;
        DoubleMatrix1D out;
        int            pos;
        double         max;
        double       []latpos;
        
        // Present the KMER network with an instance. Update the single synchronous layer.
        instance.toArray(this.insbuf);
        this.net.setInput(this.insbuf);
        this.net.updateSynchronous();
        this.net.getOutput(this.outbuf);
        
        // Find the neuron with highest activation
        max = Double.NEGATIVE_INFINITY; pos = -1;
        for (i=0; i<this.outbuf.length; i++)
        {
            if (this.outbuf[i] > max) { max = this.outbuf[i]; pos = i; }
        }
        
        // Make the appropriate output.
        out = null;
        if (pos != -1)
        {
            if (this.stratify) latpos = new double[]{pos};
            else
            {
                if      (n2 == 0) latpos = new double[1];
                else if (n3 == 0) latpos = new double[2];
                else              latpos = new double[3];
                this.lattice.indexToLattice(pos, latpos);
            }
            out = DoubleFactory1D.dense.make(latpos);
        }
        
        return(out);
    }
    
    public double estimateError(DoubleMatrix1D instance) throws LearnerException
    {
        int            i;
        DoubleMatrix1D neuwin;
        int            pos;
        double         max, err;
        
        // Present the KMER network with an instance. Update the single synchronous layer.
        instance.toArray(insbuf);
        this.net.setInput(insbuf);
        this.net.updateSynchronous();
        this.net.getOutput(outbuf);
        
        // Find the neuron with highest activation
        max = Double.NEGATIVE_INFINITY;
        pos = -1;
        for (i=0; i<this.outbuf.length; i++)
        {
            if (this.outbuf[i] > max) { max = this.outbuf[i]; pos = i; }
        }
        
        err = 0;
        if (pos != -1)
        {
            // Measure the distance between the winning neuron's centre and the input instance.
            neuwin = DoubleFactory1D.dense.make(this.net.getNeuron(pos+this.inputSize).getWeights());
            err    = Statistic.EUCLID.apply(neuwin, instance);
        }
        
        return(err);
    }
    
    public ObjectMatrix1D estimate(ObjectMatrix1D instance, double []conf) throws LearnerException
    {
        throw new LearnerException("Cannot handle Object based data");
    }
    
    // **********************************************************\
    // *                  Network Construction                  *
    // **********************************************************/
    public void create() throws LearnerException
    {
        NetworkGraph somgraph;
        int          inSize, numneu;
        NeuralNet    net;
        
        try
        {
            net            = new NeuralNet();
            
            // Make the SOM network graph: Input layer fully connected to output layer containing the neurons
            if (this.inputSize == -1) inSize = net.numberOfInputs(this.dataModel);
            else                      inSize = this.inputSize;
            this.inputSize = inSize;
            
            // Determine number of neurons in the 1 to 3 dimensional lattice of neurons
            if (n2 == 0) { numneu = n1;       } // 1D Lattice
            if (n3 == 0) { numneu = n1*n2;    } // 2D Lattice
            else         { numneu = n1*n2*n3; } // 3D Lattice
            this.numneu = numneu;
            
            // Make buffer for the neuron output.
            this.outbuf = new double[numneu];
            
            // Make the Self-Organizing Map network topology graph
            somgraph    = NetworkGraphFactory.makeSOM(inSize, numneu);
            
            // Gaussian Activation function for the SOM neurons
            net.setNeuronType(Neuron.ACTIVATION_KERNEL_GAUSSIAN, new double[1]);
            
            // Convert graph to real network of neurons
            net.create(somgraph);
            this.neuBeg = inSize;
            
            int    i, netneu;
            Neuron neunow;
            double []rad;
            
            // Initialize neuron weights and radius
            rad    = new double[1];
            netneu = net.getNumberOfNeurons();
            for (i=inSize; i<netneu; i++)
            {
                neunow = net.getNeuron(i);
                neunow.setActivation(Neuron.ACTIVATION_KERNEL_GAUSSIAN);
                neunow.initWeights(Neuron.WEIGHT_INIT_RANDOM_RANGE, 0.0, 1.0);
                rad[0] = Uniform.staticNextDoubleFromTo(0, 0.1);
                neunow.setActivationParameters(rad);
            }
            
            // All done.
            this.net = net;
        }
        catch(GraphException ex) { throw new LearnerException(ex); }
    }
    
    public void init() throws ConfigException
    {
        // Initialize DataModel and shared Estimator logic
        super.init();
        
        // Double array buffer for instance data
        this.insbuf = new double[this.actind.length];
    }
    
    public void cleanUp() throws DataFlowException
    {
        this.net = null;
    }
    
    protected DataModel makeOutputDataModel(DataModel dmin) throws DataModelException
    {
        int             i;
        DataModelDouble dm;
        AttributeDouble att;
        int             numneu;
        double        []laleg;
        
        // Stratification of the lattice. Output 1 categorical value (the active neuron's index)
        if (this.stratify)
        {
            if (this.n2 == 0) numneu = n1;
            if (this.n3 == 0) numneu = n1*n2;
            else              numneu = n1*n2*n3;
            laleg = new double[numneu];
            for (i=0; i<laleg.length; i++) laleg[i] = i;
            dm  = new DataModelDouble("Index DataModel", 1);
            att = dm.getAttributeDouble(0);
            att.initAsSymbolCategorical(laleg);
            att.setIsActive(true);
            att.setName("Active Neuron");
        }
        else
        {
            // No stratification. Output the lattice coordinates as continuous values.
            if      (this.n2 == 0) dm = new DataModelDouble("Lattice DataModel", 1);
            else if (this.n3 == 0) dm = new DataModelDouble("Lattice DataModel", 2);
            else                   dm = new DataModelDouble("Lattice DataModel", 3);
            
            att = dm.getAttributeDouble(0);
            att.initAsNumberContinuous(new double[]{0,n1});
            att.setIsActive(true);
            att.setName("dim1");
            if (n2 != 0)
            {
                att = dm.getAttributeDouble(1);
                att.initAsNumberContinuous(new double[]{0,n2});
                att.setIsActive(true);
                att.setName("dim2");
            }
            if (n3 != 0)
            {
                att = dm.getAttributeDouble(1);
                att.initAsNumberContinuous(new double[]{0,n3});
                att.setIsActive(true);
                att.setName("dim3");
            }
        }
        
        return(dm);
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
        int       i;
        int     []actind;
        Attribute attnow;
        
        // Check if the input is primitive
        if (!dm.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
            throw new ConfigException("Primitive input data required.");
        
        // Check attribute properties
        actind   = dm.getActiveIndices();
        for (i=0; i<actind.length; i++)
        {
            attnow = dm.getAttribute(actind[i]);
            if (!attnow.hasProperty(Attribute.PROPERTY_CONTINUOUS))
                throw new ConfigException("Continuous input data expected. Attribute '"+attnow.getName()+"' is not continuous.");
        }
    }
    
    // **********************************************************\
    // *                Parameter Specification                 *
    // **********************************************************/
    /**
     * Set lattice topology parameters.
     * @param _n1 Size of dimension 1.
     * @param _n2 Size of dimension 2. 0 if 1D lattice.
     * @param _n3 Size of dimension 3. 0 if 2D lattice.
     * @param _neighborhood Kind of neighborhood function to use.
     * @param _stratify If <code>true</code> the (categorical) index in the lattice of the winning
     * neuron is used as output. Else, the (continuous) coordinates in the lattice of the winning
     * neuron are the output.
     */
    public void setLatticeParameters(int _n1, int _n2, int _n3, int _neighborhood, boolean _stratify)
    {
        n1 = _n1;
        n2 = _n2;
        n3 = _n3;
        neighborhood = _neighborhood;
        stratify     = _stratify;
    }
    
    /**
     * Set the parameters used to create a BatchPresenter if a non-batch presenter
     * is given as trainset.
     * @param _batchType Type (reread/recycle) of batch presenter to make.
     * @param _balance Kind of goal balancing to do.
     * @param _sampleFraction Amount of sub-sampling to do.
     */
    public void setBatchParameters(int _batchType, int _balance, double _sampleFraction)
    {
        batchType      = _batchType;
        balance        = _balance;
        sampleFraction = _sampleFraction;
    }
    
    /**
     * Set the KMER parameter.
     * @param _eta The learning rate
     * @param _rho The overlap variability
     * @param _maxt The number of time-steps (epochs) to train
     */
    public void setKMERParameters(double _eta, double _rho, int _maxt)
    {
        eta  = _eta;
        rho  = _rho;
        maxt = _maxt;
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    private BatchPresenter makeBatchTrainSet(Presenter im) throws LearnerException
    {
        // Make a batch TrainSet based on the supplied presenter with the specified parameters
        InstanceBatch     ib;
        
        ib = new InstanceBatch();
        ib.create(im, this.batchType, this.balance, this.sampleFraction);
        
        return(ib);
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public boolean isSupervised()
    {
        return(false);
    }
    
    public void setTrainSet(Presenter instances) throws LearnerException
    {
        try
        {
            checkDataModelFit(0, instances.getDataModel());
            if (!(instances instanceof BatchPresenter)) this.trainData = makeBatchTrainSet(instances);
            else                                        this.trainData = (BatchPresenter)instances;
            this.dataModel = this.trainData.getDataModel();
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public void initializeTraining() throws LearnerException
    {
        // Create a network according to the parameters, datamodels, etc...
        create();
        
        // Create all buffers for Back-Propagation training
        initKMER();
    }
    
    public void train() throws LearnerException
    {
        // Train a number of epochs.
        trainKMER(this.maxt);
    }
    
    // **********************************************************\
    // *                          Training                      *
    // **********************************************************/
    /**
     * Train with KMER using the given batchpresenter for specified amount of time-steps.
     * @param _batch The BatchPresenter supplying the training data.
     * @param numEpochs The number of epochs to train
     */
    public void trainKMER(BatchPresenter _batch, int numEpochs) throws LearnerException
    {
        setTrainSet(_batch);
        initializeTraining();
        trainKMER(numEpochs);
    }
    
    private void trainKMER(int maxt) throws LearnerException
    {
        int i;
        
        for (i=0; i<maxt; i++)
        {
            KMEREpoch();
            
            if ( (i%1)==0) { System.out.print("."); System.out.flush();  }
            if ((i%10)==0) { System.out.print(i+"  "+meanna+"  "+stdevna); System.out.println(); }
        }
    }
    
    private void KMEREpoch() throws LearnerException
    {
        int    ic, i, j, numIns;
        double din, dinmin;
        int    minpos;
        int    numact;
        double d3, sign;
        double []na;       // Number of active neurons for all training instances
        
        if (epoch > maxt) epoch = (int)maxt;
        
        // Start new batch
        ((BatchPresenter)this.trainData).nextBatch();
        numIns = this.trainData.getNumberOfInstances();
        na     = new double[numIns];
        
        // Optimized Batch KMER Algorithm. (Van Hulle, Faithful Representations and Topographic Maps p139)
        for (i=0; i<this.numneu; i++)
        {
            for (j=0; j<this.inputSize; j++) this.dwei[i][j] = 0;
            this.dsigma[i] = 0;
        }
        for (ic=0; ic<numIns; ic++)
        {
            // Feed the instance through the KMER network.
            this.trainData.getInstance(ic).toArray(this.inbuf);
            this.net.setInput(this.inbuf);
            
            // Look for the active neurons
            for (i=0; i<this.numneu; i++) this.actbuf[i] = false;
            dinmin = Double.MAX_VALUE;
            numact = 0; 
            minpos = -1;
            for (i=0; i<this.numneu; i++)
            {
                // Calculate euclidian distance between input and neuron
                din = 0;
                for (j=0; j<this.inputSize; j++) 
                    din += (this.weibuf[i][j] - this.inbuf[j]) * (this.weibuf[i][j] - this.inbuf[j]);
                
                // Mark and count active neurons
                if (din < this.sigmabuf[i][0]*this.sigmabuf[i][0])
                {
                    numact++;
                    this.actbuf[i] = true;
                }
                if (din < dinmin) { dinmin = din; minpos = i; } // Remember closest numbers
            }
            na[ic] = numact;
            
            // If no active neurons
            if (numact == 0)
            {
                // Make the closest neuron active. Adapt it's radius.
                numact         = 1;
                this.actbuf[minpos] = true;
                this.dsigma[minpos] += this.rho/this.numneu + 1.0;
            }
            
            // Adjust the neurons
            for (i=0; i<this.numneu; i++)
            {
                if (!this.actbuf[i]) // Non-Active neuron
                {
                    d3 = 1.0/numact * this.lattice.neighborhood(i, minpos, epoch);
                    for (j=0; j<this.inputSize; j++)
                    {
                        if (this.inbuf[j] - this.weibuf[i][j] <= 0) sign = -1;
                        else                                        sign = 1;
                        this.dwei[i][j] += d3*sign;
                    }
                    this.dsigma[i] += rho / numneu;
                }
                else      // Active neurons
                {
                    for (j=0; j<this.inputSize; j++)
                    {
                        if (this.inbuf[j] - this.weibuf[i][j] <= 0) sign = -1;
                        else                                        sign = 1;
                        this.dwei[i][j] += sign/numact;
                    }
                    this.dsigma[i] -= 1;
                }
            }
        }
        // Batch update of neurons
        for (i=0; i<this.numneu; i++)
        {
            for (j=0; j<this.inputSize; j++) this.weibuf[i][j] += this.eta*this.dwei[i][j];
            this.sigmabuf[i][0] += this.eta*this.dsigma[i];
        }
        
        // Overlap variability heuristic
        this.meanna = 0;
        for (i=0; i<numIns; i++) this.meanna += na[i];
        this.meanna /= numIns;
        this.stdevna = 0;
        for (i=0; i<numIns; i++) this.stdevna += (na[i]-meanna)*(na[i]-meanna);
        this.stdevna /= numIns-1;
        this.stdevna = Math.sqrt(stdevna);
        //if (meanna != 0)  ov = stdevna / meanna;
        //else              ov = -1;
        
        this.epoch++;
    }
    
    private void initKMER() throws LearnerException
    {
        int    i;
        Neuron neunow;
        
        // Initialize Neighborhood
        if (this.n2 == 0) { this.lattice = new Lattice(this.n1);                   } // 1D Lattice
        if (this.n3 == 0) { this.lattice = new Lattice(this.n1, this.n2);          } // 2D Lattice
        else              { this.lattice = new Lattice(this.n1, this.n2, this.n3); } // 3D Lattice
        this.lattice.setNeighborhood(this.neighborhood, this.maxt);
        
        // Initialize Training buffers
        this.epoch    = 0;
        this.inbuf    = new double[this.inputSize];
        this.actbuf   = new boolean[this.numneu];
        this.weibuf   = new double[this.numneu][];
        this.dwei     = new double[this.numneu][];
        this.sigmabuf = new double[this.numneu][];
        this.dsigma   = new double[this.numneu];
        for (i=0; i<numneu; i++)
        {
            neunow           = this.net.getNeuron(this.neuBeg+i);
            this.weibuf[i]   = neunow.getWeights();
            this.dwei[i]     = new double[inputSize];
            this.sigmabuf[i] = neunow.getActivationParameter();
        }
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.n1               = oin.readInt();
            this.n2               = oin.readInt();
            this.n3               = oin.readInt();
            this.neighborhood     = oin.readInt();
            this.stratify         = oin.readBoolean();
            this.eta              = oin.readDouble();
            this.rho              = oin.readDouble();
            this.maxt             = oin.readInt();
            this.batchType        = oin.readInt();
            this.balance          = oin.readInt();
            this.sampleFraction   = oin.readDouble();
            this.inputSize        = oin.readInt();
            this.numneu           = oin.readInt();
            create();
            initKMER();
            this.net.loadState(oin);
        }
        catch(LearnerException ex)       { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            super.saveState(oout);
            oout.writeInt(this.n1);
            oout.writeInt(this.n2);
            oout.writeInt(this.n3);
            oout.writeInt(this.neighborhood);
            oout.writeBoolean(this.stratify);
            oout.writeDouble(this.eta);
            oout.writeDouble(this.rho);
            oout.writeInt(this.maxt);
            oout.writeInt(this.batchType);
            oout.writeInt(this.balance);
            oout.writeDouble(this.sampleFraction);
            oout.writeInt(this.inputSize);
            oout.writeInt(this.numneu);
            this.net.saveState(oout);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                    Construction                        *
    // **********************************************************/
    public KMER()
    {
        super();
        name        = "KMER";
        description = "Kernel-Based Maximum Entropy Learning Neural Network.";
        
        this.inputSize = -1;
        this.numneu    = -1;
    }
}
