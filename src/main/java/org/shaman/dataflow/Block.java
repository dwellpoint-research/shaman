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

import java.util.LinkedList;
import java.util.List;

import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.ShamanException;


/**
 * <h2>Data Flow Blocker</h2>
 * Consumes all data of it's supplier without any effect.
 * Can collect all blocked data.
 */

// *********************************************************\
// *                Block Transformation                   *
// *********************************************************/
public class Block extends Transformation
{
    // Remember all Blocked objects until the next clearQueues ?
    private boolean    collect;
    // The List of Blocked Objects.  
    private LinkedList collectList;  
    
    // **********************************************************\
    // *                  Block Configuration                   *
    // **********************************************************/
    public List getBlockedData() { return(collectList); }
    
    public void clearBlockedData() 
    {
        this.collectList.clear();
        this.collectList = new LinkedList();
    }
    
    public void setCollect(boolean collect)
    {
        this.collect = collect;
    }
    
    public boolean getCollect() { return(this.collect); }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void clear()
    {
        super.clear();
        if (collectList != null) collectList.clear();
    }
    
    /**
     * Don't do anyhting. Block the Data Flow.
     * @throws ShamanException If there's a problem using the Flow base-class.
     */
    public Object []transform(Object in) throws DataFlowException
    {
        // Add the data to the collect list if asked.
        if (collect)
        {
            this.collectList.addLast(in);
        }
        
        // Nothing there...
        return(null);
    }
    
    public void init() throws ConfigException
    {
        // All input is fine.
        setSupplierAsInputDataModel(0);
        
        // Start a List if the Blocked data should be remembered
        if (collect) this.collectList = new LinkedList();
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Block Input");
        else return(null);
    }
    
    public int getNumberOfInputs()  { return(1); }
    public int getNumberOfOutputs() { return(0); }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws ConfigException
    {
    }
    
    public Block()
    {
        super();
        name        = "Block";
        description = "Data Flow Blocking Transformation";
        
        // Default non-collection
        this.collect = false;
    }
}