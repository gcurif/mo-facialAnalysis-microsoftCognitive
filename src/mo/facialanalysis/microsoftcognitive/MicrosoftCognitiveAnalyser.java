/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mo.facialanalysis.microsoftcognitive;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import facialAnalysisCore.Emotion;
import facialAnalysisCore.FacialAnalyser;
import facialAnalysisCore.FacialAnalysis;
import facialAnalysisCore.Person;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import mo.organization.FileDescription;
import static mo.organization.ProjectOrganization.logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author gustavo
 */
public class MicrosoftCognitiveAnalyser extends FacialAnalyser {

    private String urlGet;

    public MicrosoftCognitiveAnalyser() {
        this.userAlias = "Ocp-Apim-Subscription-Key";
        this.keyAlias = "Ocp-Apim-Subscription-Key";

    }

    public MicrosoftCognitiveAnalyser(String appyKey) {
        this.userAlias = "Ocp-Apim-Subscription-Key";
        this.keyAlias = "Ocp-Apim-Subscription-Key";
        this.key = appyKey;
        this.user = appyKey;
    }

    public MicrosoftCognitiveAnalyser(String urlBase, String appyKey) {
        this.userAlias = "Ocp-Apim-Subscription-Key";
        this.keyAlias = "Ocp-Apim-Subscription-Key";
        this.key = appyKey;
        this.user = appyKey;
        this.urlBase = urlBase;
    }

    public MicrosoftCognitiveAnalyser(String urlBase, String urlGet, String appyKey) {
        this.userAlias = "Ocp-Apim-Subscription-Key";
        this.keyAlias = "Ocp-Apim-Subscription-Key";
        this.key = appyKey;
        this.user = appyKey;
        this.urlBase = urlBase;
        this.urlGet = urlGet;
    }


    public String getKey() {
        return key;
    }

    
    
