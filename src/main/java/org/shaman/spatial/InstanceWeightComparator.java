package org.shaman.spatial;

import weka.core.Instance;

import java.util.Comparator;

public class InstanceWeightComparator implements Comparator
{
    // Order from highest to lowest Instance weight. Break ties with the hashCode of the Instances.
    public int compare(Object arg0, Object arg1)
    {
        Instance a = (Instance) arg0;
        Instance b = (Instance) arg1;
        if      (b.weight() > a.weight()) return (1);
        else if (b.weight() < a.weight()) return (-1);
        else                              return new Integer(b.hashCode()).compareTo(new Integer(a.hashCode()));
    }
}
