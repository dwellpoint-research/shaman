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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Object Based Data Attribute</h2>
 * The structure of an attribute containing <code>Object</code> based data. <br>
 * Implements all aspects of an Attribute for non-primitive data.
 * <br>
 */

public class AttributeObject extends Attribute
{
   /**
    * The default value of this Attribute's data.
    * Cloned in <code>createDefaultObject</code> to create default data object.
    * If <code>null</code> a guess is made...
    */ 
   private Object defaultObject;        // Default Object of this Attribute's data
   /**
    * Used to determine the goal class membership of a value.
    * A value belongs to goal class <code>i</code> if it is found in the
    * <code>Array</code> of <code>Object</code>, <code>goalClass[i]</code>
    */
   private Object  [][]goalClass;        // If  GOAL_CLASS then Array of Object[] containing the goal classes

   /** <code>Object</code>s that correspond to missing values. */
   private Object  []missingObject;      // Objects denoting missing values

   /**
    * Array containing the distinct legal values of this attribute if unordered
    * or the legal intervals if an order is defined. Used by the default ObjectDoubleConverter.
    */
   private Object  []obLegal;            // If Ordered : [i/2] = begin legal interval, [i/2+1] = end legal interval. Else legal values.

   /** What is the Object corresponding to a missing value.
    *  Used by the default ObjectDoubleConverter
    */
   private Object  missingAsObject;      // The one missing value object
   
   // **********************************************************\
   // *                  Default Value Creation                *
   // **********************************************************/
   /**
    * Create a 'default' value for this Attribute.
    * It has the right Java type and complies with all AttributeProperties.
    * @throws DataModelException If the Attribute has an impossible configuration.
    */
   public Object getDefaultObject() throws DataModelException
   {
       String rawtype;
       Object obnow;
     
       // Check if the default value is already there.
       obnow = this.defaultObject;
       if (obnow == null)
       {    
          try
          {
              // If not, make an educated guess...
              rawtype = getRawType();
              if      (rawtype.equals("java.lang.Double"))     obnow = new Double(0);
              else if (rawtype.equals("java.lang.String"))     obnow = "";
              else if (rawtype.equals("java.util.Date"))       obnow = new java.util.Date();
              else if (rawtype.equals("java.lang.Integer"))    obnow = new Integer(0);
              else if (rawtype.equals("java.lang.Long"))       obnow = new Long(0);
              else if (rawtype.equals("java.lang.Float"))      obnow = new Float(0);
              else if (rawtype.equals("java.lang.Short"))      obnow = new Short((short)0);
              else if (rawtype.equals("java.lang.Character"))  obnow = new Character((char)' ');
              else if (rawtype.equals("java.lang.Byte"))       obnow = new Byte((byte)0);
              else if (rawtype.equals("java.lang.Boolean"))    obnow = new Boolean(false);
              else if (rawtype.equals("java.lang.Object"))     obnow = new Object();
              else if (rawtype.equals("java.util.Set"))        obnow = new HashSet();
              else if (rawtype.equals("java.util.Map"))        obnow = new HashMap();
              else if (rawtype.equals("java.math.BigDecimal")) obnow = new BigDecimal(0);
              else if (rawtype.equals("java.math.BigInteger")) obnow = new BigInteger("0");
              else if (rawtype.equals("[D"))                   obnow = new double[]{};
              else                                             obnow = Class.forName(rawtype).newInstance();
             
              // And store for further reference
              this.defaultObject = obnow;
          }
          catch(ClassNotFoundException ex) { throw new DataModelException(ex); }
          catch(IllegalAccessException ex) { throw new DataModelException(ex); }
          catch(InstantiationException ex) { throw new DataModelException(ex); }
       }
       
       return(obnow);   
   }

   public void   setDefaultObject(Object defaultObject) throws DataModelException
   { 
       if (defaultObject != null) this.defaultObject = defaultObject;
       else throw new DataModelException("Cannot set default Object as 'null' in attribute '"+getName()+"'");
   }

