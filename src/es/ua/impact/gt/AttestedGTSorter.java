package es.ua.impact.gt;

import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author xavi
 */
public class AttestedGTSorter {

    private final TreeNode head;
    private TEIManager sorted;
    private TEIManager random;
    private final HashMap<String, Boolean> tagged;
    private final HashMap<String, ArrayList<String>> missing;
    private final ArrayList<Node> toMerge;

    public static void main(String[] args) {
        if (args.length == 0) {
            main2(args);
        } else {
            AttestedGTSorter agts = new AttestedGTSorter();

            CmdOptionTester optionTester = new CmdOptionTester();
            CmdOptions parser = new CmdOptions();

            Option osorted = parser.addStringOption('s', "sorted");
            Option orandom = parser.addStringOption('r', "random");

            try {
                parser.parse(args);
            } catch (IllegalOptionValueException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownOptionException ex) {
                Logger.getLogger(AttestedGTSorter.class.getName()).log(Level.SEVERE, null, ex);
            }

            String fsorted = optionTester.testFile(parser, osorted, false, false, true);
            String frandom = optionTester.testFile(parser, orandom, false, false, true);
            agts.sort(fsorted, frandom);
            agts.save();
        }
    }

    public static void main2(String[] args) {
        AttestedGTSorter agts = new AttestedGTSorter();

        String fsorted = "/home/xavi/Dropbox/extraction/sorted.xml";
        String frandom = "/home/xavi/Dropbox/extraction/random.xml";
        agts.sort(fsorted, frandom);
        agts.save();
    }

    public AttestedGTSorter() {
        head = new TreeNode();
        tagged = new HashMap<String, Boolean>();
        missing = new HashMap<String, ArrayList<String>>();
        head.setHead();
        toMerge = new ArrayList<Node>();
    }

    public void save() {

        sorted.save();

        ArrayList<String> ids = new ArrayList<String>();
        Set<Entry<String, Boolean>> entries = tagged.entrySet();

        for (Entry<String, Boolean> entry : entries) {
            if (!entry.getValue()) {
                ids.add(entry.getKey());
            }
        }
        Collections.sort(ids);
        sorted.export(ids, missing);

    }

    public void sort(String fsorted, String frandom) {
        sorted = new TEIManager(fsorted);
        Set<ArrayList<String>> sls = sorted.getBlockText();
        for (ArrayList<String> ls : sls) {
            String id = ls.remove(0);
            tagged.put(id, false);
            missing.put(id, new ArrayList<String>());
            addBranch(ls, id);
        }

        String sentence = "";
        String word;
        random = new TEIManager(frandom);
        Node first = random.getNextWord();
        Node next = first;
        TreeNode tr = head;
        TreeNode nxtr;
        boolean oneword = true;
        boolean findCombined = false;
        boolean findDropCapital = true;
        boolean twowords = false;


        word = next.getAttributes().getNamedItem("nform").getTextContent().replaceAll("'","");

        while (next != null) {
            nxtr = tr.findNextCombinedNode(word, findCombined, findDropCapital);
            findDropCapital = false;
            if (nxtr == null && tr.hasEnd()) {
                // hem trobat el final d'un bloc
                //System.out.println(tr.getEnd() + sentence);
                fillInfo(random, sorted, first, next, tr.getEnd());
                sentence = "";
                nxtr = head;
                first = next;
                findCombined = false;
                oneword = true;
                twowords = false;
                findDropCapital = true;
            } else if (nxtr == null && oneword) {
                // no hi ha node següent
                // provem a unir dues paraules de la part ordenada amb una de la desordenada
                findCombined = true;
                oneword = false;
                twowords = true;
                nxtr = tr;
                //}
            } else if (nxtr == null && twowords) {
                // provem a unir dues paraules de la part desordenada amb una de la ordenada
                next = random.testNextWord();
                twowords = false;

                if (next != null) {
                    String tword = word + next.getAttributes().getNamedItem("nform").getTextContent().replaceAll("'","");

                    if (tr.testNextCombinedNode(tword)) {
                        next = random.getNextWord();
                        word = tword;
                    }
                }
                nxtr = tr;
            } else {
                // ignorem la paraula i en triem una altra
                // continuem cercant el final d'un bloc
                oneword = true;
                findCombined = false;
                next = random.getNextWord();

                if (next == null) {
                    if (nxtr != null) {
                        if (nxtr.hasEnd()) {
                            fillInfo(random, sorted, first, next, nxtr.getEnd());
                        }
                    }
                    break;
                }
                word = next.getAttributes().getNamedItem("nform").getTextContent().replaceAll("'","");
                sentence += " " + word;

                if (nxtr == null) {
                    // we leave a word in random as not appearing in sorted doc
                    nxtr = head;
                    sentence = "";
                    nxtr = head;
                    first = next;
                    findCombined = false;
                    oneword = true;
                    findDropCapital = true;
                }
            }
            tr = nxtr;

        }

        merge();
    }

