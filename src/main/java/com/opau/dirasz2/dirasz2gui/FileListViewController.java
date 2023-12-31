package com.opau.dirasz2.dirasz2gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.IkonResolver;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.opau.dirasz2.dirasz2gui.FileListViewController.ListItemComparator.OrderType;

public class FileListViewController {

    ListView<ListItem> list;
    App app;
    public FileListViewController(App app) {

        this.app = app;
        list = (ListView<ListItem>) app.scene.lookup("#fileList");
        try {
            listFiles(System.getProperty("user.home"));
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Hiba történt a mappa megnyitása során");
            a.show();
        }
        list.setCellFactory(i -> new ListItemCell());
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ListItem itm = list.getSelectionModel().getSelectedItem();
                if (itm.type.equals("DIR")) {
                    try {
                        listFiles(itm.path);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (itm.type.equals("remixal-file-music-fill")) {
                    Programme p = null;
                    try {
                        p = new FileProgramme(itm.path);
                    } catch (UnsupportedAudioFileException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    app.programmeController.enqueueAfterLast(p);
                } else {
                    //load macro
                }
            }
        });
    }

    void listFiles(String path) throws IOException {
        list.getItems().clear();
        File entry = new File(path);
        if (entry.getParent() != null) {
            list.getItems().add(new ListItem("..", entry.getParent(), "DIR"));
        }

        for (File e : entry.listFiles()) {
            ListItem itm = new ListItem(e.getName(), e.getAbsolutePath(), "");
            if (e.getName().startsWith(".") && e != null) {
                continue;
            }
            if (e.isDirectory()) {
                itm.type = "DIR";
            } else {
                if (isAudioFile(e.getName())) {
                    itm.type = "remixal-file-music-fill";
                } else if (e.getName().endsWith(".dprg")) {
                    itm.type = "remixal-file-list-2-fill";
                } else {
                    continue;
                }
            }
            list.getItems().add(itm);
        }
        list.getItems().sort(new ListItemComparator(OrderType.BY_NAME));
        list.getItems().sort(new ListItemComparator(OrderType.BY_TYPE));
    }

    public class ListItemComparator implements Comparator<ListItem> {
        OrderType type = OrderType.BY_NAME;
        public ListItemComparator() {}
        public ListItemComparator(OrderType type) {
            this.type = type;
        }

        @Override
        public int compare(ListItem o1, ListItem o2) {
            if (type == OrderType.BY_NAME) {
                return o1.displayName.toLowerCase().compareTo(o2.displayName.toLowerCase());
            } else {
                if (o1.type.equals("DIR") && !o2.type.equals("DIR")) {
                    return -1;
                } else if (!o1.type.equals("DIR") && o2.type.equals("DIR")) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        public enum OrderType {BY_NAME,BY_TYPE}
    }

    boolean isAudioFile(String n) {
        //return (n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".wav"));
        return (n.endsWith(".wav"));
    }

    public class ListItemCell extends ListCell<ListItem> {
        //private FontIcon icon = new FontIcon();

        @Override
        protected void updateItem(ListItem listItem, boolean b) {
            super.updateItem(listItem, b);
            FontIcon fi = new FontIcon();
            if (b) {
                setGraphic(null);
                setText(null);
            } else {
                //icon.setIconLiteral("di-java");
                //icon.setImage();
                if (listItem.type == "DIR") {
                    fi.setIconLiteral("remixal-folder-3-fill");
                } else {
                    fi.setIconLiteral(listItem.type);
                }
                fi.setIconSize(16);
                setGraphic(fi);
                setText(listItem.toString());
            }
        }
    }

    public class ListItem {

        String displayName;
        String path;
        String type;

        public ListItem(String disp, String p, String t) {
            displayName = disp;
            path = p;
            type = t;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}
