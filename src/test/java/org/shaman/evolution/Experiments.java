/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April - May 2005                                     *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
 \*********************************************************/
package org.shaman.evolution;

import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.neural.MLP;
import org.shaman.neural.Neuron;
import org.shaman.preprocessing.Normalization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;


/**
 * <h2>Experiments in Evolutionary Algorithms</h2>
 */

// **********************************************************\
// *              Neural Network Experiments                *
// **********************************************************/
public class Experiments
{
    static final String DATADIR = "./src/main/resources/results/EvolvingLocomotion/Walking/";

    // **********************************************************\
    // *              Neural Network Evolution                  *
    // **********************************************************/
    private void neuralNetEvolution() throws Exception
    {
        MemorySupplier    ms   = new MemorySupplier();
        InstanceSetMemory im   = new InstanceSetMemory();
        Normalization     norm = new Normalization();

        // Load 3-class wine classification data-set. 14 continuous variables and 1 class.
        TestSets.loadWine(ms, false);
        im.create(ms);

        // Standardize the data.
        norm.registerSupplier(0, ms, 0);
        ms.registerConsumer(0, norm, 0);
        norm.setType(Normalization.TYPE_STANDARDIZE);
        norm.init();
        im.create(ms);
        norm.trainTransformation(im);
        im = InstanceSetMemory.estimateAll(im, norm);

        MLPEnvironment mlpenv;
        MLP            mlp;

        // Create MLP right for classifying the wine
        mlp = new MLP();
        mlp.setNeuronType(Neuron.ACTIVATION_SIGMOID_EXP, new double[]{.5});
        mlp.setNetworkParameters(5, 0, MLP.OUTPUT_ONE_OF_N);
        //mlp.setNetworkParameters(20, 10, MLP.OUTPUT_ONE_OF_N);
        mlp.setTrainSet(im);
        mlp.create();

        // = new Normal(0.0, 0.1, new MersenneTwister());
        Normal pdftemp = new Normal(0.0, 1.0, new MersenneTwister());

        // Configuration MLP Evolution environment with previous
        mlpenv = new MLPEnvironment();
        mlpenv.setMLPTemplate(mlp);
        mlpenv.setPDFTemplate(pdftemp);
        mlpenv.setDataSet(im);
        mlpenv.initialize();

        Evolution      evo;

        // Setup evolution and parameters
        evo = new Evolution();
        evo.setEnvironment(mlpenv);
        evo.setPopulationSize(250);
        evo.setPCrossover(0.8);
        evo.setPMutation(0.1);
        evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        evo.setSurvivalFraction(0.1);
        evo.initialize();

        int     i;
        boolean found;
        MLPFitnessFunction fitfunc;

        fitfunc = (MLPFitnessFunction)mlpenv.makeFitnessFunction();
        found   = false;
        for(i=0; (i<500) && (!found); i++)
        {
            evo.generation();
            evo.logFitness();
            if (i%10 == 0) fitfunc.fitness(evo.getFittest(), true);
        }
        double fit = fitfunc.fitness(evo.getFittest(), true);
    }

