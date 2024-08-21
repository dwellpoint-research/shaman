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
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.doublealgo.Formatter;
import cern.jet.random.Uniform;


/**
 * <h2>Support Vector Machine</h2>
 * SVM with SMO training. Supports Hard and Soft Margins.
 * For 2-class Classification problems.
 *
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *                Support Vector Machine                  *
// **********************************************************/
public class SMO extends ClassifierTransformation implements Classifier
{
    /** Hard Margin Hyperplane. */
    public static final int MARGIN_HARD       = 0;
    /** Soft Margin 1-Norm Hyperplane. */
    public static final int MARGIN_SOFT_1NORM = 1;
    /** Soft Margin 2-Norm Hyperplane. */
    public static final int MARGIN_SOFT_2NORM = 2;
    
    private Kernel         kernel;        // The Mercer Kernel that does the implicit mapping into feature space
    private DoubleMatrix1D []sv;          // The Support Vectors
    private double         []svclass;     // Classification of the SV's (-1 or 1)
    private double         []alpha;       // The Lagrange Multipliers
    private double         threshold;     // The Threshold
    
    // **********************************************************\
    // *              Sequential Minimal Optimisation           *
    // **********************************************************/
    public void train() throws LearnerException
    {
        initSMO();
        
        do
        {
            this.numChanged = 1;
            trainSMO();
            System.out.println("Numchanged = "+this.numChanged+" all "+this.examineAll);
        }
        while(this.numChanged > 0);
    }
    
    // **********************************************************\
    // *              Sequential Minimal Optimisation           *
    // **********************************************************/
    // Parameters
    private double   C;
    private double   tol;
    private double   eps;
    private int      margin;
    
    // Working Buffers
    DoubleMatrix1D []point;      // The Training Instances
    double         []target;     // Their Target values (1 or -1)
    double         []alph;       // Their alpha's
    private double         []errorcache; // The error cache
    private IntArrayList   nonbound;     // Indices of the non-bound alpha's
    
    public void initSMO() throws LearnerException
    {
        int    i;
        double []goal;
        
        // Get all instances and their goal classes from the instance Presenter.
        point = ((CachingPresenter)this.trainData).getInstances();
        goal  = ((CachingPresenter)this.trainData).getGoals();
        
        // Convert the goal to the target vector
        target = new double[goal.length];
        for (i=0; i<goal.length; i++)
        {
            if (goal[i] == 0.0) target[i] =  1.0;
            else                target[i] = -1.0;
        }
        
        // Initialize the alpha's and the threshold
        alph = new double[point.length];
        for (i=0; i<alph.length; i++) alph[i] = 0.0;
        threshold = 0;
        
        // Initialize the Error Cache
        errorcache = new double[point.length];
        for (i=0; i<point.length; i++) errorcache[i] = -target[i];
        
        // Initialize the non-bound index list
        nonbound = new IntArrayList();
        
        eps        = 1e-8;
        numChanged = 0;
        examineAll = true;
    }
    
    boolean isAtBound(double a)
    {
        if ((Math.abs(a) > eps) && (Math.abs(C-a) > eps)) return(false);
        else                                              return(true);
    }
    
    public void SMOIteration() throws LearnerException
    {
        int i;
        
        IntArrayList nbbuf = new IntArrayList();
        nonbound           = new IntArrayList();
        for (i=0; i<alph.length; i++)
        {
            if (!isAtBound(alph[i])) { nonbound.add(i); nbbuf.add(i); }
        }
        System.out.println("Looping over Non bound "+nonbound);
        
        for (i=0; i<nbbuf.size(); i++)
        {
            if (nonbound.contains(nbbuf.getQuick(i))) numChanged += examineExample(nbbuf.getQuick(i));
        }
        
        if (examineAll) examineAll = false;
        else if (numChanged == 0) examineAll = true;
        
        trainSMO();
    }
    
    private int     numChanged;
    private boolean examineAll;
    
    public void trainSMO() throws LearnerException
    {
        // Based on Pseudocode for the SMO Algorithm (Christianini 2000, An Introducation to Support Vector Machines, p162)
        int     i, pos, numsv;
        
        // Loop until the support vectors found
        if ((numChanged > 0) || (examineAll))
        {
            numChanged = 0;
            if (examineAll)
            {
                //System.out.println("Examining All ");
                for (i=0; i<point.length; i++)
                {
                    numChanged += examineExample(i);
                }
                examineAll = false;
            }
            else
            {
                
            }
        }
        else
        {
            // Make the support vectors
            numsv = 0;
            for (i=0; i<alph.length; i++) if (!isAtBound(alph[i])) numsv++;
            sv      = new DoubleMatrix1D[numsv];
            alpha   = new double[numsv];
            svclass = new double[numsv];
            pos     = 0;
            for (i=0; i<alph.length; i++)
            {
                if ((alph[i] != 0) && (alph[i] != C))
                {
                    sv[pos]      = point[i];
                    alpha[pos]   = alph[i];
                    svclass[pos] = target[i];
                    pos++;
                }
            }
            //System.out.println("Found "+numsv+" support vectors");
            
            Formatter fmt = new Formatter();
            for (i=0; i<sv.length; i++)
            {
                //System.out.println("Alpha "+alpha[i]+" SV : "+fmt.toString(sv[i]));
            }
        }
    }
    
