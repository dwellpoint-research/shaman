/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *                                                       * 
 *  Copyright (c) 2001-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * <h2>Dublin Core Meta-Data Extraction</h2>
 */
public class DC extends Hashtable
{
    private static final long serialVersionUID = 1L;
    
    // Dublin Core elements
    public static final String TITLE = "title";
    public static final String SUBJECT = "subject";
    public static final String DESCRIPTION = "description";
    public static final String CREATOR = "creator";
    public static final String PUBLISHER = "publisher";
    public static final String CONTRIBUTOR = "contributor";
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    public static final String IDENTIFIER = "identifier";
    public static final String SOURCE = "source";
    public static final String LANGUAGE = "language";
    public static final String RELATION = "relation";
    public static final String COVERAGE = "coverage";
    public static final String RIGHTS = "rights";
    
    // Output formats
    public final static String HTML = "HTML";
    //    public final static String HTML32 = "HTML3.2 (strict)";
    //    public final static String XML = "XML";
    public final static String RDF = "RDF";
    public final static String SOIF = "SOIF";
    
    // SCHEMEs...
    //
    // TITLE_SCHEMES
    // CREATOR_SCHEMES
    final static String[] SUBJECT_SCHEMES = {
            "Keywords (default)",
            "LCSH",
            "MeSH",
            "AAT",
            "DDC",
            "LCC",
            "NLM",
            "UDC"
    };
    final static String[] DESCRIPTION_SCHEMES = {
            "Abstract (default)",
            "URI"
    };
    // PUBLISHER_SCHEMES
    // CONTRIBUTOR_SCHEMES
    // DATE_SCHEMES
    // TYPE_SCHEMES
    final static String[] FORMAT_SCHEMES = {
            "Free text (default)",
            "IMT"
    };
    final static String[] IDENTIFIER_SCHEMES = {
            "URI (default)",
            "ISBN",
            "ISSN",
            "SICI",
            "FPI",
            "DOI"
    };
    final static String[] SOURCE_SCHEMES = {
            "Free text (default)",
            "URI",
            "ISBN",
            "ISSN",
            "SICI",
            "FPI",
            "DOI"
    };
    final static String[] LANGUAGE_SCHEMES = {
            "Free text (default)",
            "Z39.53",
            "ISO639-1",
            "RFC-1766"
    };
    final static String[] RELATION_SCHEMES = {
            "Free text (default)",
            "URI",
            "ISBN",
            "ISSN",
            "SICI",
            "FPI",
            "DOI"
    };
    final static String[] RIGHTS_SCHEMES = {
            "Free text (default)",
            "URI"
    };
    
    public DC() {
        this.put(TITLE, new Vector());
        this.put(SUBJECT, new Vector());
        this.put(DESCRIPTION, new Vector());
        this.put(CREATOR, new Vector());
        this.put(PUBLISHER, new Vector());
        this.put(CONTRIBUTOR, new Vector());
        this.put(DATE, new Vector());
        this.put(TYPE, new Vector());
        this.put(FORMAT, new Vector());
        this.put(IDENTIFIER, new Vector());
        this.put(SOURCE, new Vector());
        this.put(LANGUAGE, new Vector());
        this.put(RELATION, new Vector());
        this.put(COVERAGE, new Vector());
        this.put(RIGHTS, new Vector());
    }
    
    // Add a new element.
    public void add(DCElement e) {
        Vector v = (Vector)this.get(e.name);
        if (v != (Vector)null)
            v.addElement(e);
        //this.put(e.name, v);
    }
    
    // How many elements named 'name'.
    public int size(String name) {
        Vector v = (Vector)this.get(name);
        if (v != (Vector)null)
            return(v.size());
        else
            return(0);
    }
    
