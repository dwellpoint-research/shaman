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

import org.shaman.bayes.NaiveBayes;
import org.shaman.dataflow.Block;
import org.shaman.dataflow.FileSource;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.TestSets;
import org.shaman.rule.ExpressionParser;
import org.shaman.rule.ExpressionRule;

import junit.framework.TestCase;


/**
 * Expression based Data-flow Logic Test
 */
public class ExpressionRuleTest extends TestCase
{
    public static void testExpressionRule() throws ShamanException
    {
        // Classifier Flow Network
       MemorySupplier    ms = new MemorySupplier();
       NaiveBayes        nb = new NaiveBayes();
       InstanceSetMemory im = new InstanceSetMemory();

       // Rule Flow Network
       FileSource      fsrc = new FileSource();
       ExpressionRule   exp = new ExpressionRule();
       Block         block1 = new Block();
       Block         block2 = new Block();

       // Train a Classifier
       nb.registerSupplier(0, ms, 0);
       ms.registerConsumer(0, nb, 0);
       TestSets.loadCancer(ms, false, true);
       im.create(ms);
       nb.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE);
       nb.init();
       nb.trainTransformation(im);

       // Connect the Expression Rule Network
       fsrc.registerConsumer(0,  exp, 0);
       exp.registerSupplier(0,  fsrc, 0);
       exp.registerConsumer(0, block1, 0);
       exp.registerConsumer(1, block2, 0);
       block1.registerSupplier(0,  exp, 0);
       block2.registerSupplier(0,  exp, 1);

       fsrc.setFileParameters("./src/main/resources/data/cancer.txt", false, ",");
       fsrc.setSourceDataModels((DataModelObject)ms.getInputDataModel(0), ms.getOutputDataModel(0));
       fsrc.init();

       ExpressionParser expar = new ExpressionParser();
       expar.addTransformationFunction("bayes", nb);
       exp.setParserTemplate(expar);
       exp.setExpression("(1 + bayes(\"class\")) == 2");
       exp.init();

       block1.setCollect(true);
       block1.init();
       block2.setCollect(true);
       block2.init();
       
       // Push through Network
       while (fsrc.push()) ;
       fsrc.cleanUp();
       
       // Check output sizes
       assertEquals(254, block1.getBlockedData().size());
       assertEquals(445, block2.getBlockedData().size());
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
    
    public ExpressionRuleTest(String name)
    {
        super(name);
    }
}
