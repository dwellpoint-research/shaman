/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005 & May 2006                                *
 *  April 2015                                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005-15 Shaman Research                *
 \*********************************************************/
package org.shaman.evolution;

import cern.jet.random.Uniform;
import org.shaman.dataflow.Persister;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;
import org.shaman.graph.GraphFactory;
import org.shaman.graph.GraphNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * <h2>Genetic Algorithm</h2>
 */
public class Evolution implements Persister
{
    // Re-scale fitness values before (proportionate) selection?
    public static final int FITNESS_SCALE_NONE         = 0;        // No. Use raw fitness values.
    public static final int FITNESS_SCALE_RANK         = 10;       // Rescale on fitness value rank.
    public static final int FITNESS_SCALE_TOP          = 20;       // Assign maximum fitness to top values.
    public static final int FITNESS_SCALE_LINEAR_SHIFT = 30;

    // Genetic Algorithm parameters
    private Environment environment;          // GA Environment supplying Genotypes and Fitness function
    private int    populationSize;            // Number of genotypes in the population
    private double pMutation;                 // Probability of mutation for every gene in the offspring
    private double pCrossover;                // Probability of crossover between every pair of selected genotypes
    private int    fitnessScale;              // Type of fitness value scaling
    private double fitnessScaleParam;         // Fitness scaling parameter
    private double survivalFraction;          // Fraction of best genotypes that survive into the next generation
    // Island Model parameters
    private Graph  islandGraph;               // Graph of the Islands over which the population is split up
    private int    migrationFrequency;        // How many generations between each migration
    private double migrationFraction;         // Fraction of (fittest) genotypes on each island that migrate to a neighboring island
    // Persistence
    private String logFilePath;               // Path to file to log fitness statistics in
    private int    persistFrequency;          // Frequency (once how many generations) to persist the evolution state
    private String persistFilePath;           // Path to evolution persistence file
    private int    fitnessBufferSize;         // Keep all fitness values for the last generations.
    // Parallelization
    private int    numberOfThreads;           // Number of parallel Fitness Evaluation threads

    // ---- Genetic Algorithm ----
    private int             generation;       // Generation number
    private Genotype      []genotype;         // Current population of genotypes
    private double        []fitness;          // Current fitness values
    private FitnessFunction fitnessFunction;  // Fitness function. Used in serial fitness evaluation.
    private double        []pFitnessScale;    // Fitness scaling probabilities

    private double          maxFitness;       // Maximum fitness ever encountered
    private Genotype        fittest;          // Fittest genotype ever encountered

    private TreeMap<Integer, double []> fitnessBuffer; // Map of: int generation -> double []{ordered fitness values}

    private int           []islandIndex;     // [i] = Index of Island of genotype[i]

    private Writer fitnessLogWriter;          // Fitness stats file

    //  Parallelization
    private ThreadPoolExecutor fitnessPool;                           // Fitness evaluation Thread-pool and executor                    
    private CountDownLatch fitnessWait;                               // Wait for all fitness function evaluations to end before running (island-)GA logic.
    private BlockingQueue<FitnessFunction> fitnessFunctionsBuffer;    // Buffer of Fitness Functions to be used in Fitness Evaluation commands

    // *********************************************************\
    // *                 Genetic Algorithm                     *
    // *********************************************************/
    public void generation() throws LearnerException
    {
        Genotype []newGeneration;
        double   []fitnessValues;

        // Increase generation counter on which logging is based.
        this.generation++;

        // Make fitness value buffer for each genotype in the population.
        fitnessValues = new double[this.populationSize];

        // Evaluate fitness of genotypes.
        if (this.numberOfThreads <= 1) evaluateFitnessSerial(fitnessValues);
        else                           evaluateFitnessThreaded(fitnessValues);

        // Persist evolution state once in a while?
        if (this.persistFrequency != 0 && this.generation % this.persistFrequency == 0) persistPopulation();

        // Keep track of the fitness values over the generations?
        if (this.fitnessBuffer != null) updateFitnessBuffer(fitnessValues);

        // Create the next generation, taking into account the island model or treating the entire population as a whole.
        if (this.islandGraph == null)
            newGeneration = makeNewGenerationPanmictic(fitnessValues);
        else newGeneration = makeNewGenerationIslands(fitnessValues);

        this.genotype = newGeneration;
    }