   // **********************************************************\
   // *                     Typical Usage                      *
   // **********************************************************/
   /**
    * Initialize as Categorical attribute. <br>
    * Installs the default <code>ConverterDefault</code> implementation.
    * No order or distance is installed.
    * Sets a <code>AttributePropertyCategorical</code> property.
    * @param _rawType The name of the Java class of the data objects of this Attribute.
    * @param _legal The distinct legal values of this attribute's data.
    */
   public void initAsSymbolCategorical(String _rawType, Object []_legal)
   {
       this.rawType   = _rawType;
       this.order     = null;
       this.distance  = null;
       this.converter = new ConverterDefault();
       this.obLegal   = _legal;
       addProperty(PROPERTY_CATEGORICAL, new AttributePropertyCategorical());
       setDefaultMissingValues();
   }

   /**
    * Initialize as continuous attribute.
    * Installs the default <code>OrderNumber, DistanceNumber, ConverterDefault</code> implementations.
    * Sets a <code>AttributePropertyContinuous</code> property.
    * @param _rawType The Java class name of the this attribute's data.
    * @param range The range in which to numbers lie.
    */
   public void initAsNumberContinuous(String _rawType, Object []range)
   {
       this.rawType       = _rawType;
       this.order         = new OrderNumber();
       this.distance      = new DistanceNumber();
       this.converter     = new ConverterDefault();
       this.obLegal       = range;
       addProperty(PROPERTY_CONTINUOUS, new AttributePropertyContinuous());
       setDefaultMissingValues();
   }

   /**
    * Initialize as continuous attribute.
    * Installs the default <code>OrderNumber, DistanceNumber, ConverterDefault</code> implementations.
    * The range is equal to the entire range of the given Java type.
    * Sets a <code>AttributePropertyContinuous</code> property.
    * @param _rawType The Java class name of the this attribute's data.
    */
   public void initAsNumberContinuous(String _rawType)
   {
       Object []rnow;

       if      (_rawType.equals("java.lang.Integer"))     rnow = new Integer[]    { new Integer(Integer.MIN_VALUE),           new Integer(Integer.MAX_VALUE) };
       else if (_rawType.equals("java.lang.Double"))      rnow = new Double[]     { new Double(Double.NEGATIVE_INFINITY),     new Double(Double.POSITIVE_INFINITY) };
       else if (_rawType.equals("java.lang.Byte"))        rnow = new Byte[]       { new Byte(Byte.MIN_VALUE),                 new Byte(Byte.MAX_VALUE) };
       else if (_rawType.equals("java.lang.Float"))       rnow = new Float[]      { new Float(Float.NEGATIVE_INFINITY),       new Float(Float.POSITIVE_INFINITY) };
       else if (_rawType.equals("java.lang.Long"))        rnow = new Long[]       { new Long(Long.MIN_VALUE),                 new Long(Long.MAX_VALUE) };
       else if (_rawType.equals("java.lang.Short"))       rnow = new Short[]      { new Short(Short.MIN_VALUE),               new Short(Short.MAX_VALUE) };
       else if (_rawType.equals("java.math.BigDecimal"))  rnow = new BigDecimal[] { new BigDecimal(-Double.MAX_VALUE),        new BigDecimal(Double.MAX_VALUE) };
       else if (_rawType.equals("java.math.BigInterger")) rnow = new BigInteger[] { new BigInteger(""+Integer.MIN_VALUE),     new BigInteger(""+Integer.MAX_VALUE) };
       else if (_rawType.equals("java.lang.String"))      rnow = new String[]     { ""+(-Double.MAX_VALUE),                   ""+Double.MAX_VALUE };
       else rnow = null; // YADA YADA YADA

       initAsNumberContinuous(_rawType, rnow);
   }

   /**
    * Initialize as free text String. <br>
    * Sets <code>rawType</code> to <code>java.lang.String</code>.
    * Installs the default ObjectDouble converter.
    */
   public void initAsFreeText()
   {
       this.rawType       = new String().getClass().getName();
       this.order         = null;
       this.distance      = null;
       this.converter     = new ConverterDefault();
       this.obLegal       = null;
       setDefaultMissingValues();
   }

   /**
    * Initialize as free form Object. <br>
    * Installs the default ObjectDouble converter.
    * @param _rawType The name of the class of the data objects of this Attribute
    */
   public void initAsFreeObject(String _rawType)
   {
       this.rawType       = _rawType;
       this.order         = null;
       this.distance      = null;
       this.converter     = new ConverterDefault();
       this.obLegal       = null;
       this.missingObject = new Object[0];
   }

