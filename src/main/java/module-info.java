module org.example.simplechatapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.simplechatapp to javafx.fxml;
    exports org.example.simplechatapp;
}