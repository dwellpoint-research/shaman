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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.nfunk.jep.ASTConstant;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.FunctionTable;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.function.PostfixMathCommand;
import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.datamodel.DataModelPropertyVectorType;
import org.shaman.datamodel.DistanceDate;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.LearnerException;
import org.shaman.util.DateUtil;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Expression Parser</h2>
 * Java Expression Parser (JEP) extension that integrates
 * with the data-modeling and -flow package. Attributes of a
 * datamodel con be used as variables, Transformations as functions.
 * Contains some basic String, Set and Date manipulation functions.
 * Supports both primitive and Object based vector formats.
 * <br>
 * Based on the Java Expression Parser.
 *     http://www.singularsys.com/jep/
 */

// **********************************************************\
// *                 Expression Evaluation                  *
// **********************************************************/
public class ExpressionParser extends JEP implements Cloneable
{
    // Eye candy formatting of numbers
    private static boolean formatNumbers = false;
    
    private DataModel       dataModel;        // The DataModel of the input variable vector
    private DataModelDouble dmdo;
    private DataModelObject dmob;
    private Map             funcTrans;        // The External Transformations. (map of "functionName" -> transformation) 
    private Map             virtFunc;         // map of (functionName -> VirtualFieldFunction implementation)
    
    // The Current Data Vector
    private Object           vecnow;
    private String           expst;            // The Last Expression that was Parsed
    private String         []varnames;         // The DataModel Attributes that are used as variables. And their indices.
    private int            []varind;
    
    // Various very handy extra functions that are added to JEPs default set of functions.
    private static boolean            functions_there;
    private static PostfixMathCommand func_setunion;
    private static PostfixMathCommand func_setintersection;
    private static PostfixMathCommand func_setdifference;
    private static PostfixMathCommand func_setcontains;
    private static PostfixMathCommand func_setempty;
    private static PostfixMathCommand func_setmake;
    private static PostfixMathCommand func_setlimit;
    
    private static PostfixMathCommand func_mapsize;
    private static PostfixMathCommand func_mapput;
    private static PostfixMathCommand func_mapget;
    
    private static PostfixMathCommand func_ifthenelse;
    
    private static PostfixMathCommand func_timenow;
    private static PostfixMathCommand func_timeduration;
    private static PostfixMathCommand func_timemakedate;
    private static PostfixMathCommand func_timemakedouble;
    private static PostfixMathCommand func_timeparsedate;
    private static PostfixMathCommand func_timepreviousday;
    private static PostfixMathCommand func_formatdate;
    
    private static PostfixMathCommand func_tostring;
    private static PostfixMathCommand func_stringcontains;
    private static PostfixMathCommand func_string_indexof;
    private static PostfixMathCommand func_string_substring;
    private static PostfixMathCommand func_string_substringfrom;
    private static PostfixMathCommand func_string_equals;
    
    // The Smart Plus Function that can handle mixed Double/String expressions.
    private static PostfixMathCommand func_smartplus;
    
    // Function returning null
    private static PostfixMathCommand func_null;
    
    // Virtual Field function. Need to be non-static because access to current profile is needed.
    private PostfixMathCommand func_virtualfield;
    
    // **********************************************************\
    // *               Virtual Field Functions                  *
    // **********************************************************/
    public void addVirtualFieldFunction(String name, VirtualFieldFunction func) throws ShamanException
    {
        Object already = this.virtFunc.put(name, func);
        
        if (already != null)
            throw new LearnerException("Cannot add VirtualFieldFunction '"+name+"'. Already present in the parser.");
    }
    
    public void removeVirtualFieldFunction(String name) throws ShamanException
    {
        Object already = this.virtFunc.remove(name);
        
        if (already == null)
            throw new LearnerException("Cannot find VirtualFieldFunction '"+name+"'.");
    }
    
    public VirtualFieldFunction getVirtualFieldFunction(String funcname) throws ShamanException
    {
        VirtualFieldFunction func;
        
        func = (VirtualFieldFunction)this.virtFunc.get(funcname);
        
        if (func == null)
            throw new LearnerException("Cannot find VirtualFieldFunction with name '"+funcname+"'");
        
        return(func);
    }
    
    public void setVirtualFieldFunctionMap(Map virtFunc)
    {
        this.virtFunc = virtFunc;
    }
    
    public Map getVirtualFieldFunctionMap()
    {
        return(this.virtFunc);
    }
   
    // **********************************************************\
    // *   Replace all '+' operations with untyped ones         *
    // **********************************************************/
    private void replacePlus(Node root)
    {
        int i;
        
        // Recursively replace all '+' functions with their untypes variant.     
        if (root instanceof ASTFunNode)
        {
            ASTFunNode funnode;       
            
            funnode = (ASTFunNode)root;
            if (funnode.getName().equals("+"))
            { 
                funnode.setFunction("+", ExpressionParser.func_smartplus);
            }
        }
        
        // Recurse down tree.
        for (i=0; i<root.jjtGetNumChildren(); i++) replacePlus(root.jjtGetChild(i));
    }
   
    public FunctionTable getFunctionTable() { return(this.funTab); }
    
    // **********************************************************\
    // *      Clone the Parser and all it's Settings            *
    // **********************************************************/
    public Object clone() throws CloneNotSupportedException
    {
        ExpressionParser parseout;
        
        // Make a new parser. Copy datamodel and current expression
        parseout           = new ExpressionParser();
        parseout.dataModel = this.dataModel;
        parseout.vecnow    = this.vecnow;
        parseout.expst     = this.expst;
        
        // Clone the Function- and Symbol-Tables of the Template. Copy JEP settings.
        parseout.funTab          = (FunctionTable)this.funTab.clone();
        parseout.symTab          = (SymbolTable)this.symTab.clone();
        parseout.allowUndeclared = this.allowUndeclared;
        parseout.implicitMul     = this.implicitMul;
        parseout.funcTrans       = (Map)((HashMap)this.funcTrans).clone();
        parseout.initLocalDefaults();
        
        Iterator itfunc;
        String   fname;
        Transformation trans;
        
        // Replace the Transformation links with new ones that refer to the cloned Parser
        itfunc = this.funcTrans.keySet().iterator();
        while(itfunc.hasNext())
        {
            fname = (String)itfunc.next();
            trans = (Transformation)this.funcTrans.get(fname);
            parseout.addFunction(fname, new ExpressionParserTransformationFunction(parseout, trans));
        }
        
        // Use the same map for Virtual Field Functions in clone
        parseout.setVirtualFieldFunctionMap(this.virtFunc);
        
        return(parseout);
    }

