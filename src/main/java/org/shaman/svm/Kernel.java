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
package org.shaman.svm;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;

/**
 * <h2>Mercer Kernel Functions</h2>
 * Kernels for use in kernel-based machine-learning
 * methods. e.g. Support Vector Machines, kernel PCA
 */

// **********************************************************\
// *       Collection of Common Mercer Kernel Functions     *
// **********************************************************/
public class Kernel
{
   /** Linear Kernel. K(x,z) = <x.z> */
   public static final int KERNEL_LINEAR     = 1;
   /** Gaussian Kernel. Parameter 0 = sigma.  K(x,z) = exp(-|x-z| / sigma) */
   public static final int KERNEL_GAUSSIAN   = 2;
   /** Polynomial Kernel. Parameter 0 = c, 1 = d.  K(x,z) = (<x.z> + c)^d */
   public static final int KERNEL_POLYNOMIAL = 3;

   private KernelFunction func;
   private double []par;

   // **********************************************************\
   // *                 Kernel Function Evaluation             *
   // **********************************************************/
   /**
    * Give the Kernel Function applied on the 2 input vectors.
    * @param x Vector 1
    * @param w Vector 2
    * @return The Kernel function's output.
    */
   public double apply(DoubleMatrix1D x, DoubleMatrix1D z)
   {
     return(func.apply(x, z));
   }

   // **********************************************************\
   // *                    Kernel Construction                 *
   // **********************************************************/
   /**
    * Make a Mercer Kernel of the given type and parameters.
    * @param _type The type of kernel
    * @param _par The kernel parameters
    */
   public Kernel(int _type, double []_par)
   {
     par = _par;
     if      (_type == KERNEL_LINEAR)     func = new KernelLinear();
     else if (_type == KERNEL_GAUSSIAN)   func = new KernelGaussian();
     else if (_type == KERNEL_POLYNOMIAL) func = new KernelPoly();
   }

   // **********************************************************\
   // *                      Common Kernels                    *
   // **********************************************************/
   /**
    * <h2>Mercer Kernel Function</h2>
    */
   interface KernelFunction
   {
     double apply(DoubleMatrix1D x, DoubleMatrix1D z);
   }

  /**
    * <h3>Linear Kernel Function</h3>
    */
   class KernelLinear implements KernelFunction
   {
     public double apply(DoubleMatrix1D x, DoubleMatrix1D z)
     {
       // K(x,z) = <x.z>
       return(x.zDotProduct(z));
     }
   }

  /**
    * <h3>Polynomial Kernel Function</h3>
    */
   class KernelPoly implements KernelFunction
   {
     public double apply(DoubleMatrix1D x, DoubleMatrix1D z)
     {
       // K(x,z) = (<x.z> + c)^d
       return(Math.pow(x.zDotProduct(z) + par[0], par[1]));
     }
   }

   /**
    * <h3>Gaussian Kernel Function</h3>
    */
   class KernelGaussian implements KernelFunction
   {
     public double apply(DoubleMatrix1D x, DoubleMatrix1D z)
     {
       // K(x,z) = exp(-|x-z| / sigma)
       double xn      = Algebra.DEFAULT.norm2(x);
       double zn      = Algebra.DEFAULT.norm2(z);
       double dotxz   = x.zDotProduct(z);
       double sigmasq = par[0];
       double k;

       k = Math.exp(-xn/sigmasq)*Math.exp(-zn/sigmasq)*Math.exp(2*dotxz/sigmasq);

       return(k);
     }
   }
}