    private Genotype []makeNewGenerationPanmictic(double []fitnessValues)
    {
        // Create a new generation of genotype using selection / crossover / mutation operators. Use the entire population.
        return makeNewGenerationForPopulation(this.genotype, fitnessValues);
    }

    private Genotype []makeNewGenerationForPopulation(Genotype []genotype, double []fitnessValues)
    {
        int populationSize;
        Genotype []newGeneration;
        Genotype []crossOverGenotypes;
        Genotype   genotype1, genotype2;
        int        startIndex;
        double     sumFitness;

        // Make the new generation
        populationSize  = genotype.length;
        newGeneration   = new Genotype[populationSize];

        // Post-process fitness values to avoid population diversity problems
        scaleFitness(fitnessValues);

        // Keep the best genotypes for next generation
        if (this.survivalFraction > 0) startIndex = survival(fitnessValues, genotype, newGeneration);
        else                           startIndex = 0;

        // Complete generation using crossover and mutation operators
        sumFitness         = sumFitness(fitnessValues);
        crossOverGenotypes = new Genotype[2];
        for(int i=startIndex; i<populationSize; i+=2)
        {
            genotype1 = select(fitnessValues, sumFitness, genotype);
            genotype2 = select(fitnessValues, sumFitness, genotype);
            crossOver(genotype1, genotype2, crossOverGenotypes);

            newGeneration[i  ] = crossOverGenotypes[0];
            if (i+1 < newGeneration.length) newGeneration[i+1] = crossOverGenotypes[1];
        }

        return newGeneration;
    }

    private int survival(double []fitnessValues, Genotype []generation, Genotype []newGeneration)
    {
        int    i, numberOfSurvivors;
        int  []fitnessOrderedIndex;

        // Keep the best genotypes for the next generation
        numberOfSurvivors = (int)(generation.length * this.survivalFraction);
        if (numberOfSurvivors % 2 == 1) numberOfSurvivors--;

        fitnessOrderedIndex  = getFitnessOrder(fitnessValues);
        for (i=0; i<numberOfSurvivors; i++) newGeneration[i] = generation[fitnessOrderedIndex[i]];

        return(i);
    }

    private void scaleFitness(double []fitnessValues)
    {
        if      (this.fitnessScale == FITNESS_SCALE_NONE)
        {
            // Select with probability directly proportional to the raw fitness
        }
        else if ((this.fitnessScale == FITNESS_SCALE_RANK) || (this.fitnessScale == FITNESS_SCALE_TOP))
        {
            int []fitnessOrderedIndex;
            int   i;

            // Order the population on decreasing fitness
            fitnessOrderedIndex = getFitnessOrder(fitnessValues);

            // Assign selection probabilities to the genotypes.
            for (i=0; i<fitnessValues.length; i++) fitnessValues[fitnessOrderedIndex[i]] = this.pFitnessScale[i];
        }
    }

    private int []getFitnessOrder(double []fitnessValues)
    {
        TreeMap<Double, List<Integer>> orderMap;

        // Sort the fitness values, keeping track of their indexes in the given array.
        orderMap = new TreeMap<>();
        for(int i=0; i<fitnessValues.length; i++)
        {
            double value = fitnessValues[i];
            if (!orderMap.containsKey(value)) orderMap.put(value, new LinkedList<>());
            orderMap.get(value).add(i);
        }

        // Return the indexes of the fitness values from high to low value.
        int i=0;
        int []fitnessOrderedIndex = new int[fitnessValues.length];
        for(Double value: orderMap.descendingKeySet())
        {
            for(Integer index: orderMap.get(value)) fitnessOrderedIndex[i++] = index;
        }

        return(fitnessOrderedIndex);
    }

