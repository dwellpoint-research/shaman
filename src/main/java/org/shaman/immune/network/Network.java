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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/*********************************************************\
 *   TCP Network Connection Communication Primitives     *
 \*********************************************************/
public class Network
{
    public  String         servername;
    public  int            port;
    private boolean        opened;
    private Socket         sock;
    private BufferedWriter out;
    private BufferedReader in;
    
    /*********************************************************\
     *                       Member Access                   *
     \*********************************************************/
    public String  getServerName() { return(servername); }
    public int     getPort()       { return(port); }
    public boolean getOpened()     { return(opened); }
    public Socket  getSocket()     { return(sock); }
    
    /*********************************************************\
     *              Communication primitives                 *
     \*********************************************************/
    public String readString() throws IOException
    {
        String s;
        
        s = null;
        if (opened)
        {
            int  len;
            char []cbuf;
            
            len  = readInt();
            cbuf = new char[len];
            in.read(cbuf, 0, len);
            s    = new String(cbuf);
        }
        else throw new IOException("Network::readString() Error : Network not open");
        
        return(s);
    }
    
    public void writeString(String s) throws IOException
    {
        if (opened)
        {
            writeInt(s.length());
            out.write(s);
            out.flush();
        }
        else throw new IOException("Network::writeString() Error : Network not open");
    }
    
    public void writeInt(int n) throws IOException
    {
        if (opened)
        {
            String is;
            String zeroes = "0000000000";
            String ints;
            int    islen;
            
            is    = new Integer(n).toString();
            islen = is.length();
            ints  = zeroes.substring(0, 10-islen);
            ints  = ints.concat(is);
            
            out.write(ints);
            out.flush();
        }
        else throw new IOException("Network::writeInt() Error : Network connection is not open");
    }
    
    
    public int readInt() throws IOException
    {
        int  n;
        
        n = -1;
        if (opened)
        {
            char ci[] = new char[10];
            
            in.read(ci, 0, 10);
            n = new Integer(new String(ci)).intValue();
        }
        else throw new IOException("Network::writeInt() Error : Network connection is not open");
        
        return(n);
    }
    
    /*********************************************************\
     *             Connection open and close                 *
     \*********************************************************/
    public void open() throws IOException
    {
        if (opened) close();
        
        sock   = new Socket(servername, port);
        out    = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
        in     = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        opened = true;
    }
    
    public void close() throws IOException
    {
        if (opened)
        {
            //out.flush();
            out.close();
            in.close();
            sock.close();
            opened = false;
        }
    }
    
    public Network(String _servername, int _port)
    {
        servername = _servername;
        port       = _port;
        opened     = false;
    }
    
    public Network(Socket _sock) throws IOException
    {
        sock       = _sock;
        out        = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
        in         = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        servername = sock.getInetAddress().getHostName();
        port       = sock.getPort();
        opened     = true;
    }
    
}

