package org.shaman.spatial;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.GraphNode;
import org.shaman.graph.Statistics;
import org.shaman.immune.network.DAISException;

import cern.jet.math.Arithmetic;
import cern.jet.random.Uniform;

public class InstanceMigrationOld
{
    private Graph graph;
    private Statistics graphStats;
    private Map<GraphNode, MigratingInstances> instances;
    
    private void migration() throws Exception
    {
        final int EPOCHS = 1000;
        
        double []idx;
        double [][]stats;
        
        System.err.println("Instance migration. Average degree: "+this.graphStats.getAverageDegree());
        
        idx   = new double[EPOCHS];
        stats = new double[EPOCHS][];
        for(int i=0; i<EPOCHS; i++)
        {
            migrationStep();
            
            idx[i]   = i;
            stats[i] = calculateStats(i);
            
            if (i%50 == 0) System.out.println("Finished epoch "+i);
        }
        // For each epoch (y), for all node degrees(x): The average number of instances in a node of degree 'x'
        //logToFile2D("degree_instancecount.txt", idx, stats);
        
        logToFile2D("instance_stats.txt", idx, stats);
    }
    
    private double []calculateStats(int epoch) throws Exception
    {
        //return instancesPerDegree(epoch);
        return instanceStats(epoch);
    }
    
