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
 * <h2>Data Flow Join of 2 inputs</h2>
 */

// *********************************************************\
// *   Description of a Connection between 2 NetworkNodes  *
// *********************************************************/
public class NetworkConnection
{
     private String srcname;
     private String dstname;
     private int    srcport;
     private int    dstport;
     
     public NetworkConnection(String srcname, int srcport, String dstname, int dstport)
     {
       this.srcname = srcname;
       this.dstname = dstname;
       this.srcport = srcport;
       this.dstport = dstport;
     }
     
     public String toString()
     {
         return(srcname+":"+srcport+" -> "+dstname+":"+dstport);
     }
     
	/**
	 * Returns the dstname.
	 * @return String
	 */
	public String getDestinationName()
	{
		return dstname;
	}

	/**
	 * Returns the dstport.
	 * @return int
	 */
	public int getDestinationPort()
	{
		return dstport;
	}

	/**
	 * Returns the srcname.
	 * @return String
	 */
	public String getSourceName()
	{
		return srcname;
	}

	/**
	 * Returns the srcport.
	 * @return int
	 */
	public int getSourcePort()
	{
		return srcport;
	}

	/**
	 * Sets the dstname.
	 * @param dstname The dstname to set
	 */
	public void setDstname(String dstname)
	{
		this.dstname = dstname;
	}

	/**
	 * Sets the dstport.
	 * @param dstport The dstport to set
	 */
	public void setDstport(int dstport)
	{
		this.dstport = dstport;
	}

	/**
	 * Sets the srcname.
	 * @param srcname The srcname to set
	 */
	public void setSrcname(String srcname)
	{
		this.srcname = srcname;
	}

	/**
	 * Sets the srcport.
	 * @param srcport The srcport to set
	 */
	public void setSrcport(int srcport)
	{
		this.srcport = srcport;
	}

}