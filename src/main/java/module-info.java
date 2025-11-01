module org.example.controller {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.controller to javafx.fxml;
    exports org.example.controller;
}