    private double []instanceStats(int epoch) throws Exception
    {
        double []stats = new double[2];
        Set<String> allKeys;
        double      sumWeights;
        int         allCnt;
        MigratingInstances instances;
        Map<String, Double> nodeInstances;
        
        // Calculate average weight of an instance. And total number of distinct instances.
        allKeys    = new HashSet<String>();
        allCnt     = 0;
        sumWeights = 0;
        for(GraphNode node: this.graph.getNodes())
        {
            instances = this.instances.get(node);
            nodeInstances = instances.getInstances();
            for(String inskey: nodeInstances.keySet())
            {
                allKeys.add(inskey);
                sumWeights += nodeInstances.get(inskey);
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
        for(GraphNode node: this.instances.keySet())
        {
            degree = node.getDegree();
            if (degree < MAXDEGREE)
            {
                cntinstance[degree] += this.instances.get(node).getInstances().size();
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
        Map<GraphNode, Set<String>> migratingInstances;
        Map<GraphNode, Map<String, Double>> arrivingInstances;
     
        // For all Nodes, select a fraction P_MIG of the instances for migration
        arrivingInstances  = new HashMap<GraphNode, Map<String, Double>>();
        migratingInstances = new HashMap<GraphNode, Set<String>>();
        for(GraphNode node: this.instances.keySet())
        {
            migratingInstances.put(node, this.instances.get(node).selectForMigration());
            arrivingInstances.put(node, new HashMap<String, Double>());
        }
        
        // For all Nodes, collect the instances arriving from neighboring nodes
        GraphNode []neighbors;
        int         ranNeighbor;
        Set<String> leavingSet;
        MigratingInstances instanceSet;
        Map<String, Double> arrivingNeighbor;
        
        for(GraphNode node: this.instances.keySet())
        {
            neighbors   = node.getNeighbors();
            instanceSet = this.instances.get(node);
            
            // For all Instance leaving this Node, select a Random neighbor to migrate to.
            leavingSet  = migratingInstances.get(node);
            for(String instanceKey: leavingSet)
            {
                ranNeighbor = Uniform.staticNextIntFromTo(0, neighbors.length-1);
                arrivingNeighbor = arrivingInstances.get(neighbors[ranNeighbor]);
                arrivingNeighbor.put(instanceKey, instanceSet.getInstanceWeight(instanceKey));
            }
        }
        
        Map<String, Double> arrivingSet;
        
        // Perform migration. Remove the 'leaving' and add the 'arriving' instances
        for(GraphNode node: this.instances.keySet())
        {
            leavingSet = migratingInstances.get(node);
            arrivingSet = arrivingInstances.get(node);
            instanceSet = this.instances.get(node);
            instanceSet.migrate(leavingSet, arrivingSet);
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
    
    private void setupGraph() throws Exception
    {
        final int NUMBER_OF_INSTANCES = 100;
        Graph graph;
        
        // Create a Graph of the specified topology. Link it with the Graph Statistics class
        graph = GraphFactory.makeRandom(1000, 0.006);
        //graph = GraphFactory.makeScaleFree(1000, 3.0);
        //graph = GraphFactory.makeGrid(30,30);
        
        this.graph       = graph;
        this.graphStats = new Statistics(graph);
        
        MigratingInstances instances;
        
        // For each GraphNode create a set of (weighted) instances
        this.instances = new HashMap<GraphNode, MigratingInstances>();
        for(GraphNode node: this.graph.getNodes())
        {
            instances = new MigratingInstances("node"+node.getID(), NUMBER_OF_INSTANCES);
            this.instances.put(node, instances);
        }
    }
    
    private void run() throws Exception
    {
        setupGraph();
        migration();
    }
    
    public static final void main(String []args)
    {
        InstanceMigrationOld app;
        
        try
        {
            app = new InstanceMigrationOld();
            app.run();
        }
        catch(Exception ex) { ex.printStackTrace(); System.exit(5); }
    }
}

class MigratingInstances
{
    private static double P_MIG = 0.1;
    
    private String nodeId;
    private Map<String, Double> instanceWeights;
    
    public MigratingInstances(String nodeId, int numInstances)
    {
        String key;
        double weight;
        
        // Create the Map of weighted instances. Create unique identifier over all Nodes in graph by including the node name.
        this.instanceWeights = new LinkedHashMap<String, Double>();
        for(int i=0; i<numInstances; i++)
        {
            key    = nodeId+"#"+i;
            weight = 1.0;
            this.instanceWeights.put(key,weight);
        }
    }
    
    public void migrate(Set<String> leavingSet, Map<String, Double> arrivingSet)
    {
        // Remove the instances that migrate to a neighboring node
        for(String leavingKey: leavingSet)
            this.instanceWeights.remove(leavingKey);
        
        // Add the instances arriving from a neighboring node
        this.instanceWeights.putAll(arrivingSet);
    }
    
    public Set<String> selectForMigration()
    {
        return selectForMigrationWeightPDF();
        //return selectForMigrationUniform();
    }
    
    public Set<String> selectForMigrationWeightPDF()
    {
        Set<String> migKeys;
        Map<Double, List<String>> weightOrderedKeys;
        List<String> weightKeys;
        double       weight, sumWeights;
        
        // Order the instances on decreasing weight. Calculate total sum of instance weights
        sumWeights = 0;
        migKeys    = new HashSet<String>();
        weightOrderedKeys = new TreeMap<Double, List<String>>();
        for(Map.Entry<String, Double> entry: this.instanceWeights.entrySet())
        {
            weight     = entry.getValue();
            weightKeys = weightOrderedKeys.get(-weight);
            if (weightKeys == null)
            {
                weightKeys = new LinkedList<String>();
                weightOrderedKeys.put(-weight, weightKeys);
            }
            weightKeys.add(entry.getKey());
            sumWeights += weight;
        }
        
        double maxWeight;
        double sumMig, ranWeight, wadd;
        Iterator<Double> itWeight;
        Iterator<String> itKeys;
        String           key;
        
        // Determine the maximal sum of the weight of the instances that migrate
        maxWeight = sumWeights*P_MIG;
        
        // Pick instances using the ordered Map is CDF. Stop when sum of their weights exceeds the threshold
        sumMig = 0;
        while(sumMig < maxWeight)
        {
            // Pick a random random smaller than the total weight of all instances
            ranWeight = Uniform.staticNextDoubleFromTo(0, maxWeight);
            
            // Loop over the instances, ordered from high to low weight. Stop when sum of encountered weights exceeds the picked number.
            wadd     = 0;
            key      = null;
            weight   = 0;
            itWeight = weightOrderedKeys.keySet().iterator();
            while(itWeight.hasNext() && wadd < ranWeight)
            {
                weight = itWeight.next();
                itKeys = weightOrderedKeys.get(weight).iterator();
                while(itKeys.hasNext() && wadd < ranWeight)
                {
                    key = itKeys.next();
                    wadd += -weight;
                }
            }
            
            // The key right before the weight exceeded the threshold selects the instance to migrate.
            if (key != null)
            {
                migKeys.add(key);
                sumMig += -weight;
            }
        }
        
        return(migKeys);
    }
    
    public Set<String> selectForMigrationUniform()
    {
        Set<String> migKeys;
        
        // For all instances
        migKeys = new HashSet<String>();
        for(String key: this.instanceWeights.keySet())
        {
            // Select instance for migration?
            if(Uniform.staticNextDouble() <= P_MIG) migKeys.add(key);
        }
        
        return(migKeys);
    }
    
    
    
    public Map<String, Double> getInstances() { return this.instanceWeights; }
    
    public Double getInstanceWeight(String key) { return this.instanceWeights.get(key); }
}