    private double sumFitness(double []fitnessValues)
    {
        double sumFitness;

        // Calculate sum of fitness values
        sumFitness = 0;
        for (int i=0; i<fitnessValues.length; i++) sumFitness += fitnessValues[i];

        return sumFitness;
    }

    private Genotype select(double []fitnessValues, double sumFitness, Genotype []genotype)
    {
        int    pos;
        double accFitness, random;


        // Roulette wheel weighted by fitness values
        random = Uniform.staticNextDoubleFromTo(0, sumFitness);
        pos    = 0;
        accFitness = 0;
        while (accFitness+fitnessValues[pos] < random)
        {
            accFitness += fitnessValues[pos];
            pos++;
        }

        return(genotype[pos]);
    }

    private void crossOver(Genotype genotype1, Genotype genotype2, Genotype[] crossOverGenotypes)
    {
        int      i, crossOverPosition, len;
        Genotype crossOverGenotype1, crossOverGenotype2;

        // Determine crossover position in 'pCrossover' percent of the case, else take entire genotype
        if (flip(this.pCrossover)) crossOverPosition = Uniform.staticNextIntFromTo(0, genotype1.getLength()-1);
        else                       crossOverPosition = genotype1.getLength();

        // Make the 2 crossed over genotypes
        crossOverGenotype1 = genotype1.crossover(genotype2, crossOverPosition);
        crossOverGenotype2 = genotype2.crossover(genotype1, crossOverPosition);

        // Apply mutation to the new genotypes
        len = crossOverGenotype1.getLength();
        for (i=0; i<len; i++) if (flip(this.pMutation)) crossOverGenotype1.mutate(i);
        len = crossOverGenotype2.getLength();
        for (i=0; i<len; i++) if (flip(this.pMutation)) crossOverGenotype2.mutate(i);

        // Return the 2 new genotypes for the next generation
        crossOverGenotypes[0] = crossOverGenotype1;
        crossOverGenotypes[1] = crossOverGenotype2;
    }

    private boolean flip(double p)
    {
        // Return true with probability p.
        return(Uniform.staticNextDoubleFromTo(0, 1.0) <= p);
    }

    private void updateFitnessBuffer(double[] fitnessValues)
    {
        synchronized (this.fitnessBuffer)
        {
            // Remove the fitness value of all generations that are too long ago.
            Set<Integer> pruneGenerations = new HashSet<>(this.fitnessBuffer.headMap(this.generation - this.fitnessBufferSize + 1).keySet());
            if (!pruneGenerations.isEmpty())
            {
                for (Integer pruneGeneration : pruneGenerations) this.fitnessBuffer.remove(pruneGeneration);
            }

            // Copy / sort and remember the fitness values of this generation.
            double[] sortedFitness = Arrays.copyOf(fitnessValues, fitnessValues.length);
            Arrays.sort(sortedFitness);
            this.fitnessBuffer.put(this.generation, sortedFitness);
        }
    }

    // *********************************************************\
    // *                   Island Model                        *
    // *********************************************************/
    public Graph makeIslandsGrid(int gridSize) throws GraphException
    {
        Graph grid;

        grid = GraphFactory.makeGrid(gridSize, gridSize, GraphFactory.GRID_NEIGHBORHOOD_C9);

        return grid;
    }

