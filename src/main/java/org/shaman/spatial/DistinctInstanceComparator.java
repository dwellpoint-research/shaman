package org.shaman.spatial;

import weka.core.Instance;

import java.util.Comparator;


public class DistinctInstanceComparator implements Comparator
{
    public int compare(Object arg0, Object arg1)
    {
        if ((arg0 instanceof Instance) && (arg1 instanceof Instance))
        {
            Instance a = (Instance) arg0;
            Instance b = (Instance) arg1;

            for(int i=0; i<a.numAttributes(); i++)
                if (a.value(i) != b.value(i)) return new Double(a.value(i)).compareTo(new Double(b.value(i)));

            return 0;
        }
        else return 0;
    }
}
