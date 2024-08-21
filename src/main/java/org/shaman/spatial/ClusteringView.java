package org.shaman.spatial;

import weka.core.Instance;
import weka.core.Instances;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class ClusteringView extends JPanel
{
    private static Color []CLASS_COLORS = new Color[]{Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.MAGENTA};
    //new Color[]{new Color(0x76777A), new Color(0x6E5778), new Color(0x217CED)};

    private Instances dataSet;
    private String epochSummary;
    private int idx1, idx2;

    public void buildLayout()
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
    }

    public void drawInstances()
    {
        if (this.dataSet == null) return;

        // Just pick random colors when there's too many clusters for the pre-defined constant colors.
        if (this.dataSet.classAttribute().numValues() > CLASS_COLORS.length)
        {
            CLASS_COLORS = new Color[this.dataSet.classAttribute().numValues()];
            for(int i=0; i<CLASS_COLORS.length; i++) CLASS_COLORS[i] = generateRandomColor(Color.WHITE);
        }

        Graphics gr;
        int w,h;
        int []idx;
        double val;
        double []min,max;

        // Find bounding box
        idx = new int[]{this.idx1, this.idx2};
        min = new double[]{Integer.MAX_VALUE, Integer.MAX_VALUE};
        max = new double[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
        for(Instance instance: this.dataSet)
        {
            for(int i=0; i<idx.length; i++)
            {
                val = instance.value(idx[i]);
                if (val < min[i]) min[i] = val;
                if (val > max[i]) max[i] = val;
            }
        }

        boolean hasClass;
        double px, py;
        int    cx, cy;
        int    cluster;

        // Clear window
        gr  = getGraphics();
        w = getWidth();
        h = getHeight();
        gr.setColor(Color.BLACK);
        gr.fillRect(0, 0, w, h);

        // Print summary
        if (this.epochSummary != null)
        {
            gr.setColor(Color.WHITE);
            gr.drawString(this.epochSummary, 0, 15);
        }

        // Draw instances as dots colored according to class, scaled to fit the bounding box
        hasClass = dataSet.classIndex() >= 0;
        gr.setColor(Color.GRAY);
        for(Instance instance: this.dataSet)
        {
            px = (instance.value(idx[0])-min[0])/(max[0]-min[0]);
            py = (instance.value(idx[1])-min[1])/(max[1]-min[1]);
            cx = (int)(px*w);
            cy = (int)(py*h);
            if (hasClass)
            {
                // Instance present in one of the nodes' dataset: colored dot
                cluster = (int)instance.classValue();
                if (cluster >= 0)
                {
                    gr.setColor(CLASS_COLORS[cluster]);
                    gr.fillOval(cx, cy, 2, 2);
                }
                // Cluster Centroid: White square
                else  if (cluster == -1)
                {
                    gr.setColor(Color.WHITE);
                    gr.fillRect(cx-2, cy-2, 4, 4);
                }
                // Instance in total dataset but not in any of the nodes: dark gray dot
                else if (cluster == -2)
                {
                    gr.setColor(Color.DARK_GRAY);
                    gr.fillOval(cx, cy, 2, 2);
                }
                // Cluster centroid of k-means trained on total dataset.
                else  if (cluster == -3)
                {
                    gr.setColor(Color.DARK_GRAY);
                    gr.fillRect(cx-2, cy-2, 4, 4);
                }
            }
        }
    }

    private Color generateRandomColor(Color mix)
    {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        if (mix != null)
        {
            red = (red + mix.getRed()) / 2;
            green = (green + mix.getGreen()) / 2;
            blue = (blue + mix.getBlue()) / 2;
        }

        Color color = new Color(red, green, blue);
        return color;
    }

    public ClusteringView()
    {
        buildLayout();
    }

    public void setEpochSummary(String summary)
    {
        this.epochSummary = summary;
    }

    public void setDataSet(Instances instances)
    {
        this.dataSet = instances;
    }

    public void setDimensions(int idx1, int idx2)
    {
        this.idx1 = idx1;
        this.idx2 = idx2;
    }
}
