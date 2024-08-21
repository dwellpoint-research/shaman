/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  January 2006                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2006 Shaman Research                   *
 \*********************************************************/
package org.shaman.evolution;

import junit.framework.TestCase;
import org.shaman.exceptions.ShamanException;
import org.shaman.graph.Graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Test evolution of Text strings
 */
public class TextEvolutionTest extends TestCase
{
    // *********************************************************\
    // *            Test Evolution on Text Strings             *
    // *********************************************************/
    public void testIslandTextEvolution() throws ShamanException, IOException
    {
        TextEnvironment txtenv;
        String          goal;

        txtenv = new TextEnvironment();
        //goal = "the facts of life to make an alteration in the evolvement of an organic life system is fatal a coding sequence cannot be revised once it s been established";
        goal = "the facts of life";
        txtenv.setText(goal);
        txtenv.initialize();

        FileWriter experimentWriter = new FileWriter(new File("./src/main/resources/results/IslandEvolution/text_islands_exp.txt"));
        final int NUM_TESTS = 100;
        for(int i=0; i<NUM_TESTS; i++)
        {
            Evolution evo;
            Graph islands;

            evo = new Evolution();
            evo.setEnvironment(txtenv);
            evo.setPopulationSize(1000);
            evo.setPCrossover(0.8);
            evo.setPMutation(0.01);
            evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);

            islands = evo.makeIslandsGrid(3);
            //evo.setIslandParameters(islands, 5, 0.2);

            evo.initialize();

            boolean found = false;
            while (!found)
            {
                evo.generation();
                if (evo.getGeneration() % 100 == 0)
                {
                    System.err.println("\t"+evo.getGeneration()+". Max fitness "+evo.getMaxFitness()+"/"+goal.length()+" in population of "+evo.getCurrentPopulationSize()+" for '"+((TextGenotype)evo.getFittest()).getText()+"'");
                }

                found = evo.getMaxFitness() == goal.length();
            }

            System.err.println(i+"/"+NUM_TESTS+". Found in generation " + evo.getGeneration() + " '" + goal + "'");

            experimentWriter.write(i+"\t"+evo.getGeneration()+"\n");
            experimentWriter.flush();
        }
        experimentWriter.close();
    }

    public void testTextEvolution() throws Exception
    {
        TextEnvironment txtenv;
        Evolution       evo;
        String          goal;

        txtenv = new TextEnvironment();
        goal = "paranoia is the belief in a hidden order behind the visible";
        txtenv.setText(goal);
        txtenv.initialize();

        evo = new Evolution();
        evo.setEnvironment(txtenv);
        evo.setPopulationSize(100);
        evo.setPCrossover(0.6);
        evo.setPMutation(0.01);
        evo.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        //evo.setFitnessScale(Evolution.FITNESS_SCALE_TOP);
        //evo.setFitnessScaleParam(0.25);
        evo.setFitnessBufferSize(50);
        evo.initialize();

        int     i;
        boolean found;

        found = false;
        for(i=0; (i<10000) && (!found); i++)
        {
            evo.generation();

            if (evo.getGeneration() % 100 == 0)
            {
                System.err.println("\t"+evo.getGeneration()+". Max fitness "+evo.getMaxFitness()+"/"+goal.length()+" in population of "+evo.getCurrentPopulationSize()+" for '"+((TextGenotype)evo.getFittest()).getText()+"'");
                System.err.println(evo.getFitnessBufferCopy().size());
            }

            if (evo.getMaxFitness() == goal.length()) found = true;
        }
        assertTrue(i < 2000);

        System.err.println(i+" fittest : "+evo.getMaxFitness()+" = "+evo.getFittest());
    }

    // **********************************************************\
    // *                     Test-Case Setup                    *
    // **********************************************************/
    protected void setUp() throws Exception
    {
    }

    protected void tearDown() throws Exception
    {
    }

    public TextEvolutionTest()
    {
    }
}