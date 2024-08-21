/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *                                                       *
 *                                                       *
 *                                                       *
 *  by Johan Kaers (johankaers@gmail.com)                *
 *                                                       * 
 *  Copyright (c) 2001-6 Shaman Research                 *
\*********************************************************/
package org.shaman.text;

/**
 * <h2>Porter Stemmer</h2>
 * Porter Stemming Algorithm for English text.
 */
public class Porter implements Stemmer {
    
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
        str = Clean(str);
        
        if ((str != "") && (str.length() > 2)) {
            str = stripPrefixes(str);
            
            if (str != "") {
                str = stripSuffixes(str);
            }
            
        }
        
        return str;
    }
    
    
    /**
     * Description of the method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String Clean(String str) {
        int last = str.length();
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
     * @param stem  Description of Parameter
     * @return      Description of the Returned Value
     */
    private int measure(String stem) {
        
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
     * Description of the Method
     *
     * @param word  Description of Parameter
     * @return      Description of the Returned Value
     */
    private boolean containsVowel(String word) {
        
        for (int i = 0; i < word.length(); i++) {
            if (i > 0) {
                if (vowel(word.charAt(i), word.charAt(i - 1))) {
                    return true;
                }
            }
            else {
                if (vowel(word.charAt(0), 'a')) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private boolean cvc(String str) {
        int length = str.length();
        
        if (length < 3) {
            return false;
        }
        
        if ((!vowel(str.charAt(length - 1), str.charAt(length - 2)))
                && (str.charAt(length - 1) != 'w') && (str.charAt(length - 1) != 'x') && (str.charAt(length - 1) != 'y')
                && (vowel(str.charAt(length - 2), str.charAt(length - 3)))) {
            
            if (length == 3) {
                if (!vowel(str.charAt(0), '?')) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                if (!vowel(str.charAt(length - 3), str.charAt(length - 4))) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        return false;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step1(String str) {
        
        NewString stem = new NewString();
        
        if (str.charAt(str.length() - 1) == 's') {
            if ((hasSuffix(str, "sses", stem)) || (hasSuffix(str, "ies", stem))) {
                String tmp = "";
                for (int i = 0; i < str.length() - 2; i++) {
                    tmp += str.charAt(i);
                }
                str = tmp;
            }
            else {
                if ((str.length() == 1) && (str.charAt(str.length() - 1) == 's')) {
                    str = "";
                    return str;
                }
                if (str.charAt(str.length() - 2) != 's') {
                    String tmp = "";
                    for (int i = 0; i < str.length() - 1; i++) {
                        tmp += str.charAt(i);
                    }
                    str = tmp;
                }
            }
        }
        
        if (hasSuffix(str, "eed", stem)) {
            if (measure(stem.str) > 0) {
                String tmp = "";
                for (int i = 0; i < str.length() - 1; i++) {
                    tmp += str.charAt(i);
                }
                str = tmp;
            }
        }
        else {
            if ((hasSuffix(str, "ed", stem)) || (hasSuffix(str, "ing", stem))) {
                if (containsVowel(stem.str)) {
                    
                    String tmp = "";
                    for (int i = 0; i < stem.str.length(); i++) {
                        tmp += str.charAt(i);
                    }
                    str = tmp;
                    if (str.length() == 1) {
                        return str;
                    }
                    
                    if ((hasSuffix(str, "at", stem)) || (hasSuffix(str, "bl", stem)) || (hasSuffix(str, "iz", stem))) {
                        str += "e";
                        
                    }
                    else {
                        int length = str.length();
                        if ((str.charAt(length - 1) == str.charAt(length - 2))
                                && (str.charAt(length - 1) != 'l') && (str.charAt(length - 1) != 's') && (str.charAt(length - 1) != 'z')) {
                            
                            tmp = "";
                            for (int i = 0; i < str.length() - 1; i++) {
                                tmp += str.charAt(i);
                            }
                            str = tmp;
                        }
                        else
                            if (measure(str) == 1) {
                                if (cvc(str)) {
                                    str += "e";
                                }
                            }
                    }
                }
            }
        }
        
        if (hasSuffix(str, "y", stem)) {
            if (containsVowel(stem.str)) {
                String tmp = "";
                for (int i = 0; i < str.length() - 1; i++) {
                    tmp += str.charAt(i);
                }
                str = tmp + "i";
            }
        }
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String step2(String str) {
        
        String[][] suffixes = {{"ational", "ate"},
                {"tional", "tion"},
                {"enci", "ence"},
                {"anci", "ance"},
                {"izer", "ize"},
                {"iser", "ize"},
                {"abli", "able"},
                {"alli", "al"},
                {"entli", "ent"},
                {"eli", "e"},
                {"ousli", "ous"},
                {"ization", "ize"},
                {"isation", "ize"},
                {"ation", "ate"},
                {"ator", "ate"},
                {"alism", "al"},
                {"iveness", "ive"},
                {"fulness", "ful"},
                {"ousness", "ous"},
                {"aliti", "al"},
                {"iviti", "ive"},
                {"biliti", "ble"}};
        NewString stem = new NewString();
        
        for (int index = 0; index < suffixes.length; index++) {
            if (hasSuffix(str, suffixes[index][0], stem)) {
                if (measure(stem.str) > 0) {
                    str = stem.str + suffixes[index][1];
                    return str;
                }
            }
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
        
        String[][] suffixes = {{"icate", "ic"},
                {"ative", ""},
                {"alize", "al"},
                {"alise", "al"},
                {"iciti", "ic"},
                {"ical", "ic"},
                {"ful", ""},
                {"ness", ""}};
        NewString stem = new NewString();
        
        for (int index = 0; index < suffixes.length; index++) {
            if (hasSuffix(str, suffixes[index][0], stem)) {
                if (measure(stem.str) > 0) {
                    str = stem.str + suffixes[index][1];
                    return str;
                }
            }
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
        
        String[] suffixes = {"al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment", "ent", "sion", "tion",
                "ou", "ism", "ate", "iti", "ous", "ive", "ize", "ise"};
        
        NewString stem = new NewString();
        
        for (int index = 0; index < suffixes.length; index++) {
            if (hasSuffix(str, suffixes[index], stem)) {
                
                if (measure(stem.str) > 1) {
                    str = stem.str;
                    return str;
                }
            }
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
        
        if (str.charAt(str.length() - 1) == 'e') {
            if (measure(str) > 1) {
                /*
                 *  measure(str)==measure(stem) if ends in vowel
                 */
                String tmp = "";
                for (int i = 0; i < str.length() - 1; i++) {
                    tmp += str.charAt(i);
                }
                str = tmp;
            }
            else
                if (measure(str) == 1) {
                    String stem = "";
                    for (int i = 0; i < str.length() - 1; i++) {
                        stem += str.charAt(i);
                    }
                    
                    if (!cvc(stem)) {
                        str = stem;
                    }
                }
        }
        
        if (str.length() == 1) {
            return str;
        }
        if ((str.charAt(str.length() - 1) == 'l') && (str.charAt(str.length() - 2) == 'l') && (measure(str) > 1)) {
            if (measure(str) > 1) {
                /*
                 *  measure(str)==measure(stem) if ends in vowel
                 */
                String tmp = "";
                for (int i = 0; i < str.length() - 1; i++) {
                    tmp += str.charAt(i);
                }
                str = tmp;
            }
        }
        return str;
    }
    
    
    /**
     * Description of the Method
     *
     * @param str  Description of Parameter
     * @return     Description of the Returned Value
     */
    private String stripPrefixes(String str) {
        
        String[] prefixes = {"kilo", "micro", "milli", "intra", "ultra", "mega", "nano", "pico", "pseudo"};
        
        int last = prefixes.length;
        for (int i = 0; i < last; i++) {
            if (str.startsWith(prefixes[i])) {
                String temp = "";
                for (int j = 0; j < str.length() - prefixes[i].length(); j++) {
                    temp += str.charAt(j + prefixes[i].length());
                }
                return temp;
            }
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

/**
 * Description of the Class
 *
 * @author    adhaene
 * @created   July 4, 2001
 */
class NewString {
    
    /**
     * Description of the Field
     */
    public String str;
    
    
    /**
     * Constructor for the NewString object
     */
    NewString() {
        str = "";
    }
}

