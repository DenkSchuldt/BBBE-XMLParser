/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmlparser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/*
 *   XMLParser
 * -------------
 * Description:
 *      XMLParser parses the "events.xml" file in order to extract the required data to
 *      generate the "saved-data.json" file, which is the file that Popcorn Maker
 *      reads at the begining to build the timeline when the user wants to start a new project.
 *      If you modify this file, then you can add events by default, to start a new project.
 * 
 * Required libraries:
 *      -> dom4j-1.6.1.jar : http://sourceforge.net/projects/dom4j/
 *      -> gson-2.2.3.jar  : https://code.google.com/p/google-gson/downloads/detail?name=google-gson-2.2.3-release.zip
 */
public class XMLParser {
    
    public static FileWriter fw = null;
    public static PrintWriter pw = null;
    public static ArrayList<String> totalEvents = new ArrayList<>(), totalModules = new ArrayList<>();
    public static double start_slide = 0, end_slide = 0, saveLast = 0, end_of_conference = 0, start_audio = 0, end_audio = 0; 
    public static int trackEvent = 0, number_of_slides = 0;
    public static String fst_timestamp = "",                          
                         currentSlide = "",
                         lastSlide = "",
                         meeting_id = "",
                         directory = "",
                         lastDirectory = "",
                         json = "";                         
    public static boolean voiceLayerCreated = false,
                          imageLayerCreated = false,
                          waiting_next = false,
                          writeComa = false,
                          printed_fst_image_event = false;    
    
    public static void main(String[] args) throws IOException, DocumentException {
        try{
            
            /*
             * Reads the "events.xml" file.
             */
            InputStream is = new FileInputStream("events.xml");
            Document doc = new SAXReader().read(is);
            is.close();
            
            Element content = (Element)doc.getRootElement();
            meeting_id = content.attribute("meeting_id").getValue();
            fst_timestamp = meeting_id.substring(findIndex(meeting_id));
            lastDirectory = meeting_id + "/presentation/default/thumbnails";
            List<Element> events = content.elements("event");
            
            for(int i=0;i<events.size();i++){
                Element currentElement = events.get(i);
                Attribute eventName = currentElement.attribute("eventname");
                Attribute timestamp = currentElement.attribute("timestamp");
                if(eventName.getStringValue().equals("EndAndKickAllEvent")){
                    // Finding the end of the conference.
                    end_of_conference = getSeconds(timestamp.getValue(),fst_timestamp);                    
                }
                if(eventName.getStringValue().equals("GotoSlideEvent")){
                    number_of_slides++;
                    System.out.println("Showing slide: " + currentElement.elementText("slide") + " at t = " + getSeconds(timestamp.getValue(),fst_timestamp) + " segundos.");
                }
                
                if(eventName.getStringValue().equals("StartRecordingEvent")){
                    start_audio = getSeconds(timestamp.getValue(),fst_timestamp);                    
                }
                
                if(eventName.getStringValue().equals("ParticipantLeftEvent")){
                    end_audio = getSeconds(timestamp.getValue(),fst_timestamp);
                }
            }
                        
            updateJSON(true,end_of_conference);
                        
            for(int i=0;i<events.size();i++){
                //This for is just for the GotoSlideEvent, so a layer will be created for this.
                Element currentElement = events.get(i);
                Attribute eventName = currentElement.attribute("eventname");
                Attribute timestamp = currentElement.attribute("timestamp");
                
                if(eventName.getStringValue().equals("SharePresentationEvent")){
                    Element presentationName = currentElement.element("presentationName");
                    directory = meeting_id + "/presentation/" +presentationName.getText()+ "/thumbnails";
                }
                
                if(eventName.getStringValue().equals("GotoSlideEvent")){
                    if(!imageLayerCreated){
                        //Writes the layer
                        json += "{" +
                                "\"name\": \"Layer\"," +
                                "\"id\": \"1\"," +
                                "\"trackEvents\": [";
                        imageLayerCreated = true;
                    }
                    if(number_of_slides == 1){
                        saveLast = getSeconds(timestamp.getValue(),fst_timestamp);
                        break;
                    }
                    if(waiting_next){
                        end_slide = getSeconds(timestamp.getValue(),fst_timestamp);
                        if(writeComa){
                            json += ",{";
                        }
                        else{
                            json += "{";
                        }
                        json += "\"id\": \"TrackEvent"+ trackEvent +"\"," +
                                "\"type\": \"image\"," +
                                "\"popcornOptions\": {";
                        trackEvent++;
                        if(!printed_fst_image_event){
                            json += "\"start\" : "+ start_slide +",";
                            printed_fst_image_event = true;
                        }else{
                            json += "\"start\" : "+ saveLast +",";
                        }
                        json += "\"end\" : "+ end_slide +"," +
                                "\"target\": \"video-container\"," +
                                "\"src\": \""+lastDirectory+"/thumb-"+(Integer.parseInt(currentSlide)+1)+".png\"," +
                                "\"width\": 80," +
                                "\"height\": 80," +
                                "\"top\": 10," +
                                "\"left\": 10," +
                                "\"transition\": \"popcorn-fade\"" +
                                "}," +
                                "\"track\": \"1\"," +
                                "\"name\": \"TrackEvent" +trackEvent+ "\"" +
                                "}"; //End event
                        writeComa = true;
                        saveLast = getSeconds(timestamp.getValue(),fst_timestamp);
                        currentSlide = currentElement.elementText("slide");
                    }else{
                        start_slide = getSeconds(timestamp.getValue(),fst_timestamp);
                        currentSlide = currentElement.elementText("slide");
                        lastSlide = currentSlide;
                        waiting_next = true;
                    }
                    lastDirectory = directory;
                }
            }
            //Writes the last slide:
            if(number_of_slides == 1){
                json += "{";
            }
            else{
                json += ",{";
            }            
            json += "\"id\": \"TrackEvent" +trackEvent+ "\"," +
                    "\"type\": \"image\"," +
                    "\"popcornOptions\": {" +
                    "\"start\": " +saveLast+ "," +
                    "\"end\": " +end_of_conference+ "," +
                    "\"target\": \"video-container\"," +                    
                    "\"src\": \""+lastDirectory+"/thumb-" +(Integer.parseInt(currentSlide)+1)+ ".png\"," +
                    "\"width\": 80," +
                    "\"height\": 80," +
                    "\"top\": 10," +
                    "\"left\": 10," +
                    "\"transition\": \"popcorn-fade\"" +
                    "}," +
                    "\"track\": \"1\"," +
                    "\"name\": \"TrackEvent" +(trackEvent+1)+ "\"" +
                    "}]" + //End event
                    "}";
            trackEvent++;
            String fileName = findFile("Z:/butter/public/templates/basic/" + meeting_id + "/audio",".wav");
            
            //Writes the layer for the audio.
            json += ",{" +
                    "\"name\": \"Layer\"," +
                    "\"id\": \"2\"," +
                    "\"trackEvents\": [" +
                    "{" +
                    "\"id\": \"TrackEvent" +trackEvent+ "\"," +
                    "\"type\": \"sequencer\"," +
                    "\"popcornOptions\": {" +
                    "\"start\": " +start_audio+ "," +
                    "\"end\": " +end_audio+ "," +
                    "\"source\": \""+meeting_id+"/audio/" + fileName + "\"," +
                    "\"title\": \"" + fileName +"\"," +
                    "\"fallback\": []," +
                    "\"duration\": " +end_of_conference+ "," +
                    "\"target\": \"video-container\"," +
                    "\"width\": 100," +
                    "\"height\": 100," +                
                    "\"top\": 0," +
                    "\"left\": 0," +
                    "\"from\": 0," +
                    "\"volume\": 100," +
                    "\"hidden\": true," +
                    "\"mute\": false," +
                    "\"zindex\": 999," +
                    "\"denied\": false" +
                    "}," +
                    "\"track\": \"1\"," +
                    "\"name\": \"TrackEvent" +(trackEvent+1)+ "\"" +
                    "}]" + //End event
                    "}";                                    
            
            updateJSON(false);
            
            fw = new FileWriter("saved-data.json");
            pw = new PrintWriter(fw);
            pw.write(prettyJSON(json));            
            
        }catch (DocumentException | IOException e){
            System.out.println("Houston, We have a problem! :S");
        }finally{
            fw.close();
        }
    }
        
