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
package org.shaman.rule;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.Order;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Condition</h2>
 * Test whether the condition is satisfied by the given data.
 */
// *********************************************************\
// *                        Condition                      *
// *********************************************************/
public class Condition implements Cloneable, Persister
{
    /** Attribute == Value */
    public static final int IS_EQUAL         = 0;
    /** Attribute != Value */
    public static final int IS_NOT_EQUAL     = 1;
    /** Attribute > Value */
    public static final int IS_GREATER       = 2;
    /** Attribute < Value */
    public static final int IS_SMALLER       = 3;
    /** Attribute >= Value */
    public static final int IS_GREATER_EQUAL = 4;
    /** Attribute <= Value */
    public static final int IS_SMALLER_EQUAL = 5;
    /** Attribute in Value(Set) */
    public static final int IS_IN            = 6;
    
    /** The DataModel of the data */
    private DataModel        dataModel;
    /** The Attribute involved in this condition */
    private String           attribute;
    /** The condition to satify on the attribute's data */
    private int              condition;
    /** The condition with this value must by satisfied by the attribute's data */
    private Object           value;
    
    // **********************************************************\
    // *                        Data Access                     *
    // **********************************************************/
    /**
     * Get this condition's operator.
     * @return This condition's operator. (<code>IS_x</code>)
     */
    public int    getCondition() { return(condition); }
    /**
     * Get the name of the attribute involved in this condition
     * @return The name of the attribute of this condition
     */
    public String getAttribute() { return(attribute); }
    /**
     * Get the value used, together with the condition, to determine of this condition holds.
     * @return The value. Double Object if DataModel is primitive.
     */
    public Object getValue()     { return(value);     }
    
    /**
     * Change the operator of this condition
     * @param _condition The new operator.
     */
    public void setCondition(int _condition) { condition = _condition; }
    
    /**
     * Change the condition's value.
     * @param _value The new value.
     */
    public void setValue(Object _value)      { value     = _value; }
    
    /**
     * Change the name of the attribute involved in this condition
     * @param _attribute The new attribute's name
     * @throws LearnerException If the attribute cannot be found in the DataModel.
     */
    public void setAttribute(String _attribute) throws LearnerException
    {
        int actind;
        
        if (dataModel != null)
        {
            actind    = dataModel.getAttributeIndex(attribute);
            if (actind == -1) throw new LearnerException("Cannot set attribute's name to '"+attribute+"'. There is no such attribute in the DataModel.");
            
            attribute = _attribute;
        }
    }
    
    /**
     * Change the DataModel used by this condition.
     * @param _dataModel The new DataModel to use.
     * @throws DataModelException If this condition's attribute cannot be found in the new DataModel.
     */
    public void setDataModel(DataModel _dataModel) throws DataModelException
    {
        int actind;
        
        dataModel = _dataModel;
        if (attribute != null)
        {
            actind    = dataModel.getAttributeIndex(attribute);
            if (actind == -1) throw new DataModelException("Cannot use DataModel "+dataModel.getName()+" it doesn't contain the attribute with name '"+attribute+"'");
        }
    }
    
    // **********************************************************\
    // *                    Condition Checking                  *
    // **********************************************************/
    /**
     * Check if the given instance complies with this condition.
     * @param instance The primitive instance
     * @return <code>true</code> if the given instance sattifies this condition.
     * @throws DataModelException Never.
     */
    public boolean apply(DoubleMatrix1D instance) throws DataModelException
    {
        double  dat, val;
        boolean match;
        int     datind;
        
        match = false;
        if (instance.size() == dataModel.getNumberOfActiveAttributes()) // Instance Vector
        {
            datind = dataModel.getActiveAttributeIndex(attribute);
            if (datind != -1)  dat = instance.getQuick(datind);
            else throw new DataModelException("Cannot check condition involving unknown active attribute '"+attribute+"'");
        }
        else                                                            // Raw Data Vector
        {
            datind = dataModel.getAttributeIndex(attribute);
            if (datind != -1) dat = instance.getQuick(datind);
            else throw new DataModelException("Cannot check condition involving unknown attribute '"+attribute+"'");
        }
        
        if (condition != IS_IN)
        {
            AttributeDouble atdo = (AttributeDouble)dataModel.getAttribute(attribute);
            val = ((Double)value).doubleValue();
            // Check order relational conditions using primitive Java operators.
            if      ((condition == IS_GREATER)       && (dat  > val)) match = true;
            else if ((condition == IS_GREATER_EQUAL) && (dat >= val)) match = true;
            else if ((condition == IS_SMALLER)       && (dat  < val)) match = true;
            else if ((condition == IS_SMALLER_EQUAL) && (dat <= val)) match = true;
            else if (condition == IS_EQUAL)
            {
                // If Categorical attribute. Check category equality. Else use Java equality operator.
                if (atdo.hasProperty(Attribute.PROPERTY_CATEGORICAL)) match = (atdo.getCategory(dat) == val);
                else                                                  match = (dat == val);
            }
            else if (condition == IS_NOT_EQUAL)
            {
                if (atdo.hasProperty(Attribute.PROPERTY_CATEGORICAL)) match = (atdo.getCategory(dat) != val);
                else                                                  match = (dat != val);
            }
        }
        else
        {
            // Set membership test.
            Set    vset = (Set)value;
            Double odat = new Double(dat);
            if (vset.contains(odat)) match = true;
            else                     match = false;
        }        
        
        return(match);
    }
    
