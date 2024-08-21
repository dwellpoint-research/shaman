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
package org.shaman;

import java.io.File;
import java.io.IOException;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.Attribute;
import org.shaman.datamodel.AttributeDouble;
import org.shaman.datamodel.AttributeObject;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelDouble;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ShamanException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.util.FileUtil;


/**
 * <h2>Test Utilities<h2>
 * A number of handy methods for the unit-tests
 */
public class TestUtils
{
    // **********************************************************\
    // *              Test Output File Comparision              *
    // **********************************************************/
    public static boolean compareTextFile(String file, String filetemp) throws DataFlowException
    {
        boolean equal;
        File    ffile, ffiletemp;
        String  sfile, sfiletemp;
        
        equal = false;
        try
        {
            // Compare both text files.
            ffile     = new File(file);
            ffiletemp = new File(filetemp);
            sfile     = FileUtil.readTextFileToString(ffile, false);
            sfiletemp = FileUtil.readTextFileToString(ffiletemp, false);
            equal     = sfile.equals(sfiletemp);
        }
        catch(IOException ex) { throw new DataFlowException(ex); }
        
        return(equal);
    }
    
    // **********************************************************\
    // *        Request/Response Transformation Access          *
    // **********************************************************/
    public static Object []transform1To1(Transformation trans, Object in) throws ShamanException
    {
        Object []out;
        
        out = null;
        if ((trans.getNumberOfInputs() == 1) && ((trans.getNumberOfOutputs() == 1)))
        {
            trans.isolate();
            if (in != null) trans.setSupplierData(0, in);
            out = trans.getConsumerData(0);
            
            trans.clear();
        }
        else throw new DataFlowException("Wrong number of input or output ports. Expected 1-1 but found a "+trans.getNumberOfInputs()+"-"+trans.getNumberOfOutputs()+" Transformation.");
    
        return(out);
    }
    
    public static Object []transform2To1(Transformation trans, Object in1, Object in2) throws ShamanException
    {
        Object  []out;
        
        out = null;
        if ((trans.getNumberOfInputs() == 2) && ((trans.getNumberOfOutputs() == 1)))
        {
            trans.isolate();
            if (in1 != null) trans.setSupplierData(0, in1);
            if (in2 != null) trans.setSupplierData(1, in2);
            out = trans.getConsumerData(0);
            
            trans.clear();
        }
        else throw new DataFlowException("Wrong number of input or output ports. Expected 2-1 but found a "+trans.getNumberOfInputs()+"-"+trans.getNumberOfOutputs()+" Transformation.");
    
        return(out);
    }
    
    public static Object [][]transform2To2(Transformation trans, Object in1, Object in2) throws ShamanException
    {
        Object  [][]out;
        
        out = null;
        if ((trans.getNumberOfInputs() == 2) && ((trans.getNumberOfOutputs() == 2)))
        {
            trans.isolate();
            if (in1 != null) trans.setSupplierData(0, in1);
            if (in2 != null) trans.setSupplierData(1, in2);
            
            out    = new Object[2][];
            out[0] = trans.getConsumerData(0);
            out[1] = trans.getConsumerData(1);
            
            trans.clear();
        }
        else throw new DataFlowException("Wrong number of input or output ports. Expected 2-2 but found a "+trans.getNumberOfInputs()+"-"+trans.getNumberOfOutputs()+" Transformation.");
    
        return(out);
    }
    
    public static Object [][]transformNToM(Transformation trans, Object []in) throws ShamanException
    {
        int        i;
        Object [][]out;
        
        out = null;
        if (trans.getNumberOfInputs() == in.length)
        {
            trans.isolate();
            for (i=0; i<in.length; i++) if (in[i] != null) trans.setSupplierData(i, in[i]);
            out = new Object[trans.getNumberOfOutputs()][];
            for (i=0; i<out.length; i++) out[i] = trans.getConsumerData(i);
            
            trans.clear();
        }
        else throw new DataFlowException("Wrong number of input or output ports. Expected 2-1 but found a "+trans.getNumberOfInputs()+"-"+trans.getNumberOfOutputs()+" Transformation.");
    
        return(out);
    }
    
    // **********************************************************\
    // *                 Simple DataModel Makers                *
    // **********************************************************/
    public static DataModelObject makeStringDataModel(int size) throws DataModelException
    {
        DataModelObject  dm;
        int              i;
        AttributeObject  atob;
        
        dm = new DataModelObject("String Datamodel", size);
        for (i=0; i<size; i++)
        {
            atob = dm.getAttributeObject(i);
            atob.initAsFreeText();
            atob.setName("attribute"+i);
            atob.setIsActive(true);
        }
        
        return(dm);
    }
    
    public static DataModel makeNumberDataModel(int size, boolean primitive) throws DataModelException
    {
        DataModel        dmnum;
        int              i;
        AttributeDouble  atdo;
        AttributeObject  atob;
        Attribute        at;
        
        if (primitive) dmnum = new DataModelDouble("Number Datamodel", size);
        else           dmnum = new DataModelObject("Number Datamodel", size);
        
        for (i=0; i<size; i++)
        {
            if (primitive)
            {
                atdo = (AttributeDouble)dmnum.getAttribute(i);
                atdo.initAsNumberContinuous();
                at   = atdo;
            }
            else
            {
                atob = (AttributeObject)dmnum.getAttribute(i);
                atob.initAsNumberContinuous("java.lang.Double");
                at   = atob;
            }
            at.setName("attribute"+i);
            at.setIsActive(true);
        }
        
        return(dmnum);
    }
}
