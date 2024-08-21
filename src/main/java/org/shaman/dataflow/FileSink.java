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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>File Sink</h2>
 * Stores input in file
 */

// **********************************************************\
// *   Data Flow End-point that Stores the Incomming Data   *
// **********************************************************/
public class FileSink extends Transformation
{
    /** The DataModel of the (Object Based) input source. */
    protected DataModelObject dataModelObject;
    /** Names of the Attributes to store in the data container. */
    protected String  []attnames;
    /** Names of the fields in the data container corresponding to the attribute-names */
    protected String  []fieldnames;
    /** Name of the key attribute */
    protected String keyname;
    /** Create a new record if it not there. */
    protected boolean createifnotthere;
    
    // Storage Parameters (file)
    /** Filename of the file in which to store the data.
     *  Stored in a tab-separated files with a typeline containing the field-names and 1 line per record.
     */
    protected String filename;
    /** Raw outputstream coupled with the output file.
     *  Filename gets the priority if both are specified. 
     */
    protected OutputStream fileoutput;
    
    // Stuff made during init()
    // ------------------------
    // DataBase
    protected DataModel         dataModel;    // Output DataSource
    // File
    protected BufferedWriter    fsink;        // File Output Stream
    
    // Shared
    protected int               []attind;     // Indices in 'dataModel' containing the attributes with names in []attNames
    protected int               []attobind;   // Indices in 'dataModelObject' if the Attribute with names in []attNames
    protected int               attkeyind;    // Index in of the Attribute containing the key-field.
    
    // **********************************************************\
    // *                      DataFlow                          *
    // **********************************************************/
    public int getNumberOfInputs()  { return(1); }
    
    public int getNumberOfOutputs() { return(0); }
    
    /**
     * Store the incomming data vectors using the appriopriate data storage interface.
     */ 
    public void transform() throws DataFlowException
    {
        Object in;
        
        // If a Valid Input Vector is available. Store it.
        while (this.areInputsAvailable(0, 1))
        {
            in = this.getInput(0);
            sinkFile(in);
        }
    }
    
    private void sinkFile(Object vec) throws DataFlowException
    {
        int            i;
        StringBuffer   sbuf;
        DoubleMatrix1D vecdo;
        ObjectMatrix1D vecob;
        
        vecdo = null; vecob = null;
        if (isPrimitive())
        {
            try
            {
                vecdo = (DoubleMatrix1D)vec;
                if (hasObjectOutput()) vecob = toObjectVector(vecdo);
            }
            catch(DataModelException ex) { throw new DataFlowException(ex); }
        }
        else
        {
            vecob = (ObjectMatrix1D)vec;
        }
        
        try
        {
            sbuf = new StringBuffer();
            if (hasObjectOutput() || (!isPrimitive()))
            {
                // Convert the sink-attributes to Objects and write out.
                for (i=0; i<attobind.length; i++)
                {
                    sbuf.append(""+vecob.getQuick(attobind[i]));
                    if (i != attobind.length-1) sbuf.append("\t");
                }
            }
            else
            {
                // Write out the primitive doubles
                for (i=0; i<attind.length; i++)
                {
                    sbuf.append(vecdo.getQuick(attind[i]));
                    if (i != attind.length-1) sbuf.append("\t");
                }
            }
            fsink.write(sbuf.toString());
            fsink.newLine();
        }
        catch(java.io.IOException ex) { throw new DataFlowException(ex); }
    }
    
    private ObjectMatrix1D toObjectVector(DoubleMatrix1D vecdo) throws DataModelException
    {
        int              i;
        ObjectMatrix1D   vecob;
        AttributeObject  attob;
        AttributeDouble  attdo;
        double           val;
        Object           valob;
        DataModelObject  dmob;
                
        // Convert the sink attributes to their Object representation. Also the key attribute.
        dmob  = this.dataModelObject;
        vecob = ObjectFactory1D.dense.make(dataModelObject.getAttributeCount());
        for (i=0; i<attind.length; i++)
        {
            attob = dmob.getAttributeObject(attobind[i]);
            attdo = ((DataModelDouble)this.dataModel).getAttributeDouble(attind[i]);
            val   = vecdo.getQuick(attind[i]);
            valob = attdo.getObjectValue(val, attob);
            vecob.setQuick(attobind[i], valob);
        }
        if (keyname != null)
        {
            attob = dmob.getAttributeObject(attkeyind);
            attdo = ((DataModelDouble)this.dataModel).getAttributeDouble(attkeyind);
            val   = vecdo.getQuick(attkeyind);
            valob = attdo.getObjectValue(val, attob);
            vecob.setQuick(attkeyind, valob);
        }
        
        return(vecob);
    }
    
    // **********************************************************\
    // *               Data Sink Connection Cleanup             *
    // **********************************************************/
    public void cleanUp() throws DataFlowException
    {
        try
        {
            if (fsink != null) { fsink.flush(); fsink.close(); }
        }
        catch(java.io.IOException ex) { throw new DataFlowException(ex); }
    }
    
    // **********************************************************\
    // *              Storage Parameter Specification           *
    // **********************************************************/
    /**
     * Specify the DataModel of the data container that is used.
     * @param _primitive If 'true', use the input port's datamodel as storage datamodel.
     *                   else convert the data to the given (Object based) DataModel.
     * @param _dataModelObject An Object-Based DataModel that is used to convert the
     *                         incomming data to before storing it.
     */
    public void setSinkDataModel(DataModelObject _dataModelObject)
    {
        // If not null the given ObjectDataModel is used to convert to before storing.
        dataModelObject = _dataModelObject;
    }
    
