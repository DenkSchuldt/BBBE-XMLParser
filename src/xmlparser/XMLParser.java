/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmlparser;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 *
 * @author Denny
 */
public class XMLParser {
    
    public static void main(String[] args) throws IOException {
        parseXML();
        writeJSON();
    }    
    
    public static void parseXML(){
        ArrayList<String> totalEvents = new ArrayList<String>();
        ArrayList<String> totalModules = new ArrayList<String>();
        String fst_timestamp = "";
        String meeting_id = "";
        try{
            InputStream is = new FileInputStream("events.xml");
                SAXReader reader = new SAXReader();
                org.dom4j.Document doc = reader.read(is);
            is.close();
            Element content = (Element)doc.getRootElement();            
            meeting_id = content.attribute("meeting_id").getValue();
            fst_timestamp = meeting_id.substring(findIndex(meeting_id));            
            List<Element> events = content.elements("event");
            for(int i=0;i<events.size();i++){
                Element currentElement = events.get(i);
                Attribute eventName = currentElement.attribute("eventname");
                Attribute module = currentElement.attribute("module");
                Attribute timestamp = currentElement.attribute("timestamp");
                if(!totalEvents.contains(eventName.getStringValue()))
                    totalEvents.add(eventName.getStringValue());
                if(!totalModules.contains(module.getStringValue()))
                    totalModules.add(module.getStringValue());
                long seconds = Long.parseLong(timestamp.getValue()) - Long.parseLong(fst_timestamp);
                System.out.println("Event #" + (i+1) + " occurs at t = " + seconds/1000.000 + " seconds");
            }
            System.out.println("\n<-----------------Events----------------->");
            for(String uniqueEvent : totalEvents){
                System.out.println(uniqueEvent);
            }
            System.out.println("\n<-----------------Modules----------------->");
            for(String uniqueModule : totalModules){
                System.out.println(uniqueModule);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("\n\n\tTechnical failure... x(");
        }
    }    
    
    public static void writeJSON() throws IOException{
        FileWriter fw = null;
        PrintWriter pw = null;
        try{            
            fw = new FileWriter("data.json");
            pw = new PrintWriter(fw);
            pw.println("{");
            pw.println("\t\"target\": [{");
            pw.println("\t\t\"id\": \"Target0\",");        
            pw.println("\t\t\"name\": \"video-container\",");
            pw.println("\t\t\"element\": \"video-container\"");
            pw.println("\t}],");            
            pw.println("\t\"media\": [{");
            pw.println("\t\t\"id\": \"Media0\",");
            pw.println("\t\t\"name\": \"Media0\",");        
            pw.println("\t\t\"url\": \"#t=,30\",");
            pw.println("\t\t\"target\": \"video\",");
            pw.println("\t\t\"duration\": 30,");
            pw.println("\t\t\"controls\": false,");
            pw.println("\t\t\"tracks\": [{}],");
            pw.println("\t}],");
            pw.println("\t\"name\": \"My new project\",");
            pw.println("\t\"template\": \"basic\"");
            pw.println("}");
        }catch(Exception e){
            System.out.println("Houston, ¡Tenemos un problema!");
            e.printStackTrace();
        }finally{
            fw.close();
        }
    }
    
    
    public static int findIndex(String s){        
        for(int i=0; i<s.length(); i++)
            if(s.charAt(i) == '-')
                return (i+1);
        return 0;
    }
}
