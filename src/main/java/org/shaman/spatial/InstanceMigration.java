package org.shaman.spatial;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Statistics;
import org.shaman.immune.network.DAISException;

import cern.jet.math.Arithmetic;

/**
 * Weighted Instance migration through graph with proportionate selection.
 * @author johan
 */
public class InstanceMigration
{
    public static final int    NUMBER_OF_EPOCHS  = 50;
    
    public static final int    GRID_X            = 32;
    public static final int    GRID_Y            = 32;
    public static final double GRID_NEIGHBORHOOD = GraphFactory.GRID_NEIGHBORHOOD_L4;
    
    public static final int    NUMBER_OF_INSTANCES_PER_NEIGHBOR = 10;
    public static final int    NUMBER_OF_INSTANCES_PER_NODE     = 1;
    
    public static final double P_SELECT = 1.0;
 
    public static final double WEIGHT_RATIO = 2.0;
    
    
    private Graph      graph;
    private Statistics graphStats;
    private Random     random;
    private int        instanceCount = 0;
    
    private Map<GraphNode, List<WeightedInstance>> nodeInstances;

    
    private void setupGraph() throws Exception
    {
        int i;
        Graph graph;

        // Create a Graph of the specified topology. Link it with the Graph Statistics class
        //graph = GraphFactory.makeRandom(32*32, 0.006);
        //graph = GraphFactory.makeScaleFree(1000, 3.0);
        graph = GraphFactory.makeGrid(GRID_X, GRID_Y, GRID_NEIGHBORHOOD);

        this.graph       = graph;
        this.graphStats = new Statistics(graph);

        List<WeightedInstance> instances;
        String nodeid;
        double weight;
        int    numInstances;
        WeightedInstance instance;

        // For each GraphNode create a set of weighted instances
        instance = null;
        this.nodeInstances = new HashMap<GraphNode, List<WeightedInstance>>();
        for(GraphNode node: this.graph.getNodes())
        {
            nodeid    = "node"+node.getID();
            instances = new LinkedList<WeightedInstance>();
            
            // Size of the set depends on the number of neighbors it has to keep contribution to selections for each Node the same.
            if (NUMBER_OF_INSTANCES_PER_NODE != 0) numInstances = NUMBER_OF_INSTANCES_PER_NODE;
            else                                   numInstances = NUMBER_OF_INSTANCES_PER_NEIGHBOR*(node.getDegree()+1);
            instance     = null;
            for(i=0; i<numInstances; i++)
            {
                weight = 0.0;
                instance = new WeightedInstance(""+(this.instanceCount++), weight);
                instances.add(instance);
            }
            this.nodeInstances.put(node, instances);
        }
        
        // Put a single instance in the Nodes with the given weight
        instance.setWeight(WEIGHT_RATIO);
        
        this.random = new Random();
    }
    
    private void migration() throws Exception
    {
        double []idx;
        double [][]stats;
        String logline;

        System.err.println("Instance migration. Average degree: "+this.graphStats.getAverageDegree());

        idx   = new double[NUMBER_OF_EPOCHS];
        stats = new double[NUMBER_OF_EPOCHS][];
        for(int i=0; i<NUMBER_OF_EPOCHS; i++)
        {
            migrationStep();

            idx[i]   = i;
            stats[i] = calculateStats(i);

            //if (i%10 == 0)
            {
                logline = ""+i;
                for(double stat: stats[i]) logline += "\t"+stat;
                
                System.out.println(logline);
            }
        }
        // For each epoch (y), for all node degrees(x): The average number of instances in a node of degree 'x'
        //logToFile2D("degree_instancecount.txt", idx, stats);

        logToFile2D("instance_stats.txt", idx, stats);
    }

    private double []calculateStats(int epoch) throws Exception
    {
        //return instancesPerDegree(epoch);
        //return instanceStats(epoch);
        return weightStats(epoch);
    }
    
    private double []weightStats(int epoch) throws Exception
    {
        double []stats = new double[2];
        List<WeightedInstance> instances;
        int cntAll, cntRatio;
        double wfrac;
        Set<String> allIds;
        
        // Count fraction of instances with weight 'WEIGHT_RATIO'
        allIds = new HashSet<String>();
        cntAll = 0;
        cntRatio = 0;
        for(GraphNode node: this.graph.getNodes())
        {
            instances = this.nodeInstances.get(node);
            for(WeightedInstance instance: instances)
            {
                cntAll++;
                if (instance.getWeight() == WEIGHT_RATIO) cntRatio++;
                
                allIds.add(instance.getId());
            }
        }
        
        wfrac = ((double)cntRatio)/cntAll;
        stats[0] = allIds.size();
        stats[1] = wfrac;
        
        return(stats);
    }

    private double []instanceStats(int epoch) throws Exception
    {
        double []stats = new double[2];
        Set<String> allKeys;
        double      sumWeights;
        int         allCnt;
        List<WeightedInstance> instances;

        // Calculate average weight of an instance. And total number of distinct instances.
        allKeys    = new HashSet<String>();
        allCnt     = 0;
        sumWeights = 0;
        for(GraphNode node: this.graph.getNodes())
        {
            instances = this.nodeInstances.get(node);
            for(WeightedInstance instance: instances)
            {
                allKeys.add(instance.getId());
                sumWeights += instance.getWeight();
                allCnt++;
            }
        }

        stats[0] = allKeys.size();
        stats[1] = sumWeights/allCnt;

        return(stats);
    }

