package es.ua.impact.gt;

import es.ua.impact.utils.*;
import es.ua.impact.utils.CmdOptions.*;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntrySplitter {
    public static void main(String [] args) {
        EntrySplitter es = new EntrySplitter();
        es.run(args);
    }
    
    void run(String [] args){
        
        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();
        
        Option odir = parser.addStringOption('d', "dir");
        
        try {
            parser.parse(args);
            } catch (IllegalOptionValueException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(ReplaceGTChars.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String d = optionTester.testDirectory(parser, odir);
        
        if (d != null) {
                File dir = new File(d);
                FileListFilter select = new FileListFilter("", "out.txt");
                File[] files = dir.listFiles(select);
                
                for (File fi : files) {
                    // we need to parse the txt
                    // change 3spaces by a tab
                    
                }
        }
    }
}
