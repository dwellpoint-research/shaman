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
 * <h2>Datamodel for Machine Learning Algorithms</h2>
 * The Structure of a Data Vector used in a machine learning flow. <br>
 * Can be used for primitive or <code>Object</code>based data vectors.
 * Selects the goal attribute.
 * Can make fixed-form datamodels for machine-learning algorithm support.
 */

// **********************************************************\
// *  The Structure of a Data Vector somewhere in a Flow    *
// **********************************************************/
public abstract class DataModel implements Cloneable
{
    /** (Mandatory) Property describing the type of vector associated with the DataModel */
    public static final String PROPERTY_VECTORTYPE = "VectorType";
    /** (Mandotaty) Property for intergration with machine learning algorithms */
    public static final String PROPERTY_LEARNING   = "Learning";
    
    /** The name of this DataModel */
    protected String      name;
    /** Array of Attributes describing the data format of associated data-vectors */
    protected Attribute []attribute; 
    /** Map of properties enriching the structure of this datamodel */
    protected HashMap     properties;
    
    // **********************************************************\
    // *                 DataModel Properties                   *
    // **********************************************************/
    /**
    * Add a property of the attribute to the list.
    * @param key The name of the property.
    * @param value The property itself
    */
    public void addProperty(String key, DataModelProperty value)
    {
        this.properties.put(key, value);
    }

    /**
     * Get the value of the given property.
     * @param key The name of the property.
     * @return The value of the property. <code>null</code> if not present.
     */
    public DataModelProperty getProperty(String key)
    {
        return((DataModelProperty)this.properties.get(key));
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
    
    public DataModelProperty getVectorTypeProperty()
    {
        return(getProperty(PROPERTY_VECTORTYPE));
    }
    
    public DataModelPropertyLearning getLearningProperty()
    {
        return((DataModelPropertyLearning)getProperty(PROPERTY_LEARNING));
    }

    // **********************************************************\
    // *                 Attribute Definition                   *
    // **********************************************************/
    /**
     * Get the number of attributes in this model.
     * @return The number of attributes.
     */
    public int getAttributeCount()
    {
        if (attribute == null) return(0);
        else                   return(attribute.length);
    }

    /**
     * Get the name of the specified attribute.
     * @param index The index of the attribute
     * @return The name of the specified attribute.
     *        <code>null</code> if the index is out of bounds.
     */
    public String getAttributeName(int index)
    {
        if ((index < attribute.length) && (index >= 0)) return(attribute[index].getName());
        else                                            return(null);
    }

    /**
     * Get the name of Java class of the specified attribute
     * @param index The index of the attribute
     * @return The name of the Java class of the specified attribute.
     *         <code>null</code> if the index is out of bounds.
     */
    public String getAttributeType(int index)
    {
        if ((index < attribute.length) && (index >= 0)) return(attribute[index].getRawType());
        else                                            return(null);
    }
   
    /**
     * Get the index of the attribute with the given name
     * @param attname The name of the attribute
     * @return The index of the attribute with the given name. -1 if not found.
     */
    public int getAttributeIndex(String attname)
    {
        int i;
        int ind;

        ind = -1;
        for (i=0; (i<attribute.length) && (ind == -1); i++)
        {
            if (attribute[i].name.equals(attname)) ind = i;
        }

        return(ind);
    }

    /**
     * Get the index of the attribute with the given name in the active attributes.
     * @param name The name of attribute to find
     * @return The index of the attribute in the active attributes.
     *         -1 if not found or if the attribute is not active
     */
    public int getActiveAttributeIndex(String name)
    {
        int i, j, ind;

        ind = -1; j = 0;
        for (i=0; (i<attribute.length) && (ind == -1); i++)
        {
            if (attribute[i].getIsActive())
            {
                if (attribute[i].getName().equals(name)) ind = j;
                j++;
            }
        }

        return(ind);
    } 

    // **********************************************************\
    // *                  Attribute Access                      *
    // **********************************************************/
    /**
     * Get the attribute at the specified index
     * @param ind The index
     * @return The requested attribute.
     *         <code>null</code> if the index is out of bounds.
     */
    public Attribute getAttribute(int ind)
    {
        if ((ind >= 0) && (ind < attribute.length)) return(attribute[ind]);
        else                                        return(null);
    }
    
    /**
     * Change the attribute at the given index.
     * @param ind The index of the attribute to replace.
     * @param attnew The new attribute.
     */
    public void   setAttribute(int ind, Attribute attnew) { attribute[ind] = attnew; }

    /**
     * Get the attribute with the given name.
     * @param name The name of the attribute.
     * @return The attribute with the given name.
     * @throws DataModelException if the attribute with the given name does not exist.
     */
    public Attribute getAttribute(String name) throws DataModelException
    {
        int ind;

        ind = getAttributeIndex(name);
        if (ind == -1) throw new DataModelException("Cannot find the Attribute with name '"+name+"'");
        else return(getAttribute(ind));
    }

    public Attribute []getAttributes() { return(attribute); }
    public void setAttributes(Attribute []attnew) { attribute = attnew; }

    /**
     * Give the indices of all 'active' attributes
     * @return The indices of the active attributes
     */
    public int []getActiveIndices()
    {
        int       []indact;
        int       i, pos;
        int       numact;

        numact = 0;
        for (i=0; i<attribute.length; i++)
        {
            if (attribute[i].getIsActive()) numact++;
        }

        pos    = 0;
        indact = new int[numact];
        for (i=0; i<attribute.length; i++)
        {
            if (attribute[i].getIsActive()) indact[pos++] = i;
        }

        return(indact);
    }

    /**
     * Get the attributes that are 'active'
     * @return The active attributes
     */
    public Attribute []getActiveAttributes()
    {
        Attribute []actatt;
        int       i;
        int       []indact;

        indact = getActiveIndices();
        actatt = new Attribute[indact.length];
        for (i=0; i<indact.length; i++) actatt[i] = attribute[indact[i]];

        return(actatt);
    }

   /**
    * Count the number of 'active' attributes
    * @return The number of active attributes
    */
    public int getNumberOfActiveAttributes()
    {
        int i, naa;

        naa = 0;
        for (i=0; i<attribute.length; i++) if (attribute[i].getIsActive()) naa++;

        return(naa);
    }


    // **********************************************************\
    // *                     Data Access                        *
    // **********************************************************/
    /**
     * Change the name of this datamodel
     * @param _name The new name
     */
    public void   setName(String _name) { name = _name; }

    /**
     * Get the name of this datamodel
     * @return The name of this datamodel
     */
    public String getName()             { return(name); }

    // **********************************************************\
    // *                    Initialization                      *
    // **********************************************************/
    public String toString()
    {
        int          i;
        StringBuffer stout;
        Attribute    attnow;
   
        stout = new StringBuffer();
        stout.append(name+" ("+getAttributeCount()+") Properties : "+this.properties.keySet()+"\n");
        for (i=0; i<attribute.length; i++)
        {
            attnow = attribute[i];
            stout.append("   "+attnow.getName()+"  ... "+attnow.getRawType()+" "+(attnow.getIsActive()?"":"*")+"\n");
        }
   
        return(stout.toString());
    }
    
     public abstract Object clone() throws CloneNotSupportedException;

    /**
     * Make a LearnerDataModel with the given name, the specified number of attribute of
     * for the given dataformat.
     * @param _name The name of the new LearnerDataModel
     * @param _numat The number of attribute to create
     */
    public DataModel(String _name, int _numat)
    {
        this.name       = _name;
        this.properties = new HashMap();
    }
}
