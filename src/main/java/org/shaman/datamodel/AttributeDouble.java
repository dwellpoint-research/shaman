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
package org.shaman.datamodel;

import java.util.HashMap;
import java.util.Iterator;

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Primitive Data Attribute</h2>
 * The structure of an attribute containing primitive data. <br>
 * Implements all aspects of an Attribute for primitive <code>double</code> data.
 */

public class AttributeDouble extends Attribute
{
   /**
    * Default value. Used to initialize a data value of this Attribute in <code>
    */  
   private double defaultValue;          // Default value of this Attribute's data
   
   /**
    * Used to determine the goal class membership of a value.
    * A value belongs to goal class <code>i</code> if it is found in the
    * <code>Array</code> of <code>double</code>, <code>goalClass[i]</code>
    */
   private double  [][]goalClass;        // If  GOAL_CLASS then Array of double[] containing the goal classes

   /**
    *  <code>double</code> values that correspond to missing values.
    **/
   private double  []missingDouble;      // Double values denoting missing values

   /**
    * Array containing the distinct legal values of this attribute if unordered
    * or the legal intervals if an order is defined. Used by the default ObjectDoubleConverter.
    */
   private double  []doLegal;

   /**
    *  The unique double value corresponding to a missing value.
    */
   private double  missingAsDouble;      // The one missing value double

   // **********************************************************\
   // *                     Typical Usage                      *
   // **********************************************************/
   /**
    * Initialize as continuous Attribute spanning the whole range of <code>Double</code>. <br>
    * Installs the default <code>Order, Distance, ObjectDoubleConverter</code> implementations.
    */
   public void initAsNumberContinuous()
   {
       double []range = new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
       initAsNumberContinuous(range);
   }

   /**
    * Initialize continuous Attribute spanning the range between <code>range[0]</code> and <code>range[1]</code> <br>
    * Installs the default <code>OrderNatural, DistanceNumber, ConverterDefault</code> implementations.
    * Sets a <code>AttributePropertyContinuous</code> property.
    * @param range The range between which this attributes data must lie.
    */
   public void initAsNumberContinuous(double []range)
   {
       this.rawType         = Double.TYPE.getName();
       this.order           = new OrderNatural();       // Default Order, Distance and Convertion
       this.distance        = new DistanceNumber();
       this.converter       = new ConverterDefault();
       this.doLegal         = range;
       this.missingDouble   = new double[0];
       this.missingAsDouble = Double.NaN;
       this.defaultValue    = 0;
       addProperty(PROPERTY_CONTINUOUS, new AttributePropertyContinuous());
   }

   /**
    * Initialize as Categorical Attribute. <br>
    * Installs the default <code>ConverterDefault</code> implementation.
    * No order or distance is installed.
    * Sets a <code>AttributePropertyCategorical</code> property.
    * @param _legal The distinct legal double values of this attribute's data.
    */
   public void initAsSymbolCategorical(double []_legal)
   {
       this.rawType         = Double.TYPE.getName();
       this.order           = null;
       this.distance        = null;
       this.converter       = new ConverterDefault();
       this.doLegal         = _legal;
       this.missingDouble   = new double[0];
       this.missingAsDouble = Double.NaN;
       addProperty(PROPERTY_CATEGORICAL, new AttributePropertyCategorical());
   }

   /**
    * Initialize as free-form number. <br>
    * Only installs the ConverterDefault, no order, distance nor properties.
    */
   public void initAsFreeNumber()
   {
       this.rawType         = Double.TYPE.getName();
       this.order           = null;
       this.distance        = null;
       this.converter       = new ConverterDefault();
       this.doLegal         = null;
       this.missingDouble   = new double[0];
       this.missingAsDouble = Double.NaN;
       this.defaultValue    = 0;
   }

