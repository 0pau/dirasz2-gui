package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

import javax.sound.sampled.*;
import java.util.ArrayList;

public class AudioSourcesListManager {
    App app;
    VBox container;

    public AudioSourcesListManager(App a) {
        app = a;
        container = (VBox) app.scene.lookup("#audioSourcesList");
        a.lineMixer.setListener((int id, String uuid)->{
            Platform.runLater(()->{
                if (id == 0) {
                    VBox v = new VBox();
                    v.setSpacing(5);
                    Label l = new Label();
                    l.setText(a.lineMixer.getInputSourceForUuid(uuid).name);
                    v.getChildren().add(l);
                    Slider slider = new Slider();
                    v.getChildren().add(slider);
                    v.setUserData(uuid);
                    container.getChildren().add(v);
                } else {
                    for (Node n : container.getChildren()) {
                        if (((String)n.getUserData()).equals(uuid)) {
                            container.getChildren().remove(n);
                            break;
                        }
                    }
                }
            });
        });
    }
}
