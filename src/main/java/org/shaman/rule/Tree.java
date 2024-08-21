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
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;

import org.shaman.dataflow.Persister;
import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;


/**
 * <h2>Decision Tree</h2>
 * Decision Tree model. All nodes in the tree have a Condition that should hold when they
 * are selected. They also contain the probability distribution of the goal-class and the
 * branches leaving from this node.
 */

// *********************************************************\
// *                      Decision Tree                    *
// *********************************************************/
public class Tree implements Persister
{
    private Condition   cond;        // Condition for selecting this branch
    private double    []cpdf;        // PDF of Class Distribution at this node
    private Tree      []subTree;     // The branches of this node.
    private DataModel   dataModel;   // The DataModel used by this Tree.
    
    // *********************************************************\
    // *        Decision Tree -> Set of Rules Conversion       *
    // *********************************************************/
    /**
     * Flatten the Decision Tree into an equivalent set of rules.
     * @param dataModel The datamodel used in the rules.
     * @return The flattened tree.
     * @throws LearnerException If the tree could not by flattened.
     */
    protected Rule []getRules(DataModel dataModel) throws LearnerException
    {
        // All paths from root the leaf become a rule.
        LinkedList rules;
        Rule     []rule;
        
        // Start at the root with an empty rule. Recurse down from root the all leaves.
        try
        {
            rules  = new LinkedList();
            getRulesRec(rules, new Rule(dataModel));
        }
        catch(CloneNotSupportedException ex) { throw new LearnerException(ex); }
        
        // Convert the list to an array of rules.
        rule = (Rule [])rules.toArray(new Rule[]{});
        return(rule);
    }
    
    private void getRulesRec(LinkedList rules, Rule rulepart) throws LearnerException, CloneNotSupportedException
    {
        int       i;
        double    max;
        int       cl;
        Condition newcond;
        Rule      newrule;
        String    goalname;
        
        if (getSubTree() == null)
        {
            // Reached a leaf. Set the most occuring class as consequence.
            cl = -1; max = -1;
            for (i=0; i<this.cpdf.length; i++) if (this.cpdf[i] > max) { cl = i; max = this.cpdf[i]; }
            newrule  = (Rule)rulepart.clone();
            goalname = this.dataModel.getAttribute(this.dataModel.getLearningProperty().getGoalIndex()).getName();
            newrule.setGoalClassConsequence(goalname, cl);
            rules.addLast(newrule);
        }
        else
        {
            // Recurse down all subtrees. Add the selection condition of the subtree to the partial rule.
            for (i=0; i<this.subTree.length; i++)
            {
                if (this.subTree[i] != null)
                {
                    newcond = this.subTree[i].getCondition();
                    newrule = Rule.specialize(rulepart, newcond);
                    this.subTree[i].getRulesRec(rules, newrule);
                }
            }
        }
    }
    
    // *********************************************************\
    // *                   Tree Operations                     *
    // *********************************************************/
    public void      setCondition(Condition _cond)  { this.cond = _cond; }
    public Condition getCondition()                 { return(this.cond); }
    public void      setClassPDF(double []_cpdf)    { this.cpdf = _cpdf; }
    public double  []getClassPDF()                  { return(this.cpdf); }
    public void      setSubTree(Tree []_subTree)    { this.subTree = _subTree; }
    public Tree[]    getSubTree()                   { return(this.subTree); }
    
    public void setDataModel(DataModel _dataModel) throws DataModelException
    {
        int i;
        if (this.subTree != null)
        {
            for (i=0; i<this.subTree.length; i++)
                if (this.subTree[i] != null) this.subTree[i].setDataModel(_dataModel);
        }
        this.dataModel = _dataModel;
        if (this.cond != null) this.cond.setDataModel(this.dataModel);
    }
    
    public DataModel getDataModel() { return(this.dataModel); }
    
    public void setLeaf(Condition leaf)
    {
        this.subTree = null;
        this.cond    = leaf;
    }
    