    private boolean isDropCapitalMissing(String r, String s) {
        return s.substring(1).equalsIgnoreCase(r);
    }

    public void fillInfo(TEIManager random, TEIManager sorted, Node firstRandom, Node nextRandom, String idBlock) {

        tagged.put(idBlock, true);

        Element id = sorted.getID(idBlock);
        NodeList list = id.getElementsByTagName("w");

        Node spointer = list.item(0);
        Node rpointer = firstRandom;

        String sword = null;
        String rword = null;

        boolean rjoin = false;
        boolean sjoin = false;
        boolean nextSorted = false;
        boolean isHead = true;
        boolean ignoreWord = false;

        int sortedCounter = 0;

        Element prev = null;

        while (rpointer != null && spointer != null) {
            if (rpointer.getNodeType() == Node.ELEMENT_NODE) {
                if (rpointer.getNodeName().equalsIgnoreCase("w")) {

                    Element repointer = (Element) rpointer;

                    String rnewpart = repointer.getAttribute("nform");

                    if (rnewpart.endsWith(".")) {
                        rnewpart = rnewpart.substring(0, rnewpart.length() - 1);
                    }

                    if (rjoin) {
                        rword += rnewpart;
                    } else {
                        rword = rnewpart;
                    }

                    do {
                        nextSorted = false;
                        if (spointer.getNodeType() == Node.ELEMENT_NODE) {
                            if (spointer.getNodeName().equalsIgnoreCase("w")) {

                                if (ignoreWord) {
                                    ignoreWord = false;
                                    spointer = list.item(++sortedCounter);
                                    break;
                                }

                                Element sepointer = (Element) spointer;

                                if (sjoin) {
                                    sword += spointer.getTextContent();
                                } else {
                                    sword = spointer.getTextContent();
                                }

                                if (head.checkHacks(rword, sword) || isDropCapitalMissing(rword, sword)) {

                                    transferAttributes(repointer, sepointer);
                                    //System.err.println(sword);    
                                    if (sjoin) {
                                        if (spointer.getParentNode() == prev.getParentNode()) {
                                            sepointer.setTextContent(prev.getTextContent() + " " + sepointer.getTextContent());
                                            spointer.getParentNode().removeChild(prev);
                                            sortedCounter--;
                                        } else {
                                            sepointer.setTextContent(prev.getTextContent() + " " + sepointer.getTextContent());
                                            addToMerge(spointer.getParentNode(), prev.getParentNode());
                                            prev.getParentNode().removeChild(prev);
                                            sortedCounter--;
                                        }
                                    }
                                    sjoin = false;
                                } else if (sortedCounter < (list.getLength() - 1)) {
                                    Element nxt = (Element) list.item(sortedCounter + 1);
                                    if (head.checkHacks(rword,sword + nxt.getTextContent())) {
                                        nextSorted = true;
                                        sjoin = true;
                                        prev = sepointer;
                                    } else {

                                        // si hay un punto o apostrofe entre dos palabras, ignoramos las dos
                                        if (rword.equalsIgnoreCase(sword + "." + nxt.getTextContent())
                                            || rword.equalsIgnoreCase(sword + "'" + nxt.getTextContent())) {
                                            missing.get(idBlock).add(sword);
                                            missing.get(idBlock).add(nxt.getTextContent());
                                            nextSorted = true;
                                            sjoin = false;
                                            ignoreWord = true;
                                        } else {
                                            // hack para â
                                            if ((rword + "â").equalsIgnoreCase(sword)) {
                                                missing.get(idBlock).add(sword);
                                            } else {
                                                // si dos palabras random son una sorted ignoramos todo
                                                Node trnext;
                                                Node trpointer = rpointer;
                                                do {
                                                    trnext = trpointer.getNextSibling();
                                                    if (trnext.getNodeType() == Node.ELEMENT_NODE) {
                                                        if (trnext.getNodeName().equalsIgnoreCase("w")) {
                                                            Element trepointer = (Element) trnext;

                                                            String tempword = rword + trepointer.getAttribute("nform");
                                                            if (tempword.equalsIgnoreCase(sword)) {
                                                                rpointer = trnext;
                                                                nextSorted = false;
                                                                missing.get(idBlock).add(sword);
                                                            }
                                                            trnext = null;
                                                        }
                                                    }
                                                    trpointer = trnext;
                                                } while (trnext != null);
                                            }

                                        }
                                    }
                                }
                            } else {
                                if (sortedCounter < list.getLength()) {
                                    nextSorted = true;
                                }
                            }
                        } else {
                            if (sortedCounter < list.getLength()) {
                                nextSorted = true;
                            }
                        }

                        if (sortedCounter < list.getLength()) {
                            spointer = list.item(++sortedCounter);
                        }

                    } while (nextSorted);

                    isHead = false;

                    if (sortedCounter > list.getLength()) {
                        break;
                    }

                    // anar recorrent el fitxer random i anar afegint
                    // tota la informacio al fitxer sorted
                    // despres esborrar totes les dades del fitxer random
                    // per a evitar errors en el processament
                }
            }

            rpointer = rpointer.getNextSibling();
        }
    }

