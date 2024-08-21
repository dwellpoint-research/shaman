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
package org.shaman.immune.network;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.shaman.graph.Clustering;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Node;
import org.shaman.graph.Statistics;
import org.shaman.immune.core.AntigenBit;
import org.shaman.immune.core.Body;
import org.shaman.immune.core.BodyListener;
import org.shaman.immune.core.Detector;
import org.shaman.immune.core.FunctionSupplier;
import org.shaman.learning.InstanceSetMemory;

import cern.colt.bitvector.BitVector;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.math.Arithmetic;
import cern.jet.random.Uniform;
import cern.jet.stat.Probability;


public class TestNet implements BodyListener
{
    // Experiment Parameters
    int    numbits, numself, numagen, numchange, matchlen, numdet, speed;
    double pflowdet;
    
    DAIS   imnet;                           // The Distributed Artificial Immune System
    Body   bodyStem;                        // The Body Stem for it's Nodes
    
    InstanceSetMemory   im;                 // Random Noise Set
    BodyNode            []nodes;            // The DAIS Nodes containing the Bodies
    Body                []bodies;           // The Bodies of these Nodes
    AntigenBit          [][]agtest;         // The antigens to test detection performance on.
    int                 []matchagen;        // Is the anomaly in the antigen detected?
    
    int                 []detmat;         // Detector Matrix for visualization
    
    private long       expbeg;    // Time when the experiment started.
    private FileWriter tout;      // Log-file
    private int        notcount;  // Notification Counter
    
    // *********************************************************\
    // *            Antibody Migration Experiment              *
    // *********************************************************/
    void testMigration()
    {
        //testMigrationRing();
        
        try
        {
            final int MAXTIME = 1000;
            int numnodes;
            
            numnodes = 100;
            
            // Create the Scale Free DAIS
            setDAISParameters();
            numdet   = 100;
            pflowdet = 1.0;
            createDAIS(numnodes, GraphFactory.TOPOLOGY_SCALE_FREE, new double[]{5.0});
            System.out.println("Created Scale-Free DAIS with "+numnodes+" nodes");
            //createDAIS(numnodes, Graph.TOPOLOGY_RANDOM, new double[]{0.1,10});
            //System.out.println("Created Random DAIS with "+numnodes+" nodes");
            //createDAIS(numnodes, Graph.TOPOLOGY_FULLY_CONNECTED, new double[]{});
            //createDAIS(numnodes, Graph.TOPOLOGY_RING, new double[]{});
            testMigrationComplex(numnodes, MAXTIME);
            predictMigrationComplex(numnodes, MAXTIME);
        }
        catch(DAISException ex) { ex.printStackTrace(); }
    }
    
