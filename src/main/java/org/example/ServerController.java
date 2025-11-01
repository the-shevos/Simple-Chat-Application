package org.example;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ServerController {

    @FXML private Button emojiBtn;
    @FXML private Pane emojiPane;
    @FXML private TextField massagePut;
    @FXML private ScrollPane scrollpane;
    @FXML private Button sendButton;

    private ServerSocket serverSocket;
    private VBox messagebox = new VBox();
    private final List<ClientHandler> clients = new ArrayList<>();

    @FXML
    public void initialize() {
        emojiPane.setVisible(false);
        scrollpane.setContent(messagebox);
        messagebox.setSpacing(5);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(6000);
                showMessage("✅ Server started on port 6000");
                showMessage("📅 Date: " + new Date());

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    handler.start();
                    showMessage("👤 Client connected: " + clientSocket.getInetAddress());
                }
            } catch (IOException e) {
                showMessage("⚠️ Server stopped: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    public void addClientAction(ActionEvent actionEvent) {
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
    public void onServerClose(ActionEvent event) {
        closeServer();
    }

    private void closeServer() {
        try {
            for (ClientHandler c : clients) {
                c.send("[SERVER_CLOSED]", "Server is shutting down");
                c.close();
            }
            clients.clear();

            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            showMessage("🚪 Server closed");
            Platform.exit();
        } catch (IOException e) {
            showMessage("Error closing server: " + e.getMessage());
        }
    }

    class ClientHandler extends Thread {
        Socket socket;
        DataInputStream input;
        DataOutputStream output;
        int clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientId = new Random().nextInt(10000);
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
                send("[TEXT]", "Welcome! Your Client ID: " + clientId);
            } catch (IOException e) {
                showMessage("⚠️ Error initializing client handler");
            }
        }

        public void run() {
            try {
                while (true) {
                    String type = input.readUTF();
                    switch (type) {
                        case "[TEXT]":
                            String text = input.readUTF();
                            showMessage("Client " + clientId + ": " + text);
                            broadcast("[TEXT]", "Client " + clientId + ": " + text);
                            break;
                        case "[IMAGE]":
                            int len = input.readInt();
                            byte[] data = input.readNBytes(len);
                            showImage(data);
                            broadcast("[IMAGE]", data);
                            break;
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

        void send(String type, byte[] data) throws IOException {
            output.writeUTF(type);
            output.writeInt(data.length);
            output.write(data);
        }

        void close() throws IOException {
            socket.close();
        }
    }

    void broadcast(String type, String text) {
        for (ClientHandler c : clients) {
            try {
                c.send(type, text);
            } catch (IOException ignored) {}
        }
    }

    void broadcast(String type, byte[] data) {
        for (ClientHandler c : clients) {
            try {
                c.send(type, data);
            } catch (IOException ignored) {}
        }
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

    @FXML
    void sendButtonOnAcc(ActionEvent event) {
        try {
            String text = massagePut.getText().trim();
            if (!text.isEmpty()) {
                broadcast("[TEXT]", "Server: " + text);
                showMessage("Server: " + text);
                massagePut.clear();
            }
        } catch (Exception e) {
            showMessage("❌ Send failed");
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
                broadcast("[IMAGE]", data);
                showImage(data);
            } catch (IOException e) {
                showMessage("❌ Image send failed");
            }
        }
    }

    private void showImage(byte[] data) {
        Platform.runLater(() -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                Image img = new Image(is);
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(180);
                imageView.setPreserveRatio(true);
                messagebox.getChildren().add(imageView);
                scrollpane.setVvalue(1.0);
            } catch (Exception e) {
                showMessage("Image load error.");
            }
        });
    }

    private void showMessage(String text) {
        Platform.runLater(() -> {
            Label label = new Label(text);
            label.setWrapText(true);
            messagebox.getChildren().add(label);
            scrollpane.setVvalue(1.0);
        });
    }
}
