/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *              Artificial Immune Systems                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.immune.core;

import java.util.LinkedList;
import java.util.List;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributePropertyFuzzy;
import org.shaman.datamodel.AttributePropertyFuzzyCategorical;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.random.Uniform;


// **********************************************************\
// *            Common Functions' Density Sampler           *
// **********************************************************/
public class FunctionSupplier extends MemorySupplier
{
   // Types of Functions
   public static final int TYPE_UNIT_SPHERE      = 0;
   public static final int TYPE_HOLLOW_SPHERE    = 1;
   public static final int TYPE_UNIT_CUBE        = 2;
   public static final int TYPE_RANDOM_BITSTRING = 10;
   public static final int TYPE_3D               = 20;

   private int    type;               // Type of function to generate
   private int    numberOfInstances;  // Number of instance to create
   private double par;                // Parameters (size of hollow / number of bits)

   private boolean goal;              // Include a goal attribute

   // **********************************************************\
   // *         Set the Function Source Parameters             *
   // **********************************************************/
   public void setParameters(int _type, int _numberOfInstances, double _par)
   {
     type              = _type;
     numberOfInstances = _numberOfInstances;
     par               = _par;
   }
   
   public void setGoal(boolean goal)
   {
       this.goal = goal;
   }
   
   // **********************************************************\
   // *                   Make some data in 3D                 *
   // **********************************************************/
   public InstanceSetMemory createHollowCircle(int numpart, double hole) throws LearnerException
   {
       int               i;
       InstanceSetMemory im;
       List              inlist;
       DoubleMatrix1D    innow;
       DataModel         dm;
       double            x,y,z,g;
       double            norm;
       
       try
       {
           dm     = getOutputDataModel(0);
           inlist = new LinkedList();
           for (i=0; i<numpart; i++)
           {
               innow = ((DataModelDouble)dm).createDefaultVector();
               
               x = Uniform.staticNextDoubleFromTo(-1,1);
               y = Uniform.staticNextDoubleFromTo(-1,1);
               z = Uniform.staticNextDoubleFromTo(-1,1);
               
               if (Uniform.staticNextBoolean())
               {
                   y    = 0.5;
                   norm = Math.sqrt(x*x + z*z);
                   
                   if ((norm <= 1) && (norm >= hole)) g = 1.0;
                   else                               g = 0.0;
               }
               else g = 0.0;
               
               innow.setQuick(0, x);
               innow.setQuick(1, y);
               innow.setQuick(2, z);
               innow.setQuick(3, g);
               
               inlist.add(innow);
           }
       }
       catch(DataModelException ex)  { throw new LearnerException(ex); }
       catch(ConfigException ex)     { throw new LearnerException(ex); }
       
       im = new InstanceSetMemory();
       im.create(inlist, dm);
       
       return(im);
   }
   
   public InstanceSetMemory createHollowSphere(int numint, double radius) throws LearnerException
   {
       int               i,j,k;
       double            countsphere;
       InstanceSetMemory im;
       List              inlist;
       DoubleMatrix1D    innow;
       DataModelDouble   dm;
       double            intlen;
       double            x,y,z,g;
       double            norm;
       
       try
       {
           countsphere = 0;
           dm          = (DataModelDouble)getOutputDataModel(0);
           intlen      = 2.0/numint;
           inlist      = new LinkedList();
           i = 0; j = 0; k= 0;
           for (i=0; i<numint; i++)
           {
               for (j=0; j<numint; j++)
               {
                   for (k=0; k<numint; k++)
                   {
                       x = -1.0+(i*intlen);
                       y = -1.0+(j*intlen);
                       z = -1.0+(k*intlen);
                       
                       norm = Math.sqrt(x*x + y*y + z*z);
                       
                       if (norm <= radius)
                       {
                           g = 1.0;
                           countsphere++;
                       } 
                       else g = 0.0;
                       
                       innow = (DoubleMatrix1D)dm.createDefaultVector();
                       innow.setQuick(0, x);
                       innow.setQuick(1, y);
                       innow.setQuick(2, z);
                       innow.setQuick(3, g);
                       
                       inlist.add(innow);
                   }
               }
           }
           countsphere /= (i*j*k);
           System.out.println("Sphere Part = "+countsphere);
       }
       catch(DataModelException ex) { throw new LearnerException(ex); }
       catch(ConfigException ex)    { throw new LearnerException(ex); }
       
       im = new InstanceSetMemory();
       im.create(inlist, dm);
       
       return(im);
   }

