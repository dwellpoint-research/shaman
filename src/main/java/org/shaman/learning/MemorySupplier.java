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
package org.shaman.learning;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.shaman.dataflow.InputRegistration;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Memory Buffer of Machine Learning Instances</h2>
 * Caches data-vectors in memory. It simply remembers
 * all vectors that are pushed in. Has some methods that
 * can read simple data-sets from file or Database directly
 * into it's memory cache.
 */
// **********************************************************\
// *               Memory Cache for Data                    *
// **********************************************************/
public class MemorySupplier extends Transformation
{
    private DoubleMatrix1D []dinstance;    // The double instances.
    private ObjectMatrix1D []oinstance;    // The object instance.
    private LinkedList     pushed;         // All data that has been pushed in.
    
    // **********************************************************\
    // *                   Flow/Transformation                  *
    // **********************************************************/
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(1); }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Memory Cache Output");
        else           return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 1) return("Data Input");
        else           return(null);
    }
    
    public double getCertainty()
    {
        if ((dinstance != null) || (oinstance != null)) return(1.0);
        else                                            return(0.0);
    }
    
    /**
     * In PUSH mode, the input vector at port 0 is remembered.
     * In PULL mode, this is never called.
     * @throws DataFlowException If there was a problem getting the input at port 0.
     */
    public void transform() throws DataFlowException
    {
        Object in;
        
        in = getInput(0);
        if (in != null)
        {
            dinstance = null;    // Clear the vector arrays
            oinstance = null;
            pushed.addLast(in);  // Remember this vector in the PUSH Buffer.
        }
    }
    
    public void checkDataModelFit(int port, DataModel dm) throws ConfigException
    {
        // All fine.
    }
    
    
    public void init() throws ConfigException
    {
        DataModel  dmsup;
        DataModel  dmin;
        DataModel  dmout;
        InputRegistration ir;
        
        // Empty PUSH Buffer
        pushed    = new LinkedList();
        oinstance = null;
        dinstance = null;
        
        // Set Output DataModel if possible. It's the same as the input model.
        ir = getInputRegistration(0);
        if (ir.getSupplier() != null)
        {
            try
            {
                dmsup = getSupplierDataModel(0);
                dmin  = dmsup;
                dmout = (DataModel)dmin.clone();
                setInputDataModel(0,dmin);
                setOutputDataModel(0, dmout);
            }
            catch(CloneNotSupportedException ex) { throw new DataModelException(ex); }
        }
        else
        {
            throw new ConfigException("Cannot initialize MemorySupplier. No supplier registered at port 0.");
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
        this.dinstance = null;
        this.oinstance = null;
        this.pushed.clear();
    }
    
    // **********************************************************\
    // *                        Data Access                     *
    // **********************************************************/
    /**
     * Gives to stored primitive data as an array of Double vectors
     * @return Array of vectors containing the cached primitive data
     */
    public DoubleMatrix1D []getDoubleInstances()
    {
        if ((this.dinstance == null) && (this.pushed.size() > 0))
            this.dinstance = (DoubleMatrix1D [])this.pushed.toArray(new DoubleMatrix1D[]{});
        
        return(this.dinstance);
    }
    
    /**
     * Gives to stored object data as an array of Object vectors
     * @return Array of vectors containing the cached Object data
     */
    public ObjectMatrix1D []getObjectInstances()
    {
        if ((this.oinstance == null) && (this.pushed.size() > 0))
            this.oinstance = (ObjectMatrix1D [])this.pushed.toArray(new ObjectMatrix1D[]{});
        
        return(this.oinstance);
    }
    
    /**
     * Explicitly provide the double data that should be used by this memory cache.
     * @param din The double vectors to ouput.
     */
    public void setDoubleInstances(DoubleMatrix1D []din)
    {
        dinstance = din;
    }
    
    /**
     * Explicitly provide the Object data that should be used by this memory cache.
     * @param oin The Object vectors to output.
     */
    public void setObjectInstances(ObjectMatrix1D []oin)
    {
        oinstance = oin;
    }
    
    public void reset()
    {
        this.pushed.clear();
        this.dinstance = null;
        this.oinstance = null;
    }
    
    // **********************************************************\
    // *        Load a set of instances from a MNIST file       *
    // **********************************************************/
    /**
     * Load the MNIST data-set. Large handwritten digit recognition set.
     * @param fndata Filename of the MNIST data file.
     * @param fnlabels Filename of the MNIST labels file.
     * @throws LearnerException If there's an I/O exception or the files are in an illegal format.
     */
    public void loadFromMNISTFile(String fndata, String fnlabels) throws LearnerException
    {
        final int BATCHSIZE = 10000;
        int             i,j,ind;
        FileInputStream fdat;
        FileInputStream flab;
        int             numin, intodo, innow;
        int             numrow, numcol, ilen;
        byte            []dbuf;
        byte            []lbuf;
        double          []ibuf;
        byte            []bint = new byte[4];
        
        try
        {
            fdat = new FileInputStream(fndata);
            flab = new FileInputStream(fnlabels);
            
            // Check magic cookies
            fdat.read(bint); if (toInt(bint) != 2051) throw new LearnerException("Data file is not a MNIST images file.");
            flab.read(bint); if (toInt(bint) != 2049) throw new LearnerException("Label file is not a MNIST labels file.");
            
            // Check number of instances match
            fdat.read(bint); numin = toInt(bint);
            flab.read(bint); if (toInt(bint) != numin) throw new LearnerException("Number of instances in data and label files do not match.");
            
            // Read image dimensions
            fdat.read(bint); numrow = toInt(bint);
            fdat.read(bint); numcol = toInt(bint);
            
            // Make space for the instances
            this.dinstance = new DoubleMatrix1D[numin];
            
            // Read the data into instance vectors
            ilen   = numrow*numcol;
            ibuf   = new double[ilen+1];
            lbuf   = new byte[BATCHSIZE];
            dbuf   = new byte[ilen*BATCHSIZE];
            intodo = numin;
            
            System.out.println("Reading "+fndata+" # instances "+numin+" dims "+numrow+"  "+numcol);
            
            ind = 0;
            while (intodo > 0)
            {
                if (intodo > BATCHSIZE) innow = BATCHSIZE;
                else                    innow = intodo;
                
                fdat.read(dbuf, 0, innow*ilen);
                flab.read(lbuf, 0, innow);
                
                for (i=0; i<innow; i++)
                {
                    for (j=0; j<ilen; j++) ibuf[j] = (double)dbuf[(ilen*i)+j];
                    for (j=0; j<ilen; j++) if (ibuf[j] < 0) ibuf[j] = 256 + ibuf[j];
                    ibuf[ilen]          = lbuf[i];
                    this.dinstance[ind] = DoubleFactory1D.dense.make(ibuf);
                    ind++;
                }
                
                intodo -= BATCHSIZE;
                System.out.print(" "+ind); System.out.flush();
            }
            System.out.println();
            
            flab.close();
            fdat.close();
        }
        catch(java.io.IOException ex) { throw new LearnerException(ex);  }
        
    }
    
    private int toInt(byte []bbuf)
    {
        int b3, b2, b1, b0;
        
        if (bbuf[3] >= 0) b3 = (int)bbuf[3]; else b3 = 256 + (int)bbuf[3];
        if (bbuf[2] >= 0) b2 = (int)bbuf[2]; else b2 = 256 + (int)bbuf[2];
        if (bbuf[1] >= 0) b1 = (int)bbuf[1]; else b1 = 256 + (int)bbuf[1];
        if (bbuf[0] >= 0) b0 = (int)bbuf[0]; else b0 = 256 + (int)bbuf[0];
        
        return(b3 | (b2<<8) | (b1<<16) | (b0<<24));
    }
    
    // **********************************************************\
    // *        Load a set of instances from a text file        *
    // **********************************************************/
    /**
     * Imports all data vectors from the given structured file.
     * The input datamodel 0 is a LearnerDataModel describing the Object (String) based data in the file.
     * The output datamodel 0 is a LearnerDataModel describing the (primitive or Object) based vectors to store.
     * @param filename The filename of the file containing the data vectors
     * @param typeLine Is the first line of the file a line containing the names of all fields?
     * @param del The field delimiters to use
     * @param eol The end of line indicators to use
     * @throws LearnerException If something went wrong with the I/O or vector creation.
     */
    public void loadFromTextFile(String filename, boolean typeLine, String del, String eol) throws LearnerException
    {
        loadFromTextFile(filename, typeLine, del, eol, -1);
    }
    
    /**
     * Imports data vectors from the given structured file.
     * Stops when the specified number of records is reached.
     * The input datamodel 0 is a LearnerDataModel describing the Object (String) based data in the file.
     * The output datamodel 0 is a LearnerDataModel describing the (primitive or Object) based vectors to store.
     * @param filename The filename of the file containing the data vectors
     * @param typeLine Is the first line of the file a line containing the names of all fields?
     * @param del The field delimiters to use
     * @param eol The end of line indicators to use
     * @param maxrec The maximal number of lines to import
     * @throws LearnerException If something went wrong with the I/O or vector creation.
     */
    public void loadFromTextFile(String filename, boolean typeLine, String del, String eol, int maxrec) throws LearnerException
    {
        int              i,j;
        String           sf, line;
        StringTokenizer  itok, atok;
        int              numin;
        ObjectMatrix1D   sa;
        DoubleMatrix1D   sad;
        Object           innow;
        Vector           vecin;
        DataModel        dmin;
        DataModel        dmout;
        boolean          stop;
        
        try
        {
            dmin  = (DataModel)getInputDataModel(0);
            dmout = (DataModel)getOutputDataModel(0);
            if (dmin instanceof DataModelDouble)
                throw new LearnerException("Cannot use primitive input data model when loading from text file.");
            
            // Read the entire String into 1 String.
            FileInputStream fin = new FileInputStream(filename);
            byte []bbuf = new byte[fin.available()];
            fin.read(bbuf);
            fin.close();
            sf = new String(bbuf);
            
            // Split up into lines (1 instances per line)
            itok  = new StringTokenizer(sf, eol);
            
            // Make space for the instance data
            vecin   = new Vector();
            
            i = 0;
            // Read the first line
            line = itok.nextToken();
            if (typeLine) line = itok.nextToken();
            while (line.length() < 2) line = itok.nextToken();
            atok = new StringTokenizer(line, del);
            sa   = ObjectFactory1D.dense.make(atok.countTokens());
            
            // Iterate until no more instances left
            stop = false;
            while (itok.hasMoreTokens() && !stop)
            {
                // Parse the line into it's attributes
                for (j=0; j<sa.size(); j++) sa.setQuick(j, atok.nextToken());
                
                // Convert the input into to output model's representation.
                if (dmout instanceof DataModelDouble)
                {
                    sad   = ((DataModelObject)dmin).toDoubleFormat(sa, (DataModelDouble)dmout);
                    innow = sad;
                }
                else
                {
                    ((DataModelObject)dmout).checkFit(sa);
                    innow = sa;
                }
                vecin.add(innow);
                
                // Move on to next line
                line = itok.nextToken();
                while (line.length() < 2) line = itok.nextToken();
                atok = new StringTokenizer(line, del);
                sa   = ObjectFactory1D.dense.make(atok.countTokens());
                i++;
                
                if ((i>0) && (i%10000 == 0)) System.out.println("Read "+i+" records");
                if ((maxrec != -1) && (i == maxrec)) stop = true;
            }
            
            // Copy into the data arrays
            numin     = vecin.size();
            if (dmout instanceof DataModelDouble)
            {
                dinstance = new DoubleMatrix1D[numin];
                vecin.copyInto(this.dinstance);
            }
            else
            {
                oinstance = new ObjectMatrix1D[numin];
                vecin.copyInto(this.oinstance);
            }
            
            System.out.println("Read data from file # "+numin);
        }
        catch(DataModelException  ex)    { throw new LearnerException(ex); }
        catch(ConfigException ex) { throw new LearnerException(ex); }
        catch(java.io.IOException ex)    { throw new LearnerException(ex); }
    }
    
    
    public MemorySupplier()
    {
        super();
        name        = "memorysupplier";
        description = "Automatically Recycling Memory Cache for primitive or Object vectors.";
        
    }
}
