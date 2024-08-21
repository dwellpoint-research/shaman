/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  For K-Medoids clustering                             *
 *                                                       *
 *  Copyright (c) 2002-5 Shaman Research                 *
\*********************************************************/

package org.shaman.clustering;

/**
 * Title:        Mathematics on Vectors<br/>
 * Description: Operations on vectors, implemented as arrays<br/>
 * @author Isabel Van Dooren
 * @version 2.0
 */

public final class VectorMath
{

// *************************** COVARIANCE **************************************/
/**<br/>Covariance between two vectors is calculated*/
public static float covariance(float[] vector1, float[] vector2)
{
   int length = vector1.length;
   if(length != vector2.length)
   {
      System.out.println("Two vectors do not have the same number of elements");
      return 0;
   }

   float result = 0;
   int notmissing = 0;
   for(int i=0 ; i<length ; i++)
   {
      if(!( Float.isNaN(vector1[i]) || Float.isNaN(vector2[i]) ))
      {
         result += vector1[i]*vector2[i];
         notmissing++;
      }
   }
   if(notmissing<2)
   {
      System.out.println("Two vectors don't have features in common");
      return 0;
   }
   return result/(notmissing-1);
}



// **************************** MIN ELEMENT *************************************/
/**<br/>The minimum element in a vector is returned, the element position is written in a intW object*/
public static double minElement(double[] vector, int []minpos)
{
   double min = Double.POSITIVE_INFINITY;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
       if (vector[i] < min)
       {
          min = vector[i];
          minpos[0] = i;
        }
    }
    return min;
}

/**<br/>The minimum element in a vector is returned, the element position is written in a intW object*/
public static byte minElement(byte[] vector, int []minpos)
{
   byte min = Byte.MAX_VALUE;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
       if (vector[i] < min)
       {
          min = vector[i];
          minpos[0] = i;
        }
    }
    return min;
}

/**<br/>The minimum element in a vector is returned*/
public static double minElement(double[] vector)
{
   double min = Double.POSITIVE_INFINITY;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
       if (vector[i] < min)
       {
          min = vector[i];
        }
    }
    return min;
}
/**<br/>The minimum element in a vector is returned*/
public static byte minElement(byte[] vector)
{
   byte min = Byte.MAX_VALUE;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
       if (vector[i] < min)
       {
          min = vector[i];
        }
    }
    return min;
}

/**<br/>The maximum element in a vector is returned*/
public static double maxElement(double[] vector)
{
   double max = Double.NEGATIVE_INFINITY;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
       if (vector[i] > max)
       {
          max = vector[i];
        }
    }
    return max;
}

// **************************** IS IN *******************************************/
/**<br/>Is a certain value 'val' in the vector?*/
public static boolean isIn(double[] vector, double val)
{
   boolean resul = false;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
      if (vector[i] == val)
      {
         resul = true;
         break;
      }
    }
    return resul;
}
/**<br/>Is a certain value 'val' in the vector?*/
public static boolean isIn(int[] vector, int val)
{
   boolean resul = false;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
      if (vector[i] == val)
      {
         resul = true;
         break;
      }
    }
    return resul;
}

/**<br/>Is a certain value 'val' in the vector?*/
public static boolean isIn(int[] vector, int val, int []pos)
{
   boolean resul = false;
   int length = vector.length;
   int i;
   for(i=0 ; i<length ; i++)
   {
      if (vector[i] == val)
      {
         resul  = true;
         pos[0] = i;
         break;
      }
    }
    return resul;
}
// ******************************* SELECT ELEMENTS ******************************/
/**<br/>Elements with indices specified are selected and copied (in order) in another vector*/
public static double[] selectElements(double[] vector , int[] indices)
{
   int length = indices.length;
   double[] resul = new double[length];
   int i;
   for(i=0 ; i<length ; i++)
   {
      resul[i] = vector[indices[i]];
   }
   return resul;
}
/**<br/>Elements with indices specified are selected and copied (in order) in another vector*/
public static byte[] selectElements(byte[] vector , int[] indices)
{
   int length = indices.length;
   byte[] resul = new byte[length];
   int i;
   for(i=0 ; i<length ; i++)
   {
      resul[i] = vector[indices[i]];
   }
   return resul;
}

/**<br/>The first 'length' elements are copied (in order) in another vector*/
public static double[] selectElements(double[] vector , int length)
{
   double[] resul = new double[length];
   int i;
   for(i=0 ; i<length ; i++)
   {
      resul[i] = vector[i];
   }
   return resul;
}
/**<br/>The first 'length' elements are copied (in order) in another vector*/
public static int[] selectElements(int[] vector , int length)
{
   int[] resul = new int[length];
   int i;
   for(i=0 ; i<length ; i++)
   {
      resul[i] = vector[i];
   }
   return resul;
}
/**<br/>The elements with index start until stop are copied (in order) in another vector*/
public static double[] selectElements(double[] vector , int start, int stop)
{
   double[] resul = new double[stop-start+1];
   int i;
   for(i=0 ; i<stop-start ; i++)
   {
      resul[i] = vector[start+i];
   }
   return resul;
}
/**<br/>The elements with index start until stop are copied (in order) in another vector*/
public static byte[] selectElements(byte[] vector , int start, int stop)
{
   byte[] resul = new byte[stop-start+1];
   int i;
   for(i=0 ; i<stop-start ; i++)
   {
      resul[i] = vector[start+i];
   }
   return resul;
}
// ******************printVector***************************************************/
/**<br/>Prints the data in a vector*/
public static void printVector(double[] vector)
{
   int length = vector.length;
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the first 'length' elements in a vector*/
public static void printVector(double[] vector, int length)
{
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the data in a vector*/
public static void printVector(byte[] vector)
{
   int length = vector.length;
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the first 'length' elements in a vector*/
public static void printVector(byte[] vector, int length)
{
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the data in a vector*/
public static void printVector(int[] vector)
{
   int length = vector.length;
   System.out.print("[");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print("  " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the first 'length' elements in a vector*/
public static void printVector(int[] vector, int length)
{
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the data in a vector*/
public static void printVector(float[] vector)
{
   int length = vector.length;
   System.out.print("[");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print("  " + vector[i]);
   }
   System.out.println(" ]");
}
/**<br/>Prints the first 'length' elements in a vector*/
public static void printVector(float[] vector, int length)
{
   System.out.print("[ ");
   int i;
   for(i=0 ; i<length ; i++)
   {
     System.out.print(" " + vector[i]);
   }
   System.out.println(" ]");
}
}