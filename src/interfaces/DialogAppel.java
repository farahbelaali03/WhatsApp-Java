package interfaces;

import call.CallManager;
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

public class DialogAppel {

    public DialogAppel(Call call, Client client) {}

    // ✅ Prend le CallManager en parametre — pas de static
    public static void afficherDialog(Call call, Client client, CallManager callManager) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Appel entrant");
            dialog.setResizable(false);

            String caller   = call.getCaller();
            String callType = call.getCallType();
            String callId   = call.getIdCall();

            String ini = caller.length() >= 2 ? caller.substring(0, 2).toUpperCase() : caller.toUpperCase();

            Label avatar = new Label(ini);
            avatar.setMinSize(80, 80); avatar.setMaxSize(80, 80);
            avatar.setAlignment(Pos.CENTER);
            avatar.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            avatar.setTextFill(Color.WHITE);
            avatar.setStyle("-fx-background-color:#3C3489; -fx-background-radius:40px;");

            Label callerLabel = new Label("Appel entrant de " + caller);
            callerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            callerLabel.setTextFill(Color.web("#E9EDEF"));

            String typeIcon = callType.equals("VIDEO") ? "Appel video" : "Appel audio";
            Label typeLabel = new Label(typeIcon);
            typeLabel.setFont(Font.font("Arial", 14));
            typeLabel.setTextFill(Color.web("#25D366"));

            VBox info = new VBox(8, callerLabel, typeLabel);
            info.setAlignment(Pos.CENTER);

            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color:#25D366; -fx-text-fill:black; -fx-font-size:14px; -fx-font-weight:bold; -fx-background-radius:24px; -fx-padding:10px 28px; -fx-cursor:hand;");
            acceptBtn.setOnAction(e -> {
                dialog.close();
                callManager.accepterAppel(callId);
            });

            Button refuseBtn = new Button("Refuser");
            refuseBtn.setStyle("-fx-background-color:#E24B4A; -fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold; -fx-background-radius:24px; -fx-padding:10px 28px; -fx-cursor:hand;");
            refuseBtn.setOnAction(e -> {
                dialog.close();
                callManager.refuserAppel(callId);
            });

            HBox buttons = new HBox(20, refuseBtn, acceptBtn);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(24, avatar, info, buttons);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(36, 40, 36, 40));
            root.setStyle("-fx-background-color: linear-gradient(to bottom, #111B21, #0B141A);");

            Scene scene = new Scene(root, 340, 300);
            dialog.setScene(scene);
            dialog.show();
        });
    }
}