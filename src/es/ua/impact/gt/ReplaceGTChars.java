package es.ua.impact.gt;

import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import es.ua.impact.utils.FileListFilter;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplaceGTChars {

    private static final String replacements_file = "spanish_replacements.xml";
    
    private static final String replacements = "es/ua/impact/resources/" + replacements_file;
    
    public static void main(String[] args) {
        ReplaceGTChars main = new ReplaceGTChars();
        
        main.run(args);
    }

    public void run(String[] args) {
        ArrayList<ReplaceRule> set;
        XMLReplacementsReader repReader = new XMLReplacementsReader();
        String key, value;
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();
        boolean debug = false;

        InputStream s = this.getClass().getClassLoader().getResourceAsStream(replacements);
        
        repReader.addXML(s);
        set = repReader.getReplacements();

        Option ofile = parser.addStringOption('f', "files");
        Option odir = parser.addStringOption('d', "dir");
        Option oprint = parser.addBooleanOption('p', "print");
        Option osort = parser.addBooleanOption('s',"sort");
        Option oall = parser.addBooleanOption('a',"all");
        Option oxml = parser.addBooleanOption('x',"xml");
        Option odebug = parser.addBooleanOption('g',"debug");
        
        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        }

        GTTextExtractor gt = new GTTextExtractor(set);
        debug = optionTester.testBoolean(parser, odebug);
        gt.setDebug(debug);
        
        boolean print = optionTester.testBoolean(parser, oprint);
        String f = optionTester.testFile(parser, ofile);
        if(optionTester.testBoolean(parser, oall))
            gt.showAll();
        
        if(optionTester.testBoolean(parser, osort)) {
            gt.sort();
            gt.setShowXML(optionTester.testBoolean(parser, oxml));
        }
        
        if (f != null) {
            gt.getText(f);
            gt.replace();
            if (print) {
                gt.print();
            } else {
                gt.save(f);
            }
        } else {
            String d = optionTester.testDirectory(parser, odir);
            if (d != null) {
                File dir = new File(d);
                FileListFilter select = new FileListFilter("", "xml");
                File[] files = dir.listFiles(select);
                
                if (print) {
                    for (File fi : files) {
                        gt.getText(fi.toString());
                    }
                    gt.replace();
                    gt.print();
                } else {
                    for (File fi : files) {
                        if(debug) System.err.println(fi.toString());
                        gt.getText(fi.toString());
                        gt.replace();
                        gt.save(fi.toString());
                        gt.clear();
                    }
                }
            }
        }

    }

    public ReplaceGTChars() {
        
    }
}
