package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class ServerController {

    @FXML private Button emojiBtn;
    @FXML private Pane emojiPane;
    @FXML private TextField massagePut;
    @FXML private ScrollPane scrollpane;
    @FXML private Button sendButton;
    @FXML private VBox messageArea;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();

    @FXML
    public void initialize() {
        emojiPane.setVisible(false);
        scrollpane.setContent(messageArea);
        messageArea.setSpacing(8);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(6000);
                showMessage("✅ Server started on port 6000");

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    handler.start();
                    showMessage("👤 New Client Connected: " + clientSocket.getInetAddress());
                }
            } catch (IOException e) {
                showMessage("⚠️ Server stopped: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    public void addClientAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Client");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showMessage("❌ Failed to open client: " + e.getMessage());
        }
    }

    @FXML
    void btnImageOnAction(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                broadcast("[IMAGE]", "Server", data);
                showMessage("Server sent an image:");
                showImage(data);
            } catch (IOException e) {
                showMessage("❌ Failed to send image.");
            }
        }
    }

    @FXML
    void sendButtonOnAcc(ActionEvent event) {
        String text = massagePut.getText().trim();
        if (!text.isEmpty()) {
            broadcast("[TEXT]", "Server: " + text);
            showMessage("Server: " + text);
            massagePut.clear();
        }
    }

    @FXML void emojiBtnOnAcc(ActionEvent event) { emojiPane.setVisible(!emojiPane.isVisible()); }
    @FXML void emoji1OnAcc(ActionEvent event) { massagePut.appendText("😀"); }
    @FXML void emoji2OnAcc(ActionEvent event) { massagePut.appendText("😂"); }
    @FXML void emoji3OnAcc(ActionEvent event) { massagePut.appendText("😎"); }
    @FXML void emoji4OnAcc(ActionEvent event) { massagePut.appendText("🤔"); }
    @FXML void emoji5OnAcc(ActionEvent event) { massagePut.appendText("🥰"); }
    @FXML void emoji6OnAcc(ActionEvent event) { massagePut.appendText("😭"); }
    @FXML void emoji7OnAcc(ActionEvent event) { massagePut.appendText("🤐"); }
    @FXML void emoji8OnAcc(ActionEvent event) { massagePut.appendText("😡"); }

    private void showMessage(String text) {
        Platform.runLater(() -> {
            Label label = new Label(text);
            label.setWrapText(true);
            messageArea.getChildren().add(label);
            scrollpane.setVvalue(1.0);
        });
    }

    private void showImage(byte[] data) {
        Platform.runLater(() -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                Image img = new Image(is);
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                messageArea.getChildren().add(imageView);
                scrollpane.setVvalue(1.0);
            } catch (IOException e) {
                showMessage("⚠️ Error displaying image.");
            }
        });
    }

    private void broadcast(String type, String text) {
        for (ClientHandler c : clients) {
            try {
                c.send(type, text);
            } catch (IOException ignored) {}
        }
    }

    private void broadcast(String type, String sender, byte[] data) {
        for (ClientHandler c : clients) {
            try {
                c.send(type, sender, data);
            } catch (IOException ignored) {}
        }
    }

    class ClientHandler extends Thread {
        private final Socket socket;
        private final DataInputStream input;
        private final DataOutputStream output;
        private final int clientId;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
            this.clientId = new Random().nextInt(10000);
            send("[TEXT]", "Welcome! Your Client ID: " + clientId);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String type = input.readUTF();
                    if (type.equals("[TEXT]")) {
                        String text = input.readUTF();
                        showMessage("Client " + clientId + ": " + text);
                        broadcast("[TEXT]", "Client " + clientId + ": " + text);
                    } else if (type.equals("[IMAGE]")) {
                        int len = input.readInt();
                        byte[] data = input.readNBytes(len);
                        showMessage("Client " + clientId + " sent an image:");
                        showImage(data);
                        broadcast("[IMAGE]", "Client " + clientId, data);
                    }
                }
            } catch (IOException e) {
                showMessage("Client " + clientId + " disconnected.");
                clients.remove(this);
            }
        }

        void send(String type, String text) throws IOException {
            output.writeUTF(type);
            output.writeUTF(text);
        }

        void send(String type, String sender, byte[] data) throws IOException {
            output.writeUTF(type);
            output.writeUTF(sender);
            output.writeInt(data.length);
            output.write(data);
        }
    }
}
