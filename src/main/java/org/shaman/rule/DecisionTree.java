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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.Classifier;
import org.shaman.learning.ClassifierTransformation;
import org.shaman.learning.Presenter;

import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Decision Tree Classifier</h2>
 * Decision Tree flow with ID3-style learning.
 * Missing values handling is supported.
 * The tree can be flattened into a set of rules.
 */

// *********************************************************\
// *    Decision Tree Classification with ID3 Learning     *
// *********************************************************/
public class DecisionTree extends ClassifierTransformation implements Classifier, Persister
{
    public static final int PRUNE_NONE                = 0;
    //public static final int PRUNE_SUBTREE_REPLACEMENT = 1;
    //public static final int PRUNE_SUBTREE_RAISING     = 2;
    
    public static final int MAXDEPTH_INFINITE = -1;
    
    private int  prune;          // Type of pruning
    private int  maxDepth;       // Maximum depth of the Tree
    private int  minObjects;     // Minimum number of Object that needs to be present in a branch
    
    // ---------
    private int []insind;        // [i] = position in instance of Attribute i. -1 if not in instance.
    private Tree  root;          // The root of this decision tree
    
    // *********************************************************\
    // *             Decision Tree Classification              *
    // *********************************************************/
    private int classifyObject(Object instance, double []confidence) throws LearnerException
    {
        ObjectMatrix1D   oins;
        DoubleMatrix1D   dins;
        int              i, cl;
        double           clmax;
        Attribute      []actatt;
        AttributeDouble  attdonow;
        AttributeObject  attobnow;
        boolean          missing, match, found;
        double         []cpdf;
        Tree             branch;
        DataModel        dataModel;
        
        cl        = -1;
        dataModel = this.dataModel;
        actatt    = dataModel.getActiveAttributes();
        
        // Check if there are missing values that cannot be handling correctly.
        missing = false; dins = null; oins = null;
        if (this.primitive)
        {
            dins = (DoubleMatrix1D)instance;
            for (i=0; (i<actatt.length) && (!missing); i++)
            {
                attdonow = (AttributeDouble)actatt[i];
                if ((attdonow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN) &&
                    (attdonow.isMissingAsDouble(dins.getQuick(i)))               ) missing = true;
            }
        }
        else
        {
            oins = (ObjectMatrix1D)instance;
            for (i=0; (i<actatt.length) && (!missing); i++)
            {
                attobnow = (AttributeObject)actatt[i];
                if ((attobnow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN) &&
                    (attobnow.isMissingAsObject(oins.getQuick(i)))               ) missing = true;
            }
        }
        if (missing) return(-1);
        
        // Classify the instance with the decision tree
        cpdf   = null;
        found  = false;
        branch = this.root;
        while (!found) // Move down the tree as far as possible.
        {
            // Find the matching branch to the next level.
            i     = 0;
            match = false;
            while (!match)
            {
                // None of the branches matched... Stop here.
                if (i == branch.getSubTree().length)
                {
                    match = true;
                    found = true;
                    cpdf  = branch.getClassPDF();
                }
                else if (branch.getSubTree()[i] != null) // Branch exists.
                {
                    try
                    {
                        // Try to match with the current branch...
                        if (this.primitive) match = branch.getSubTree()[i].getCondition().apply(dins);
                        else                match = branch.getSubTree()[i].getCondition().apply(oins);
                    }
                    catch(DataModelException ex) { throw new LearnerException(ex); }
                    
                    if (match)
                    {
                        // If matched. Move down the matching branch to the next level in the decision tree.
                        branch = branch.getSubTree()[i];
                        if (branch.getSubTree() == null) // This is a leaf node. Stop here.
                        {
                            found = true;
                            cpdf  = branch.getClassPDF();
                        }
                    }
                    else i++; // Branch didn't match. Try next one.
                }
                else i++; // Branch doesn't exist. Try next one.
            }
        }
        
        // Find the class with highest probability in the node where the tree search ended.
        cl = 0; clmax = 0;
        for (i=0; i<cpdf.length; i++) if (cpdf[i] > clmax) { cl = i; clmax = cpdf[i]; }
        if (confidence != null)
        {
            for (i=0; i<confidence.length; i++) confidence[i] = 0;
            confidence[cl] = cpdf[cl];
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
    
    // **********************************************************\
    // *                    Learner Interface                   *
    // **********************************************************/
    public void initializeTraining() throws LearnerException
    {
        initID3();
    }
    
    public void train() throws LearnerException
    {
        ID3();
    }
    
    public Presenter getTrainSet()
    {
        return(this.trainData);
    }
    
    public void setTrainSet(Presenter _instances)
    {
        this.trainData = _instances;
        this.dataModel = _instances.getDataModel();
    }
    
    public boolean isSupervised()
    {
        return(true);
    }
    
    // *********************************************************\
    // *               Parameter and Model Access              *
    // *********************************************************/
    /**
     * Get the Decision Tree model.
     * @return The decision tree.
     */
    public Tree getTree()           { return(this.root); }
    
    /**
     * Set the decision tree model.
     * @param _root The decision tree
     */
    public void setTree(Tree _root) { this.root = _root; }
    
    /**
     * Set the type of Pruning algorithm to use.
     * @param _prune Type of pruning to use.
     */
    public void setPruneType(int prune)  { this.prune = prune; }
    
    /**
     * Set the maximum number if decision nodes on any path trough the Tree.
     * @param maxDepth Maximum depth of the Tree
     */ 
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    
    /**
     * Set the minimum number of objects that need to be present for a branch to form.
     * @param minObjects Minimum number of Object needed for a branch.
     */
    public void setMinObjects(int minObjects) { this.minObjects = minObjects; }
    
    /**
     * Flatten the tree into a set of rules.
     * @return The set of rules corresponding to the decision tree.
     */
    public Rule []getRules() throws LearnerException
    {
        return(this.root.getRules(this.dataModel));
    }
    
    // *********************************************************\
    // *                    ID3 Induction                      *
    // *********************************************************/
    private void initID3() throws LearnerException
    {
        int       i,j;
        
        // Initialize the ID3 parameters
        this.prune = PRUNE_NONE;
        
        // Make attribute index -> act
        this.actind = this.dataModel.getActiveIndices();
        this.insind = new int[this.dataModel.getAttributes().length]; j = 0;
        for (i=0; i<this.insind.length; i++)
        {
            if (this.dataModel.getAttribute(i).getIsActive()) this.insind[i] = j++;
            else                                              this.insind[i] = -1;
        }
    }
    
    private void ID3() throws LearnerException
    {
        int          i;
        IntArrayList examples;
        ArrayList    attributes;
        
        this.root = null;
        
        // Make a list of all Attribute to choose from
        attributes = new ArrayList(this.dataModel.getAttributes().length);
        for (i=0; i<this.dataModel.getAttributes().length; i++)
            if (this.dataModel.getAttribute(i).getIsActive()) attributes.add(new Integer(i));
            
        // Make a list of all points to train on.
        examples   = new IntArrayList(this.trainData.getNumberOfInstances());
        for (i=0; i<this.trainData.getNumberOfInstances(); i++) examples.add(i);
        
        try
        {
            // Do ID3 induction
            this.root = ID3Rec(1, null, 0, examples, attributes);
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
    }
    
    private Tree ID3Rec(int depth, String attname, int cat, IntArrayList examples, ArrayList attributes) throws LearnerException, DataModelException
    {
        int            i, numclass, inclass;
        IntArrayList []exclass;
        Tree           root;
        int            ind;
        double       []cpdf;
        double         sumc;
        Condition      seldat;
        boolean        allsame;
        
        if (examples.size() == 0) return(null);
        
        // Split the examples up according to their goal class. And calculate class probability distribution
        numclass = this.attgoal.getNumberOfGoalClasses();
        cpdf     = new double[numclass];
        exclass  = new IntArrayList[numclass];
        for (i=0; i<exclass.length; i++) exclass[i] = new IntArrayList();
        for (i=0; i<examples.size(); i++)
        {
            // Add index of current example to list of it's class. Add example weight to sum of class weights.
            ind            = examples.get(i);
            inclass        = this.trainData.getGoalClass(ind);
            cpdf[inclass] += this.trainData.getWeight(ind);
            exclass[inclass].add(ind);
        }
        // Create the PDF of class distribution taking into account instance weighting.
        sumc = 0;
        for (i=0; i<cpdf.length; i++) sumc   += cpdf[i];
        for (i=0; i<cpdf.length; i++) cpdf[i] = cpdf[i] / sumc;
        
        // Make selection condition. Always follows format 'attribute = value'. 
        if (attname != null) seldat = new Condition(this.dataModel, attname, Condition.IS_EQUAL, new Double(cat));
        else                 seldat = null;
        root = new Tree(this.dataModel, seldat, cpdf);
        
        // Stop expanding when examples all fall in the same class or if there are no attributes left.
        allsame = false;
        for (i=0; (i<numclass) && (!allsame); i++) if (exclass[i].size() == examples.size()) allsame = true;
        if ((!allsame) && (attributes.size() > 0))
        {
            Attribute       attnow, attmax;
            int             j, posnow, posmax;
            Object          posmaxob;
            double          ig, igmax;
            IntArrayList  []exbranch;
            IntArrayList  []exbranchmax;
            
            // Find the attribute with the highest information gain.
            posmaxob    = (Integer)attributes.get(0);
            posmax      = ((Integer)posmaxob).intValue();
            attmax      = this.dataModel.getAttribute(posmax);
            exbranchmax = new IntArrayList[attmax.getNumberOfCategories()];
            igmax       = infoGain(examples, posmax, exbranchmax);
            for (j=1; j<attributes.size(); j++)
            {
                // Calculate information-gain of given Attribute
                posnow   = ((Integer)attributes.get(j)).intValue();
                attnow   = this.dataModel.getAttribute(posnow);
                exbranch = new IntArrayList[attnow.getNumberOfCategories()];
                ig       = infoGain(examples, posnow, exbranch);
                if (ig >= igmax)
                {
                    // Remember Attribute with highest gain. Branch on this Attribute
                    posmax      = posnow;
                    igmax       = ig;
                    exbranchmax = exbranch;
                    attmax      = attnow;
                    posmaxob    = (Integer)attributes.get(j);
                }
            }
            
            
            Tree      []subtree;
            ArrayList   attsub;
            boolean     recurse;
            
            // Remove the Attribute of this tree from the list of available Attributes
            attsub  = (ArrayList)attributes.clone();
            attsub.remove(posmaxob);
            
            // Create sub-tree, recursively train them
            subtree = new Tree[exbranchmax.length];
            for (i=0; i<subtree.length; i++)
            {
                // Make sure it's still necessary to recurse into the branch...
                recurse = ((this.maxDepth == -1) || (depth                 <  this.maxDepth)) &&
                                                    (exbranchmax[i].size() >= this.minObjects);
                if (recurse) subtree[i] = ID3Rec(depth+1, attmax.getName(), i, exbranchmax[i], attsub);
                else         subtree[i] = null;
            }
            
            // Set the subtrees of this node.
            root.setSubTree(subtree);
        }
        
        return(root);
    }
    
    // *********************************************************\
    // *               Information Gain Criterium              *
    // *********************************************************/
    private double infoGain(IntArrayList examples, int attind, IntArrayList []exbranch) throws DataModelException, LearnerException
    {
        int             i;
        double          ig;
        int             ind;
        int             mispos, cat;
        double          val;
        Object          oval;
        Attribute       attnow;
        AttributeDouble attdonow;
        AttributeObject attobnow;
        double          wei, weiexall;
        double        []weiexbranch;
        double          enall;
        double          enbranch;
        double          enpar;
        
        ig     = 0;
        attnow = this.dataModel.getAttribute(attind);
        mispos = attnow.getNumberOfCategories()-1;
        
        // Calculate the entropy of the entire example set.
        enall = entropy(examples);
        
        // Make empty sets that will contain the examples partitioned over the value at the indicated index.
        for (i=0; i<exbranch.length; i++) exbranch[i] = new IntArrayList();
        
        // Partition the examples in the sub-branches according to type attribute
        weiexbranch = new double[exbranch.length];
        weiexall    = 0;
        for (i=0; i<examples.size(); i++)
        {
            ind = examples.get(i);
            cat = -1;
            if (this.primitive)
            {
                attdonow = (AttributeDouble)attnow;
                val      = this.trainData.getInstance(ind).getQuick(this.insind[attind]);
                if (attdonow.isMissingAsDouble(val)) // Missing value?
                {
                    if      (attdonow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN) cat = -1;     // Treat missing values as if they don't exist
                    else if (attdonow.getMissingIs() == Attribute.MISSING_IS_VALUE)   cat = mispos; // Treat them is a special kind of value
                }
                else cat = attdonow.getCategory(val);
            }
            else
            {
                attobnow = (AttributeObject)attnow;
                oval     = this.trainData.getObjectInstance(ind).getQuick(this.insind[attind]);
                if (attobnow.isMissingAsObject(oval)) // Missing Object Value?
                {
                    if      (attobnow.getMissingIs() == Attribute.MISSING_IS_UNKNOWN) cat = -1;     // Treat missing values as if they don't exist
                    else if (attobnow.getMissingIs() == Attribute.MISSING_IS_VALUE)   cat = mispos; // Treat them is a special kind of value
                }
                else cat = attobnow.getCategory(oval);
            }
            
            // Add current example to it's branch's example list. Add it's weight to the sum of weights.
            if (cat != -1)
            {
                exbranch[cat].add(ind);
                wei               = this.trainData.getWeight(ind);
                weiexbranch[cat] += wei;
                weiexall         += wei;
            }
        }
        
        // Calculate the entropy over the partitions
        enpar    = 0;
        for (i=0; i<exbranch.length; i++)
        {
            if (weiexbranch[i] > 0)
            {
                enbranch = entropy(exbranch[i]);
                enpar   += (weiexbranch[i] / weiexall) * enbranch;
            }
        }
        
        // Information gain is the difference between entropy of the entire set and the entropy over the partitions
        ig = enall - enpar;
        
        return(ig);
    }
    
    private double entropy(IntArrayList examples) throws DataModelException, LearnerException
    {
        int      i, exind, inclass;
        double   log2;
        double   wei, weiall;
        double []numclass;
        double []pclass;
        double   en;
        
        log2 = Math.log(2.0);
        
        // Calculate the entropy of the given set taking into account the instance weighting.
        numclass = new double[this.attgoal.getNumberOfGoalClasses()];
        pclass   = new double[this.attgoal.getNumberOfGoalClasses()];
        for (i=0; i<numclass.length; i++) { pclass[i] = 0; numclass[i] = 0; }
        weiall = 0;
        for (i=0; i<examples.size(); i++)
        {
            exind   = examples.get(i);
            inclass = this.trainData.getGoalClass(exind);
            wei     = this.trainData.getWeight(exind);
            numclass[inclass] += wei;
            weiall            += wei;
            
        }
        for (i=0; i<numclass.length; i++) pclass[i] = numclass[i] / weiall;
        
        en = 0;
        for (i=0; i<numclass.length; i++)
        {
            if (pclass[i] != 0) en -= pclass[i]*(Math.log(pclass[i])/log2);
        }
        
        return(en);
    }
    
    // **********************************************************\
    // *            Transformation/Flow Interface               *
    // **********************************************************/
    public void init() throws ConfigException
    {
        int  i,j;
        
        // Set input/output datamodel of the right type
        super.init();
        
        // Make attribute index -> active
        this.insind = new int[this.dataModel.getAttributes().length];
        j = 0;
        for (i=0; i<insind.length; i++)
        {
            if (this.dataModel.getAttribute(i).getIsActive()) this.insind[i] = j++;
            else                                              this.insind[i] = -1;
        }
        
        if (this.root != null)
        {
            // Connect the Tree's conditions to the DataModel
            this.root.setDataModel(dataModel);
        }
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void checkDataModelFit(int port, DataModel dataModel) throws DataModelException
    {
        checkClassifierDataModelFit(dataModel, true, true, true);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            super.loadState(oin);
            this.prune      = oin.readInt();
            this.maxDepth   = oin.readInt();
            this.minObjects = oin.readInt();
            this.root  = new Tree();
            this.root.loadState(oin);
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            super.saveState(oout);
            oout.writeInt(this.prune);
            oout.writeInt(this.maxDepth);
            oout.writeInt(this.minObjects);
            this.root.saveState(oout);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *          Decision Tree Classifier Creation            *
    // *********************************************************/
    public DecisionTree()
    {
        super();
        this.name        = "Decision Tree";
        this.description = "Decision Tree for sets of discrete attributes.";
        
        this.prune      = PRUNE_NONE;
        this.maxDepth   = -1;
        this.minObjects =  1;
    }
}