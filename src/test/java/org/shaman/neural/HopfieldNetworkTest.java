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
package org.shaman.neural;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.exceptions.DataModelException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.learning.Validation;
import org.shaman.learning.ValidationClassifier;
import org.shaman.neural.HopfieldNetwork;

import junit.framework.TestCase;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.random.Uniform;


/**
 * Hopfield Network Test
 */
public class HopfieldNetworkTest extends TestCase
{
    private DataModelDouble makeHopfieldDataModel() throws DataModelException
    {
        final int DIMX = 8;
        final int DIMY = 8;
        int             i;
        DataModelDouble dm;
        AttributeDouble att;
        
        // Datamodel of : category, input0, ..., input dimx*dimy
        dm  = new DataModelDouble("Pattern", DIMX*DIMY+1);
        att = new AttributeDouble("pattern");
        att.initAsSymbolCategorical(new double[]{0,1,2});
        att.setIsActive(false);
        att.setValuesAsGoal();
        dm.setAttribute(0, att);
        dm.getLearningProperty().setGoal("pattern");
        for (i=0; i<DIMX*DIMY; i++)
        {
            att = new AttributeDouble("input"+i);
            att.initAsSymbolCategorical(new double[]{-1,1});
            att.setIsActive(true);
            dm.setAttribute(i+1, att);
        }
        
        return(dm);
    }
    
    private String spat = new String(
            "        "+
            "        "+
            "        "+
            "        "+
            "        "+
            "        "+
            "        "+
            "        "
          );
    
    private String spat0 = new String(
            "  ****  "+
            " *    * "+
            "*      *"+
            "*      *"+
            "*      *"+
            "*      *"+
            " *    * "+
            "  ****  "
          );
    
    private String spat1 = new String(
            "     11 "+
            "    1 1 "+
            "   1  1 "+
            "  1   1 "+
            "      1 "+
            "      1 "+
            "      1 "+
            "      1 "
          );
    
    private String spat2 = new String(
            "  11111 "+
            " 1     1"+
            "      1 "+
            "     1  "+
            "    1   "+
            "   1    "+
            "  1     "+
            " 1111111"
          );
    
    private String spat3 = new String(
            "  11111 "+
            " 1     1"+
            "       1"+
            "    111 "+
            "       1"+
            "       1"+
            " 1     1"+
            "  11111 "
          );
    
    private List makeHopfieldPatterns(DataModelDouble dm)
    {
        List           lpat;
        
        lpat = new LinkedList();
        lpat.add(makePattern(dm, 0, spat0));
        lpat.add(makePattern(dm, 1, spat1));
        lpat.add(makePattern(dm, 2, spat2));
        
        return(lpat);
    }
    
    private DoubleMatrix1D makePattern(DataModelDouble dm, int goal, String spat)
    {
        DoubleMatrix1D pat;
        int            i;
        
        // Make training instances from the patterns
        pat = DoubleFactory1D.dense.make(dm.getAttributeCount());
        pat.setQuick(0, goal);
        for (i=0; i<spat.length(); i++)
        {
            char patdat = spat.charAt(i);
            if (patdat == ' ') pat.setQuick(i+1, -1);
            else               pat.setQuick(i+1,  1);
        }
        
        return(pat);
    }
    
    private List makeTestPatterns(int num, List patterns, double pmut)
    {
        int            i, j, ranpos;
        List           lpat;
        DoubleMatrix1D patnow;
        
        // Make a list of the given size with mutated patterns
        // Start from a random input pattern, then flip every state
        // with the given mutation probability
        lpat = new LinkedList();
        for (i=0; i<num; i++)
        {
            ranpos = Uniform.staticNextIntFromTo(0, patterns.size()-1);
            patnow = (DoubleMatrix1D)patterns.get(ranpos);
            patnow = patnow.copy();
            for (j=1; j<patnow.size(); j++)
            {
                if (Uniform.staticNextDouble() < pmut)
                {
                    patnow.setQuick(j, patnow.getQuick(j)*-1);
                }
            }
            lpat.add(patnow);
        }
        
        return(lpat);
    }
    
