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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.ShamanException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>File Source</h2>
 * Reads data from a file on demand and outputs it
 * to it's single output port.
 */

// **********************************************************\
// *                 Read Data from a File                  *
// **********************************************************/
public class FileSource extends Transformation
{
    /** The DataModel of the external data source's (Object based) data vectors */
    protected DataModelObject dataModelSource;
    /** The Output DataModel */
    protected DataModel       dataModel;   
    
    /** The filename of the external data-source file */
    protected String      filename;
    /** Is there a (1st) line containing the field-names in the file? */
    protected boolean     typeLine;
    /** What are the attribute delimiters of the file */
    protected String      dels;
    /** The raw inputstream pointing to the file. Filename gets the priority if both are specified. */
    protected InputStream fileinput;
    
    // **** Run-time Data ***
    protected boolean sourceDry;
    protected int   []attind;      // Attribute indices of the fields in the DB
    protected int   []attindsrc;
    
    // File Input
    protected LineNumberReader  fsource;     // The File from which to read data-vectors
    
    // **********************************************************\
    // *               Atypical Flow Methods                    *
    // **********************************************************/
    public int getNumberOfInputs()  { return(0); }
    
    public int getNumberOfOutputs() { return(1); }
    
    /**
     * Get one data-vector from the external input.
     * Push it out of the output port.
     * Automatically re-start when the source runs dry (and this method returns <code>false</code>)
     * @return <code>true</code> if a data-vector was found and pushed.
     * @throws DataFlowException If getting the data or pushing it failed.
     */
    public boolean push() throws DataFlowException
    {
        // Get one data vector vector from the source and push to output.
        transform();
        
        if (!this.sourceDry) return(true);
        else                 return(false);
    }
    
    /**
     * Get 1 data-vector from the external input.
     * Add it to the output queue.
     * @throws DataFlowException If getting the data from the external source or putting it in the output failed.
     */
    public void transform() throws DataFlowException
    {
        Object out;
        
        out = null;
        
        // If the source is not dry
        if (!this.sourceDry)
        {
            try
            {
                // Read another record.
                out = sourceFile();
            }
            catch(DataModelException ex) { throw new DataFlowException(ex); }
            
            // If nothing left to read...
            if (out == null)
            {
                try
                {
                    // Stop and Start again. Automatic re-Winding.
                    cleanUp();
                    init();
                    this.sourceDry = true;
                }
                catch(ConfigException ex) { throw new DataFlowException(ex); }
            }
        }
        
        // Output the next record
        if (out != null) setOutput(0, out);
    }
    
    protected Object sourceFile() throws DataFlowException, DataModelException
    {
        int             i, numatt;
        Object          out;
        String          line;
        String          toknow;
        StringTokenizer ltok;
        ObjectMatrix1D  vecob;
        DoubleMatrix1D  vecdo;
        
        out = null;
        try
        {
            // Make a new Object vector containing the data from this line.
            numatt = dataModel.getAttributeCount();
            line   = fsource.readLine();
            if (line != null)
            {
                ltok   = new StringTokenizer(line, dels);
                vecob  = ObjectFactory1D.dense.make(numatt);
                i      = 0;
                while((i<numatt) && (ltok.hasMoreTokens()))
                {
                    toknow = (String)ltok.nextToken();
                    vecob.setQuick(i, toknow);
                    i++;
                }
                
                // Check if the number of attributes agrees
                if (ltok.hasMoreTokens())
                {
                    throw new DataFlowException("Number of attribute in datamodel and in current record do not agree.");
                }
                
                if (!isPrimitive()) // Object Based output
                {
                    // Check fit of the new data vector.
                    forceStringFit(dataModel, vecob);
                    ((DataModelObject)this.dataModel).checkFit(vecob);
                    out = vecob;
                }
                else // Primitive Output
                {
                    // Check fit, convert to double vector and check fit as double vector also.
                    forceStringFit(dataModel, vecob);
                    dataModelSource.checkFit(vecob);
                    vecdo = dataModelSource.toDoubleFormat(vecob, (DataModelDouble)this.dataModel);
                    ((DataModelDouble)this.dataModel).checkFit(vecdo);
                    out   = vecdo;
                }
            }
            else out = null;
        }
        catch(java.io.IOException ex) { throw new DataFlowException(ex); }
        
        return(out);
    }
    
    private void forceStringFit(DataModel dm, ObjectMatrix1D obin)
    {
        int       i;
        Attribute []att;
        String    stnow;
        Object    obnow;
        String    rawtype;
        
        att = dm.getAttributes();
        for (i=0; i<att.length; i++)
        {
            stnow   = (String)obin.getQuick(i);
            obnow   = stnow;
            rawtype = att[i].getRawType();
            
            if      (rawtype.equals("java.lang.Double"))    obnow = new Double(stnow);
            else if (rawtype.equals("java.lang.Integer"))   obnow = new Integer(stnow);
            else if (rawtype.equals("java.lang.Long"))      obnow = new Long(stnow);
            else if (rawtype.equals("java.lang.Float"))     obnow = new Float(stnow);
            else if (rawtype.equals("java.lang.Short"))     obnow = new Short(stnow);
            else if (rawtype.equals("java.lang.Character")) obnow = new Character(stnow.charAt(0));
            else if (rawtype.equals("java.lang.Byte"))      obnow = new Byte(stnow);
            else if (rawtype.equals("java.lang.Boolean"))   obnow = new Boolean(stnow);
            obin.setQuick(i, obnow);
        }
    }
    