    public static String findFile(String folder, String ext){
        GenericExtFilter filter = new GenericExtFilter(ext);
        File dir = new File(folder);
        String[] list = dir.list(filter);
        return list[0];
    }
    
    public static class GenericExtFilter implements FilenameFilter{
        private String ext;
        public GenericExtFilter(String ext){
            this.ext = ext;
        }
        public boolean accept(File dir, String name){
            return (name.endsWith(ext));
        }
    }
    
    
    /*
     * Method: updateJSON
     * Usage: updateJSON(json, true/false, duration of the conference);
     * ----------------------------------------------------------------
     * Description: If init is true, updates json adding the first part of the 
     *              result, if not, updates json adding the end of the result.
     */
    public static void updateJSON(boolean init, Object... params){
        if(init){
            json += "{" +
                    "\"target\": [{" +
                    "\"id\": \"Target0\"," +
                    "\"name\": \"video-container\"," +
                    "\"element\": \"video-container\"" +
                  "}]," +
                  "\"media\": [{" +
                    "\"id\": \"Media0\"," +
                    "\"name\": \"Media0\"," +                    
                    "\"target\": \"video\"," +
                    "\"duration\": " +params[0]+ "," +
                    "\"popcornOptions\": {\"frameAnimation\": true}," +
                    "\"controls\": true," +
                    "\"tracks\": [";
        }else{
            json += "]" +
                    "}]," +
                    "\"name\": \"My new project\"," +
                    "\"template\": \"basic\"" +
                "}";
        }
    }
    
    /*
     * Method: prettyJSON
     * Usage: prettyJSON("Your ugly json String here");
     * ------------------------------------------------
     * Description: returns a pretty json String.
     */
    public static String prettyJSON(String json){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return gson.toJson(je);
    }    
    
    /*
     * Method: getSeconds
     * Usage: getSeconds("Timestamp of an event", fst_timestamp);
     * ----------------------------------------------------------
     * Description: Returns the seconds in which an event must
     *              appear in the timeline.
     */
    public static double getSeconds(String a, String b){
        long seconds = Long.parseLong(a) - Long.parseLong(b);
        return (seconds/1000.00);
    }
    
    /*
     * Method: findIndex
     * Usage: findIndex("The meeting-id here");
     * ----------------------------------------
     * Description: Returns the index of the '-' char,
     *              in order to extract the start of
     *              the conference.
     */    
    public static int findIndex(String s){
        for(int i=0; i<s.length(); i++) {
            if(s.charAt(i) == '-'){
                return (i+1);
            }
        }
        return 0;
    }
}