    private int examineExample(int i2)
    {
        int    k,b;
        double y2, alph2, E2, r2;
        double errnow, winE1;
        int    i1;
        
        y2    = target[i2];
        alph2 = alph[i2];
        E2    = getError(i2);
        r2    = E2*y2;
        if (((r2 < -tol) && (alph2 < C)) || ((r2 > tol) && (alph2 > 0)))
        {
            //System.out.println("Nonbound size "+nonbound.size());
            
            if (nonbound.size() > 1)
            {
                // Second choice heuristic. Find point which maximizes |E1-E2|
                i1 = -1;
                if (E2 > 0)
                {
                    winE1 = Double.MAX_VALUE;
                    for (k=0; k<nonbound.size(); k++)
                    {
                        errnow = errorcache[nonbound.getQuick(k)];
                        if (errnow < winE1) { winE1 = errnow; i1 = nonbound.getQuick(k); }
                    }
                }
                else
                {
                    winE1 = Double.MIN_VALUE;
                    for (k=0; k<nonbound.size(); k++)
                    {
                        errnow = errorcache[nonbound.getQuick(k)];
                        if (errnow > winE1) { winE1 = errnow; i1 = nonbound.getQuick(k); }
                    }
                }
                
                // Try to take a step
                if ((i1 != -1) && (takeStep(i1,i2) == 1)) return(1);
            }
            
            // Second Choice Backup Plan 1.
            if (nonbound.size() > 0)
            {
                b = Uniform.staticNextIntFromTo(0, nonbound.size()-1);
                k = b;
                do
                {
                    i1 = nonbound.getQuick(k);
                    if (takeStep(i1,i2) == 1) return(1);
                    k++; if (k == nonbound.size()) k = 0;
                } while (k != b);
            }
            
            // Second Choice Backup Plan 2.
            b  = Uniform.staticNextIntFromTo(0, point.length-1);
            i1 = b;
            do
            {
                if (takeStep(i1, i2) == 1) return(1);
                i1++; if (i1 == point.length) i1 = 0;
            } while (i1 != b);
            
            // Give up.
            ;
        }
        
        return(0);
    }
    
    private int takeStep(int i1, int i2)
    {
        int    i, k, ie, ind;
        double alph1, y1, E1;
        double alph2, y2, E2;
        double a1, a2;
        double s, L, H, Lobj, Hobj, k11, k12, k22, eta;
        double b1, b2;
        double thold;
        double k1e, k2e;
        
        if (i1 == i2) return(0);
        
        // Solve for the 2 selected lagrange multipliers.
        alph1 = alph[i1]; y1 = target[i1]; E1 = getError(i1);
        alph2 = alph[i2]; y2 = target[i2]; E2 = getError(i2);
        s     = y1*y2;
        if (s == -1)
        {
            L = max(0.0,   alph2-alph1);
            H = min(C  , C+alph2-alph1);
        }
        else
        {
            L = max(0.0, alph1+alph2-C);
            H = min(C  , alph1+alph2);
        }
        if (L == H) return(0);
        
        k11 = kernel.apply(point[i1], point[i1]);
        k12 = kernel.apply(point[i1], point[i2]);
        k22 = kernel.apply(point[i2], point[i2]);
        eta = 2*k12-k11-k22;
        if (eta < 0)
        {
            a2 = alph2 - y2*(E1-E2)/eta;
            if      (a2 < L) a2 = L;
            else if (a2 > H) a2 = H;
        }
        else
        {
            double gamma, v1, v2;
            gamma = alph1 + s*L;
            v1    = E1 + target[i1] - y1*alph1*k11 - y2*L*k12;
            v2    = E2 + target[i2] - y1*alph1*k12 - y2*L*k22;
            Lobj  = gamma - s*L + L - 0.5*k11*(gamma-s*L)*(gamma-s*L) - 0.5*k22*L*L - s*k12*(gamma-s*L)*L - y1*(gamma-s*L)*v1 - y2*L*v2;
            gamma = alph1 + s*H;
            v1    = E1 + target[i1] - y1*alph1*k11 - y2*H*k12;
            v2    = E2 + target[i2] - y1*alph1*k12 - y2*H*k22;
            Hobj  = gamma - s*H + H - 0.5*k11*(gamma-s*H)*(gamma-s*H) - 0.5*k22*H*H - s*k12*(gamma-s*H)*H - y1*(gamma-s*H)*v1 - y2*H*v2;
            if      (Lobj > Hobj+tol) a2 = L;
            else if (Lobj < Hobj-tol) a2 = H;
            else                      a2 = alph2;   // No progress possible
        }
        
        if      (Math.abs(a2)   < eps) a2 = 0;   // Numerical Stability enhancement found at
        else if (Math.abs(C-a2) < eps) a2 = C;   // http://www.research.microsoft.com/~jplatt/smo.html
        if (Math.abs(a2-alph2) < tol*(a2+alph2+tol)) return(0);
        a1 = alph1+s*(alph2-a2);
        
        // Update the threshold.
        thold = threshold;
        b1 = E1 + y1*(a1 - alph1)*k11 + y2*(a2-alph2)*k12 + thold;
        b2 = E2 + y1*(a1 - alph1)*k12 + y2*(a2-alph2)*k22 + thold;
        threshold = (b1 + b2)/2.0;
        
        // Update the nonbound list.
        if (isAtBound(a1) && nonbound.contains(i1)) nonbound.remove(nonbound.indexOf(i1));
        if (isAtBound(a2) && nonbound.contains(i2)) nonbound.remove(nonbound.indexOf(i2));
        
        // Update the error cache
        errorcache[i1] = 0;
        errorcache[i2] = 0;
        for (k=0; k<nonbound.size(); k++)
        {
            ie = nonbound.getQuick(k);
            if ((ie != i1) && (ie != i2))
            {
                k1e = kernel.apply(point[i1], point[ie]);
                k2e = kernel.apply(point[i2], point[ie]);
                errorcache[ie] = errorcache[ie] + y1*(a1 - alph1)*k1e + y2*(a2 - alph2)*k2e + thold - threshold;
            }
        }
        
        // Store new lagrange multipliers.
        alph[i1] = a1;
        alph[i2] = a2;
        
        //System.out.println("a["+i1+"] = "+a1);
        //System.out.println("a["+i2+"] = "+a2);
        
        return(1);
    }
    
