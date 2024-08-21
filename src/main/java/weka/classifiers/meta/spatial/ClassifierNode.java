package weka.classifiers.meta.spatial;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class ClassifierNode // TODO: extends org.shaman.graph.Node implements .GraphNode
{
    private Instances  data;                  // The dataset to train the classifier on
    private AbstractClassifier classifier;      // The Weka Classifier used to test instances on
    private AbstractClassifier trainClassiifer; // and the one to train the dataset on
    
    private Instances  selectedInstances;      // The instances selected for the next epoch
        
    // Dataset to train on
    public void setData(Instances data)
    {
        this.data = data;
    }
    
    public void setClassifier(Classifier classifier) throws Exception
    {
        // Make a copy of the Classifier template to train with and to test on.
        this.classifier      = (AbstractClassifier)AbstractClassifier.makeCopy(classifier);
        this.trainClassiifer = (AbstractClassifier)AbstractClassifier.makeCopy(classifier);
    }
    
    public Classifier getClassifier(){
        return this.classifier;
    }
    
    public Instances getData()
    {
        return this.data;
    }
    
    // Derive classification model from dataset
    public void train() throws Exception
    {
        this.classifier.buildClassifier(this.data);
    }
    
    public void trainSafe() throws Exception
    {
        AbstractClassifier trainClassifier, tempClassifier;
        
        trainClassifier = this.trainClassiifer;
        tempClassifier  = this.classifier;
        trainClassifier.buildClassifier(this.data);
        this.classifier      = trainClassifier;
        this.trainClassiifer = tempClassifier;
    }
    
    // Set label of instance and return confidence of classification
    public double classify(Instance instance) throws Exception
    {
        double []pc;
        double   maxp;
        
        maxp = Double.NEGATIVE_INFINITY;
        pc   = this.classifier.distributionForInstance(instance);
        for(int i=0; i<pc.length; i++)
            if (pc[i] > maxp) maxp = pc[i];
        
        return(maxp);
    }
    
    public double [][]testConfusion(Instances instances) throws Exception
    {
        Attribute  atclass;
        double [][]cmatrix;
        double   []conf;
        double     maxconf;
        int        numclass, instanceclass, maxidx;
        
        // Create confusion matrix for the given test-set of instances
        atclass  = instances.classAttribute();
        numclass = atclass.numValues();
        cmatrix  = new double[numclass][numclass];
        for(Instance instance: instances)
        {
            instanceclass = (int)instance.classValue();
            conf          = this.classifier.distributionForInstance(instance);
            maxconf       = Double.NEGATIVE_INFINITY;
            maxidx        = -1;
            for(int i=0; i<conf.length; i++)
                if (conf[i] > maxconf) { maxconf = conf[i]; maxidx = i; }
            cmatrix[instanceclass][maxidx]++;
        }
        
        return(cmatrix);
    }
    
    public void setSelectedInstances(Instances instances)
    {
        this.selectedInstances = instances;
    }
    
    public Instances getSelectedInstances()
    {
        return this.selectedInstances;
    }
}