    // *********************************************************\
    // *                 Plain Text I/O                        *
    // *********************************************************/
    public void save(Writer out) throws java.io.IOException
    {
        out.write("TREE\n");
        saveRec(out, 0, 10000);
        out.flush();
    }
    
    public void save(Writer out, int maxlev) throws java.io.IOException
    {
        out.write("TREE\n");
        saveRec(out, 0, maxlev);
        out.flush();
    }
    
    private void saveRec(Writer out, int lev, int maxlev) throws java.io.IOException
    {
        int    i;
        String pad;
        String pdf;
        
        if (lev <= maxlev)
        {
            pad = "";
            for (i=0; i<lev; i++) pad += "     ";
            pdf = "";
            for (i=0; i<this.cpdf.length; i++) pdf += this.cpdf[i]+"  ";
            
            if (this.cond != null) out.write(pad+this.cond.toString()+" | "+pdf+"\n");
            if (this.subTree != null)
            {
                for (i=0; i<this.subTree.length; i++)
                {
                    if (this.subTree[i] != null) this.subTree[i].saveRec(out, lev+1, maxlev);
                    else                         out.write(pad+"   null\n");
                }
            }
        }
    }
    
    public String toString()
    {
        String st = null;
        try
        {
            StringWriter sw = new StringWriter();
            save(sw);
            st = (sw.getBuffer()).toString();
        }
        catch(java.io.IOException ex) { ex.printStackTrace(); }
        
        return(st);
    }
    
    // **********************************************************\
    // *             State Persistence Implementation           *
    // **********************************************************/
    public void loadState(ObjectInputStream oin) throws ConfigException
    {
        try
        {
            int  numsubtree, i;
            
            if (oin.readBoolean())
            {
                this.cond = new Condition();
                this.cond.loadState(oin);
            }
            this.cpdf = (double [])oin.readObject();
            
            numsubtree = oin.readInt();
            if (numsubtree > 0)
            {
                this.subTree = new Tree[numsubtree];
                for (i=0; i<numsubtree; i++)
                {
                    if (oin.readBoolean())
                    {
                        this.subTree[i] = new Tree();
                        this.subTree[i].loadState(oin);
                    }
                    else this.subTree[i] = null;
                }
            }
            else this.subTree = null;
        }
        catch(IOException ex)            { throw new ConfigException(ex); }
        catch(ClassNotFoundException ex) { throw new ConfigException(ex); }
    }
    
    public void saveState(ObjectOutputStream oout) throws ConfigException
    {
        try
        {
            int i;
            
            if (this.cond != null)
            {
                oout.writeBoolean(true);
                this.cond.saveState(oout);
            }
            else oout.writeBoolean(false);
            
            oout.writeObject(this.cpdf);
            
            if (this.subTree != null)
            {
                oout.writeInt(this.subTree.length);
                for(i=0; i<this.subTree.length; i++)
                {
                    if (this.subTree[i] != null)
                    {
                        oout.writeBoolean(true);
                        this.subTree[i].saveState(oout);
                    }
                    else oout.writeBoolean(false);
                }
            }
            else oout.writeInt(0);
        }
        catch(IOException ex) { throw new ConfigException(ex); }
    }
    
    // *********************************************************\
    // *                   Tree Construction                   *
    // *********************************************************/
    public Tree(DataModel _dataModel, Condition _cond)
    {
        this.dataModel = _dataModel;
        this.cond      = _cond;
        this.subTree   = null;
    }
    
    public Tree(DataModel _dataModel, Condition _cond, double []_cpdf)
    {
        this.dataModel = _dataModel;
        this.cond      = _cond;
        this.cpdf      = _cpdf;
        this.subTree   = null;
    }
    
    public Tree()
    {
        this.dataModel = null;
        this.cond      = null;
        this.subTree   = null;
        this.cpdf      = null;
    }
}