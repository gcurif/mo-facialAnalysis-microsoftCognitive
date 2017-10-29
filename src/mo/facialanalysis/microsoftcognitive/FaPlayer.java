/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mo.facialanalysis.microsoftcognitive;

import facialAnalysisCore.FacialAnalyser;
import facialAnalysisCore.FacialAnalysis;
import facialAnalysisCore.Instant;
import facialAnalysisCore.Person;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mo.core.ui.dockables.DockableElement;
import mo.core.ui.dockables.DockablesRegistry;
import mo.visualization.Playable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import mo.core.ui.dockables.DockableElement;
import mo.core.ui.dockables.DockablesRegistry;
import mo.visualization.Playable;

/**
 *
 * @author gustavo
 */
public class FaPlayer implements Playable {

    private long start;
    private long end;
    private boolean stopped = false;

    private Instant currentInstant;
    private Instant nextInstant;

    ArrayList<Instant> source;
    Person personSource;
    FacialAnalyser analyser;

    FaDisplayPanel faPanel;
    JPanel pan;

    int positionMarker;
    boolean stoped;
    

    private static final Logger logger = Logger.getLogger(FaPlayer.class.getName());


    
    public FaPlayer(FacialAnalysis source, FacialAnalyser analyser) {

        this.analyser = analyser;
        this.positionMarker = 0;
        this.stoped = false;

        if (source!= null) {

                this.start = source.getPerson(0).getEmotion(0).getInstants().get(positionMarker).getTime();

                int length = source.getPerson(0).getEmotion(0).getInstants().size();
                this.end = source.getPerson(0).getEmotion(0).getInstants().get(length - 1).getTime();

                this.personSource = source.getPerson(0);
                faPanel = new FaDisplayPanel(source.getPerson(0),source.getStart());

                this.currentInstant = source.getPerson(0).getEmotion(0).getInstants().get(positionMarker);
                this.nextInstant = source.getPerson(0).getEmotion(0).getInstants().get(positionMarker + 1);

                pan = (JPanel) faPanel;

                SwingUtilities.invokeLater(() -> {
                    try {
                        DockableElement e = new DockableElement();
                        e.add(pan);
                        
                        DockablesRegistry.getInstance().addAppWideDockable(e);
                    } catch (Exception ex) {
                        logger.log(Level.INFO, null, ex);
                    }
                });
        }        
        

    }
    

    public FaPlayer(File sourceFile, FacialAnalyser analyser) {

        this.analyser = analyser;
        this.positionMarker = 0;
        this.stoped = false;

        if (sourceFile != null) {
            if (!sourceFile.isDirectory()) {

                
                FacialAnalysis a2 = analyser.analysisFromFile(sourceFile.getPath());

                this.start = a2.getPerson(0).getEmotion(0).getInstants().get(positionMarker).getTime();

                int length = a2.getPerson(0).getEmotion(0).getInstants().size();
                this.end = a2.getPerson(0).getEmotion(0).getInstants().get(length - 1).getTime();

                this.personSource = a2.getPerson(0);
                faPanel = new FaDisplayPanel(a2.getPerson(0),a2.getStart());

                this.currentInstant = a2.getPerson(0).getEmotion(0).getInstants().get(positionMarker);
                this.nextInstant = a2.getPerson(0).getEmotion(0).getInstants().get(positionMarker + 1);

                pan = (JPanel) faPanel;

                SwingUtilities.invokeLater(() -> {
                    try {
                        DockableElement e = new DockableElement();
                        e.add(pan);
                        DockablesRegistry.getInstance().addAppWideDockable(e);
                    } catch (Exception ex) {
                        logger.log(Level.INFO, null, ex);
                    }
                });

            }
        }
    }

    public FaPlayer(Person source) {

        this.personSource = source;
        this.analyser = null;
        this.positionMarker = 0;

        pan = (JPanel) new FaDisplayPanel(source,source.getEmotion(0).getInstants().get(0).getTime());

        SwingUtilities.invokeLater(() -> {
            try {
                DockableElement e = new DockableElement();
                e.add(pan);
                DockablesRegistry.getInstance().addAppWideDockable(e);
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        });

    }

    public FaPlayer(String sourcePath) {

        //completar
        File source = new File(sourcePath);

        if (source != null) {
            if (source.isFile() && source.canRead()) {

            } else {
                this.source = null;
            }

        } else {
            this.source = null;
        }
    }

    public FaPlayer(String sourcePath, FacialAnalyser analyser) {
        //completar

        File source = new File(sourcePath);
        this.analyser = analyser;

        if (source != null && this.analyser != null) {
            if (source.isFile() && source.canRead()) {

            } else {
                this.source = null;
            }

        } else {
            this.source = null;
        }
    }

    @Override
    public long getStart() {
        return this.start;
    }

    @Override
    public long getEnd() {
        return this.end;
    }

    @Override
    public void play(long millis) {

        System.out.println("1millis: " + millis + ", current: " + this.currentInstant.getValue().longValue());

        if (!stoped) {
            while (millis > this.currentInstant.getTime()) {

                positionMarker++;
                if(positionMarker< this.personSource.getEmotion(0).getInstants().size()){
                    this.currentInstant = this.personSource.getEmotion(0).getInstants().get(positionMarker);
                    this.faPanel.seekMarker(millis);
                }else{
                //esto es para salir del while sin utilizar un break
                  millis=0;
                }

                System.out.println("2millis: " + millis + ", current: " + this.currentInstant.getTime());

            }
        } else {
            stoped = false;
            play(0);
        }

    }

    @Override
    public void pause() {
    }

    @Override
    public void seek(long desiredMillis) {
        this.currentInstant = this.getCompatibleInstant(personSource.getEmotion(0).getInstants(), desiredMillis);
        //this.faPanel.seekMarker(new Double(desiredMillis * 10));
        this.faPanel.seekMarker(desiredMillis);

    }

    @Override
    public void stop() {
        this.stoped = true;
        this.positionMarker = 0;
        this.currentInstant = this.personSource.getEmotion(0).getInstants().get(positionMarker);
    }

    public Instant getCompatibleInstant(ArrayList<Instant> instants, long millis) {

        for (int i = 0; i < instants.size(); i++) {

            if (instants.get(i).getTime() >= millis) {
                this.positionMarker = i;
                return instants.get(i);
            }
        }

        return null;
    }
}