    /**
     * Check if the given Object instance complies with this condition.
     * @param instance The Object instance
     * @return <code>true</code> if the given instance sattifies this condition. <code>false</code> else.
     * @throws DataModelException If no order relation is found in the attribute and this condition requires one.
     *                            Or if the attribute cannot be found in the DataModel.
     */
    public boolean apply(ObjectMatrix1D instance) throws DataModelException
    {
        Object  dat, val;
        int     cat;
        boolean match;
        int     datind;
        
        val   = value;
        match = false;
        if (instance.size() == dataModel.getNumberOfActiveAttributes()) // Instance Vector
        {
            datind = dataModel.getActiveAttributeIndex(attribute);
            if (datind != -1)  dat = instance.getQuick(datind);
            else throw new DataModelException("Cannot check condition involving unknown active attribute '"+attribute+"'");
        }
        else                                                            // Raw Data Vector
        {
            datind = dataModel.getAttributeIndex(attribute);
            if (datind != -1) dat = instance.getQuick(datind);
            else throw new DataModelException("Cannot check condition involving unknown attribute '"+attribute+"'");
        }
        
        if (condition != IS_IN)
        {
            AttributeObject atob = (AttributeObject)dataModel.getAttribute(attribute);
            Order           ord  = atob.getOrder();
            if ((ord == null) && !((condition == IS_EQUAL) || (condition == IS_NOT_EQUAL)))
                throw new DataModelException("Can't check condition because no order relation found.");
            if      ((condition == IS_GREATER)       && (ord.order(atob, dat, val) >  0)) match = true;
            else if ((condition == IS_GREATER_EQUAL) && (ord.order(atob, dat, val) >= 0)) match = true;
            else if ((condition == IS_SMALLER)       && (ord.order(atob, dat, val) <  0)) match = true;
            else if ((condition == IS_SMALLER_EQUAL) && (ord.order(atob, dat, val) <= 0)) match = true;
            else if (condition == IS_EQUAL)
            {
                cat = ((Double)val).intValue();
                if (atob.hasProperty(Attribute.PROPERTY_CATEGORICAL)) match = (atob.getCategory(dat) == cat);
                else                                                  match = (dat.equals(val));
            }
            else if (condition == IS_NOT_EQUAL)
            {
                cat = ((Double)val).intValue();
                if (atob.hasProperty(Attribute.PROPERTY_CATEGORICAL)) match = (atob.getCategory(dat) != cat);
                else                                                  match = !(dat.equals(val));
            }
        }
        else
        {
            Set  vset = (Set)value;
            if (vset.contains(dat)) match = true;
            else                    match = false;
        }
        
        return(match);
    }
    // **********************************************************\
    // *                 Basic Java Object Operations           *
    // **********************************************************/
    /**
     * Convert this Condition into human-readable format.
     * @return A String of the format :
     *         <code>attribute OPERATOR value</code>
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer(attribute);
        if      (condition == IS_EQUAL)          sb.append(" == ");
        else if (condition == IS_NOT_EQUAL)      sb.append(" != ");
        else if (condition == IS_GREATER)        sb.append("  > ");
        else if (condition == IS_GREATER_EQUAL)  sb.append(" >= ");
        else if (condition == IS_SMALLER)        sb.append("  < ");
        else if (condition == IS_SMALLER_EQUAL)  sb.append(" <= ");
        else if (condition == IS_IN)             sb.append(" in ");
        sb.append(value.toString());
        
        return(sb.toString());
    }
    
    /**
     * Equality relation on condition.
     * @param o2 the second object.
     * @return <code>true</code> if the condition, value and attribute are equal.
     */
    public boolean equals(Object o2)
    {
        boolean equal;
        if ((o2 == null) || ((o2 != null) && !(o2 instanceof Condition))) equal = false;
        else
        {
            Condition c2 = (Condition)o2;
            
            equal = (c2.condition == condition) &&
            (c2.value.equals(value))    &&
            (c2.attribute.equals(attribute));
        }
        
        return(equal);
    }
    
    /**
     * Copy the condition.
     * @return A copy of this condition.
     * @throws CloneNotSupportedException if cloning cannot take place.
     */
    public Object clone() throws CloneNotSupportedException
    {
        Condition cout;
        
        cout           = new Condition();
        cout.attribute = attribute;
        cout.condition = condition;
        cout.dataModel = dataModel;
        cout.value     = value;
        
        return(cout);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            this.attribute = (String)oin.readObject();
            this.condition = oin.readInt();
            this.value     = oin.readObject();
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            oout.writeObject(this.attribute);
            oout.writeInt(this.condition);
            oout.writeObject(this.value);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    /**
     * Make an empty condition
     */
    public Condition()
    {
    }
    
    /**
     * Make a condition, using the given DataModel and parameters of the form :
     * <code>attribute CONDITION value</code>
     * @param _dataModel The datamodel of this condition
     * @param _attribute The name of the attribute.
     * @param _condition Type of condition relation to use (IS_x)
     * @param _value The value involved in this condition.
     */
    public Condition(DataModel _dataModel, String _attribute, int _condition, Object _value)
    {
        dataModel = _dataModel;
        attribute = _attribute;
        condition = _condition;
        value     = _value;
    }    
}