    private void addToMerge(Node a, Node b) {
        toMerge.add(a);
        toMerge.add(b);
    }

    private void merge() {
        if (toMerge.size() % 2 == 0) {
            for (int i = toMerge.size() - 1; i > 0; i--) {
                Node b = toMerge.get(i);
                Node a = toMerge.get(i - 1);
                Node c;
                for (int j = 0; j < b.getChildNodes().getLength(); j++) {
                    c = b.getChildNodes().item(i);
                    a.appendChild(c);
                }

                toMerge.remove(i);
                toMerge.remove(i - 1);

                b.getParentNode().removeChild(b);
            }
        }
    }

    private void transferAttributes(Element rpointer, Element spointer) {
        NamedNodeMap ratt = rpointer.getAttributes();

        for (int i = 0; i < ratt.getLength(); ++i) {
            Node att = ratt.item(i);
            spointer.setAttribute(att.getNodeName(), att.getNodeValue());
        }
    }

    public void runTest() {

        String[] strs = {"El hombre era bueno", "Mi amigo grande", "El amigo tuyo", "Hasta nunca", "El hombre llegó"};

        int i = 0;
        for (String str : strs) {
            addBranch(Arrays.asList(str.split(" ")), new Integer(++i).toString());
        }

        int j = (int) (Math.random() * strs.length);

        String x = "E l hombre era bue no";

        System.out.println(x);

        ArrayList l = new ArrayList(Arrays.asList(x.split(" ")));
        String k = head.find(l);

        System.out.println("FOUND: " + ((k != null) ? k : "NOT FOUND"));
    }

    public void addBranch(List<String> l, String end) {
        TreeNode node = head;
        for (String word : l) {
            node = node.addConnection(word.replaceAll("'",""));
        }
        node.addEnd(end);
    }

    private class TreeNode {

        private HashMap<String, TreeNode> connections;
        private ArrayList<String> ends;
        private boolean head = false;

        public TreeNode() {
            ends = new ArrayList<String>();;
            connections = new HashMap<String, TreeNode>();
        }

        public boolean hasEnd() {
            return (ends.size() > 0);
        }

        public void setHead() {
            head = true;
        }

        public boolean isHead() {
            return head;
        }

        public void addEnd(String end) {
            ends.add(end);
        }

        public TreeNode addConnection(String str) {
            TreeNode tnode = connections.get(str);

            if (tnode == null) {
                tnode = new TreeNode();
            }

            connections.put(str, tnode);

            return tnode;
        }

        public boolean testNextCombinedNode(String word) {
            if (connections.get(word) != null) {
                return true;
            } else {
                return (connections.get(word + "â") != null);
            }
        }

        public TreeNode findNextCombinedNode(String word, boolean combined, boolean dropCapital) {
            if (word.endsWith(".")) {
                word = word.substring(0, word.length() - 1);
            }



            if (!combined && !dropCapital) {
                return findNextNode(word);
            }

            TreeNode next = connections.get(word);
            Set<String> keys = connections.keySet();
            if (next == null && dropCapital) {
            //if (dropCapital) {
                // we check if there's a drop-capital missing
                if (isHead()) {
                    for (String key : keys) {
                        if (key.substring(1).equals(word)) {
                            next = connections.get(key);
                            break;
                        }
                    }
                }
            }

            if (next == null && combined) {
                // we try to join with next word
                for (String key : keys) {
                    TreeNode child = connections.get(key);
                    Set<String> k2s = child.connections.keySet();
                    for (String k2 : k2s) {
                        if (checkHacks(word, key + k2) || word.equalsIgnoreCase(key + "." + k2)
                                || word.equalsIgnoreCase(key + "'" + k2)) {
                            next = child.connections.get(k2);
                            break;
                        }
                    }
                    if (next != null) {
                        break;
                    }
                }
            }


            return next;
        }

