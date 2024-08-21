/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                      Web Spider                       *
 *                                                       *
 *  January 2005                                         *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2005 Shaman Research                   *
\*********************************************************/
package org.shaman.spider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.shaman.exceptions.ShamanException;
import org.shaman.util.FileUtil;


// **********************************************************\
// *             Brute Force HTTP Web Spider                *
// **********************************************************/
public class Spider
{
    final String host     = "news.bbc.co.uk";
    final String basedir  = "/1/hi/world/";
    final String basepage = "default.stm";
    final int    pagenum  = 100;
    final String pagedir  = "c:/Projects/data/bbcnews/world/";
    
    // **********************************************************\
    // *           Leach all URLs in the given file             *
    // **********************************************************/
    private void crawl() throws ShamanException
    {
        HttpClient client;
        Set        pageset;
        String     page;
        Set        linkset, newlinks, loopset;
        Iterator   itlink, itnew;
        String     link, newlink;
        URL        url;
        
        // Make HTTP client for given host
        client = new HttpClient(new MultiThreadedHttpConnectionManager());
        client.getHostConfiguration().setHost(host, 80, "http");
        
        // Read the base page
        page    = readPage(client, makePageUrl(this.basedir+this.basepage));
        pageset = extractLinks(page);
        
        // Recursively find links in the basedir starting from the basepage
        loopset = new HashSet();
        loopset.add(this.basepage);
        while((pageset.size() < this.pagenum) && (loopset.size() > 0))
        {
            linkset = new HashSet(pageset);
            loopset = new HashSet();
            
            itlink  = linkset.iterator();
            while(itlink.hasNext() && (pageset.size() < this.pagenum))
            {
                link     = (String)itlink.next();
                url      = makePageUrl(link);
                page     = readPage(client, url);
                newlinks = extractLinks(page);
                itnew    = newlinks.iterator();
                while(itnew.hasNext() && (pageset.size() < this.pagenum))
                {
                    newlink = (String)itnew.next();
                    if (!pageset.contains(newlink))
                    {
                        pageset.add(newlink);
                        loopset.add(newlink);
                        System.err.println("Found link "+pageset.size()+" = "+newlink);
                    }
                }
            }
        }
        
        try
        {
            // Read and store these pages
            File   pfile, pdir;
            String pagefile;
            int    cnt;
            
            cnt    = 0;
            pdir   = new File(this.pagedir);
            itlink = pageset.iterator();
            while(itlink.hasNext())
            {
                link     = (String)itlink.next();
                System.err.println("Saving "+cnt+"/"+this.pagenum+" "+link);
                try
                {
                    page     = readPage(client, makePageUrl(link));
                    pagefile = link.substring(link.lastIndexOf("/")+1);
                    pfile    = new File(pdir, pagefile);
                    FileUtil.writeStringToTextFile(pfile, page);
                    cnt++;
                }
                catch(ShamanException ex) { System.err.println("***FAILED****"); }
            }
        }
        catch(IOException ex) { throw new ShamanException(ex); }
    }
    
    private URL makePageUrl(String pagefile) throws ShamanException
    {
        URL purl;
        
        try
        {
            purl = new URL("http://"+host+pagefile);
        }
        catch(MalformedURLException ex) { throw new ShamanException(ex); }
        
        return(purl);
    }
    
    private String readPage(HttpClient client, URL url) throws ShamanException
    {
        String    page;
        
        page = null;
        try
        {
            GetMethod gm;
        
            // Do a HTTP GET to receive the page on the given URL
            gm = new GetMethod(url.getPath());
            gm.setFollowRedirects(true);
            headersIE6(url, gm);
            client.executeMethod(gm);
            
            if (gm.getStatusCode() == 200)
            {
                // Read HTML body
                InputStream     isres = gm.getResponseBodyAsStream();
                BufferedReader bufres = new BufferedReader(new InputStreamReader(isres));
                StringBuffer   sbhtml = new StringBuffer();
                String         lnow;
                lnow = bufres.readLine();
                while(lnow != null)
                {
                    sbhtml.append(lnow+"\n");
                    lnow = bufres.readLine();
                }
                page = sbhtml.toString();
            }
            else throw new ShamanException("HTTP Status not OK (200) but "+gm.getStatusCode()+" for "+url);
        }
        catch(HttpException ex) { throw new ShamanException(ex); }
        catch(IOException ex)   { throw new ShamanException(ex); }
        
        return(page);
    }
    
    private Set extractLinks(String page)
    {
        Set    linkset;
        int    posbeg, posend;
        String link;
        
        // Find all links to pages under the basedir
        linkset = new HashSet();
        posbeg  = 0;
        posbeg  = page.indexOf("href=\"", posbeg);
        while (posbeg != -1)
        {
            posbeg += 6;
            posend  = page.indexOf('"', posbeg);
            link    = page.substring(posbeg, posend);
            if (link.startsWith(this.basedir)) linkset.add(link);
            posbeg = page.indexOf("href=\"", posbeg);
        }
        
        return(linkset);
    }
    
    private void headersIE6(URL url, GetMethod gm)
    {
        gm.addRequestHeader("Accept", "*/*");
        gm.addRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; Q312461)");
        gm.addRequestHeader("Host", url.getHost());
    }
    
    // **********************************************************\
    // *                      Construction                      *
    // **********************************************************/
    private static Spider instance;
    public Spider() { }
    
    public static void main(String[] args)
    {
        instance = new Spider();
        try
        {
            instance.crawl();
        }
        catch(Exception ex) { ex.printStackTrace(); }
    }
}