module com.opau.dirasz2.dirasz2gui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.opau.dirasz2.dirasz2gui to javafx.fxml;
    exports com.opau.dirasz2.dirasz2gui;
}