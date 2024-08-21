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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.CachingPresenter;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>CN2 Sequential Covering</h2>
 * Algorith that learns a set of if-then rules that cover the train set.
 * Based on the CN2 algorithm.
 *
 * <i> Tom M. Mitchell (1997), Machine Learning, p275</i>
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// * Sequential Covering Algorithm using CN2-type Learning *
// *********************************************************/
public class CN2 extends ClassifierTransformation implements Classifier
{
    /** Performance is frequency of positive instances in the set of covered instances. goal should be GOAL_DEFAULT_NEGATIVE */
    public static final int PERFORMANCE_RELATIVE_FREQUENCY = 0;
    /** Performance is a measure for the uniformity of the target function in the set of covered instances. */
    public static final int PERFORMANCE_ENTROPY            = 1;
    
    /** Make rules to detect all goal classes */
    public static final int GOAL_ALL              = 0;
    /** Make rules only for the positive class */
    public static final int GOAL_DEFAULT_NEGATIVE = 1;
    
    // Model/Training Parameters
    private int    performance;             // Type of performance measure to use for evaluating possible rules
    private int    goal;                    // How to use the goal attribute.
    private int    positiveClass;           // positive goal class in the case of GOAL_DEFAULT_NEGATIVE
    private int    beamSize;                // Size of the 'beam' in the LearOneRule beam-search
    private double performanceThreshold;    // Stop finding new rules if the performance drops underneat this threshold.
    
    // Model
    private Rule []rule;
    
    // Training Algorithm buffers.
    private double []weightOriginal;
    
    // **********************************************************\
    // *              CN2 Sequential Rule Covering              *
    // **********************************************************/
    private void cn2() throws LearnerException, DataModelException
    {
        int          i;
        int          numins, covered;
        LinkedList   rules;
        Rule         rulenow;
        IntArrayList matchind;
        
        numins = 0;
        if (goal == GOAL_DEFAULT_NEGATIVE)
        {
            // If default negative reasoning is used, count the positive instances.
            for (i=0; i<this.trainData.getNumberOfInstances(); i++)
                if ((this.trainData.getWeight(i) != 0) && (this.trainData.getGoalClass(i) == positiveClass)) numins++;
        }
        else
        {
            // Count all active instances.
            numins = 0;
            for (i=0; i<this.trainData.getNumberOfInstances(); i++) if (this.trainData.getWeight(i) != 0) numins++;
        }
        
        // Initialize the rule-set to the empty set
        rules = new LinkedList();
        
        // Learn the First Rule.
        covered = 0;
        rulenow = learnOneRule(beamSize);
        // Loop until no more good rules found. Or all instances are covered.
        while ((covered != numins) && (rulenow.getPerformance() > performanceThreshold))
        {
            // Add the new rule to the Collection
            rules.addLast(rulenow);
            
            // Disable the instances that are covered by this rule.
            matchind = getMatching(rulenow);
            for (i=0; i<matchind.size(); i++) this.trainData.setWeight(matchind.get(i), 0.0);
            
            if (goal == GOAL_ALL) covered += matchind.size(); // Adjust the number of covered instances.
            else
            {
                // DEFAULT NEGATIVE goal handling. Add the number of positive instances covered to total.
                for (i=0; i<matchind.size(); i++)
                    if (this.trainData.getGoalClass(matchind.get(i)) == positiveClass) covered++;
            }
            System.out.println("Covered "+covered+"/"+numins+" instances");
            
            // Find the next rule that best covers the remaining instances
            rulenow = learnOneRule(beamSize);
        }
        
        // Convert the rule set into an array.
        rule = (Rule [])rules.toArray(new Rule[]{});
        for (i=0; i<rule.length; i++) System.out.println(rule[i]);
        
        // Restore the instance weights.
        for (i=0; i<weightOriginal.length; i++) this.trainData.setWeight(i, weightOriginal[i]);
    }
    
    private void initCN2() throws LearnerException
    {
        int i;
        
        // Make a backup of the original instance weighting situation.
        weightOriginal = new double[this.trainData.getNumberOfInstances()];
        for (i=0; i<weightOriginal.length; i++) weightOriginal[i] = this.trainData.getWeight(i);
    }
    
