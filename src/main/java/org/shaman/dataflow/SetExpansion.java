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
package org.shaman.dataflow;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Set Attribute Expansion</h2>
 * Iterates over the elements of an attribute containing
 * a set. The Set attribute is replaced by the current
 * Set-element.
 * Optionally other attributes containing Maps are used
 * for lookups with key the current Set-element. These
 * attributes are replace by the result of the lookup.
 * After every iteration, the vector is output containing
 * the replaced attribute values.
 */

// *********************************************************\
// *                Set Attribute Expansion                *
// *********************************************************/
public class SetExpansion extends Transformation
{
    private String   setName;          // The name  of the Attribute containing the (Key-)Set
    private String []mapName;          // The names of the Attributes containing the Maps indexed by the Set-elements.
    
    // Created by Init()
    private int             indSet;
    private int           []indMap;
    private DataModelObject dataModel;
    
    // **********************************************************\
    // *                 Parameter Specification                *
    // **********************************************************/
    public void setSetAttribute(String setName)
    {
        this.setName = setName;
    }
    
    public String getSetAttribute()
    {
        return(this.setName);
    }
    
    public void setMapAttributes(String []mapName)
    {
        this.mapName = mapName;
    }
    
    public String []getMapAttributes()
    {
        return(this.mapName);
    }
    
    // **********************************************************\
    // *                    Data Flow Method                    *
    // **********************************************************/
    private void expandSet(ObjectMatrix1D invec) throws DataFlowException
    {
        Object obset;
        
        obset = (Set)invec.getQuick(this.indSet);
        if (obset instanceof Set)
        {
            Set            set;
            Iterator       itel;
            Object         elnow, obmap, valnow;
            Map            mapnow;
            ObjectMatrix1D vecout;
            int            i;
            
            // Iterate over all elements in the Set.
            set  = (Set)obset;
            itel = set.iterator();
            while(itel.hasNext())
            {
                elnow  = itel.next();
                vecout = invec.copy();
                
                // Set the current element in stead of the entire set.
                vecout.setQuick(this.indSet, elnow);
                
                // Replace Map attributes by the value obtained by doing a lookup with the current element
                for (i=0; i<this.indMap.length; i++)
                {
                    obmap = invec.getQuick(this.indMap[i]);
                    if (obmap instanceof Map)
                    {
                        // Do Lookup in the cureent Map. If not found use the default object...
                        mapnow = (Map)obmap;
                        valnow = mapnow.get(elnow);
                        if (valnow == null)
                        {
                            try
                            {
                                valnow = this.dataModel.getAttributeObject(this.indMap[i]).getDefaultObject();
                            }
                            catch(DataModelException ex) { throw new TransformationException(ex); }
                        }
                        vecout.setQuick(this.indMap[i], valnow);
                    }
                    else throw new TransformationException("Found non-Map in Attribute '"+this.mapName[i]+"'. Type of incomming Object is "+obmap.getClass().getName());
                }
                
                // Output the modified vector
                setOutput(0, vecout);
            }
        }
        else throw new TransformationException("Found non-Set in Attribute '"+this.setName+"'. Type of incomming object is '"+obset.getClass().getName()+"'.");
        
    }
    
    public void transform() throws DataFlowException
    {
        while (areInputsAvailable(0, 1))
        {
            Object         inob;
            
            inob = getInput(0);
            expandSet((ObjectMatrix1D)inob);
        }
    }
    
    // **********************************************************\
    // *               Transformation Interface                 *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmin, dmout;
        
        // Make sure there's a set on the specified attribute
        dmin = getSupplierDataModel(0);
        checkDataModelFit(0, dmin);
        this.dataModel = (DataModelObject)dmin;
        
        // Output model is input model with a free-from Object attribute in stead of the given Set and Map attributes.
        dmout = makeOutputDataModel(this.dataModel, indSet);
        
