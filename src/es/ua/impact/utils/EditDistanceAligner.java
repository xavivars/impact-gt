/** 
 * Copyright (C) 2012
 *
 * Author:
 *  Xavier Ivars i Ribes <xavi.ivars@gmail.com>
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
 */
package es.ua.impact.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class EditDistanceAligner {

    private static HashMap<String, AlignmentData> alignments = new HashMap<String, AlignmentData>();
    
    static public String getKey(String w1,String w2) {
        w1 = "#" + w1.replaceAll(" ", "#") + "#";
        w2 = "#" + w2.replaceAll(" ", "#") + "#";
        
        return w1+"|"+w2;
    }
    
    static public int getEditDistance(String w1,String w2) {
        int ret;
        
        String w = getKey(w1, w2);
        
        if(alignments.containsKey(w)) {
            ret = alignments.get(w).distance;
        } else {
            int [] alignment = new int[w1.length()];
            ret = EditDistanceAligner.editDistance(Arrays.asList(ArrayUtils.toObject(w1.toCharArray())),
                Arrays.asList(ArrayUtils.toObject(w2.toCharArray())),alignment);
            alignments.put(w,new AlignmentData(w, alignment, ret));
        }
        
        return ret;
    }
    
    static public int [] getAlignment(String w1, String w2) {
        int [] ret;
        
        String w = getKey(w1, w2);
        
        if(alignments.containsKey(w)) {
            ret = alignments.get(w).alignment;
        } else {
            ret = new int[w2.length()];
            int d = EditDistanceAligner.editDistance(Arrays.asList(ArrayUtils.toObject(w1.toCharArray())),
                Arrays.asList(ArrayUtils.toObject(w2.toCharArray())),ret);
            alignments.put(w,new AlignmentData(w, ret, d));
        }
        
        return ret;
    }
    
    static public int editDistance(List<Character> s,List<Character> t,int [] alignment) {
        
        int m = s.size(); // length of s
        int n = t.size(); // length of t
        short[][] table = new short[m + 1][n + 1];
        short[] d = null;
        short[] p = null;
        
        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }
        
        short[] swap; // placeholder to assist in swapping p and d

        // indexes into strings s and t
        short i; // iterates through s
        short j; // iterates through t
        
        Character s_i = null; // ith object of s

        short cost; // cost
        
        p = table[0];
        for (j = 0; j <= n; j++) {
            p[j] = j;
        }
        
        for (i = 1; i <= m; i++) {
            d = table[i];
            s_i = s.get(i - 1);
            d[0] = i;

            Character t_j = null; // ith object of s
            for (j = 1; j <= n; j++) {
                t_j = t.get(j - 1);
                cost = s_i.equals(t_j) ? (short) 0 : (short) 1;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[j] = minimum(d[j - 1] + 1, p[j] + 1, p[j - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            p = d;
        }
        
        if(alignment!=null){
            i = (short) s.size();
            j = (short) t.size();
            
            if(s.get(i-1).equals(t.get(j-1)))
                alignment[i-1]=j-1;
            else
                alignment[i-1]=-1;
            while(j>1){
                if(table[i-1][j-1]<table[i-1][j]){
                    if(table[i-1][j-1]<table[i][j-1]){
                        i--;
                        j--;
                        if(s.get(i-1).equals(t.get(j-1)))
                            alignment[i-1]=j-1;
                        else
                            alignment[i-1]=-1;
                    }
                    else{
                        j--;
                        /*if(s.get(i-1).equals(t.get(j-1)))
                            alignment[i-1]=j-1;
                        else
                            alignment[i-1]=-1;*/
                    }
                }
                else{
                    if(table[i-1][j]<table[i][j-1]){
                        i--;
                        if(s.get(i-1).equals(t.get(j-1)))
                            alignment[i-1]=j-1;
                        else
                            alignment[i-1]=-1;
                    }
                    else{
                        j--;
                        /*if(s.get(i-1).equals(t.get(j-1))){
                            alignment[i-1]=j-1;
                        }
                        else
                            alignment[i-1]=-1;*/
                    }
                }
            }
        }
        
        
        if(false){
            for(j=0;j<t.size()+1;j++){
                for(i=0;i<s.size();i++){
                    System.out.print(table[i][j]);
                    System.out.print("\t");
                }
                System.out.println(table[i][j]);
            }
        }
        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    private static short minimum(int a, int b, int c) {
        return (short) Math.min(a, Math.min(b, c));
    }
}