    // **********************************************************\
    // *    Parse the Expression. Check extra functions.        *
    // **********************************************************/
    public void parseExpression(String expression)  // Don't USE.
    {
        try
        {
            this.expst = expression;
            parseExp(expression);
        }
        catch(ConfigException ex) { errorList.add(ex.getMessage()); }
    }
    
    public void parseExp(String expression) throws ConfigException
    {
        // Remember the Parsed Expression
        this.expst = expression;
        
        // Use JEP to Parse the expression
        super.parseExpression(expression);
        if (hasError()) throw new ConfigException("'"+getErrorInfo().trim()+"' in "+expression);
        else
        {
            Set  fnames;    
            Node topnode;
            
            // First Replace all '+' functions with their untypes variation
            replacePlus(this.getTopNode());
            
            // Make a list of extra functions to validate.
            fnames = new HashSet();
            fnames.addAll(this.funcTrans.keySet());
            
            // Recurse down the Parse Tree on validate the special DataModel and Transformation functions
            topnode = super.getTopNode();
            checkSpecialFunctions(topnode, fnames);
            
            // Make a list with DataModel variables that occur in the expression.
            // Also look for ones that aren't there.
            Set  vnames;
            
            vnames = new HashSet();
            findVariables(topnode, vnames);
            if (this.dataModel != null)
            {
                String   varnow;
                int      i, varindnow;
                Iterator varit;
                HashMap  varmap;
                String   []varnames;
                int      []varind;
                String   notmodelvars = "";
                
                varit  = vnames.iterator();
                varmap = new HashMap();
                while(varit.hasNext())
                {
                    varnow    = (String)varit.next();
                    varindnow = this.dataModel.getAttributeIndex(varnow);
                    if (varindnow != -1) varmap.put(varnow, new Integer(varindnow));
                    else                 notmodelvars += " "+varnow;
                }
                if (!notmodelvars.equals(""))
                    throw new DataModelException("Unknown variable(s) "+notmodelvars+" used in expression...");
                
                varnames = new String[varmap.size()];
                varind   = new int[varmap.size()];
                varit    = varmap.keySet().iterator();
                i        = 0;
                while(varit.hasNext())
                {
                    varnames[i] = (String)varit.next();
                    varind[i]   = ((Integer)varmap.get(varnames[i])).intValue();
                    i++;
                }
                
                // Use this information while evaluating.
                this.varnames = varnames;
                this.varind   = varind;
            }
            else
            {
                this.varnames = new String[]{};
                this.varind   = new int[]{};
            }
        }
    }
    
    private void findVariables(Node root, Set vnames) throws DataModelException
    {
        int    i;
        String varname;
        
        // Remember the name of the node if it's a variable. Skip the standard 'true' and 'false' ones.
        if (root instanceof ASTVarNode)
        {
            ASTVarNode var = (ASTVarNode)root;
            varname = var.getName();
            if (!varname.equals("true") && !varname.equals("false")) vnames.add(varname);
        }
        
        // Recurse in tree.
        for (i=0; i<root.jjtGetNumChildren(); i++)
        {
            findVariables(root.jjtGetChild(i), vnames);
        }
    }
   
    private void checkSpecialFunctions(Node root, Set fnames) throws ConfigException
    {
        int        i;
        
        // If it's a "special" function check if it's parameters occur in the DataModel
        if (root instanceof ASTFunNode)
        {
            ASTFunNode       fun;
            ASTConstant      con;
            Object           conval;
            Node             funchild;
            String           fname;
            Transformation   tr;
            DataModel        dmcheck;
            
            fun   = (ASTFunNode)root;
            fname = fun.getName();
            if (fnames.contains(fname))   // Special Function
            {
                for (i=0; i<fun.jjtGetNumChildren(); i++)
                {
                    // If the parameter is a constant.
                    funchild = fun.jjtGetChild(i);
                    if (funchild instanceof ASTConstant)
                    {
                        // Check if it's a String.
                        con    = (ASTConstant)funchild;
                        conval = con.getValue();
                        if (conval instanceof String)
                        {
                            // If this is a Transformation reference
                            tr      = (Transformation)funcTrans.get(fname);
                            if (tr != null)
                            {
                                // Check if the Attribute name is found in the DataModel
                                dmcheck = tr.getOutputDataModel(0);
                                if (dmcheck != null)
                                {
                                    if (dmcheck.getAttributeIndex((String)conval) == -1)
                                        throw new DataModelException("Can't find attribute '"+conval+"' for function '"+fun.getName()+"'.");
                                }
                                else throw new DataModelException("Illegal function "+fun.getName()+". Define DataModel first.");
                            }
                        }
                        else throw new DataModelException("Illegal argument type in function '"+fun.getName()+"'. Use an attribute name.");
                    }
                    else ; // Can't verify...
                }
            }
        }
        
        // Recurse.
        for (i=0; i<root.jjtGetNumChildren(); i++)
        {
            checkSpecialFunctions(root.jjtGetChild(i), fnames);
        }
    }

    // **********************************************************\
    // *      Evaluate the Expression using the JEP Parser      *
    // **********************************************************/
    public double getValue(DoubleMatrix1D invec) throws LearnerException
    {
        int           i;
        double        value;
        
        // Set the current input vector so the ExpressionParserFunctions can access with getDataVector()
        this.vecnow = invec;
        
        // Fill in the variables with the current values
        if (this.dataModel != null)
        {
            for (i=0; i<this.varnames.length; i++) addVariable(this.varnames[i], invec.getQuick(this.varind[i]));
        }
        
        // Parse with JEP superclass.
        value = super.getValue();
        
        // Check for errors.
        if (hasError())
        {
            String errinfo;
            
            // Get the error info
            errinfo = getErrorInfo();
            
            // **CLEAR** the errors. Else, the following evaluations will also throw the error.
            this.errorList.clear();
            
            // Throw an exception if there really was an error...
            if (errinfo != null) throw new LearnerException(errinfo+" in '"+this.expst+"'");
        } 
        
        return(value);
    }
    