    /**
     * Set the Attributes that are stored and the (field) names that are
     * used in the data storage. If not specified, all the datamodel's attributes
     * are sinked.
     * @param _attnames The list of attributes of the input dataModel that are stored.
     * @param _fieldnames The corresponding list of field names to use in the data storage.
     */
    public void setAttributeFields(String []_attnames, String []_fieldnames)
    {
        attnames   = _attnames;
        fieldnames = _fieldnames;
    }
    
    /**
     * Set the attribute that contains the primary key.
     * @param _keyname The name of the key attribute.
     *                 If <code>null</null>, incomming data is are INSERTED into the data container,
     *                 else the data is UPDATED.
     */
    public void setPrimaryKey(String _keyname)
    {
        keyname = _keyname;
    }
    
    /**
     * Set the name of the file to store data in.
     * @param _filename The name of the file to use for data storage.
     */
    public void setFileParameters(String _filename)
    {
        filename = _filename;
    }
    
    /**
     * Set the raw output stream coupled with the output file.
     * @param _fileoutput The outputstream coupled to the output file.
     */ 
    public void setFileParameters(OutputStream _fileoutput)
    {
        fileoutput = _fileoutput;
    }
    
    public String getOutputName(int port)
    {
        return(null);
    }
    
    public String getInputName(int port)
    {
        if   (port == 0) return("Sink Input");
        else return(null);
    }
    
    protected boolean isPrimitive()   { return(this.dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }
    private boolean hasObjectOutput() { return(this.dataModelObject != null); }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    private void initFile() throws ConfigException
    {
        try
        {
            String typeline;
            int    i, numatt;
            
            // Open Sink File
            if      (filename != null) fsink = new BufferedWriter(new FileWriter(filename));
            else if (fileoutput != null)
            {
                fsink = new BufferedWriter(new OutputStreamWriter(fileoutput));
            }
            else throw new ConfigException("Please specify the output filename or -stream");
            
            // Write the typeline
            numatt   = dataModel.getAttributeCount();
            typeline = "";
            for (i=0; i<fieldnames.length; i++)
            {
                typeline += fieldnames[i];
                if (i != numatt-1) typeline += "\t";
            }
            fsink.write(typeline);
            fsink.newLine();
        }
        catch(java.io.IOException ex) { throw new ConfigException(ex); }
    }
    
    public void init() throws ConfigException
    {
        int        i;
        DataModel  dmsup;
        DataModel  dmin;
        int        attnowind;
        
        // Make sure the input is compatible with this transformation's data requirements
        dmsup = getSupplierDataModel(0);
        checkDataModelFit(0, dmsup);
        dmin      = dmsup;
        dataModel = dmin;
        setInputDataModel(0,dataModel);
        
        // There is no output datamodel to make
        if (!isPrimitive() && hasObjectOutput())
            throw new DataModelException("Cannot make a Object based Flow Sink for Object vectors with another DataModel. ");
        
        // If the Attribute->Field translation is not defined. Assume DataModel is default.
        if (attnames == null)
        {
            attnames   = new String[dataModel.getAttributeCount()];
            fieldnames = new String[attnames.length];
            for (i=0; i<attnames.length; i++)
            {
                attnames[i]   = dataModel.getAttributeName(i);
                fieldnames[i] = dataModel.getAttributeName(i);
            }
        } 
        
        // Check if the attributes to sink are really in the datamodels and store their indices.
        attind   = new int[attnames.length];
        if (!isPrimitive() || hasObjectOutput()) attobind = new int[attnames.length];
        for (i=0; i<attnames.length; i++)
        {
            attnowind = dataModel.getAttributeIndex(attnames[i]);
            if (attnowind != -1) attind[i] = attnowind;
            else throw new DataModelException("Cannot find sink-attribute with name '"+attnames[i]+"' in input DataModel");
            if (!isPrimitive() || hasObjectOutput())
            {
                if (!isPrimitive()) attnowind = dataModel.getAttributeIndex(attnames[i]);
                else                attnowind = dataModelObject.getAttributeIndex(attnames[i]);
                if (attnowind != -1) attobind[i] = attnowind;
                else throw new DataModelException("Cannot find sink-attribute with name '"+attnames[i]+"' in Object based DataModel");
            }
        }
        
        // Check if the field-names agree with the attribute names
        if (fieldnames.length != attnames.length) throw new DataModelException("The number of Fields does not agree with the number of attributes.");
        
        // Get the index of the key attribute.
        if (keyname != null)
        {
            if (!isPrimitive() || hasObjectOutput())
            {
                if (!isPrimitive()) attkeyind = dataModel.getAttributeIndex(keyname);
                else                attkeyind = dataModelObject.getAttributeIndex(keyname);
            }
            else
            {
                attkeyind = dataModel.getAttributeIndex(keyname);
            }
            
            if (attkeyind == -1) throw new DataModelException("Cannot find key attribute '"+keyname+"' in DataModel");
        }
        
        // Open the file
        initFile();
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        // It's all fine.
    }
    
    // **********************************************************\
    // *                       Constructor                      *
    // **********************************************************/
    public FileSink()
    {
        super();
        name        = "filesink";
        description = "Saves input to a file";
    }
}
