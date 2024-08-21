package org.shaman.rtf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;

public class PictureExtract
{
    public static void main(String []args)
    {
        try
        {
            LineNumberReader frtf;
            StringBuffer     ptxt;
            String           pline;
            
            frtf = new LineNumberReader(new FileReader(new File("/users/johankaers/softdev/temp/pictdata.txt")));
            ptxt = new StringBuffer();
            pline = frtf.readLine();
            while(pline != null)
            {
                ptxt.append(pline);
                pline = frtf.readLine();
            }
            
            byte []out = new byte[ptxt.length()/2];
            String bhex;
            short  bbin;
            int    i, pos;
            
            pos = 0;
            for (i=0; i<ptxt.length(); i+=2)
            {
                bhex = ptxt.substring(i, i+2);
                bbin = Short.decode("0x"+bhex);
                out[pos++] = (byte)bbin;
            }
            
            FileOutputStream fout = new FileOutputStream(new File("/users/johankaers/softdev/temp/figure.pict"));
            fout.write(out);
            fout.flush();
            fout.close();
            
        }
        catch(Exception ex){ ex.printStackTrace(); }
    }

}