    private double getError(int i)
    {
        double err;
        
        // First look in the cache.
        if (nonbound.contains(i)) err = errorcache[i];
        else // If not cached...
        {
            // Evaluate the SVM for error.
            int    k;
            double fx;
            
            fx = 0;
            for (k=0; k<alph.length; k++)
            {
                //if ((alph[k] > 0) && (alph[k] < C)) fx += target[k]*alph[k]*kernel.apply(point[k], point[i]);
                if (alph[k] != 0) fx += target[k]*alph[k]*kernel.apply(point[k], point[i]);
            }
            fx -= threshold;
            err = fx - target[i];
        }
        
        return(err);
    }
    
    private double max(double a, double b) { if (a >= b) return(a); else return(b); }
    private double min(double b, double a) { if (a <= b) return(a); else return(b); }
    
    // **********************************************************\
    // *             SMO Parameter specification                *
    // **********************************************************/
    /**
     * Set the parameters of the Sequential Minimal Optimization training.
     * @param _C
     * @param _tol
     */
    public void setSMOParameters(double _C, double _tol)
    {
        C   = _C;
        tol = _tol;
    }
    
    public void setKernel(Kernel _kernel)
    {
        this.kernel = _kernel;
    }
    
    // **********************************************************\
    // *       2-Class Support Vector Classification            *
    // **********************************************************/
    public int classify(DoubleMatrix1D instance)
    {
        int    i;
        double fx;
        
        fx = 0;
        for (i=0; i<sv.length; i++) fx += svclass[i]*alpha[i]*kernel.apply(sv[i], instance);
        fx -= threshold;
        
        //System.out.println("out "+fx+" th "+threshold);
        
        if (fx >= 0) return(1);
        else         return(0);
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence)
    {
        int    i,k;
        double fx;
        
        fx = 0;
        for (k=0; k<alph.length; k++)
        {
            //if ((alph[k] > 0) && (alph[k] < C)) fx += target[k]*alph[k]*kernel.apply(point[k], point[i]);
            if (alph[k] != 0) fx += target[k]*alph[k]*kernel.apply(instance, point[k]);
        }
        fx -= threshold;
        
        /*
         fx = 0;
         for (i=0; i<sv.length; i++) fx += svclass[i]*alpha[i]*kernel.apply(instance, sv[i]);
         fx += threshold;*/
        
        //System.out.println("out "+fx+" th "+threshold);
        
        int clout;
        
        if (fx >= 0) clout = 0;
        else         clout = 1;
        confidence[clout] = fx;
        
        return(clout);
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        throw new LearnerException("Cannot handle Object based data");
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
        this.trainData = (CachingPresenter)_trainData;
        this.dataModel = _trainData.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *                       Constructor                     *
    // *********************************************************/
    public SMO()
    {
        super();
        name        = "SMO";
        description = "Sequential Minimal Optimization";
    }
}
