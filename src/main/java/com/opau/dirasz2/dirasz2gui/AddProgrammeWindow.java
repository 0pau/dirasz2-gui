package com.opau.dirasz2.dirasz2gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;

public class AddProgrammeWindow {

    //region window constants
    App app;
    Scene scene;
    Stage s;
    VBox fileProps;
    VBox playlistProps;
    VBox manualProps;

    //endregion
    //region props
    Programme.Type programmeType = Programme.Type.FILE;
    int startTime = -1;
    int duration = 0;
    String label = "";
    String fileProgrammePath = "";
    String playListProgrammePath = "";
    int playListProgrammePlayCount = 0;
    PlaylistProgramme.Order playListProgrammeOrder = PlaylistProgramme.Order.LINEAR;
    //endregion

    public AddProgrammeWindow(App a) throws IOException {
        app = a;
        s = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("add_item_window.fxml"));
        scene = new Scene(fxmlLoader.load());
        s.setResizable(true);
        s.setScene(scene);
        s.initOwner(app.scene.getWindow());
        s.initModality(Modality.APPLICATION_MODAL);
        initItems();
        s.showAndWait();
    }

    void initItems() {

        Button ok = (Button) scene.lookup("#saveButton");
        ok.setOnMouseClicked((e)->{
            save();
        });

        ComboBox<String> combo = (ComboBox<String>) scene.lookup("#programmeTypeCombo");
        combo.getItems().add("Fájl");
        combo.getItems().add("Lejátszási lista");
        combo.getItems().add("Kézi vezérlés");
        combo.getSelectionModel().select(0);

        combo.getSelectionModel().selectedIndexProperty().addListener((opt, oldVal, newVal)->{
            switch (newVal.intValue()) {
                case 0:
                    hideAllProps();
                    fileProps.setVisible(true);
                    fileProps.setManaged(true);
                    break;
                case 1:
                    hideAllProps();
                    playlistProps.setVisible(true);
                    playlistProps.setManaged(true);
                    break;
                case 2:
                    hideAllProps();
                    manualProps.setVisible(true);
                    manualProps.setManaged(true);
                    break;
            }
            s.sizeToScene();
        });

        fileProps = (VBox) scene.lookup("#fileProgrammeProps");
        playlistProps = (VBox) scene.lookup("#playlistProgrammeProps");
        manualProps = (VBox) scene.lookup("#manualProgrammeProps");
        hideAllProps();
        fileProps.setVisible(true);
        fileProps.setManaged(true);

        ComboBox<String> combo2 = (ComboBox<String>) scene.lookup("#playlistDirectionCombo");
        combo2.getItems().add("Sorrendben");
        combo2.getItems().add("Véletlenszerűen");
        combo2.getSelectionModel().select(0);

        Button startTimeSetButton = (Button) scene.lookup("#startTimeSetButton");
        startTimeSetButton.setOnMouseClicked((e)->{
            TimePicker tp = new TimePicker(scene, false,0);
            int t = tp.show("Alapértelmezett");
            Label l = (Label) scene.lookup("#startTimeLabel");
            if (t > -1) {
                startTime = t;
                l.setText(Utils.formatTimeInt(t,false));
            } else if (t == -2) {
                startTime = -1;
                l.setText("Az utolsó után");
            }
        });

        Button programmeDurationSetButton = (Button) scene.lookup("#programmeDurationSetButton");
        programmeDurationSetButton.setOnMouseClicked((e)->{
            TimePicker tp = new TimePicker(scene, true, 0);
            int t = tp.show(null);
            Label l = (Label) scene.lookup("#programmeDurationLabel");
            if (t > -1) {
                duration = t;
                l.setText(Utils.formatTimeInt(t,true));
            }
        });

        Button fileProgrammePathButton = (Button) scene.lookup("#fileProgrammePathButton");
        fileProgrammePathButton.setOnMouseClicked((e)->{
            Label l = (Label) scene.lookup("#fileProgrammePathLabel");
            TextField tf = (TextField) scene.lookup("#programmeLabelField");
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hullámformátumú fájlok","*.wav"));
            fc.setTitle("Válassza ki a fájlt");
            File f = fc.showOpenDialog(scene.getWindow());
            if (f != null) {
                fileProgrammePath = f.getPath();
                l.setText(fileProgrammePath);
                if (tf.getText().isEmpty()) {
                    tf.setText(fileProgrammePath.substring(fileProgrammePath.lastIndexOf("/")+1, fileProgrammePath.lastIndexOf(".")));
                }
            }
        });

        Button playlistProgrammePathButton = (Button) scene.lookup("#playlistProgrammePathButton");
        playlistProgrammePathButton.setOnMouseClicked((e)->{
            Label l = (Label) scene.lookup("#playlistProgrammePathLabel");
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Válassza ki a fájlt");
            File f = dc.showDialog(scene.getWindow());
            if (f != null) {
                playListProgrammePath = f.getPath();
                l.setText(playListProgrammePath);
            }
        });
    }

    void hideAllProps() {
        fileProps.setManaged(false);
        fileProps.setVisible(false);
        playlistProps.setManaged(false);
        playlistProps.setVisible(false);
        manualProps.setManaged(false);
        manualProps.setVisible(false);
    }

    void save() {
        try {
            TextField programmeLabelField = (TextField) scene.lookup("#programmeLabelField");
            label = programmeLabelField.getText();
            if (label.isEmpty()) {
                throw new Exception("A műsor címe nem lehet üres.");
            }

            ComboBox<String> typeCombo = (ComboBox<String>) scene.lookup("#programmeTypeCombo");
            switch (typeCombo.getSelectionModel().getSelectedIndex()) {
                case 0:
                    programmeType = Programme.Type.FILE;
                    if (fileProgrammePath.isEmpty()) {
                        throw new Exception("Adja meg a fájl elérési útvonalát!");
                    }
                    break;
                case 1:
                    programmeType = Programme.Type.PLAYLIST;
                    TextField playlistPlayCountField = (TextField) scene.lookup("#playlistPlayCountField");
                    playListProgrammePlayCount = Integer.parseInt(playlistPlayCountField.getText());
                    if (playListProgrammePath.isEmpty()) {
                        throw new Exception("Adja meg a lejátszási lista fájljainak elérési útvonalát!");
                    }
                    if (playListProgrammePlayCount < 1) {
                        throw new Exception("A lejátszandó elemek száma nem megfelelő.");
                    }
                    ComboBox<String> combo2 = (ComboBox<String>) scene.lookup("#playlistDirectionCombo");
                    switch (combo2.getSelectionModel().getSelectedIndex()) {
                        case 0:
                            playListProgrammeOrder = PlaylistProgramme.Order.LINEAR;
                            break;
                        case 1:
                            playListProgrammeOrder = PlaylistProgramme.Order.SHUFFLE;
                            break;
                    }
                    break;
                case 2:
                    programmeType = Programme.Type.MANUAL;
                    if (duration < 1) {
                        throw new Exception("A műsor hossza nem megfelelő.");
                    }
                    break;
            }

            Programme p = null;

            switch (programmeType) {
                case FILE:
                    p = new FileProgramme(fileProgrammePath);
                    break;
                case PLAYLIST:
                    p = new PlaylistProgramme(playListProgrammePath, playListProgrammePlayCount, playListProgrammeOrder);
                    break;
                case MANUAL:
                    p = new ManualProgramme(app);
                    p.setLength(duration);
                    break;
            }

            p.setLabel(label);
            if (startTime == -1) {
                app.programmeController.enqueueAfterLast(p);
            } else {
                p.setStartTime(startTime);
                app.programmeController.enqueue(p);
            }

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Hiba");
            alert.setContentText(e.getMessage());
            alert.initOwner(s.getOwner());
            alert.showAndWait();
            return;
        }

        s.close();
    }

}