   // **********************************************************\
   // *                Categorical Value Access                *
   // **********************************************************/
   public int getNumberOfCategories() throws DataModelException
   {
       int nv;

       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL)) 
           throw new DataModelException("The attribute with name '"+this.name+"' is not a categorical attribute.");

       nv = doLegal.length;
       if (missingIs == MISSING_IS_VALUE) nv++;

       return(nv);
   }

   /**
    * Get the category of the given vales.
    * If the value is the missingAsDouble and missing values should be a seperate value, the extra
    * missing value category index is returned.
    * @param d The value
    * @return The category of the value
    * @throws DataModelException If the attribute is not categorical
    */
   public int getCategory(double d) throws DataModelException
   {
       int     i;
       boolean found;
       int     pos;

       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL)) 
           throw new DataModelException("The attribute with name '"+this.name+"' is not a categorical attribute.");

       if ((this.missingIs == MISSING_IS_VALUE) && (isMissingAsDouble(d))) pos = this.doLegal.length;
       else
       {
           found = false;
           pos   = -1;
           for (i=0; (i<this.doLegal.length) && (!found); i++)
           {
               if (this.doLegal[i] == d) { pos = i; found = true; }
           }
       }

       return(pos);
   }

   /**
    * Give the value of the category with the given index.
    * @param vi The category index.
    * @return The value of the given category. missingAsDouble if the missing value category is asked.
    * @throws DataModelException If the attribute is not categorical
    */
   public double getCategoryDouble(int vi) throws DataModelException
   {
       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL))
           throw new DataModelException("The attribute with name '"+this.name+"' is not a categorical attribute.");

       if   (vi < this.doLegal.length) return(this.doLegal[vi]);
       else                            return(missingAsDouble);
   }

   // **********************************************************\
   // *              Object <-> Double Converting              *
   // **********************************************************/
   public Object getObjectValue(double d, AttributeObject ato) throws DataModelException
   {
       if (this.converter != null) return(this.converter.toObject(d, this, ato));
       else                        return(null);
   }

   // **********************************************************\
   // *                   Other Things (tm)                    *
   // **********************************************************/
   public double  [][]getGoalClasses()     { return(this.goalClass); }
   public int     getNumberOfGoalClasses() { return(this.goalClass.length); }
   public void    setRawType(String _rawType) throws DataModelException
   {
       try
       {
           if (!Class.forName(_rawType).isPrimitive()) 
               throw new DataModelException("Cannot use non-primitive class '"+_rawType+"' as type of a primitive Attribute.");
       }
       catch(ClassNotFoundException ex) { throw new DataModelException(ex); }
   }

   protected Object getCategoricalObject(double d, AttributeObject ato) throws DataModelException
   {
       int    i;
       Object val;

       val = null;
       if (this.doLegal != null)
       {
           for (i=0; i<this.doLegal.length; i++)
           {
               if (d == this.doLegal[i]) val = ato.getLegalValues()[i];
           }
       }
       else throw new DataModelException("Cannot find the double "+d+" in the legal values table.");

       return(val);
   }
   
   public double getDefaultValue()                    { return(this.defaultValue); }
   public void   setDefaultValue(double defaultValue) { this.defaultValue = defaultValue; }

   // **********************************************************\
   // *                     Goal Definition                    *
   // **********************************************************/
   /**
    * Set the legal values of this attribute as the goal for a supervised classifier.
    * @throws DataModelException never
    */
   public void setValuesAsGoal() throws DataModelException
   {
       int i;

       // Set the legal values of this attribute as goal classes.
       this.isGoal   = true;
       this.goalType = GOAL_CLASS;
       this.goalClass = new double[this.doLegal.length][];
       for (i=0; i<this.goalClass.length; i++) this.goalClass[i] = new double[]{this.doLegal[i]};
   }

   /**
    * Set the specified groups (double []) of legal values
    * as the goal for a supervised classifier.
    * @param gr the array of double arrays containing the values belonging to each goal class.
    * @throws DataModelException never
    */
   public void setValueGroupsAsGoal(double [][]gr) throws DataModelException
   {
       // Group (not necessarily all) legal values into groups that form the goal classes.
       this.isGoal    = true;
       this.goalType  = GOAL_CLASS;
       this.goalClass = gr;
   }

   public int [][]getGoalClassIndices() throws DataModelException
   {
       int     i, j, k, pos;
       int     [][]gc;
       boolean found;

       if (this.goalType == GOAL_CLASS)
       {
           gc = new int[this.goalClass.length][];
           for (i=0; i<this.goalClass.length; i++)
           {
               gc[i] = new int[this.goalClass[i].length];
               for (j=0; j<this.goalClass[i].length; j++)
               {
                    found = false;
                    pos   = -1;
                    for (k=0; (k<this.doLegal.length) && (!found); k++)
                    {
                         if (doLegal[k] == goalClass[i][j]) { found = true; pos = k; }
                    }
                    if (found) gc[i][j] = pos;
                    else throw new DataModelException("Cannot find index in legal table of goal value "+this.goalClass[i][j]);
               }
           }
      }
      else throw new DataModelException("Cannot make goal class index table because the goal-type is wrong.");

      return(gc);
   }

   // **********************************************************\
   // *        Attribute Fit and Instance Construction         *
   // **********************************************************/
   public void    setMissingValues(double []mv)  { this.missingDouble = mv; }
   public void    setLegalValues(double []legal) { this.doLegal = legal; }
   public double []getLegalValues() { return(this.doLegal); }
   public double []getMissingValues() { return(this.missingDouble); }

   public double  getMissingAsDouble() throws DataModelException { return(missingAsDouble); }
   public void    setMissingAsDouble(double dm) throws DataModelException { missingAsDouble = dm; }

   /**
    * Check if the given value fits this attribute.
    * This is the case if it's legal, a missing value or illegal and can be treated as missing.
    * @param d The value
    * @return <code>true</code> if the value fits this attribute
    * @throws DataModelException If the value does not fit this attribute in some way
    */
   public boolean checkFit(double d) throws DataModelException
   {
       boolean fit;

       fit = true;
       if (checkType(d))
       {
           if (!isMissingValue(d))
           {
               if (!isLegal(d) && !illegalIsMissing)
                   throw new DataModelException("Value "+d+" is illegal and cannot be treated as missing in Attribute '"+name+"'");
           }
       }
       else throw new DataModelException("Value "+d+" is not primitive");

       return(fit);
   }

   /**
    * Check if the given value fits all requirement to be a goal value.
    * @param d The value.
    * @return The value d
    * @throws DataModelException if the value does not fit the requirements of a goal value.
    * (wrong type, missing or illegal value or non-goal value).
    */
   public double getGoalValue(double d) throws DataModelException
   {
     if (isGoal) // Is this attribute a goal attribute?
     {
        if (checkType(d))  // Is the Java type OK?
        {
          if (!isMissingValue(d)) // Is it a missing value. Can't use that as goal?
          {
             if (isLegal(d)) // Is it a legal value?
             {
                if (isGoal(d)) // Is the value present in some goal class?
                {
                    // Right. All fine.
                    return(d);
                }
                else throw new DataModelException("Attribute value "+d+" does not belong to a goal class.");
             }
             else throw new DataModelException("Attribute value "+d+" is an illegal value.");
          }
          else return(getMissingAsDouble());
        }
        else throw new DataModelException("Attribute value "+d+" has non-primitive Java type.");
     }
     else throw new DataModelException("Attribute "+name+" is not a goal");
   }

   /**
    * Get the goal class index of the legal value at the specified index.
    * goalType should be GOAL_CLASS.
    * @param ind Index in the legal values table.
    * @return The index of the goal class. -1 if not found.
    * @throws DataModelException if the goalType is not GOAL_CLASS
    */
   public int getGoalClass(int ind) throws DataModelException
   {
       return(getGoalClass(doLegal[ind]));
   }

   /**
    * Get the goal class index of the specified value.
    * goalType should be GOAL_CLASS.
    * @param d The value for which the goal class index should be found.
    * @return  The goal class index of the given value. -1 if it is not found.
    * @throws DataModelException If the goal type is not GOAL_CLASS.
    */
   public int getGoalClass(double d) throws DataModelException
   {
       int    i,j;
       int    gind;
       double []gcvnow;

       gind = -1;
       if (this.goalType == Attribute.GOAL_CLASS)
       {
           for (i=0; (i<this.goalClass.length) && (gind == -1); i++)
           {
               gcvnow = this.goalClass[i];
               for (j=0; (j<gcvnow.length) && (gind == -1); j++)
               {
                   if (d == gcvnow[j]) gind = i;
               }
           }
       }
       else throw new DataModelException("Cannot get Goal Class for non-categorical goal");

       return(gind);
   }

   /**
    * Checks if the given value corresponds to the unique primitive missing value indicator.
    * @param d The value
    * @return <code>true</code> if the value is the unique primitive missing value indicator.
    */
   public boolean isMissingAsDouble(double d)
   {
       boolean missing;

       if   (Double.isNaN(missingAsDouble)) missing = Double.isNaN(d);
       else                                 missing = (missingAsDouble == d);

       return(missing);
   }

   /**
    * Checks if the given value is a missing value.
    * @param d The value
    * @return <code>true</code> if the value is a missing value.
    * @throws DataModelException never
    */
   public boolean isMissingValue(double d) throws DataModelException
   {
       int     i;
       boolean isMissing;

       isMissing = false;
       if (missingDouble != null)
       {
           for (i=0; (i<missingDouble.length) && (!isMissing); i++)
           {
               if (Double.isNaN(d))
               {
                   if (Double.isNaN(missingDouble[i])) isMissing = true;
               }
               else if (d == missingDouble[i]) isMissing = true;
           }
       }

       return(isMissing);
   }

   /**
    * Checks if the given value is a goal value.
    * @param d the value
    * @return <code>true</code> if the value is a goal value
    * @throws DataModelException never
    */
   public boolean isGoal(double d) throws DataModelException
   {
       boolean goal;

       goal = false;
       if      (goalType == GOAL_CLASS)
       {
           if (getGoalClass(d) >= 0) goal = true;
       }
       else if (goalType == GOAL_VALUE) goal = true;

       return(goal);
   }

  /**
   * Check if the given value is a legal value according to all order, distance and property specifications.
   * @param d The value
   * @return <code>true</code> if the given value is legal.
   * @throws DataModelException If the legallity of the value cannot be determined.
   */
   public boolean isLegal(double d) throws DataModelException
   {
       boolean           fit;
       Iterator          itap;
       AttributeProperty apnow;

       // Iterate over all properties. Check legallity with all of them.
       fit  = true;
       itap = properties.values().iterator();
       while (itap.hasNext() && fit)
       {
          apnow = (AttributeProperty)itap.next();
          fit   = apnow.isLegal(this, d);
       }

       return(fit);
   }

   // **********************************************************\
   // *                      Order and Distance                *
   // **********************************************************/
   public int order(double d1, double d2) throws DataModelException
   {
       if (order != null) return(order.order(this, d1, d2));
       else               return(0);
   }

   public double distance(double d1, double d2) throws DataModelException
   {
       if (distance != null) return(distance.distance(this, d1, d2));
       else                  return(0);
   }

   // **********************************************************\
   // *                       Initialization                   *
   // **********************************************************/
   void init()
   {
       this.isActive         = true;
       this.rawType          = Double.TYPE.getName();
       this.order            = null;
       this.distance         = null;
       this.converter        = null;
       this.missingAsDouble  = 0;
       this.missingIs        = MISSING_IS_UNKNOWN;
       this.illegalIsMissing = false;
       this.properties       = new HashMap();
       this.defaultValue     = 0;
   }

   public AttributeDouble(String _name)
   {
       this.name = _name;
       init();
   }

   public AttributeDouble()
   {
       init();
   }

   // **********************************************************\
   // *                         Cloneing                       *
   // **********************************************************/
   public Object clone() throws CloneNotSupportedException
   {
       AttributeDouble out;

       out = new AttributeDouble(name);
       super.clone(out);
       if (this.goalClass != null) out.goalClass = (double [][])this.goalClass.clone();
       else                   out.goalClass = null;
       if (missingDouble != null) out.missingDouble = (double [])this.missingDouble.clone();
       else                       out.missingDouble = null;
       if (doLegal != null) out.doLegal = (double [])this.doLegal.clone();
       else                 out.doLegal = null;
       out.missingAsDouble  = this.missingAsDouble;
       out.defaultValue     = this.defaultValue;

       return(out);
   }
}
