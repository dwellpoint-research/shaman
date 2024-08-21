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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


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
public class DataModelDouble extends DataModel implements Cloneable
{
    // **********************************************************\
    // *                  Detailed Debug Output                 *
    // **********************************************************/
    /**
     * Output the contents of the given vector on standard error.
     * Format output with attributename of object data-types included
     * @param recvec The vector containing data of this datamodel
     */
    public void dumpVector(DoubleMatrix1D recvec)
    {
        int    i;
        
        for (i=0; i<getAttributeCount(); i++)
        {
            double atval = recvec.getQuick(i);
            String statt = "\t"+getAttribute(i).getName()+ "\t\t"+atval;
            System.err.println(statt);
        }
        System.err.println();
    } 

    // **********************************************************\
    // *                 DataModel construction                 *
    // **********************************************************/
    /**
     * Add the given array attribute to this DataModel's Attribute list.
     * Successively adds the attributes using <code>addAttribute</code>
     * @param atts An array of Attribute to add to this DataModel
     * @return This datamodel extended with the given array of Attributes.
     * @throws DataModelException If one of the attributes cannot be added to this datamodel because of data format or other incompatibilities.
     */ 
    public DataModelDouble addAttributes(AttributeDouble []atts) throws DataModelException
    {
        int             i;
        DataModelDouble dmnow;
        
        dmnow = this;
        for (i=0; i<atts.length; i++) dmnow = dmnow.addAttribute(atts[i]);
        
        return(dmnow);
    }
    
    public DataModelDouble addAttribute(AttributeDouble atnew) throws DataModelException
    {
        int maxpos = this.attribute.length;
        return(addAttribute(atnew, maxpos));
    }
    
    /**
     * Add a new attribute at the given position in the attribute list.
     * @param atnew The attribute add join in.
     * @param pos   The position in the attribute list where to join the new attribute in.
     * @throws DataModelException If the cloning of this datamodel fails or the new attribute cannot be added.
     */ 
    public DataModelDouble addAttribute(AttributeDouble atin, int pos) throws DataModelException
    {
        DataModelDouble  dmnew;
        LinkedList       atlist;
        AttributeDouble  []atdonew;
     
        // Check Data Representation Match between the DataModel and the new Attribute
        try
        {
            // Add the Attribute at the right place.  
            dmnew    = (DataModelDouble)this.clone();
            atlist   = new LinkedList(Arrays.asList(dmnew.getAttributes()));
            atlist.add(pos, atin);
            atdonew = (AttributeDouble [])atlist.toArray(new AttributeDouble[]{});
            dmnew.setAttributes(atdonew);
        }
        catch(CloneNotSupportedException ex)
        {
            throw new DataModelException("Cannot clone the DataModel.");
        }
     
        return(dmnew);
    }

    // **********************************************************\
    // *                    Default Vector Creation             *
    // **********************************************************/
    /**
     * Make a vector containing the default values of all it's attributes.
     */
    public DoubleMatrix1D createDefaultVector() throws DataModelException
    {
        int             i;
        AttributeDouble attdo;
        double          donow;
        DoubleMatrix1D  doout;
     
        // Make double vector with the default values of the attributes.
        doout = DoubleFactory1D.dense.make(getAttributeCount());
        for (i=0; i<attribute.length; i++)
        {
            attdo = getAttributeDouble(i);
            donow = attdo.getDefaultValue();
            doout.setQuick(i, donow);
        }
       
        return(doout);
    }

    // **********************************************************\
    // *                DataModel - Data Compatibility          *
    // **********************************************************/
    /**
     * Check if the given double Vector's elements fit with the corresponding Attributes in this DataModel.
     * dataFormat should be DATA_DOUBLE.
     * @param din
     * @return <code>true</code> if the double Vector contains only fitting values.
     * @throws DataModelException if some element's fit cannot be determined.
     */
    public boolean checkFit(DoubleMatrix1D din) throws DataModelException
    {
        int     i;
        boolean fit;

        fit = true;
        for (i=0; (i<attribute.length) && (fit); i++)
        {
            fit = fit && getAttributeDouble(i).checkFit(din.getQuick(i));
        }

        return(fit);
    }

    // **********************************************************\
    // *                 DataFormat convertion                  *
    // **********************************************************/
    /**
     * Convert the given double vector to an Object vector with attributes from the given DataModel.
     * The ObjectDoubleConverter if this datamodel is used to convert from doubles to Objects.
     * @param din The double vector to convert to an Object vector
     * @param dmout The datamodel of the Object vector.
     * @return The Object matrix containint he converted double vector.
     * @throws DataModelException if something goes wrong in converting between the 2 representations.
     */
    public ObjectMatrix1D toObjectFormat(DoubleMatrix1D din, DataModelObject dmout) throws DataModelException
    {
        int             i;
        int             numatt;
        ObjectMatrix1D  om;
        AttributeObject ato;
        Object          ov;

        numatt = din.size();
        om = ObjectFactory1D.dense.make(numatt);
        for (i=0; i<numatt; i++)
        {
            ato = (AttributeObject)dmout.getAttributeObject(i);
            ov  = getAttributeDouble(i).getObjectValue(din.getQuick(i), ato);
            om.setQuick(i, ov);
        }

        return(om);
    }

    // **********************************************************\
    // *                  Attribute Access                      *
    // **********************************************************/
    /**
     * Get the attribute with the given name as a AttributeDouble.
     * @param name The name of the attribute.
     * @return The attribute with the given name.
     * @throws DataModelException if the attribute with the given name does not exist.
     *                            or if the dataFormat is DATA_OBJECT.
     */
    public AttributeDouble getAttributeDouble(String name) throws DataModelException
    {
        int ind;

        ind = getAttributeIndex(name);
        if (ind == -1) throw new DataModelException("Cannot find the AttributeDouble with name '"+name+"'");
        else return(getAttributeDouble(ind));
    }

    /**
     * Get the attribute at the given index as AttributeDouble
     * @param ind The index.
     * @return The attribute at the given index
     * @throws DataModelException if the dataFormat is DATA_OBJECT
     */
    public AttributeDouble getAttributeDouble(int ind) throws DataModelException
    {
        return((AttributeDouble)attribute[ind]);
    }
    
    // **********************************************************\
    // *                    Initialization                      *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        int                       i;
        DataModel                 dmout;
        DataModelPropertyLearning learn, learnout;

        dmout = new DataModelDouble(this.name, this.attribute.length);
        for (i=0; i<attribute.length; i++) dmout.attribute[i] = this.attribute[i];
        dmout.properties = (HashMap)this.properties.clone();
        learn    = getLearningProperty();
        learnout = (DataModelPropertyLearning)learn.clone(dmout);
        dmout.addProperty(PROPERTY_LEARNING, learnout);

        return(dmout);
    }
   
    /**
     * Make a LearnerDataModel with the given name, the specified number of attribute of
     * for the given dataformat.
     * @param _name The name of the new LearnerDataModel
     * @param _numat The number of attribute to create.
     * @param _dataFormat The format of the data that this model describes.
     */
    public DataModelDouble(String _name, int _numat)
    {
        super(_name, _numat);
        
        // Install space for Attributes describing primitive values
        this.attribute = new AttributeDouble[_numat];
        for (int i=0; i<this.attribute.length; i++) this.attribute[i] = new AttributeDouble("Attribute"+i);
        
        // And add the properties defining double vector class and machine learning integration
        addProperty(PROPERTY_VECTORTYPE, DataModelPropertyVectorType.doubleVector);
        addProperty(PROPERTY_LEARNING,   new DataModelPropertyLearning(this));
    }
}