    public Object getValueAsObject(ObjectMatrix1D invec) throws LearnerException
    {
        int           i;
        Object        value;
        
        value = null;
        
        // Set the current input vector so the ExpressionParserFunctions can access with getDataVector()
        this.vecnow = invec;
        
        try
        {
            // Fill the input vector variables with their current values
            if (this.dmob != null)
            {
                // Only add the variables that are used in the expression to the Symbol Table.
                for (i=0; i<this.varnames.length; i++)
                {  
                    Object valnow = invec.getQuick(varind[i]);
                    
                    if (valnow == null)
                    {
                        // Make sure a 'null' never goes to the JEP logic. Replace by default object.
                        AttributeObject atnull = this.dmob.getAttributeObject(varind[i]);
                        super.addVariable(varnames[i], atnull.getDefaultObject());
                    } 
                    else super.addVariable(varnames[i], invec.getQuick(varind[i]));
                }
            } 
            
            // Evaluate with the JEP superclass
            value = super.getValueAsObject();
            
            // Check for errors.
            if (hasError())
            {
                String errinfo;
                
                // Get the error info
                errinfo = getErrorInfo();
                
                // **CLEAR** the errors. Else, the following evaluations will also throw the error.
                this.errorList.clear();
                
                // Throw an exception if there really was an error...
                if (errinfo != null) throw new LearnerException(errinfo+" in '"+this.expst+"'");
            } 
        }
        catch(DataModelException ex) { throw new LearnerException(ex); }
        
        return(value);
    }
    
    public boolean getValueAsBoolean(DoubleMatrix1D invec) throws LearnerException
    {
        double val;
        
        val = getValue(invec);
        if (val == 1.0) return(true);
        else            return(false);
    }
    
    public boolean getValueAsBoolean(ObjectMatrix1D invec) throws LearnerException
    {
        Object val;
        Number nval;
        
        val = getValueAsObject(invec);
        if (val instanceof Number)
        {
            nval = (Number)val;
            if (nval.intValue() == 1) return(true);
            else                      return(false);
        }
        else throw new LearnerException("Expression does not evaluate to a Number. Cannot derive boolean value.");
    }   
    
    public Object    getDataVector() { return(this.vecnow); }
    public DataModel getDataModel()  { return(this.dataModel); }

    // **********************************************************\
    // *  Check if the given DataModel can be used in functions *
    // **********************************************************/
    public void checkDataModelFit(DataModel dmin) throws ConfigException
    {
        Iterator       trit;
        Transformation trnow;
        
        // For all registered external Transformations (functions).
        trit = this.funcTrans.values().iterator();
        while (trit.hasNext())
        {
            // Check if it's possible to call the Transformation using the DataModel.
            trnow = (Transformation)trit.next();
            trnow.checkDataModelFit(0, dmin);
        }
    }
    
    // **********************************************************\
    // *   Add/Remove External Transformation as JEP Function   *
    // **********************************************************/
    public void addTransformationFunction(String name, Transformation trans) throws ConfigException
    {
        // Is the name already occupied?
        if (!this.funcTrans.containsKey(name))
        {
            if (!this.funTab.containsKey(name))
            {
                // Add the new Function to this Parser
                addFunction(name, new ExpressionParserTransformationFunction(this, trans));
                
                // Remember name -> transformation mapping
                this.funcTrans.put(name, trans);
            }
            else throw new ConfigException("Cannot add Function. Function name '"+name+"' already used by a JEP function.");
        }
        else throw new ConfigException("Cannot add Function. Function name '"+name+"' is already mapped to Transformation '"+((Transformation)funcTrans.get(name)).getName()+"'");
    }
    
    public void removeTransformationFunction(String name) throws ConfigException
    {
        // If it's there. Remove it.
        if (this.funcTrans.containsKey(name))
        {
            removeFunction(name);
        }
        else throw new ConfigException("Cannot find Transformation Function with name "+name);
    }

    // **********************************************************\
    // *        Set Up the JEP Parser with DataModel            *
    // **********************************************************/
    public static void setFormatNumbers(boolean formatNumbers)
    {
        ExpressionParser.formatNumbers = formatNumbers;
    }
    
    public static boolean getFormatNumbers()
    {
        return(ExpressionParser.formatNumbers);
    }
    
    /**
     * Define the DataModel of the incomming data vectors.
     * It's attributes are installed as variables in the parser.
     * @param dmin The datamodel of the incomming vectors.
     */
    public void setDataModel(DataModel dmin) throws ConfigException
    {
        int       j;
        Attribute []atts;
        
        // Add the input attributes as variables
        atts           = dmin.getAttributes();
        this.dataModel = dmin;
        if (dmin.getVectorTypeProperty().equals(DataModelPropertyVectorType.doubleVector))
        {
            this.dmdo = (DataModelDouble)dmin;
            for (j=0; j<atts.length; j++) addVariable(atts[j].getName(), 0);
        }
        else
        {
            this.dmob = (DataModelObject)dmin;
            for (j=0; j<atts.length; j++) addVariable(atts[j].getName(), "");
        }
        
        if ((expst != null) && (expst.equals(""))) parseExp(expst);
    }
    
