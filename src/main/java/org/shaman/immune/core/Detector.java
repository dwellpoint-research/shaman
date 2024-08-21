/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *              Artificial Immune Systems                *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.immune.core;

/*********************************************************\
 *              Non-self detector Interface              *
 \*********************************************************/
public interface Detector extends Particle
{
    /*********************************************************\
     *          Generate a random (immature) detector        *
    \*********************************************************/
    public void initRandom(int _len, Body _bod) throws AISException;
    public void makeRandom() throws AISException;
    
    /*********************************************************\
     *               Immune Response Model                   *
    \*********************************************************/
    public boolean getMature();
    public void    setMature(boolean _mature);
    public int     overActivationThreshold(int matchlen, double th);
    public void    resetImmuneResponse();
    public int     giveIdle();
    public void    increaseIdle();
    public boolean isActive();
    public void    decreaseActivation(double sub);
    public void    increaseActivation(int pos, int matchLen, double add);
    
    public int     getAge();
    public void    setAge(int _age);
    public int     getMatchPeriod();
    public void    setMatchPeriod(int _matchPeriod);
    public void    setActivation(double _activation);
    public double  getActivation();
    
    
    /*********************************************************\
     *                        Cloning                        *
    \*********************************************************/
    public void makeClone(Detector det, Particle ag, double fmt) throws AISException;
}
