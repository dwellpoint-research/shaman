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
import java.util.Set;

import junit.framework.TestCase;

import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.function.PostfixMathCommandI;
import org.shaman.TestUtils;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.rule.ExpressionParser;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * Expression Parser Function Test
 */
public class ExpressionParserTest extends TestCase
{
    private ExpressionParser parse;
    
    // **********************************************************\
    // *           Test String Concatenation Function           *
    // **********************************************************/
    public void testStringPlusFunction() throws ConfigException, LearnerException
    {
        DataModelObject  dmtest;
        String           exp;
        Double           number;
        ObjectMatrix1D   invec;
        String           sout;
        
        dmtest = (DataModelObject)TestUtils.makeNumberDataModel(1, false);
        parse.setDataModel(dmtest);
        
        // Test String and Number formatting default behaviour
        exp    = "\"Strings and numbers \"+attribute0";
        parse.parseExp(exp);
        invec  = (ObjectMatrix1D)dmtest.createDefaultVector();
        
        number = new Double(1.0);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 1.0", sout);
        
        number = new Double(0.1250000004);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 0.1250000004", sout);
        
        number = new Double(1000000000);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 1.0E9", sout);
        
        // Switch on eye candy. Format the number according to DecimalFormat pattern '#.##;-#.##'
        ExpressionParser.setFormatNumbers(true);
        number = new Double(1.0);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 1", sout);
        
        number = new Double(0.1250000004);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 0,13", sout);
        
        number = new Double(-0.1250000004);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers -0,13", sout);
        
        number = new Double(1000000000);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers 1000000000", sout);
        
        number = new Double(-1000000000.0055);
        invec.setQuick(0, number);
        sout   = (String)parse.getValueAsObject(invec);
        assertEquals("Strings and numbers -1000000000,01", sout);
    }
    
    public void testEvaluatorSetFunctions() throws ShamanException
    {
        HashSet set1 = new HashSet();
        HashSet set2 = new HashSet();
        HashSet sw1, sw2;
        Set     setout;
        Object  obout;
        
        set1.add("element1"); set1.add("element2"); set1.add("element3");
        set2.add("element2"); set2.add("element4");
        
        ExpressionParser exppar;
        
        exppar = new ExpressionParser();
        
        // Union
        sw1 = (HashSet)set1.clone();
        sw2 = (HashSet)set2.clone();
        exppar.addFunction("set1", new SetFunction(sw1));
        exppar.addFunction("set2", new SetFunction(sw2));
        exppar.parseExp("setunion(set1(), set2())");
        setout = (Set)exppar.getValueAsObject();
        assertEquals(setout.size(), 4);
        
        // Intersection
        sw1 = (HashSet)set1.clone();
        sw2 = (HashSet)set2.clone();
        exppar.addFunction("set1", new SetFunction(sw1));
        exppar.addFunction("set2", new SetFunction(sw2));
        exppar.parseExp("setintersection(set1(), set2())");
        setout = (Set)exppar.getValueAsObject();
        assertEquals(setout.size(), 1);
        
        // Difference
        sw1 = (HashSet)set1.clone();
        sw2 = (HashSet)set2.clone();
        exppar.addFunction("set1", new SetFunction(sw1));
        exppar.addFunction("set2", new SetFunction(sw2));
        exppar.parseExp("setdifference(set1(), set2())");
        setout = (Set)exppar.getValueAsObject();
        assertEquals(setout.size(), 2);
        
        // Membership
        sw1 = (HashSet)set1.clone();
        exppar.addFunction("set1", new SetFunction(sw1));
        exppar.parseExp("setcontains(set1(), \"element5\")");
        obout = exppar.getValueAsObject();
        assertEquals(new Double(0.0), obout);
        exppar.parseExp("setcontains(set1(), \"element3\")");
        obout = exppar.getValueAsObject();
        assertEquals(new Double(1.0), obout);
    }
    
    // **********************************************************\
    // *                     Test-Case Setup                    *
    // **********************************************************/
    protected void setUp() throws Exception
    {
        this.parse = new ExpressionParser();
    }
    
    protected void tearDown() throws Exception
    {
    }
    
    public ExpressionParserTest(String name)
    {
        super(name);
    }
}

class SetFunction extends PostfixMathCommand implements PostfixMathCommandI
{
    Set outset;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        checkStack(stack);
        stack.push(outset);
    }
    
    public SetFunction(Set outset)
    {
        this.outset = outset;
    }
}