    // **********************************************************\
    // *              Evolving Virtual Creatures                *
    // **********************************************************/
    private void evolvingVirtualCreatures() throws Exception
    {
        PhysicsEnvironment env;
        Evolution           evo;

        env = new PhysicsEnvironment();
        env.setFigureTemplate(new FigureSymmetricArms());
        //env.setFigureTemplate(new FigureJointArms());
        env.initialize();

        evo = new Evolution();
        env.setEvolution(evo);
        evo.setEnvironment(env);
        evo.setPopulationSize(100);
        evo.setPCrossover(0.8);
        evo.setPMutation(0.01);
        evo.setNumberOfThreads(3);
        evo.setFitnessBufferSize(100);
        //evo.setSurvivalFraction(0.2);
        //Graph islands = evo.makeIslandsGrid(3);
        //evo.setIslandParameters(islands, 5, 0.2);


        evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        //evo.setLogFile(DATADIR+"/fitness2.txt");
        //evo.setPersistence(1, DATADIR+"/evolution2.obj");
        evo.initialize();
        //evo.initRandomPopulation();;

        PhysicsFitnessFunction visualizeFunction = ((PhysicsFitnessFunction) env.makeFitnessFunction());
        visualizeFunction.initialize(env);
        visualizeFunction.fitness(evo.getFittest(), true);

        while(true)
        {
            // Run for 100 generations and save the fittest genotype to a file.
            for(int i=0; i<100; i++)
            {
                evo.generation();
                evo.logFitness();
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            NumberGenotype fittest = (NumberGenotype)evo.getFittest();
            double fitness = evo.getMaxFitness();
            ObjectOutputStream fittestOut = new ObjectOutputStream(new FileOutputStream(DATADIR+"walker_"+dateFormat.format(new Date(System.currentTimeMillis()))+".obj"));
            fittestOut.writeDouble(fitness);
            fittest.saveState(fittestOut);
            fittestOut.flush();
            fittestOut.close();;

            // Start over.
            evo.initRandomPopulation();
        }

        //System.exit(0);
        
        /*
        Extend: :
        - second level of limbs: method to extend box and put joint is same as already there to put arms on body. args:
           * place (5 possibilities, only plane with own joints not possble).
           * box dimensions x,y,z
           * joint angles
          place of parameters is fixed in genotypes so gene PDFs can take into account place selection constraints.
        - additional sine wave in anti-phase
        - wave frequency / amplitude modifiers
        */
    }

    public void showEvolvedCreature()
    {
        PhysicsEnvironment env;
        Evolution          evo;

        try
        {
            env = new PhysicsEnvironment();
            env.setFigureTemplate(new FigureSymmetricArms());
            env.initialize();

            evo = new Evolution();
            env.setEvolution(evo);
            evo.setEnvironment(env);
            evo.setPopulationSize(100);
            evo.setPCrossover(0.8);
            evo.setPMutation(0.01);
            evo.setNumberOfThreads(3);
            evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
            evo.initialize();
            
            List<NumberGenotype> genotypes = new LinkedList<>();
            final double []maxFitness = {0.0};
            NumberGenotype []fittest = new NumberGenotype[1];
            Files.list(new File(DATADIR).toPath()).forEach(new Consumer<Path>()
            {
                @Override
                public void accept(Path path)
                {
                    if (path.getFileName().toString().endsWith(".obj"))
                    {
                        try
                        {
                            ObjectInputStream oin = new ObjectInputStream(new FileInputStream(path.toFile()));
                            double fitness = oin.readDouble();
                            NumberGenotype genotype = (NumberGenotype)env.makeRandomGenotype();
                            genotype.loadState(oin);
                            if (fitness > maxFitness[0])
                            {
                                maxFitness[0] = fitness;
                                fittest[0] = genotype;
                            }
                            genotypes.add(genotype);
                            oin.close();
                        }
                        catch(Exception ex) { ex.printStackTrace(); }
                    }
                }
            });
            
            System.out.println("Loaded "+genotypes.size()+" evolved genotypes. Maximum fitness = "+maxFitness[0]);
            evo.setGenotype(genotypes.toArray(new Genotype[genotypes.size()]));
            evo.setFittest(fittest[0]);
            evo.setMaxFitness(maxFitness[0]);


            Genotype genotype;

            genotype = evo.getFittest();
            if (genotype == null)
                genotype = env.makeRandomGenotype();

            ((PhysicsFitnessFunction) env.makeFitnessFunction()).fitness(genotype, true);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }

    // **********************************************************\
    // *       Execute Evolutionary Algorithm Experiment        *
    // **********************************************************/
    public static void main(String []args)
    {
        Experiments app;

        app = new Experiments();
        try
        {
            // Run one of the experiments
            app.evolvingVirtualCreatures();
            //app.showEvolvedCreature();
            //app.neuralNetEvolution();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}