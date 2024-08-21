package org.shaman.evolution;

import org.shaman.util.gui.SeparatorPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.TreeMap;

/**
 * @author Johan Kaers
 */
public class FitnessLogPanel extends JPanel
{
    private BufferStrategy bufferStrategy;
    private Evolution evolution;
    private double maxFitness;
    private boolean maxLine;

    public void draw()
    {
        TreeMap<Integer, double []> fitnessBuffer;

        // Fetch (a copy) of the fitness history over the last 'bufferSize' generations.
        fitnessBuffer = this.evolution.getFitnessBufferCopy();
        if (fitnessBuffer == null) return;
        int bufferSize = this.evolution.getFitnessBufferSize();

        // Find the maximum fitness overall and per generation.
        double maxFitness = 0;
        double []maxFitnessGeneration = new double[fitnessBuffer.size()];
        int generation = 0;
        for (double[] generationFitness : fitnessBuffer.values())
        {
            maxFitnessGeneration[generation++] = generationFitness[generationFitness.length-1];
            if (generationFitness[generationFitness.length - 1] > maxFitness)
                maxFitness = generationFitness[generationFitness.length - 1];
        }
        // Override overall maximum fitness when it is known and given.
        if (this.maxFitness != 0) maxFitness = this.maxFitness;

        final int NUM_BINS = 100;
        double xStep = ((double)getWidth())/(bufferSize*2);
        if (!this.maxLine) xStep *= 2;
        double yStep = ((double)getHeight())/NUM_BINS;
        double fitBinSize = maxFitness / NUM_BINS;

        
        Graphics2D graphics;
        
        if (this.bufferStrategy != null)
             graphics = (Graphics2D)this.bufferStrategy.getDrawGraphics();
        else graphics = (Graphics2D)getGraphics();

        // Draw fitness density plot
        double xPos = 0;
        for(double []fitnessValues: fitnessBuffer.values())
        {
            double []bins = new double[NUM_BINS];
            double maxBin = 0;
            for(double value: fitnessValues)
            {
                int binIndex = (int)Math.floor(value/fitBinSize);
                if (binIndex == NUM_BINS) binIndex--;
                bins[binIndex]++;
                if (bins[binIndex] > maxBin) maxBin = bins[binIndex];
            }
            for(int i=0; i<bins.length; i++)
            {
                bins[i] = (bins[i] / maxBin)*255;
            }

            double yPos = 0;
            for(int j=bins.length-1; j>=0; j--)
            {
                double binValue = bins[j];
                int grey = (int)binValue;
                graphics.setColor(new Color(grey, grey, grey));
                graphics.fillRect((int)xPos, (int)yPos,  ((int)(xPos+xStep))-((int)xPos),  ((int)(yPos+yStep)-((int)yPos)));
                yPos += yStep;
            }

            xPos += xStep;
        }
        if (this.maxLine)
        {
            graphics.setColor(Color.BLACK);
            graphics.fillRect((int) xPos, 0, getWidth(), getHeight());

            graphics.setColor(Color.GRAY);

            // Draw maximum fitness plot
            xPos = getWidth() / 2.0;
            double yScale = getHeight() / maxFitness;
            double fitPrev = 0;
            for (double fitGeneration : maxFitnessGeneration)
            {
                int yBegin = getHeight() - (int) (fitPrev * yScale);
                int yEnd = getHeight() - (int) (fitGeneration * yScale);

                graphics.drawLine((int) xPos, yBegin, (int) (xPos + xStep), yEnd);

                xPos += xStep;
                fitPrev = fitGeneration;
            }


            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString("Generation " + this.evolution.getGeneration() + ". Maximum Fitness: " + this.evolution.getMaxFitness(), getWidth() / 2, 15);
        }
        
        if (this.bufferStrategy != null) this.bufferStrategy.show();
        
        // TODO: Add text display about: generation, fittest fitness, avg fitness
        // TODO: Visualization of neural net.
        
        /*
        for(int x=0; x<getWidth(); x+=10)
        {
            for(int y=0; y<getHeight(); y+=10)
            {
                int grey = Uniform.staticNextIntFromTo(0, 255);

                graphics.setColor(new Color(grey, grey, grey));
                graphics.fillRect(x, y, 10, 10);
            }
        }
        */
    }

    public void setMaximumFitness(double maxFitness)
    {
        this.maxFitness = maxFitness;
    }
    
    public void setMaximumLine(boolean maxLine)
    {
        this.maxLine = maxLine;
    }

    private void buildLayout()
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new SeparatorPanel(200));
        setBackground(Color.BLACK);
        //setDoubleBuffered(true);
    }

    public FitnessLogPanel(Evolution evolution)
    {
        this.evolution = evolution;
        this.maxLine = true;
        buildLayout();
    }
    
    public void setBufferStrategy(BufferStrategy bufferStrategy)
    {
        this.bufferStrategy = bufferStrategy;
    }
}
