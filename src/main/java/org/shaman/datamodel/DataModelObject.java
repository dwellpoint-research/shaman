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
 *
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// *  The Structure of a Data Vector somewhere in a Flow    *
// **********************************************************/
public class DataModelObject extends DataModel implements Cloneable
{
    // **********************************************************\
    // *                  Detailed Debug Output                 *
    // **********************************************************/
    /**
     * Output the contents of the given vector on standard error.
     * Format output with attributename of object data-types included
     * @param recvec The vector containing data of this datamodel
     */
    public void dumpVector(ObjectMatrix1D recvec)
    {
        int    i;
        String statt;
        
        for (i=0; i<getAttributeCount(); i++)
        {
            if (i < recvec.size())
            {
                Object atval = recvec.getQuick(i);
                statt = "\t"+getAttribute(i).getName()+ "\t\t"+atval;
                if (atval != null) statt += " \t\t "+recvec.getQuick(i).getClass().getName();
                else               statt += " \t\t null";
            }
            else
            {
                statt = "\t"+getAttribute(i).getName()+ "\t\t***OUT OF BOUNDS ***";
            }
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
    public DataModelObject addAttributes(AttributeObject []atts) throws DataModelException
    {
        int             i;
        DataModelObject dmnow;
        
        dmnow = this;
        for (i=0; i<atts.length; i++) dmnow = dmnow.addAttribute(atts[i]);
        
        return(dmnow);
    }
    
    public DataModelObject addAttribute(AttributeObject atnew) throws DataModelException
    {
        return(addAttribute(atnew, this.attribute.length));
    }
   
    /**
     * Add a new attribute at the given position in the attribute list.
     * @param atnew The attribute add join in.
     * @param pos   The position in the attribute list where to join the new attribute in.
     * @throws DataModelException If the cloning of this datamodel fails or the new attribute cannot be added.
     */ 
    public DataModelObject addAttribute(AttributeObject atnew, int pos) throws DataModelException
    {        
        DataModelObject  dmnew;
        LinkedList       atlist;
        AttributeObject  []atobnew;
     
        try
        {
             // Add the Attribute at the right place.  
             dmnew    = (DataModelObject)this.clone();
             atlist   = new LinkedList(Arrays.asList(dmnew.getAttributes()));
             atlist.add(pos, atnew);
             atobnew = (AttributeObject [])atlist.toArray(new AttributeObject[]{});
             dmnew.setAttributes(atobnew);
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
    public ObjectMatrix1D createDefaultVector() throws DataModelException
    {
        int             i;
        AttributeObject attob;
        Object          obnow;
        ObjectMatrix1D  obout;

        // Make default objects for the entire vector.
        obout = ObjectFactory1D.dense.make(getAttributeCount());
        for (i=0; i<attribute.length; i++)
        {
            attob = getAttributeObject(i);
            obnow = attob.getDefaultObject();
            obout.setQuick(i, obnow);
        }

        return(obout);
    }

    // **********************************************************\
    // *                DataModel - Data Compatibility          *
    // **********************************************************/
    /**
     * Check if the given Object Vector's elements fit with the corresponding Attributes in this DataModel.
     * dataFormat should be DATA_OBJECT.
     * @param oin The Object vector.
     * @return <code>true</code> if the Object Vector contains only fitting values.
     * @throws DataModelException if some element's fit cannot be determined.
     */
    public boolean checkFit(ObjectMatrix1D oin) throws DataModelException
    {
        int     i;
        boolean fit;

        fit = true;
        for (i=0; (i<attribute.length) && (fit); i++)
        {
            fit = fit && getAttributeObject(i).checkFit(oin.getQuick(i));
        }

        return(fit);
    }

    // **********************************************************\
    // *                 DataFormat convertion                  *
    // **********************************************************/
    /**
     * Convert the given Object vector to a double vector with attributes from the given DataModel.
     * The ObjectDoubleConverter if this datamodel is used to convert from Objects to doubles.
     * @param oin The Object vector to convert to a double vector
     * @param dmout The datamodel of the double vector.
     * @return The double vector containing the converted Object vector.
     * @throws DataModelException if something goes wrong in converting between the 2 representations.
     */
    public DoubleMatrix1D toDoubleFormat(ObjectMatrix1D oin, DataModelDouble dmout) throws DataModelException
    {
        int             i;
        int             numatt;
        AttributeDouble atd;
        DoubleMatrix1D  dm;
        double          dv;

        numatt = oin.size();
        dm = DoubleFactory1D.dense.make(numatt);
        for (i=0; i<numatt; i++)
        {
            atd = (AttributeDouble)dmout.getAttributeDouble(i);
            dv  = getAttributeObject(i).getDoubleValue(oin.getQuick(i), atd);
            dm.setQuick(i, dv);
        }

        return(dm);
    }

    // **********************************************************\
    // *                  Attribute Access                      *
    // **********************************************************/
    /**
     * Get the attribute with the given name as a AttributeObject.
     * @param name The name of the attribute.
     * @return The attribute with the given name.
     * @throws DataModelException if the attribute with the given name does not exist.
     *                            or if the dataFormat is DATA_DOUBLE.
     */
    public AttributeObject getAttributeObject(String name) throws DataModelException
    {
        int ind;

        ind = getAttributeIndex(name);
        if (ind == -1) throw new DataModelException("Cannot find the AttributeObject with name '"+name+"'");
        else return(getAttributeObject(ind));
    }

    /**
     * Get the attribute at the given index as AttributeObject
     * @param ind The index.
     * @return The attribute at the given index
     * @throws DataModelException if the dataFormat is DATA_DOUBLE
     */
    public AttributeObject getAttributeObject(int ind) throws DataModelException
    {
        return((AttributeObject)attribute[ind]);
    }

    // **********************************************************\
    // *                    Initialization                      *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        int                       i;
        DataModelObject           dmout;
        DataModelPropertyLearning learn, learnout;

        dmout                 = new DataModelObject(name, attribute.length);
        for (i=0; i<attribute.length; i++) dmout.attribute[i] = attribute[i];
        dmout.properties = (HashMap)this.properties.clone();
        learn    = getLearningProperty();
        learnout = (DataModelPropertyLearning)learn.clone(dmout);
        dmout.addProperty(PROPERTY_LEARNING, learnout);
        dmout.addProperty(PROPERTY_VECTORTYPE, getProperty(PROPERTY_VECTORTYPE));

        return(dmout);
    }
   
    /**
     * Make a LearnerDataModel with the given name, the specified number of attribute of
     * for the given dataformat.
     * @param _name The name of the new LearnerDataModel
     * @param _numat The number of attribute to create.
     * @param _dataFormat The format of the data that this model describes.
     */
    public DataModelObject(String _name, int _numat)
    {
        super(_name, _numat);
        
        // Make space for Attribute describing Objects
        this.attribute = new AttributeObject[_numat];
        for (int i=0; i<this.attribute.length; i++) this.attribute[i] = new AttributeObject("Attribute"+i);
        
        // And add the properties defining double vector class and machine learning integration
        addProperty(PROPERTY_VECTORTYPE, DataModelPropertyVectorType.objectVector);
        addProperty(PROPERTY_LEARNING,   new DataModelPropertyLearning(this));
    }
}
