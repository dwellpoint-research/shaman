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

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Attribute Base Class</h2>
 * Base Class for describing the structure of an attribute. <br>
 * Includes support for missing value handling, legal value definition,
 * categorical, continuous, periodical and ordinal data. Has activation
 * and goal selection support. Can use custom order and distance relations.
 * Contains a set of attribute properties (e.g. a fuzzy structure) that define
 * it's specific structure, can be enforced and checked. Supports converting
 * Object data to primitive data (doubles) via a convertion interface.
 */
public abstract class Attribute
{
   // The very basic data properties.
   /** A property indicating that the attribute is continuous (has an order and distance) */
   public static final String PROPERTY_CONTINUOUS  = "continuous";
   /** A property indicating that the attribute is categorical (has neither order nor distance) */
   public static final String PROPERTY_CATEGORICAL = "categorical";

   // Goal Interpretation
   /** The goal is the (continuous) value of the data. */
   public static final int GOAL_VALUE = 1;  // Use the goal's value as goal
   /** The goal is the (categorical) goal class membership of the value of the data.
    *  Uses the <code>goalClass</code> field to determine the goal class of a value.
    */
   public static final int GOAL_CLASS = 2;  // Use the specified object arrays as groups of values that define the goal classes

   // Missing Value Handling
   /** Treat missing values as a distinct category of data when possible */
   public static final int MISSING_IS_VALUE   = 1;  // Treat missing values as separate class
   /** Treat missing values as unknown data when possible */
   public static final int MISSING_IS_UNKNOWN = 2;  // Treat missing values as unknown where possible

   // Attribute Definition
   /** The unique name of this attribute */
   protected String   name;                // Unique Attribute Identification

   /** Is the attribute active in the Flow?
    *  If <code>false</code>, it's data will not be changed by the Flow's data-transformation.
    */
   protected boolean  isActive;            // Active in machine learning?

   // General Data Typing
   /** The Java class name of the attribute's data.
    *  Equals <code>Double.TYPE</code> in AttributeDouble,
    *  the name of some sub-class of <code>Object</code> in AttributeObject.
    */
   protected String  rawType;    // The raw Java type of the attribute's data.

   /**
    * How to convert non-primitive data to primitive data and vice-versa.
    * @see ObjectDoubleConverter
    */
   protected ObjectDoubleConverter converter;  // How to convert the attribute from double to Objects and back...

   /** The order relation between data of this Attribute.
    *  @see Order
    */
   protected Order    order;               // Order Function
   /** The distance function between data of this Attribute.
    *  @see Distance
    */
   protected Distance distance;            // Distance Function

   /** Can this attribute be the goal of some machine learning algorithm? */
   protected boolean isGoal;               // Is a goal attribute?

   /** What kind of goal does this attribute represent?
    *  @see #GOAL_VALUE
    *  @see #GOAL_CLASS
    */
   protected int     goalType;             // Goal classes specification.

   // Missing Values
   /** Should illegal values be treated as missing value? */
   protected boolean illegalIsMissing;     // Treat illegal values as if they were missing ones.

   /** The meaning of a missing value. An unknown value, or a distinct class of values.
    *  @see #MISSING_IS_VALUE
    *  @see #MISSING_IS_UNKNOWN
    */
   protected int     missingIs;            // Meaning of a missing value

   // High-level Data Properties
   /**
    * Map of properties of this attribute. Map if (String, AttributeProperty).
    * e.g. "date" -> AttributePropertyDate or Fuzzy -> Fuzzy Membership Function AttributeProperty
    */
   protected HashMap properties;         // e.g. Normalized, Standardized, Fuzzy

   // **********************************************************\
   // *     Missing, Legal Value Recognition Defaults          *
   // **********************************************************/
   // Classification Goal Specific
   /**
    * Get the goal class of the 'ind'th categorical value of this Attribute.
    * @param ind The index of the categorical value.
    * @return The goal class of the 'ind'th categorical value. -1 if it is not present in any goal class.
    * @throws DataModelException If the Attribute is not Categorical.
    */
   public abstract int     getGoalClass(int ind) throws DataModelException;

   /**
    * Get the indices of the categorical values a the goal classes.
    * @return 2D integer array indexed as [goalClass][i],
    *            containing the index of the categorical value 'i' of the 'goalClass'th goal class.
    * @throws DataModelException If the Attribute is not Categorical or
    *                            a goal class contains a value whose index cannot be found.
    */
   public abstract int [][]getGoalClassIndices() throws DataModelException;