    private double []instancesPerDegree(int epoch) throws Exception
    {
        int MAXDEGREE = 30;

        int []cntinstance;
        int []cntnode;
        int   degree;

        // Count the number of instances per Node degree
        cntinstance = new int[MAXDEGREE];
        cntnode     = new int[MAXDEGREE];
        for(GraphNode node: this.nodeInstances.keySet())
        {
            degree = node.getDegree();
            if (degree < MAXDEGREE)
            {
                cntinstance[degree] += this.nodeInstances.get(node).size();
                cntnode[degree]++;
            }
        }

        double []avginst = new double[MAXDEGREE];
        for(int i=0; i<cntinstance.length; i++)
        {
            if (cntnode[i] == 0) avginst[i] = 0;
            else
            {
                avginst[i] = ((double)cntinstance[i])/cntnode[i];
            }
        }

        return(avginst);
    }

    private void migrationStep() throws Exception
    {
        Map<GraphNode, List<WeightedInstance>> selectedInstances;
        TreeSet<WeightedInstance> neighborInstances;
        TreeMap<Double, List<WeightedInstance>> sampleInstance;
        List<WeightedInstance> selection, weightInstances;
        Double sumWeight, ranWeight;
        WeightedInstance selectedInstance;

        // For all Nodes: pick instances from the Node and its neighbors with weight proportionate selection
        selectedInstances = new HashMap<GraphNode, List<WeightedInstance>>();
        for(GraphNode node: this.nodeInstances.keySet())
        {
            // Collect the instance of this Node and its neighbors in an ordered (from high to low weight) Set
            neighborInstances = new TreeSet<WeightedInstance>();
            for(GraphNode neighbor: node.getNeighbors())
            {
                addInstances(neighborInstances, this.nodeInstances.get(neighbor));
            }
            addInstances(neighborInstances, this.nodeInstances.get(node));

            // Construct a Map with key the cummulative weights (from high to low) of the Instances
            sampleInstance = new TreeMap<Double, List<WeightedInstance>>();
            sumWeight = 0.0;
            for(WeightedInstance instance: neighborInstances)
            {
                weightInstances = sampleInstance.get(sumWeight);
                if (weightInstances == null)
                {
                    weightInstances = new LinkedList<WeightedInstance>();
                    sampleInstance.put(sumWeight, weightInstances);
                }
                weightInstances.add(instance);
                
                sumWeight += instance.getWeight();
            }

            // Pick instances according the weight-proportionate selection
            ranWeight = 0.0;
            selection = new LinkedList<WeightedInstance>();
            for(WeightedInstance instance: this.nodeInstances.get(node))
            {
                if (this.random.nextDouble() < P_SELECT)
                {
                    ranWeight = this.random.nextDouble()*sumWeight;
                    if (!sampleInstance.containsKey(ranWeight))
                        ranWeight = sampleInstance.lowerKey(ranWeight);
                    if (ranWeight == null) ranWeight = sampleInstance.firstKey();
                    weightInstances = sampleInstance.get(ranWeight);
                    selectedInstance = weightInstances.get(0);
                    selection.add(selectedInstance);
                }
                else selection.add(instance);
            }
            
            // Remember selected instance. Don't switch them over until all Nodes (and potential neighbors) are done
            selectedInstances.put(node, selection);
        }
        
        // Commit selected instances to their Nodes
        for(GraphNode node: this.nodeInstances.keySet())
        {
            this.nodeInstances.get(node).clear();
            this.nodeInstances.get(node).addAll(selectedInstances.get(node));
        }
    }
    
    private void addInstances(TreeSet<WeightedInstance> allInstances, List<WeightedInstance> moreInstances)
    {
        int allSize;
        
        // Add given instance to the ordered Set.
        for(WeightedInstance instance: moreInstances)
        {
            // When the instances is already in the Set, make a clone and and that.
            allSize = allInstances.size();
            allInstances.add(instance);
            if (allSize == allInstances.size())
            {
                allInstances.add(new WeightedInstance(""+(this.instanceCount++), instance.getWeight()));
            }
        }
    }

    private void randomWalk() throws Exception
    {
        System.err.println(graphStats.getAverageDegree());

        getVisitedAfterRandomWalk(10000, 10);
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

    // ------------------------------------------------------

    private void run() throws Exception
    {
        setupGraph();
        migration();
    }

    public static final void main(String []args)
    {
        InstanceMigration app;

        try
        {
            app = new InstanceMigration();
            app.run();
        }
        catch(Exception ex) { ex.printStackTrace(); System.exit(5); }
    }
}

class WeightedInstance implements Comparable<WeightedInstance>
{
    private String id;
    private double weight;

    public WeightedInstance(String id, double weight)
    {
        this.id     = id;
        this.weight = weight;
    }

    public double getWeight() { return this.weight; }
    public String getId()     { return this.id; }
    
    public void setWeight(double weight) { this.weight = weight; }

    // Order from highest to lowest Instance weight. Break ties with the hashCode of the Instances
    public int compareTo(WeightedInstance b)
    {
        WeightedInstance a = this;
        if      (b.weight > a.weight) return(1);
        else if (b.weight < a.weight) return(-1);
        else return new Integer(b.hashCode()).compareTo(new Integer(a.hashCode()));
    }
    
    public String toString() { return this.id+" / "+this.weight; };
}
