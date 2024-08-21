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

/**
 * <h2>Dutch Stemmer</h2>
 * <br>
 * The Porter Stemming algorithm for Dutch words.

 * @created   July 4, 2001
 */
public class DutchPorter implements Stemmer {
    
    /**
     * Returns the stem of a given word
     *
     * @param word  word to be stemmed
     * @return      stem
     */
    public String stem(String word) {
        return stripAffixes(word);
    }
    
    
    /**
     * returns the given word without the affixes
     *
     * @param str  word to be stripped
     * @return     the word without affixes
     */
    public String stripAffixes(String str) {
        
        str = str.toLowerCase();
        if (str.length() > 0) str = Clean(str);
        
        if ((str != "") && (str.length() > 2)) {
            str = stripSuffixes(str);
        }
        
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param stem  Description of Parameter
     * @return      Description of the Returned Value
     */
    public int measure(String stem) {
        
        int i = 0;
        
        int count = 0;
        int length = stem.length();
        
        while (i < length) {
            for (; i < length; i++) {
                if (i > 0) {
                    if (vowel(stem.charAt(i), stem.charAt(i - 1))) {
                        break;
                    }
                }
                else {
                    if (vowel(stem.charAt(i), 'a')) {
                        break;
                    }
                }
            }
            
            for (i++; i < length; i++) {
                if (i > 0) {
                    if (!vowel(stem.charAt(i), stem.charAt(i - 1))) {
                        break;
                    }
                }
                else {
                    if (!vowel(stem.charAt(i), '?')) {
                        break;
                    }
                }
            }
            if (i < length) {
                count++;
                i++;
            }
        }
        
        return (count);
    }
    
    
    /**
     * Description of the method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String Clean(String str) {
        int last = str.length();
        //Character ch = new Character(str.charAt(0));
        String temp = "";
        
        for (int i = 0; i < last; i++) {
            if (Character.isLetterOrDigit(str.charAt(i))) {
                temp += str.charAt(i);
            }
        }
        return temp;
    }
    
    /**
     * Description of the Method
     *
     * @param word    Description of Parameter
     * @param suffix  Description of Parameter
     * @param stem    Description of Parameter
     * @return        Description of the Returned Value
     */
    private boolean hasSuffix(String word, String suffix, NewString stem) {
        
        String tmp = "";
        
        if (word.length() <= suffix.length()) {
            return false;
        }
        if (suffix.length() > 1) {
            if (word.charAt(word.length() - 2) != suffix.charAt(suffix.length() - 2)) {
                return false;
            }
        }
        
        stem.str = "";
        
        for (int i = 0; i < word.length() - suffix.length(); i++) {
            stem.str += word.charAt(i);
        }
        tmp = stem.str;
        
        for (int i = 0; i < suffix.length(); i++) {
            tmp += suffix.charAt(i);
        }
        
        if (tmp.compareTo(word) == 0) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
    /**
     * Description of the Method
     *
     * @param ch    Description of Parameter
     * @param prev  Description of Parameter
     * @return      Description of the Returned Value
     */
    private boolean vowel(char ch, char prev) {
        switch (ch) {
        case 'a':
        case 'e':
        case 'i':
        case 'o':
        case 'u':
            return true;
        case 'y':
        {
            
            switch (prev) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            default:
                return true;
            }
        }
        
        default:
            return false;
        }
    }
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step1(String str) {
        
        NewString stem = new NewString();
        int i = 0;
        
        
        if (hasSuffix(str, "en", stem) &&
                (measure(str.substring(0, (str.length() - 2))) > 1) &&
                (!vowel(str.charAt(str.length() - 3), str.charAt(str.length() - 4)))
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
            // En dan nog de DupV
            str = dupV(str);
        }
        
        if (hasSuffix(str, "e", stem) &&
                (measure(str.substring(0, (str.length() - 1))) > 1) &&
                (!vowel(str.charAt(str.length() - 2), str.charAt(str.length() - 3)))
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 1; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
            // En dan nog de DupV
            str = dupV(str);
        }
        
        return str;
    }
    
    private String dupV(String str) {
        
        //String suff = "";
        String tmp = str;
        int i;
        
        if ((str.charAt(str.length() - 2) == 'o') &&
                (str.charAt(str.length() - 1) == 'z')
        ) {
            tmp = "";
            //suff = "eid";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            tmp = tmp.concat("oos");
            return tmp;
        }
        if ((str.charAt(str.length() - 2) == 'e') &&
                (str.charAt(str.length() - 1) == 'd')
        ) {
            tmp = "";
            //suff = "eid";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            tmp = tmp.concat("eid");
            
            return tmp;
        }
        else {
            if (vowel(str.charAt(str.length() - 2), str.charAt(str.length() - 3)) &&
                    !vowel(str.charAt(str.length() - 1), str.charAt(str.length() - 2))
            ) {
                tmp = "";
                for (i = 0; i < str.length() - 1; i++) {
                    tmp += str.charAt(i);
                }
                tmp += str.charAt(str.length() - 2);
                tmp += str.charAt(str.length() - 1);
                
            }
            return tmp;
        }
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step2(String str) {
        
        NewString stem = new NewString();
        int i = 0;
        
        if ((hasSuffix(str, "etj", stem)) &&
                (!vowel(str.charAt(str.length() - 4), str.charAt(str.length() - 5)))
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 3; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
        }
        
        if (hasSuffix(str, "tj", stem)) {
            String tmp = "";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
        }
        
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step3(String str) {
        
        NewString stem = new NewString();
        int i = 0;
        
        if ((hasSuffix(str, "heid", stem)) &&
                (measure(str.substring(0, str.length() - 4)) > 1)
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 4; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
        }
        
        if ((hasSuffix(str, "ing", stem)) &&
                (measure(str.substring(0, str.length() - 3)) > 1)
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 3; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
            // En dan nog de DupV
            str = dupV(str);
        }
        
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step4(String str) {
        
        NewString stem = new NewString();
        int i = 0;
        
        if ((hasSuffix(str, "baar", stem)) &&
                (measure(str.substring(0, str.length() - 3)) > 1)
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 4; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
        }
        
        if ((hasSuffix(str, "ig", stem)) &&
                (measure(str.substring(0, str.length() - 2)) > 1)
        ) {
            String tmp = "";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            str = tmp;
            // En dan nog de DupV
            str = dupV(str);
        }
        
        
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step5(String str) {
        // Tidy up
        
        NewString stem = new NewString();
        int i = 0;
        
        
        if (hasSuffix(str, "v", stem)) {
            String tmp = "";
            for (i = 0; i < str.length() - 1; i++) {
                tmp += str.charAt(i);
            }
            tmp += "f";
            str = tmp;
        }
        
        if (hasSuffix(str, "pp", stem)) {
            String tmp = "";
            for (i = 0; i < str.length() - 2; i++) {
                tmp += str.charAt(i);
            }
            tmp += "v";
            tmp += "v";
            str = tmp;
        }
        
        return str;
    }
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String stripSuffixes(String str) {
        
        str = step1(str);
        if (str.length() >= 1) {
            str = step2(str);
        }
        if (str.length() >= 1) {
            str = step3(str);
        }
        if (str.length() >= 1) {
            str = step4(str);
        }
        if (str.length() >= 1) {
            str = step5(str);
        }
        
        return str;
    }
    
}