        // Remember the indices of the Set and Map attributes
        this.indSet = dmin.getAttributeIndex(this.setName);
        this.indMap = new int[this.mapName.length];
        for (i=0; i<this.indMap.length; i++)
        {
            this.indMap[i] = dmin.getAttributeIndex(this.mapName[i]);
        }
        
        // Install the output datamodel.
        setOutputDataModel(0, dmout);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    private DataModel makeOutputDataModel(DataModelObject dataModel, int indSet) throws DataModelException
    {
        AttributeObject  atset, atmap;
        DataModelObject  dmout;
        AttributeObject  atob;
        int              i, indnow;
        
        dmout = null;
        
        try
        {
            dmout = (DataModelObject)dataModel.clone();
            
            // Replace the Set attribute with a free-form Object Attribute with the same name
            indnow = dataModel.getAttributeIndex(this.setName);
            atset  = dataModel.getAttributeObject(indnow);
            atob   = new AttributeObject();
            atob.initAsFreeObject("java.lang.Object");
            atob.setName(atset.getName());
            atob.setIsActive(atset.getIsActive());
            dmout.setAttribute(indnow, atob);
            
            // Same for the Map attributes
            for (i=0; i<this.mapName.length; i++)
            {
                indnow = dataModel.getAttributeIndex(this.mapName[i]);
                atmap  = dataModel.getAttributeObject(indnow);
                atob   = new AttributeObject();
                atob.initAsFreeObject("java.lang.Object");
                atob.setName(atmap.getName());
                atob.setIsActive(atmap.getIsActive());
                dmout.setAttribute(indnow, atob);
            }
        }
        catch(CloneNotSupportedException ex)
        {
            throw new DataModelException(ex);
        }
        
        return(dmout);
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        int              indSet;
        AttributeObject  atset, atmap;
        DataModelObject  dmin;
        
        // Only valid for Object based data
        if (!dataModel.getProperty(DataModel.PROPERTY_VECTORTYPE).equals(DataModelPropertyVectorType.objectVector))
            throw new DataModelException("Can only handle Object based input");
        dmin = (DataModelObject)dataModel;
        
        // Find the Set attribute and check it's format.
        indSet = dataModel.getAttributeIndex(this.setName);
        if (indSet == -1)
            throw new DataModelException("Cannot find Set attribute with name '"+this.setName+"'");
        else
        {
            atset = dmin.getAttributeObject(indSet);
            try
            {
                String setclassname;
                Class  setclass;
                
                setclassname = atset.getRawType();
                setclass     = Class.forName(setclassname);
                if (!java.util.Set.class.isAssignableFrom(setclass))
                    throw new DataModelException("Expected 'java.util.Set' for attribute '"+this.setName+"' its type but found '"+atset.getRawType()+"'");
            }
            catch(ClassNotFoundException ex) { throw new DataModelException(ex); }
        }
        
        // Find the Map attributes and check their format
        int i;
        int indMap;
        
        for (i=0; i<this.mapName.length; i++)
        {
            indMap = dataModel.getAttributeIndex(this.mapName[i]);
            if (indMap == -1)
                throw new DataModelException("Cannot find Map attribute with name '"+this.mapName[i]+"'");
            else
            {
                try
                {
                    String mapclassname;
                    Class  mapclass;
                    
                    atmap        = dmin.getAttributeObject(indMap);
                    mapclassname = atmap.getRawType();
                    mapclass     = Class.forName(mapclassname);
                    
                    if (!java.util.Map.class.isAssignableFrom(mapclass))
                        throw new DataModelException("Expected 'java.util.Map' for attribute '"+this.mapName[i]+"' its type buf found '"+atmap.getRawType()+"'");
                }
                catch(ClassNotFoundException ex) { throw new DataModelException(ex); }
            }
        }
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Vector Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Vector Input");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    
    
    public SetExpansion()
    {
        super();
        name        = "Identity";
        description = "Identity Transformation";
        
        this.mapName = new String[]{};
    }
}