    // Return first element named 'name'.
    public DCElement getFirstElement(String name) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0) {
            return((DCElement)null);
        }
        return((DCElement)v.firstElement());
    }
    
    // As above but returns element value.
    public String getFirstElementValue(String name) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0)
            return(null);
        return(((DCElement)v.firstElement()).value);
    }
    
    public String getFirstElementValue(String name, String type, String scheme, String language) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0)
            return(null);
        Enumeration ev = v.elements();
        while (ev.hasMoreElements()) {
            DCElement e = (DCElement) ev.nextElement();
            if (e.type.equals(type) &&
                    e.scheme.equals(scheme) &&
                    e.language.equals(language))
                return(e.value);
        }
        return(null);
    }
    
    public String getFirstElementScheme(String name) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0)
            return(null);
        return(((DCElement)v.firstElement()).scheme);
    }
    
    // Sets value for first element named name.  Creates element if it doesn't
    // exist.
    public void setFirstElementValue(String name, String value) {
        Vector v = (Vector)this.get(name);
        DCElement e;
        if (v == (Vector)null)
            return;
        if (v.size() == 0) {
            add(new DCElement(name, value));
        }
        else {
            e = (DCElement)v.firstElement();
            e.value = value;
        }
    }
    public void setFirstElementValue(String name, String type, String scheme, String language, String value) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null)
            return;
        Enumeration ev = v.elements();
        while (ev.hasMoreElements()) {
            DCElement e = (DCElement) ev.nextElement();
            if (e.type.equals(type) &&
                    e.scheme.equals(scheme) &&
                    e.language.equals(language)) {
                e.value = value;
                return;
            }
        }
        add(new DCElement(name, type, scheme, language, value));
    }
    
    // Sets scheme for first element named name.  Creates element if it doesn't
    // exist.
    public void setFirstElementScheme(String name, String scheme) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0)
            add(new DCElement(name, "", scheme, "", ""));
        else {
            DCElement e = (DCElement)v.firstElement();
            e.scheme = scheme;
        }
    }
    
    // Returns enumerated list of Elements for element named name.
    public Enumeration elements(String name) {
        Vector v = (Vector)this.get(name);
        if (v == (Vector)null || v.size() == 0) {
            return((Enumeration)null);
        }
        return(v.elements());
    }
    
    // SCHEME as META tag attribute
    public String toHTML() {
        StringBuffer s = new StringBuffer("");
        s.append("<LINK REL=\"schema.DC\" HREF=\"http://purl.org/dc\">\n");
        Enumeration ee = this.elements();
        while (ee.hasMoreElements()) {
            Enumeration ev = ((Vector)ee.nextElement()).elements();
            while (ev.hasMoreElements()) {
                DCElement e = (DCElement) ev.nextElement();
                s.append(e.toHTML());
            }
        }
        return(s.toString());
    }
    
    // RDF
    public String toRDF() {
        StringBuffer s = new StringBuffer("");
        s.append("<?xml version=\"1.0\"?>\n");
        s.append("<rdf:RDF\n");
        s.append(" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        s.append(" xmlns:dc=\"http://purl.org/dc/elements/1.0/\">\n");
        String i = getFirstElementValue(DC.IDENTIFIER);
        String about;
        if (i == null) {
            about = "";
        }
        else {
            about = " about=\"" + i + "\"";
        }
        s.append(" <rdf:Description" + about + ">\n");
        Enumeration ee = this.elements();
        while (ee.hasMoreElements()) {
            Enumeration ev = ((Vector)ee.nextElement()).elements();
            while (ev.hasMoreElements()) {
                DCElement e = (DCElement) ev.nextElement();
                if (e.name == DC.IDENTIFIER && e.value == i) continue;
                s.append(e.toRDF());
            }
        }
        s.append(" </rdf:Description>\n");
        s.append("</rdf:RDF>\n");
        return(s.toString());
    }
    
    // SOIF
    public String toSOIF() {
        StringBuffer s = new StringBuffer("");
        String i = getFirstElementValue(DC.IDENTIFIER);
        if (i == null) {
            i = "";
        }
        s.append("@FILE { " + i + "\n");
        Enumeration ee = this.elements();
        while (ee.hasMoreElements()) {
            Enumeration ev = ((Vector)ee.nextElement()).elements();
            while (ev.hasMoreElements()) {
                DCElement e = (DCElement) ev.nextElement();
                s.append(e.toSOIF());
            }
        }
        s.append("}\n");
        return(s.toString());
    }
}
