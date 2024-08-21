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
 *    MNIST2ARFF.java
 *    Copyright (C) 2005 FracPete
 *
 */

package org.shaman.mnist;

/**
 * This little helper class transforms the raw data (floats, stored in little
 * endian) into ARFF format. It takes as first parameter the feature file,
 * as second the label data and finally as third and last parameter the output
 * file
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */

public class MNIST2ARFF extends MNIST2Format {
  /**
   * initializes the transformation
   */
  public MNIST2ARFF() {
    super();
  }

  /**
   * writes the header of the output file
   */
  protected void writeHeader() throws Exception {
    int                 i;

    m_Output.println("@relation MNIST-576");
    m_Output.println();

    for (i = 0; i < NUM_FEATURES; i++) {
      m_Output.println("@attribute feature" + (i+1) + " numeric");
    }
    
    m_Output.println("@attribute class {0,1,2,3,4,5,6,7,8,9}");

    m_Output.println();
    m_Output.println("@data");
  }

  /**
   * writes the given data to the output file
   */
  protected void writeData(float[] features, int cls) throws Exception {
    int                 i;

    for (i = 0; i < NUM_FEATURES; i++) {
      if (i > 0)
        m_Output.print(",");
      m_Output.print(features[i]);
    }
    
    m_Output.println("," + cls);
  }

  /**
   * writes the footer of the output file
   */
  protected void writeFooter() throws Exception {
  }

  /**
   * you must call the class like this:<br/>
   * MNIST2CSV feature-file label-file output-file
   */
  public static void main(String[] args) throws Exception {
    MNIST2ARFF    m;
    
    // ./data/mnist/train-images-idx3-ubyte ./data/mnist/train-labels-idx1-ubyte ./data/mnist/train-images.arff
    

    // correct parameters?
    if (args.length != 3) {
      System.out.println();
      System.out.println("Converts the MNIST database from raw Intel float");
      System.out.println("into ARFF format (576 features per line).");
      System.out.println();
      System.out.println("Usage: MNIST2ARFF feature-file label-file output-file");
      System.out.println();
      System.exit(1);
    }
    
    // process
    m = new MNIST2ARFF();
    m.setFeatureFile(args[0]);
    m.setLabelFile(args[1]);
    m.setOutputFile(args[2]);
    m.execute();
  }
}