   /**
    * Get the number of goal classes.
    * @return The number of goal classes.
    * @throws DataModelException If the Attribute is not Categorical.
    */
   public abstract int     getNumberOfGoalClasses() throws DataModelException;

   // **********************************************************\
   // *                Categorical Value Access                *
   // **********************************************************/
   /**
    * Get the number of categorical values in this Attribute.
    * Missing values have their own category if <code>missingIs == MISSING_IS_VALUE</code>
    * @return The number of categorical values.
    * @throws DataModelException If the Attribute is not Categorical.
    */
   public abstract int    getNumberOfCategories() throws DataModelException;

   // **********************************************************\
   // *      Shared (primitive/object) Attribute Behaviour     *
   // **********************************************************/
   /**
    * Set the order relation to the specified Order.
    * @param _order The new Order relation.
    */
   public void     setOrder(Order _order)           { order = _order; }
   /**
    * Get the order relation of this Attribute.
    * @return The current Order relation
    */
   public Order    getOrder()                       { return(order); }
   /**
    * Set the distance function to the given Distance.
    * @param _distance The new Distance to use.
    */
   public void     setDistance(Distance _distance)  { distance = _distance; }
   /**
    * Get the current distance function.
    * @return The current distance function
    */
   public Distance getDistance()                    { return(distance); }
   /**
    * Determine the activation of this Attribute.
    * An inactive attribute is not used in data transformation operations, just copied.
    * @param _active The new activation value
    */
   public void     setIsActive(boolean _active)     { isActive = _active;  }
   /**
    * Get the current activation status.
    * @return <code>true</code> if the Attribute is active
    */
   public boolean  getIsActive()                    { return(isActive); }

   /**
    * Set the meaning of missing values.
    * @param _missingIs The new meaning of missing values.
    * @see #MISSING_IS_VALUE
    * @see #MISSING_IS_UNKNOWN
    */
   public void     setMissingIs(int _missingIs)     { missingIs = _missingIs; }
   /**
    * Get the current meaning of missing values.
    * @return The meaning of missing values.
    * @see #MISSING_IS_VALUE
    * @see #MISSING_IS_UNKNOWN
    */
   public int      getMissingIs()                   { return(missingIs); }
   /**
    * Set wheter or not illegal values should to considered to be missing values.
    * @param _illegalIsMissing The new value
    */
   public void     setIllegalIsMissing(boolean _illegalIsMissing) { illegalIsMissing = _illegalIsMissing; }
   /**
    * Are illegal values considered to be missing values?
    * @return Are illegal values treated as missing ones?
    */
   public boolean  getIllegalIsMissing()                          { return(illegalIsMissing); }
   /**
    * Determine wheter or not this attribute can be used as a goal during machine-learning.
    * @param _isGoal Is it a possible goal attribute?
    */
   public void     setIsGoal(boolean _isGoal) { isGoal = _isGoal; }
   /**
    * Can this attribute be used as a goal field in a supervised machine-learning algorithm?
    * @return <code>true</code> if so.
    */
   public boolean  getIsGoal()                { return(isGoal); }
   /**
    * Set what kind of goal this attribute represents.
    * A goal where the value should be estimated, or one where the goal class membership counts.
    * @param _goalType Which kind of goal is this?
    */
   public void     setGoalType(int _goalType) { goalType = _goalType; }
   /**
    * What kind of goal does this attribute represent?
    * @return The type of goal.
    * @see #GOAL_CLASS
    * @see #GOAL_VALUE
    */
   public int      getGoalType()              { return(goalType); }
   /**
    * Get the name of the Java Class of this Attribute's data.
    * Equal to <code>Double.TYPE</code> for AttributeDouble.
    * @return The name of the data's Class.
    */
   public String   getRawType()               { return(rawType);  }
   /**
    * Set the Object<->Double converter for this Attribute.
    * @param _converter The new ObjectDoubleConverter.
    */
   public void     setConverter(ObjectDoubleConverter _converter) { converter = _converter; }
   /**
    * Get the current ObjectDoubleConverter of this Attribute.
    * @return The current ObjectDoubleConverter.
    */
   public ObjectDoubleConverter getConverter() { return(converter); }
   /**
    * Change the unique name of this Attribute.
    * @param _name The new name
    */
   public void     setName(String _name)      { name = _name; }
   /**
    * Get the Attribute's unique name.
    * @return The name.
    */
   public String   getName()                  { return(name); }