    /**
     * Add the standard JEP functions and Constants.
     * Add default Order and Distance functions that use the DataModel's structure.
     */
    private void initializeDefaults()
    {
        addStandardConstants();
        addVariable("true", 1.0);
        addVariable("false", 0.0);
        
        // Standard Mathematical Functions.
        addStandardFunctions();
        
        // Make the shared utility functions if they're not there yet
        if (!functions_there)
        {
            func_setunion             = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_UNION);
            func_setintersection      = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_INTERSECTION);
            func_setdifference        = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_DIFFERENCE);
            func_setcontains          = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_CONTAINS);
            func_setempty             = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_EMPTY);
            func_setmake              = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_MAKE);
            func_setlimit             = new ExpressionParserSetFunction(ExpressionParserSetFunction.TYPE_LIMIT);
            
            func_mapsize              = new ExpressionParserMapFunction(ExpressionParserMapFunction.TYPE_SIZE);
            func_mapget               = new ExpressionParserMapFunction(ExpressionParserMapFunction.TYPE_GET);
            func_mapput               = new ExpressionParserMapFunction(ExpressionParserMapFunction.TYPE_PUT);
            
            func_ifthenelse           = new ExpressionParserCondExpFunction();
            
            func_timenow              = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_NOW);
            func_timeduration         = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_DURATION);
            func_timemakedate         = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_MAKE_DATE);
            func_timemakedouble       = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_MAKE_DOUBLE);
            func_timeparsedate        = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_PARSE_DATE);
            func_timepreviousday      = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_PREVIOUS_DAY);
            func_formatdate           = new ExpressionParserTimeFunction(ExpressionParserTimeFunction.TYPE_FORMAT_DATE);
            
            func_tostring             = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_TOSTRING);
            func_stringcontains       = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_CONTAINS);
            func_string_indexof       = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_INDEXOF);
            func_string_substring     = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_SUBSTRING);
            func_string_substringfrom = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_SUBSTRINGFROM);
            func_string_equals        = new ExpressionParserStringFunction(ExpressionParserStringFunction.TYPE_EQUALS);
            
            func_smartplus            = new ExpressionParserStringPlusFunction();
            
            func_null                 = new ExpressionParserNullFunction();
            
            functions_there = true;
        }
        
        // Set Operation Functions
        addFunction("setunion",         func_setunion);
        addFunction("setintersection",  func_setintersection);
        addFunction("setdifference",    func_setdifference);
        addFunction("setcontains",      func_setcontains);
        addFunction("setempty",         func_setempty);
        addFunction("setmake",          func_setmake);
        addFunction("setlimit",         func_setlimit);
        
        // Map operation Functions
        addFunction("mapsize",          func_mapsize);
        addFunction("mapput",           func_mapput);
        addFunction("mapget",           func_mapget);
        
        // Conditional Expression Function
        addFunction("ifthenelse",       func_ifthenelse);
        
        // Time Handling Function
        addFunction("timenow",          func_timenow);
        addFunction("timeduration",     func_timeduration);
        addFunction("timepreviousday",  func_timepreviousday);
        addFunction("timemakedate",     func_timemakedate);
        addFunction("timemakedouble",   func_timemakedouble);
        addFunction("timeparsedate",    func_timeparsedate);
        addFunction("formatdate",       func_formatdate);
        
        // String Handling Functions
        addFunction("tostring",         func_tostring);
        addFunction("stringcontains",   func_stringcontains);
        addFunction("indexof",          func_string_indexof);
        addFunction("substring",        func_string_substring);
        addFunction("substringfrom",    func_string_substringfrom);
        addFunction("stringequals",     func_string_equals);
        
        // null function
        addFunction("null",             func_null);
        
        // External function and old parameter system
        funcTrans = new HashMap();
        virtFunc  = new HashMap();
        
        // Initialize Local Defaults
        initLocalDefaults();
    }
    
    private void initLocalDefaults()
    {
        // Make the virtual field function
        this.func_virtualfield = new ExpressionParserVirtualFieldFunction(this);
        removeFunction("virtualfield");
        addFunction("virtualfield", this.func_virtualfield);
    }
    
    
    /**
     * Make a new expession parser.
     */
    public ExpressionParser()
    {
        super();
        
        initializeDefaults();
        this.expst = null;
        //this.ev    = new FastEvaluatorVisitor();
    }
    
    /**
     * Make a new expression parser with as variables the datamodel's attributes.
     * @param _dataModel The datamodel of the input vectors. It's attributes are variables for the parser.
     */
    public ExpressionParser(DataModel dataModel) throws ConfigException
    {
        super();
        initializeDefaults();
        
        this.expst = null;
        //this.ev    = new FastEvaluatorVisitor();
        setDataModel(dataModel);
    }

    /**
     * Set the output of an Expression evaluation in a Vector, following the DataModel concepts.
     * @param dataModel The DataModel of the vector
     * @param destind Index of the destination attribute
     * @param value The value to put into the vector
     * @param oout The vector
     */
    public static void setObjectDestination(DataModelObject dataModel, int destind, Object value, ObjectMatrix1D oout) throws ShamanException
    {
        AttributeObject atob;
        String          attype;
        String          obtype;
        
        // Determine the java type of the Object and the Attribute
        atob   = dataModel.getAttributeObject(destind);
        attype = atob.getRawType();
        
        // Replace a missing value with it's unique value as an Object.
        if (atob.isMissingValue(value)) value = atob.getMissingAsObject();
        
        // Determine the type of the value
        if (value != null) obtype = value.getClass().getName();
        else               obtype = attype;
        
        if (obtype.equals(attype)) oout.setQuick(destind, value); // No Problemo. Perfect Type Match.
        else
        {
            try
            {
                // Handle some well known type mismatch cases...
                if      (attype.equals("java.lang.String"))
                {
                    // If Attribute is a String. Convert Object with toString()
                    oout.setQuick(destind, value.toString());
                }
                else if (attype.equals("java.util.Date"))
                {
                    // If Attribute is a Date. Try to convert the value to a Date.
                    if      (obtype.equals("java.lang.Double"))
                    {
                        // For Doubles : get the long value and make a Date
                        Date datval = new Date(((Double)value).longValue());
                        oout.setQuick(destind, datval);
                    }
                    else if (obtype.equals("java.lang.String"))
                    {
                        // For String : Try Parsing with the default SimpleDataFormat
                        try
                        {
                            SimpleDateFormat dsf = new SimpleDateFormat();
                            Date          datval = dsf.parse((String)value);
                            oout.setQuick(destind, datval);
                        }
                        catch(java.text.ParseException ex)
                        {
                            throw new LearnerException("Can't Parse Expression value String as Date with the default SimpleDateFormat parser.");
                        }
                    }
                    else if (java.util.Date.class.isAssignableFrom(Class.forName(obtype)))
                    {
                        // This is a supertype of Date. All fine.
                        oout.setQuick(destind, value);
                    }
                    else throw new LearnerException("Can't convert Expression value with type "+obtype+" to Attribute '"+dataModel.getAttributeName(destind)+"' with type java.util.Date");
                }
                else if (Class.forName(attype).isAssignableFrom(Class.forName(obtype)))
                {
                    // The Expression Value is a supertype of the object type. All fine.
                    oout.setQuick(destind, value);
                }
                else throw new LearnerException("Can't assign the Expression value with type "+obtype+" to Attribute '"+dataModel.getAttributeName(destind)+"' with type "+attype);
            }
            catch(ClassNotFoundException ex) { throw new LearnerException(ex); }
        }
    }
}