    private Genotype []makeNewGenerationIslands(double []fitnessValues) throws LearnerException
    {
        Map<Integer, Island> islands;
        Map<GraphNode, Integer> islandNodes;

        // Distribute the genotypes and their fitness values per island.
        islands = new TreeMap<>();
        islandNodes = new HashMap<>();
        for(int i=0; i<this.islandIndex.length; i++)
        {
            int idx = this.islandIndex[i];
            GraphNode islandNode = this.islandGraph.getNodes()[idx];
            if (!islands.containsKey(idx))
            {
                islands.put(idx, new Island(islandNode));
                islandNodes.put(islandNode, idx);
            }
            islands.get(idx).add(this.genotype[i], i, fitnessValues[i]);
        }

        // Is it time for a migration step?
        if (this.generation % this.migrationFrequency == 0)
        {
            try
            {
                Map<GraphNode, List<Integer>> migrationIndices;

                // Collect all migrants per destination Island.
                migrationIndices = new HashMap<>();

                // For each Island.
                for (Island island : islands.values())
                {
                    Map<GraphNode, List<Integer>> islandMigration;

                    // Make a list of the fittest genotypes that will migrate to a neighboring island. Collect them per destination island.
                    islandMigration = island.migration();
                    for (GraphNode destinationIsland: islandMigration.keySet())
                    {
                        if (!migrationIndices.containsKey(destinationIsland)) migrationIndices.put(destinationIsland, new LinkedList<>());
                        migrationIndices.get(destinationIsland).addAll(islandMigration.get(destinationIsland));
                    }
                }

                // Integrate the migrants in their destination island.
                for(GraphNode destinationNode: migrationIndices.keySet())
                {
                    List<Integer> migrantIndices;
                    Island destination;
                    List<Genotype> migrantGenotypes;
                    List<Double> migrantFitness;

                    // Collect the global indices, genotypes and fitness values of the migrants.
                    migrantIndices = migrationIndices.get(destinationNode);
                    migrantGenotypes = new LinkedList<>();
                    migrantFitness = new LinkedList<>();
                    for(Integer migrantIndex: migrantIndices)
                    {
                        migrantGenotypes.add(this.genotype[migrantIndex]);
                        migrantFitness.add(fitnessValues[migrantIndex]);
                    }

                    Integer destinationIndex;

                    // Put them on their new island.
                    destinationIndex = islandNodes.get(destinationNode);
                    destination = islands.get(destinationIndex);
                    destination.integrateMigrants(migrantGenotypes, migrantIndices, migrantFitness);

                    // Adjust their island assignment.
                    for(Integer migrantIndex: migrantIndices)
                    {
                        this.islandIndex[migrantIndex] = destinationIndex;
                    }
                }
            }
            catch(GraphException ex) { throw new LearnerException(ex); }
        }

        TreeMap<Integer, Genotype> indexGenotype;

        // For each island separately.
        indexGenotype = new TreeMap<>();
        for(Island island: islands.values())
        {
            List<Integer> genotypeIndices;
            Genotype []newIslandGeneration;

            // Make the new genaration of genotype from the population on this island.
            newIslandGeneration = island.makeNewGeneration();

            // Collect them on their index in the global population.
            genotypeIndices = island.getIndices();
            int i = 0;
            for(Integer index: genotypeIndices) if (index != null) indexGenotype.put(index, newIslandGeneration[i++]);
        }

        // Return the new genotypes sorted on their index.
        return indexGenotype.values().toArray(new Genotype[indexGenotype.size()]);
    }

    class Island
    {
        private GraphNode  islandNode;
        private List<Genotype> genotypes;
        private List<Integer> indices;
        private List<Double> fitnessValues;
        // ---------

        public Island(GraphNode islandNode)
        {
            this.islandNode = islandNode;
            this.genotypes = new ArrayList<>();
            this.indices = new ArrayList<>();
            this.fitnessValues = new ArrayList<>();
        }

        public void add(Genotype genotype, int index, double fitnessValue)
        {
            // Collect the genotype, its index in the global population and its fitness value.
            this.genotypes.add(genotype);
            this.indices.add(index);
            this.fitnessValues.add(fitnessValue);
        }