    private void predictMigrationComplex(int numnodes, int MAXTIME)
    {
        int    i,j;
        double alldet;
        double avgdeg;
        double []pd;
        double []Pk;
        double []ind;
        double []pbt;
        double []pbtacc;
        double []pred;
        double accpk;
        double dna;
        double pk;
        double pam;
        double lam;
        double est;
        double [][]preddeg;
        
        try
        {
            Statistics graphstat = new Statistics(imnet.getGraph());
            // Make Space for the Prediction
            Pk      = graphstat.getDegreeDistribution();
            ind     = new double[MAXTIME];
            pred    = new double[MAXTIME];
            preddeg = new double[MAXTIME][Pk.length];
            
            //alldet = (numnodes-1)*numdet;
            alldet = numnodes*numdet;
            
            // Get graph stats
            avgdeg = graphstat.getAverageDegree();
            pbt    = new double[Pk.length];
            pbtacc = new double[Pk.length];
            
            pam   = 1.0;
            for (i=1; i<MAXTIME; i++)
            {
                for (j=1; j<Pk.length; j++)
                {
                    if (Pk[j] > 0)
                    {
                        dna        = (numdet/avgdeg)*j*pam;
                        // Real Binomial Distribution
                        //lam        = dna / ((numnodes-1)*numdet);
                        lam       = dna / (numnodes*numdet);
                        pbtacc[j] = Probability.binomialComplemented(0, i, lam);
                        
                        // Exponential Exproximation
                        //lam        = pam/(((numnodes-1)*numdet)/dna);
                        //pbtacc[j] += lam*Math.exp(-lam*i);
                    }
                }
                
                // Prediction per Degree and Overal
                pred[i] = 0;
                for (j=0; j<Pk.length; j++)
                {
                    if (Pk[j] != 0)
                    {
                        preddeg[i][j] = numdet + pbtacc[j]*(numdet*(numnodes-1));
                        pbt[j]   = pbtacc[j]*Pk[j];
                        pred[i] += pbt[j];
                    }
                }
                pred[i] =  numdet + pred[i]*(numdet*(numnodes-1));
                
                if (i%5 == 0) System.out.println(i+"  "+pred[i]);
                
                ind[i] = i;
            }
            logToFile("antibody_migration_prediction.txt", ind, pred);
            logToFile2D("migration_prediction_degree.txt", ind, preddeg);
            
            
            /*
             pd   = imnet.getGraph().getNeighborDistanceDistribution();
             System.out.println("Neighbor Distance Distribution");
             for (i=0; i<pd.length; i++)
             {
             System.out.println("p(distance) "+i+" = "+pd[i]);
             }
             
             alldet = (numnodes-1)*numdet;
             
             // FULLY CONNECTED
              // ---------------
               //lam = 0.001; // = Connected p_am = 0.1
                lam = 1.0 / (numnodes-1); // = Connected p_am = 1.0
                
                // Predict the Antibody Migration
                 accpk = 0;
                 pred[0] = 0; ind[0] = 0;
                 for(i=1; i<MAXTIME; i++)
                 {
                 ind[i] = i;
                 
                 pk     = lam*Math.exp(-lam*i);
                 accpk += pk;
                 
                 pred[i] = numdet + (accpk*alldet);
                 
                 if (i%10 == 0) System.out.println("t "+i+"  pred "+pred[i]);
                 }
                 logToFile("antibody_migration_prediction.txt", ind, pred);
                 */
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    private void testMigrationComplex(int numnodes, int MAXTIME)
    {
        int       i,j,k,t,l;
        int       numdetall, dind;
        Integer   dInd;
        double    avgdeg;
        double    []ind;
        double    []dat;
        double    []datpred;
        double    [][]datdegacc;
        double    [][]datdeg;
        double    [][]numdegacc;
        double    [][]numdeg;
        double    []pd;
        double    [][]dssize;
        HashMap   detind;
        BitVector []detvec;
        Detector  []detnow;
        double    []detmatraw;
        int       mind, colgrey;
        double    colinc;
        double    []Pk;
        int       []inddeg;
        double    []degcount;
        
        try
        {
            
            // Make a body index -> degree mapping. Count # of nodes per degree.
            Statistics graphstat = new Statistics(this.graph);
            Pk       = graphstat.getDegreeDistribution();
            System.out.println("Degree Distribution : ");
            for (i=0; i<Pk.length; i++) System.out.print(Pk[i]+" ");
            System.out.println();
            
            inddeg   = new int[numnodes];
            degcount = new double[Pk.length];
            for (i=0; i<inddeg.length; i++)
            {
                inddeg[i] = nodes[i].getDegree();
                degcount[inddeg[i]]++;
            }
            datdeg    = new double[MAXTIME][Pk.length];
            datdegacc = new double[MAXTIME][Pk.length];
            numdeg    = new double[MAXTIME][Pk.length];
            numdegacc = new double[MAXTIME][Pk.length];
            
            // Make a 'detector encountered' matrix for visualization
            detmat    = new int[numnodes*numnodes];
            detmatraw = new double[numnodes*numnodes];
            for (i=0; i<numnodes; i++) { detmat[(i*numnodes)+i] = 0x00FFFFFF; detmatraw[(i*numnodes)+i] = 1.0; }
            
            // Create a map with antibody -> bitvector index
            detind = new HashMap();
            k = 0;
            for (i=0; i<nodes.length; i++)
            {
                detnow = nodes[i].getBody().getDetectorSet().getDetectors();
                for (j=0; j<detnow.length; j++) detind.put(detnow[j], new Integer(k++));
            }
            numdetall = k;
            colinc    = 1.0/numdet;
            
            // Calculate the Neighbor Distance Probability Distribution Function
            pd     = null; //imnet.getGraph().getNeighborDistanceDistribution();
            l      = 99; //pd.length;  // diameter of the graph
            avgdeg = graphstat.getAverageDegree();
            System.out.println("Average Degree "+avgdeg+" Diameter "+l);
            
            // Create the BitVectors that marks which antibodies have been encountered before.
            detvec = new BitVector[numnodes];
            dssize = new double[numnodes][];
            for (i=0; i<nodes.length; i++)
            {
                detvec[i] = new BitVector(numdetall);
                detnow = nodes[i].getBody().getDetectorSet().getDetectors();
                for (j=0; j<detnow.length; j++)
                {
                    detvec[i].set(((Integer)detind.get(detnow[j])).intValue());
                }
                dssize[i] = new double[MAXTIME];
            }
            
            // Circulate the Detectos through the DAIS
            ind     = new double[MAXTIME];
            dat     = new double[MAXTIME];
            datpred = new double[MAXTIME];
            for (i=1; i<MAXTIME; i++)
            {
                // Do a synchronous antibody migration step
                for (j=0; j<nodes.length; j++) nodes[j].sendDetectors();
                for (j=0; j<nodes.length; j++) nodes[j].absorbDetectors();
                
                for (j=0; j<Pk.length; j++) numdegacc[i][j] = 0;
                
                // Calculate how many distinct antibodies were encountered (on average).
                for (j=0; j<nodes.length; j++)
                {
                    // Find the new antibodies (ones that havened been here before)
                    detnow = nodes[j].getBody().getDetectorSet().getDetectors();
                    for (k=0; k<detnow.length; k++)
                    {
                        dInd = (Integer)detind.get(detnow[k]);
                        if (dInd != null)
                        {
                            dind = dInd.intValue();
                            if (!detvec[j].get(dind))
                            {
                                mind = (j*numnodes)+(dind/numdet);
                                detvec[j].set(dind);
                                
                                detmatraw[mind] += colinc;
                                colgrey = ((int)(detmatraw[mind]*255)) & 0xFF;
                                detmat[mind] = colgrey | (colgrey<<8) | (colgrey<<16);
                            }
                        }
                    }
                    dssize[j][i]             = detvec[j].cardinality(); // Overall
                    datdegacc[i][inddeg[j]] += dssize[j][i];            // Per Degree
                    
                    numdegacc[i][inddeg[j]] += nodes[j].getNumberOfDetectors();
                }
                // Take Overall Average
                dat[i] = 0;
                for (j=0; j<nodes.length; j++)  dat[i] += dssize[j][i];
                dat[i] /= numnodes;
                
                // Take Averages per degree
                for (j=0; j<Pk.length; j++)
                {
                    if (degcount[j] > 0)
                    {
                        datdeg[i][j] = datdegacc[i][j] / degcount[j];
                        numdeg[i][j] = numdegacc[i][j] / degcount[j];
                    }
                }
                
                datpred[i] = 0;
                for (j=0; j<Pk.length; j++) datpred[i] += datdeg[i][j]*Pk[j];
                
                
                if (i%10 == 0)
                {
                    System.out.println(i+"  "+dat[i]+"  "+datpred[i]);
                    
                    //System.out.println(i+" "+degcount[5]+" "+datdeg[i][5]);
                    //for (j=0; j<Pk.length; j++) System.out.print(numdeg[i][j]+" ");
                    //System.out.println();
                }
                
                ind[i] = i;
            }
            
            // Save to real results and the prediction to log files.
            logToFile  ("antibody_migration.txt",        ind, dat);
            logToFile2D("migration_degree.txt", ind, datdeg);
            //logToFile2D("antibody_count_degree.txt",     ind, numdeg);
            //logToFile("antibody_migration_scale_free_prediction.txt", ind, datpred);
        }
        catch(GraphException ex) { ex.printStackTrace(); }
        catch(DAISException ex)  { ex.printStackTrace(); }
        catch(IOException ex)    { ex.printStackTrace(); }
    }
    
    private void testMigrationRing()
    {
        int      MAXTIME = 400;
        int      i,j,t;
        int      numnodes;
        HashSet  detset;
        HashSet  begset;
        Detector []detnow;
        double   []ind;
        double   []dat;
        double   []datpred;
        
        try
        {
            numnodes = 10;
            
            // Create the DAIS
            setDAISParameters();
            createDAIS(numnodes, GraphFactory.TOPOLOGY_RING, null);
            
            // Migrate antibodies throughout the DAIS for a while.
            ind     = new double[MAXTIME];
            dat     = new double[MAXTIME];
            datpred = new double[MAXTIME];
            begset  = new HashSet();
            detset  = new HashSet();
            begset.addAll(Arrays.asList(nodes[0].getBody().getDetectorSet().getDetectors()));
            dat[0]     = numdet;
            datpred[0] = numdet;
            for (i=1; i<MAXTIME; i++)
            {
                // Do a synchronous antibody migration step
                for (j=0; j<nodes.length; j++) nodes[j].sendDetectors();
                for (j=0; j<nodes.length; j++) nodes[j].absorbDetectors();
                
                // Find any antibodies that haven't been at node 0 yet
                detnow = bodies[0].getDetectorSet().getDetectors();
                for (j=0; j<detnow.length; j++)
                {
                    if (!detset.contains(detnow[j]) && !begset.contains(detnow[j])) detset.add(detnow[j]);
                }
                
                // Log the total number of detectors encountered
                ind[i] = i;
                dat[i] = numdet + detset.size();
                
                // Predict the number of encountered antibodies
                double pred;
                double pn, div, pncum;
                
                pred = 0; pn = 0;
                for (j=1; j<nodes.length; j++)
                {
                    for (t=j; t<=i; t++)
                    {
                        pn += Arithmetic.binomial(i,t)*Math.pow(pflowdet, t)*Math.pow(1-pflowdet, i-t);
                    }
                }
                pred       = numdet+(pn*numdet);
                datpred[i] = pred;
                System.out.println(i+"  "+dat[i]+" prediction "+datpred[i]);
            }
            logToFile("antibody_migration.txt", ind, dat);
            logToFile("antibody_migration_prediction.txt", ind, datpred);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    // *********************************************************\
    // *          DAIS Network Graph Construction              *
    // *********************************************************/
    Graph graph;
    int   graphSize;
    
    private static final int ATTACK_RANDOM   = 0;
    private static final int ATTACK_DIRECTED = 1;
    
    private void testGraph()
    {
        try
        {
            int numnodes;
            
            //testGraphSimpleOnes();   // Ring and Fully Connected Topologies.
            
            // Scale Free Graph Model.
            //makeGraphScaleFree(1000, 3.0);
            
            Statistics graphstat = new Statistics(this.graph);
            
            setDAISParameters();
            numnodes = 500;
            numdet   = 10;
            pflowdet = 1.0;
            createDAIS(numnodes, GraphFactory.TOPOLOGY_SCALE_FREE, new double[]{3.0});
            //System.out.println("Created Scale-Free DAIS with "+numnodes+" nodes");
            //createDAIS(numnodes, GraphFactory.TOPOLOGY_RANDOM, new double[]{0.006, 50} ); //new double[]{0.012429,50});
            System.out.println("Created Random DAIS with "+numnodes+" nodes");
            //createDAIS(numnodes, GraphFactory.TOPOLOGY_CONNECTED, new double[]{});
            //createDAIS(numnodes, GraphFactory.TOPOLOGY_RING, new double[]{});
            
            System.out.println("Created Graph With Avg. Degree "+graphstat.getAverageDegree());
            
            //this.makeGraphScaleFree(1000, 3);
            getDegreeDistribution();
            //getAttackBehaviour(ATTACK_RANDOM);
            getAttackBehaviour(ATTACK_DIRECTED);
            //getNeighborDistanceDistribution();
            //getVisitedAfterRandomWalk(1000, 100); // Walk Length, Repeats
            
            //getEstimateVisitedAfterRandomWalk(1000);
            
            // Decentralized Scale Free Graph Model Building
            //makeGraphScaleFreeLocal(2000, 3);
        }
        catch(GraphException ex) { ex.printStackTrace(); }
        catch(DAISException ex)  { ex.printStackTrace(); }
        catch(IOException   ex)  { ex.printStackTrace(); }
    }
    
    private void getEstimateVisitedAfterRandomWalk(int walklen) throws DAISException, IOException, GraphException
    {
        int    r,t,i,j;
        double []rvf;
        double []vf;
        double []Pk;
        double N;
        double v,s,st;
        double avgdeg;
        
        rvf = getVisitedAfterRandomWalk(walklen, 100);
        
        vf = new double[walklen];
        
        Statistics graphstat = new Statistics(this.graph);
        Pk     = graphstat.getDegreeDistribution();
        N      = graph.getNumberOfNodes();
        avgdeg = graphstat.getAverageDegree();
        v      = 0;
        for (t=1; t<walklen; t++)
        {
            // Never been there.
            v += getEVARPRep(avgdeg, t, 0) * (1.0-(v/N));
            
            // Been there once or more
            for (r=1; r<=t; r++)
            {
                v += getEVARPRep(avgdeg, t, r) * getEVARSame(Pk, r);
            }
            System.out.println(getEVARPRep(avgdeg, t, 0));
            
            /*
             //v += ((v/N)*(1-getEVARSame(Pk, t))) + Math.pow(1-(v/N), 2);
              st = 0;
              for (i=0; i<=(t/N); i++)
              {
              st += Math.pow((v/N), i+1) * getEVARSame(Pk, i);
              }
              v += st + Math.pow(1-(v/N), 2);
              
              //System.out.println(getEVARPRep(avgdeg, t, 1));
               */
            
            //v += 1-(v/N);
            vf[t] = v/N;
            
            System.out.println("Visited Fraction Estimate "+t+"  "+vf[t]+" measured "+rvf[t]);
        }
    }
    
    private double getEVARPRep(double avgdeg, int t, int r)
    {
        int    i;
        double p;
        double iad;
        
        iad = 1.0/avgdeg;
        if (r > 0)
        {
            p = Arithmetic.binomial(t,r)*Math.pow(iad, r)*Math.pow(1-iad, t-r);
        }
        else
        {
            p = 0;
            for (i=1; i<t; i++) p += Arithmetic.binomial(t,i)*Math.pow(iad, i)*Math.pow(1-iad, t-i);
            p = 1.0 - p;
        }
        
        return(p);
    }
    
    private double getEVARSame(double []Pk, int i)
    {
        int    j,k;
        double s;
        double b;
        double p,q;
        
        b = 0;
        s = 0;
        for (j=1; j<Pk.length; j++)
        {
            if (Pk[j] > 0)
            {
                p = 1.0/j;
                q = 1.0-p;
                b = 0;
                for (k=1; k<=i; k++) b += Arithmetic.binomial(i,k)*Math.pow(p,k)*Math.pow(q, i-k);
                s += b*Pk[j];
            }
        }
        
        return(s);
    }
    
    private double []getVisitedAfterRandomWalk(int walklen, int repeat) throws DAISException, IOException, GraphException
    {
        int    i;
        double []ind;
        double []vf;
        
        System.out.println("Measuring Random Walk Behaviour. Avg. Degree "+new Statistics(this.graph).getAverageDegree());
        
        Statistics graphstat = new Statistics(this.graph);
        vf  = graphstat.getVisitedFractionsAfterRandomWalk(walklen, repeat);
        ind = new double[walklen];
        for (i=0; i<ind.length; i++) ind[i] = i;
        
        logToFile("visited_fraction.txt", ind, vf);
        
        System.out.println("Visited after a random walk of "+walklen+" calculated");
        
        return(vf);
    }
    
    private void getNeighborDistanceDistribution() throws DAISException, IOException, GraphException
    {
        int    i;
        double []d;
        double []pd;
        
        Statistics graphstat = new Statistics(this.graph);
        pd = graphstat.getNeighborDistanceDistribution();
        d  = new double[pd.length];
        for (i=0; i<pd.length; i++) d[i] = i;
        
        logToFile("scalefree_neighbor_distance.txt", d, pd);
    }
    
    private double getAverageNumberOfDetectors()
    {
        int         i;
        GraphNode []nodes;
        BodyNode    nodenow;
        double      avgnumdet;
        
        nodes     = imnet.getGraph().getNodes();
        avgnumdet = 0;
        for (i=0; i<nodes.length; i++)
        {
            nodenow    = (BodyNode)nodes[i];
            avgnumdet += nodenow.getNumberOfDetectors();
        }
        avgnumdet /= nodes.length;
        
        return(avgnumdet);
    }
    
    private void makeGraphScaleFreeLocal(int size, int m) throws DAISException, IOException, GraphException
    {
        int       i, rind;
        GraphNode []node;
        BodyNode  nodenew;
        BodyNode  nodeprev;
        
        graphSize = size;
        graph     = new GraphFactory(GraphFactory.TOPOLOGY_SCALE_FREE, size, new double[]{m}).create();
        
        nodeprev = new BodyNode("node0", "node0");
        for (i=1; i<size; i++)
        {
            // Let a random node add the new node in a Scale-Free manner.
            nodenew = new BodyNode("node"+i, "node"+i);
            nodeprev.addToScaleFree(nodenew, m);
            node    = nodeprev.getLocalNodes();
            
            rind     = Uniform.staticNextIntFromTo(0, node.length-1);
            nodeprev = (BodyNode)node[rind];
            
            if(i%10 == 0) System.out.println("Added "+i);
        }
        
        double []Pk    = nodeprev.getLocalDegreeDistribution();
        double []Pkind = new double[Pk.length];
        for (i=0; i<Pkind.length; i++) Pkind[i] = i;
        logToFile("scalefree_local_degree_distribution.txt", Pkind, Pk);
        
    }
    
    private void makeGraphScaleFree(int size, double m) throws DAISException, GraphException
    {
        int   i;
        Node  nodenew;
        
        // Build a graph acoording to the Scale Free Model
        graphSize = size;
        graph     = new GraphFactory(GraphFactory.TOPOLOGY_SCALE_FREE, size, new double[]{m}).create();
        for (i=0; i<size; i++)
        {
            nodenew = new BodyNode("node"+i, "node"+i);
            graph.add(nodenew);
            
            if(i%100 == 0) System.out.println("Added "+i);
        }
    }
    
    private void getDegreeDistribution() throws IOException, GraphException
    {
        int i;
        
        // Calculate the Degree Distribution
        // ---------------------------------
        double []Pk    = new Statistics(this.graph).getDegreeDistribution();
        double []Pkind = new double[Pk.length];
        for (i=0; i<Pkind.length; i++) Pkind[i] = i;
        logToFile("graph_degree_distribution.txt", Pkind, Pk);
    }
    
    
    private void getAttackBehaviour(int type) throws DAISException, IOException, GraphException
    {
        // Calculate Largest Cluster under attack...
        // -----------------------------------------
        int       size;
        int       i,j;
        GraphNode []node;
        int       rind;
        double    []lcSize;
        double    []apLen;
        double    []avgvis;
        double    []attack;
        int       walklen, repeat;
        
        walklen = 1000; repeat = 100;
        Statistics graphstat = new Statistics(this.graph);
        Clustering graphclus = new Clustering(this.graph);
        
        size   = graphSize;
        lcSize = new double[size];
        apLen  = new double[size];
        avgvis = new double[size];
        attack = new double[size];
        if (type == ATTACK_RANDOM)
        {
            double k, ks;
            double fc;
            
            k  = graphstat.getAverageDegree();
            ks = graphstat.getAverageNumberOfSecondNeighbors();
            fc = 1.0 - 1/(ks/k);
            System.out.println("Critical Fraction = "+fc);
            
            // Random Node Attack
            for (i=0; i<size-5; i++)
            {
                node = graph.getNodes();
                rind = Uniform.staticNextIntFromTo(0, node.length-1);
                graph.remove(node[rind]);
                
                graphclus.findLargestCluster();
                lcSize[i] = ((double)graphclus.getLargestClusterSize())/size;
                apLen[i]  = graphclus.getAveragePathLengthInLargestCluster();
                avgvis[i] = graphstat.getVisitedFractionAfterRandomWalk(walklen, repeat);
                attack[i] = ((double)i)/size;
                
                System.out.println("Removed "+attack[i]+". Largest Cluster Size : "+lcSize[i]+" Average Path Length in Largest Cluster "+apLen[i]+" Visited Fraction "+avgvis[i]);
            }
            for (; i<size; i++) { lcSize[i] = 0; attack[i] = ((double)i)/size; }
            logToFile("attack_random_clustersize.txt", attack, lcSize, 10);
            logToFile("attack_random_path_length.txt", attack, apLen);
            logToFile("attack_random_visited.txt", attack, avgvis);
        }
        else if (type == ATTACK_DIRECTED)
        {
            // Most Connected Node Attack
            int maxdegree, maxind;
            
            for (i=0; i<size-50; i++)
            {
                // Find the node with the highest connectivity. Remove it.
                node = graph.getNodes();
                maxdegree = 0; maxind = -1;
                for (j=0; j<node.length; j++)
                {
                    if (node[j].getDegree() > maxdegree) { maxdegree = node[j].getDegree(); maxind = j; }
                }
                if (maxdegree > 0)
                {
                    graph.remove(node[maxind]);
                    
                    // Find the size of the largest cluster.
                    graphclus.findLargestCluster();
                    lcSize[i] = ((double)graphclus.getLargestClusterSize())/size;
                    apLen[i]  = graphclus.getAveragePathLengthInLargestCluster();
                    avgvis[i] = graphstat.getVisitedFractionAfterRandomWalk(walklen, repeat);
                    attack[i] = ((double)i)/size;
                    
                    System.out.println("Removed "+attack[i]+". Largest Cluster Size : "+lcSize[i]+" Average Path Length in Largest Cluster "+apLen[i]+" Visited Fraction "+avgvis[i]);
                }
                else { lcSize[i] = 0; attack[i] = ((double)i)/size; }
            }
            for (; i<size; i++) { lcSize[i] = 0; attack[i] = ((double)i)/size; }
            logToFile("attack_directed_clustersize.txt", attack, lcSize, 10);
            logToFile("attack_directed_path_length.txt", attack, apLen);
            logToFile("attack_directed_visited.txt", attack, avgvis);
        }
    }
    
    private void testGraphSimpleOnes()
    {
        Graph graph;
        
        try
        {
            // Test Ring Topology
            graph = new GraphFactory(GraphFactory.TOPOLOGY_RING, 10, null).create();
            System.out.println("RING TOPOLOGY\n"+
            "*************");
            graph.printTopology();
            
            
            // Test Fully Connected Topology
            graph = new GraphFactory(GraphFactory.TOPOLOGY_CONNECTED, 10, null).create();
            System.out.println("FULLY CONNECTED TOPOLOGY\n"+
            "************************");
            graph.printTopology();
        }
        catch(GraphException ex) { ex.printStackTrace(); }
    }
    
    // *********************************************************\
    // *    Tests for Distributed Artificial Immune System     *
    // *********************************************************/
    private void setDAISParameters()
    {
        // The Default Body Parameters....
        numbits   = 32;
        matchlen  = 8;
        numself   = 100;
        numdet    = 50;
        numchange = 3;
        numagen   = 500;
        
        speed     = 100;   // # ms between exchange of antibodies
        pflowdet  = 0.1;   // Probability that a detectors gets sent to the other side
    }
    
    private void createDAIS(int numnodes, int topology, double []topopar) throws DAISException
    {
        int    i,j,l;
        
        try
        {
            // Make the Body Stem from which to Nodes derive their Body Structure
            nodes  = new BodyNode[numnodes];
            bodies = new Body[numnodes];
            agtest = new AntigenBit[numnodes][];
            
            FunctionSupplier  fs = new FunctionSupplier();
            Body        bodyStem = new Body();
            im                   = new InstanceSetMemory();
            
            fs.registerConsumer(0, bodyStem, 0);
            fs.registerConsumer(0, im, 0);
            bodyStem.registerSupplier(0, fs, 0);
            
            // Create some Random Bitstrings
            fs.setParameters(FunctionSupplier.TYPE_RANDOM_BITSTRING, numself, numbits);
            fs.init();
            
            // Make the Body Stem for the DAIS
            bodyStem.setPersistenceOptions(false, false);  // Self is part of Detectors not part of Body Structure
            bodyStem.setDataRepresentation(Body.DATA_FUZZY);
            bodyStem.setMatchParameters(Body.MATCH_CONTIGUOUS, matchlen);
            bodyStem.setDetectorAlgorithm(Body.DETECTOR_RANDOM);
            bodyStem.setDetectorParameters(false, 0.0, numdet);
            bodyStem.init();
            
            im.create(fs);
            bodyStem.trainTransformation(im);
            
            // Create a Distributed Body DAIS and set all parameters
            imnet = new DAIS("Test DAIS");
            imnet.setTopology(topology, numnodes, topopar);
            imnet.setType(DAIS.TYPE_BODY);
            imnet.setTriggerTimed(speed);
            imnet.setBodyStem(bodyStem);
            imnet.setBodySelfDataSet(im);
            imnet.setBodyNumberOfDetectors(numdet);
            imnet.setBodyPFlowDetector(pflowdet);
            imnet.init();
            
            // Initialize and connect all Nodes in the Network
            for (i=0; i<nodes.length; i++)
            {
                nodes[i]  = new BodyNode("node"+i);
                agtest[i] = new AntigenBit[numagen];
                imnet.register(nodes[i]);
                bodies[i] = nodes[i].getBody();
                
                if (i%100 == 0) System.out.println("Added Node "+i);
            }
            
            // Remember the network graph.
            graph     = imnet.getGraph();
            graphSize = nodes.length;
        }
        catch(Exception ex) { ex.printStackTrace();  }
    }
    
    private void testDAIS()
    {
        int    i,j,l;
        Object []seldat;
        Object []selob;
        int    selind, pos;
        int    numnodes;
        
        try
        {
            numnodes = 10;
            
            // Make the DAIS
            setDAISParameters();
            createDAIS(numnodes, GraphFactory.TOPOLOGY_RING, null);
            
            // Register this test as a listener for body 0
            //nodes[0].getBody().addListener(this);
            
            // Create antigens starting from the self set
            DoubleMatrix1D datnow;
            DoubleMatrix1D []random = im.getInstances();
            matchagen               = new int[numagen];
            for (i=0; i<numagen; i++)
            {
                for (j=0; j<numnodes; j++)
                {
                    agtest[j][i] = (AntigenBit)bodyStem.createAntigen();
                    agtest[j][i].init(numbits, bodyStem);
                }
                
                selind = Uniform.staticNextIntFromTo(0, numself-1);
                pos    = Uniform.staticNextIntFromTo(0, numbits-numchange-1);
                datnow = random[selind].copy();
                for (l=pos; l<pos+numchange; l++) datnow.setQuick(l, 1.0-datnow.getQuick(l));
                
                for (j=0; j<numnodes; j++)
                {
                    agtest[j][i].setBody(bodies[j]);
                    agtest[j][i].compile(bodies[j].getMorphology(), datnow);
                }
                
                matchagen[i]  = 0;
            }
            
            // Open the Log-file for the Antibody Flow Experiment
            tout   = new FileWriter("./dais_detector_flow.txt");
            
            // Record the begin time
            expbeg = System.currentTimeMillis();
            
            // Start the Distributed Body Activatity
            //for (i=0; i<nodes.length; i++) nodes[i].start();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public void notify(String not, Object ob)
    {
        int    i,j,k;
        int    match;
        Body   bod;
        double perf;
        long   tnow;
        
        // Get a notification from the first body when it received new antibodies
        try
        {
            tnow = System.currentTimeMillis();
            if ((tnow-expbeg) < 10000)
            {
                // Next update
                notcount++;
                
                // Try to detect the anomalies in the antigens
                bod = nodes[0].getBody();
                for (i=0; i<numagen; i++)
                {
                    //match = bod.matchDetectors(agtest[0][i]);
                    match = matchAntigen(i, bod.getDetectorSet().getDetectors());
                    if (match != -1) matchagen[i] = 1;
                }
                
                // Count the total success rate since beginning
                j = 0; for (i=0; i<matchagen.length; i++) if (matchagen[i] == 1) j++;
                perf = ((double)j)/matchagen.length;
                
                String logst = notcount+"\t"+(tnow-expbeg)+"\t"+perf+"\n";
                System.out.print(logst);
                tout.write(logst);
                tout.flush();
            }
            else
            {
                //for (i=0; i<nodes.length; i++) nodes[i].stopLive();
            }
        }
        catch(IOException ex) { ex.printStackTrace(); }
        //catch(DAISException ex) { ex.printStackTrace(); }
    }
    
    public void understood(String und) {}
    
    private int matchAntigen(int agind, Detector []det)
    {
        int     i,j;
        double  mnow;
        boolean match;
        int     mind;
        
        match = false; mind = -1;
        for (i=0; (i<det.length) && (!match); i++)
        {
            if (det[i] != null)
            {
                // Find out from which body this detector is originally from.
                for (j=0; j<bodies.length; j++)
                {
                    // Take the antigen with the right fieldorder (if MHC is enabled) to match with
                    if (bodies[j] == det[i].getBody())
                    {
                        mnow = agtest[j][agind].match(det[i], matchlen);
                        if (mnow >= matchlen) { match = true; mind = i; }
                    }
                }
            }
        }
        
        return(mind);
    }
    
    // *********************************************************\
    // *          File Logging of Experimental Results         *
    // *********************************************************/
    private void logToFile(String filename, double []ind, double []dat) throws IOException
    {
        logToFile(filename, ind, dat, 1);
    }
    
    private void logToFile(String filename, double []ind, double []dat, int stride) throws IOException
    {
        // Log to two-column space-separated file
        int i;
        FileWriter   pkout;
        StringBuffer stout = new StringBuffer();
        
        for (i=0; i<ind.length; i+=stride) stout.append(ind[i]+" "+dat[i]+"\n");
        pkout = new FileWriter(filename);
        pkout.write(stout.toString());
        pkout.flush();
        pkout.close();
    }
    
    private void logToFile2D(String filename, double []ind, double [][]dat) throws IOException
    {
        int        i,j;
        FileWriter lout;
        StringBuffer stout = new StringBuffer();
        
        for (i=0; i<ind.length; i++)
        {
            stout.append(ind[i]);
            for (j=0; j<dat[i].length; j++) stout.append(" "+dat[i][j]);
            stout.append("\n");
        }
        lout = new FileWriter(filename);
        lout.write(stout.toString());
        lout.flush();
        lout.close();
    }
    
    // *********************************************************\
    // *    Tests for Distributed Artificial Immune System     *
    // *********************************************************/
    public void test()
    {
        testGraph();
        //testDAIS();
        //testNet();
        //testMigration();
    }
    
    
    public static void main(String argv[])
    {
        TestNet instance = new TestNet();
        instance.test();
    }
}