// **********************************************************\
// *        Virtual Field Function Call Function            *
// **********************************************************/
class ExpressionParserVirtualFieldFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    private ExpressionParser parser;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        // Check and get arguments from stack
        checkStack(stack);
        
        Object ofieldname, ofuncname;
        String  fieldname,  funcname;
        ofieldname = stack.pop();
        ofuncname  = stack.pop();
        if (!(ofieldname instanceof String) || !(ofuncname instanceof String))
            throw new org.nfunk.jep.ParseException("Arguments have wrong type. Should both be String but are "+ofieldname.getClass().getName()+" and "+ofuncname.getClass().getName());
        fieldname = (String)ofieldname;
        funcname  = (String)ofuncname;
        
        // Get DataModel and Current Vector from parser
        DataModelObject dmprof;
        ObjectMatrix1D  prof;
        
        prof   = (ObjectMatrix1D)this.parser.getDataVector();
        dmprof = (DataModelObject)this.parser.getDataModel();
        
        try
        {
            // Find and call the VirtualFieldFunction
            VirtualFieldFunction func;
            Object               out;
            
            func = this.parser.getVirtualFieldFunction(funcname);
            out  = func.calculate(prof, dmprof, fieldname);
            
            // Output the function's value
            stack.push(out);
        }
        catch(ShamanException ex)
        {
            throw new org.nfunk.jep.ParseException(ex.getMessage());
        }
    }
    
    public ExpressionParserVirtualFieldFunction(ExpressionParser parser)
    {
        this.parser             = parser;
        this.numberOfParameters = 2;
    }
}

// **********************************************************\
// *             String Processing Functions                *
// **********************************************************/
class ExpressionParserStringFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public static final int TYPE_TOSTRING      = 0;
    public static final int TYPE_CONTAINS      = 1;
    public static final int TYPE_INDEXOF       = 2;
    public static final int TYPE_SUBSTRINGFROM = 3;
    public static final int TYPE_SUBSTRING     = 4;
    public static final int TYPE_EQUALS     = 5;
    
    private int type;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        checkStack(stack);
        
        if      (type == TYPE_TOSTRING)         // Convert Argument to String
        {
            Object arg = stack.pop();
            String argstring = arg.toString();
            stack.push(argstring);
        }
        else if (type == TYPE_CONTAINS)         // s1.indexOf(s2) != -1
        {
            Object o1, o2;
            String s1, s2;
            int    contains;
            
            o2 = stack.pop();
            o1 = stack.pop();
            
            if (!(o2 instanceof String)) throw new org.nfunk.jep.ParseException("Second argument of contains() is not a String but a "+o2.getClass().getName());
            if (!(o1 instanceof String)) throw new org.nfunk.jep.ParseException("First argument of contains() is not a String but a "+o1.getClass().getName());
            
            s1 = (String)o1;
            s2 = (String)o2;
            contains = s1.indexOf(s2);
            
            if (contains == -1) stack.push(new Double(0.0));
            else                stack.push(new Double(1.0));
        }
        else if (type == TYPE_INDEXOF)
        {
            Object  o1, o2;
            String  s1, s2;
            int     idxof;
            
            o2 = stack.pop();
            o1 = stack.pop();
            
            if (!(o2 instanceof String)) throw new org.nfunk.jep.ParseException("Second argument of indexof() is not a String but a "+o2.getClass().getName());
            if (!(o1 instanceof String)) throw new org.nfunk.jep.ParseException("First argument of indexof() is not a String but a "+o1.getClass().getName());
            
            s1 = (String)o1;
            s2 = (String)o2;
            
            idxof = s1.indexOf(s2);
            
            stack.push(new Double(idxof));
        }
        else if (type == TYPE_SUBSTRING)
        {
            Object o1, o2, o3;
            String s1;
            int    indbeg, indend;
            String subst;
            
            o3 = stack.pop(); o2 = stack.pop(); o1 = stack.pop();
            if (!(o3 instanceof Double)) throw new org.nfunk.jep.ParseException("Third (End Index) argument of substring() is not a Double but a "+o3.getClass().getName());
            if (!(o2 instanceof Double)) throw new org.nfunk.jep.ParseException("Second (Begin Index) argument of substring() is not a Double but a "+o2.getClass().getName());
            if (!(o1 instanceof String)) throw new org.nfunk.jep.ParseException("First (String) argument of substring() is not a String but a "+o1.getClass().getName());
            
            s1     = (String)o1;
            indbeg = ((Double)o2).intValue();
            indend = ((Double)o3).intValue();
            
            // Limit to legal values and execute the subString method in the given String.
            if (indbeg < 0)           indbeg = 0;
            if (indend > s1.length()) indend = s1.length();
            subst = s1.substring(indbeg, indend);
            
            stack.push(subst);
        }
        else if (type == TYPE_SUBSTRINGFROM)
        {
            Object o1, o2;
            String s1;
            int    indbeg;
            String subst;
            
            o2 = stack.pop(); o1 = stack.pop();
            if (!(o2 instanceof Double)) throw new org.nfunk.jep.ParseException("Second (Begin Index) argument of substringfrom() is not a Double but a "+o2.getClass().getName());
            if (!(o1 instanceof String)) throw new org.nfunk.jep.ParseException("First (String) argument of substringfrom() is not a String but a "+o1.getClass().getName());
            
            // Limit to legal values.
            s1     = (String)o1;
            indbeg = ((Double)o2).intValue();
            if (indbeg < 0) indbeg = 0;
            
            subst = s1.substring(indbeg);
            
            stack.push(subst);
        }
        else if (type == TYPE_EQUALS)
        {
            Object o3 = stack.pop();
            Object o2 = stack.pop();
            Object o1 = stack.pop();
            if (!(o1 instanceof String)) throw new ParseException("First (String) argument of stringequals() is not a String but a " + o1.getClass().getName());
            if (!(o2 instanceof String)) throw new ParseException("Second (String) argument of stringequals() is not a String but a " + o2.getClass().getName());
            if (!(o3 instanceof Double)) throw new ParseException("Third (Double) argument of stringequals() is not a Double but a " + o3.getClass().getName());
            
            String s1 = (String) o1;
            String s2 = (String) o2;
            boolean ignoreCase = ((Double) o3).doubleValue() == 1;
            
            if (ignoreCase)
                stack.push(s1.equalsIgnoreCase(s2) ? new Double(1.0) : new Double(0.0));
            else
                stack.push(s1.equals(s2) ? new Double(1.0) : new Double(0.0));
        }
    }
    
    public ExpressionParserStringFunction(int type)
    {
        this.type = type;
        
        if      (type == TYPE_TOSTRING)      numberOfParameters = 1;
        else if (type == TYPE_CONTAINS)      numberOfParameters = 2;
        else if (type == TYPE_INDEXOF)       numberOfParameters = 2;
        else if (type == TYPE_SUBSTRINGFROM) numberOfParameters = 2;
        else if (type == TYPE_SUBSTRING)     numberOfParameters = 3;
        else if (type == TYPE_EQUALS)        numberOfParameters = 3;
    }
}

class ExpressionParserStringPlusFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    private DecimalFormat decform;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        checkStack(stack);
        Object o1, o2;
        
        o2 = stack.pop();
        o1 = stack.pop();
        
        Double d1, d2, dout;
        Object out;
        
        // Default object
        out = "";
        
        // If one or both are String. Return a String.
        if      ((o1 instanceof String) || (o2 instanceof String))
        {
            // Apply gentle formatting rules to numbers?
            if (ExpressionParser.getFormatNumbers())
            {
                // Format numbers so they make sense for 'a certain application' we can not name.
                if (o1 instanceof Double) o1 = formatNumber(((Double)o1).doubleValue());
                if (o2 instanceof Double) o2 = formatNumber(((Double)o2).doubleValue());
            }
            // Make sure the newline escape sequence really does generate a newline.
            if (o2.equals("\\n")) out = o1.toString()+"\n";
            else                  out = o1.toString()+o2.toString();
        }
        else if ((o1 instanceof Double) && (o2 instanceof Double))
        {
            // If both Doubles. Return the sum as Double.
            d1   = (Double)o1;
            d2   = (Double)o2;
            dout = new Double(d1.doubleValue()+d2.doubleValue());
            out  = dout;
        }
        
        stack.push(out);
    }
    
    private String formatNumber(double d)
    {
        String dfor;
        
        // Convert the double to a well formatted string
        // - rounded to 2 decimal digits
        // - never using scientific formatting (no En)
        // - no showing .0 for integers
        if (this.decform == null) this.decform = new DecimalFormat("#.##;-#.##");
        dfor = decform.format(d);
        
        return(dfor);
    }
    
    public ExpressionParserStringPlusFunction()
    {
        numberOfParameters = 2;
    }
}

// **********************************************************\
// *                Time Related Functions                  *
// **********************************************************/
class ExpressionParserTimeFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public static final int TYPE_NOW          = 0;
    public static final int TYPE_MAKE_DATE    = 1;
    public static final int TYPE_MAKE_DOUBLE  = 2;
    public static final int TYPE_DURATION     = 3;
    public static final int TYPE_PARSE_DATE   = 4;
    public static final int TYPE_PREVIOUS_DAY = 5;
    public static final int TYPE_FORMAT_DATE  = 6;
    
    private int type;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        checkStack(stack);
        
        if (type == TYPE_NOW)
        {
            long    now = System.currentTimeMillis();
            Double dnow = new Double(now);
            stack.push(dnow);
        }
        else if (type == TYPE_DURATION)
        {
            Object oper;
            String period;
            Double perdur;
            
            oper = stack.pop();
            if (oper instanceof String)
            {
                period = (String)oper;   
                if      (period.equalsIgnoreCase("second")) perdur = new Double(DistanceDate.SECOND);
                else if (period.equalsIgnoreCase("minute")) perdur = new Double(DistanceDate.MINUTE);
                else if (period.equalsIgnoreCase("hour"))   perdur = new Double(DistanceDate.HOUR);
                else if (period.equalsIgnoreCase("day"))    perdur = new Double(DistanceDate.DAY);
                else if (period.equalsIgnoreCase("week"))   perdur = new Double(DistanceDate.WEEK);
                else if (period.equalsIgnoreCase("month"))  perdur = new Double(DistanceDate.MONTH);
                else if (period.equalsIgnoreCase("year"))   perdur = new Double(DistanceDate.YEAR);
                else throw new org.nfunk.jep.ParseException("Cannot handle duration argument '"+period+"'");
                
                stack.push(perdur);
            }
            else throw new org.nfunk.jep.ParseException("Cannot handle duration argument of type "+oper.getClass().getName()+". Should be a String");
        }
        else if (type == TYPE_MAKE_DATE)
        {
            Object otime;
            
            otime = stack.pop();
            if (otime instanceof Double)
            {
                Double dtime    = (Double)otime;
                long   ltime    = dtime.longValue();
                Date   datetime = new java.util.Date(ltime);
                
                stack.push(datetime);
            }
            else throw new org.nfunk.jep.ParseException("Cannot handle time argument of type "+otime.getClass().getName()+". Should be a Double"); 
        }
        else if (type == TYPE_MAKE_DOUBLE)
        {
            Object otime;
            
            otime = stack.pop();
            if (otime instanceof Date)
            {
                Date   dtime  = (Date)otime;
                long   ltime  = dtime.getTime();
                Double dotime = new Double(ltime);
                
                stack.push(dotime);
            }
            else throw new org.nfunk.jep.ParseException("Cannot handle time argument of type "+otime.getClass().getName()+". Should be a Double"); 
        }
        else if (type == TYPE_PARSE_DATE)
        {
            Object odate;
            Object oformat;
            String sdate;
            String sformat;
            Date   parseddate;
            Double datetime;
            
            oformat = stack.pop();
            odate   = stack.pop();
            if ((oformat instanceof String) && (odate instanceof String))
            {
                sdate   = (String)odate;
                sformat = (String)oformat;
                try
                {
                    // Parse the given date with the given date-format
                    parseddate = DateUtil.parseDate(sdate, sformat);
                    datetime   = new Double(parseddate.getTime());
                    stack.push(datetime);
                }
                catch(java.text.ParseException ex)
                {
                    throw new org.nfunk.jep.ParseException("Cannot parse date string "+sdate+" with date-format "+sformat+".");
                }
            }
            else throw new org.nfunk.jep.ParseException("Date- or date-format-String have wrong type. Should both be Strings.");
        }
        else if (type == TYPE_FORMAT_DATE)
        {
            Object oformat = stack.pop();
            Object odate = stack.pop();
            if ((oformat instanceof String) && (odate instanceof Date))
            {
                Date sdate = (Date) odate;
                String sformat = (String) oformat;
                // Parse the given date with the given date-format
                String formattedDate = DateUtil.formatDate(sdate, sformat);
                stack.push(formattedDate);
            }
            else throw new org.nfunk.jep.ParseException("Date- or date-format-String have wrong type. Should both be Strings.");
        }
        else if (type == TYPE_PREVIOUS_DAY)
        {
            Object odate;
            odate = stack.pop();
            if ((odate instanceof Date) || (odate instanceof Double))
            {
                Date date, prevday;
                
                // Make or get a Date from the input
                if (odate instanceof Double) date = new Date(((Double)odate).longValue());
                else                         date = (Date)odate;
                
                // Use the Calendar to reach the previous day
                prevday = DateUtil.getPreviousDay(date);
                
                // Return output in the same type as the input
                if (odate instanceof Double) stack.push(new Double(prevday.getTime()));
                else                         stack.push(prevday);
            }
            else throw new org.nfunk.jep.ParseException("Cannot handle time argument of type "+odate.getClass().getName()+". Should be a Date or Double"); 
        }
    }
    
    public ExpressionParserTimeFunction(int type)
    {
        this.type = type;
        
        if      (type == TYPE_NOW)          numberOfParameters = 0;
        else if (type == TYPE_MAKE_DATE)    numberOfParameters = 1;
        else if (type == TYPE_MAKE_DOUBLE)  numberOfParameters = 1;
        else if (type == TYPE_DURATION)     numberOfParameters = 1;
        else if (type == TYPE_PARSE_DATE)   numberOfParameters = 2;
        else if (type == TYPE_PREVIOUS_DAY) numberOfParameters = 1;
        else if (type == TYPE_FORMAT_DATE)  numberOfParameters = 2;
    }
}

