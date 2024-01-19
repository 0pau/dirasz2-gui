package com.opau.dirasz2.dirasz2gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Optional;

public class TimePicker {
    int time = -1;
    Stage s = new Stage();
    Scene scene;
    public TimePicker(Scene parent, boolean showSeconds, int t) {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("time_picker.fxml"));
        try {
            scene = new Scene(fxmlLoader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
        s.setResizable(false);
        s.setScene(scene);
        s.initOwner(parent.getWindow());
        s.initModality(Modality.APPLICATION_MODAL);

        Button ok = (Button) scene.lookup("#okButton");

        TextField hour = (TextField) scene.lookup("#hour");
        TextField minute = (TextField) scene.lookup("#minute");
        TextField seconds = (TextField) scene.lookup("#seconds");
        Label secondsTick = (Label) scene.lookup("#secondTick");

        int[] hms = Utils.getHMSFromTimeInt(t);
        hour.setText(String.valueOf(hms[0]));
        minute.setText(String.valueOf(hms[1]));
        seconds.setText(String.valueOf(hms[2]));

        seconds.setManaged(showSeconds);
        seconds.setVisible(showSeconds);
        secondsTick.setManaged(showSeconds);
        secondsTick.setVisible(showSeconds);

        ok.setOnMouseClicked((e)->{
            int h,m,sec = 0;
            try {
                h = Integer.parseInt(hour.getText());
                m = Integer.parseInt(minute.getText());
                sec = Integer.parseInt(seconds.getText());
                if (h < 0 || h > 23 || m < 0 || m > 59 || sec < 0 || sec > 59) {
                    throw new Exception();
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Hiba");
                alert.setContentText(String.format("A megadott formátum hibás"));
                alert.initOwner(s.getOwner());
                alert.showAndWait();
                return;
            }
            time = Utils.timeToInt(h,m,sec);
            s.close();
        });

        Button custom = (Button) scene.lookup("#customAction");
        custom.setOnMouseClicked((e)->{
            time = -2;
            s.close();
        });

        //initItems();
    }

    public int show(String customString) {

        Button custom = (Button) scene.lookup("#customAction");
        if (customString == null) {
            custom.setManaged(false);
            custom.setVisible(false);
        } else {
            custom.setText(customString);
        }

        s.showAndWait();
        return time;
    }
}
