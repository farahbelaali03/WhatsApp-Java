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

/**
 * Écran de connexion.
 * Appelle client.connecter(username) au clic sur "Se connecter".
 * Si connexion réussie → ouvre MainWindow.
 * Si échec → affiche message d'erreur en rouge.
 */
public class LoginWindow extends javafx.application.Application {

    private TextField usernameField;
    private TextField ipField;
    private TextField portField;
    private Label errorLabel;
    private Button connectBtn;

    private static CallManager callManager;
    public static CallManager getCallManager() { return callManager; }

    // Constructeur sans argument (pour instanciation depuis Main)
    public LoginWindow() {}

    @Override
    public void start(Stage stage) {
        stage.setTitle("WhatsApp");

        // Logo
        Label logo = new Label("WhatsApp");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        logo.setTextFill(Color.web("#25D366"));

        Label subtitle = new Label("Connectez-vous au serveur pour commencer");
        subtitle.setTextFill(Color.web("#8696A0"));
        subtitle.setFont(Font.font("Arial", 13));

        // Champs
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

        portField = new TextField("5000");
        portField.setPrefWidth(80);
        styleField(portField);

        VBox ipWrap   = new VBox(4, labelIp, ipField);
        VBox portWrap = new VBox(4, labelPort, portField);
        HBox networkRow = new HBox(10, ipWrap, portWrap);
        HBox.setHgrow(ipWrap, Priority.ALWAYS);

        // Bouton
        connectBtn = new Button("Se connecter");
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-padding: 12px;" +
                        "-fx-cursor: hand;"
        );
        connectBtn.setOnAction(e -> handleLogin(stage));

        // Label erreur (rouge, caché par défaut)
        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#E24B4A"));
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setVisible(false);

        // Note réseau
        Label note = new Label("ⓘ  Connexion TCP · Port 5000");
        note.setTextFill(Color.web("#667781"));
        note.setFont(Font.font("Arial", 11));

        // Formulaire
        VBox form = new VBox(14,
                logo, subtitle,
                new VBox(4, labelUser, usernameField),
                networkRow,
                connectBtn,
                errorLabel,
                note
        );
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(36, 32, 36, 32));
        form.setMaxWidth(380);
        form.setStyle(
                "-fx-background-color: #202C33;" +
                        "-fx-background-radius: 16px;"
        );

        StackPane root = new StackPane(form);
        root.setStyle("-fx-background-color: #111B21;");
        root.setPadding(new Insets(60));

        Scene scene = new Scene(root, 480, 540);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // Logique connexion

    private void handleLogin(Stage stage) {
        String username = usernameField.getText().trim();
        String ip       = ipField.getText().trim();

        if (username.isEmpty()) {
            showError("Veuillez entrer un nom d'utilisateur.");
            return;
        }

        // Désactiver le bouton pendant la tentative
        connectBtn.setDisable(true);
        connectBtn.setText("Connexion…");
        errorLabel.setVisible(false);

        // Connexion dans un thread séparé pour ne pas bloquer l'UI
        Thread t = new Thread(() -> {
            Client client = new Client();

            // Appel de la méthode d'Afnane
            client.connecter(username);

            Platform.runLater(() -> {
                if (client.isConnecte()) {
                    callManager = new CallManager(
                            client.getOut(), ip, username, client
                    );
                    client.setOnIncomingCall(c -> callManager.afficherDialogAppel(c));
                    client.setOnCallAccepted(c -> callManager.appelAccepte(c));
                    client.setOnCallRefused(() -> callManager.appelRefuse());
                    client.setOnCallEnded(() -> callManager.appelTermine());
                    // Succès → ouvrir MainWindow
                    System.out.println("[LoginWindow] Connecté : " + username);
                    MainWindow mainWindow = new MainWindow(username, client);
                    mainWindow.start(new Stage());
                    stage.close();
                } else {
                    // Échec → message d'erreur rouge
                    showError("Impossible de se connecter au serveur.\nVérifiez que le serveur est lancé.");
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
                "-fx-background-color: #2A3942;" +
                        "-fx-text-fill: #E9EDEF;" +
                        "-fx-prompt-text-fill: #667781;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10px 13px;" +
                        "-fx-font-size: 13px;"
        );
        field.setMaxWidth(Double.MAX_VALUE);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
