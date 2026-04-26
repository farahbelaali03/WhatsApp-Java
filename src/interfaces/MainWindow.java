package interfaces;

import client.Client;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.util.List;

import static javafx.application.Platform.runLater;

/**
 * Écran principal.
 * Liste les utilisateurs connectés en temps réel via USER_LIST.
 * Clic contact → ChatWindow. Boutons → appels audio/vidéo.
 */
public class MainWindow {

    private String currentUser;
    private Client client;
    private VBox contactList;

    private static final String[] COLORS = {
            "#3C3489","#712B13","#0F6E56","#0C447C",
            "#633806","#1D6B3A","#7B2D8B","#8B4513"
    };

    public MainWindow(String currentUser, Client client) {
        this.currentUser = currentUser;
        this.client = client;
    }

    public void start(Stage stage) {
        stage.setTitle("WhatsApp – " + currentUser);
        stage.setOnCloseRequest(e -> client.deconnecter());

        // Header
        Label title = new Label("WhatsApp");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#E9EDEF"));

        Button deconnecterBtn = new Button("⏻ Déconnecter");
        deconnecterBtn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#8696A0;" +
                        "-fx-font-size:12px; -fx-cursor:hand;"
        );
        deconnecterBtn.setOnAction(e -> { client.deconnecter(); stage.close(); });

        HBox header = new HBox(title, new Pane(), deconnecterBtn);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-background-color: #202C33;");

        // Statut connecté
        Label statusLabel = new Label("● Connecté en tant que " + currentUser);
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.web("#25D366"));
        statusLabel.setPadding(new Insets(5, 16, 5, 16));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-background-color: #0B1F0F;");

        // Recherche
        TextField search = new TextField();
        search.setPromptText("Rechercher ou démarrer une discussion");
        search.setStyle(
                "-fx-background-color:#2A3942; -fx-text-fill:#E9EDEF;" +
                        "-fx-prompt-text-fill:#667781; -fx-background-radius:8px;" +
                        "-fx-padding:7px 12px; -fx-font-size:13px;"
        );
        HBox searchBox = new HBox(search);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 12, 8, 12));
        searchBox.setStyle("-fx-background-color: #202C33;");

        // Onglets
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton tabDisc = makeTab("Discussions", tabGroup, true);
        ToggleButton tabConn = makeTab("Connectés",   tabGroup, false);
        HBox tabs = new HBox(tabDisc, tabConn);
        tabs.setStyle("-fx-background-color:#111B21; -fx-border-color:#2A3942; -fx-border-width:0 0 1 0;");

        // Liste contacts
        contactList = new VBox();
        contactList.setStyle("-fx-background-color: #111B21;");
        mettreAJourListe(client.getUtilisateursConnectes());

        // Callback USER_LIST — mis à jour automatiquement
        client.setOnUserListUpdated(() ->
                Platform.runLater(() -> mettreAJourListe(client.getUtilisateursConnectes()))
        );

        ScrollPane scroll = new ScrollPane(contactList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#111B21; -fx-background:#111B21;");

        tabDisc.setOnAction(e -> mettreAJourListe(client.getUtilisateursConnectes()));
        tabConn.setOnAction(e -> mettreAJourListe(client.getUtilisateursConnectes()));

        VBox sidebar = new VBox(header, statusLabel, searchBox, tabs, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.setPrefWidth(360);
        sidebar.setStyle("-fx-background-color: #111B21;");

        // Zone bienvenue
        Label icon = new Label("💬");
        icon.setFont(Font.font(48));
        Label wTitle = new Label("WhatsApp");
        wTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        wTitle.setTextFill(Color.web("#E9EDEF"));
        Label wSub = new Label("Sélectionnez un contact pour démarrer");
        wSub.setFont(Font.font("Arial", 13));
        wSub.setTextFill(Color.web("#8696A0"));
        VBox welcome = new VBox(12, icon, wTitle, wSub);
        welcome.setAlignment(Pos.CENTER);
        StackPane mainArea = new StackPane(welcome);
        mainArea.setStyle("-fx-background-color: #0B141A;");

        HBox root = new HBox(sidebar, mainArea);
        HBox.setHgrow(mainArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Met à jour la liste des contacts.
     * Appelée via Platform.runLater() depuis le thread réseau.
     */
    public void mettreAJourListe(List<String> users) {
        contactList.getChildren().clear();
        if (users == null || users.stream().filter(u -> !u.equals(currentUser)).count() == 0) {
            Label empty = new Label("Aucun autre utilisateur connecté");
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
        String ini   = initiales(username);
        String color = couleur(username);

        Label avatar = makeAvatar(ini, color);

        Label name = new Label(username);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#E9EDEF"));

        Label onlineStatus = new Label("● En ligne");
        onlineStatus.setFont(Font.font("Arial", 12));
        onlineStatus.setTextFill(Color.web("#25D366"));

        VBox info = new VBox(3, name, onlineStatus);

        Button callBtn = new Button("📞");
        callBtn.setStyle("-fx-background-color:transparent; -fx-font-size:14px; -fx-cursor:hand;");
        callBtn.setOnAction(e -> new AudioCallWindow(username, ini, color).start(new Stage()));

        Button videoBtn = new Button("📹");
        videoBtn.setStyle("-fx-background-color:transparent; -fx-font-size:14px; -fx-cursor:hand;");
        videoBtn.setOnAction(e -> new VideoCallWindow(username, ini, color).start(new Stage()));

        HBox row = new HBox(12, avatar, info, new Pane(), callBtn, videoBtn);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle("-fx-cursor:hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#202C33; -fx-cursor:hand;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;"));
        row.setOnMouseClicked(e -> new ChatWindow(currentUser, username, ini, color, client).start(new Stage()));

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
        btn.setToggleGroup(group); btn.setSelected(sel);
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

    private void run() {
        runLater(() -> mettreAJourListe(client.getUtilisateursConnectes()));
    }
}
