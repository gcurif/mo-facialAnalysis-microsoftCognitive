package mo.facialanalysis.microsoftcognitive;


import bibliothek.util.xml.XElement;
import bibliothek.util.xml.XIO;
import facialAnalysisCore.FacialAnalysis;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import mo.analysis.AnalysisProvider;
import mo.core.plugin.Extends;
import mo.core.plugin.Extension;
import mo.organization.Configuration;
import mo.organization.ProjectOrganization;
import static mo.organization.ProjectOrganization.logger;
import mo.organization.StagePlugin;


@Extension(
    xtends = {
            @Extends(
                    extensionPointId = "mo.analysis.AnalysisProvider"
                    )
             }
            )


public class MicrosoftFacialAnalysisPlugin implements AnalysisProvider{

    private List<Configuration> configs;

    public MicrosoftFacialAnalysisPlugin(){
        configs = new ArrayList<>();
    }
    
    @Override
    public String getName() {return "Microsoft Cognitive plugin";}

    @Override
    public Configuration initNewConfiguration(ProjectOrganization organization) {

        
        MicrosoftFacialAnalysisConfigDialog d = new MicrosoftFacialAnalysisConfigDialog();
        boolean accepted = d.showDialog();
        
        if (accepted) {
            MicrosoftFacialAnalysisConfig c = new MicrosoftFacialAnalysisConfig(d.getConfigurationName(), (MicrosoftCognitiveAnalyser) d.getAnalyzer(),organization);
            configs.add(c);            
            return c;
        }
        return null;
    }

    @Override
    public List<Configuration> getConfigurations() {return configs;}

    @Override
    public StagePlugin fromFile(File file) {

        File ol =  new File(file.getParentFile().getParentFile().getPath());        
        if (file.isFile()) {
            try {

                MicrosoftFacialAnalysisPlugin mc = new MicrosoftFacialAnalysisPlugin();
                XElement root = XIO.readUTF(new FileInputStream(file));
                XElement[] pathsX = root.getElements("path");
                for (XElement pathX : pathsX) {
                    String path = pathX.getString(); 
                    MicrosoftFacialAnalysisConfig c = new MicrosoftFacialAnalysisConfig(new File(path).getParentFile());
                    c.setOrganizationLocation(ol);
                    Configuration config = c.fromFile(new File(file.getParentFile(), path));
                                      
                    if (config != null) {
                        mc.configs.add(config);
                    }
                }
                return mc;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } 
        }
        return null;        
        
     }

    @Override
    public File toFile(File parent) {
    
        File file = new File(parent, "MicrosoftCognitiveFacial-analysis.xml");
        if (!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        XElement root = new XElement("analysis");
        for (Configuration config : configs) {
            File p = new File(parent, "MicrosoftCognitiveFacial-analysis");
            p.mkdirs();
            File f = config.toFile(p);

            XElement path = new XElement("path");
            Path parentPath = parent.toPath();
            Path configPath = f.toPath();
            path.setString(parentPath.relativize(configPath).toString());
            root.addElement(path);
        }
        try {
            XIO.writeUTF(root, new FileOutputStream(file));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return file;      
    
    
    
    
    }
    
    

    
}