        public Map<GraphNode, List<Integer>> migration() throws GraphException
        {
            double []fitness;
            int []fitnessOrder;

            // Sort the genotypes on fitness.
            fitness = new double[this.fitnessValues.size()];
            { int i=0; for(Double fitnessValue: this.fitnessValues) fitness[i++] = fitnessValue; }
            fitnessOrder = getFitnessOrder(fitness);

            Map<GraphNode, List<Integer>> migrating;
            int migrationCount;

            // Count how many genotypes should migrate to another island.
            migrationCount = (int)(this.genotypes.size() * migrationFraction);

            // Migrate the 'n' fittest genotypes to a neighboring island.
            GraphNode []neighborIslands = this.islandNode.getNeighbors();
            migrating = new HashMap<>();
            for(int i=0; i<migrationCount; i++)
            {
                // Find the local and global index of the migrating genotype
                int migratingIndex = fitnessOrder[i];
                int globalIndex = this.indices.get(migratingIndex);

                // Pick a destination island and collect add the global index of the migration genotype to it.
                int destinationIsland = Uniform.staticNextIntFromTo(0, neighborIslands.length-1);
                GraphNode destinationNode = neighborIslands[destinationIsland];
                if (!migrating.containsKey(destinationNode)) migrating.put(destinationNode, new LinkedList<>());
                migrating.get(destinationNode).add(globalIndex);

                // Clear the local index of the migrating genotype so it's excluded from the next generation on this island.
                this.genotypes.set(migratingIndex, null);
                this.fitnessValues.set(migratingIndex, null);
                this.indices.set(migratingIndex, null);
            }

            return migrating;
        }

        public void integrateMigrants(List<Genotype> migrantGenotypes, List<Integer> migrantIndices, List<Double> migrantFitness)
        {
            Iterator<Genotype> genotypeIterator = migrantGenotypes.iterator();
            Iterator<Integer> indexIterator = migrantIndices.iterator();
            Iterator<Double> fitnessIterator = migrantFitness.iterator();

            // Loop over the migrants.
            int pos = 0;
            while(genotypeIterator.hasNext())
            {
                Genotype genotype = genotypeIterator.next();
                Integer index = indexIterator.next();
                Double fitness = fitnessIterator.next();

                // Find a position that's free.
                while(pos < this.genotypes.size() && this.genotypes.get(pos) != null) pos++;
                if (pos < this.genotypes.size())
                {
                    // Fill in the blanks left by genotypes that have migrated away from this Island.
                    this.genotypes.set(pos, genotype);
                    this.indices.set(pos, index);
                    this.fitnessValues.set(pos, fitness);
                }
                else
                {
                    // Extend the island with more genotypes there are more incoming than outgoing migrants.
                    this.genotypes.add(genotype);
                    this.indices.add(index);
                    this.fitnessValues.add(fitness);
                }
            }
        }

        public Genotype []makeNewGeneration()
        {
            int populationSize;
            Genotype []islandPopulation;
            double []fitnessValues;

            // Collect the genotypes and their fitness values. Skip over migrated genotypes.
            populationSize = 0;
            for(Genotype genotype: this.genotypes) if (genotype != null) populationSize++;
            islandPopulation = new Genotype[populationSize];
            fitnessValues = new double[populationSize];
            { int i=0; for(Genotype genotype:   this.genotypes)     if (genotype != null)     islandPopulation[i++] = genotype; }
            { int i=0; for(Double fitnessValue: this.fitnessValues) if (fitnessValue != null) fitnessValues[i++]    = fitnessValue; }

            Genotype []newGeneration;

            // Create the new generation on the population of this island. Use the same GA parameters as for a global population.
            newGeneration = makeNewGenerationForPopulation(islandPopulation, fitnessValues);

            return newGeneration;
        }

        public List<Integer> getIndices()
        {
            return this.indices;
        }
    }

