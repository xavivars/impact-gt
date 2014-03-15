package es.ua.impact.utils;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringModifier {

    public static enum TipoPalabra {

        AGUDA, LLANA, ESDRUJULA
    }
    private Syllabicator syl;
    private String accents;
    Pattern paccents, plastchar, popen, pclosed;
    Matcher matcher;

    public StringModifier() {
        syl = new Syllabicator();
        accents = ".*[áéíóú].*";

        String lastChar = "(" + syl.v + "|[ns])";

        plastchar = Pattern.compile(lastChar, Pattern.CASE_INSENSITIVE);
        paccents = Pattern.compile(accents, Pattern.CASE_INSENSITIVE);
        popen = Pattern.compile(syl.a, Pattern.CASE_INSENSITIVE);
        pclosed = Pattern.compile(syl.i, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Given a word, it returns the type of word according to 
     * stress pronunciation
     * @param word
     * @return 
     */
    public TipoPalabra detect(String word) {

        TipoPalabra tipo = null;

        String[] silabas = syl.split(word).split("-");

        // we look for an acute
        for (int i = silabas.length - 1; i >= 0; i--) {
            matcher = paccents.matcher(silabas[i]);

            if (matcher.find()) {
                int n = silabas.length - i;
                switch (n) {
                    case 1:
                        tipo = TipoPalabra.AGUDA;
                        break;
                    case 2:
                        tipo = TipoPalabra.LLANA;
                        break;
                    default:
                        tipo = TipoPalabra.ESDRUJULA;
                        break;
                }
                break;
            }
        }

        if (tipo == null) {
            // we try do decide if it's AGUDA or LLANA

            if (syl.countSyllables(syl.split(word)) == 1) {
                tipo = TipoPalabra.AGUDA;
            } else {

                int i = word.length() - 1;
                char c = word.charAt(i);

                matcher = plastchar.matcher("" + c);

                if (matcher.find()) {
                    tipo = TipoPalabra.LLANA;
                } else {
                    tipo = TipoPalabra.AGUDA;
                }
            }
        }

        return tipo;
    }

    private String removeDiacritic(String word) {
        return word.replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i").replaceAll("ó", "o").replaceAll("ú", "u");
    }

    private String addDiacritic(String word) {
        return addDiacritic(word, detect(word));
    }

    private String addDiacritic(String word, TipoPalabra tipo) {
        String ret = null;
        String sp, ls;

        switch (tipo) {
            case AGUDA:
                sp = syl.split(word);
                ls = syl.getLastSyl(sp);
                matcher = popen.matcher(ls);
                if (matcher.find()) {
                    ls = ls.replace('a', 'á').replace('e', 'é').replace('o', 'ó');
                    ret = syl.replaceLastSyl(sp, ls);
                } else {
                    ls = ls.replace('i', 'í').replace('u', 'ú');
                    ret = syl.replaceLastSyl(sp, ls);
                }
                break;
            case LLANA:
                sp = syl.split(word);
                int n = syl.countSyllables(sp) - 2;
                ls = syl.getSyl(sp, n);
                matcher = popen.matcher(ls);
                if (matcher.find()) {
                    ls = ls.replace('a', 'á').replace('e', 'é').replace('o', 'ó');
                    ret = syl.replaceSyl(sp, ls, n);
                } else {
                    ls = ls.replace('i', 'í').replace('u', 'ú');
                    ret = syl.replaceSyl(sp, ls, n);
                }
                break;
        }

        return ret.replaceAll(syl.sep, "");
    }

    public String contract(String[] words) {
        String ret = null;

        if (words.length == 2) {
            if (words[1].equals("el")) {
                if (words[0].equals("a")) {
                    ret = "al";
                } else if (words[0].equals("de")) {
                    ret = "del";
                }
            }
        }

        return ret;
    }

    public String joinLemmas(String word) {
        String ret = "";
        String[] words = word.split(" ");

        boolean noFirst = false;
        
        for (String wd : words) {
            
            if (!wd.equals("EMPTY")) {
                if(noFirst) ret += "+";
            
                noFirst = true;
            
                ret += wd;
            }
        }

        return ret;
    }

    /**
     * Joins a verb with pronouns updating the accent
     * if needed
     * @param words
     * @return 
     */
    public String joinWithDiacritics(String[] words) {
        StringBuilder ret = new StringBuilder();
        TipoPalabra inicial = null;


        if (words.length > 0) {

            if (words.length > 1) {
                String contracted = contract(words);
                if (contracted != null) {
                    ret.append(contracted);
                } else {


                    inicial = detect(words[0]);
                    matcher = paccents.matcher(words[0]);

                    switch (inicial) {
                        case AGUDA:
                            if (matcher.find()) {
                                if (words.length == 2) {
                                    words[0] = removeDiacritic(words[0]);
                                }
                            } else {
                                if (words.length > 2) {
                                    words[0] = addDiacritic(words[0], TipoPalabra.AGUDA);
                                }
                            }
                            break;
                        case LLANA:
                            if (!matcher.find()) {
                                words[0] = addDiacritic(words[0], TipoPalabra.LLANA);
                            }
                            break;
                        case ESDRUJULA:
                            break;
                    }

                    if (words[0].endsWith("mos") && words[1].equals("nos")) {
                        words[0] = words[0].substring(0, words[0].length() - 1);
                    }

                    for (int i = 0; i < words.length; i++) {
                        ret.append(words[i]);
                    }
                }
            } else {
                ret.append(words[0]);
            }

        }

        return ret.toString();
    }

    public static void main(String[] args) {
        StringModifier s = new StringModifier();

        for (String st : args) {
            System.out.println(s.joinWithDiacritics(st.split("#")));
        }


    }
}
