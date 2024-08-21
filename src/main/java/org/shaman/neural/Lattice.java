/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/
package org.shaman.neural;


/**
 * <h2>Low Dimensional Lattice</h2>
 * Used to attach a low-dimensional topology to a layer of neurons.
 * Can be used by Topographic Map algorithms. (e.g. SOM, KMER)
 */

// **********************************************************\
// *           Low Dimensional Lattice for Neurons          *
// **********************************************************/
public class Lattice
{
    /** 1 dimensional (linear) lattice */
    public static final int DIMENSION_1D = 1;
    /** 2 dimensional (rectangular) lattice */
    public static final int DIMENSION_2D = 2;
    /** 3 dimensional (block) lattice */
    public static final int DIMENSION_3D = 3;
    
    /** Gaussian neighborhood function (e.g. Eq (5.14) p141 in Van Hulle's book.) */
    public static final int NEIGHBORHOOD_GAUSSIAN = 0;
    
    private int    neighborhood;       // Type of neighborhood function
    private double tmax;               // Number of epochs until end
    private double sigma0;             // Neighborhood range during first epoch
    private int    dimension;          // Number of dimensions (<=3)
    private int    []size;             // int[dimension] containing the number of neurons per dimension
    
    // **********************************************************\
    // *            Neighborhoud Function Initialization        *
    // **********************************************************/
    private int []libuf;
    private int []ljbuf;
    
    /**
     * Convert the given ind to lattice coordinates.
     * @param ind The index in the lattice
     * @param l The returned coordinate in the lattice corresponding to the index
     */
    protected void indexToLattice(int ind, double []l)
    {
        if      (dimension == 1) l[0] = ind;
        else if (dimension == 2)
        {
            l[1] = ind / size[1];
            l[0] = ind % size[1];
        }
        else if (dimension == 3)
        {
            l[2] = ind / (size[1]*size[0]); ind -= l[2]*(size[1]*size[0]);
            l[1] = ind / size[1];
            l[0] = ind % size[1];
        }
    }
    
    // Same as above but for integer values....
    private void indexToLattice(int ind, int []l)
    {
        if      (dimension == 1) l[0] = ind;
        else if (dimension == 2)
        {
            l[1] = ind / size[1];
            l[0] = ind % size[1];
        }
        else if (dimension == 3)
        {
            l[2] = ind / (size[1]*size[0]); ind -= l[2]*(size[1]*size[0]);
            l[1] = ind / size[1];
            l[0] = ind % size[1];
        }
    }
    
    /**
     * Get the neighborhood function for 2 neuron's at the given time
     * @param ii Index of the first neuron
     * @param ij Index of the second neuron
     * @param t current time.
     * @return The neighborhood function of the neurons.
     */
    protected double neighborhood(int ii, int ij, int t)
    {
        if (libuf == null) libuf = new int[dimension];
        if (ljbuf == null) ljbuf = new int[dimension];
        
        indexToLattice(ii, libuf);
        indexToLattice(ij, ljbuf);
        
        return(neighborhood(libuf, ljbuf, t));
    }
    
    /**
     * Get the neighborhood function for 2 neuron's at the given time
     * @param li Lattice coordinates of the first neuron
     * @param lj Lattice coordinates of the second neuron
     * @param t current time.
     * @return The neighborhood function of the neurons.
     */
    protected double neighborhood(int []li, int []lj, int t)
    {
        int    i;
        double dij;
        double sigmat;
        double nei;
        
        // Euclidian Distance in the lattice between neuron i and j.
        dij = 0; for (i=0; i<li.length; i++) dij += (li[i]-lj[i])*(li[i]-lj[i]);
        
        // Kernal width at time t
        sigmat = sigma0*Math.exp(-2*sigma0*(t/tmax));
        
        // Neigborhood function
        if (neighborhood == NEIGHBORHOOD_GAUSSIAN) nei = Math.exp(-dij/(2*sigmat*sigmat));
        else nei = 0;
        
        return(nei);
    }
    
    /**
     * Set the parameters of the neighborhood function.
     * Make an educated guess about the the value of sigma0 (the range spanned by the neighborhood function at time 0).
     * @param _neighborhood The kind of neighborhood to use.
     * @param _tmax The maximum number of time steps
     */
    public void setNeighborhood(int _neighborhood, double _tmax)
    {
        int i;
        
        sigma0 = 0;
        for (i=0; i<size.length; i++) sigma0 += size[i];
        sigma0 /= size.length;
        
        setNeighborhood(_neighborhood, _tmax, sigma0);
    }
    
    /**
     * Set the parameters of the neighborhood function.
     * @param _neighborhood The kind of neighborhood to use.
     * @param _tmax The maximum number of time steps
     * @param _sigma0 The range spanned by the neighborhood function at time 0
     */
    public void setNeighborhood(int _neighborhood, double _tmax, double _sigma0)
    {
        neighborhood = _neighborhood;
        tmax         = _tmax;
        sigma0       = _sigma0;
    }
    
    // **********************************************************\
    // *                   Lattice Construction                 *
    // **********************************************************/
    /**
     * Make a 3 dimensional lattice (block) with the given dimensions.
     * @param s1 Number of neurons in dimension 1
     * @param s2 Number of neurons in dimension 2
     * @param s3 Number of neurons in dimension 3
     */
    public Lattice(int s1, int s2, int s3)
    {
        dimension = DIMENSION_3D;
        size      = new int[3];
        size[0]   = s1;
        size[1]   = s2;
        size[2]   = s3;
    }
    
    /**
     * Make a 2 dimensional lattice (rectangular) with the given dimensions.
     * @param s1 Number of neurons in dimension 1
     * @param s2 Number of neurons in dimension 2
     */
    public Lattice(int s1, int s2)
    {
        dimension = DIMENSION_2D;
        size      = new int[2];
        size[0]   = s1;
        size[1]   = s2;
    }
    
    /**
     * Make a 1 dimensional lattice (linear) with the given dimension.
     * @param s1 Number of neurons.
     */
    public Lattice(int s1)
    {
        dimension = DIMENSION_1D;
        size      = new int[1];
        size[0]   = s1;
    }
}