   /**
    * Add a property of the attribute to the list.
    * @param key The name of the property.
    * @param value The property itself
    */
   public void addProperty(String key, AttributeProperty value)
   {
       this.properties.put(key, value);
   }

   /**
    * Get the value of the given property.
    * @param key The name of the property.
    * @return The value of the property. <code>null</code> if not present.
    */
   public AttributeProperty getProperty(String key)
   {
       return((AttributeProperty)this.properties.get(key));
   }

   /**
    * Check if the property is present.
    * @param key The name of the property.
    * @return <code>true</code> if the property is present.
    */
   public boolean hasProperty(String key)
   {
       return(this.properties.containsKey(key));
   }

   /**
    * Remove the given property from the list.
    * @param key The name of the property to remove.
    */
   public void removeProperty(String key)
   {
       this.properties.remove(key);
   }

   /**
    * Change the Java type of this Attribute.
    * Should be Double.TYPE for AttributeDouble and the name of a sub-class of Object in AttributeObject.
    * @param rawType The name of the Java class of this Attributes Data.
    * @throws DataModelException If the given Class-name cannot contain the data.
    */
   public abstract void setRawType(String rawType) throws DataModelException;

   /**
    * Check if the Object has the correct type.
    * @param ob The object to check.
    * @return <code>true</code> if the class if the given Object is the raw Java type of this attribute.
    */
   public boolean checkType(Object ob)
   {
       // Check if the raw Java type of the given object agrees with the attribute's specification.
       if (ob == null) return(true); // 'null's are of all types

       try
       {
           if (Class.forName(rawType).isAssignableFrom(ob.getClass())) return(true);
           else return(false);
       }
       catch(ClassNotFoundException ex) { return(false); }
   }

   /**
    * Check if the raw Java type of this attribute is primitive.
    * @param d A double value.
    * @return <code>true</code> if this is an AttributeDouble.
    */
   public boolean checkType(double d)
   {
       if (this instanceof AttributeDouble) return(true);
       else                                 return(false);
   }

   // **********************************************************\
   // *                     Goal Definition                    *
   // **********************************************************/
   /**
    * This attribute can be a supervised estimator's goal. The data's value is the goal.
    */
   public void setAsGoal()
   {
       // Used for regression problems
       this.isGoal   = true;
       this.goalType = GOAL_VALUE;
   }

   /**
    * This attribute can be a supervised classifier's goal.
    * The goal classes are all (categorical) legal values of the data.
    * @throws DataModelException If the attribute is not a categorical one.
    */
   public abstract void setValuesAsGoal() throws DataModelException;

   // **********************************************************\
   // *                         Cloning                        *
   // **********************************************************/
   /**
    * Make a copy of this Attribute.
    * @return A deep copy of this Attribute.
    * @throws CloneNotSupportedException If the copy could not be made.
    */
   public abstract Object clone() throws CloneNotSupportedException;

   /**
    * Make a copy of all things contained in this base class in the given output Attribute.
    * @param out The attribute that is the clone.
    * @throws CloneNotSupportedException If this base class cannot clone.
    */
   protected void clone(Attribute out) throws CloneNotSupportedException
   {
       // Copy or clone Attribute structure to the given attribute
       out.name     = this.name;
       out.isActive = this.isActive;
       out.rawType  = this.rawType;
       if (converter != null)
          if (converter != this) out.converter = (ObjectDoubleConverter)(this.converter.clone());
          else                   out.converter = (ObjectDoubleConverter)out;
       else converter = null;
       if (order != null)
          if (order     != this) out.order     = (Order)(this.order.clone());
          else                   out.order     = (Order)out;
       else order = null;
       if (distance != null)
          if (distance  != this) out.distance  = (Distance)(this.distance.clone());
          else                   out.distance  = (Distance)out;
       else distance = null;
       out.isGoal    = this.isGoal;
       out.goalType  = this.goalType;
       out.illegalIsMissing = this.illegalIsMissing;
       out.missingIs        = this.missingIs;
       if (this.properties != null) out.properties = (HashMap)this.properties.clone();
   }
}
