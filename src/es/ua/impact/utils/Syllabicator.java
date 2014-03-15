/** 
 * Copyright (C) 2008
 *
 * Author:
 *  Xavier Ivars i Ribes <xavi.ivars@gmail.com>
 * 
 * Based on Hyphenator class from Rafael C. Carrasco.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * Adapted from José A. Mañas in Communications of the ACM 30(7), 1987.
 */
package es.ua.impact.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that performs simple syllabication of Spanish words.
 * @author Xavier Ivars i Ribes
 * @see José A. Mañas in Communications of the ACM 30(7), 1987.
 */
public class Syllabicator {

    String v = "[aáeéiíoóuúü]";            // vowels
    String a = "[aáeéíoóú]", i = "[iuü]";  // open and closed vowels
    String h = "[h]";                      // letter h
    String c = "[bcdfghjklmnñpqrstvxyz]";  // consonants       
    String r = "[hlr]";                    // liquid and mute consonants
    String b = "[bcdfgjkmnñpqstvxyz]";     // non-liquid consonants
    // consontant pairs
    String ppl = "[bcfgkpt][l]", ppr = "[bcdfgkpt][r]", tcl = "ch|rr|ll";
    String pp = "((" + ppl + ")|(" + ppr + ")|(" + tcl + "))";
    String l = "((" + c + ")|(" + v + "))"; // any letter
    String sep; // separator
    String[] rules; // syllabication rules
    Pattern pattern;
    Matcher matcher;
    
    Pattern separator;
    Matcher counter;
    
    private final int RULE_1 = 1;
    private final int RULE_2 = 2;
    private final int RULE_3 = 3;
    private final int RULE_4 = 4;
    private final int RULE_5 = 5;
    private final int RULE_6 = 13;
    private final int RULE_7 = 18;
    private final int RULE_8 = 22;
    private final int RULE_9 = 23;

    public Syllabicator() {
        sep = "-";
        separator = Pattern.compile(sep);
        rules = new String[10];

        rules[1] = i + h + i;
        rules[2] = a + h + i;
        rules[3] = i + h + a;
        rules[4] = v + c + v;
        rules[5] = l + pp + v;
        rules[6] = pp + v;
        rules[7] = l + c + c + v;
        rules[8] = a + a;
        rules[9] = l;


        String all = "(" + rules[1] + ")|(" + rules[2] + ")|(" + rules[3] + ")|(" + rules[4] +
                ")|(" + rules[5] + ")|(" + rules[6] + ")|(" + rules[7] + ")|(" + rules[8] +
                ")|(" + rules[9] + ")";


        pattern = Pattern.compile(all, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Set separator
     * @param sep the string printed between chunks. 
     */
    public void useHyphen(String sep) {
        this.sep = sep;
        separator = Pattern.compile(sep);
    }

    /**
     * Return first matching pattern.
     */
    private int getGroup() {
        int gc = matcher.groupCount();
        for (int i = 1; i <= gc; ++i) {
            if (matcher.end() == matcher.end(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Syllabicates a word.
     * @param input the word to be syllabicated.
     * @return syllabicated word.
     */
    public String split(String input) {
        StringBuffer output = new StringBuffer(2 * input.length());
        int pos = 0;
        matcher = pattern.matcher(input);
        while (matcher.find()) {
            output.append(input.charAt(pos));
            switch (getGroup()) {
                case RULE_7:
                    output.append(input.charAt(++pos));
                case RULE_4:
                case RULE_5:
                case RULE_8:
                    output.append(sep);
                    break;
                default:
                    break;
            }
            matcher.region(++pos, matcher.regionEnd());
        }
        return output.toString();
    }

    public String show(String input) {
        StringBuffer output = new StringBuffer(2 * input.length());
        int pos = 0;
        matcher = pattern.matcher(input);
        while (matcher.find()) {

            System.out.println(matcher.groupCount());
            System.out.println(matcher.group());
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                System.out.println("GROUP " + i + ": " + matcher.group(i));
            }
        }
        return output.toString();
    }
    
    public int countSyllables(String word) {
        if(word==null || word.equalsIgnoreCase(""))
            return 0;
        
        int total=1;
        boolean after=false;
        char s = sep.charAt(0);
        for(char c:word.toCharArray()) {
            if(c == s)
                after=true;
            if(after==true) {
                ++total;
                after=false;
            }
        }
        
        return total;
    }

    public String getSyl(String word, int start, int end) {
        StringBuffer output = new StringBuffer(word.length());
        
        int added = 0;
        int pos = 0;
        char separator = sep.charAt(0);
        char c;
        while(added < start) {
            try {
                c = word.charAt(pos);
            } catch (IndexOutOfBoundsException e) {
                return "";
            }
            if(c==separator)
                ++added;
            ++pos;
        }
        
        while(added <= end) {
            try {
                c = word.charAt(pos);
            } catch (IndexOutOfBoundsException e) {
                return output.toString();
            }
            if(c==separator)
            {
                ++added;
                if(added > end)
                    break;
            }
            ++pos;
            output.append(c);
        }
        
        return output.toString();
    }
    
    public String getSyl(String word,int n) {
        return getSyl(word,n,n);
    }
    
    public String getLastSyl(String word) {
        return getSyl(word,countSyllables(word)-1);
    }
    
    public String replaceLastSyl(String word,String s) {
        return replaceSyl(word,s,countSyllables(word)-1);
    }
    
    public String replaceSyl(String w, String s, int n) {
        String [] r = w.split(this.sep);
        StringBuilder ret = new StringBuilder();
        
        if(r.length>n) {
            r[n] = s;
        }
        
        for(int i = 0; i < r.length; i++) {
            if(i!=0) {
                ret.append(sep);
            }
            ret.append(r[i]);
        }
        
        return ret.toString();
    }
    
    public static void main(String[] args) {
        Syllabicator syl = new Syllabicator();

        if (args.length > 0) {

            for (int k = 0; k < args.length; ++k) {
                if (args[k].equals("-h")) {
                    syl.useHyphen(args[++k]);
                } else {
                    String word = args[k];
                    if (word.length() > 0) {
                        String spl=syl.split(word);
                        System.out.println("WORD: "+word);
                        System.out.println("SPLIT: "+spl);
                        int sl = syl.countSyllables(spl);
                        System.out.println("TOTAL: "+sl);
                        System.out.println("FIRST SYL: "+syl.getSyl(spl, 0));
                        System.out.println("LAST SYL: "+syl.getSyl(spl,sl-1));
                    }
                }
            }
        }
    }
}
