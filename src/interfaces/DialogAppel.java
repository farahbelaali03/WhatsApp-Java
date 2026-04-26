package interfaces;

import client.Client;
import models.Call;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog appel entrant (INCOMING_CALL).
 * Ouvert par Souraya quand INCOMING_CALL arrive.
 * Bouton Accepter → souraya.accepterAppel(callId)
 * Bouton Refuser  → souraya.refuserAppel(callId)
 * Méthode statique afficherDialog() appelée depuis n'importe où
 * via Platform.runLater() (cahier des charges §2.3).
 */
public class DialogAppel {

    /**
     * Affiche le dialog d'appel entrant.
     * Appelée par Souraya quand INCOMING_CALL arrive.
     * @param call   l'objet Call reçu du serveur
     * @param client le client courant (pour accepter/refuser)
     */
    public static void afficherDialog(Call call, Client client) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Appel entrant");
            dialog.setResizable(false);

            String caller   = call.getCaller();
            String callType = call.getCallType(); // "AUDIO" ou "VIDEO"
            String callId   = call.getIdCall();

            // Initiales appelant
            String ini = caller.length() >= 2
                    ? caller.substring(0, 2).toUpperCase()
                    : caller.toUpperCase();

            Label avatar = new Label(ini);
            avatar.setMinSize(80, 80); avatar.setMaxSize(80, 80);
            avatar.setAlignment(Pos.CENTER);
            avatar.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            avatar.setTextFill(Color.WHITE);
            avatar.setStyle("-fx-background-color:#3C3489; -fx-background-radius:40px;");

            // Infos
            Label callerLabel = new Label("Appel entrant de " + caller);
            callerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            callerLabel.setTextFill(Color.web("#E9EDEF"));

            // Type d'appel avec icône
            String typeIcon = callType.equals("VIDEO") ? "📹 Appel vidéo" : "📞 Appel audio";
            Label typeLabel = new Label(typeIcon);
            typeLabel.setFont(Font.font("Arial", 14));
            typeLabel.setTextFill(Color.web("#25D366"));

            VBox info = new VBox(8, callerLabel, typeLabel);
            info.setAlignment(Pos.CENTER);

            // Boutons
            Button acceptBtn = new Button("✓  Accepter");
            acceptBtn.setStyle(
                    "-fx-background-color:#25D366; -fx-text-fill:black;" +
                            "-fx-font-size:14px; -fx-font-weight:bold;" +
                            "-fx-background-radius:24px; -fx-padding:10px 28px; -fx-cursor:hand;"
            );
            acceptBtn.setOnAction(e -> {
                dialog.close();
                // accepterAppel(callId)
                call.CallManager cm = LoginWindow.getCallManager();
                if (cm != null) cm.accepterAppel(callId);
                System.out.println("[DialogAppel] Appel accepté : " + callId);
                // Ouvrir la bonne fenêtre selon le type
                String ini2 = caller.length() >= 2 ? caller.substring(0, 2).toUpperCase() : caller.toUpperCase();
                if (callType.equals("VIDEO")) {
                    new VideoCallWindow(caller, ini2, "#3C3489").start(new Stage());
                } else {
                    new AudioCallWindow(caller, ini2, "#3C3489").start(new Stage());
                }
            });

            Button refuseBtn = new Button("✕  Refuser");
            refuseBtn.setStyle(
                    "-fx-background-color:#E24B4A; -fx-text-fill:white;" +
                            "-fx-font-size:14px; -fx-font-weight:bold;" +
                            "-fx-background-radius:24px; -fx-padding:10px 28px; -fx-cursor:hand;"
            );
            refuseBtn.setOnAction(e -> {
                dialog.close();
                // refuserAppel(callId)
                call.CallManager cm = LoginWindow.getCallManager();
                if (cm != null) cm.refuserAppel(callId);
                System.out.println("[DialogAppel] Appel refusé : " + callId);
            });

            HBox buttons = new HBox(20, refuseBtn, acceptBtn);
            buttons.setAlignment(Pos.CENTER);

            // Root
            VBox root = new VBox(24, avatar, info, buttons);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(36, 40, 36, 40));
            root.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #111B21, #0B141A);"
            );

            Scene scene = new Scene(root, 340, 300);
            dialog.setScene(scene);
            dialog.show();
        });
    }
}
