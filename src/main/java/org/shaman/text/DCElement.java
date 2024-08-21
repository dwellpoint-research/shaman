package org.shaman.text;

/**
 * <h2>Dublin Core Meta-Data Extraction</h2>
 */
public class DCElement
{
    public String name;
    public String type;
    public String scheme;
    public String language;
    public String value;
    
    public DCElement(String n, String t, String s, String l, String v) {
        this.name = n.toLowerCase();
        this.type = t;
        this.scheme = s;
        this.language = l;
        this.value = v;
    }
    public DCElement(String n, String t, String v) {
        this.name = n.toLowerCase();
        this.type = t;
        this.scheme = "";
        this.language = "";
        this.value = v;
    }
    public DCElement(String n, String v) {
        this.name = n.toLowerCase();
        this.type = "";
        this.scheme = "";
        this.language = "";
        this.value = v;
    }
    
    public String toHTML() {
        if (this.value.equals(""))
            return("");
        
        StringBuffer s = new StringBuffer("");
        s.append("<META NAME=\"DC."+this.name);
        if (! this.type.equals("")) {
            s.append("."+this.type);
        }
        s.append("\"");
        if (! this.scheme.equals("")) {
            s.append(" SCHEME=\""+this.scheme+"\"");
        }
        if (! this.language.equals("")) {
            s.append(" LANGUAGE=\""+this.language+"\"");
        }
        s.append(" CONTENT=\""+this.value+"\"");
        s.append(">\n");
        return(s.toString());
    }
    
    public String toRDF() {
        if (this.value.equals(""))
            return("");
        
        StringBuffer s = new StringBuffer("");
        s.append("  <dc:" + this.name);
        if (! this.type.equals("")) {
            //s.append(">\n   <" + this.type.toLowerCase() + ">\n");
            s.append(" TYPE=\""+this.type+"\"");
        }
        if (! this.scheme.equals("")) {
            s.append(" SCHEME=\""+this.scheme+"\"");
        }
        if (! this.language.equals("")) {
            s.append(" LANGUAGE=\""+this.language+"\"");
        }
        s.append(">\n");
        s.append("    " + this.value + "\n");
        //if (! this.type.equals("")) {
        //    s.append("   </" + this.type.toLowerCase() + ">\n");
        //}
        s.append("  </dc:" + this.name + ">\n");
        return(s.toString());
    }
    
    public String toSOIF() {
        boolean b = false;
        
        if (this.value.equals(""))
            return("");
        
        StringBuffer s = new StringBuffer("");
        StringBuffer v = new StringBuffer("");
        s.append("DC." + this.name);
        if (! this.type.equals("")) {
            s.append("." + this.type);
        }
        
        if (! this.scheme.equals("")) {
            v.append("(SCHEME="+this.scheme+")");
            b = true;
        }
        if (! this.language.equals("")) {
            v.append("(LANGUAGE="+this.language+")");
            b = true;
        }
        if (b) {
            v.append(" ");
        }
        v.append(this.value+"\n");
        int i = v.length();
        s.append("{" + i + "}: " + v.toString());
        return(s.toString());
    }
}
