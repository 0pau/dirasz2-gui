package com.opau.dirasz2.dirasz2gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

import javax.sound.sampled.*;
import java.util.ArrayList;

public class AudioSourcesListManager {
    App app;
    VBox container;
    ArrayList<InputAudioSource> sources = new ArrayList<>();

    public AudioSourcesListManager(App a) {
        app = a;
        container = (VBox) app.scene.lookup("#audioSourcesList");
        getAudioSources();
    }

    void getAudioSources() {
        sources.clear();
        container.getChildren().clear();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info i : infos) {
            Mixer m = AudioSystem.getMixer(i);
            for (Line.Info lineInfo : m.getSourceLineInfo()) {
                if (lineInfo instanceof Port.Info) {
                    sources.add(new InputAudioSource(i.getName().replace("Port ", ""), lineInfo));
                }
            }
        }

        for (InputAudioSource s : sources) {
            VBox v = new VBox();
            v.setPadding(new Insets(0,10,10,10));
            v.setSpacing(5);
            Label l = new Label();
            l.setText(s.name);
            v.getChildren().add(l);
            Slider slider = new Slider();
            v.getChildren().add(slider);
            container.getChildren().add(v);
        }
    }

    public class InputAudioSource {
        public String name;
        public Line.Info portInfo;

        public InputAudioSource(String n, Line.Info i) {
            name = n;
            portInfo = i;
        }
    }
}
