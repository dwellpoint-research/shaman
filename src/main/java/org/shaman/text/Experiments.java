/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers (johankaers@gmail.com)                *
 *                                                       * 
 *  Copyright (c) 2005-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;

import java.io.File;
import java.util.LinkedList;

import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataModelException;
import org.shaman.learning.Classifier;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.learning.MemorySupplier;
import org.shaman.util.FileUtil;

import cern.colt.matrix.ObjectFactory1D;
import cern.colt.matrix.ObjectMatrix1D;


// *********************************************************
// *         BBC News Text Classification Experiment       *
// *********************************************************
public class Experiments
{
    // BBC News DataSet
    private String basedir = "c:/Projects/data/bbcnews/";
    private String []directories = new String[] {
            basedir+"business/",
            basedir+"entertainment/",
            basedir+"sciencenature/",
            basedir+"technology/",
            basedir+"world/"
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
    private void testBBCNews() throws ShamanException
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
        testSentence(tc, "IBM sold it's personal computer division to Lenovo");
        
//        // Cross Validation of Text Classification
//        ValidationClassifier valcl;
//        DoubleMatrix2D cm;
//        double         [][]cmraw;
//        
//        im.create(ms);
//        
//        Validation val = new Validation(im, tc);
//        val.create(Validation.SPLIT_CROSS_VALIDATION, new double[]{3.0});
//        val.test();
//        valcl = val.getValidationClassifier();
//        cmraw = valcl.getConfusionMatrix();
//        cm    = DoubleFactory2D.dense.make(cmraw);
//        System.out.println("Classification Error : "+valcl.getClassificationError());
//        System.out.println("Confusion Matrix\n"+cm);
    }
    
    private void testSentence(TextClassification tc, String sentence) throws ShamanException
    {
        double []conf;
        int    i;
        
        conf = new double[this.subjects.length];
        tc.classify(makeTextVector(sentence), conf);
        System.out.println(sentence);
        for (i=0; i<this.subjects.length; i++)
        {
            System.out.println("\t"+this.subjects[i]+"\t\t"+conf[i]);
        }
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
            int        i,j;
            File     []subfiles;
            File       subdir;
            String     subnow, pagenow;
            ObjectMatrix1D  vecnow;
            
            // Load the pages from the different subject directories
            repvec = new LinkedList();
            for (i=0; i<this.subjects.length; i++)
            {
                subnow   = this.subjects[i];
                subdir   = new File(this.directories[i]);
                subfiles = subdir.listFiles();
                for (j=0; j<subfiles.length; j++)
                {
                    pagenow = FileUtil.readTextFileToString(subfiles[j], false);
                    vecnow  = dmtext.createDefaultVector();
                    vecnow.setQuick(0, subnow);
                    vecnow.setQuick(1, pagenow);
                    repvec.add(vecnow);
                }
                System.out.println("Loaded "+j+" pages for subject '"+subnow+"'");
            }
            
            // Initialize the memorysupplier with the document repository
            ObjectMatrix1D []vecob = (ObjectMatrix1D[])repvec.toArray(new ObjectMatrix1D[]{});
            ms.setObjectInstances(vecob);
        }
        catch(java.io.IOException ex) { throw new ShamanException(ex); }
    }
    
    // *********************************************************\
    // *              Text Mining Main Program                 *
    // *********************************************************/
    public static void main(String[] args)
    {
        Experiments instance;
        
        instance = new Experiments();
        try
        {
            instance.testBBCNews();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}
