/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005                                           *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.evolution;

import cern.jet.random.AbstractDistribution;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.LearnerException;
import org.shaman.learning.InstanceSetMemory;
import org.shaman.neural.MLP;


public class MLPEnvironment implements Environment
{
    // Parameters
    private InstanceSetMemory    dataset;       // The dataset to evaluate network performance on
    private MLP                  mlp;           // The neural-network
    private AbstractDistribution pdfTemplate;   // Neuron Weight PDF template
    
    // ---- Evolution Resources ----
    private AbstractDistribution []genotypePDFs;      // PDF's for the neuron weights that define the genotype
    
    // *********************************************************\
    // *       Evolutionary Environment Implementation         *
    // *********************************************************/
    public Genotype makeRandomGenotype() throws LearnerException
    {
        NumberGenotype gen;
        
        gen = new NumberGenotype(this.genotypePDFs);
        gen.initRandom(this.genotypePDFs.length);
        
        return(gen);
    }
    
    public FitnessFunction makeFitnessFunction() throws ConfigException
    {
        MLPFitnessFunction fit;
        
        fit = new MLPFitnessFunction();
        fit.setMLPTemplate(this.mlp);
        fit.setPDFTemplate(this.pdfTemplate);
        fit.setDataSet(this.dataset);
        fit.initialize();
        
        return(fit);
    }
    
    // *********************************************************\
    // *                      Configuration                    *
    // *********************************************************/
    public void setMLPTemplate(MLP mlp) { this.mlp = mlp; }
    public void setDataSet(InstanceSetMemory dataset) { this.dataset = dataset; }
    public void setPDFTemplate(AbstractDistribution pdfTemplate) { this.pdfTemplate = pdfTemplate; }
    
    // *********************************************************\
    // *               Initialization / Cleanup                *
    // *********************************************************/
    public void initialize() throws ConfigException
    {
        MLPFitnessFunction fitnessFunction;
        
        // Make the fitness function that measures the performance of evolved MLP
        fitnessFunction = (MLPFitnessFunction)makeFitnessFunction();
        
        // Get the String of PDFs associated with the Neural Net's weights.
        this.genotypePDFs = fitnessFunction.getGenoTypePDFs();
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    // *********************************************************\
    // *                    Construction                       *
    // *********************************************************/
    public MLPEnvironment()
    {
    }
}