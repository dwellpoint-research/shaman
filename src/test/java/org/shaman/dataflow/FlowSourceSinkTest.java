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

import java.io.File;

import org.shaman.TestUtils;
import org.shaman.dataflow.FileSink;
import org.shaman.dataflow.FileSource;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;

import junit.framework.TestCase;


/**
 * <h2>File Input/Output Test</h2>
 */
public class FlowSourceSinkTest extends TestCase
{
   public void testFileSourceSink() throws ShamanException
   {
       MemorySupplier   ms = new MemorySupplier(); // To get DataModels from
       FileSource     fsrc = new FileSource();
       FileSink       fdst = new FileSink();

       TestSets.loadCancer(ms, false);
       DataModelObject dmob = (DataModelObject)ms.getInputDataModel(0);
       DataModelDouble dmdo = (DataModelDouble)ms.getOutputDataModel(0);

       fsrc.registerConsumer(0, fdst, 0);
       fdst.registerSupplier(0, fsrc, 0);
       
       // File Input Test
       fsrc.setFileParameters("./src/main/resources/data/cancer.txt", false, ",");
       fsrc.setSourceDataModels(dmob, dmdo);
       fsrc.init();

       fdst.setAttributeFields(new String []{"class"},
                               new String []{"class"});
       fdst.setFileParameters("./src/main/resources/data/cancer_out.txt");
       fdst.setSinkDataModel(dmob);
       fdst.init();

       // Push until the source is dry.
       while(fsrc.push()) ;

       fdst.cleanUp();
       fsrc.cleanUp();
       
       // Compare with expected output file
       File fout = new File("./src/main/resources/data/cancer_out.txt");
       assertTrue(TestUtils.compareTextFile("./src/main/resources/data/cancer_out.txt", "./src/main/resources/data/cancer_class.txt"));
       fout.delete();
   }

	// **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
	public FlowSourceSinkTest(String name)
	{
		super(name);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	protected void tearDown() throws Exception
	{
        super.tearDown();
	}
}
