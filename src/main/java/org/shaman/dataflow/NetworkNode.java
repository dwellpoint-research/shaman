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
package org.shaman.dataflow;

/**
 * <h2>Desription of a Network Node</h2>
 * Used in combination with NetworkConnection to make
 * an easy description of a TransformationNetwork.
 */

// *********************************************************\
// * Basic Description of Node in a Transformation Network *
// *********************************************************/
public class NetworkNode
{
     private String name;
     private String className;
     private String description;
     private int    []grow;   
     private int    order;
     
     public String toString()
     {
        return(order+"\t"+name+"\t"+className+"\t"+description);
     }
     
     public NetworkNode(String name, String className, String description)
     {
       this.name        = name;
       this.className   = className;
       this.description = description;
       this.grow        = null;
       this.order       = -1;
     }
     
     public NetworkNode(String name, String className, String description, int order)
     {
       this.name        = name;
       this.className   = className;
       this.grow        = null;
       this.description = description;
       this.order       = order;
     }
     
     public NetworkNode(String name, String className, int []grow, String description, int order)
     {
       this.name        = name;
       this.className   = className;
       this.grow        = grow;
       this.description = description;
       this.order       = order;
     }
     
     /**
      * Returns the grow array.
      * @return grow. The grow array for the specified transformation
      */ 
     public int []getGrow()
     {
        return(this.grow);
     }
     
     /**
      * Sets the grow array
      * @param grow The new grow array.
      * /
     public void setGrow(int []grow)
     {
        this.grow = grow;
     }
     
	/**
	 * Returns the description.
	 * @return String
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the order.
	 * @return int
	 */
	public int getOrder()
	{
		return order;
	}

	/**
	 * Sets the description.
	 * @param description The description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Sets the order.
	 * @param order The order to set
	 */
	public void setOrder(int order)
	{
		this.order = order;
	}

	/**
	 * Returns the className.
	 * @return String
	 */
	public String getClassName()
	{
		return className;
	}

	/**
	 * Sets the className.
	 * @param className The className to set
	 */
	public void setClassName(String className)
	{
		this.className = className;
	}

}