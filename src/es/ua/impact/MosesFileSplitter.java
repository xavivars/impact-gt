package es.ua.impact;

import es.ua.impact.utils.CmdOptionTester;
import es.ua.impact.utils.CmdOptions;
import es.ua.impact.utils.CmdOptions.IllegalOptionValueException;
import es.ua.impact.utils.CmdOptions.Option;
import es.ua.impact.utils.CmdOptions.UnknownOptionException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MosesFileSplitter {

    private String input;
    private String outputDir;
    private int testLength;
    private int inputLength;

    public static void main(String[] args) {

        CmdOptionTester optionTester = new CmdOptionTester();
        CmdOptions parser = new CmdOptions();

        Option oinput = parser.addStringOption('i', "input");
        Option olength = parser.addIntegerOption('l', "length");
        Option ooutdir = parser.addStringOption('o', "output-dir");
        
        try {
            parser.parse(args);
        } catch (IllegalOptionValueException ex) {
            Logger.getLogger(MosesFileSplitter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownOptionException ex) {
            Logger.getLogger(MosesFileSplitter.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        String input = optionTester.testFile(parser, oinput, false, false, true);
        String odir = optionTester.testDirectory(parser, ooutdir, false, false, false);
        int length = (Integer) parser.getOptionValue(olength, 5000);

        MosesFileSplitter m = new MosesFileSplitter(input, odir, length);
        m.run();

    }

    public MosesFileSplitter(String i, String o, int l) {
        input = i;
        outputDir = o;
        testLength = l;
        try {
            inputLength = getLines(i);
            System.err.println("INFO: Input file has "+inputLength+" lines.");
        } catch(Exception e) {
            inputLength = -1;
        }
    }
    
    private Set<Integer> createRandomNumbers(int amount,int max) {
     
        HashSet<Integer> rand = new HashSet<Integer>(amount);
        
        Random randomGenerator = new Random(42);

        System.err.println("INFO: Creating TUNING random numbers...");
        for(int i = 0; i < amount;) {
            if(rand.add(randomGenerator.nextInt(max))) ++i;
        }
        
        return rand;
    }
    
    /**
     * Method to create 'amount' of random integers, with 'max' as maximum
     * and a set of excluded values
     * @param amount
     * @param max
     * @param exclude
     * @return 
     */
    private Set<Integer> createRandomNumbers(int amount,int max,Set<Integer> exclude) {
     
        HashSet<Integer> rand = new HashSet<Integer>(amount);
        
        Random randomGenerator = new Random();
        int r;
        
        System.err.println("INFO: Creating TEST random numbers...");
                
        for(int i = 0; i < amount;) {
            r = randomGenerator.nextInt(max);
            if(!exclude.contains(r)) {
                if(rand.add(r)) ++i;
            }
        }
        
        return rand;
    }
    
    /**
     * Method to split each input line into 3 output files
     */
    public void run() {
        if(inputLength > testLength*2) {
            try {
                Set<Integer> tuningInt = createRandomNumbers(testLength,inputLength);
                Set<Integer> testInt = createRandomNumbers(testLength,inputLength,tuningInt);
                
                BufferedReader f = new BufferedReader(new FileReader(input));
                
                FileWriter trainsource = new FileWriter(outputDir+"/train.source");
                FileWriter traintarget = new FileWriter(outputDir+"/train.target");
                FileWriter trainalign = new FileWriter(outputDir+"/train.align");
                
                FileWriter testsource = new FileWriter(outputDir+"/test.source");
                FileWriter testtarget = new FileWriter(outputDir+"/test.target");
                FileWriter testalign = new FileWriter(outputDir+"/test.align");
                
                FileWriter tuningsource = new FileWriter(outputDir+"/tuning.source");
                FileWriter tuningtarget = new FileWriter(outputDir+"/tuning.target");
                FileWriter tuningalign = new FileWriter(outputDir+"/tuning.align");
                
                String line = null;
                int i = 0;
                int tr =0,tu=0,te=0;
                
                FileWriter source, target, align;
                
                while((line = f.readLine()) != null) {
                    String [] parts = line.split(" \\|\\|\\| ");
                    
                    if(tuningInt.contains(i)) {
                        source = tuningsource;
                        target = tuningtarget;
                        align = tuningalign;
                        tu++;
                    } else if (testInt.contains(i)) {
                        source = testsource;
                        target = testtarget;
                        align = testalign;
                        te++;
                    } else {
                        source = trainsource;
                        target = traintarget;
                        align = trainalign;
                        tr++;
                    }
                    
                    source.write(parts[0]+"\n");
                    target.write(parts[1]+"\n");
                    align.write(parts[2]+"\n");
                    ++i;
                    
                    if(i%100000==0) {
                        System.err.println("INFO: "+i+" lines written ("+tr+","+tu+","+te+").");
                    }
                    
                }
                System.err.println("INFO: "+i+" lines written ("+tr+","+tu+","+te+").");
                
                trainsource.close();traintarget.close();trainalign.close();
                tuningsource.close();tuningtarget.close();tuningalign.close();
                testsource.close();testtarget.close();testalign.close();
                
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        } else {
            System.err.println("ERROR: Input file is too small");
        }
    }
    

    /**
     * Method to count the number of lines of a File
     * @param s
     * @return
     * @throws IOException 
     */
    private int getLines(String s) throws IOException {
        LineNumberReader reader = null;
        int ret = -1;
        try {
            reader = new LineNumberReader(new FileReader(s));
            while ((reader.readLine()) != null);
            ret = reader.getLineNumber();
        } catch (Exception ex) {
            
        } finally {
            if (reader != null) {
                reader.close();
            }
            return ret;
        }
    }
}
