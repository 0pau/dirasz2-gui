package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

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

                    HBox controls = new HBox();
                    controls.setSpacing(10);
                    controls.setAlignment(Pos.CENTER_LEFT);


                    //remixmz-user-voice-fill
                    FontIcon mute_icon = new FontIcon();
                    mute_icon.setIconLiteral("mdi2v-volume-mute");

                    FontIcon mute_monitor_icon = new FontIcon();
                    mute_monitor_icon.setIconLiteral("mdi2m-monitor-speaker-off");

                    ToggleButton mute_btn = new ToggleButton();
                    mute_btn.setPadding(new Insets(4));
                    mute_btn.setGraphic(mute_icon);
                    controls.getChildren().add(mute_btn);
                    mute_btn.selectedProperty().addListener((e)->{
                        app.lineMixer.getInputSourceForUuid((String)v.getUserData()).setEnabled(!mute_btn.isSelected());
                    });

                    ToggleButton mute_monitoring_btn = new ToggleButton();
                    mute_monitoring_btn.setPadding(new Insets(4));
                    mute_monitoring_btn.setGraphic(mute_monitor_icon);
                    controls.getChildren().add(mute_monitoring_btn);

                    Slider slider = new Slider();
                    slider.setValue(100);
                    HBox.setHgrow(slider, Priority.ALWAYS);
                    controls.getChildren().add(slider);
                    slider.valueProperty().addListener((d)->{
                        double val = slider.getValue();
                        mute_btn.setSelected((val == 0));
                        app.lineMixer.getInputSourceForUuid((String)v.getUserData()).volume = val;
                    });

                    v.getChildren().add(controls);
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