    // *********************************************************\
    // *                  Fitness Evaluation                   *
    // *********************************************************/
    private void evaluateFitnessThreaded(double []fitnessValues) throws LearnerException
    {
        try
        {
            FitnessCommand  fitnessCommand;

            // Create a countdown matching the population size. All FitnessCommands decrease the countdown with 1 after they finish.
            this.fitnessWait = new CountDownLatch(fitnessValues.length);

            // Evaluate all the genotype in a number of Threads. Use a number of FitnessFunctions for this.
            for (int i=0; i<fitnessValues.length; i++)
            {
                fitnessCommand = new FitnessCommand(this);
                fitnessCommand.setIndex(i);
                fitnessCommand.setGenotype(this.genotype[i]);
                fitnessCommand.setFitnessFunction(this.fitnessFunctionsBuffer.take());
                this.fitnessPool.execute(fitnessCommand);
            }

            // Wait for all Fitness Evaluation threads to finish
            this.fitnessWait.await();

            // Copy raw fitness values in the given fitness buffer
            for (int i=0; i<this.fitness.length; i++) fitnessValues[i] = this.fitness[i];
        }
        catch(InterruptedException ex) { throw new LearnerException(ex); }
    }

    protected synchronized void setFitnessResult(int i, double fitness, FitnessFunction fitnessFunction)
    {
        try
        {
            // Record given fitness value
            this.fitness[i] = fitness;

            // Check if it's the absolute fittest one.
            if (fitness >= this.maxFitness)
            {
                this.maxFitness = fitness;
                this.fittest    = this.genotype[i];
            }

            // Return the Fitness function to the buffer
            this.fitnessFunctionsBuffer.put(fitnessFunction);
        }
        catch(InterruptedException ex) { }
        finally
        {
            // One more fitness evaluated.
            this.fitnessWait.countDown();
        }
    }

    private void evaluateFitnessSerial(double []fitnessValues) throws LearnerException
    {
        int        i;
        double     fitness;

        // Evaluate fitness of the current population. Remember fittest genotype ever.
        for (i=0; i<this.populationSize; i++)
        {
            fitness         = this.fitnessFunction.fitness(this.genotype[i]);
            this.fitness[i] = fitness;
            if (fitness >= this.maxFitness)
            {
                this.maxFitness = fitness;
                this.fittest    = this.genotype[i];
            }
            fitnessValues[i] = fitness;
        }
    }

