package interfaces;

import call.CallManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWindow {

    private String currentUser;
    private Client client;
    private CallManager callManager; // ✅ CallManager propre a cette instance
    private VBox contactList;
    private StackPane mainArea;
    private VBox messagesBox;
    private TextField inputField;
    private String currentContact;
    private Map<String, List<Message>> pendingMessages = new HashMap<>();

    private static final String[] COLORS = {
            "#3C3489","#712B13","#0F6E56","#0C447C",
            "#633806","#1D6B3A","#7B2D8B","#8B4513"
    };

    // ✅ Constructeur avec CallManager propre
    public MainWindow(String currentUser, Client client, CallManager callManager) {
        this.currentUser = currentUser;
        this.client = client;
        this.callManager = callManager;
    }

    public void start(Stage stage) {
        stage.setTitle("WhatsApp - " + currentUser);
        stage.setOnCloseRequest(e -> client.deconnecter());

        Label title = new Label("WhatsApp");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#E9EDEF"));

        Button deconnecterBtn = new Button("Deconnecter");
        deconnecterBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8696A0; -fx-font-size:12px; -fx-cursor:hand;");
        deconnecterBtn.setOnAction(e -> { client.deconnecter(); stage.close(); });

        HBox header = new HBox(title, new Pane(), deconnecterBtn);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-background-color: #202C33;");

        Label statusLabel = new Label("Connecte en tant que " + currentUser);
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.web("#25D366"));
        statusLabel.setPadding(new Insets(5, 16, 5, 16));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-background-color: #0B1F0F;");

        TextField search = new TextField();
        search.setPromptText("Rechercher ou demarrer une discussion");
        search.setStyle("-fx-background-color:#2A3942; -fx-text-fill:#E9EDEF; -fx-prompt-text-fill:#667781; -fx-background-radius:8px; -fx-padding:7px 12px; -fx-font-size:13px;");
        HBox searchBox = new HBox(search);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 12, 8, 12));
        searchBox.setStyle("-fx-background-color: #202C33;");

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton tabDisc = makeTab("Discussions", tabGroup, true);
        ToggleButton tabConn = makeTab("Connectes", tabGroup, false);
        HBox tabs = new HBox(tabDisc, tabConn);
        tabs.setStyle("-fx-background-color:#111B21; -fx-border-color:#2A3942; -fx-border-width:0 0 1 0;");

        contactList = new VBox();
        contactList.setStyle("-fx-background-color: #111B21;");
        mettreAJourListe(client.getUtilisateursConnectes());

        client.setOnUserListUpdated(() ->
                Platform.runLater(() -> mettreAJourListe(client.getUtilisateursConnectes()))
        );

        // ✅ FIX: callback messages defini ici une seule fois
        client.setOnMessageReceived(message -> {
            Platform.runLater(() -> {
                String sender = message.getSender();
                if (sender.equals(currentContact) && messagesBox != null) {
                    afficherBubble(message.getContent(), false, sender,
                            message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
                } else {
                    pendingMessages.computeIfAbsent(sender, k -> new ArrayList<>()).add(message);
                }
            });
        });

        ScrollPane scroll = new ScrollPane(contactList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#111B21; -fx-background:#111B21;");

        tabDisc.setOnAction(e -> mettreAJourListe(client.getUtilisateursConnectes()));
        tabConn.setOnAction(e -> mettreAJourListe(client.getUtilisateursConnectes()));

        VBox sidebar = new VBox(header, statusLabel, searchBox, tabs, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.setPrefWidth(360);
        sidebar.setMaxWidth(360);
        sidebar.setStyle("-fx-background-color: #111B21;");

        mainArea = new StackPane(makeBienvenuePane());
        mainArea.setStyle("-fx-background-color: #0B141A;");

        HBox root = new HBox(sidebar, mainArea);
        HBox.setHgrow(mainArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    private VBox makeBienvenuePane() {
        Label wTitle = new Label("WhatsApp");
        wTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        wTitle.setTextFill(Color.web("#E9EDEF"));
        Label wSub = new Label("Selectionnez un contact pour demarrer");
        wSub.setFont(Font.font("Arial", 13));
        wSub.setTextFill(Color.web("#8696A0"));
        VBox welcome = new VBox(12, wTitle, wSub);
        welcome.setAlignment(Pos.CENTER);
        return welcome;
    }

    private void ouvrirChat(String username, String ini, String color) {
        currentContact = username;

        Label avatar = makeAvatar(ini, color);
        Label name = new Label(username);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#E9EDEF"));
        Label status = new Label("En ligne");
        status.setFont(Font.font("Arial", 12));
        status.setTextFill(Color.web("#25D366"));
        VBox nameBox = new VBox(2, name, status);

        // ✅ Utiliser callManager de cette instance
        Button callBtn = new Button("Appel audio");
        callBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8696A0; -fx-font-size:12px; -fx-cursor:hand;");
        callBtn.setOnAction(e -> callManager.demandeAppel(username, models.Call.TYPE_AUDIO));

        Button videoBtn = new Button("Appel video");
        videoBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8696A0; -fx-font-size:12px; -fx-cursor:hand;");
        videoBtn.setOnAction(e -> callManager.demandeAppel(username, models.Call.TYPE_VIDEO));

        HBox chatHeader = new HBox(12, avatar, nameBox, new Pane(), callBtn, videoBtn);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(10, 16, 10, 16));
        chatHeader.setStyle("-fx-background-color: #202C33;");

        messagesBox = new VBox(6);
        messagesBox.setPadding(new Insets(14, 40, 14, 40));
        messagesBox.setStyle("-fx-background-color: #0B141A;");

        // Afficher messages en attente
        List<Message> pending = pendingMessages.remove(username);
        if (pending != null) {
            for (Message m : pending) {
                afficherBubble(m.getContent(), false, m.getSender(),
                        m.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        }

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.setStyle("-fx-background-color:#0B141A; -fx-background:#0B141A;");
        msgScroll.vvalueProperty().bind(messagesBox.heightProperty());

        inputField = new TextField();
        inputField.setPromptText("Saisir un message");
        inputField.setStyle("-fx-background-color:#2A3942; -fx-text-fill:#E9EDEF; -fx-prompt-text-fill:#667781; -fx-background-radius:10px; -fx-padding:9px 14px; -fx-font-size:13px;");
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("Envoyer");
        sendBtn.setStyle("-fx-background-color:#25D366; -fx-text-fill:white; -fx-font-size:13px; -fx-background-radius:20px; -fx-padding:8px 16px; -fx-cursor:hand;");
        sendBtn.setOnAction(e -> sendMessage());

        HBox inputBar = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(8, 12, 8, 12));
        inputBar.setStyle("-fx-background-color: #202C33;");

        VBox chatPane = new VBox(chatHeader, msgScroll, inputBar);
        VBox.setVgrow(msgScroll, Priority.ALWAYS);
        mainArea.getChildren().setAll(chatPane);
    }

    private void sendMessage() {
        if (inputField == null || currentContact == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        afficherBubble(text, true, currentUser, time);
        inputField.clear();
        client.envoyerMessage(text, currentContact);
    }

    private void afficherBubble(String content, boolean isOut, String sender, String time) {
        if (messagesBox == null) return;
        String display = "[" + time + "] " + sender + " : " + content;
        if (isOut) display += "  vv";

        Label bubble = new Label(display);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setFont(Font.font("Arial", 13));
        bubble.setPadding(new Insets(7, 10, 7, 10));
        bubble.setTextFill(Color.web("#E9EDEF"));
        bubble.setStyle("-fx-background-color:" + (isOut ? "#005C4B" : "#202C33") + "; -fx-background-radius:8px;");

        HBox row = new HBox(bubble);
        row.setAlignment(isOut ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesBox.getChildren().add(row);
    }

    public void mettreAJourListe(List<String> users) {
        contactList.getChildren().clear();
        if (users == null || users.stream().filter(u -> !u.equals(currentUser)).count() == 0) {
            Label empty = new Label("Aucun autre utilisateur connecte");
            empty.setTextFill(Color.web("#8696A0"));
            empty.setPadding(new Insets(20, 16, 0, 16));
            contactList.getChildren().add(empty);
            return;
        }
        for (String username : users) {
            if (!username.equals(currentUser))
                contactList.getChildren().add(makeContactRow(username));
        }
    }

    private HBox makeContactRow(String username) {
        String ini = initiales(username);
        String color = couleur(username);
        Label avatar = makeAvatar(ini, color);

        Label name = new Label(username);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#E9EDEF"));
        Label onlineStatus = new Label("En ligne");
        onlineStatus.setFont(Font.font("Arial", 12));
        onlineStatus.setTextFill(Color.web("#25D366"));
        VBox info = new VBox(3, name, onlineStatus);

        // ✅ Utiliser callManager de cette instance
        Button callBtn = new Button("audio");
        callBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8696A0; -fx-font-size:12px; -fx-cursor:hand;");
        callBtn.setOnAction(e -> callManager.demandeAppel(username, models.Call.TYPE_AUDIO));

        Button videoBtn = new Button("video");
        videoBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8696A0; -fx-font-size:12px; -fx-cursor:hand;");
        videoBtn.setOnAction(e -> callManager.demandeAppel(username, models.Call.TYPE_VIDEO));

        HBox row = new HBox(12, avatar, info, new Pane(), callBtn, videoBtn);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle("-fx-cursor:hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#202C33; -fx-cursor:hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;"));
        row.setOnMouseClicked(e -> ouvrirChat(username, ini, color));
        return row;
    }

    private Label makeAvatar(String initials, String color) {
        Label av = new Label(initials);
        av.setMinSize(42, 42); av.setMaxSize(42, 42);
        av.setAlignment(Pos.CENTER);
        av.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        av.setTextFill(Color.WHITE);
        av.setStyle("-fx-background-color:" + color + "; -fx-background-radius:21px;");
        return av;
    }

    private String initiales(String name) {
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
    }

    private String couleur(String name) {
        return COLORS[Math.abs(name.hashCode()) % COLORS.length];
    }

    private ToggleButton makeTab(String text, ToggleGroup group, boolean sel) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(sel);
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        String base = "-fx-background-color:transparent; -fx-font-size:13px; -fx-padding:10px 0;";
        btn.setStyle(base + (sel ? "-fx-text-fill:#25D366; -fx-border-color:#25D366; -fx-border-width:0 0 2 0;"
                : "-fx-text-fill:#8696A0; -fx-border-color:transparent;"));
        btn.selectedProperty().addListener((obs, o, v) ->
                btn.setStyle(base + (v ? "-fx-text-fill:#25D366; -fx-border-color:#25D366; -fx-border-width:0 0 2 0;"
                        : "-fx-text-fill:#8696A0; -fx-border-color:transparent;"))
        );
        return btn;
    }
}