    // **********************************************************\
    // *             Data Source Connection Cleanup             *
    // **********************************************************/
    /**
     * Close connection to external input source.
     * @throws ShamanException If closing the connection failed.
     */
    public void cleanUp() throws DataFlowException
    {
        cleanUpFile();
    }
    
    protected void cleanUpFile() throws DataFlowException
    {
        try
        {
            if (fsource != null) { this.fsource.close(); }
        }
        catch(java.io.IOException ex) { throw new DataFlowException(ex); }
    }
    
    // **********************************************************\
    // *              Source Parameter Specification            *
    // **********************************************************/
    /**
     * Specify which DataModels. If _dataModel is for primitive data,
     *    produces primitive data-vectors of type <code>_dataModel</code>
     *    by converting the Object based input vector of type <code>_dataModelSource</code> to primitives.
     *    Else produce Object based data-vectors of type  <code>_dataModel</code>.
     * @param _dataModelSource The (Object based) dataModel of the input.
     * @param _dataModel The (primitive or Object based) dataModel of the output.
     */
    public void setSourceDataModels(DataModelObject _dataModelSource, DataModel _dataModel)
    {
        dataModel       = _dataModel;       // The output datamodel
        dataModelSource = _dataModelSource; // The datamodel of the source.
    }
    
    protected boolean isPrimitive() { return(this.dataModel.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector)); }
   
    /**
     * Set the external file DataSource parameters.
     * @param _filename Name of the file containing the data
     * @param _typeLine The file contains (the first line) a line with the names of the attributes
     * @param _dels The attribute data delimiter character(s)
     */
    public void setFileParameters(String _filename, boolean _typeLine, String _dels)
    {
        filename = _filename;
        typeLine = _typeLine;
        dels     = _dels;
    }
    
    /**
     * Set the external file DataSource parameters.
     * @param _fileinput An input stream on a file
     * @param _typeLine The file contains (the first line) a line with the names of the attributes
     * @param _dels The attribute data delimiter character(s)
     */
    public void setFileParameters(InputStream _fileinput, boolean _typeLine, String _dels)
    {
        fileinput = _fileinput;
        typeLine  = _typeLine;
        dels      = _dels;
    }
    
    public String getInputName(int port)
    {
        return(null);
    }
    
    public String getOutputName(int port)
    {
        if   (port == 0) return("Source Output");
        else return(null);
    }
    
    // **********************************************************\
    // *                       Construction                     *
    // **********************************************************/
    protected void initFile() throws ConfigException
    {
        try
        {
            String          line;
            StringTokenizer ltok;
            ArrayList       types;
            String          tnow;
            int             i;
            
            // Open Source File
            if      (filename != null) fsource  = new LineNumberReader(new BufferedReader(new FileReader(filename)));
            else if (fileinput != null)
            {
                fsource = new LineNumberReader(new BufferedReader(new InputStreamReader(fileinput)));
            }
            else throw new ConfigException("Please specify file or inputstream for input");
            
            if (typeLine)
            {
                // Read the typeline if there
                line  = fsource.readLine();
                ltok  = new StringTokenizer(line, dels);
                types = new ArrayList();
                while (ltok.hasMoreTokens()) { types.add(ltok.nextToken()); }
                
                // Check if the fields are presents as attributes in the datamodel(s)
                for (i=0; i<types.size(); i++)
                {
                    tnow = (String)types.get(i);
                    if (dataModel.getAttributeIndex(tnow) == -1)
                        throw new DataModelException("Cannot find attribute with name '"+tnow+" in output DataModel.");
                    if (isPrimitive())
                    {
                        if (dataModelSource.getAttributeIndex(tnow) == -1)
                            throw new DataModelException("Cannot find attribute with name '"+tnow+"' in Source DataModel.");
                    }
                }
                if                   (types.size() != dataModel.getAttributeCount())
                    throw new DataModelException("Number of fields in the file and number of attributes in the output DataModel do not agree.");
                if (isPrimitive() && (types.size() != dataModelSource.getAttributeCount()))
                    throw new DataModelException("Number of fields in the file and number of attributes in the Source DataModel do not agree.");
            }
        }
        catch(java.io.IOException ex) { throw new ConfigException(ex); }
    }
    
    public void init() throws ConfigException
    {
        // No inputs to check.
        
        // Set output model
        setOutputDataModel(0, this.dataModel);
        
        // Open connection to data storage...
        initFile();
        
        // Initialize source state
        sourceDry = false;
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws DataModelException
    {
        ; // All is fine. There's no input.
    }
    
    // **********************************************************\
    // *                      Constructor                       *
    // **********************************************************/
    public FileSource()
    {
        super();
        name        = "filesource";
        description = "Reads data from files";
    }
}
