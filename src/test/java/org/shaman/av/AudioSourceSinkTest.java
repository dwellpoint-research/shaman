/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                    Audio / Video                      *
 *                                                       *
 *  August 2004                                          *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2004-5 Shaman Research                 *
\*********************************************************/
package org.shaman.av;

import org.shaman.exceptions.ShamanException;

import junit.framework.TestCase;


/**
 * <h2>Audio Test</h2>
 */

// **********************************************************\
// *                 Unit Tests for Audio                   *
// **********************************************************/
public class AudioSourceSinkTest extends TestCase
{
    public void testEmpty(){}

    public void doNot_testAudioSourceSink() throws ShamanException
    {
        AudioSource src  = new AudioSource();
        AudioSink   sink = new AudioSink();
        src.registerConsumer(0, sink, 0);
        sink.registerSupplier(0, src, 0);
        
        src.setFilePath("d:/Music/MyMusicII/Pulp Fiction/01-Misirlou.mp3");
        src.setInputType(AudioSource.TYPE_MPEG);
        src.setOutputSize(8192);
        src.setActive(true);
        src.init();
        
        sink.setType(AudioSink.TYPE_SOUND);
        sink.init();
        
        while(src.push());
        System.err.println("No audio left");
        src.cleanUp();
        sink.cleanUp();
        
        System.exit(0);
    }
    
    public void doNot_testFFT() throws ShamanException
    {
        AudioSource src  = new AudioSource();
        AudioSink   sink = new AudioSink();
        AudioFFT     fft = new AudioFFT();
        AudioFFT    ifft = new AudioFFT();
        src.registerConsumer(0,  fft, 0);
        fft.registerSupplier(0, src, 0);
        fft.registerConsumer(0, ifft, 0);
        ifft.registerSupplier(0, fft, 0);
        ifft.registerConsumer(0, sink, 0);
        sink.registerSupplier(0, ifft, 0);
        
        //src.setFilePath("c:/Projects/Perpetuum/samples/ezekiel.mp3");
        src.setFilePath("d:/Music/MyMusicII/Pulp Fiction/15-Surf rider.mp3"); //01-Misirlou.mp3");
        src.setInputType(AudioSource.TYPE_MPEG);
        src.setOutputSize(4096);
        fft.setForward(true);
        ifft.setForward(false);
        src.init();
        fft.init();
        ifft.init();
        sink.init();
        
        while(src.push());
        System.err.println("No audio left");
        src.cleanUp();
        sink.cleanUp();
        
        System.exit(0);
    }
    
    // **********************************************************\
    // *               Unit Test Setup/Teardown                 *
    // **********************************************************/
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public AudioSourceSinkTest(String name)
    {
        super(name);  
    }
}
