/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *                                                       * 
 *  Copyright (c) 2005-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;

import java.io.File;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataModelException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.text.TextClassification;
import org.shaman.util.FileUtil;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


// *********************************************************
// *         BBC News Text Classification Experiment       *
// *********************************************************
public class CategorizerTest extends TestCase
{
    // BBC News DataSet
    private String basedir = "./src/main/resources/data/";
    private String []files = new String[] {
            basedir+"bbcnews_business.htm",
            basedir+"bbcnews_entertainment.htm",
            basedir+"bbcnews_science.htm",
            basedir+"bbcnews_technology.htm",
            basedir+"bbcnews_world.htm"
    };
    private String []subjects = new String []{ 
            "business",
            "entertainment",
            "sciencenature",
            "technology",
            "world"
    };
    
    // *********************************************************\
    // *                Test Text Classification               *
    // *********************************************************/
    public void testBBCNews() throws ShamanException
    {
        // Load the BBC News repository into a MemorySupplier
        MemorySupplier     ms = new MemorySupplier();
        TextClassification tc = new TextClassification();
        InstanceSetMemory  im = new InstanceSetMemory();
        
        ms.registerConsumer(0, tc, 0);
        ms.registerConsumer(0, im ,0);
        tc.registerSupplier(0, ms, 0);
        
        loadRepository(ms);
        
        // Set the Text Classification Parameters
        tc.setClassifierOutput(Classifier.OUT_CLASS_AND_CONFIDENCE_VECTOR);
        tc.setTextProcessingComponents(
                       "org.shaman.text.IteratedLovinsStemmer",
          new String[]{"org.shaman.text.DigitWords",
                       "org.shaman.text.Stopwords"},
          new String[]{"org.shaman.text.HTMLSeparator",
                       "org.shaman.text.SentenceSeparator",
                       "org.shaman.text.WordSeparator"} );
        tc.setTrainParameters(3000, 0.7);
        tc.init();
        
        // Train the classifier
        im.create(ms);
        tc.trainTransformation(im);
        
        // Test some classifications
        assertEquals(0, testSentence(tc, "My credit card was stolen by a man in an orange suit."));
        assertEquals(1, testSentence(tc, "Motown music is from Detroit."));
        assertEquals(2, testSentence(tc, "Darwin's the 'Origin of the Species' explains evolution."));
        assertEquals(3, testSentence(tc, "My computer caught a virus and infected the microwave oven."));
        assertEquals(4, testSentence(tc, "Pakistan and India are nuclear neighbours."));
    }
    
    private int testSentence(TextClassification tc, String sentence) throws ShamanException
    {
        double []conf;
        double   max;
        int      maxpos;
        int      i;
        
        conf = new double[this.subjects.length];
        tc.classify(makeTextVector(sentence), conf);
        
        max    = conf[0];
        maxpos = 0;
        for (i=0; i<this.subjects.length; i++)
        {
            if (max < conf[i]) { max = conf[i]; maxpos = i; }
            //System.out.println("\t"+this.subjects[i]+"\t\t"+conf[i]);
        }
        
        return(maxpos);
    }
    
    private ObjectMatrix1D makeTextVector(String text)
    {
        ObjectMatrix1D txtvec;
        
        txtvec = ObjectFactory1D.dense.make(1);
        txtvec.setQuick(0, text);
        
        return(txtvec);
    }
    
    // *********************************************************\
    // *                 BBC News Data-Set                     *
    // *********************************************************/
    private DataModelObject makeTextDataModel() throws DataModelException
    {
        DataModelObject dmtm;
        AttributeObject atob;
        
        dmtm = new DataModelObject("Text DataModel", 2);
        atob = new AttributeObject("subject");
        atob.initAsSymbolCategorical("java.lang.String", this.subjects);
        atob.setValuesAsGoal();
        atob.setIsActive(false);
        dmtm.setAttribute(0, atob);
        atob = new AttributeObject("content");
        atob.initAsFreeText();
        atob.setIsActive(true);
        dmtm.setAttribute(1, atob);
        dmtm.getLearningProperty().setGoal("subject");
        
        return(dmtm);
    }
    
    public void loadRepository(MemorySupplier ms) throws ShamanException
    {
        DataModelObject dmtext;
        
        
        // Install the Text-Mining source DataModel in the MemorySupplier
        dmtext = makeTextDataModel();
        ms.setInputDataModel(0, dmtext);
        ms.setOutputDataModel(0, dmtext);
        
        try
        {
            LinkedList repvec;
            int        i;
            File       subfile;
            String     subnow, pagenow;
            ObjectMatrix1D  vecnow;
            
            // Load the pages from the different subject directories
            repvec = new LinkedList();
            for (i=0; i<this.subjects.length; i++)
            {
                subnow   = this.subjects[i];
                subfile  = new File(this.files[i]);
                pagenow = FileUtil.readTextFileToString(subfile, false);
                vecnow  = dmtext.createDefaultVector();
                vecnow.setQuick(0, subnow);
                vecnow.setQuick(1, pagenow);
                repvec.add(vecnow);
                System.out.println("Loaded subject '"+subnow+"'");
            }
            
            // Initialize the memorysupplier with the document repository
            ObjectMatrix1D []vecob = (ObjectMatrix1D[])repvec.toArray(new ObjectMatrix1D[]{});
            ms.setObjectInstances(vecob);
        }
        catch(java.io.IOException ex) { throw new ShamanException(ex); }
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
}
