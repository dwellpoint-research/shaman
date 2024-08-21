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
package org.shaman.datamodel;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.shaman.exceptions.DataModelException;


/**
 * <h2>Default Data Format Converter</h2>
 * The default Object to Double and back converter.
 * Can handle simple categorical and continuous values.
 */
public class ConverterDefault implements ObjectDoubleConverter
{
   // **********************************************************\
   // *   Default Object <-> Double Converter Implementation   *
   // **********************************************************/
   /**
    * Convert the given object to a double in a way that makes a lot of sense most of the time.
    * @param ob The object
    * @param ao The attribute describing the Object's structure
    * @param ad The attribute describing the returned value's structure.
    * @return The double corresponding to the given object
    * @throws DataModelException If the given Object is illegal or cannot be converted to double.
    */
   public double toDouble(Object ob, AttributeObject ao, AttributeDouble ad) throws DataModelException
   {
         double  val;

         val = 0;
         if(ao.checkType(ob))
         {
           if (!ao.isMissingValue(ob))
           {
             if (ao.isLegal(ob))
             {
               if      (ad.hasProperty(Attribute.PROPERTY_CATEGORICAL)) val = ao.getCategoricalDouble(ob, ad);
               else if (ad.hasProperty(Attribute.PROPERTY_CONTINUOUS))  val = getContinuousDouble(ob);
               else    val = 0;
             }
             else if (ao.getIllegalIsMissing()) val = ad.getMissingAsDouble();
                  else throw new DataModelException("Illegal object '"+ob+"' found in Attribute '"+ao.getName()+"'. Cannot treat it as missing.");
           }
           else val = ad.getMissingAsDouble();
         }
         else throw new DataModelException("Object has wrong type. "+ob.getClass().getName()+". Attribute name "+ao.getName());

         return(val);
   }

   /**
    * Convert the given double to an Object. Only works if both attributes are categorical
    * or the Object Attribute is a String.
    * @param d The double
    * @param ad The attribute describing the double's structure
    * @param ao The attribute describing the returned Object's structure.
    * @throws DataModelException If the double is illegal or if it cannot be converted to an Object.
    */
   public Object toObject(double d, AttributeDouble ad, AttributeObject ao) throws DataModelException
   {
         Object  val;

         val = null;
         if (!ad.isMissingValue(d))
         {
             if (ad.isLegal(d))
             {
               // Convert categorical double to categorical object
               if (ao.hasProperty(Attribute.PROPERTY_CATEGORICAL)) val = ad.getCategoricalObject(d, ao);
               else
               {
                 // Check if the Object is an type that can hold a double number.
                 if      (ao.getRawType().equals("java.lang.String"))     val = ""+d;
                 else if (ao.getRawType().equals("java.lang.Double"))     val = new Double(d);
                 else if (ao.getRawType().equals("java.math.BigDecimal")) val = new BigDecimal(d);
                 else // Otherwise it's really not that clear what to do...
                    throw new DataModelException("Cannot convert non-categorical double to Object of type "+ao.getRawType());
               }
             }
             else if (ad.getIllegalIsMissing()) val = ao.getMissingAsObject();
                  else throw new DataModelException("Illegal value "+d+" found in Attribute '"+ad.getName()+"'. Cannot treat it as missing.");
         }
         else val = ao.getMissingAsObject();

         return(val);
   }

   private double getContinuousDouble(Object ob) throws DataModelException
   {
     double val;

     val = 0;
     if (ob instanceof String)
     {
       try { val = Double.parseDouble((String)ob); }
       catch(NumberFormatException ex)
       {
         val = ob.hashCode();
         //throw new DataModelException(ex);
       }
     }
     else if (ob instanceof Double)     val = ((Double)ob).doubleValue();
     else if (ob instanceof Integer)    val = ((Integer)ob).doubleValue();
     else if (ob instanceof Byte)       val = ((Byte)ob).doubleValue();
     else if (ob instanceof Float)      val = ((Float)ob).doubleValue();
     else if (ob instanceof Long)       val = ((Long)ob).doubleValue();
     else if (ob instanceof Short)      val = ((Short)ob).doubleValue();
     else if (ob instanceof BigDecimal) val = ((BigDecimal)ob).doubleValue();
     else if (ob instanceof BigInteger) val = ((BigInteger)ob).doubleValue();
     else throw new DataModelException("Default ObjectDouble converter does not recognize type "+ob.getClass().getName());

     return(val);
   }

   // **********************************************************\
   // *                         Cloneing                       *
   // **********************************************************/
   public Object clone() throws CloneNotSupportedException
   {
     ConverterDefault cdn = new ConverterDefault();

     return(cdn);
   }

   public ConverterDefault()
   {
   }
}
