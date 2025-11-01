package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ClientController {

    @FXML private Button emojiBtn;
    @FXML private Pane emojiPane;
    @FXML private TextField massagePut;
    @FXML private ScrollPane scrollpane;
    @FXML private Button sendButton;
    @FXML private Label headerLabel;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private VBox messagebox = new VBox();
    private int clientId;

    @FXML
    public void initialize() {
        emojiPane.setVisible(false);
        scrollpane.setContent(messagebox);
        messagebox.setSpacing(5);

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 6000);
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
                showMessage("Connected to server ✅");

                while (true) {
                    String type = input.readUTF();
                    switch (type) {
                        case "[TEXT]":
                            String msg = input.readUTF();
                            if (msg.contains("Your Client ID")) {
                                clientId = Integer.parseInt(msg.replaceAll("\\D+", ""));
                                Platform.runLater(() -> headerLabel.setText("Client ID: " + clientId));
                            }
                            showMessage(msg);
                            break;
                        case "[IMAGE]":
                            showImage(input.readNBytes(input.readInt()));
                            break;
                        case "[SERVER_CLOSED]":
                            showMessage("Server closed connection. Exiting...");
                            closeClient();
                            break;
                    }
                }
            } catch (IOException e) {
                showMessage("Disconnected.");
                closeClient();
            }
        }).start();
    }

    private void closeClient() {
        Platform.runLater(() -> {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
                Stage stage = (Stage) sendButton.getScene().getWindow();
                stage.close();
            } catch (IOException ignored) {}
        });
    }

    @FXML void emojiBtnOnAcc(ActionEvent event) {
        emojiPane.setVisible(!emojiPane.isVisible());
    }

    @FXML void emoji1OnAcc(ActionEvent event) { massagePut.appendText("😁"); }
    @FXML void emoji2OnAcc(ActionEvent event) { massagePut.appendText("🤣"); }
    @FXML void emoji3OnAcc(ActionEvent event) { massagePut.appendText("😆"); }
    @FXML void emoji4OnAcc(ActionEvent event) { massagePut.appendText("😅"); }
    @FXML void emoji5OnAcc(ActionEvent event) { massagePut.appendText("😊"); }
    @FXML void emoji6OnAcc(ActionEvent event) { massagePut.appendText("😍"); }
    @FXML void emoji7OnAcc(ActionEvent event) { massagePut.appendText("😎"); }
    @FXML void emoji8OnAcc(ActionEvent event) { massagePut.appendText("😜"); }

    @FXML void sendButtonOnAcc(ActionEvent event) {
        try {
            String text = massagePut.getText().trim();
            if (!text.isEmpty()) {
                output.writeUTF("[TEXT]");
                output.writeUTF(text);
                output.flush();
                massagePut.clear();
            }
        } catch (IOException e) {
            showMessage("Send failed.");
        }
    }

    @FXML public void imageUploadOonAcc(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                output.writeUTF("[IMAGE]");
                output.writeInt(data.length);
                output.write(data);
                output.flush();
            } catch (IOException e) {
                showMessage("Failed to send image.");
            }
        }
    }

    private void showMessage(String text) {
        Platform.runLater(() -> {
            Label label = new Label(text);
            label.setWrapText(true);
            messagebox.getChildren().add(label);
            scrollpane.setVvalue(1.0);
        });
    }

    private void showImage(byte[] data) {
        Platform.runLater(() -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                Image img = new Image(is);
                ImageView view = new ImageView(img);
                view.setFitWidth(180);
                view.setPreserveRatio(true);
                messagebox.getChildren().add(view);
                scrollpane.setVvalue(1.0);
            } catch (Exception e) {
                showMessage("Image error.");
            }
        });
    }
}
