/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    MNIST2Format.java
 *    Copyright (C) 2005 FracPete
 *
 */

package org.shaman.mnist;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



/**
 * This little helper class transforms the raw data (floats, stored in little
 * endian) into another format. It takes as first parameter the feature file,
 * as second the label data and finally as third and last parameter the output
 * file
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */

public abstract class MNIST2Format {
  /** the number of features */
  protected final static int NUM_FEATURES = 576;

  /** whether we're only testing */
  protected final static boolean TESTING = false;
  
  /** the feature file */
  protected String m_FeaturesFilename;
  protected FileInputStream m_Features;

  /** the label file */
  protected String m_LabelsFilename;
  protected FileInputStream m_Labels;

  /** the csv output file */
  protected String m_OutputFilename;
  protected PrintStream m_Output;
  
  /**
   * initializes the transformation
   */
  public MNIST2Format() {
    m_FeaturesFilename = "";
    m_LabelsFilename   = "";
    m_OutputFilename   = "";
  }

  /**
   * sets the file to retrieve the features from
   * @param filename      the filename 
   */
  public void setFeatureFile(String filename) {
    m_FeaturesFilename = filename;
  }

  /**
   * sets the file to retrieve the labels from
   * @param filename      the filename 
   */
  public void setLabelFile(String filename) {
    m_LabelsFilename = filename;
  }

  /**
   * sets the file to store the output in
   * @param filename      the filename 
   */
  public void setOutputFile(String filename) {
    m_OutputFilename = filename;
  }

  /**
   * returns the byte-array as Java float
   * (converting from little to big endian)
   * @param array           to array representing a 4-byte float in little
   *                        endian
   * @return                a Java float (big endian)
   * @throws Exception      if anything goes wrong
   */
  protected float arrayToFloat(byte[] array) throws Exception {
    return ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).getFloat();
  }

  /**
   * returns the byte-array as Java int
   * (converting from little to big endian)
   * @param array           to array representing a 4-byte intt in little
   *                        endian
   * @return                a Java int (big endian)
   * @throws Exception      if anything goes wrong
   */
  protected int arrayToInt(byte[] array) throws Exception {
    return ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  /**
   * writes the header of the output file
   */
  protected abstract void writeHeader() throws Exception;

  /**
   * writes the given data to the output file
   */
  protected abstract void writeData(float[] features, int cls) throws Exception;

  /**
   * writes the footer of the output file
   */
  protected abstract void writeFooter() throws Exception;

  /**
   * generates the output from the raw data
   * @throws Exception      in case something goes wrong
   */
  public void execute() throws Exception {
    byte[]              array;
    int                 cls;
    int                 count;
    int                 readCount;
    float               value;
    float[]             features;
    int                 lineCount;
    int                 available;

    // init
    m_Features = new FileInputStream(m_FeaturesFilename);
    m_Labels   = new FileInputStream(m_LabelsFilename);
    m_Output   = new PrintStream(new FileOutputStream(m_OutputFilename));
    array      = new byte[4];
    features   = new float[NUM_FEATURES];

    // print output header
    writeHeader();
    
    // read data
    count     = 0;
    lineCount = 0;
    while ((available = m_Features.available()) > 0) {
      count++;

      // read+print feature
      readCount           = m_Features.read(array);
      value               = arrayToFloat(array);
      features[count - 1] = value;

      // class?
      if (count == NUM_FEATURES) {
        readCount = m_Labels.read(array);
        cls       = arrayToInt(array);

        // write data
        writeData(features, cls);

        // re-initialize variables
        count    = 0;
        features = new float[NUM_FEATURES];
        cls      = 0;
        
        // info
        lineCount++;
        if ( (lineCount % 100 == 0) || (available == 0) ) {
          System.out.print("line " + lineCount + "\r");
          System.out.flush();
          if (TESTING)
            break;
        }
      }
    }

    // close streams
    m_Features.close();
    m_Labels.close();
    m_Output.flush();
    m_Output.close();

    // write footer
    writeFooter();

    System.out.println();
  }
}
