package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML private TextField messageInput;
    @FXML private ScrollPane scrollPane;
    @FXML private Button sendButton;
    @FXML private VBox messageBox;
    @FXML private Label headerLabel;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    @FXML
    public void initialize() {
        emojiPane.setVisible(false);
        scrollPane.setContent(messageBox);
        messageBox.setSpacing(5);
        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 6000);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            showMessage("✅ Connected to Server");
            while (true) {
                String type = input.readUTF();
                switch (type) {
                    case "[TEXT]":
                        String msg = input.readUTF();
                        showMessage(msg);
                        break;
                    case "[IMAGE]":
                        String sender = input.readUTF();
                        int len = input.readInt();
                        byte[] data = input.readNBytes(len);
                        showMessage(sender + " sent an image:");
                        showImage(data);
                        break;
                }
            }
        } catch (IOException e) {
            showMessage("⚠️ Disconnected from server.");
            closeClient();
        }
    }

    private void closeClient() {
        Platform.runLater(() -> {
            try {
                if (socket != null) socket.close();
                Stage stage = (Stage) sendButton.getScene().getWindow();
                stage.close();
            } catch (IOException ignored) {}
        });
    }

    @FXML void emojiBtnOnAcc(ActionEvent event) { emojiPane.setVisible(!emojiPane.isVisible()); }
    @FXML void emoji1OnAcc(ActionEvent event) { messageInput.appendText("😁"); }
    @FXML void emoji2OnAcc(ActionEvent event) { messageInput.appendText("🤣"); }
    @FXML void emoji3OnAcc(ActionEvent event) { messageInput.appendText("😆"); }
    @FXML void emoji4OnAcc(ActionEvent event) { messageInput.appendText("😅"); }
    @FXML void emoji5OnAcc(ActionEvent event) { messageInput.appendText("😊"); }
    @FXML void emoji6OnAcc(ActionEvent event) { messageInput.appendText("😍"); }
    @FXML void emoji7OnAcc(ActionEvent event) { messageInput.appendText("😎"); }
    @FXML void emoji8OnAcc(ActionEvent event) { messageInput.appendText("😜"); }

    @FXML
    void sendButtonOnAcc(ActionEvent event) {
        try {
            String text = messageInput.getText().trim();
            if (!text.isEmpty()) {
                output.writeUTF("[TEXT]");
                output.writeUTF(text);
                messageInput.clear();
            }
        } catch (IOException e) {
            showMessage("❌ Message send failed.");
        }
    }

    @FXML
    public void imageUploadOnAcc(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                output.writeUTF("[IMAGE]");
                output.writeUTF("You");
                output.writeInt(data.length);
                output.write(data);
                showMessage("You sent an image:");
                showImage(data);
            } catch (IOException e) {
                showMessage("❌ Image send failed.");
            }
        }
    }

    private void showMessage(String text) {
        Platform.runLater(() -> {
            Label label = new Label(text);
            label.setWrapText(true);
            messageBox.getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }

    private void showImage(byte[] data) {
        Platform.runLater(() -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                Image img = new Image(is);
                ImageView view = new ImageView(img);
                view.setFitWidth(180);
                view.setPreserveRatio(true);
                messageBox.getChildren().add(view);
                scrollPane.setVvalue(1.0);
            } catch (Exception e) {
                showMessage("⚠️ Image display error.");
            }
        });
    }
}
