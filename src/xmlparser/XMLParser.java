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
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 *
 * @author Denny
 */
public class XMLParser {
    
    public static void main(String[] args) throws IOException {
        parseXML_to_JSON();        
    }    
    
    public static void parseXML_to_JSON() throws IOException{
        ArrayList<String> totalEvents = new ArrayList<>();
        ArrayList<String> totalModules = new ArrayList<>();
        String fst_timestamp = "";
        String meeting_id = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        boolean voiceLayerCreated = false;
        boolean imageLayerCreated = false;
        boolean waiting_next = false;
        boolean writeComa = false;        
        boolean printed = false;
        double start = 0, end = 0, saveLast = 0, last = 0;        
        String currentSlide = "";
        int trackEvent = 0;
        try{
            InputStream is = new FileInputStream("events.xml");
                SAXReader reader = new SAXReader();
                org.dom4j.Document doc = reader.read(is);
            is.close();
            
            fw = new FileWriter("data.json");
            pw = new PrintWriter(fw);                                    

            writeJSON(pw,true); 
            
            Element content = (Element)doc.getRootElement();            
            meeting_id = content.attribute("meeting_id").getValue();
            fst_timestamp = meeting_id.substring(findIndex(meeting_id));            
            List<Element> events = content.elements("event");
            for(int i=0;i<events.size();i++){
                //This for is just for the ParticipantTalkingEvent, so a layer will be created for this.
                Element currentElement = events.get(i);
                Attribute eventName = currentElement.attribute("eventname");                
                Attribute timestamp = currentElement.attribute("timestamp");
                
                if(eventName.getStringValue().equals("ParticipantTalkingEvent")){
                    //Writes on the JSON a "Text" event, which will simulate a "voice" event.
                    if(!voiceLayerCreated){
                        //Writes the layer
                        pw.println("\t\t{");
                        pw.println("\t\t\t\"name\": \"Layer\",");
                        pw.println("\t\t\t\"id\": \"0\",");
                        pw.println("\t\t\t\"trackEvents\": [");                       
                        voiceLayerCreated = true;
                        start = getSeconds(timestamp.getValue(),fst_timestamp);
                    }else{
                        if(currentElement.element("talking").getStringValue().equals("true")){
                            start = getSeconds(timestamp.getValue(),fst_timestamp);
                        }else{
                            if(writeComa) pw.println("\t\t\t,{"); //Begin event with Coma
                            else pw.println("\t\t\t{"); //Begin event
                            pw.println("\t\t\t\t\"id\": \"TrackEvent"+ trackEvent +"\","); trackEvent++;
                            pw.println("\t\t\t\t\"type\": \"text\",");
                            pw.println("\t\t\t\t\"popcornOptions\": {");
                            pw.println("\t\t\t\t\t\"start\":" + start + ",");
                            pw.println("\t\t\t\t\t\"end\":" + getSeconds(timestamp.getValue(),fst_timestamp) + ",");
                            pw.println("\t\t\t\t\t\"target\": \"video-container\",");
                            pw.println("\t\t\t\t\t\"text\": \"Bridge -> " + currentElement.element("bridge").getStringValue()+" | Participant -> " + currentElement.element("participant").getStringValue() +"\",");
                            pw.println("\t\t\t\t\t\"position\": \"center\",");
                            pw.println("\t\t\t\t\t\"transition\": \"popcorn-fade\",");
                            pw.println("\t\t\t\t\t\"fontSize\": \"12\",");
                            pw.println("\t\t\t\t\t\"fontColor\": \"#000000\",");
                            pw.println("\t\t\t\t\t\"left\": 10,");
                            pw.println("\t\t\t\t\t\"top\": 10,");
                            pw.println("\t\t\t\t},");
                            pw.println("\t\t\t\t\"track\": \"0\",");
                            pw.println("\t\t\t\t\"name\": \"TrackEvent3\"");                        
                            pw.println("\t\t\t}"); //End event
                            writeComa = true;
                        }
                    }
                }
            }pw.write("\t\t}"); writeComa = false;            
            for(int i=0;i<events.size();i++){
                //This for is just for the GotoSlideEvent, so a layer will be created for this.
                Element currentElement = events.get(i);
                Attribute eventName = currentElement.attribute("eventname");                
                Attribute timestamp = currentElement.attribute("timestamp");
                if(eventName.getStringValue().equals("EndAndKickAllEvent")){
                    last = getSeconds(timestamp.getValue(),fst_timestamp);
                }
                if(eventName.getStringValue().equals("GotoSlideEvent")){
                    if(!imageLayerCreated){
                        //Writes the layer
                        pw.println("\n\t\t,{");
                        pw.println("\t\t\t\"name\": \"Layer\",");
                        pw.println("\t\t\t\"id\": \"1\",");
                        pw.println("\t\t\t\"trackEvents\": [");
                        imageLayerCreated = true;
                    }
                    if(waiting_next){
                        end = getSeconds(timestamp.getValue(),fst_timestamp);
                        if(writeComa) pw.println("\t\t\t,{"); //Begin event with Coma
                        else pw.println("\t\t\t{"); //Begin event
                        pw.println("\t\t\t\t\"id\": \"TrackEvent"+ trackEvent +"\","); trackEvent++;
                        pw.println("\t\t\t\t\"type\": \"image\",");
                        pw.println("\t\t\t\t\"popcornOptions\": {");
                        if(!printed){
                            pw.println("\t\t\t\t\t\"start\": "+ start +",");
                            printed = true;
                        }else{
                            pw.println("\t\t\t\t\t\"start\": "+ saveLast +",");
                        }
                        pw.println("\t\t\t\t\t\"end\": "+ end +",");
                        pw.println("\t\t\t\t\t\"target\": \"video-container\",");
                        pw.println("\t\t\t\t\t\"src\": \""+ currentSlide +"\",");
                        pw.println("\t\t\t\t\t\"width\": 80,");
                        pw.println("\t\t\t\t\t\"height\": 80,");
                        pw.println("\t\t\t\t\t\"top\": 0,");
                        pw.println("\t\t\t\t\t\"left\": 0,");
                        pw.println("\t\t\t\t\t\"transition\": \"popcorn-fade\",");
                        pw.println("\t\t\t\t},");
                        pw.println("\t\t\t\t\"track\": \"1\",");
                        pw.println("\t\t\t\t\"name\": \"TrackEvent6\"");
                        pw.println("\t\t\t}"); //End event
                        writeComa = true;
                        saveLast = getSeconds(timestamp.getValue(),fst_timestamp);
                        currentSlide = currentElement.elements().get(0).getStringValue();
                    }else{
                        start = getSeconds(timestamp.getValue(),fst_timestamp);
                        currentSlide = currentElement.elements().get(0).getStringValue();
                        waiting_next = true;
                    }                    
                }
            }
            //Write last slide:
            pw.println("\t\t\t,{"); //Begin event with Coma                        
            pw.println("\t\t\t\t\"id\": \"TrackEvent"+ trackEvent +"\","); trackEvent++;
            pw.println("\t\t\t\t\"type\": \"image\",");
            pw.println("\t\t\t\t\"popcornOptions\": {");                    
            pw.println("\t\t\t\t\t\"start\": "+ saveLast +",");                        
            pw.println("\t\t\t\t\t\"end\": "+ last +",");
            pw.println("\t\t\t\t\t\"target\": \"video-container\",");
            pw.println("\t\t\t\t\t\"src\": \""+ currentSlide +"\",");
            pw.println("\t\t\t\t\t\"width\": 80,");
            pw.println("\t\t\t\t\t\"height\": 80,");
            pw.println("\t\t\t\t\t\"top\": 0,");
            pw.println("\t\t\t\t\t\"left\": 0,");
            pw.println("\t\t\t\t\t\"transition\": \"popcorn-fade\",");
            pw.println("\t\t\t\t},");
            pw.println("\t\t\t\t\"track\": \"1\",");
            pw.println("\t\t\t\t\"name\": \"TrackEvent6\"");
            pw.println("\t\t\t}"); //End event
            pw.write("\t\t}"); writeComa = false;
            writeJSON(pw,false);
        }catch (DocumentException | IOException e){
            System.out.println("\n\n\tHouston, ¡Tenemos un problema!");
        }finally{
            fw.close();
        }
    }    
    
    public static void writeJSON(PrintWriter pw, boolean init){
        if(init){
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
            pw.println("\t\t\"tracks\": [");
        }else{
            pw.println("],");            
            pw.println("\t}],");
            pw.println("\t\"name\": \"My new project\",");
            pw.println("\t\"template\": \"basic\"");
            pw.println("}");
        }
    }
    
    public static double getSeconds(String a, String b){
        long seconds = Long.parseLong(a) - Long.parseLong(b);
        return (seconds/1000.00);
    }
    
    public static int findIndex(String s){        
        for(int i=0; i<s.length(); i++)
            if(s.charAt(i) == '-')
                return (i+1);
        return 0;
    }
}

