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
package org.shaman.immune.network;

import org.shaman.immune.core.AISException;
import org.shaman.immune.core.BodyListener;

/*********************************************************\
 *             A live component base class               *
 \*********************************************************/
public abstract class Live implements Runnable, BodyListener
{	
    // Activation Trigger
    public static final int TRIGGER_NONE         = 0;  // Do not get triggered
    public static final int TRIGGER_TIMER        = 1;  // Activate at regular intervals
    public static final int TRIGGER_NOTIFICATION = 2;  // Activate after immune response has taken place in the local body
    int      trigger;             // Type of activation trigger
    long     timerPeriod;         // Period between 2 timed triggers
    String   []triggerNots;       // Notification that should trigger activation
    
    private Thread    triggerThread;       // Thread that trigger the activity
    private boolean   triggerRunning;      // Is the triggerthread running
    
    // State
    public static final int LIVE_INITIALIZED = 1;
    public static final int LIVE_RUNNING     = 10;  // Trigger mechanism in place. Now running and active.
    public static final int LIVE_SUSPENDED   = 20;  // Part of DAIS but not active.
    public static final int LIVE_STOPPED     = 50;  // Ended. Can be recycled by re-initialization.
    protected int    liveState;
    
    /*********************************************************\
     *                    Activity                           *
     \*********************************************************/
    public abstract void activate(String not) throws AISException;
    
    public void understood(String und) {};
    
    /*********************************************************\
     *                Activation Mechanism                   *
     \*********************************************************/
    public void suspend() throws IllegalStateException
    {
        if (liveState != LIVE_RUNNING) throw new IllegalStateException("Live::suspend() Error : Object not in RUNNING state.");
        
        liveState = LIVE_SUSPENDED;
    }
    
    public void wakeUp() throws IllegalStateException
    {
        if (liveState != LIVE_SUSPENDED) throw new IllegalStateException("Live::wakeUp() Error : Object not in SUSPENDED state.");
        
        liveState = LIVE_RUNNING;
    }
    
    public void startStatic()
    {
        trigger  = TRIGGER_NONE;
        start();
    }
    
    public void startTimed(long _timerPeriod) throws IllegalStateException
    {
        trigger     = TRIGGER_TIMER;
        timerPeriod = _timerPeriod;
        start();
    }
    
    private void start() throws IllegalStateException
    {
        if ((liveState == LIVE_INITIALIZED) || (liveState == LIVE_STOPPED))
        {
            
            // Timer based trigger
            if (trigger == TRIGGER_TIMER)
            {
                triggerThread  = new Thread(this);
                triggerRunning = true;
                triggerThread.start();
            }
            
            if (trigger == TRIGGER_NONE)
            {
                triggerThread = null;
            }
            
            // Node is running
            liveState = LIVE_RUNNING;
        }
        else throw new IllegalStateException("Live::start() Error : Object is not in INITIALIZED or STOPPED state.");
    }
    
    public void stopLive() throws IllegalStateException
    {
        // End the triggermechanism
        if ((liveState == LIVE_RUNNING) || (liveState == LIVE_SUSPENDED))
        {
            if (triggerThread != null) destroyTriggerThread();
            
            liveState = LIVE_STOPPED;
        }
        else throw new IllegalStateException("Live::stopLive() Error : Object is not RUNNING or SUSPENDED");
    }
    
    public void notify(String message, Object ob) throws AISException
    {
        int i;
        boolean found;
        
        found = false;
        for (i=0; (i<triggerNots.length) && (!found); i++)
        {
            if (triggerNots[i].equals(message)) found = true;
        }
        
        if (found && (liveState == LIVE_RUNNING) && (trigger == TRIGGER_NOTIFICATION))
        {
            // Do one activation cycle
            activate(message);
        }
    }
    
    public void run()
    {
        long period;
        
        // Timed trigger activation
        while (triggerRunning)
        {
            try
            {
                // Sleep for a while
                if (timerPeriod != 0) period = timerPeriod;
                else                  period = 100;
                Thread.sleep(timerPeriod);
                
                // Do one DAIS node activation cycle.
                if ((liveState == LIVE_RUNNING) && (trigger == TRIGGER_TIMER)) activate(BodyListener.TIMER);
            }
            catch(InterruptedException ex) { ex.printStackTrace(); }
            catch(Exception ex)            { ex.printStackTrace(); }
        }
    }
    
    void destroyTriggerThread()
    {
        triggerRunning = false;
    }
    
    public int getLiveState() { return(liveState); }
    
    /*********************************************************\
     *            Constructor and initialization             *
     \*********************************************************/
    public Live()
    {
        liveState = LIVE_INITIALIZED;
    }
}
