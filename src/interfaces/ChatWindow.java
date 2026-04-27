package interfaces;

import client.Client;
import models.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Écran de chat.
 * Appelle client.envoyerMessage(content, recipient) à l'envoi.
 * Reçoit les messages via client.setOnMessageReceived() → afficherMessage().
 */
public class ChatWindow {

    private String currentUser;
    private String contactName;
    private String contactInitials;
    private String contactColor;
    private Client client;

    private VBox messagesBox;
    private TextField inputField;

    public ChatWindow(String currentUser, String contactName,
                      String contactInitials, String contactColor,
                      Client client) {
        this.currentUser     = currentUser;
        this.contactName     = contactName;
        this.contactInitials = contactInitials;
        this.contactColor    = contactColor;
        this.client          = client;
    }

    public void start(Stage stage) {
        stage.setTitle("WhatsApp – " + contactName);

        // Header
        Label avatar = makeAvatar(contactInitials, contactColor);

        Label name = new Label(contactName);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#E9EDEF"));

        Label status = new Label("● En ligne");
        status.setFont(Font.font("Arial", 12));
        status.setTextFill(Color.web("#25D366"));

        VBox nameBox = new VBox(2, name, status);

        // Bouton appel audio
        Button callBtn = makeIconButton("📞");
        callBtn.setOnAction(e -> {
            call.CallManager cm = LoginWindow.getCallManager();
            if (cm != null) cm.demandeAppel(contactName, models.Call.TYPE_AUDIO);
            new AudioCallWindow(contactName, contactInitials, contactColor).start(new Stage());
        });
        // Bouton appel vidéo
        Button videoBtn = makeIconButton("📹");
        videoBtn.setOnAction(e -> {
            call.CallManager cm = LoginWindow.getCallManager();
            if (cm != null) cm.demandeAppel(contactName, models.Call.TYPE_VIDEO);
            new VideoCallWindow(contactName, contactInitials, contactColor).start(new Stage());
        });

        HBox header = new HBox(12, avatar, nameBox, new Pane(), callBtn, videoBtn);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: #202C33;");

        // Zone messages
        messagesBox = new VBox(6);
        messagesBox.setPadding(new Insets(14, 40, 14, 40));
        messagesBox.setStyle("-fx-background-color: #0B141A;");

        ScrollPane scroll = new ScrollPane(messagesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#0B141A; -fx-background:#0B141A;");
        scroll.vvalueProperty().bind(messagesBox.heightProperty());

        // Barre de saisie
        inputField = new TextField();
        inputField.setPromptText("Saisir un message");
        inputField.setStyle(
                "-fx-background-color:#2A3942; -fx-text-fill:#E9EDEF;" +
                        "-fx-prompt-text-fill:#667781; -fx-background-radius:10px;" +
                        "-fx-padding:9px 14px; -fx-font-size:13px;"
        );
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color:#25D366; -fx-text-fill:white;" +
                        "-fx-font-size:14px; -fx-background-radius:20px;" +
                        "-fx-min-width:40px; -fx-min-height:40px; -fx-cursor:hand;"
        );
        sendBtn.setOnAction(e -> sendMessage());

        HBox inputBar = new HBox(10,
                makeIconButton("😊"), makeIconButton("📎"),
                inputField, makeIconButton("🎙️"), sendBtn
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(8, 12, 8, 12));
        inputBar.setStyle("-fx-background-color: #202C33;");

        VBox root = new VBox(header, scroll, inputBar);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 520);
        stage.setScene(scene);
        stage.show();

        // Callback réception message
        // Appelé automatiquement par le thread réseau d'Afnane
        client.setOnMessageReceived(message -> {
            // Afficher seulement les messages de ce contact
            if (message.getSender().equals(contactName)) {
                Platform.runLater(() -> afficherMessage(message));
            }
        });
    }

    // Envoi — appelle client.envoyerMessage() d'Afnane

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // Afficher immédiatement dans l'UI (côté envoyeur)
        addBubble(text, true, currentUser, time);
        inputField.clear();

        // Envoyer via le vrai réseau
        client.envoyerMessage(text, contactName);
    }

    /**
     * Appelée par le callback onMessageReceived via Platform.runLater().
     * Reçoit un objet Message d'Afnane et l'affiche.
     */
    public void afficherMessage(Message message) {
        String time = message.getTimestamp()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        addBubble(message.getContent(), false, message.getSender(), time);
    }

    private void addBubble(String content, boolean isOut, String sender, String time) {
        // Format imposé par le cahier des charges §2.4
        String display = "[" + time + "] " + sender + " : " + content;
        if (isOut) display += "  ✓✓";

        Label bubble = new Label(display);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setFont(Font.font("Arial", 13));
        bubble.setPadding(new Insets(7, 10, 7, 10));
        bubble.setTextFill(Color.web("#E9EDEF"));
        bubble.setStyle(
                "-fx-background-color:" + (isOut ? "#005C4B" : "#202C33") + ";" +
                        "-fx-background-radius:8px;"
        );

        HBox row = new HBox(bubble);
        row.setAlignment(isOut ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesBox.getChildren().add(row);
    }

    // Helpers

    private Label makeAvatar(String initials, String color) {
        Label av = new Label(initials);
        av.setMinSize(38, 38); av.setMaxSize(38, 38);
        av.setAlignment(Pos.CENTER);
        av.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        av.setTextFill(Color.WHITE);
        av.setStyle("-fx-background-color:" + color + "; -fx-background-radius:19px;");
        return av;
    }

    private Button makeIconButton(String icon) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color:transparent; -fx-font-size:16px; -fx-cursor:hand;");
        return btn;
    }
}
