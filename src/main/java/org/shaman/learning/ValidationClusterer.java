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
package org.shaman.learning;


import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Test and Reporting Algorithms for Clusterer</h2>
 *
 * Statistically founded test procedures and reporting for
 * clustering algorithms.
 *
 * <br>
 * <i>Tom M. Mitchell (1997), Machine Learning, Chapter 5</i><br>
 * <i>Witten I., Frank E. (2000), Data Mining, Chapter 5</i>
 */

// **********************************************************\
// *        Cluterer Testing and Test Result Reporting      *
// **********************************************************/
public class ValidationClusterer
{
    // **********************************************************\
    // *      Statistically Founded Test for Clusterers         *
    // **********************************************************/
    // General Data about the data to Test
    private Validation  val;           // The Validation to which this test belongs
    private Presenter   instances;     // The complete set of instances (if possible)
    private DataModel   dataModel;     // DataModel of the Data
    private int         numTest;             // Number of instances tested
    private int       []split;               // How to Split up the total instances set
    private int         numFolds;            // Number of Folds
    private Classifier  cl;                  // The Clusterer used in the tests
    
    // Cluster Test Buffers
    private int [][]clusTest;               // Result of the clustering test for all folds
    
    // **********************************************************\
    // *             Clusterer Testing / Reporting              *
    // **********************************************************/
    /**
     * Get the relative assignment to the clusters of the given fold.
     * @param fold The fold number (starts at 1)
     * @return The relative cluster assignment of the instance in the given fold.
     */
    public double []getClusterMembership(int fold)
    {
        int    i;
        double count;
        double []clmem;
        int    maxind;
        
        // Find the number of clusters in the fold.
        maxind = -1;
        for (i=0; i<clusTest[fold].length; i++)
        {
            if (maxind < clusTest[fold][i]) maxind = clusTest[fold][i];
        }
        clmem = new double[maxind+1];
        
        // Determine Relative Cluster Membership counts.
        for (i=0; i<clmem.length; i++) clmem[i] = 0;
        count = 0;
        for (i=0; i<clusTest[fold].length; i++)
        {
            if (clusTest[fold][i] != -1) { count++; clmem[clusTest[fold][i]]++; }
        }
        if (count != 0) for (i=0; i<clmem.length; i++) clmem[i] /= count;
        
        return(clmem);
    }
    
    /**
     * Get the relative assignment to the clusters over all the folds
     * @return
     */
    public double []getClusterMembership()
    {
        int      i,j, cnt;
        double []clmem;
        int      maxind;
        
        // Find maximum cluster number over all folds.
        maxind = -1;
        for (i=0; i<this.clusTest.length; i++)
        {
            for (j=0; j<this.clusTest[i].length; j++)
            {
                if (maxind < clusTest[i][j]) maxind = clusTest[i][j];
            }
        }
        clmem = new double[maxind+1];
        
        // Determine membership counts
        cnt = 0;
        for (i=0; i<this.clusTest.length; i++)
        {
            for (j=0; j<this.clusTest[i].length; j++)
            {
                if (this.clusTest[i][j] != -1) { cnt++; clmem[this.clusTest[i][j]]++; }
            }
        }
        if (cnt > 0) for (i=0; i<clmem.length; i++) clmem[i] /= cnt;
        
        return(clmem);
    }
    
    /**
     * Count the number of successfull tests in the given fold
     * @param fold The fold number (starts at 1)
     * @return The number of successful tests in the fold
     */
    public int getNumberOfTests(int fold)
    {
        int i, count;
        
        count = 0;
        for (i=0; i<clusTest[fold].length; i++) if (clusTest[fold][i] != -1) count++;
        
        return(count);
    }
    
    /**
     * Count the total number of successful tests
     * @return The number of tests
     */
    public int getNumberOfTests()
    {
        int i, count;
        
        count = 0;
        for (i=1; i<=numFolds; i++) count += getNumberOfTests(i);
        
        return(count);
    }
    
    // **********************************************************\
    // *                   Clusterer Testing                    *
    // **********************************************************/
    void testClusterer(int fold, Presenter testSet) throws LearnerException
    {
        int            i, numins;
        DoubleMatrix1D ins;
        ObjectMatrix1D oins;
        int            clus;
        
        numins           = testSet.getNumberOfInstances();
        clusTest[fold-1] = new int[numins];
        for (i=0; i<numins; i++)
        {
            if (isPrimitive())
            {
                ins  = testSet.getInstance(i);
                clus = cl.classify(ins);
            }
            else
            {
                oins = testSet.getObjectInstance(i);
                clus = cl.classify(oins);
            }
            clusTest[fold-1][i] = clus;
        }
    }
    
    // **********************************************************\
    // *           Test Initialization / Cleanup                *
    // **********************************************************/
    void initTest(Validation _val, Presenter _instances, int []_split, int _numFolds, Classifier _cl) throws DataModelException
    {
        // Remember the general information about the comming test.
        val       = _val;
        instances = _instances;
        dataModel = instances.getDataModel();
        split     = _split;
        numFolds  = _numFolds;
        cl        = _cl;
        numTest   = 0;
        
        // Clusterer Test Result buffers
        clusTest = new int[numFolds][];
    }
    
    void endTest()
    {
        // Nothing really
    }
    
    public ValidationClusterer()
    {
    }
    
    private boolean isPrimitive()
    {
        return(this.dataModel instanceof DataModelDouble);
    }
}