   // **********************************************************\
   // *            Random Binary Noise Data Source             *
   // **********************************************************/
   private DoubleMatrix1D []generateBitString()
   {
       int            i,j,len;
       DoubleMatrix1D []iran;
       DoubleMatrix1D inow;
       double         bnow;
       
       // Make random bit-strings...
       len  = (int)par;
       iran = new DoubleMatrix1D[numberOfInstances];
       if (!this.goal)
       {
           
           for (i=0; i<numberOfInstances; i++)
           {
               inow = DoubleFactory1D.dense.make(len);
               for (j=0; j<len; j++)
               {
                   if (Uniform.staticNextDouble() <= 0.5) bnow = 0.0;
                   else                                   bnow = 1.0;
                   inow.setQuick(j, bnow);
               }
               iran[i] = inow;
           }
       }
       else
       {
           // If this is a supervised data-set, modify half of string into second class...
           int numbits, numchange, pos;
           numbits   = len;
           numchange = 3;
           
           for (i=0; i<numberOfInstances/2; i++)
           {
               inow = DoubleFactory1D.dense.make(len);
               inow.setQuick(0, 0.0);
               for (j=1; j<len; j++)
               {
                   if (Uniform.staticNextDouble() <= 0.5) bnow = 0.0;
                   else                                   bnow = 1.0;
                   inow.setQuick(j, bnow);
               }
               iran[i] = inow;
               
               inow = inow.copy();
               inow.setQuick(0, 1.0);
               pos  = Uniform.staticNextIntFromTo(1, numbits-numchange-1);
               for (j=pos; j<pos+numchange; j++) inow.setQuick(j, 1.0-inow.getQuick(j));
               iran[i+(numberOfInstances/2)] = inow;
           }
       }
       
       return(iran);
   }

   private DataModelDouble makeBitStringDataModel() throws DataModelException
   {
       DataModelDouble                   dm;
       int                               i, len;
       AttributeDouble                   atdo;
       AttributePropertyFuzzyCategorical atpfuz;
       double         []legdo = new double[]{0.0,1.0};
       
       len = (int)par;
       dm  = new DataModelDouble("BitString DataModel", len);
       for (i=0; i<len; i++)
       {
           atdo = dm.getAttributeDouble(i);
           atdo.initAsSymbolCategorical(legdo);
           atpfuz = new AttributePropertyFuzzyCategorical(atdo, AttributePropertyFuzzyCategorical.TYPE_CRISP);
           atpfuz.setThreshold(1.0);
           atdo.addProperty(AttributePropertyFuzzy.PROPERTY_FUZZY, atpfuz);
           atdo.setIsActive(true);
       }
       
       if (this.goal)
       {
           atdo = dm.getAttributeDouble(0);
           atdo.setIsActive(false);
           atdo.setValuesAsGoal();
           dm.getLearningProperty().setGoal(0);
       }
       
       return(dm);
   }
   
   private DataModelDouble make3DDataModel() throws DataModelException
   {
       DataModelDouble    dm;
       AttributeDouble    atdo;
       int                i;
       double           []legdo = new double[]{0.0,1.0};
       
       dm = new DataModelDouble("3D Model", 4);
       for (i=0; i<3; i++)
       {
           atdo = new AttributeDouble();
           atdo.initAsNumberContinuous(new double[]{-1.0,1.0});
           atdo.setIsActive(true);
           dm.setAttribute(i, atdo);
           
           if (i==0) atdo.setName("x");
           if (i==1) atdo.setName("y");
           if (i==2) atdo.setName("z");
       }
       atdo = dm.getAttributeDouble(i);
       atdo.initAsSymbolCategorical(legdo);
       atdo.setValuesAsGoal();
       atdo.setIsActive(false);
       dm.getLearningProperty().setGoal(3);
       
       return(dm);
   }

   // **********************************************************\
   // *             Generate the function samples              *
   // **********************************************************/
   public void init() throws ConfigException
   {
       DataModel        dmout;
       DoubleMatrix1D []dat;
       
       // Generate the correct output datamodel.
       dmout = null;
       if      (type == TYPE_RANDOM_BITSTRING) dmout = makeBitStringDataModel();
       else if (type == TYPE_3D)               dmout = make3DDataModel();
       setOutputDataModel(0, dmout);
       
       dat = null;
       if (type == TYPE_RANDOM_BITSTRING)
       {
           dat = generateBitString();
           
           // Initialize the output queues for this data set.
           setDoubleInstances(dat);
       }
       else ; // The other ones please.
   }
}
