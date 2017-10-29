package mo.facialanalysis.microsoftcognitive;





import facialAnalysisCore.SendVideoWindow;
import com.mashape.unirest.http.exceptions.UnirestException;
import facialAnalysisCore.Emotion;
import facialAnalysisCore.FacialAnalyser;
import facialAnalysisCore.FacialAnalysis;
import facialAnalysisCore.FileDescriptorMaker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
//import facialCore.FacialAnalyzer;
//import facialTools.FacialAnalysis;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import mo.analysis.IndividualAnalysisConfiguration;
import mo.analysis.PlayableAnalyzableConfiguration;
import mo.organization.Configuration;
import mo.organization.Participant;
import mo.organization.ProjectOrganization;
import static mo.organization.ProjectOrganization.logger;
import mo.visualization.Playable;



public class MicrosoftFacialAnalysisConfig implements IndividualAnalysisConfiguration, PlayableAnalyzableConfiguration{

    String name;
    FacialAnalyser analyser;
    ProjectOrganization organization;
    private final String[] creators = {"webcamcaptureplugin.WebcamRecorder",
                                       "mo.facialanalysis.microsoftcognitive.MicrosoftCognitiveAnalyser.complete"};   
    
   private File stageFolder;
   private Participant participant;
   private ArrayList<File> files;
   private FaPlayer player;
    
   private String key;
   private File organizationLocation;

    public MicrosoftFacialAnalysisConfig(){
    }
    
    
    public MicrosoftFacialAnalysisConfig(String name, MicrosoftCognitiveAnalyser analyser, ProjectOrganization organization) {
        this.name = name;
        this.analyser = analyser;
        this.files =  new ArrayList<File>();
        this.player = null;
        this.key = analyser.getKey();  
        this.organizationLocation = organization.getLocation();
        this.organization = organization;
    }

    
    public MicrosoftFacialAnalysisConfig(String name) {
        this.name = name;
    }
    
    public MicrosoftFacialAnalysisConfig(String name, MicrosoftCognitiveAnalyser analyser, File organizationLocation) {
        
        this.name = name;
        this.analyser = analyser;
        this.files =  new ArrayList<File>();
        this.player = null;
        this.key = analyser.getKey();  
        this.organizationLocation = organizationLocation;
    }
    
    public MicrosoftFacialAnalysisConfig(File organizationLocation) {

        this.files =  new ArrayList<File>();
        this.player = null;  
        this.organizationLocation = organizationLocation;
    }       

    public void setOrganizationLocation(File organizationLocation) {
        this.organizationLocation = organizationLocation;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
    @Override
    public void initIndividualAnalysis(Participant participant)  {
     
        File dir =  new  File(this.organizationLocation.getPath()+"\\"+participant.folder);

        SendVideoWindow w = new SendVideoWindow(dir,participant,this.analyser);
        w.setVisible(true);
        w.toFront();
        w.setAlwaysOnTop(true);
        
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public File toFile(File file) {
        File f = new File(file, "MicrosoftCognitiveFacial-analysis_"+name+".xml");
        try {

            f.createNewFile();
            FileWriter output = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(output);

            bw.write(this.name+"\n"); 
            bw.write(this.key+"\n");

            bw.close();
            output.close();
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return f;  
    }

    @Override
    public Configuration fromFile(File file) {
                
        FileReader fr ; 
        try {
            
            fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);    

            this.name = br.readLine();
            this.key = br.readLine();
    
            MicrosoftCognitiveAnalyser mca = new MicrosoftCognitiveAnalyser("https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognizeinvideo",
                                                                         "https://westus.api.cognitive.microsoft.com/emotion/v1.0/operations/",
                                                                         this.key);   
            
            MicrosoftFacialAnalysisConfig config = new MicrosoftFacialAnalysisConfig(this.name,mca,this.organizationLocation);
            
            br.close();
            fr.close();
            
            return config;
            
       } catch (FileNotFoundException ex) {
           Logger.getLogger(MicrosoftFacialAnalysisConfig.class.getName()).log(Level.SEVERE, null, ex);
       } catch (IOException ex) {
           Logger.getLogger(MicrosoftFacialAnalysisConfig.class.getName()).log(Level.SEVERE, null, ex);
       }   
       return null;

    }

    @Override
    public void setupAnalysis(File file, ProjectOrganization po, Participant p) {
           this.stageFolder = file;
           this.organizationLocation = po.getLocation();
           this.participant = p;
    }

    @Override
    public void startAnalysis() {
        
        TimeStampsDialog t = new TimeStampsDialog();
        t.setVisible(true);
        
        File inputFile = files.get(0);
        
        if(!inputFile.exists()){
            JOptionPane.showMessageDialog(null,"El archivo no existe");        
            return;        
        }
 
        if(inputFile.getName().endsWith(".json")){
            
            FacialAnalysis analysis = this.analyser.analysisFromFile(inputFile.getPath());
            if(analysis==null){
                JOptionPane.showMessageDialog(null,"Ha ocurrido un error con la lectura del archivo");                    
                return;
            }
            else{
                this.player = new FaPlayer(analysis,this.analyser);
            }
            return;
        }
        
        FacialAnalysis analysis = this.analyser.uploadVideo(inputFile);

        System.out.println(analysis.getStatus());
        
        if(analysis!=null){
            while(!analysis.getStatus().equals("Running")){
                System.out.println(analysis.getStatus());
                try {
                    Thread.sleep(7000);
                    } catch (InterruptedException ex) {
                    Logger.getLogger(MicrosoftFacialAnalysisConfig.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.analyser.update(analysis);
            }
        }else{
            JOptionPane.showMessageDialog(null,"Ha ocurrido un error al enviar o procesar el archivo"); 
            return;
        }
        
        String outputFileName = inputFile.getName().substring(inputFile.getName().indexOf("."));
        
        this.player = new FaPlayer(analysis,this.analyser);
        if(t.isAccepted()){
            for(Emotion e : analysis.getPerson(0).getEmotions()){
                e.makeTimeStamps(t.getSensitivity());
                e.timeStampsToFile(stageFolder.getPath()+"\\"+outputFileName+"_"+e.getName()+".txt");
                FileDescriptorMaker.makeFileDescriptor(new File(stageFolder.getPath()+"\\"+inputFile+"_"+e.getName()+".txt"),
                                                    "creator="+analyser.getClass().getName(),
                                                    "compatible=mo.analysis.NotesRecorder",
                                                    "captureFile="+this.organizationLocation.getPath()+"\\"+ this.participant.folder+ "\\capture\\"+ analysis.getVideoName());
            }
        }            }

    @Override
    public void cancelAnalysis() {
        Thread.currentThread().interrupt();
    }

    @Override
    public List<String> getCompatibleCreators() {
        return asList(creators);
    }

    @Override
    public void addFile(File file) {
        if (!files.contains(file)) {
            files.add(file);
        }    }

    @Override
    public void removeFile(File file) {
        File toRemove = null;
        for (File f : files) {
            if (f.equals(file)) {
                toRemove = f;
            }
        }
        
        if (toRemove != null) {
            files.remove(toRemove);
        }      }

    @Override
    public Playable getPlayer() {
        if(this.player == null){this.player = new FaPlayer(this.files.get(0),this.analyser); }
        return this.player;
    }


    
}
