package org.shaman.spatial;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class CircleDataGenerator
{
    //public static final double INTERIOR_RADIUS = 0.797884; // Half of point in unit square fall inside circle with this radius
    public static double INTERIOR_RADIUS = 0.4;
    int dimensionForCircle;
    
    public Instances generateInstances(int size, int dimension)
    {
        double         x,y,r;
        DenseInstance  instance;
        Instances data;
        Attribute atclass;
        ArrayList<Attribute> atts;
        List<String> classes;
        dimensionForCircle = dimension;
        
        atts = new ArrayList<Attribute>();
        for(int i=0; i< dimension; i++)
            atts.add(new Attribute("x" +i));
        classes = new ArrayList<String>();
        classes.add("1");
        classes.add("-1");
        atclass = new Attribute("interior", classes);
        atts.add(atclass);
        data = new Instances("Circle", atts, size);
        data.setClassIndex(dimensionForCircle);
        
        for(int i=0; i<size; i++)
        {
            double sumOfSquares=0;
             //create dimension +1
             instance = new DenseInstance(dimension+1);
             instance.setDataset(data);
             for(int dim=0; dim< dimension; dim++){
                 double val = Math.random()*(Math.random()<0.5?1:-1);
                 instance.setValue(dim, val);
                 sumOfSquares = val*val + sumOfSquares;
             }
             r = Math.sqrt(sumOfSquares);
            instance.setValue(dimensionForCircle, (r<=INTERIOR_RADIUS?"1":"-1"));
            data.add(instance);
        }
        
        return(data);
    }
}