    // *********************************************************\
    // *               Initialization / Cleanup                *
    // *********************************************************/
    public void initialize() throws ConfigException
    {
        // Get the fitness function from the environment
        try
        {
            this.fitnessFunction = this.environment.makeFitnessFunction();
        }
        catch(LearnerException ex) { throw new ConfigException(ex); }

        // Initialize fitness scaling procedure
        initFitnessScaling();

        // Open statistics log file when path given
        if (this.logFilePath != null)
        {
            try
            {
                this.fitnessLogWriter = new FileWriter(this.logFilePath, true);
            }
            catch(IOException ex) { throw new ConfigException(ex); }
        }

        // Prepare Parallel Fitness Evaluation?
        if (this.numberOfThreads > 1)
        {
            try
            {
                ThreadPoolExecutor fitnessPool;
                BlockingQueue<FitnessFunction> fitnessFunctionsBuffer;
                FitnessFunction  fitnessFunction;

                // A Thread Execution Pool for the 'n' parallel running Fitness Evaluation Commands
                fitnessPool = new ThreadPoolExecutor(this.numberOfThreads, this.numberOfThreads, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

                // A Buffer for 'n' Fitness Functions
                fitnessFunctionsBuffer = new LinkedBlockingQueue<>(this.numberOfThreads);
                for (int i=0; i<this.numberOfThreads; i++)
                {
                    fitnessFunction = this.environment.makeFitnessFunction();
                    fitnessFunctionsBuffer.put(fitnessFunction);
                }

                this.fitnessPool = fitnessPool;
                this.fitnessFunctionsBuffer = fitnessFunctionsBuffer;
            }
            catch(InterruptedException ex) { throw new ConfigException(ex); }
            catch(LearnerException ex)     { throw new ConfigException(ex); }
        }

        // Initialize population. From previously persisted file or generated randomly.
        initPopulation();
    }

    private void initFitnessScaling()
    {
        // Prepare fitness scaling buffers
        this.pFitnessScale = new double[this.populationSize];

        if      (this.fitnessScale == FITNESS_SCALE_RANK)
        {
            // Rank scaling. Probability of selection depends only on rank.
            for (int i=0; i<this.pFitnessScale.length; i++) this.pFitnessScale[i] = 1 / Math.sqrt(1+i);
        }
        else if (this.fitnessScale == FITNESS_SCALE_TOP)
        {
            int topEdge;

            // Top scaling. Only ever select the top 'fitnessScaleParam'% fittest ones. With equal probability.
            topEdge = (int)(this.populationSize * this.fitnessScaleParam);
            for (int i=0; i<this.pFitnessScale.length; i++)
            {
                if (i < topEdge)
                    this.pFitnessScale[i] = 1.0;
                else this.pFitnessScale[i] = 0.0;
            }
        }
    }

    private void initPopulation() throws ConfigException
    {
        // Initialize a generation of random genotypes.
        initRandomPopulation();

        // Load from saved state when file specified. Else start with random population.
        if (this.persistFilePath != null)
        {
            File persistFile;

            persistFile = new File(this.persistFilePath);
            if (persistFile.exists())
            {
                try
                {
                    ObjectInputStream oin = new ObjectInputStream(new FileInputStream(persistFile));
                    loadState(oin);
                    oin.close();
                }
                catch(IOException ex){ throw new ConfigException(ex); }
            }
            else
            {
                System.out.println("Cannot find persisted evolution. Starting a random new one.");
            }
        }
    }

    public void initRandomPopulation() throws ConfigException
    {
        try
        {
            // Make space for one generation of genotypes
            this.genotype = new Genotype[this.populationSize];
            this.fitness  = new double[this.populationSize];

            // Clear fitness stats
            this.generation = 0;
            this.maxFitness = 0;
            this.fittest    = null;

            // Keep a log of the ordered fitness values of a number of generations.
            if (this.fitnessBufferSize > 0) this.fitnessBuffer = new TreeMap<>();

            // Generate the random genotypes.
            for (int i = 0; i < this.genotype.length; i++)
            {
                this.genotype[i] = this.environment.makeRandomGenotype();
            }
            this.fittest = this.genotype[0];

            // When using an island model.
            if (this.islandGraph != null)
            {
                int numberOfIslands;
                int []islandIndex;

                // Spread the genotypes out randomly over the islands.
                islandIndex = new int[this.populationSize];
                numberOfIslands = this.islandGraph.getNumberOfNodes();
                for(int i=0; i<this.populationSize; i++)
                {
                    islandIndex[i] = Uniform.staticNextIntFromTo(0, numberOfIslands-1);
                }
                this.islandIndex = islandIndex;
            }
        }
        catch(LearnerException ex) { throw new ConfigException(ex); }
    }

    public void cleanUp() throws DataFlowException
    {
        // Shutdown Fitness Evaluation Thread Pool if it's there...
        if (this.fitnessPool != null) this.fitnessPool.shutdownNow();
    }

    // *********************************************************\
    // *                  Evolution Results                    *
    // *********************************************************/
    public void logFitness() throws LearnerException
    {
        double averageFitness, maxFitnessInGeneration;
        String logMessage;

        // Calculate average fitness
        averageFitness = maxFitnessInGeneration = 0;
        for (int i=0; i<this.fitness.length; i++)
        {
            averageFitness += this.fitness[i];
            if (this.fitness[i] > maxFitnessInGeneration) maxFitnessInGeneration = this.fitness[i];
        }
        averageFitness /= this.fitness.length;

        try
        {
            // Log message about fitness
            logMessage = this.generation+"\t"+averageFitness+"\t"+maxFitnessInGeneration+"\t"+ this.maxFitness +"\n";
            if (this.fitnessLogWriter != null)
            {
                this.fitnessLogWriter.write(logMessage);
                this.fitnessLogWriter.flush();
            }
            System.out.print(logMessage);
        }
        catch(IOException ex) { throw new LearnerException(ex); }
    }

    public void persistPopulation() throws LearnerException
    {
        if (this.persistFilePath != null)
        {
            try
            {
                ObjectOutputStream oout;

                oout = new ObjectOutputStream(new FileOutputStream(this.persistFilePath));
                saveState(oout);
                oout.close();
            }
            catch (ConfigException | IOException ex) { throw new LearnerException(ex); }
        }
    }

    public Genotype getFittest()  { return(this.fittest); }
    public double getMaxFitness() { return(this.maxFitness); }

    public int getGeneration()
    {
        return this.generation;
    }

    public int getCurrentPopulationSize()
    {
        return this.genotype.length;
    }

    public TreeMap<Integer, double []> getFitnessBufferCopy()
    {
        TreeMap<Integer, double []> copy;
        if (this.fitnessBuffer != null)
        {
            synchronized (this.fitnessBuffer)
            {
                copy = new TreeMap<>(this.fitnessBuffer);
            }
        }
        else copy = null;

        return copy;
    }

    public int getFitnessBufferSize()
    {
        return this.fitnessBufferSize;
    }

    // *********************************************************\
    // *                    Persistence                        *
    // *********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            int               i;

            // Read generation number and absolute fittest genotype
            this.generation = ((Integer)oin.readObject()).intValue();
            this.fittest    = this.environment.makeRandomGenotype();
            this.fittest.loadState(oin);
            this.maxFitness = ((Double)oin.readObject()).doubleValue();

            // Read current population
            for (i=0; i<this.genotype.length; i++)
            {
                this.genotype[i] = this.environment.makeRandomGenotype();
                this.genotype[i].loadState(oin);
            }
            this.fitness = (double [])oin.readObject();
        }
        catch(LearnerException ex)       { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }

    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            int  i;

