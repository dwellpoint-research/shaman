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

import org.shaman.dataflow.Block;
import org.shaman.dataflow.FileSource;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.rule.ExpressionRouter;

import junit.framework.TestCase;


/**
 * Expression based Data-flow Logic Test
 */
public class ExpressionRouterTest extends TestCase
{
    public static void testExpressionRouter() throws ShamanException
    {
        DataModelObject     dmob;
        DataModelDouble     dmdo;
        MemorySupplier       ms = new MemorySupplier();
        FileSource         fsrc = new FileSource();
        ExpressionRouter     er = new ExpressionRouter(); er.grow(1);
        Block             block = new Block();
        
        fsrc.registerConsumer(0,  er, 0);
        er.registerSupplier(0,  fsrc, 0);
        er.registerConsumer(0, block, 0);
        block.registerSupplier(0,  er, 0);
        
        // Read machine-learning data-set from file.
        TestSets.loadCancer(ms, true, true);
        dmob = (DataModelObject)ms.getInputDataModel(0);
        dmdo = (DataModelDouble)ms.getOutputDataModel(0);
        
        fsrc.setFileParameters("./src/main/resources/data/cancer.txt", false, ",");
        fsrc.setSourceDataModels(dmob, dmdo);
        fsrc.init();
        
        // Calculate the average of the fields. Route over average to Block.
        String []exp = new String[]{
                "((Field2 + Field3 + Field4 + Field5 + Field6 + Field7 + Field8 + Field9) / 8) >= 3.70"};
        er.setExpressions(exp);
        er.init();
        
        // Remember the blocked instances
        block.setCollect(true);
        block.init();
        
        // Push the Data through the Evaluator
        while (fsrc.push()) ;
        
        // Check if the correct number of instances was routed to the Block
        assertEquals(246, block.getBlockedData().size());
    }
    
    // **********************************************************\
    // *                     Test-Case Setup                    *
    // **********************************************************/
    protected void setUp() throws Exception
    {
    }
    
    protected void tearDown() throws Exception
    {
    }
    
    public ExpressionRouterTest(String name)
    {
        super(name);
    }
}