// **********************************************************\
// *             Conditional Expression Function            *
// **********************************************************/
class ExpressionParserCondExpFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        Object othen, oelse;
        Object ocond;
        
        // Check input Stack
        checkStack(stack);
        
        // Pop Else, Then, If (cond) from Stack.
        oelse = stack.pop();
        othen = stack.pop();
        ocond = stack.pop();
        
        if (ocond instanceof Number)
        {       
            boolean cond;
            
            if (((Number)ocond).intValue() == 1) cond = true;
            else                                 cond = false;
            
            if (cond) stack.push(othen);
            else      stack.push(oelse);
        }
        else throw new org.nfunk.jep.ParseException("Not a numeric value as Condition. Type is "+ocond.getClass().getName()+" Value : "+ocond);
        
    }
    
    public ExpressionParserCondExpFunction()
    {
        numberOfParameters = 3;
    }
}

// **********************************************************\
// *                 JEP Collection Functions               *
// **********************************************************/
class ExpressionParserMapFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public static final int TYPE_SIZE = 0;
    public static final int TYPE_GET  = 1;
    public static final int TYPE_PUT  = 2;
    
    private int type;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        Object oin, opar, opar2;
        Map    min;
        
        if (this.type == TYPE_SIZE)
        {
            oin = stack.pop();
            if (oin instanceof Map)
            {
                min = (Map)oin;
                stack.push(new Double(min.size()));
            }
            else throw new org.nfunk.jep.ParseException("Argument is not a Map");
        }
        else if (this.type == TYPE_GET)
        {
            opar = stack.pop();
            oin  = stack.pop();
            if (oin instanceof Map)
            {
                Object oout;
                
                min  = (Map)oin;
                oout = min.get(opar);
                if (oout == null) oout = new Double(0);
                stack.push(oout);
            }
            else throw new org.nfunk.jep.ParseException("First argument is not a Map");
        }
        else if (this.type == TYPE_PUT)
        {
            opar2 = stack.pop();
            opar  = stack.pop();
            oin   = stack.pop();
            
            if (oin instanceof Map)
            {
                min = (Map)oin;
                min.put(opar, opar2);
                
                stack.push(new Double(1.0));
            }
            else throw new org.nfunk.jep.ParseException("First argument is not a Map");
        }
    }
    
    public ExpressionParserMapFunction(int type)
    {
        this.type = type;
        if      (this.type == TYPE_SIZE) numberOfParameters = 1;
        else if (this.type == TYPE_GET)  numberOfParameters = 2;
        else if (this.type == TYPE_PUT)  numberOfParameters = 3;
    }
}

class ExpressionParserSetFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public static final int TYPE_UNION        = 0;
    public static final int TYPE_INTERSECTION = 1;
    public static final int TYPE_DIFFERENCE   = 2;
    public static final int TYPE_CONTAINS     = 3;
    public static final int TYPE_EMPTY        = 10;
    public static final int TYPE_MAKE         = 20;
    public static final int TYPE_LIMIT        = 100;
    
    private int type;
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        Object ob1, ob2;
        Object obout;
        
        Set    s1, s2;
        Set    sout;
        
        // Check input Stack
        checkStack(stack);
        
        // Perform the correct Set Theory operation
        sout = null; obout = null;
        if (this.type == TYPE_UNION)  // UNION of 2 Sets. Or 1 Set and an Object.
        { 
            ob2 = stack.pop();
            ob1 = stack.pop();
            if      ((ob1 instanceof Set) && (ob2 instanceof Set))
            {
                s1 = new HashSet((Set)ob1);
                s2 = new HashSet((Set)ob2);
                s1.addAll(s2);
                sout = s1;
            }
            else if ((ob1 instanceof Set) && !(ob2 instanceof Set))
            {
                s1 = new HashSet((Set)ob1);
                s1.add(ob2);
                sout = s1;
            }
            else if (!(ob1 instanceof Set) && (ob2 instanceof Set))
            {
                s2 = new HashSet((Set)ob2);
                s2.add(ob1);
                sout = s2;
            }
            else
            {
                sout = new HashSet();
                sout.add(ob1);
                sout.add(ob2);
            }
        }
        else if (type == TYPE_INTERSECTION) // INTERSECTION of 2 Sets. 
        {
            ob2 = stack.pop();
            ob1 = stack.pop();
            if      ((ob1 instanceof Set) && (ob2 instanceof Set))
            {
                s1 = new HashSet((Set)ob1);
                s2 = new HashSet((Set)ob2);
                s1.retainAll(s2);
                sout = s1;
            }
            else throw new org.nfunk.jep.ParseException("One or both arguments are not Sets. Class names are : "+ob1.getClass().getName()+" and "+ob2.getClass().getName());
        }
        else if (type == TYPE_DIFFERENCE) // DIFFERENCE of 2 sets.  A / B
        {
            ob2 = stack.pop();
            ob1 = stack.pop();
            if      ((ob1 instanceof Set) && (ob2 instanceof Set))
            {
                s1 = new HashSet((Set)ob1);
                s2 = new HashSet((Set)ob2);
                s1.removeAll(s2);
                sout = s1;
            }
            else throw new org.nfunk.jep.ParseException("One or both arguments are not Sets. Class names are : "+ob1.getClass().getName()+" and "+ob2.getClass().getName());
        }
        else if (type == TYPE_CONTAINS) // Set Membership Functions. B in A
        {
            ob2 = stack.pop();
            ob1 = stack.pop();
            
            if      ((ob1 instanceof Set) && !(ob2 instanceof Set))
            {
                s1 = (Set)ob1;
                if (s1.contains(ob2)) obout = new Double(1.0);
                else                  obout = new Double(0.0);
            }
            else throw new org.nfunk.jep.ParseException("First argument is not a Set, or second argument is a Set. Class names are : "+ob1.getClass().getName()+" and "+ob2.getClass().getName());
            
        }
        else if (type == TYPE_EMPTY) // Make the Empty Set.
        {
            sout = new HashSet();
        }
        else if (type == TYPE_MAKE)  // Make a set containing the input element. If it's already one, just leave it.
        {
            ob1 = stack.pop();
            if (ob1 instanceof Set) obout = ob1;
            else
            {
                sout = new HashSet();
                sout.add(ob1);
            }
        }
        else if (type == TYPE_LIMIT)
        {
            ob1 = stack.pop();  // 'n'
            ob2 = stack.pop();  // Set
            
            // Only keep a maximum of 'n' elements in the given Set
            if      ((ob2 instanceof Set) && (ob1 instanceof Number))
            {
                int lim;
                
                
                s1   = (Set)ob2;
                lim  = ((Number)ob1).intValue();
                sout = s1;
                if (s1.size() > lim)
                {
                    Iterator itset;
                    int      i;
                    
                    s2    = new HashSet();
                    itset = s1.iterator();
                    for (i=0; i<lim; i++) s2.add(itset.next());
                    s1.clear();
                    s1.addAll(s2);
                }
            }
            else throw new org.nfunk.jep.ParseException("First Argument is not a Set or second argument is not a Number");
        }
        else throw new org.nfunk.jep.ParseException("Unknown SET operation function. ");
        
        // Push the output of the Set Operation on the stack.
        if (sout != null) obout = sout;
        stack.push(obout);
    }
    
    public ExpressionParserSetFunction(int type)
    {
        this.type = type;
        if (    (this.type == TYPE_UNION)        ||
                (this.type == TYPE_INTERSECTION) ||
                (this.type == TYPE_DIFFERENCE)   ||
                (this.type == TYPE_LIMIT)        ||
                (this.type == TYPE_CONTAINS) )      numberOfParameters = 2;
        else if (this.type == TYPE_EMPTY)           numberOfParameters = 0;
        else if (this.type == TYPE_MAKE)            numberOfParameters = 1;
    }
}

// **********************************************************\
// *        Attribute Order to JEP Function Bridge          *
// **********************************************************/
class ExpressionParserNullFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    public void run(Stack stack) throws ParseException
    {
        stack.push(null);
    }
    
    public ExpressionParserNullFunction()
    {
        this.numberOfParameters = 0;
    }
}

// **********************************************************\
// *        Transformation to JEP Function Bridge           *
// **********************************************************/
class ExpressionParserTransformationFunction extends org.nfunk.jep.function.PostfixMathCommand implements org.nfunk.jep.function.PostfixMathCommandI
{
    private Transformation   tr;       // The Transformation
    private ExpressionParser parser;   // The parser in which this function exists
    
    public void run(java.util.Stack stack) throws org.nfunk.jep.ParseException
    {
        DataModel        dmout;
        Object           arg;
        Object           vec;
        Object         []outall;
        DoubleMatrix1D   outdo;
        ObjectMatrix1D   outob;
        int              attind;
        double           dval;
        Object           oval;
        boolean          prim;
        AttributeDouble  attdo;
        AttributeObject  attob;
        
        // Check input Stack
        checkStack(stack);
        
        try
        {
            // Get the output datamodel of the transformation.
            dmout = tr.getOutputDataModel(0);
            if (dmout.getProperty(DataModel.PROPERTY_VECTORTYPE) == DataModelPropertyVectorType.doubleVector) prim = true;
            else                                                                                              prim = false;
            
            // Get the current data vector from the rule
            vec = parser.getDataVector();
            
            // Get and 'parse' the argument.
            arg = stack.pop();
            
            // Parse the single argument
            attind = -1;
            if (arg instanceof String)
            {
                // Name of the output attribute.
                attind = dmout.getAttributeIndex((String)arg);
            }
            else throw new org.nfunk.jep.ParseException("Illegal argument type "+arg.getClass().getName());
            
            // Couldn't find the desired output attribute.
            if (attind == -1) throw new org.nfunk.jep.ParseException("Illegal argument "+arg);
            
            // Call the Transformation in Request<->Response mode
            outall = tr.transform(vec);
            
            if (outall != null)   // If there was output. Get the first output vector.
            {
                // Get the wanted output attribute from the first output vector and return it to the parser.
                if (prim)
                {
                    outdo = (DoubleMatrix1D)outall[0];
                    dval  = outdo.getQuick(attind);
                    stack.push(new Double(dval));
                }
                else
                {
                    outob = (ObjectMatrix1D)outall[0];
                    oval  = outob.getQuick(attind);
                    stack.push(oval);
                }
            }
            else // No output.
            {
                // Output the missing value for the attribute
                if (prim)
                {
                    attdo = ((DataModelDouble)dmout).getAttributeDouble(attind);
                    stack.push(new Double(attdo.getMissingAsDouble()));
                }
                else
                {
                    attob = ((DataModelObject)dmout).getAttributeObject(attind);
                    stack.push(attob.getMissingAsObject());   // Make sure this is not 'null'!
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            throw new org.nfunk.jep.ParseException(ex.getMessage());
        }
    }
    
    public ExpressionParserTransformationFunction(ExpressionParser _parser, Transformation _tr)
    {
        numberOfParameters = 1;
        
        parser = _parser;
        tr     = _tr;
    }
}