   /**
    * Installs the default missing value Objects.
    */
   private void setDefaultMissingValues()
   {
       this.missingObject   = new Object []{ null };
       this.missingAsObject = null;
   }

   // **********************************************************\
   // *                Categorical Value Access                *
   // **********************************************************/
   public int getCategory(Object o) throws DataModelException
   {
       int i;
       boolean found;
       int     pos;

       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL))
           throw new DataModelException("The attribute with name '"+name+"' is not a categorical attribute.");
       if ((missingIs == MISSING_IS_VALUE) && (isMissingAsObject(o))) pos = this.obLegal.length;
       else
       {
           found = false;
           pos   = -1;
           for (i=0; (i<  this.obLegal.length) && (!found); i++)
           {
               if (this.obLegal[i].equals(o)) { pos = i; found = true; }
           }
       }

       return(pos);
   }

   public int getNumberOfCategories() throws DataModelException
   {
       int nv;

       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL))
           throw new DataModelException("The attribute with name '"+name+"' is not a categorical attribute.");
       nv = this.obLegal.length;
       if (this.missingIs == MISSING_IS_VALUE) nv++;

       return(nv);
   }

   public Object getCategoryObject(int vi) throws DataModelException
   {
       if (!hasProperty(Attribute.PROPERTY_CATEGORICAL))
           throw new DataModelException("The attribute with name '"+name+"' is not a categorical attribute.");
       if (vi < this.obLegal.length) return(this.obLegal[vi]);
       else                          return(this.missingAsObject);
   }

   // **********************************************************\
   // *              Object <-> Double Converting              *
   // **********************************************************/
   public double getDoubleValue(Object ob, AttributeDouble atd) throws DataModelException
   {
       if (this.converter != null) return(this.converter.toDouble(ob, this, atd));
       else                        return(0);
   }

   // **********************************************************\
   // *                   Other Things (tm)                    *
   // **********************************************************/
   public Object  [][]getGoalClasses()     { return(this.goalClass); }
   public int     getNumberOfGoalClasses() { return(this.goalClass.length); }

   public void    setRawType(String _rawType) throws DataModelException
   {
       try
       {
           if (Class.forName(_rawType).isPrimitive())
               throw new DataModelException("Cannot use primitive class '"+_rawType+"' as raw type of an Object Attribute.");
           else this.rawType = _rawType;
       }
       catch(ClassNotFoundException ex) { throw new DataModelException(ex); }
   }

   protected double getCategoricalDouble(Object ob, AttributeDouble atd) throws DataModelException
   {
       int     i;
       double  val;
       double  []doLegal;
       boolean found; 

       val     = -1;
       doLegal = atd.getLegalValues();
       found   = false;
       if ((this.obLegal != null) && (doLegal != null) && (this.obLegal.length == doLegal.length))
       {
           // Find the index of the Object. Take the corresponding legal value in the Double one.
           for (i=0; i<obLegal.length; i++)
           {
              if (ob.equals(this.obLegal[i])) { val = atd.getLegalValues()[i]; found = true; }
           }
           if (!found)
           {
               // What to do when cannot find the Object in the legal values table.
               if (atd.getMissingIs() == Attribute.MISSING_IS_VALUE) val = atd.getMissingAsDouble();
               else throw new DataModelException("Cannot find the Object in the legal values table and cannot treat as missing.");
           }
      }
      else throw new DataModelException("The Object and Double Attributes' structures do not agree in attribute '"+this.name+"'.");

      return(val);
   }

   // **********************************************************\
   // *                     Goal Definition                    *
   // **********************************************************/
   public void setValuesAsGoal() throws DataModelException
   {
       int i;

       // Set the legal values of this attribute as goal classes.
       this.isGoal   = true;
       this.goalType = GOAL_CLASS;

       this.goalClass = new Object[this.obLegal.length][];
       for (i=0; i<this.goalClass.length; i++) this.goalClass[i] = new Object[] { this.obLegal[i] };
   }

   public void setValueGroupsAsGoal(Object [][]gr) throws DataModelException
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
                   found = false; pos = -1;
                   for (k=0; (k<this.obLegal.length) && (!found); k++)
                   {
                       if (this.obLegal[k].equals(this.goalClass[i][j])) { found = true; pos = k; }
                   }
                   if (found) gc[i][j] = pos;
                   else throw new DataModelException("Cannot find index in legal table of goal value "+goalClass[i][j]);
               }
           }
        }
        else throw new DataModelException("Cannot make goal class index table because the goal-type is wrong.");

        return(gc);
   }

   // **********************************************************\
   // *        Attribute Fit and Instance Construction         *
   // **********************************************************/
   public void  setMissingValues(Object []mv)  { this.missingObject = mv; }
   public void  setLegalValues(Object []legal) { this.obLegal = legal; }
   public Object []getLegalValues()   { return(this.obLegal); }
   public Object []getMissingValues() { return(this.missingObject); }

   public void   setMissingAsObject(Object _mao) { this.missingAsObject = _mao; }
   public Object getMissingAsObject()            { return(this.missingAsObject); }


   /**
    * Check if the given Object fits this attribute.
    * This is the case if it's legal, a missing value or illegal and can be treated as missing.
    * @param ob The Object
    * @return <code>true</code> if the value fits this attribute
    * @throws DataModelException If the value does not fit this attribute in some way
    */
    public boolean checkFit(Object ob) throws DataModelException
    {
        boolean fit;

        fit = true;
        if (checkType(ob))
        {
           if (!isMissingValue(ob))
           {
               if (!isLegal(ob) && !illegalIsMissing) throw new DataModelException("Value "+ob+" is illegal and cannot be treated as missing in Attribute '"+name+"'");
           }
        }
        else throw new DataModelException("Value "+ob+" does not have the correct type "+rawType+" != "+ob.getClass().getName()+" in attribute "+getName());

        return(fit);
    }

   /**
    * Check if the given Object fits all requirement to be a goal Object.
    * @param ob The Object.
    * @return The Object ob
    * @throws DataModelException if the value does not fit the requirements of a goal Object.
    *        (wrong type, missing or illegal value or non-goal value).
    */
   public Object getGoalValue(Object ob) throws DataModelException
   {
     if (isGoal) // Is this attribute a goal attribute?
     {
        if (checkType(ob))  // Is the Java type OK?
        {
          if (!isMissingValue(ob)) // Is it a missing value. Can't use that as goal?
          {
             if (isLegal(ob)) // Is it a legal value?
             {
                if (isGoal(ob)) // Is the value present in some goal class?
                {
                  return(ob);
                }
                else throw new DataModelException("Attribute value "+ob+" does not belong to a goal class.");
             }
             else throw new DataModelException("Attribute value "+ob+" is an illegal value.");
          }
          else return(getMissingAsObject()); // throw new DataModelException("Attribute value "+ob+" is a missing value.");
        }
        else throw new DataModelException("Attribute value "+ob+" has wrong Java type. "+ob.getClass().getName()+" while expecting "+rawType);
     }
     else throw new DataModelException("Attribute "+name+" is not a goal");
   }

   /**
    * Get the goal class index of the legal Object at the specified index.
    * goalType should be GOAL_CLASS.
    * @param ind Index in the legal Objects table.
    * @return The index of the goal class. -1 if not found.
    * @throws DataModelException if the goalType is not GOAL_CLASS
    */
   public int getGoalClass(int ind) throws DataModelException
   {
       return(getGoalClass(obLegal[ind]));
   }

   /**
    * Get the goal class index of the specified Object.
    * goalType should be GOAL_CLASS.
    * @param go The Object for which the goal class index should be found.
    * @return  The goal class index of the given Object. -1 if it is not found.
    * @throws DataModelException If the goal type is not GOAL_CLASS.
    */
   public int getGoalClass(Object go) throws DataModelException
   {
       int    i,j;
       int    gind;
       Object []gcvnow;

       gind = -1;
       if (this.goalType == Attribute.GOAL_CLASS)
       {
          for (i=0; (i<this.goalClass.length) && (gind == -1); i++)
          {
             gcvnow = this.goalClass[i];
             for (j=0; (j<gcvnow.length) && (gind == -1); j++)
             {
                 if (go.equals(gcvnow[j])) gind = i;
             }
          }
       }
       else throw new DataModelException("Cannot get Goal Class for non-categorical goal.");

       return(gind);
   }

   /**
    * Checks if the given Objects corresponds to the unique Object missing value indicator.
    * @param o The Object
    * @return <code>true</code> if the value is the unique Object missing value indicator.
    */
   public boolean isMissingAsObject(Object o)
   {
       boolean missing;

       if   (this.missingAsObject == null) missing = (o == null);
       else                                missing = o.equals(this.missingAsObject);

       return(missing);
   }

   /**
    * Checks if the given Object is a missing Object.
    * @param ob the Object
    * @return <code>true</code> if the Object is a missing Object.
    * @throws DataModelException never
    */
   public boolean isMissingValue(Object ob) throws DataModelException
   {
       int     i;
       boolean isMissing;

       isMissing = false;
       if (this.missingObject != null)
       {
           for (i=0; (i<this.missingObject.length) && (!isMissing); i++)
           {
               if (ob == null)
               {
                   if (this.missingObject[i] == null) isMissing = true;
               }
               else if (ob.equals(this.missingObject[i])) isMissing = true;
           }
       }

       return(isMissing);
   }

   /**
    * Checks if the given Object is a goal Object.
    * @param ob The Object
    * @return <code>true</code> if the Object is a goal Object
    * @throws DataModelException never
    */
   public boolean isGoal(Object ob) throws DataModelException
   {
       boolean goal;

       goal = false;
       if      (this.goalType == GOAL_CLASS)
       {
           if (getGoalClass(ob) >= 0) goal = true;
       }
       else if (this.goalType == GOAL_VALUE) goal = true;

       return(goal);
   }

   /**
   * Check if the given Object is a legal Object according to all order, distance and attribute specifications.
   * @param o The Object
   * @return <code>true</code> if the given Object is legal.
   * @throws DataModelException If the legallity of the value cannot be determined.
   */
   public boolean isLegal(Object o) throws DataModelException
   {
       boolean           fit;
       Iterator          itap;
       AttributeProperty apnow;

       // Iterate over all properties. Check legallity of the Object with all of them.
       fit  = true;
       itap = this.properties.values().iterator();
       while (itap.hasNext() && fit)
       {
           apnow = (AttributeProperty)itap.next();
           fit   = apnow.isLegal(this, o);
       }

       return(fit);
   }

   // **********************************************************\
   // *                      Order and Distance                *
   // **********************************************************/
   public int order(Object o1, Object o2) throws DataModelException
   {
       if (this.order != null) return(this.order.order(this, o1, o2));
       else                   return(0);
   }

   public double distance(Object o1, Object o2) throws DataModelException
   {
       if (this.distance != null) return(this.distance.distance(this, o1, o2));
       else                       return(0);
   }

   // **********************************************************\
   // *                       Initialization                   *
   // **********************************************************/
   void init()
   {
       this.isActive         = true;
       this.rawType          = new String().getClass().getName();
       this.order            = null;
       this.distance         = null;
       this.converter        = null;
       this.missingAsObject  = "?";
       this.missingIs        = MISSING_IS_UNKNOWN;
       this.illegalIsMissing = false;
       this.defaultObject    = null;
       this.properties       = new HashMap();
   }

   public AttributeObject(String _name)
   {
       this.name = _name;
       init();
   }

   public AttributeObject()
   {
       init();
   }

   // **********************************************************\
   // *                         Cloneing                       *
   // **********************************************************/
   public Object clone() throws CloneNotSupportedException
   {
       AttributeObject out;

       out = new AttributeObject(name);
       super.clone(out);
       if (this.goalClass != null) out.goalClass = (Object [][])this.goalClass.clone();
       else                        out.goalClass = null;
       if (this.missingObject != null) out.missingObject = (Object [])this.missingObject.clone();
       else                            out.missingObject = null;
       if (this.obLegal != null) out.obLegal  = (Object [])this.obLegal.clone();
       else                      out.obLegal  = null;
       out.missingAsObject  = this.missingAsObject;
       out.defaultObject    = this.defaultObject;

       return(out);
   }
}