    private Rule learnOneRule(int beam) throws LearnerException, DataModelException
    {
        // Find a Rule that covers some of the instances.
        // Conduct a general to specific beam search for the best rule guided by the performace metric.
        // p 278 Machine Learning by Tom Mitchell
        int             i,j,k;
        int             numins;
        Rule            besthyp;
        TreeSet         candhyp, newcandhyp;
        HashSet         allcon;
        Attribute       attnow;
        AttributeDouble attdonow;
        AttributeObject attobnow;
        Condition       connow;
        int             clnow;
        double          val;
        Object          oval;
        boolean         found;
        int             newhypcount;
        Iterator        itcandhyp;
        Iterator        itallcon;
        Rule            hypnow;
        Rule            hypnew;
        double          perfnow;
        double          perfbest;
        
        // Make a list with all possible constraints (conditions).
        // All (attribute, value) pairs that occur in some active instance.
        numins = this.trainData.getNumberOfInstances();
        allcon = new HashSet();
        for (i=0; i<this.actind.length; i++)
        {
            attnow   = this.dataModel.getAttribute(this.actind[i]);
            attdonow = null;
            attobnow = null;
            if (this.primitive) attdonow = (AttributeDouble)attnow;
            else                attobnow = (AttributeObject)attnow;
            
            for (j=0; j<attnow.getNumberOfCategories(); j++)
            {
                // Check if (attribute[i], category[j]) appears in some active instance.
                found = false;
                for (k=0; (k<numins) && (!found); k++)
                {
                    if (this.trainData.getWeight(k) > 0.0)
                    {
                        if (this.primitive)
                        {
                            val   = this.trainData.getInstance(k).getQuick(i);
                            clnow = attdonow.getCategory(val);
                            if (clnow == j) found = true;
                        }
                        else
                        {
                            oval  = this.trainData.getObjectInstance(k).getQuick(i);
                            clnow = attobnow.getCategory(oval);
                            if (clnow == j) found = true;
                        }
                    }
                }
                // Add the possible condition to the list
                if (found)
                {
                    connow = new Condition(dataModel, attnow.getName(), Condition.IS_EQUAL, new Double(j));
                    allcon.add(connow);
                }
            }
        }
        
        // Initialize Best and Candidate Hypotoses with the empty rule.
        besthyp  = new Rule(dataModel);
        candhyp  = new TreeSet();
        perfbest = -1; //performance(besthyp);
        candhyp.add(besthyp);
        
        // Loop until the most specific rules have been tried.
        while (candhyp.size() != 0)
        {
            // Generate the next more specific candidate hypotheses.
            newhypcount = 0;
            newcandhyp  = new TreeSet();
            itcandhyp   = candhyp.iterator();
            while (itcandhyp.hasNext())
            {
                // Extend the current candidates with all possible conditions that do not conflict.
                hypnow   = (Rule)itcandhyp.next();
                itallcon = allcon.iterator();
                while (itallcon.hasNext())
                {
                    connow = (Condition)itallcon.next();
                    hypnew = Rule.specialize(hypnow, connow);
                    if (hypnew != null)
                    {
                        // If a new one is found. Test it's performance and add to new hyptotheses set.
                        perfnow = performance(hypnew);
                        hypnew.setPerformance(perfnow);
                        newcandhyp.add(hypnew);
                        newhypcount++;
                        
                        // Is this the best one so far? If so, remember it.
                        if (perfnow > perfbest) { besthyp = hypnew; perfbest = perfnow; }
                    }
                }
            }
            
            // Retain the 'beam' best new hyptotheses.
            candhyp   = new TreeSet();
            itcandhyp = newcandhyp.iterator();
            i         = 0;
            while ((i<beam) && (itcandhyp.hasNext())) { i++; candhyp.add(itcandhyp.next()); }
        }
        
        // Determine which goal class values occurs most in the instances that match this rule.
        int        []gc;
        int          numgoal, maxc, maxgc;
        IntArrayList indmatch;
        
        if (this.primitive) numgoal = ((AttributeDouble)this.attgoal).getNumberOfGoalClasses();
        else                numgoal = ((AttributeObject)this.attgoal).getNumberOfGoalClasses();
        indmatch = getMatching(besthyp);
        gc       = new int[numgoal];
        for (i=0; i<indmatch.size(); i++) gc[this.trainData.getGoalClass(indmatch.get(i))]++;
        maxc = 0; maxgc = 0;
        for (i=0; i<numgoal; i++) if (gc[i] > maxc) { maxc = gc[i]; maxgc = i; }
        
        // Set the most occuring goal class in the matched instances as rule consequence
        besthyp.setGoalClassConsequence(attgoal.getName(), maxgc);
        
        return(besthyp);
    }
    