        private boolean checkHacks(String w1, String w2) {
            // some hacks due to differences between sorted and random texts
            if (w1.equals("socorrer") && w2.equals("socotrer")) {
                return true;
            }

            if (w1.equals("cosas") && w2.equals("cofas")) {
                return true;
            }

            if (w1.equals("eleccion") && w2.equals("eieccion")) {
                return true;
            }

            if (w1.equals("enmascararla") && w2.equals("enmascarala")) {
                return true;
            }

            if (w1.equals("quitarèle") && w2.equals("qutarèle")) {
                return true;
            }

            if (w1.equals("escupia") && w2.equals("efcupia")) {
                return true;
            }

            if (w1.equals("aver") && w2.equals("ave")) {
                return true;
            }

            if (w1.equals("Como") && w2.equals("Gomo")) {
                return true;
            }

            if (w1.equals("las") && w2.equals("Ias")) {
                return true;
            }

            if (w1.equals("el") && w2.equals("eI")) {
                return true;
            }

            if (w1.equals("los") && w2.equals("Ios")) {
                return true;
            }

            if (w1.equals("viven") && w2.equals("vivcn")) {
                return true;
            }

            if (w1.equals("honra") && w2.equals("honIa")) {
                return true;
            }

            if (w1.equals("daño") && w2.equals("dasio")) {
                return true;
            }

            if (w1.equals("sabio") && w2.equals("sobio")) {
                return true;
            }

            if (w1.equals("le") && w2.equals("Ie")) {
                return true;
            }

            if (w1.equals("luz") && w2.equals("Iuz")) {
                return true;
            }

            if (w1.equals("lib") && w2.equals("Iib")) {
                return true;
            }

            if (w1.equals("favor") && w2.equals("savor")) {
                return true;
            }

            if (w1.equals("serenissimo") && w2.equals("seronissimo")) {
                return true;
            }

            if (w1.equals("su") && w2.equals("fu")) {
                return true;
            }

            if (w1.equals("glorias") && w2.equals("golrias")) {
                return true;
            }

            if (w1.equals("castigar") && w2.equals("cafligar")) {
                return true;
            }

            if (w1.equals("crueldad") && w2.equals("cruelded")) {
                return true;
            }

            if (w1.equals("acentia") && w2.equals("iacentia")) {
                return true;
            }
            
            if (w1.equals("fugitiua") && w2.equals("fugiliua")) {
                return true;
            }

            if (w1.equals("fantasia") && w2.equals("fantafia")) {
                return true;
            }

            if (w1.equals("destos") && w2.equals("desros")) {
                return true;
            }
            
            if (w1.equals("significa") && w2.equals("signiica")) {
                return true;
            }
            
            if (w1.equals("causa") && w2.equals("caus")) {
                return true;
            }
            
            if (w1.equals("Polifemo") && w2.equals("Polfemo")) {
                return true;
            }
            
            if (w1.equals("Oradores") && w2.equals("Otadores")) {
                return true;
            }
            
            if (w1.equals("San-Andres") && w2.equals("SanAndres")) {
                return true;
            }
            
            if (w1.equals("Espiritu-santo") && w2.equals("Espiritusanto")) {
                return true;
            }
            
            if (w1.equals("circunstantes") && w2.equals("circunstante")) {
                return true;
            }
            
            if (w1.replaceAll("'","").equalsIgnoreCase(w2.replaceAll("'",""))) {
                return true;
            }
            
            return w1.equals(w2);
        }

