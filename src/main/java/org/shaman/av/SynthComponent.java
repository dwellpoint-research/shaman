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

import java.util.Iterator;
import java.util.Map;

import org.shaman.exceptions.ConfigException;
import org.shaman.rule.ExpressionParser;



/**
 * <h2>Expression Synth Component</h2>
 */
public class SynthComponent 
{
    public static final int CHANNEL_LEFT  = 0;
    public static final int CHANNEL_RIGHT = 1;
    
    private String name;              // Name of this component
    private String expression;        // Expression of this component
    private int  []channel;           // Output channels
    
    // --- Component Operation ---
    private ExpressionParser expar;
    private double         []outbuf;
    
    // **********************************************************\
    // *            Parameter Definition and Access             *
    // **********************************************************/
    public String getName()       { return(this.name); }
    public String getExpression() { return(this.expression); }
    public int  []getChannels()   { return(this.channel); }
    
    // **********************************************************\
    // *                   Component Operation                  *
    // **********************************************************/
    public double []synthesize(double t, Map compmap)
    {
        Iterator itcomp;
        String   cname;
        Double   cval;
        double   synth;
        
        // Put the values of all other components in the parser
        itcomp = compmap.keySet().iterator();
        while(itcomp.hasNext())
        {
            cname = (String)itcomp.next();
            cval  = (Double)compmap.get(cname);
            this.expar.addVariable(cname, cval.doubleValue());
        }
        
        // And the current time
        this.expar.addVariable("t", t);
        
        // Evaluate component at the current time
        synth = this.expar.getValue();
        
        // Put value in output buffer when output channel(s) are defined
        if (this.channel != null)
        {
            int   i;
            for (i=0; i<this.channel.length; i++)
            {
                this.outbuf[this.channel[i]] = synth;
            }
        }
        
        // Put value in component value map
        compmap.put(this.name, new Double(synth));
        
        return(this.outbuf);
    }
    
    public void init() throws ConfigException
    {
        ExpressionParser expar;
        
        // Parse the given expression
        expar = new ExpressionParser();
        expar.setAllowUndeclared(true);
        expar.parseExp(this.expression);
        this.expar = expar;
        
        if (this.channel != null)
        {
            int i,maxchan;
            
            // Make an output buffer if this component does output
            maxchan = -1;
            for (i=0; i<this.channel.length; i++)
                if (this.channel[i] > maxchan) { maxchan = this.channel[i]; }
            this.outbuf = new double[maxchan+1];
            for (i=0; i<this.outbuf.length; i++) this.outbuf[i] = Double.NaN;
        }
    }
    
    // **********************************************************\
    // *        Create Synthesizer Expression Component         *
    // **********************************************************/
    public SynthComponent(String name, String expression)
    {
        this.name       = name;
        this.expression = expression;
    }
    
    public SynthComponent(String name, String expression, int []channel)
    {
        this.name       = name;
        this.expression = expression;
        this.channel    = channel;
    }
}
