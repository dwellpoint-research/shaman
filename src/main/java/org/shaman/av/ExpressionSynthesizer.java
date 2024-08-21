/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  January 2005                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005   Shaman Research                 *
\*********************************************************/
package org.shaman.av;

import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.shaman.dataflow.Transformation;
import org.shaman.datamodel.DataModel;
import org.shaman.datamodel.DataModelObject;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.DataFlowException;
import org.shaman.exceptions.DataModelException;
import org.shaman.exceptions.TransformationException;

import cern.colt.matrix.ObjectMatrix1D;


/**
 * <h2>Expression Synthesizer</h2>
 * An Audio Source using Expressions to generate sound data. 
 */
public class ExpressionSynthesizer extends Transformation
{ 
    private SynthComponent []component;            // Expression Synthesizer components
    private int              outputSize;           // Number of sample to generate in one push
    
    // **** Run-time Data ****
    private DataModelObject        dataModel;
    private DataModelPropertyAudio propAud;
    private int                    numChannels;
    private double                 sampleRate;
    private long                   samplePos;
    private double             [][]channel;
    
    // **********************************************************\
    // *               Generate Audio Vectors                   *
    // **********************************************************/
    public boolean push() throws DataFlowException
    {
        boolean        more;
        ObjectMatrix1D audio;
        
        // Generate next audio vector
        audio = synthesize();
        if (audio != null)
        {
            // Output audio. More to come.
            more = true;
            setOutput(0, audio);
        }
        else more = false; // No more audio available
        
        return(more);
    }
    
    private ObjectMatrix1D synthesize() throws TransformationException
    {
        ObjectMatrix1D audvec;
        double         tbeg, tend, tnow, tstep;
        int            numt;
        int            i,j,k;
        
        try
        {
            // Create a new audio vector
            audvec       = this.dataModel.createDefaultVector();
            for (i=0; i<this.numChannels; i++) audvec.setQuick(i, this.channel[i]);
        }
        catch(DataModelException ex) { throw new TransformationException(ex); }
        
        // Calculate begin and end-time of audio vector
        tbeg            = this.samplePos/this.sampleRate;
        numt            = this.outputSize/this.numChannels;
        this.samplePos += numt;
        tend            = this.samplePos/this.sampleRate;
        tstep           = (tend-tbeg)/numt;
        this.propAud.setTimePos((long)(tbeg*1000));
        
        // Synthesize sound using the components
        Map     compmap;
        double []compout;
        
        compmap = new HashMap();
        tnow    = tbeg;
        for (i=0; i<numt; i++)
        {
            // Evaluate all components in order. Put output in channel buffers.
            compmap.clear();
            for (j=0; j<this.component.length; j++)
            {
                compout = this.component[j].synthesize(tnow, compmap);
                if (compout != null)
                {
                    for (k=0; k<compout.length; k++)
                        if (compout[k] != Double.NaN) this.channel[k][i] = compout[k];
                }
            }
            
            // Move on to the next time moment
            tnow += tstep;
        }
        
        return(audvec);
    }
    
    // **********************************************************\
    // *                 Parameter Configuration                *
    // **********************************************************/
    public void setOutputSize(int outSize) { this.outputSize = outSize; }
    
    public void setComponents(SynthComponent []component) { this.component = component; }
    
    // **********************************************************\
    // *                Transformation Implementation           *
    // **********************************************************/
    public void init() throws ConfigException
    {
        DataModelPropertyAudio propAud;
        DataModelObject outModel; 
        AudioFormat     aform;
        
        // Make an Audio Stream DataModel and install it on the output port
        aform   = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,  22500.0f, 16, 2, 4, 22500.0f, true);
        propAud = new DataModelPropertyAudio();
        propAud.setAudioFormat(aform);
        propAud.setChannels(aform.getChannels());
        propAud.setSize(this.outputSize);
        outModel = AVDataModels.makeAudioDataModel(aform, this.outputSize, propAud);
        setOutputDataModel(0, outModel);
        this.propAud     = propAud;
        this.dataModel   = outModel;
        
        // Remember output characteristics
        this.numChannels = propAud.getChannels();
        this.sampleRate  = aform.getSampleRate();
        this.samplePos   = 0;
        
        // Initialize the components
        int i;
        
        //this.expression = "sin(700*t + 205*t*sin(3*t))*.4"; // Mathematica example
        //this.expression = "sin(2*pi*440*t)*.4";
        
        this.component = new SynthComponent[]
        {
            new SynthComponent(  "f", "30*t*sin(300*t)"),
            new SynthComponent("out", "sin(700*t + f)*.4", new int[]{0,1})
        };
        
        for (i=0; i<this.component.length; i++) this.component[i].init();
        
        // Create a vector that will hold the audio data
        this.channel = new double[this.numChannels][this.outputSize/this.numChannels];
    }
    
    public void cleanUp() throws DataFlowException
    {
    }
    
    public void checkDataModelFit(int port, DataModel dmin) throws DataModelException
    {
        ; // All fine. No inputs anyway
    }
    
    public int getNumberOfInputs()  { return(0); }
    public int getNumberOfOutputs() { return(1); }
    public String getInputName(int port) {  return(null); }
    public String getOutputName(int port)
    {
        if   (port == 0) return("Synthesized Audio Source");
        else return(null);
    }
    
    // **********************************************************\
    // *              Constructor/State Persistence             *
    // **********************************************************/
    public ExpressionSynthesizer()
    {
       super();
       name        = "Expression Synthesize";
       description = "An audio source generating sound described by an expression";
    }
}