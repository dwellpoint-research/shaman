package org.shaman.spatial;

import weka.clusterers.AbstractClusterer;
import weka.core.*;
import weka.core.matrix.DoubleVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class BaseKMeans extends AbstractClusterer
{
    private int     numClusters;             // Number of clusters
    private int     maxIterations;           // Maximum number of iterations. Stop when reached or when last iteration did not cause any cluster assignmen changes.
    private boolean kMeansPPInit;            // Perform the K-Means++ initialization step instead of the standard (random) K-Means init.
    private int     seed;
    private DistanceFunction distanceFunction = new EuclideanDistance();
    // --------------
    private Random random;
    private Instance []centroid;             // Cluster centres

    /* K-Means++ paper:
       ---------------
      @inproceedings{Arthur:2007:KAC:1283383.1283494,
      author = {Arthur, David and Vassilvitskii, Sergei},
      title = {k-means++: the advantages of careful seeding},
      booktitle = {Proceedings of the eighteenth annual ACM-SIAM symposium on Discrete algorithms},
      series = {SODA '07},
      year = {2007},
      isbn = {978-0-898716-24-5},
      location = {New Orleans, Louisiana},
      pages = {1027--1035},
      numpages = {9},
      url = {http://dl.acm.org/citation.cfm?id=1283383.1283494},
      acmid = {1283494},
      publisher = {Society for Industrial and Applied Mathematics},
      address = {Philadelphia, PA, USA},
     }
     */

    public BaseKMeans()
    {
        this.seed = (int)System.currentTimeMillis();
        this.random = new Random(this.seed);
    }

    public double[] distributionForInstance(Instance instance) throws Exception
    {
        double []pCluster;
        double dist;
        double distSum;

        // Return distances to all centroids normalized so sum = 1. Smaller means 'easier', closer to centroid.
        distSum = 0;
        pCluster = new double[this.numClusters];
        for(int i=0; i<this.numClusters; i++)
        {
            dist = this.distanceFunction.distance(this.centroid[i], instance);
            pCluster[i] = dist*dist;
            distSum += pCluster[i];
        }
        for(int i=0; i<this.numClusters; i++) pCluster[i] /= distSum;

        // Invert to simulate selection of 'hard' instances (further away from cluster centres)
        //for(int i=0; i<this.numClusters; i++) pCluster[i] = 1.0-pCluster[i];

        return pCluster;
    }

    public int clusterInstance(Instance instance) throws Exception
    {
        int minDistCluster;
        double minDist, dist;

        // Assign the given instance to the cluster whose centroid is closest.
        minDist = Double.MAX_VALUE;
        minDistCluster = -1;
        for(int i=0; i<this.numClusters; i++)
        {
            dist = this.distanceFunction.distance(this.centroid[i], instance);
            if (dist < minDist)
            {
                minDist = dist;
                minDistCluster = i;
            }
        }

        return minDistCluster;
    }

    public double evaluateQuantizationError(Instances testData) throws Exception
    {
        double sumDist, error;
        Instance point;

        // Calculate the average distance between the given points and the centroids of the clusters they're assigned to.
        sumDist = 0;
        for(int i=0; i<testData.numInstances(); i++)
        {
            point = testData.instance(i);
            sumDist += this.distanceFunction.distance(point, this.centroid[clusterInstance(point)]);
        }
        error = sumDist / testData.numInstances();

        return error;
    }

    @Override
    public void buildClusterer(Instances trainData) throws Exception
    {
        DoubleVector []centroid;

        // Pass instance structure in the given DistanceFunction
        this.distanceFunction.setInstances(trainData);

        // Pick the initial cluster centres with the K-Means or K-Means++ method.
        if (this.kMeansPPInit)
            centroid = initKmeansPlusPlus(trainData);
        else centroid = initKMeans(trainData);

        Instance []centroidInstances;

        // Prepare distance to centroid calculations
        centroidInstances = toInstances(centroid);

        int            []member1, member2, memberswap;

        // Make space for the previous and current cluster assignments for all Instances. Initialize initial assignment as unknown (-1).
        member1   = new int[trainData.numInstances()];
        for (int i=0; i<member1.length; i++) member1[i] = -1;
        member2   = new int[trainData.numInstances()];


        int        i, j;
        boolean          stop;
        Instance         point;
        double         []ccount;
        int              vecLen, clusterMin, iterationCount;
        int              changeCount;
        double           distance, distanceMin;
        DoubleVector     pointVector;

        vecLen    = trainData.instance(0).numAttributes();
        ccount    = new double[this.numClusters];
        stop      = false;
        iterationCount   = 0;

        // Iterate until no Instance to cluster assignment changed. Or until the given maximum of iterations is reached.
        while(!stop && (this.maxIterations == 0 || iterationCount < this.maxIterations))
        {
            iterationCount++;

            // Assign all Instances their nearest centroid
            for (i=0; i<trainData.numInstances(); i++)
            {
                point        = trainData.instance(i);
                clusterMin   = -1;
                distanceMin  = Double.POSITIVE_INFINITY;
                for (j=0; j<centroid.length; j++)
                {
                    // Calculate the distance using the given DistanceFunction
                    distance = this.distanceFunction.distance(centroidInstances[j], point);
                    if (distance < distanceMin)
                    {
                        distanceMin  = distance;
                        clusterMin = j;
                    }
                }
                member2[i] = clusterMin;
            }

            // Check if the cluster assignments have stabilized enough to stop
            changeCount = 0;
            for (i=0; i<trainData.numInstances(); i++)
                if (member1[i] != member2[i]) changeCount++;
            stop = (changeCount == 0);

            // Do another centroid update iteration
            if (!stop)
            {
                // Calculate the new centroid of a cluster as the (weighted) average of its instances
                for (i=0; i<this.numClusters; i++)
                {
                    centroid[i] = new DoubleVector(vecLen);
                    ccount[i]   = 0;
                }
                for (i=0; i<trainData.numInstances(); i++)
                {
                    point = trainData.instance(i);
                    pointVector = new DoubleVector(point.toDoubleArray());
                    pointVector.timesEquals(point.weight());
                    ccount[member2[i]] += point.weight();
                    centroid[member2[i]].plusEquals(pointVector);
                }
                for (i=0; i<this.numClusters; i++)
                {
                    if (ccount[i] > 0) centroid[i].timesEquals(1.0/ccount[i]);
                }
                centroidInstances = toInstances(centroid);

                // Swap membership assignment buffers
                memberswap = member2;
                member2    = member1;
                member1    = memberswap;
            }
        }

        // Remember output as centroids of the clusters
        this.centroid = toInstances(centroid);
    }

    public DoubleVector []initKmeansPlusPlus(Instances trainData)
    {
        Instance []centroid;

        centroid    = new Instance[this.numClusters];

        // Pick the first centroid at random
        centroid[0] = trainData.instance(this.random.nextInt(trainData.numInstances()));

        double []distanceProb;
        double dist, minDist, sumDist;
        Instance point;

        // Pick the other ones with higher probability when they're further away from an already picked centroid
        distanceProb = new double[trainData.numInstances()];

        // Find remaining centroids
        for(int i=1; i<centroid.length; i++)
        {
            // Collect the squared distances from each point to the closest already selected centroid
            distanceProb = new double[trainData.numInstances()];
            sumDist      = 0;
            for(int j=0; j<trainData.numInstances(); j++)
            {
                point   = trainData.instance(j);
                minDist = Double.MAX_VALUE;
                for(int k=i-1; k>=0; k--)
                {
                    dist = this.distanceFunction.distance(point, centroid[k]);
                    if (dist < minDist) minDist = dist;
                }
                distanceProb[j] = minDist*minDist;
                sumDist += distanceProb[j];
            }
            // Normalize distances
            for (int j=0; j<distanceProb.length; j++) distanceProb[j] /= sumDist;

            TreeMap<Double, List<Instance>> pdfMap;
            List<Instance> probInstances;
            double         randomDist;

            // Pick the centroid according to the probability density function based on the distances
            pdfMap  = new TreeMap<Double, List<Instance>>();
            sumDist = 0;
            for(int j=0; j<trainData.numInstances(); j++)
            {
                sumDist += distanceProb[j];
                probInstances = pdfMap.get(sumDist);
                if (probInstances == null)
                {
                    probInstances = new ArrayList<Instance>(2);
                    pdfMap.put(sumDist, probInstances);
                }
                probInstances.add(trainData.instance(j));
            }
            randomDist = this.random.nextDouble()*sumDist;
            probInstances = pdfMap.ceilingEntry(randomDist).getValue();
            centroid[i] = probInstances.get(0);
        }


        DoubleVector []centroidVectors;

        // Convert Instances to DoubleVectors for easy calculations in the K-Means iterations
        centroidVectors = new DoubleVector[centroid.length];
        for(int i=0; i<centroidVectors.length; i++) centroidVectors[i] = new DoubleVector(centroid[i].toDoubleArray());

        return centroidVectors;
    }

    private DoubleVector []initKMeans(Instances trainData)
    {
        int          []cpos;
        DoubleVector []centroid;
        boolean    already;
        int        i, j, pos, numin;

        // Select 'k' distinct random training points as initial cluster centers
        cpos   = new int[this.numClusters];
        i      = 0;
        numin  = trainData.numInstances();
        while(i<this.numClusters)
        {
            pos = this.random.nextInt(numin-1);
            already = false;
            for (j=0; j<i; j++) if (cpos[j] == pos) already = true;
            if (!already)
            {
                cpos[i] = pos;
                i++;
            }
        }
        centroid   = new DoubleVector[this.numClusters];
        for (i=0; i<cpos.length; i++) centroid[i] = new DoubleVector(trainData.instance(cpos[i]).toDoubleArray());

        return centroid;
    }

    private Instance []toInstances(DoubleVector []centroid)
    {
        Instance []centroidInstances;

        // Convert to Instances for application of DistanceFunction
        centroidInstances = new Instance[centroid.length];
        for(int i=0; i<centroid.length; i++)
        {
            centroidInstances[i] = new DenseInstance(0, centroid[i].getArray());
        }

        return centroidInstances;
    }

    public Instance []getCentroids()
    {
        return this.centroid;
    }

    @Override
    public int numberOfClusters() throws Exception
    {
        return this.numClusters;
    }

    public void setNumClusters(int n) throws Exception
    {
        this.numClusters = n;
    }

    public void setMaxIterations(int maxIterations)
    {
        this.maxIterations = maxIterations;
    }

    public int getMaxIterations()
    {
        return this.maxIterations;
    }

    public void setDistanceFunction(DistanceFunction d)
    {
        this.distanceFunction = d;
    }

    public void setInitializeUsingKMeansPlusPlusMethod(boolean k)
    {
        this.kMeansPPInit = k;
    }

    public boolean getInitialzeUsingKmeansPlusPlusMethod()
    {
        return this.kMeansPPInit;
    }

    public void setSeed(int value)
    {
        this.seed = value;
        this.random = new Random(this.seed);
    }

    public int getSeed()
    {
        return this.seed;
    }
}
