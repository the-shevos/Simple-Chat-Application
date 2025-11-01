package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader serverLoader = new FXMLLoader(getClass().getResource("/view/server.fxml"));
        Scene serverScene = new Scene(serverLoader.load());

        Stage serverStage = new Stage();
        serverStage.setTitle("Server");
        serverStage.setScene(serverScene);
        serverStage.centerOnScreen();
        serverStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