    @Override
    public FacialAnalysis analysisFromFile(String jsonPath) {

        String id = "";
        try {
            FileReader fr = new FileReader(jsonPath);
            BufferedReader br = new BufferedReader(fr);
            JSONObject jsonSource = new JSONObject(br.readLine());

            try {
                if (jsonSource.getString("status").equals("incomplete") || jsonSource.getString("status").equals("En linea")) {
                    FacialAnalysis a = new FacialAnalysis(this, jsonSource.getString("id"), jsonSource.getString("videoName"), jsonSource.getString("status"));
                    a.setVideoPath(jsonSource.getString("videoPath"));
                    a.setVideoPath(jsonSource.getString("videoPath"));
                    a.setStart(jsonSource.getLong("startUnix"));
                    a.setEnd(jsonSource.getLong("endUnix"));
                    
                    return a;
                } else {
                    //falta if success o complete
                    ArrayList<Person> persons = new ArrayList<Person>(); //preparing the array of return
                    long time = 0; //time of each timestamp or frame (event) of the array of response 

                    //aca codigo para la deteccion de la cantidad de personas
                    persons.add(new Person("contempt", "happiness", "neutral", "sadness", "disgust", "anger", "fear"));

                    long start = jsonSource.getLong("startUnix");
                    long end = jsonSource.getLong("endUnix");
                    
                    String processingResult = jsonSource.getString("processingResult");
                    JSONObject processingResultJson = new JSONObject(processingResult.toString());
                    JSONArray fragmentsJsonArray = processingResultJson.getJSONArray("fragments");

                    long timescale = processingResultJson.getLong("timescale");

                    //scrolling array of timestamps
                    for (int i = 0; i < fragmentsJsonArray.length(); i++) {
                        time = fragmentsJsonArray.getJSONObject(i).getLong("start")* 1000 / timescale;

                    if (fragmentsJsonArray.getJSONObject(i).has("events")){ 

                        // the timestamps have a array of frames
                        for (int j = 0; j < processingResultJson.getJSONArray("fragments").getJSONObject(i).getJSONArray("events").length(); j++) {

                            //getting objects of emotions markers
                            JSONObject face = new JSONObject(fragmentsJsonArray.getJSONObject(i).getJSONArray("events").getJSONArray(j).get(0).toString());//CORREGIRR!!!                        
                            JSONObject windowFaceDistribution = new JSONObject(face.get("windowFaceDistribution").toString());
                            JSONObject windowMeanScores = new JSONObject(face.get("windowMeanScores").toString());

                            System.out.println(time);
                            
                            //adding frames to return array
                            persons.get(0).getEmotion(0).addInstant(time + start, windowMeanScores.getDouble("contempt"));
                            persons.get(0).getEmotion(1).addInstant(time + start, windowMeanScores.getDouble("happiness"));
                            persons.get(0).getEmotion(2).addInstant(time + start, windowMeanScores.getDouble("neutral"));
                            persons.get(0).getEmotion(3).addInstant(time + start, windowMeanScores.getDouble("sadness"));
                            persons.get(0).getEmotion(4).addInstant(time + start, windowMeanScores.getDouble("disgust"));
                            persons.get(0).getEmotion(5).addInstant(time + start, windowMeanScores.getDouble("anger"));
                            persons.get(0).getEmotion(6).addInstant(time + start, windowMeanScores.getDouble("fear"));

                            long increment = (fragmentsJsonArray.getJSONObject(i).getLong("interval")* 1000 / timescale);

                            //updating time of timestamp/frame(event)
                            time = time + increment;
                        }
                    }
                    
                    
                    }
                    Double lastValue = persons.get(0).getEmotion(0).getInstants().get(persons.get(0).getEmotion(0).getInstants().size()-1).getValue();
                    
                            persons.get(0).getEmotion(0).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(1).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(2).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(3).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(4).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(5).addInstant(end+1, lastValue);
                            persons.get(0).getEmotion(6).addInstant(end+1, lastValue);

                    
                    FacialAnalysis fa = new FacialAnalysis(persons, this, id);
                    fa.setId(jsonSource.getString("id"));
                    fa.setStatus(jsonSource.getString("status"));
                    fa.setVideoName(jsonSource.getString("videoName"));
                    fa.setVideoPath(jsonSource.getString("videoPath"));
                    fa.setVideoPath(jsonSource.getString("videoPath"));
                    fa.setStart(jsonSource.getLong("startUnix"));

                    return fa;
                    //  HttpResponse<JsonNode> response =  new HttpResponse<JsonNode>("");

                }
            } catch (org.json.JSONException ex) {
                System.out.println("incompatible json");
                //Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public FacialAnalysis uploadVideo(String urlOrPath) {

        Path path = Paths.get(urlOrPath);
        try {

            byte[] byteArrayData = Files.readAllBytes(path);
            try {
                com.mashape.unirest.http.HttpResponse<JsonNode> response = Unirest.post(this.urlBase)
                        .header(this.keyAlias, this.key).header("Content-Type", "application/octet-stream").body(byteArrayData)
                        .asJson();

                int statusCode = response.getStatus();

                //if accepted
                if (statusCode == 202) {

                    String id, idHeader;
                    int stringIndex;

                    idHeader = response.getHeaders().getFirst("Operation-Location"); //obtain id and url header container
                    stringIndex = idHeader.lastIndexOf("/") + 1; //obtain final of url (id)
                    id = idHeader.substring(stringIndex); //obtain subString with the id

                    FacialAnalysis fa = new FacialAnalysis();
                    fa.setAnalyser(this);
                    fa.setId(id);
                    fa.setStatus("En linea");
                    fa.setVideoName(urlOrPath);

                    String stringPath = fa.getVideoPath();
                    String timePath = stringPath.substring(0, stringPath.lastIndexOf(".")) + "-temp.txt";
                    String cadena;

                    FileReader f;
                    try {
                        f = new FileReader(timePath);
                        BufferedReader b = new BufferedReader(f);
                        try {
                            if ((cadena = b.readLine()) != null) {
                                fa.setStart(Long.parseLong(cadena));
                            }
                            if ((cadena = b.readLine()) != null) {
                                fa.setEnd(Long.parseLong(cadena));
                            }
                            b.close();
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    } catch (FileNotFoundException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }

                    return fa;

                } else {

                    System.out.println(response.getStatus() + ": " + response.getStatusText());
                    return null;
                }

            } catch (UnirestException ex) {
                //execpcion por defecto
                Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (IOException ex) {
            Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    @Override
    public FacialAnalysis uploadVideo(File file) {

        Path path = Paths.get(file.getPath());
        try {

            byte[] byteArrayData = Files.readAllBytes(path);
            try {
                com.mashape.unirest.http.HttpResponse<JsonNode> response = Unirest.post(this.urlBase)
                        .header(this.keyAlias, this.key).header("Content-Type", "application/octet-stream").body(byteArrayData)
                        .asJson();

                int statusCode = response.getStatus();

                //if accepted
                if (statusCode == 202) {

                    String id, idHeader;
                    int stringIndex;

                    idHeader = response.getHeaders().getFirst("Operation-Location"); //obtain id and url header container
                    stringIndex = idHeader.lastIndexOf("/") + 1; //obtain final of url (id)
                    id = idHeader.substring(stringIndex); //obtain subString with the id

                    FacialAnalysis fa = new FacialAnalysis();
                    fa.setAnalyser(this);
                    fa.setId(id);
                    fa.setStatus("En linea");
                    fa.setVideoName(file.getName());
                    fa.setVideoPath(file.getPath());

                    String stringPath = fa.getVideoPath();
                    String timePath = stringPath.substring(0, stringPath.lastIndexOf(".")) + "-temp.txt";
                    String cadena;

                    FileReader f;
                    try {
                        f = new FileReader(timePath);
                        BufferedReader b = new BufferedReader(f);
                        try {
                            if ((cadena = b.readLine()) != null) {
                                fa.setStart(Long.parseLong(cadena));
                            }
                            if ((cadena = b.readLine()) != null) {
                                fa.setEnd(Long.parseLong(cadena));
                            }
                            b.close();
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    } catch (FileNotFoundException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    return fa;

                } else {

                    System.out.println(response.getStatus() + ": " + response.getStatusText());
                    return null;
                }

            } catch (UnirestException ex) {
                //execpcion por defecto
                Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (IOException ex) {
            Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    @Override
    public Future<FacialAnalysis> uploadVideoAsync(String urlOrPath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<FacialAnalysis> uploadVideoAsync(File file) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        return executor.submit(new MicrosoftCognitiveAsync(this, "uploadVideo", file));
    }

    @Override
    public boolean analysisToFile(FacialAnalysis analysis, String path) {

        if ((analysis.getStatus().equals("Succeeded") || analysis.getStatus().equals("Completado"))
                && analysis.getOriginalBody() != null) {

            FileWriter output = null;
            BufferedWriter bw = null;

            try {
                output = new FileWriter(path);
                bw = new BufferedWriter(output);

                JSONObject outputJson = new JSONObject(analysis.getOriginalBody().getBody().toString());
                outputJson.put("videoName", analysis.getVideoName());
                outputJson.put("id", analysis.getId());
                outputJson.put("videoPath", analysis.getVideoPath());
                outputJson.put("startUnix", analysis.getStart());
                outputJson.put("endUnix", analysis.getEnd());

                FileDescription desc = new FileDescription(new File(path),this.getClass().getName()+".complete");
                bw.write(outputJson.toString());
                System.out.println("file saved in: " + path);

                bw.close();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try {

                    if (null != output) {
                        output.close();
                    }
                    return true;

                } catch (Exception e2) {
                    e2.printStackTrace();
                    return false;
                }
            }
        } else {

            JSONObject jsonOutput = new JSONObject();
            jsonOutput.put("videoName", analysis.getVideoName());
            jsonOutput.put("id", analysis.getId());
            jsonOutput.put("status", analysis.getStatus());
            jsonOutput.put("videoPath", analysis.getVideoPath());
            jsonOutput.put("startUnix", analysis.getStart());
            jsonOutput.put("endUnix", analysis.getEnd());

            try {
                FileWriter output = new FileWriter(path);
                System.out.println("file saved in: " + path);

                BufferedWriter writer = new BufferedWriter(output);
                FileDescription desc = new FileDescription(new File(path), this.getClass().getName()+ ".incomplete");

                writer.write(jsonOutput.toString());
                writer.close();
                output.close();

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public FacialAnalysis update(FacialAnalysis analysis) {

        String id = analysis.getId();
        try {

            //calling get metod of the api
            HttpResponse<JsonNode> response = Unirest.get(this.urlGet + "/" + id)
                    .header(this.keyAlias, this.key)
                    .asJson();

            int statusCode = response.getStatus(); //getting status code of response

            if (statusCode == 200) { //if the calling is accepted 

                ArrayList<Person> persons = new ArrayList<Person>(); //preparing the array of return
                long time = 0; //time of each timestamp or frame (event) of the array of response 

                //aca codigo para la deteccion de la cantidad de personas
                persons.add(new Person("contempt", "happiness", "neutral", "sadness", "disgust", "anger", "fear"));

                String analysisStatus = response.getBody().getObject().get("status").toString(); //obtain status of analysis
                if (analysisStatus.equals("Succeeded")) { //if analysis is ready for read

                    //getting json objects and arrays of response
                    String processingResult = response.getBody().getArray().getJSONObject(0).get("processingResult").toString();
                    JSONObject processingResultJson = new JSONObject(processingResult.toString());
                    JSONArray fragmentsJsonArray = processingResultJson.getJSONArray("fragments");

                    long timescale = processingResultJson.getLong("timescale");

                    //scrolling array of timestamps
                    for (int i = 0; i < fragmentsJsonArray.length(); i++) {
                        time = (fragmentsJsonArray.getJSONObject(i).getLong("start")* 1000 / timescale);

                        if (fragmentsJsonArray.getJSONObject(i).has("events")) {
                            // the timestamps have a array of frames
                            for (int j = 0; j < fragmentsJsonArray.getJSONObject(i).getJSONArray("events").length(); j++) {

                                //getting objects of emotions markers
                                JSONObject face = new JSONObject(fragmentsJsonArray.getJSONObject(i).getJSONArray("events").getJSONArray(j).get(0).toString());//CORREGIRR!!!                        
                                JSONObject windowFaceDistribution = new JSONObject(face.get("windowFaceDistribution").toString());
                                JSONObject windowMeanScores = new JSONObject(face.get("windowMeanScores").toString());

                                //adding frames to return array
                                persons.get(0).getEmotion(0).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("contempt"));
                                persons.get(0).getEmotion(1).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("happiness"));
                                persons.get(0).getEmotion(2).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("neutral"));
                                persons.get(0).getEmotion(3).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("sadness"));
                                persons.get(0).getEmotion(4).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("disgust"));
                                persons.get(0).getEmotion(5).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("anger"));
                                persons.get(0).getEmotion(6).addInstant(time + analysis.getStart(), windowMeanScores.getDouble("fear"));

                                long increment = (fragmentsJsonArray.getJSONObject(i).getLong("interval")* 1000 / timescale);
                                //updating time of timestamp/frame(event)
                                time = time + increment;
                            }
                        }
                    }
                    
                    Double lastValue = persons.get(0).getEmotion(0).getInstants().get(persons.get(0).getEmotion(0).getInstants().size()-1).getValue();
                    
                            persons.get(0).getEmotion(0).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(1).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(2).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(3).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(4).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(5).addInstant(analysis.getEnd()+1, lastValue);
                            persons.get(0).getEmotion(6).addInstant(analysis.getEnd()+1, lastValue);
                            
                    analysis.setPersons(persons);
                    analysis.setOriginalBody(response);
                    analysis.setStatus("Completado");
                    return analysis;
                } else {
                    System.out.println("status: " + analysisStatus);
                    return null;
                }

            } else {
                System.out.println(statusCode + ": " + response.getStatusText());
                return null;
            }

        } catch (UnirestException ex) {
            Logger.getLogger(MicrosoftCognitiveAnalyser.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public Future<FacialAnalysis> updateAsync(FacialAnalysis analysis) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        return executor.submit(new MicrosoftCognitiveAsync(this, "update", analysis));
    }

}
