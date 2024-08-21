package org.shaman.evolution;

import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.Graph;
import org.shaman.graph.GraphException;

import javax.swing.*;
import java.awt.*;

/**
 * @author Johan Kaers
 */
public class FitnessLogFrame extends JFrame implements Runnable
{
    // Fear and Loathing in Las Vegas
    private static final String GOAL = "we were somewhere around barstow on the edge of the desert when the drugs began to take hold i remember saying something like I feel a bit lightheaded maybe you should drive and suddenly there was a terrible roar all around us and the sky was full of what looked like huge bats all swooping and screeching and diving around the car which was going about a hundred miles an hour with the top down to las vegas"; // And a voice was screaming: \"Holy Jesus! What are these goddamn animals?\""
    // Blade Runner
    //private static final String GOAL = "the facts of life to make an alteration in the evolvement of an organic life system is fatal a coding sequence cannot be revised once it s been established";
    
    private Evolution evolution;
    private FitnessLogPanel fitnessLogPanel;

    public void run()
    {
        boolean found = false;
        while (!found)
        {
            try
            {
                this.evolution.generation();
                if (this.evolution.getGeneration() % 100 == 0)
                {
                    System.out.println("\t"+this.evolution.getGeneration()+". Max fitness "+this.evolution.getMaxFitness()+"/"+ GOAL.length()+" in population of "+this.evolution.getCurrentPopulationSize()+" for '"+((TextGenotype)this.evolution.getFittest()).getText()+"'");
                }
                found = this.evolution.getMaxFitness() == GOAL.length();
                
                //try { Thread.sleep(10); } catch(InterruptedException ex) { }
                
                this.fitnessLogPanel.draw();
            }
            catch(LearnerException ex)
            {
                ex.printStackTrace();
            }
        }
        
        System.out.print("Found in generation "+this.evolution.getGeneration());
    }

    private void initEvolution() throws ConfigException, GraphException
    {
        TextEnvironment txtenv;
        String          goal;

        txtenv = new TextEnvironment();
        txtenv.setText(GOAL);
        txtenv.initialize();

        Evolution evolution;
        Graph islands;

        evolution = new Evolution();
        evolution.setEnvironment(txtenv);
        evolution.setPopulationSize(1000);
        evolution.setPCrossover(0.8);
        evolution.setPMutation(0.01);          // 0.001 works very well.
        evolution.setFitnessScale(Evolution.FITNESS_SCALE_RANK);
        evolution.setFitnessBufferSize(200);
        islands = evolution.makeIslandsGrid(3);
        evolution.setIslandParameters(islands, 50, 0.4);
        evolution.initialize();

        this.evolution = evolution;
    }

    private void initFrame()
    {
        Container contentPane;
        setSize(1024,512);
        setTitle("Fitness log");
        contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.BLACK);

        FitnessLogPanel logPanel = new FitnessLogPanel(this.evolution);
        logPanel.setMaximumFitness(GOAL.length());
        logPanel.setMaximumLine(false);
        contentPane.add(logPanel, BorderLayout.CENTER);
        this.fitnessLogPanel = logPanel;

        setVisible(true);
        createBufferStrategy(2);
        logPanel.setBufferStrategy(getBufferStrategy());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public static void main(String []args)
    {
        FitnessLogFrame frame = new FitnessLogFrame();
        try
        {
            frame.initEvolution();
            frame.initFrame();
            new Thread(frame).start();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(5);
        }
    }
}
