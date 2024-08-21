/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2007 Shaman Research                   *
\*********************************************************/
package org.shaman.art;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class ConjugateGradient
{
    public static final double TOL      = 0.001;
    public static final int    MAX_ITER = 1000;

    /**
     * Conjugate Gradient(CG).
     * Solves the symmetric positive definite linear system A x = b.
     * 
     * @param A Matrix A
     * @param x Solution Vector X
     * @param b Vector b
     * @return false if the solution did not converge in MAX_ITER iterations.
     */
    public boolean cg(DoubleMatrix2D A, DoubleMatrix1D x, DoubleMatrix1D b) 
    {
        int vectorSize = x.size();
        int i =0;

        double rho, rho_1, alpha, beta, rSquared, r0; 
        DoubleMatrix1D p, q, r, z;
        
        p = DoubleFactory1D.dense.make(vectorSize);
        q = DoubleFactory1D.dense.make(vectorSize);
        r = DoubleFactory1D.dense.make(vectorSize);
        z = DoubleFactory1D.dense.make(vectorSize);

        rho      = 0;
        rho_1    = 0;
        alpha    = 0;
        beta     = 0;
        r0       = 0;
        rSquared = 0;
        
        // p = Ax where A is a symmetric matrix
        multSymm(A, x, p);

        // r = b - p = b - Ax
        addVecScal(b, p, (double)-1, r);

        //r0 = b.*b
        r0 = dot_prod(b, b);

        //rSquared = r.*r;
        rSquared = dot_prod(r, r);

        // until convergence or max number of iterations
        while (i<MAX_ITER && (rSquared > TOL*TOL*r0))
        { 
            //solve A z = r
            solve_Jacobi(A, r, z);

            // rho = r.*z
            rho = dot_prod(r, z);
            if (i == 0)
            {
                // p = z for the first iteration
                copy (z, p);          
            }
            else 
            {
                // p = z + beta * p
                beta = rho / rho_1;
                addVecScal(z, p, beta, p);   
            }

            // q = A*p ( p = z in the first iteration)
            multSymm(A, p, q);        

            //alpha = rho/(p.*q)
            alpha = rho / dot_prod(p, q);

            // x = x + alpha * p
            addVecScal(x, p, alpha, x);
            
            //r = r -alpha * q
            addVecScal(r, q, -alpha, r);

            rho_1    = rho;
            rSquared = dot_prod(r, r);

            ++i;
        }

        if (i == MAX_ITER) return(false);
        else               return(true);
    }


    private DoubleMatrix1D scaled(DoubleMatrix1D v1, double scalar)
    {
        int M = v1.size();

        DoubleMatrix1D tempVec = DoubleFactory1D.dense.make(M);
        int  i = 0;
        for (i = 0; i<M; ++i)
            tempVec.setQuick(i, v1.getQuick(i) * scalar);

        return tempVec;
    }

    private void addVecScal(DoubleMatrix1D v1, DoubleMatrix1D v2, double scalar, DoubleMatrix1D result)
    {
        // result = v1 + scalar * v2
        int M = v1.size();

        int i = 0;
        for (i = 0; i<M; ++i)
            result.setQuick(i,  v1.getQuick(i) + v2.getQuick(i)*scalar);
    }

    private double dot_prod(DoubleMatrix1D v1, DoubleMatrix1D v2)
    {
        //inner product
        int M = v1.size();
        double result = 0;
        int i = 0;

        for (i = 0; i<M; ++i)
            result += v1.getQuick(i)*v2.getQuick(i);

        return result;
    }

    private void multSymm(DoubleMatrix2D A, DoubleMatrix1D r, DoubleMatrix1D result)
    {
        //result = A*r where A is upper-symmetric matrix
        int M = A.columns();
        int i = 0;
        int j = 0;
        double sum;

        for (i=0; i<M; i++)
        {
            sum = 0;

            //elements under the diagonal
            for (j=0; j<i; ++j)
                sum = sum + A.getQuick(j, i) * r.getQuick(j);

            //elements over the diagonal
            for (j=i; j<M; j++)
                sum = sum +  A.getQuick(i, j) * r.getQuick(j);

            result.setQuick(i, sum); 
        }
    }

    private void copy(DoubleMatrix1D v, DoubleMatrix1D result)
    {
        int M = v.size();
        int i = 0;

        for (i=0; i<M; i++)
            result.setQuick(i, v.getQuick(i)); 
    }

    private void solve_Jacobi(DoubleMatrix2D A, DoubleMatrix1D r, DoubleMatrix1D z)
    {
        int M = A.rows();
        int i = 0;

        for (i =0; i<M; ++i)
        {
            // A[i][i] should be always !=0 however we will check later on
            z.setQuick(i, r.getQuick(i) / A.getQuick(i, i));
        }
    }
}
