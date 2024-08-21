/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                   Utility Methods                     *
 *                                                       *
\*********************************************************/
package org.shaman.util;

/**
 * <h2>Math Utilities</h2>
 */
public class MathUtil
{
    private static final double TANH_COEF[] = {
            -.25828756643634710,
            -.11836106330053497,
            .009869442648006398,
            -.000835798662344582,
            .000070904321198943,
            -.000006016424318120,
            .000000510524190800,
            -.000000043320729077,
            .000000003675999055,
            -.000000000311928496,
            .000000000026468828,
            -.000000000002246023,
            .000000000000190587,
            -.000000000000016172,
            .000000000000001372,
            -.000000000000000116,
            .000000000000000009};
    
    public static double tanh(double x)
    {
        double  ans, y;
        y = Math.abs(x);
        
        if (Double.isNaN(x)) {
            ans = Double.NaN;
        } else if (y < 1.82501e-08) {
            // 1.82501e-08 = Math.sqrt(3.0*EPSILON_SMALL)
            ans = x;
        } else if (y <= 1.0) {          
            ans = x*(1.0+csevl(2.0*x*x-1.0,TANH_COEF));
        } else if (y < 7.977294885) {
            // 7.977294885 = -0.5*Math.log(EPSILON_SMALL)
            y = Math.exp(y);
            ans = sign((y-1.0/y)/(y+1.0/y),x);
        } else {
            ans = sign(1.0,x);
        }
        return ans;
    }
    
    private static double csevl(double x, double coef[])
    {
        double  b0, b1, b2, twox;
        int     i;
        b1 = 0.0;
        b0 = 0.0;
        b2 = 0.0;
        twox = 2.0*x;
        for (i = coef.length-1;  i >= 0;  i--) {
            b2 = b1;
            b1 = b0;
            b0 = twox*b1 - b2 + coef[i];
        }
        return 0.5*(b0-b2);
    }
    
    private static double sign(double x, double y)
    {
        double abs_x = ((x < 0) ? -x : x);
        return (y < 0.0) ? -abs_x : abs_x;
    }
}
