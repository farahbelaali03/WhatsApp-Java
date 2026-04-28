package interfaces;

import client.Client;
import call.CallManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

public class LoginWindow extends javafx.application.Application {

    private TextField usernameField;
    private TextField ipField;
    private Label errorLabel;
    private Button connectBtn;

    // ✅ Map de CallManagers — un par utilisateur connecte
    private static final java.util.Map<String, CallManager> callManagers
            = new java.util.concurrent.ConcurrentHashMap<>();

    public static CallManager getCallManager() {
        if (callManagers.isEmpty()) return null;
        return callManagers.values().iterator().next();
    }

    public static CallManager getCallManagerFor(String username) {
        return callManagers.get(username);
    }

    public LoginWindow() {}

    @Override
    public void start(Stage stage) {
        stage.setTitle("WhatsApp");

        Label logo = new Label("WhatsApp");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        logo.setTextFill(Color.web("#25D366"));

        Label subtitle = new Label("Connectez-vous au serveur pour commencer");
        subtitle.setTextFill(Color.web("#8696A0"));
        subtitle.setFont(Font.font("Arial", 13));

        Label labelUser = new Label("NOM D'UTILISATEUR");
        labelUser.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        labelUser.setTextFill(Color.web("#8696A0"));

        usernameField = new TextField();
        usernameField.setPromptText("Votre nom...");
        styleField(usernameField);
        usernameField.setOnAction(e -> handleLogin(stage));

        Label labelIp = new Label("ADRESSE IP DU SERVEUR");
        labelIp.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        labelIp.setTextFill(Color.web("#8696A0"));

        ipField = new TextField("localhost");
        styleField(ipField);

        Label labelPort = new Label("PORT");
        labelPort.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        labelPort.setTextFill(Color.web("#8696A0"));

        TextField portField = new TextField("5000");
        portField.setPrefWidth(80);
        styleField(portField);

        VBox ipWrap = new VBox(4, labelIp, ipField);
        VBox portWrap = new VBox(4, labelPort, portField);
        HBox networkRow = new HBox(10, ipWrap, portWrap);
        HBox.setHgrow(ipWrap, Priority.ALWAYS);

        connectBtn = new Button("Se connecter");
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setStyle(
                "-fx-background-color: #25D366; -fx-text-fill: black;" +
                        "-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-padding: 12px; -fx-cursor: hand;");
        connectBtn.setOnAction(e -> handleLogin(stage));

        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#E24B4A"));
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setVisible(false);

        Label note = new Label("Connexion TCP - Port 5000");
        note.setTextFill(Color.web("#667781"));
        note.setFont(Font.font("Arial", 11));

        VBox form = new VBox(14, logo, subtitle,
                new VBox(4, labelUser, usernameField),
                networkRow, connectBtn, errorLabel, note);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(36, 32, 36, 32));
        form.setMaxWidth(380);
        form.setStyle("-fx-background-color: #202C33; -fx-background-radius: 16px;");

        StackPane root = new StackPane(form);
        root.setStyle("-fx-background-color: #111B21;");
        root.setPadding(new Insets(60));

        Scene scene = new Scene(root, 480, 520);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void handleLogin(Stage stage) {
        String username = usernameField.getText().trim();
        String ip = ipField.getText().trim();

        if (username.isEmpty()) {
            showError("Veuillez entrer un nom d'utilisateur.");
            return;
        }

        connectBtn.setDisable(true);
        connectBtn.setText("Connexion...");
        errorLabel.setVisible(false);

        Thread t = new Thread(() -> {
            Client client = new Client();
            client.connecter(username, ip);

            Platform.runLater(() -> {
                if (client.isConnecte()) {
                    // ✅ Creer CallManager pour CE client specifiquement
                    CallManager cm = new CallManager(client.getOut(), ip, username, client);
                    callManagers.put(username, cm);

                    // ✅ Callbacks lies a CE CallManager specifique
                    client.setOnIncomingCall(c -> cm.afficherDialogAppel(c));
                    client.setOnCallAccepted(c -> cm.appelAccepte(c));
                    client.setOnCallRefused(() -> cm.appelRefuse());
                    client.setOnCallEnded(() -> cm.appelTermine());

                    System.out.println("[LoginWindow] Connecte : " + username);

                    // ✅ Passer cm a MainWindow
                    MainWindow mainWindow = new MainWindow(username, client, cm);
                    mainWindow.start(new Stage());
                    stage.close();
                } else {
                    showError("Impossible de se connecter au serveur.");
                    connectBtn.setDisable(false);
                    connectBtn.setText("Se connecter");
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void styleField(TextField field) {
        field.setStyle(
                "-fx-background-color: #2A3942; -fx-text-fill: #E9EDEF;" +
                        "-fx-prompt-text-fill: #667781; -fx-background-radius: 8px;" +
                        "-fx-padding: 10px 13px; -fx-font-size: 13px;");
        field.setMaxWidth(Double.MAX_VALUE);
    }

    public static void main(String[] args) { launch(args); }
}