    private IntArrayList getMatching(Rule r) throws LearnerException
    {
        // Find the instances that match with this rule.
        int          i;
        IntArrayList indmatch;
        
        indmatch = new IntArrayList();
        for (i=0; i<this.trainData.getNumberOfInstances(); i++)
        {
            if (this.trainData.getWeight(i) > 0.0)
            {
                if (this.primitive) { if (r.apply(this.trainData.getInstance(i)))       indmatch.add(i); }
                else                { if (r.apply(this.trainData.getObjectInstance(i))) indmatch.add(i); }
            }
        }
        
        return(indmatch);
    }
    
    private double performance(Rule r) throws LearnerException
    {
        int          i;
        double       p;
        IntArrayList indmatch;
        
        p        = 0;
        
        // Find the instances that match with this rule.
        indmatch = getMatching(r);
        
        // Evaluate the performance of the rule by looking at the matched instances.
        if      (performance == PERFORMANCE_ENTROPY)
        {
            // Calculate a measure for the uniformity of the target function
            // for the instances that match the rule.
            double []pi;
            int    numgoal;
            double log2 = Math.log(2.0);
            
            if (this.primitive) numgoal = ((AttributeDouble)this.attgoal).getNumberOfGoalClasses();
            else                numgoal = ((AttributeObject)this.attgoal).getNumberOfGoalClasses();
            pi      = new double[numgoal];
            for (i=0; i<indmatch.size(); i++) pi[this.trainData.getGoalClass(indmatch.get(i))]++;
            p = 0;
            for (i=0; i<numgoal; i++)
            {
                pi[i] /= indmatch.size();
                if (pi[i] != 0) p += pi[i]*(Math.log(pi[i])/log2);
            }
            p = -p;
        }
        else if (performance == PERFORMANCE_RELATIVE_FREQUENCY)
        {
            // Calculate the fraction of positive instances in the set of the matched instances.
            p = 0;
            for (i=0; i<indmatch.size(); i++)
            {
                if (this.trainData.getGoalClass(indmatch.get(i)) == positiveClass) p++;
            }
            p /= indmatch.size();
        }
        
        return(p);
    }
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        initCN2();
    }
    
    public void train() throws LearnerException
    {
        try
        {
            cn2();
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _instances) throws LearnerException
    {
        DataModel dmin;
        
        try
        {
            // Install new Training instances, initialize classifier data-structures
            dmin           = trainData.getDataModel();
            this.trainData = (CachingPresenter)trainData;
            setDataModel(dmin);
            initClassifier(dmin);
        }
        catch(ConfigException ex) { throw new LearnerException(ex); }
    }
    
    public boolean   isSupervised() { return(true); }
    
    // *********************************************************\
    // *                    CN2 Classification                 *
    // *********************************************************/
    private int classifyObject(Object instance, double []confidence) throws LearnerException
    {
        ObjectMatrix1D  oins;
        DoubleMatrix1D  dins;
        int              i;
        int              cl;
        boolean          match;
        int              nummatch, numgoal;
        double           max;
        double         []cn;

        if (this.primitive) numgoal = ((AttributeDouble)this.attgoal).getNumberOfGoalClasses();
        else                numgoal = ((AttributeObject)this.attgoal).getNumberOfGoalClasses();
        cn        = new double[numgoal];
        dins      = null;
        oins      = null;
        if (this.primitive) dins = (DoubleMatrix1D)instance;
        else                oins = (ObjectMatrix1D)instance;
        
        nummatch = 0; cl = -1;
        for (i=0; i<rule.length; i++)
        {
            // Does the instance match with the current rule.
            if (this.primitive) match = rule[i].apply(dins);
            else                match = rule[i].apply(oins);
            
            if (match)
            {
                // If it matches, mark the class found in the consequence of the rule
                cl   = rule[i].getGoalClassConsequence();
                cn[cl]++;
                nummatch++;
            }
        }
        if (goal == GOAL_DEFAULT_NEGATIVE)
        {
            // If there was a match, the instance is positive. Otherwise it's negative.
            if (nummatch > 0) cl = positiveClass;
            else              cl = 1-positiveClass;
        }
        else
        {
            // Find the class that matched most with the rules.
            if (nummatch > 0)
            {
                for (i=0; i<cn.length; i++) cn[i] /= nummatch;
                max = -1; cl = -1;
                for (i=0; i<cn.length; i++) if (cn[i] > max) { cl = i; max = cn[i]; }
                if (confidence != null) for (i=0; i<confidence.length; i++) confidence[i] = cn[i];
            }
            else cl = -1;
        }
        
        return(cl);
    }
    
    public int classify(ObjectMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifyObject(instance, confidence));
    }
    
    public int classify(DoubleMatrix1D instance, double []confidence) throws LearnerException
    {
        return(classifyObject(instance, confidence));
    }
    
    // *********************************************************\
    // *              Model/Classifier Parameters              *
    // *********************************************************/
    /**
     * Set how the performance of a rule should be calculated.
     * @param _performance The kind of performance to use.
     */
    public void setPerformance(int _performance)
    {
        performance = _performance;
    }
    
    /**
     * Set for which goal classes rules should be made.
     * @param _goal The kind of goal class handling.
     * @param _positiveClass The positive goal class (0 or 1) of _goal is DEFAULT_NEGATIVE
     */
    public void setGoalHandling(int _goal, int _positiveClass)
    {
        goal          = _goal;
        positiveClass = _positiveClass;
    }
    
    /**
     * Set the parameters of the CN2 sequential rule covering algortihm
     * @param _beamSize The size of the 'beam' in the beam-search that finds rules.
     * @param _performanceThreshold The threshold that determines when the algorihm stop.
     *          If the best performance rules drops under this, the rule generation is ended.
     */
    public void setCN2Parameters(int _beamSize, double _performanceThreshold)
    {
        beamSize             = _beamSize;
        performanceThreshold = _performanceThreshold;
    }
    
    /**
     * Change the rules to be used in the classification process.
     * @param _rule The new set of rules to use.
     */
    public void setRules(Rule []_rule)
    {
        rule = _rule;
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int i;
        
        // Initialize classifier. Check input datamodel, install classifier output model.
        super.init();
        
        // Check if the CN2 parameters agree with DataModel
        if (performance == PERFORMANCE_ENTROPY)
        {
            if (goal == GOAL_DEFAULT_NEGATIVE)
                throw new ConfigException("Can't combine ENTROPY performance measure and DEFAULT_NEGATIVE goal handling.");
        }
        else if (performance == PERFORMANCE_RELATIVE_FREQUENCY)
        {
            if (goal != GOAL_DEFAULT_NEGATIVE)
                throw new ConfigException("Can't combine RELATIVE_FREQUENCY performance with non-DEFAULT_NEGATIVE goal handling.");
            if (attgoal.getNumberOfGoalClasses() > 2)
                throw new ConfigException("Can't have DEFAULT_NEGATIVE goal handling and multi-class goal.");
        }
        
        // If there's already rules here. Connect them to the DataModel
        if (rule != null) for (i=0; i<rule.length; i++) rule[i].setDataModel(dataModel);
    }
    
    public void cleanUp() throws DataFlowException
    {
        
    }
    
    public String getOutputName(int port)
    {
        if (port == 0) return("Classified Output");
        else return(null);
    }
    
    public String getInputName(int port)
    {
        if (port == 0) return("Categorical Input");
        else return(null);
    }
    
    // *********************************************************\
    // *                CN2 Classifier Creation                *
    // *********************************************************/
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        // Make sure the input contains object or double based categorical data
        checkClassifierDataModelFit(dataModel, true, true, false);
    }
    
    public CN2()
    {
        super();
        name        = "CN2";
        description = "Sequential Covering Algorithm using CN2-Style Rule Finding.";
    }   
 }