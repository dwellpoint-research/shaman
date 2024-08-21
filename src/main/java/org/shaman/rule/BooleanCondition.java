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

import java.util.HashSet;

import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Boolean Condition</h2>
 * Evaluates a boolean condition on some attributes.
 */

// *********************************************************\
// *                        Condition                      *
// *********************************************************/
public class BooleanCondition
{
    public static final int TYPE_LEAF       = 0;     // Boolean attribute
    public static final int TYPE_LEAF_VALUE = 1;     // Boolean expression (attribute == value) 
    public static final int TYPE_COMBINED   = 2;     // (Condition1) predicate (Condition2) 
    public static final int TYPE_TRUE       = 3;     // 'true'  constant
    public static final int TYPE_FALSE      = 4;     // 'false' constant
    
    public static final int PREDICATE_AND  = 0;      // && predicate
    public static final int PREDICATE_OR   = 1;      // || predicate
    public static final int PREDICATE_NOT  = 2;      //  ! predicate
    public static final int PREDICATE_NONE = -1;     // ?
    
    private String           name;        // A free-form name for debugging purposes
    
    private DataModelObject  dataModel;
    private int              type;        
    private String           attribute;   // If LEAF       then return attribute.
    private Object           value;       // If LEAF_VALUE then return (attribute == value)
    
    private BooleanCondition cond1;       // If COMBINED   then return (cond1 predicate cond2)
    private BooleanCondition cond2;
    private int              predicate;   // AND / OR predicate
    
    // The attribute's index
    private int              attind;      // The attribute's index in the datamodel
    
    // **********************************************************\
    // *                 Condition Configuration                *
    // **********************************************************/
    public void makeLeaf(String attribute) throws DataModelException
    {
        this.type      = TYPE_LEAF;
        this.attribute = attribute;
    }
    
    public void makeLeafValue(String attribute, Object value) throws DataModelException
    {
        this.type      = TYPE_LEAF_VALUE;
        this.attribute = attribute;
        this.value     = value;
    }
    
    public void makeCombinedCondition(BooleanCondition cond1, BooleanCondition cond2, int predicate)
    {
        this.type      = TYPE_COMBINED;
        this.cond1     = cond1;
        this.cond2     = cond2;
        this.predicate = predicate;
    }
    
    public void makeNotCondition(BooleanCondition cond)
    {
        this.type      = TYPE_COMBINED;
        this.cond1     = cond;
        this.cond2     = null;
        this.predicate = PREDICATE_NOT;
    }
    
    public void makeTrue()
    {
        this.type = TYPE_TRUE;
    }
    
    public void makeFalse()
    {
        this.type = TYPE_FALSE;
    }
    
    // **********************************************************\
    // *       Set the DataModel. Find Leaf positions.          *
    // **********************************************************/
    public void setDataModel(DataModelObject dm) throws ConfigException
    {
        this.dataModel = dm;
        
        if ((this.type == TYPE_LEAF) | (this.type == TYPE_LEAF_VALUE))
        {
            this.attind = dm.getAttributeIndex(this.attribute);
            if (attind == -1) throw new DataModelException("Cannot find leaf attribute '"+this.attribute+"'");
        }
        else if (this.type == TYPE_COMBINED)
        {
            if (this.cond1 != null) this.cond1.setDataModel(dm);
            if (this.cond2 != null) this.cond2.setDataModel(dm);
        }
    }
    
    public DataModelObject getDataModel()
    {
        return(dataModel);
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return(this.name);    
    }
    
    // **********************************************************\
    // *                    Condition Checking                  *
    // **********************************************************/
    public boolean apply(ObjectMatrix1D vec) throws LearnerException
    {
        boolean fire;
        Double  value;
        
        if       (this.type == TYPE_LEAF)
        {
            value = (Double)vec.getQuick(attind);
            if (value.doubleValue() == 0.0) fire = false;
            else                            fire = true;
        }
        else if (this.type == TYPE_COMBINED)
        {
            if      (this.predicate == PREDICATE_AND) fire =  cond1.apply(vec) && cond2.apply(vec);
            else if (this.predicate == PREDICATE_OR)  fire =  cond1.apply(vec) || cond2.apply(vec);
            else if (this.predicate == PREDICATE_NOT) fire = !cond1.apply(vec);
            else throw new LearnerException("Unknown predicate encountered");
        }
        else if (this.type == TYPE_LEAF_VALUE)
        {
            Object  obat;
            
            obat = vec.getQuick(attind);
            if      (this.value instanceof HashSet)   // Set means CONTAINS
            {
                fire = ((HashSet)this.value).contains(obat);
            }
            else if (obat instanceof HashSet)         //     both ways.
            {
                fire = ((HashSet)obat).contains(this.value);
            }
            else                                      // Non-set means EQUALS
            {
                if (obat.equals(this.value)) fire = true;
                else                         fire = false;
            }
        }
        else if (this.type == TYPE_TRUE)  fire = true;
        else if (this.type == TYPE_FALSE) fire = false;
        else fire = false;
        
        return(fire);
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
        String cond;
        
        cond = "";
        if      (this.type == TYPE_LEAF) cond = attribute;
        else if (this.type == TYPE_LEAF_VALUE)
        {
            if (this.value instanceof HashSet)
            {
                cond = this.value+" contains "+attribute;
            }
            else cond = attribute+" == "+this.value;
        }
        else if (this.type == TYPE_COMBINED)
        {
            if      (this.predicate == PREDICATE_OR)  cond = "("+cond1.toString()+" || "+cond2.toString()+")";
            else if (this.predicate == PREDICATE_AND) cond = "("+cond1.toString()+" && "+cond2.toString()+")";
            else if (this.predicate == PREDICATE_NOT) cond = "!("+cond1.toString()+")";
        }
        else if (this.type == TYPE_TRUE)  cond = "true";
        else if (this.type == TYPE_FALSE) cond = "false";
        
        return(cond);
    }
    
    /**
     * Make a boolean condition
     */
    public BooleanCondition()
    {
    }
    
}
