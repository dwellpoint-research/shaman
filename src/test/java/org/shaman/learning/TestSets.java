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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.DataModelException;
import org.shaman.learning.MemorySupplier;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.random.Uniform;


/**
 * <h2>Various Test Sets</h2>
 * Test sets loading. Taken from e.g. the UCI Machine Learner DataBase
 *
 * @author Johan Kaers
 * @version 2.0
 */

// **********************************************************\
// * Various Test Sets from e.g. the UCI Machine Learner DB *
// **********************************************************/
public class TestSets
{
    public static void loadMushrooms(MemorySupplier ms)
    {
        try
        {
            int              i,j;
            DataModelObject  dmin;
            DataModelDouble  dmout;
            AttributeDouble  atdnow;
            AttributeObject  atonow;
            String [][]legal = new String[][]
            {
                    new String[]{"e", "p"},
                    new String[]{"b","c","x","f","k","s"},
                    new String[]{"f","g","y","s"},
                    new String[]{"n","b","c","g","r","p","u","e","w","y"},
                    new String[]{"t","f"},
                    new String[]{"a","l","c","y","f","m","n","p","s"},
                    new String[]{"a","d","f","n"},
                    new String[]{"c","w","d"},
                    new String[]{"b","n"},
                    new String[]{"k","n","b","h","g","r","o","p","u","e","w","y"},
                    new String[]{"e","t"},
                    new String[]{"b","c","u","e","z","r","?"},
                    new String[]{"f","y","k","s"},
                    new String[]{"f","y","k","s"},
                    new String[]{"n","b","c","g","o","p","e","w","y"},
                    new String[]{"n","b","c","g","o","p","e","w","y"},
                    new String[]{"p","u"},
                    new String[]{"n","o","w","y"},
                    new String[]{"n","o","t"},
                    new String[]{"c","e","f","l","n","p","s","z"},
                    new String[]{"k","n","b","h","r","o","u","w","y"},
                    new String[]{"a","c","n","s","v","y"},
                    new String[]{"g","l","m","p","u","w","d"}
            };
            String []sleg;
            double []dleg;
            
            dmin  = new DataModelObject("Mushrooms File DataModel", 23);
            dmout = new DataModelDouble("Mushrooms DataModel",      23);
            for(i=0; i<legal.length; i++)
            {
                atonow = dmin.getAttributeObject(i);
                atdnow = dmout.getAttributeDouble(i);
                sleg   = legal[i];
                dleg   = new double[sleg.length];
                for (j=0; j<dleg.length; j++) dleg[j] = j;
                
                atonow.initAsSymbolCategorical("java.lang.String", sleg);
                atdnow.initAsSymbolCategorical(dleg);
                
                if (i==0)
                {
                    atdnow.setIsActive(false);
                    atonow.setIsActive(false);
                    atdnow.setValuesAsGoal();
                    atonow.setValuesAsGoal();  
                }
                else
                {            
                    atdnow.setIsActive(true);
                    atonow.setIsActive(true);
                }
            }
            dmin.getLearningProperty().setGoal(0);
            dmout.getLearningProperty().setGoal(0);
            
            ms.setInputDataModel(0,dmin);
            ms.setOutputDataModel(0,dmout);
            ms.loadFromTextFile("./src/main/resources/data/mushrooms.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadIris(MemorySupplier ms)
    {
        loadIris(ms, 0);
    }
    
    public static void loadIris(MemorySupplier ms, boolean goalalter)
    {
        if (!goalalter) loadIris(ms, 0);
        else            loadIris(ms, 1);
    }
    
    public static void loadIris(MemorySupplier ms, int type)
    {
        int             i;
        DataModelObject dmin;
        DataModelDouble dmout;
        AttributeDouble atdnow;
        AttributeObject atonow;
        String    stringType   = new String().getClass().getName();
        double    []parRange   = new double[]{0, 10};
        Object    []goalLegalO = new Object[]{"Iris-setosa", "Iris-versicolor", "Iris-virginica"};
        double    []goalLegalD = new double[]{          1.0,               2.0,              3.0};
        
        try
        {
            dmin  = new DataModelObject("Iris File DataModel", 5);
            dmout = new DataModelDouble("Iris DataModel",      5);
            for (i=0; i<4; i++)
            {
                atonow = (AttributeObject)dmin.getAttribute(i);
                atdnow = (AttributeDouble)dmout.getAttribute(i);
                atonow.initAsFreeText();
                atdnow.initAsNumberContinuous(parRange);
                atonow.setIsActive(true);
                atdnow.setIsActive(true);
            }
            atonow = (AttributeObject)dmin.getAttribute(4);
            atdnow = (AttributeDouble)dmout.getAttribute(4);
            atonow.initAsSymbolCategorical(stringType, goalLegalO);
            atdnow.initAsSymbolCategorical(goalLegalD);
            
            if      (type == 1)
            {
                atonow.setValueGroupsAsGoal(new Object[][]{new Object[]{"Iris-versicolor"}, new Object[]{"Iris-virganica", "Iris-setosa"}});
                atdnow.setValueGroupsAsGoal(new double[][]{new double[]{2.0},               new double[]{3.0,               1.0}});
            }
            else if (type == 0)
            {
                atonow.setValueGroupsAsGoal(new Object[][]{new Object[]{"Iris-virginica"}, new Object[]{"Iris-versicolor", "Iris-setosa"}});
                atdnow.setValueGroupsAsGoal(new double[][]{new double[]{3.0},              new double[]{2.0,               1.0}});
            }
            else if (type == 2)
            {
                atonow.setValuesAsGoal();
                atdnow.setValuesAsGoal();
            }
            
            atonow.setIsActive(false);
            atdnow.setIsActive(false);
            dmin.getLearningProperty().setGoal(4);
            dmout.getLearningProperty().setGoal(4);
            
            ms.setInputDataModel(0,dmin);
            ms.setOutputDataModel(0,dmout);
            ms.loadFromTextFile("./src/main/resources/data/iris.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadHepatitis(MemorySupplier ms)
    {
        try
        {
            int i;
            AttributeObject atonow;
            AttributeDouble atdnow;
            String    StringType   = new String().getClass().getName();
            Object    []boolLegal  = new Object[]{ "2", "1" };
            double    []boolLegalD = new double[]{  2,   1  };
            //Object    []ageLegal   = new Object[]{ "10", "20", "30", "40", "50", "60", "70", "80" };
            //double    []ageLegalD  = new double[]{   10,   20,   30,   40,   50,   60,   70,   80  };
            Object    []contLegal  = new Object[]{ "0","100000"};
            double    []contLegalD = new double[]{ 0, 100000 };
            
            DataModelObject dmin  = new DataModelObject("Hepatitis Input Datamodel", 20);
            DataModelDouble dmout = new DataModelDouble("Hepatitis Datamodel", 20);
            
            for (i=0; i<dmin.getAttributeCount(); i++)
            {
                atonow = dmin.getAttributeObject(i);
                atonow.setIsActive(true);
                
                if (i==0)
                {
                    atonow.initAsSymbolCategorical(StringType, boolLegal);
                    atonow.setValuesAsGoal();
                    atonow.setIsActive(false);
                    dmin.getLearningProperty().setGoal(0);
                }
                else if (i==1)
                {
                    atonow.initAsNumberContinuous(StringType, contLegal);
                    //atonow.initAsSymbolCategorical(StringType, ageLegal);
                }
                else if ((i>1) && (i<=13))
                {
                    atonow.initAsSymbolCategorical(StringType, boolLegal);
                }
                else if (i!=19)
                {
                    atonow.initAsNumberContinuous(StringType, contLegal);
                }
                else atonow.initAsSymbolCategorical(StringType, boolLegal);
                
                atonow.setMissingValues(new Object[]{"?"});
                atonow.setMissingAsObject("?");
            }
            
            for (i=0; i<dmout.getAttributeCount(); i++)
            {
                atdnow = dmout.getAttributeDouble(i);
                atdnow.setIsActive(true);
                if (i==0)
                {
                    atdnow.initAsSymbolCategorical(boolLegalD);
                    atdnow.setValuesAsGoal();
                    atdnow.setIsActive(false);
                    dmout.getLearningProperty().setGoal(0);
                }
                else if (i==1)
                {
                    //atdnow.initAsSymbolCategorical(ageLegalD);
                    atdnow.initAsNumberContinuous();
                }
                else if ((i>1) && (i<=13))
                {
                    atdnow.initAsSymbolCategorical(boolLegalD);
                }
                else if (i!=19)
                {
                    atdnow.initAsNumberContinuous(contLegalD);
                }
                else atdnow.initAsSymbolCategorical(boolLegalD);
                
                if ((i==14) || (i==15) || /*(i==16) ||*/ (i==17)) atdnow.setIsActive(false);
            }
            
            ms.setInputDataModel(0,dmin);
            ms.setOutputDataModel(0,dmout);
            ms.loadFromTextFile("./src/main/resources/data/hepatitis.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static DataModel getCancerDataModel(boolean ob, boolean cont) throws DataModelException
    {
        DataModelObject dmob;
        DataModelDouble dmdo;
        
        // The Cancer DataSet
        int i;
        AttributeObject attob;
        AttributeDouble attdo;
        String    stringType   = new String().getClass().getName();
        Object    []parLegalO  = new Object[]{"1","2","3","4","5","6","7","8","9","10"};
        double    []parLegalD  = new double[]{ 1,2,3,4,5,6,7,8,9,10 };
        double    []parRange   = new double[]{1,10};
        Object    []goalLegalO = new Object[]{"2", "4"};
        double    []goalLegalD = new double[]{2,4};
        
        dmob = new DataModelObject("Cancer Input", 12);
        dmdo = new DataModelDouble("Cancer", 12);
        attob = (AttributeObject)dmob.getAttribute(0);
        attdo = (AttributeDouble)dmdo.getAttribute(0);
        attob.setName("id");
        attdo.setName("id");
        attob.initAsNumberContinuous("java.lang.Integer");
        attdo.initAsNumberContinuous();
        attob.setIsActive(false);
        attdo.setIsActive(false);
        attob = (AttributeObject)dmob.getAttribute(1);
        attdo = (AttributeDouble)dmdo.getAttribute(1);
        attob.initAsFreeText();
        attdo.initAsFreeNumber();
        attob.setIsActive(false);
        attdo.setIsActive(false);
        for (i=2; i<=10; i++)
        {
            attob = (AttributeObject)dmob.getAttribute(i);
            attdo = (AttributeDouble)dmdo.getAttribute(i);
            attob.initAsSymbolCategorical(stringType, parLegalO);
            attob.setMissingAsObject("?");
            if (cont)
            {
                attdo.initAsNumberContinuous(parRange);   // Treat as continuous data (e.g. MLP)
                //attdo.setMissingAsDouble(1);              // Treat unknown values as minimum
            }
            else
            {
                attdo.initAsSymbolCategorical(parLegalD); //       as categorical data (e.g. ID3)
                attdo.setMissingAsDouble(-1);
                attob.setMissingIs(Attribute.MISSING_IS_VALUE);
                attdo.setMissingIs(Attribute.MISSING_IS_VALUE);
            }
            attob.setIsActive(true);
            attdo.setIsActive(true);
        }
        
        attob = (AttributeObject)dmob.getAttribute(11);
        attdo = (AttributeDouble)dmdo.getAttribute(11);
        attob.initAsSymbolCategorical(stringType, goalLegalO);
        attdo.initAsSymbolCategorical(goalLegalD);
        attob.setName("class");
        attdo.setName("class");
        attob.setIsActive(false);
        attdo.setIsActive(false);
        attob.setValuesAsGoal();
        attdo.setValuesAsGoal();
        dmob.getLearningProperty().setGoal(11);
        dmdo.getLearningProperty().setGoal(11);
        
        if  (ob) return(dmob);
        else     return(dmdo);
    }
    
    public static void loadSine(MemorySupplier ms, int numins, double freq)
    {
        int i;
        AttributeDouble attnow;
        double          []parRange = new double[]{-10, 10};
        DataModelDouble dm;
        
        try
        {
            // Make the datamodel
            dm = new DataModelDouble("Wave", 2);
            attnow = dm.getAttributeDouble(0);
            attnow.initAsNumberContinuous(parRange);
            attnow.setMissingAsDouble(-1);
            attnow.setIsActive(true);
            attnow.setName("x");
            attnow = dm.getAttributeDouble(1);
            attnow.initAsNumberContinuous(parRange);
            attnow.setIsActive(false);
            attnow.setAsGoal();
            attnow.setName("sin(x)");
            dm.getLearningProperty().setGoal(1);
            ms.setOutputDataModel(0, dm);
            
            // Create the training instances
            double         xnow, xstep;
            double         []ibuf = new double[2];
            DoubleMatrix1D []inall;
            DoubleMatrix1D innow;
            
            inall = new DoubleMatrix1D[numins];
            xstep = (2.0)/numins; xnow = -1;
            for (i=0; i<numins; i++)
            {
                ibuf[0]  = xnow;
                ibuf[1]  = Math.sin(xnow*Math.PI*freq);
                innow    = DoubleFactory1D.dense.make(ibuf);
                inall[i] = innow;
                xnow   += xstep;
            }
            ms.setDoubleInstances(inall);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadImage(MemorySupplier ms, String filename)
    {
        int             i;
        AttributeDouble attnow;
        double          []parRange = new double[]{0.0, 1.0};
        //DoubleMatrix1D  innow;
        DoubleMatrix1D  []inall;
        double          []ibuf = new double[2];
        int             numins;
        int             []image;
        int             []dims = new int[2];
        int             ipos;
        DataModelDouble dm;
        
        try
        {
            // Make the datamodel
            dm = new DataModelDouble("Image DataModel", 2);
            attnow = dm.getAttributeDouble(0);
            attnow.initAsNumberContinuous(parRange);
            attnow.setIsActive(true);
            attnow = dm.getAttributeDouble(1);
            attnow.initAsNumberContinuous(parRange);
            attnow.setIsActive(true);
            ms.setOutputDataModel(0, dm);
            
            // Use an b/w image as 2 dimensional PDF :-)
            image  = loadImage(filename, dims);
            numins = 0; for (i=0; i<image.length; i++) if (image[i] > 0) numins++;
            inall  = new DoubleMatrix1D[numins];
            ipos = 0;
            for (i=0; i<image.length; i++)
            {
                if (image[i] > 0)
                {
                    ibuf[0]       = ((double)(i % dims[0])) / dims[0];
                    ibuf[1]       = 1.0 - ((double)(i / dims[0])) / dims[1];
                    inall[ipos++] = DoubleFactory1D.dense.make(ibuf);
                }
            }
            ms.setDoubleInstances(inall);
            
            System.out.println("Number of active pixels "+numins);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadMNIST(MemorySupplier ms)
    {
         loadMNIST(ms, new String[]{"./src/main/resources/data/mnist/mnist_train_data.ubyte", "./src/main/resources/data/mnist/mnist_train_labels.ubyte"});
    }
    
    public static void loadMNIST(MemorySupplier ms, String []files)
    {
        try
        {
            // The MNIST Handwritten Letters DataSet
            int             i;
            AttributeDouble attnow;
            double          []parRange  = new double[]{0, 256};
            
            // Create DataModel
            DataModelDouble dm = new DataModelDouble("MNIST", 785);
            for (i=0; i<784; i++)
            {
                attnow = dm.getAttributeDouble(i);
                attnow.initAsNumberContinuous(parRange);
                attnow.setIsActive(true);
            }
            attnow = (AttributeDouble)dm.getAttribute(784);
            attnow.setIsActive(false);
            attnow.initAsSymbolCategorical(new double[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attnow.setValuesAsGoal();
            dm.getLearningProperty().setGoal(784);
            
            ms.setInputDataModel(0, dm);
            ms.setOutputDataModel(0, dm);
            ms.loadFromMNISTFile(files[0], files[1]);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadSphere(MemorySupplier ms)
    {
        try
        {
            int i;
            AttributeDouble attnow;
            double    []parRange = new double[]{-1, 1};
            
            DataModelDouble dm = new DataModelDouble("Sphere", 4);
            for (i=0; i<=3; i++)
            {
                attnow = dm.getAttributeDouble(i);
                attnow.initAsNumberContinuous(parRange);
                attnow.setIsActive(true);
            }
            
            attnow = dm.getAttributeDouble(3);
            attnow.initAsSymbolCategorical(new double[]{0, 1});
            attnow.setValuesAsGoal();
            attnow.setIsActive(false);
            dm.getLearningProperty().setGoal(3);
            
            ms.setInputDataModel(0,dm);
            ms.setOutputDataModel(0,dm);
            DoubleMatrix1D []spdat = makeSphere();
            ms.setDoubleInstances(spdat);
            
            System.out.println("Create data from unit sphere # "+spdat.length);
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static DoubleMatrix1D []makeSphere()
    {
        double spin, spout;
        double x,y,z,d;
        double []dbuf = new double[4];
        int    i, l;
        int    numin;
        DoubleMatrix1D []din;
        
        
        spin  = 0.0;
        spout = 0.8;
        numin = 10000;
        din = new DoubleMatrix1D[numin];
        for (i=0; i<numin; i++)
        {
            x = Uniform.staticNextDoubleFromTo(-1,1);
            y = Uniform.staticNextDoubleFromTo(-1,1);
            z = Uniform.staticNextDoubleFromTo(-1,1);
            
            //if ((x<0) && (y<0) && (z >0) ) l = 1;
            //else                 l = 0;
            
            
            //d = Math.sqrt(x*x + y*y + z*z) / Math.sqrt(3);
            //fout.write(x+" "+y+" "+z+" "+d+"\n");
            
            d = Math.sqrt(x*x + y*y + z*z);
            if ( (d > spin) && (d < spout)) l = 1;
            else                            l = 0;
            
            dbuf[0] = x; dbuf[1] = y; dbuf[2] = z; dbuf[3] = l;
            din[i]  = DoubleFactory1D.dense.make(dbuf);
        }
        
        return(din);
    }
    
    public static void loadCredit(MemorySupplier ms)
    {
        try
        {
            int i;
            AttributeObject atonow;
            AttributeDouble atdnow;
            String    StringType = new String().getClass().getName();
            Object    []A1Legal  = new Object[]{ "b", "a" };
            double    []A1LegalD = new double[]{ 0,1 };
            double    []A2LegalD = new double[]{ 0.0, 100.0 };
            double    []A3LegalD = new double[]{ 0.0, 30.0 };
            Object    []A4Legal  = new Object[]{ "u", "y", "l", "t" };
            double    []A4LegalD = new double[]{   0,   1,   2,   3 };
            Object    []A5Legal  = new Object[]{ "g", "p", "gg" };
            double    []A5LegalD = new double[]{   0,   1,   2  };
            Object    []A6Legal  = new Object[]{ "c", "d", "cc", "i", "j", "k", "m", "r", "q", "w", "x", "e", "aa", "ff" };
            double    []A6LegalD = new double[]{   0,   1,    2,   3,   4,   5,   6,   7,   8,   9,  10,  11,   12,   13 };
            Object    []A7Legal  = new Object[]{ "v", "h", "bb", "j", "n", "z", "dd", "ff", "o" };
            double    []A7LegalD = new double[]{   0,   1,    2,   3,   4,   5,   6,    7,   8 };
            double    []A8LegalD = new double[]{   0, 100 };
            Object    []A9Legal  = new Object[]{ "t", "f" };
            double    []A9LegalD = new double[]{   1,   0 };
            Object    []A10Legal  = new Object[]{ "t", "f" };
            double    []A10LegalD = new double[]{   1,   0 };
            double    []A11LegalD = new double[]{   0, 100 };
            Object    []A12Legal  = new Object[]{ "t", "f" };
            double    []A12LegalD = new double[]{   1,   0 };
            Object    []A13Legal  = new Object[]{ "g", "p", "s" };
            double    []A13LegalD = new double[]{  0,    1,   2 };
            double    []A14LegalD = new double[]{ 0, 10000 };
            double    []A15LegalD = new double[]{ 0, 100000 };
            Object    []A16Legal  = new Object[]{ "+", "-" };
            double    []A16LegalD = new double[]{   1,   0 };
            
            DataModelObject dmin  = new DataModelObject("Credit Approval Input Datamodel", 16);
            DataModelDouble dmout = new DataModelDouble("Credit Approval Datamodel", 16);
            atonow = (AttributeObject)dmin.getAttribute(0);  atonow.initAsSymbolCategorical(StringType, A1Legal);
            atonow = (AttributeObject)dmin.getAttribute(1);  atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(2);  atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(3);  atonow.initAsSymbolCategorical(StringType, A4Legal);
            atonow = (AttributeObject)dmin.getAttribute(4);  atonow.initAsSymbolCategorical(StringType, A5Legal);
            atonow = (AttributeObject)dmin.getAttribute(5);  atonow.initAsSymbolCategorical(StringType, A6Legal);
            atonow = (AttributeObject)dmin.getAttribute(6);  atonow.initAsSymbolCategorical(StringType, A7Legal);
            atonow = (AttributeObject)dmin.getAttribute(7);  atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(8);  atonow.initAsSymbolCategorical(StringType, A9Legal);
            atonow = (AttributeObject)dmin.getAttribute(9);  atonow.initAsSymbolCategorical(StringType, A10Legal);
            atonow = (AttributeObject)dmin.getAttribute(10); atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(11); atonow.initAsSymbolCategorical(StringType, A12Legal);
            atonow = (AttributeObject)dmin.getAttribute(12); atonow.initAsSymbolCategorical(StringType, A13Legal);
            atonow = (AttributeObject)dmin.getAttribute(13); atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(14); atonow.initAsFreeText();
            atonow = (AttributeObject)dmin.getAttribute(15); atonow.initAsSymbolCategorical(StringType, A16Legal);
            
            atdnow = (AttributeDouble)dmout.getAttribute(0);  atdnow.initAsSymbolCategorical(A1LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(1);  atdnow.initAsNumberContinuous(A2LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(2);  atdnow.initAsNumberContinuous(A3LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(3);  atdnow.initAsSymbolCategorical(A4LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(4);  atdnow.initAsSymbolCategorical(A5LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(5);  atdnow.initAsSymbolCategorical(A6LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(6);  atdnow.initAsSymbolCategorical(A7LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(7);  atdnow.initAsNumberContinuous(A8LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(8);  atdnow.initAsSymbolCategorical(A9LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(9);  atdnow.initAsSymbolCategorical(A10LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(10); atdnow.initAsNumberContinuous(A11LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(11); atdnow.initAsSymbolCategorical(A12LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(12); atdnow.initAsSymbolCategorical(A13LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(13); atdnow.initAsNumberContinuous(A14LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(14); atdnow.initAsNumberContinuous(A15LegalD);
            atdnow = (AttributeDouble)dmout.getAttribute(15); atdnow.initAsSymbolCategorical(A16LegalD);
            
            for (i=0; i<=14; i++)
            {
                atonow = (AttributeObject)dmin.getAttribute(i);
                atonow.setMissingValues(new Object []{"?"});
                dmin.getAttribute(i).setIsActive(true);
                dmout.getAttribute(i).setIsActive(true);
                // HACK HACK HACK HACK
                //dmout.getAttribute(i).addProperty(Attribute.PROPERTY_CONTINUOUS, new AttributePropertyContinuous());
            }
            atonow = (AttributeObject)dmin.getAttribute(15);
            atdnow = (AttributeDouble)dmout.getAttribute(15);
            atonow.setIsActive(false);
            atdnow.setIsActive(false);
            atonow.setValuesAsGoal();
            atdnow.setValuesAsGoal();
            dmin.getLearningProperty().setGoal(15);
            dmout.getLearningProperty().setGoal(15);
            
            ms.setInputDataModel(0,dmin);
            ms.setOutputDataModel(0,dmout);
            ms.loadFromTextFile("./src/main/resources/data/credit.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    
    public static void loadCancer(MemorySupplier ms, boolean cont)
    {
        loadCancer(ms, cont, true);
    }
    
    public static void loadCancer(MemorySupplier ms, boolean cont, boolean prim)
    {
        try
        {
            // The Cancer DataSet
            int i;
            AttributeObject attob;
            AttributeDouble attdo;
            String    stringType   = new String().getClass().getName();
            Object    []parLegalO  = new Object[]{"1","2","3","4","5","6","7","8","9","10"};
            double    []parLegalD  = new double[]{ 1,2,3,4,5,6,7,8,9,10 };
            Object    []parRangeO  = new Object[]{"1", "10"};
            double    []parRange   = new double[]{1,10};
            Object    []goalLegalO = new Object[]{"2", "4"};
            double    []goalLegalD = new double[]{2,4};
            
            DataModelObject dmob = new DataModelObject("Cancer Input", 11);
            DataModelDouble dmdo = new DataModelDouble("Cancer", 11);
            attob = (AttributeObject)dmob.getAttribute(0);
            attdo = (AttributeDouble)dmdo.getAttribute(0);
            attob.setName("number");
            attdo.setName("number");
            attob.initAsFreeText();
            attdo.initAsFreeNumber();
            attob.setIsActive(false);
            attdo.setIsActive(false);
            for (i=1; i<=9; i++)
            {
                attob = (AttributeObject)dmob.getAttribute(i);
                attdo = (AttributeDouble)dmdo.getAttribute(i);
                if (cont)
                {
                    attob.initAsNumberContinuous("java.lang.String", parRangeO);
                }
                else
                {
                    attob.initAsSymbolCategorical(stringType, parLegalO);
                }
                attob.setMissingValues(new Object[]{"?"});
                attob.setName("Field"+(i+1));
                attdo.setName("Field"+(i+1));
                attob.setMissingAsObject(null);
                if (cont)
                {
                    attdo.initAsNumberContinuous(parRange);   // Treat as continuous data (e.g. MLP)
                    attdo.setMissingAsDouble(1);              // Treat unknown values as minimum
                    //attdo.setMissingAsDouble(0);              // Treat unknown values as minimum
                }
                else
                {
                    attdo.initAsSymbolCategorical(parLegalD); //       as categorical data (e.g. ID3)
                    attdo.setMissingAsDouble(-1);
                    attdo.setMissingValues(new double[]{-1});
                    attob.setMissingIs(Attribute.MISSING_IS_VALUE);
                    attdo.setMissingIs(Attribute.MISSING_IS_VALUE);
                }
                attob.setIsActive(true);
                attdo.setIsActive(true);
            }
            
            attob = (AttributeObject)dmob.getAttribute(10);
            attdo = (AttributeDouble)dmdo.getAttribute(10);
            attob.initAsSymbolCategorical(stringType, goalLegalO);
            attdo.initAsSymbolCategorical(goalLegalD);
            attob.setIsActive(false);
            attdo.setIsActive(false);
            attob.setValuesAsGoal();
            attdo.setValuesAsGoal();
            attdo.setName("class");
            attob.setName("class");
            dmob.getLearningProperty().setGoal(10);
            dmdo.getLearningProperty().setGoal(10);
            
            ms.setInputDataModel(0,dmob);
            if (prim) ms.setOutputDataModel(0, dmdo);
            else      ms.setOutputDataModel(0, dmob);
            ms.loadFromTextFile("./src/main/resources/data/cancer.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadZoo(MemorySupplier ms)
    {
        loadZoo(ms, false);
    }
    
    public static void loadZoo(MemorySupplier ms, boolean prim)
    {
        int i;
        AttributeObject attnow;
        AttributeDouble attdo;
        String          stringType  = new String().getClass().getName();
        Object          []boolRange = new String[]{"\"True\"", "\"False\""};
        double         []dboolRange = new double[]{1.0,          0.0 };
        Object          []legsRange = new String[]{"0.00", "2.00", "4.00", "5.00", "6.00", "8.00"};
        double         []dlegsRange = new double[]{   0.0,    2.0,    4.0,    5.0,   6.0,    8.0 };
        Object          []typeRange = new String[]{"\"Mammal\"", "\"Bird\"", "\"Snake\"", "\"Anthropod\"", "\"Fish\"", "\"Amphibian\"", "\"Insect\""};
        double         []dtypeRange = new double[]{     0.0,          1.0,          2.0,           3.0,       4.0,           5.0,            6.0    };
        String          []attNames  = new String[]{"Animal Name", "Hair", "Feathers", "Eggs", "Milk", "Airborne", "Aquatic", "Predator", "Toothed", "Backbone", "Breathes", "Venomous", "Fins", "Legs", "Tail", "Domestic", "Catsize", "Type" };
        
        try
        {
            DataModelObject dmob = new DataModelObject("Zoo Object", 18);
            DataModelDouble dmdo = new DataModelDouble("zoo Double", 18);
            for (i=0; i<18; i++) dmob.getAttribute(i).setName(attNames[i]);
            for (i=0; i<18; i++) dmdo.getAttribute(i).setName(attNames[i]);
            
            attnow = (AttributeObject)dmob.getAttribute(0);
            attdo  = (AttributeDouble)dmdo.getAttribute(0);
            attnow.initAsFreeText();
            attnow.setIsActive(false);
            attdo.initAsFreeNumber();
            attdo.setIsActive(false);
            
            for (i=1; i<17; i++)
            {
                attnow = (AttributeObject)dmob.getAttribute(i);
                attdo  = (AttributeDouble)dmdo.getAttribute(i);
                if (i != 13)
                {
                    attnow.initAsSymbolCategorical(stringType, boolRange);
                    attdo.initAsSymbolCategorical(dboolRange);
                }
                else
                { 
                    attnow.initAsSymbolCategorical(stringType, legsRange);
                    attdo.initAsSymbolCategorical(dlegsRange);
                }
                attnow.setIsActive(true);
                attdo.setIsActive(true);
            }
            attnow = (AttributeObject)dmob.getAttribute(17);
            attdo  = (AttributeDouble)dmdo.getAttribute(17);
            
            attnow.initAsSymbolCategorical(stringType, typeRange);
            attnow.setIsActive(false);
            attnow.setValueGroupsAsGoal(new Object [][]{ new Object[]{"\"Mammal\""}, new Object[]{"\"Bird\"", "\"Snake\"", "\"Anthropod\"", "\"Fish\"", "\"Amphibian\"", "\"Insect\""}});
            dmob.getLearningProperty().setGoal(17);
            
            attdo.initAsSymbolCategorical(dtypeRange);
            attdo.setIsActive(false);
            attdo.setValueGroupsAsGoal(new double[][]{new double[]{0.0}, new double[]{1.0,2.0,3.0,4.0,5.0,6.0}});
            dmdo.getLearningProperty().setGoal(17);
            
            ms.setInputDataModel(0,dmob);
            if (prim) ms.setOutputDataModel(0,dmdo);
            else      ms.setOutputDataModel(0,dmob);
            ms.loadFromTextFile("./src/main/resources/data/zoo.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    public static void loadWine(MemorySupplier ms)
    {
        loadWine(ms, false);
    }
    
    public static void loadWine(MemorySupplier ms, boolean twoclass)
    {
        int             i;
        DataModelObject dmin;
        DataModelDouble dmout;
        AttributeDouble atdnow;
        AttributeObject atonow;
        String    stringType   = new String().getClass().getName();
        double    []parRange   = new double[]{0, 10000};
        Object    []goalLegalO = new Object[]{"1", "2", "3"};
        double    []goalLegalD = new double[]{ 1, 2, 3};
        
        try
        {
            dmin  = new DataModelObject("Wine Input File DataModel", 14);
            dmout = new DataModelDouble("Wine DataModel",            14);
            atonow = (AttributeObject)dmin.getAttribute(0);
            atdnow = (AttributeDouble)dmout.getAttribute(0);
            atonow.initAsSymbolCategorical(stringType, goalLegalO);
            atdnow.initAsSymbolCategorical(goalLegalD);
            if (!twoclass)
            {
                atonow.setValuesAsGoal();
                atdnow.setValuesAsGoal();
            }
            else
            {
                atonow.setValueGroupsAsGoal(new Object[][]{new Object[]{"2"}, new Object[]{"1","3"}});
                atdnow.setValueGroupsAsGoal(new double[][]{new double[]{2.0}, new double[]{1.0,3.0}});
            }
            atonow.setIsActive(false);
            atdnow.setIsActive(false);
            dmin.getLearningProperty().setGoal(0);
            dmout.getLearningProperty().setGoal(0);
            for (i=1; i<14; i++)
            {
                atonow = (AttributeObject)dmin.getAttribute(i);
                atdnow = (AttributeDouble)dmout.getAttribute(i);
                atonow.initAsFreeText();
                atdnow.initAsNumberContinuous(parRange);
                atonow.setIsActive(true);
                atdnow.setIsActive(true);
            }
            
            ms.setInputDataModel(0,dmin);
            ms.setOutputDataModel(0,dmout);
            ms.loadFromTextFile("./src/main/resources/data/wine.data", false, ",", "\n\r");
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
    
    private static int []loadImage(String filename, int []dims)
    {
        //    final int rMask = 0x000000FF;
        //    final int gMask = 0x0000FF00;
        //    final int bMask = 0x00FF0000;
        int           i;
        ImageIcon     icin, icsc;
        Image         im, imsc;
        BufferedImage bi;
        Graphics2D    gr;
        int           []rgbbuf;
        int           []greybuf;
        int           pix;
        int           width, height, numpix;
        
        icin    = new ImageIcon(filename);
        width   = icin.getIconWidth();
        height  = icin.getIconHeight();
        numpix  = width*height;
        rgbbuf  = new int[numpix];
        greybuf = new int[numpix];
        im      = icin.getImage();
        imsc    = im.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        bi      = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        icsc    = new ImageIcon(bi);
        icsc.setImage(imsc);
        gr      = bi.createGraphics();
        gr.drawImage(imsc, 0, 0, null);
        
        rgbbuf  = bi.getRGB(0,0,width,height, null, 0, width);
        for (i=0; i<rgbbuf.length; i++)
        {
            pix = rgbbuf[i];
            if (pix != -1) greybuf[i] = 255;
            else           greybuf[i] = 0;
        }
        
        dims[0] = width;
        dims[1] = height;
        return(greybuf);
    }
}
