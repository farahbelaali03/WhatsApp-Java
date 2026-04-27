package interfaces;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AudioCallWindow {

    private String contactName;
    private String contactInitials;
    private String contactColor;
    private int seconds = 0;
    private Label timerLabel;
    private Timeline timer;
    private boolean micMuted = false;
    private boolean speakerOn = true;

    public AudioCallWindow(String contactName, String contactInitials, String contactColor) {
        this.contactName     = contactName;
        this.contactInitials = contactInitials;
        this.contactColor    = contactColor;
    }

    public void start(Stage stage) {
        stage.setTitle("Appel audio – " + contactName);

        // Avatar du contact
        Label avatar = new Label(contactInitials);
        avatar.setMinSize(100, 100);
        avatar.setMaxSize(100, 100);
        avatar.setAlignment(Pos.CENTER);
        avatar.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        avatar.setTextFill(Color.WHITE);
        avatar.setStyle(
                "-fx-background-color: " + contactColor + ";" +
                        "-fx-background-radius: 50px;"
        );

        // Anneau animé autour de l'avatar
        StackPane avatarWrap = new StackPane(avatar);
        avatarWrap.setMinSize(120, 120);
        avatarWrap.setMaxSize(120, 120);
        avatarWrap.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-background-radius: 60px;"
        );

        Label nameLabel = new Label(contactName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        nameLabel.setTextFill(Color.web("#E9EDEF"));

        Label statusLabel = new Label("Appel en cours…");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setTextFill(Color.web("#25D366"));

        // Timer
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("DM Mono", 16));
        timerLabel.setTextFill(Color.web("#8696A0"));
        timerLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 4px 16px;"
        );

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            int m = seconds / 60, s = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        // Infos réseau
        Label networkLabel = new Label("● Audio UDP · Port 5001");
        networkLabel.setFont(Font.font("Arial", 11));
        networkLabel.setTextFill(Color.web("#8696A0"));
        networkLabel.setStyle(
                "-fx-background-color: rgba(0,0,0,0.3);" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 3px 12px;"
        );

        VBox centerArea = new VBox(18, avatarWrap, nameLabel, statusLabel, timerLabel, networkLabel);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPadding(new Insets(50, 0, 30, 0));

        // Boutons contrôle
        Button micBtn = makeCtrlBtn("🎙️", "#2A3942");
        Button spkBtn = makeCtrlBtn("🔊", "#2A3942");
        Button endBtn = makeCtrlBtn("✕",  "#E24B4A");
        Button keyBtn = makeCtrlBtn("⌨️", "#2A3942");

        micBtn.setOnAction(e -> {
            micMuted = !micMuted;
            micBtn.setText(micMuted ? "🔇" : "🎙️");
            micBtn.setStyle(makeCtrlStyle(micMuted ? "#E24B4A" : "#2A3942"));
        });

        spkBtn.setOnAction(e -> {
            speakerOn = !speakerOn;
            spkBtn.setText(speakerOn ? "🔊" : "🔈");
            spkBtn.setStyle(makeCtrlStyle(speakerOn ? "#2A3942" : "#667781"));
        });

        endBtn.setOnAction(e -> {
            timer.stop();
            stage.close();
            // appeler call.endCall()
            call.CallManager cm = LoginWindow.getCallManager();
            if (cm != null) cm.terminerAppel();
            timer.stop();
            stage.close();
        });

        VBox micBox = makeCtrlCol(micBtn, "Micro");
        VBox spkBox = makeCtrlCol(spkBtn, "HP");
        VBox endBox = makeCtrlCol(endBtn, "Raccrocher");
        VBox keyBox = makeCtrlCol(keyBtn, "Clavier");

        HBox controls = new HBox(0, micBox, spkBox, endBox, keyBox);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 20, 0));
        controls.setStyle("-fx-background-color: #202C33;");
        for (var node : controls.getChildren())
            HBox.setHgrow(node, Priority.ALWAYS);

        // Root
        VBox root = new VBox(centerArea, controls);
        VBox.setVgrow(centerArea, Priority.ALWAYS);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #111B21, #0B141A);");

        Scene scene = new Scene(root, 360, 520);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> timer.stop());
        stage.show();
    }

    // Helpers

    private Button makeCtrlBtn(String icon, String color) {
        Button btn = new Button(icon);
        btn.setMinSize(52, 52);
        btn.setMaxSize(52, 52);
        btn.setStyle(makeCtrlStyle(color));
        return btn;
    }

    private String makeCtrlStyle(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-background-radius: 26px;" +
                "-fx-cursor: hand;";
    }

    private VBox makeCtrlCol(Button btn, String label) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", 11));
        lbl.setTextFill(Color.web("#8696A0"));
        VBox box = new VBox(6, btn, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4, 0, 4, 0));
        return box;
    }
}