        private TreeNode compareStrings(String w1) {

            if (w1.equals("socorrer") && connections.get("socotrer") != null) {
                return connections.get("socotrer");
            }

            if (w1.equals("escupia") && connections.get("efcupia") != null) {
                return connections.get("efcupia");
            }

            if (w1.equals("cosas") && connections.get("cofas") != null) {
                return connections.get("cofas");
            }

            if (w1.equals("eleccion") && connections.get("eieccion") != null) {
                return connections.get("eieccion");
            }
            
            if (w1.equals("enmascararla") && connections.get("enmascarala") != null) {
                return connections.get("enmascarala");
            }
            
            if (w1.equals("quitarèle") && connections.get("qutarèle") != null) {
                return connections.get("qutarèle");
            }
            
            if (w1.equals("aver") && connections.get("ave") != null) {
                return connections.get("ave");
            }

            if (w1.equals("Como") && connections.get("Gomo") != null) {
                return connections.get("Gomo");
            }

            if (w1.equals("las") && connections.get("Ias") != null) {
                return connections.get("Ias");
            }

            if (w1.equals("el") && connections.get("eI") != null) {
                return connections.get("eI");
            }

            if (w1.equals("los") && connections.get("Ios") != null) {
                return connections.get("Ios");
            }

            if (w1.equals("viven") && connections.get("vivcn") != null) {
                return connections.get("vivcn");
            }

            if (w1.equals("honra") && connections.get("honIa") != null) {
                return connections.get("honIa");
            }

            if (w1.equals("daño") && connections.get("dasio") != null) {
                return connections.get("dasio");
            }

            if (w1.equals("sabio") && connections.get("sobio") != null) {
                return connections.get("sobio");
            }

            if (w1.equals("le") && connections.get("Ie") != null) {
                return connections.get("Ie");
            }

            if (w1.equals("luz") && connections.get("Iuz") != null) {
                return connections.get("Iuz");
            }

            if (w1.equals("lib") && connections.get("Iib") != null) {
                return connections.get("Iib");
            }

            if (w1.equals("favor") && connections.get("savor") != null) {
                return connections.get("savor");
            }

            if (w1.equals("serenissimo") && connections.get("seronissimo") != null) {
                return connections.get("seronissimo");
            }

            if (w1.equals("su") && connections.get("fu") != null) {
                return connections.get("fu");
            }

            if (w1.equals("glorias") && connections.get("golrias") != null) {
                return connections.get("golrias");
            }

            if (w1.equals("castigar") && connections.get("cafligar") != null) {
                return connections.get("cafligar");
            }

            if (w1.equals("crueldad") && connections.get("cruelded") != null) {
                return connections.get("cruelded");
            }

            if (w1.equals("acentia") && connections.get("iacentia") != null) {
                return connections.get("iacentia");
            }

            if (w1.equals("fugitiua") && connections.get("fugiliua") != null) {
                return connections.get("fugiliua");
            }

            if (w1.equals("fantasia") && connections.get("fantafia") != null) {
                return connections.get("fantafia");
            }

            if (w1.equals("destos") && connections.get("desros") != null) {
                return connections.get("desros");
            }

            if (w1.equals("significa") && connections.get("signiica") != null) {
                return connections.get("signiica");
            }
            
            if (w1.equals("causa") && connections.get("caus") != null) {
                return connections.get("caus");
            }
            
            if (w1.equals("Polifemo") && connections.get("Polfemo") != null) {
                return connections.get("Polfemo");
            }
            
            if (w1.equals("Oradores") && connections.get("Otadores") != null) {
                return connections.get("Otadores");
            }
            
            if (w1.equals("San-Andres") && connections.get("SanAndres") != null) {
                return connections.get("SanAndres");
            }
            
            if (w1.equals("Espiritu-santo") && connections.get("Espiritusanto") != null) {
                return connections.get("Espiritusanto");
            }
            
            if (w1.equals("circunstantes") && connections.get("circunstante") != null) {
                return connections.get("circunstante");
            }
            
            if(connections.get(w1.replaceAll("'",""))!=null){
                return connections.get(w1.replaceAll("'",""));
            }
                
            
            return null;

        }

        public TreeNode findNextNode(String word) {
            if (connections.get(word) != null) {
                return connections.get(word);
            } else {

                if (compareStrings(word) != null) {
                    return compareStrings(word);
                }

                return connections.get(word + "â");
            }
        }
        
        public String getEnd() {
            if (ends.isEmpty()) {
                return null;
            } else {
                return ends.remove(0);
            }
        }

        public String find(ArrayList<String> wds) {
            if (wds.isEmpty() && ends.size() > 0) {
                return getEnd();
            }

            TreeNode next = connections.get(wds.get(0));

            if (next != null) {
                wds.remove(0);
                return next.find(wds);
            } else {
                if (wds.size() > 1) {
                    if (connections.get(wds.get(0) + wds.get(1)) != null) {
                        next = connections.get(wds.get(0) + wds.get(1));
                        String tmp = wds.remove(0);
                        String t2 = tmp + wds.remove(0);
                        return next.find(wds);
                    }
                }
            }

            return null;
        }

        public void printXML() {
            System.err.println("<tree>");
            printXML("");
            System.err.println("</tree>");
        }

        private void printXML(String tabs) {
            Set<Entry<String, TreeNode>> entries = connections.entrySet();
            for (Entry<String, TreeNode> node : entries) {
                System.err.println(tabs + "\t" + "<node>" + node.getKey());
                node.getValue().printXML(tabs + "\t");
                System.err.println(tabs + "\t" + "</node>");
            }

            for (String end : ends) {
                System.err.println(tabs + "\t<end>" + end + "</end>");
            }
        }
    }
}
