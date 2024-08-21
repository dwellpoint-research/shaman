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


import org.shaman.datamodel.DataModel;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Rule</h2>
 * Variable-free, propositional rule of the form
 * <it>antecedence -> concequence<it><br>
 * with both parts of the form
 * ((cond0 pred0 cond1) pred1 cond2) pred2 cond3) pred3 cond4) ....
 *
 * <br>
 * Used by algorithms that use or output propositional rules.
 * e.g. PRISM, Decision Tree, CN2 or Routing Logic between Flows.
 * Not strong enough for ILP purposes.
 *
 * <br>
 * @author Johan Kaers
 * @version 2.0
 */

// *********************************************************\
// *                          Rule                         *
// *********************************************************/
public class Rule implements Cloneable, Comparable
{
    /** Logical AND between 2 conditions */
    public static final int PREDICATE_AND = 0;
    /** Logical OR between 2 conditions */
    public static final int PREDICATE_OR  = 1;
    
    /** The DataModel of the data on which this rule is applied */
    private DataModel   dataModel;
    /** Antecedence conditions (n) */
    private Condition []antCond;
    /** Antecedence conditions join logic (n-1) */
    private int       []antPred;
    /** Concequence conditions (m) */
    private Condition []conCond;
    /** Antecedence conditions joint logic (m-1) */
    private int       []conPred;
    /** Performance measure of this rule. Used in ordering them */
    private double    performance;
    
    // **********************************************************\
    // *                      Data Access                       *
    // **********************************************************/
    /**
     * Change the antecedence of this rule.
     * @param _antCond The new conditions in the antecedence of the rule.
     * @param _antPred The new predicates joining the conditions in the antecedence
     */
    public void setAntecedence(Condition []_antCond, int []_antPred)
    {
        antCond = _antCond;
        antPred = _antPred;
    }
    
    /**
     * Change the consequence of the rule
     * @param _conCond The new conditions of the consequence of this rule.
     * @param _conPred The new predicates joining the conditions in the consequence
     */
    public void setConsequence(Condition []_conCond, int []_conPred)
    {
        conCond = _conCond;
        conPred = _conPred;
    }
    
    /**
     * Get the conditions of the antecedence of the rule.
     * @return An array with the conditions in the antecedence
     */
    public Condition []getAntecedenceConditions() { return(antCond); }
    
    /**
     * Get the predicates that connect the conditions in the antecedence.
     * @return The predicates that connect the conditions of the antcedence.
     */
    public int       []getAntecedencePredicates() { return(antPred); }
    
    /**
     * Get the conditions of the consequence of this rule.
     * @return The conditions of the consequence of this rule.
     */
    public Condition []getConcequenceConditions() { return(conCond); }
    
    /**
     * Get the predicates that connect the conditions in the consequence of this rule.
     * @return The predicates of the consequence of this rule.
     */
    public int       []getConcequencePredicates() { return(conPred); }
    
    /**
     * Get the performance measure of this rule. Used in ordering rule (via the Comparable interface).
     * @return The current performance of this rule.
     */
    public double getPerformance()                    { return(performance); }
    
    /**
     * Set the performance of this rule.
     * @param _performance The current performance of this rule.
     */
    public void   setPerformance(double _performance) { performance = _performance; }
    
    /**
     * Get the DataModel of the data on which this rule is applied.
     * @return The DataModel of this rule.
     */
    public DataModel getDataModel() { return(dataModel); }
    
    /**
     * Change the DataModel on which this rule has to work.
     * Also adjust the Conditions' DataModel
     * @param _dataModel The new DataModel to use.
     * @throws DataModelException If the new DataModel is incompatible with some condition of this rule.
     */
    public void setDataModel(DataModel _dataModel) throws DataModelException
    {
        int i;
        
        dataModel = _dataModel;
        if (antCond != null) for (i=0; i<antCond.length; i++) antCond[i].setDataModel(dataModel);
        if (conCond != null) for (i=0; i<conCond.length; i++) conCond[i].setDataModel(dataModel);
    }
    