            // Write generation number and absolute fittest genotype
            oout.writeObject(new Integer(this.generation));
            this.fittest.saveState(oout);
            oout.writeObject(new Double(this.maxFitness));

            // Write current population
            for (i=0; i<this.genotype.length; i++) this.genotype[i].saveState(oout);

            // And their fitness values
            oout.writeObject(this.fitness);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }

    // *********************************************************\
    // *                Evolution Parameters                   *
    // *********************************************************/
    public void setEnvironment(Environment env) { this.environment = env; }
    public void setPopulationSize(int popSize)  { this.populationSize = popSize; }
    public void setPMutation(double pmut)       { this.pMutation = pmut; }
    public void setPCrossover(double pcross)    { this.pCrossover = pcross; }
    public void setFitnessScale(int fitscale)   { this.fitnessScale = fitscale; }
    public void setFitnessScaleParam(double fitscaleparam) { this.fitnessScaleParam = fitscaleparam; }
    public void setSurvivalFraction(double survivalRation) { this.survivalFraction = survivalRation; }
    public void setNumberOfThreads(int numThreads)      { this.numberOfThreads = numThreads; }

    public void setIslandParameters(Graph islandGraph, int migrationFrequency, double migrationFraction)
    {
        this.islandGraph = islandGraph;
        this.migrationFrequency = migrationFrequency;
        this.migrationFraction = migrationFraction;
    }

    public void setFitnessBufferSize(int fitnessBufferSize)
    {
        this.fitnessBufferSize = fitnessBufferSize;
    }

    public void setLogFile(String logpath) { this.logFilePath = logpath; }
    public void setPersistence(int perfreq, String perpath)
    {
        this.persistFrequency = perfreq;
        this.persistFilePath = perpath;
    }
    
    public void setGenotype(Genotype []genotype)
    {
        this.genotype = genotype;
    }
    
    public Genotype []getGenotypes()
    {
        return this.genotype;
    }
    
    public void setFittest(Genotype genotype)
    {
        this.fittest = genotype;
    }
    
    public void setMaxFitness(double maxFitness)
    {
        this.maxFitness = maxFitness;
    }

    public Evolution()
    {
    }
}