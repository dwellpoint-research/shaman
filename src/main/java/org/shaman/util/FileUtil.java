/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                   Utility Methods                     *
 *                                                       *
 *  January 2005                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * File utilities.
 */
public class FileUtil
{
    public static void logToMathematicaTableFile(String filename, double []ind, double []dat) throws IOException
    {
        // Log to Mathematica table format
        int i;
        FileWriter   pkout;
        StringBuffer stout = new StringBuffer("x = {");
        
        for (i=0; i<ind.length; i++)
        {
            stout.append("{"+ind[i]+", "+dat[i]+"");
            if (i == ind.length-1) stout.append("}}");
            else                   stout.append("},\n");
        }
        pkout = new FileWriter(filename);
        pkout.write(stout.toString());
        pkout.flush();
        pkout.close();
    }
    
    public static void logToFile(String filename, double []ind, double []dat) throws IOException
    {
        logToFile(filename, ind, dat, 1);
    }
    
    public static void logToFile(String filename, double []ind, double []dat, int stride) throws IOException
    {
        // Log to two-column space-separated file
        int i;
        FileWriter   pkout;
        StringBuffer stout = new StringBuffer();
        
        for (i=0; i<ind.length; i+=stride) stout.append(ind[i]+" "+dat[i]+"\n");
        pkout = new FileWriter(filename);
        pkout.write(stout.toString());
        pkout.flush();
        pkout.close();
    }
    
    public static void logToFile2D(String filename, double []ind, double [][]dat) throws IOException
    {
        int        i,j;
        FileWriter lout;
        StringBuffer stout = new StringBuffer();
        
        for (i=0; i<ind.length; i++)
        {
            stout.append(ind[i]);
            for (j=0; j<dat[i].length; j++) stout.append(" "+dat[i][j]);
            stout.append("\n");
        }
        lout = new FileWriter(filename);
        lout.write(stout.toString());
        lout.flush();
        lout.close();
    }
    
    /**
     * Reads a text file into a string.
     *
     * @param f                the file to be read
     * @param trimWhiteSpace   if <code>true</code> , all white space in front
     *      and at the end of a line will be romved, new line characters
     *      inclusive.
     * @return                 a string with the contents of the file
     * @exception IOException
     */
    public static String readTextFileToString(File f, boolean trimWhiteSpace) throws IOException
    {
        StringBuffer buf = new StringBuffer();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(f));
            String lineSeparator = System.getProperty("line.separator");
            String line = reader.readLine();
            while (line != null)
            {
                if (trimWhiteSpace)
                {
                    line = line.trim();
                    if (line.startsWith("<!--") && line.endsWith("-->"))
                        continue;
                }
                buf.append(line);
                if (!trimWhiteSpace)
                    buf.append(lineSeparator);
                
                line = reader.readLine();
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
        
        return buf.toString();
    }
    
    /**
     * Writes a string into a text file.
     *
     * @param f                the file to be written
     * @param text             the string to write
     * @exception IOException
     */
    public static void writeStringToTextFile(File f, String text) throws IOException
    {
        writeStringToTextFile(f, text, false);
    }
    
    /**
     * Writes a string into a text file.
     *
     * @param f                the file to be written
     * @param text             the string to write
     * @param append           <code>true</code> to append to an existing file
     * @exception IOException
     */
    public static void writeStringToTextFile(File f, String text, boolean append) throws IOException
    {
        if (f.getParentFile() != null)
        {
            f.getParentFile().mkdirs();
        }
        
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(f.getAbsolutePath(), append));
            if (text == null)
            {
                writer.write("");
            }
            else
            {
                writer.write(text);
            }
        }
        finally
        {
            if (writer != null)
            {
                writer.close();
            }
        }
    }
}