    // **********************************************************\
    // *             Rule Antecedence Checking                   *
    // **********************************************************/
    /**
     * Test if the given Object instance matches with this rule.
     * @param instance The instance to test
     * @return <code>true</code> if the Object instance matches the rule. <code>false</false> if it doesn't.
     * @throws LearnerException If it cannot be checked if this rule matches the instance.
     */
    public boolean apply(ObjectMatrix1D instance) throws LearnerException
    {
        boolean match;
        int     i;
        
        match = true;
        try
        {
            if (antCond.length > 0)
            {
                match = antCond[0].apply(instance);
                for (i=1; i<antCond.length; i++)
                {
                    if      (antPred[i-1] == PREDICATE_AND) match = match && antCond[i].apply(instance);
                    else if (antPred[i-1] == PREDICATE_OR)  match = match || antCond[i].apply(instance);
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(match);
    }
    
    /**
     * Test if the given Object instance matches with this rule.
     * @param instance The instance to test
     * @return <code>true</code> if the Object instance matches the rule. <code>false</false> if it doesn't.
     * @throws LearnerException If it cannot be checked if this rule matches the instance.
     */
    public boolean apply(DoubleMatrix1D instance) throws LearnerException
    {
        boolean match;
        int     i;
        
        match = true;
        try
        {
            if (antCond.length > 0)
            {
                match = antCond[0].apply(instance);
                for (i=1; i<antCond.length; i++)
                {
                    if      (antPred[i-1] == PREDICATE_AND) match = match && antCond[i].apply(instance);
                    else if (antPred[i-1] == PREDICATE_OR)  match = match || antCond[i].apply(instance);
                }
            }
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(match);
    }
    
    // **********************************************************\
    // *                  Classification Rule                   *
    // **********************************************************/
    /**
     * Make a rule of following form :
     * <code> (att[0] == cat[0]) && (att[1] == cat[1]) ... (att[n] == cat[n]) </code>
     * @param _dataModel The DataModel to use in this rule.
     * @param att The attribute names of the conditions.
     * @param cat The categories that should be matched by the conditions.
     */
    public Rule(DataModel _dataModel, String []att, int []cat)
    {
        // Make a for categorical attributes. With att[] the names, cat[] the required category of the attributes' data.
        // (att[0] == cat[0]) && (att[1] == cat[1]) ... (att[n] == cat[n])
        int i;
        dataModel = _dataModel;
        antCond   = new Condition[att.length];
        antPred   = new int[att.length-1];
        for (i=0; i<antCond.length; i++)
        {
            antCond[i] = new Condition(dataModel, att[i], Condition.IS_EQUAL, new Double(cat[i]));
            if (i != antPred.length) antPred[i] = Rule.PREDICATE_AND;
        }
    }
    
    /**
     * Get the goal-class in a rule of the form :
     * <code> (att[0] == cat[0]) && (att[1] == cat[1]) ... (att[n] == cat[n]) ---> goal-class </code>
     * @return The goal class that is the consequence of this rule.
     */
    public int getGoalClassConsequence()
    {
        int gc = ((Double)conCond[0].getValue()).intValue();
        
        return(gc);
    }
    
    /**
     * Set the goal-class in a rule of the form :
     * <code> (att[0] == cat[0]) && (att[1] == cat[1]) ... (att[n] == cat[n]) ---> goal-class </code>
     * @param goalatt The name of the goal attribute
     * @param goalClass The goal-class
     */
    public void setGoalClassConsequence(String goalatt, int goalClass)
    {
        // The consequence of this rule is that the instance's goal-class is 'goalClass'
        conCond = new Condition[]{new Condition(dataModel, goalatt, Condition.IS_EQUAL, new Double(goalClass))};
        conPred = new int[0];
    }
    
    /**
     * Make a rule that extends the given one with 1 condition : <code>AND (att == cat)</code>
     * @param r The rule to extend
     * @param att The name of the attribute involved in the new condition.
     * @param cat The category involved in the new condition
     * @throws LearnerException if there was trouble making the new rule.
     * @return The given rule extended with the condition <code>AND (att == cat)</code>
     */
    public static Rule specialize(Rule r, String att, int cat) throws LearnerException
    {
        Rule      rout;
        Condition cond;
        
        cond = new Condition(r.dataModel, att, Condition.IS_EQUAL, new Double(cat));
        rout = specialize(r, cond);
        
        return(rout);
    }
    
    /**
     * Make a rule that extends the given one by ANDing with the given condition
     * @param r The rule to extend
     * @param cond The condition the concatenate with an AND
     * @throws LearnerException If there was trouble making the new rule.
     * @return The given rule ANDed with the given condition.
     */
    public static Rule specialize(Rule r, Condition cond) throws LearnerException
    {
        // Extends the given (att_1 == cat_1) AND ... AND(att_n == cat_n) rule
        // with: AND (att_n+1 == cat_n+1)
        // Does not generate inconsistent rules.
        int     i;
        Rule    rout;
        boolean consistent;
        
        rout = null;
        // Check if the new condition's attribute is already in the rule.
        // If so then this new rule cannot be made
        consistent = true;
        for (i=0; (i<r.antCond.length) && (consistent); i++)
        {
            consistent = !(r.antCond[i].getAttribute().equals(cond.getAttribute()));
        }
        
        if (consistent)
        {
            // Make the extended, consistent rule.
            try
            {
                rout = (Rule)r.clone();
                rout.antPred = new int[r.antPred.length+1];
                rout.antCond = new Condition[r.antCond.length+1];
                for (i=0; i<r.antPred.length; i++) rout.antPred[i] = r.antPred[i];
                rout.antPred[i] = Rule.PREDICATE_AND;
                for (i=0; i<r.antCond.length; i++) rout.antCond[i] = r.antCond[i];
                rout.antCond[i] = cond;
            }
            catch(CloneNotSupportedException ex) { throw new LearnerException(ex); }
        }
        
        return(rout);
    }
    
    // **********************************************************\
    // *                 Basic Java Object Operation            *
    // **********************************************************/
    /**
     * Make an empty Rule.
     */
    public Rule()
    {
    }
    
    /**
     * Make an empty rule for the given DataModel.
     * Empty antecedence and consequence. Matches everyting.
     * @param _dataModel The dataModel that is used by the applied data.
     */
    public Rule(DataModel _dataModel)
    {
        this.dataModel = _dataModel;
        this.antCond   = new Condition[0];
        this.antPred   = new int[0];
        this.conCond   = new Condition[0];
        this.conPred   = new int[0];
    }
    
    /**
     * Make a copy of this rule.
     * @return A copy of this rule.
     * @throws CloneNotSupportedException If a copy cannot be made.
     */
    public Object clone() throws CloneNotSupportedException
    {
        Rule rout = new Rule();
        
        rout.antCond   = (Condition [])this.antCond.clone();
        rout.antPred   = (int [])this.antPred.clone();
        rout.conCond   = (Condition [])this.conCond.clone();
        rout.conPred   = (int [])this.conPred.clone();
        rout.dataModel = this.dataModel;
        
        return(rout);
    }
    
    /**
     * Check if this rule and the given one are equal.
     * It assumes all predicates are the same. That way, equality comes down to have the same conditions.
     * @param o2 The other rule.
     * @return <code>true</code> if the 2 rules are equal. <code>false</code> if they're not.
     */
    public boolean equals(Object o2)
    {
        int     i;
        Rule    r2;
        boolean eq;
        
        // Not null and same type?
        eq = false;
        if ((o2 == null) || ((o2 != null) && !(o2 instanceof Rule))) eq = false;
        else
        {
            r2 = (Rule)o2;
            // Same Size?
            if (    (antCond.length == r2.antCond.length)
                    && (antPred.length == r2.antPred.length)
                    && (conCond.length == r2.conCond.length)
                    && (conPred.length == r2.conPred.length)) eq = false;
            else
            {
                // Assume all predicates are the same.
                // Check if the same conditions occur and if they have the same values.
                int j;
                int []cpos2;
                cpos2 = new int[antCond.length];
                for (i=0; (i<antCond.length) && (eq); i++)
                {
                    cpos2[i] = -1;
                    for (j=0; j<r2.antCond.length; j++)
                        if (r2.antCond[j].getAttribute().equals(antCond[i].getAttribute())) cpos2[i] = j;
                    if (cpos2[i] == -1) eq = false;
                }
                if (eq)
                {
                    for (i=0; i<cpos2.length; i++)
                        eq = antCond[i].getValue().equals(r2.antCond[cpos2[i]].getValue());
                }
            }
        }
        
        return(eq);
    }
    
    /**
     * Make a human-reable String from this rule.
     * @return A String of the form :
     * <code>IF condition(1) PREDICATE1 .... PREDICATE(n-1) condition(n)
     *       THEN
     *          condition(1) PREDICATE1 .... PREDICATE(n-1) condition(n)
     */
    public String toString()
    {
        int          i;
        StringBuffer sb = new StringBuffer();
        
        sb.append("IF ");
        if (antCond != null)
        {
            for (i=0; i<antCond.length; i++)
            {
                sb.append("("+antCond[i].toString()+")");
                if (i != (antCond.length-1))
                {
                    if      (antPred[i] == PREDICATE_AND) sb.append(" AND \n   ");
                    else if (antPred[i] == PREDICATE_OR)  sb.append(" OR \n   ");
                }
            }
        }
        else sb.append("TRUE");
        sb.append("\nTHEN ");
        if (conCond != null)
        {
            for (i=0; i<conCond.length; i++)
            {
                sb.append("("+conCond[i].toString()+")");
                if (i != (conCond.length-1))
                {
                    if      (conPred[i] == PREDICATE_AND) sb.append(" AND \n     ");
                    else if (conPred[i] == PREDICATE_OR)  sb.append(" OR \n     ");
                }
            }
        }
        else sb.append("TRUE");
        
        return(sb.toString());
    }
    
    /**
     * Rule ordering using the Comparable interface.
     * Orders from high to low 'performance'
     * @param o2 The other Rule
     * @return -1 if this rule has lower performance than the other Rule
     *          0 if both rules has the same performance
     *          1 if the other rule has higher performance
     */
    public int compareTo(Object o2)
    {
        int o;
        
        if ((o2 == null) || ((o2 != null) && !(o2 instanceof Rule))) o = 0;
        else
        {
            Rule r2 = (Rule)o2;
            if      (performance  > r2.performance) o =  1;
            else if (performance == r2.performance) o =  0;
            else                                    o = -1;
        }
        
        return(o);
    }
}
