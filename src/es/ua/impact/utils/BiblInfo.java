package es.ua.impact.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;

public class BiblInfo {
    private static final String info = "es/ua/impact/resources/obras.csv";
    
    private HashMap<String,Data> bibliografia;
    
    public BiblInfo() {
        
        bibliografia = new HashMap<String, Data>();
        
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(info);
        InputStreamReader r = new InputStreamReader(is);
        String line;
        
        
        Scanner scanner = new Scanner(is);
        while(scanner.hasNextLine()) {
            line = scanner.nextLine();
            String [] s = line.split("\\|");
            if(s[0].contains("-")) {
                int min = Integer.parseInt((s[0].split("-"))[0]); 
                int max = Integer.parseInt((s[0].split("-"))[1]);
                
                for(int i = min; i <= max; i++) {
                    bibliografia.put("00"+i,new Data("00"+i,s[1].trim(),s[2].trim(),s[3].trim()));
                }
            } else {
                bibliografia.put(s[0],new Data(s[0],s[1].trim(),s[2].trim(),s[3].trim()));
            }
        }
    }
    
    public String getBiblXML(String id, boolean withTag) {
        if(withTag)
            return    "                         <bibl>\n"
                    + "                           " + getBiblXML(id)
                    + "\n                        </bibl>\n";
        else
            return getBiblXML(id);
    }
    
    public String getBiblXML(String id) {

        return "<title>" + getTitle(id) + "</title>"
                + "<author>" + getAuthor(id) + "</author>"
                + "<date>" + getYear(id) + "</date>"
                + "<ident>" + getID(id) + "</ident>";
    }

    
    
    public String getYear(String id) {
        Data d = bibliografia.get(id);
        if(d!=null)
            return d.getDate();
        
        return null;
    }
    
    public String getTitle(String id) {
        Data d = bibliografia.get(id);
        if(d!=null)
            return d.getTitle();
        
        return null;
    }
    
    public String getAuthor(String id) {
        Data d = bibliografia.get(id);
        if(d!=null)
            return d.getAuthor();
        
        return null;
    }
    
    public String getID(String id) {
        Data d = bibliografia.get(id);
        if(d!=null)
            return d.getID();
        
        return null;
    }
    
    private class Data {
        private String title;
        private String author;
        private String date;
        private String ID;
        public Data(String i, String t, String a, String d) {
            title = t;
            author = a;
            date = d;
            ID = i;
        }
        public String getID() {
            return ID;
        }

        public String getAuthor() {
            return author;
        }

        public String getDate() {
            return date;
        }

        public String getTitle() {
            return title;
        }
    }
    
}
