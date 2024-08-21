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
package org.shaman.svm;


import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Least Squares Support Vector Machine</h2>
 * ESAT LS-SVMlab
 */

// **********************************************************\
// *         Least Squares Support Vector Machine           *
// **********************************************************/
public class LSSVM extends ClassifierTransformation implements Classifier
{
    // ----- Ported from LS-SVMlab -----
    // Training Instances goal vector
    private DoubleMatrix1D Y;
    
    // Kernel Definition
    private int      kertype;
    private double []kerpar;
    
    // Regularization
    private double   _inv_gamma;
    private double   _gamma;
    
    // CGA Optimization Parameters
    private double   _eps;
    private double   _fi_bound;
    private int      _max_itr;
    
    // --- Run-time Data ---
    private double []_svX;           // Input instances
    private double []_svY;           // Input goal
    private int      _dimX;
    private int      _dimY;
    private int      _nb;
    
    private int        _steps;
    private int        _xdelays;
    private int        _ydelays;
    private double [][]_R;
    private double   []_simX;
    private double   []_simY;
    
    private Kernel      kernel;
    private KernelCache kernelCache;
    
    // --- Model ---
    private DoubleMatrix1D alpha;
    private double         b;
    
    // **********************************************************\
    // *                   Classification                       *
    // **********************************************************/
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifySVM(instance, confidence));
    }
    
    public int classifySVM(DoubleMatrix1D Xt, double []confidence) throws LearnerException
    {
        double         Yt;
        DoubleMatrix1D kx;
        DoubleMatrix1D Xrow;
        double         kerval;
        int            nb, i;
        
        nb = this._nb;
        
        // Kx(i) = Kernel(train instance i, test instance) 
        kx = DoubleFactory1D.dense.make(nb);
        for (i=0; i<nb; i++)
        {
            Xrow   = this.trainData.getInstance(i);
            kerval = kernel.apply(Xrow, Xt); 
            kx.setQuick(i, kerval);
        }
        
        // Y = Kx' * (alpha + [1...1]*b)
        DoubleMatrix1D apb = DoubleFactory1D.dense.make(nb);
        for (i=0; i<nb; i++)
        {
            apb.setQuick(i, alpha.getQuick(i)*Y.getQuick(i)+b);
        }
        Yt = kx.zDotProduct(apb);
        
        // Determine output class
        int clout;
        
        if (Yt < 0) clout = 0;
        else        clout = 1;
        
        if (confidence != null)
        {
            confidence[clout]   = Yt;
            confidence[1-clout] = 0;
        }
        
        return(clout);        
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Cannot handle Object based data");
    }
    
    
    // **********************************************************\
    // *                   Training Algorithm                   *
    // **********************************************************/
    public void train() throws LearnerException
    {
        double []startv;
        
        // Prepare Data in right format
        prepareData();
        
        // Initialize SVM
        initializeSVM();
        
        // Conjugate Gradient Ascent
        CGA cga = new CGA();
        startv  = cga.cga(null, null, _R, kernelCache, _max_itr, _eps, _fi_bound, 2*_dimY, _nb);
        
        // Calculate Alpha and B
        int            i;
        int            nb = this._nb;
        DoubleMatrix1D v;
        DoubleMatrix1D nu;
        DoubleMatrix1D ones;
        double         s;
        DoubleMatrix1D alpha;
        double         b;
        
        v  = DoubleFactory1D.dense.make(nb*2);
        v.assign(startv);
        nu = v.viewPart(nb, nb);
        
        s     = 0; b = 0;
        s     = this.Y.zDotProduct(nu);
        b     = nu.zSum() / s;
        alpha = DoubleFactory1D.dense.make(nb);
        for (i=0; i<nb; i++)
        {
            alpha.setQuick(i, v.getQuick(i) - nu.getQuick(i)*b);
        }
        this.alpha = alpha;
        this.b     = b;
        
        //System.out.println("TRAINED *** alpha *** "+alpha+"\n        *** b     *** "+b);
    }
    
    // **********************************************************\
    // *              Kernel and Training Parameters            *
    // **********************************************************/
    public void setKernel(int kertype, double []kerpar)
    {
        this.kertype = kertype;
        this.kerpar  = kerpar;
    }
    
    public void setGamma(double gam)
    {
        this._gamma     = gam;
        this._inv_gamma = 1.0/gam;
    }
    
    // **********************************************************\
    // *             Construction and Initialization            *
    // **********************************************************/
    private void prepareData() throws LearnerException
    {
        if (this.trainData != null)
        {
            // Make the output class vector
            DoubleMatrix1D gvec;
            int            gnow;
            int            i, numins;
            
            numins = this.trainData.getNumberOfInstances();
            gvec   = DoubleFactory1D.dense.make(numins);
            for (i=0; i<numins; i++)
            {
                gnow = this.trainData.getGoalClass(i);
                if (gnow==0) gvec.setQuick(i, -1);
                else         gvec.setQuick(i,  1);
            }
            this.Y = gvec;
            
            // Move instances to 2D Matrix and then to C style array
            int        j;
            double [][]Xar;
            DoubleMatrix1D innow;
            DoubleMatrix2D X;
            numins = this.trainData.getNumberOfInstances();
            X      = DoubleFactory2D.dense.make(numins, this.dataModel.getNumberOfActiveAttributes());
            for (i=0; i<numins; i++)
            {
                innow = this.trainData.getInstance(i);
                X.viewRow(i).assign(innow);
            }
            Xar       = X.toArray();
            this._svX = new double[Xar.length*Xar[0].length];
            for (i=0; i<X.rows(); i++) for(j=0; j<X.columns(); j++) this._svX[(i*X.columns())+j] = X.getQuick(i,j);
            this._svY = Y.toArray();
        }
    }
    
    public void initializeSVM()
    {
        // Determine dimensions
        this._dimX = this.dataModel.getNumberOfActiveAttributes();
        this._dimY = 1;
        this._nb   = Y.size();
        
        // Set CGA parameters
        this._eps      = 1e-15;
        this._fi_bound = 1e-15;
        this._max_itr  = Y.size();
        
        this._steps   = 1;
        this._xdelays = 0;
        this._ydelays = 0;
        
        // Working buffers
        this._simX  = null;
        this._simY  = null;
        this._R     = null;
        
        // Kernel and Kernel Cache
        this.kernel      = new Kernel(this.kertype, this.kerpar);
        this.kernelCache = new KernelCache(this.kernel, this.trainData);
        
        // Initialize the Classifier structures
        initClassifier();
    }
    
    private void initClassifier()
    {
        int    i;
        double []ones;
        double [][]R;
        
        ones = new double[_nb];
        for (i=0; i<ones.length; i++) ones[i] = 1.0;
        
        R = new double[2*_dimY][];
        for (i=0; i<_dimY; i++)
        {
            R[i]       = ones;
            R[i+_dimY] = Y.toArray();
        }
        this._R = R;
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        super.init();
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        checkClassifierDataModelFit(dataModel, false, true, false);
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _trainData)
    {
        this.trainData = _trainData;
        this.dataModel = trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *     Least Squares Support Vector Machine Creation     *
    // *********************************************************/
    public LSSVM()
    {
        super();
        name        = "LS-SVM";
        description = "Least Squares Support Vector Machine";
    }
}