    private String drawOutput(DoubleMatrix1D vec)
    {
        int          i,j;
        StringBuffer sv;
        int          pos;
        
        pos = 0;
        sv  = new StringBuffer();
        for (i=0; i<8; i++)
        {
            for (j=0; j<8; j++)
            {
                if (vec.getQuick(pos++) == -1) sv.append(" ");
                else                           sv.append("*");
            }
            sv.append("\n");
        }
        return(sv.toString());
    }    
    
    // **********************************************************\
    // *     Test Hopfield Associative Memory Classification    *
    // **********************************************************/
    public void testHopfield() throws Exception
    {
        // Make the Hopfield network
        MemorySupplier    ms = new MemorySupplier();
        HopfieldNetwork  hop = new HopfieldNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, im, 0);
        ms.registerConsumer(0, hop, 0);
        hop.registerSupplier(0, ms, 0);
        
        DataModelDouble dm;
        List            trainpatterns;
        
        // Make training patterns
        dm            = makeHopfieldDataModel();
        trainpatterns = makeHopfieldPatterns(dm);
        im.create(trainpatterns, dm);
        
        // Configure and train the Hopfield network on the patterns
        ms.setOutputDataModel(0, dm);
        
        hop.setNumberOfNeurons(64);
        hop.init();
        
        hop.setTrainSet(im);
        hop.initializeTraining();
        hop.train();
        
        // Make some test-patterns by mutating the training patterns with random noise
        InstanceSetMemory   imtest;
        imtest = new InstanceSetMemory();
        imtest.create(makeTestPatterns(1000, trainpatterns, 0.2), dm);        
        
        // Test using Cross Validation. Skip training.
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(imtest, hop);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.setSkipTrain(true);
        val.test();
        valclas = val.getValidationClassifier();
        
        double     clerr  = valclas.getClassificationError();
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Error : "+valclas.getClassificationError()+"\n");
        assertTrue(clerr < 0.10);
    }

    public void testPersistence() throws Exception
    {
        // Make the Hopfield network
        MemorySupplier    ms = new MemorySupplier();
        HopfieldNetwork  hop = new HopfieldNetwork();
        InstanceSetMemory im = new InstanceSetMemory();
        
        ms.registerConsumer(0, im, 0);
        ms.registerConsumer(0, hop, 0);
        hop.registerSupplier(0, ms, 0);
        
        DataModelDouble dm;
        List            trainpatterns;
        
        // Make training patterns
        dm            = makeHopfieldDataModel();
        trainpatterns = makeHopfieldPatterns(dm);
        im.create(trainpatterns, dm);
        
        // Configure and train the Hopfield network on the patterns
        ms.setOutputDataModel(0, dm);
        
        hop.setNumberOfNeurons(64);
        hop.init();
        
        hop.setTrainSet(im);
        hop.initializeTraining();
        hop.train();
        
        // Save the trained hopfield net
        String outname = System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"hopfield_test.obj";
        ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(outname));
        hop.saveState(oout);
        oout.close();
        
        // **CRASH** AND **BURN**
        hop = null;
        // ++++++++++++++++++++++
        
        // Make new HopfieldNetwork, load persisted state, connect to data-set and initialize
        hop = new HopfieldNetwork();
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(outname));
        hop.loadState(oin);
        oin.close();
        hop.registerSupplier(0, ms, 0);
        hop.init();
        
        // Make some test-patterns by mutating the training patterns with random noise
        InstanceSetMemory   imtest;
        imtest = new InstanceSetMemory();
        imtest.create(makeTestPatterns(1000, trainpatterns, 0.2), dm);        
        
        // Test using Cross Validation. Skip training.
        Validation           val;
        ValidationClassifier valclas;
        
        val = new Validation(imtest, hop);
        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
        val.setSkipTrain(true);
        val.test();
        valclas = val.getValidationClassifier();
        
        double     clerr  = valclas.getClassificationError();
        double [][]cmraw  = valclas.getConfusionMatrix();
        DoubleMatrix2D cm = DoubleFactory2D.dense.make(cmraw);
        System.out.println(cm);
        System.out.println("Error : "+valclas.getClassificationError()+"\n");
        assertTrue(clerr < 0.10);
    }
    
    // **********************************************************\
    // *                JUnit Setup and Teardown                *
    // **********************************************************/
    public HopfieldNetworkTest(String name)
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
