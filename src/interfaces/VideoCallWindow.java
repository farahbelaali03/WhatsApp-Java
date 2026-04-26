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

public class VideoCallWindow {

    private String contactName;
    private String contactInitials;
    private String contactColor;
    private int seconds = 0;
    private Label timerLabel;
    private Timeline timer;
    private boolean micMuted = false;
    private boolean camOff   = false;

    public VideoCallWindow(String contactName, String contactInitials, String contactColor) {
        this.contactName     = contactName;
        this.contactInitials = contactInitials;
        this.contactColor    = contactColor;
    }

    public void start(Stage stage) {
        stage.setTitle("Appel – " + contactName);

        // Vidéo distante
        Label remoteAvatar = new Label(contactInitials);
        remoteAvatar.setMinSize(90, 90);
        remoteAvatar.setMaxSize(90, 90);
        remoteAvatar.setAlignment(Pos.CENTER);
        remoteAvatar.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        remoteAvatar.setTextFill(Color.WHITE);
        remoteAvatar.setStyle(
                "-fx-background-color: " + contactColor + ";" +
                        "-fx-background-radius: 45px;"
        );

        Label remoteName = new Label(contactName);
        remoteName.setFont(Font.font("Arial", FontWeight.MEDIUM, 18));
        remoteName.setTextFill(Color.web("#E9EDEF"));

        Label remoteSub = new Label("Vidéo active");
        remoteSub.setFont(Font.font("Arial", 13));
        remoteSub.setTextFill(Color.web("#8696A0"));

        // Timer
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("DM Mono", 14));
        timerLabel.setTextFill(Color.web("#E9EDEF"));
        timerLabel.setStyle(
                "-fx-background-color: rgba(0,0,0,0.45);" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 4px 14px;"
        );

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            int m = seconds / 60, s = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        // PiP (soi-même)
        Label selfAvatar = new Label("AM");
        selfAvatar.setMinSize(36, 36);
        selfAvatar.setMaxSize(36, 36);
        selfAvatar.setAlignment(Pos.CENTER);
        selfAvatar.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        selfAvatar.setTextFill(Color.WHITE);
        selfAvatar.setStyle("-fx-background-color: #3C3489; -fx-background-radius: 18px;");
        StackPane pip = new StackPane(selfAvatar);
        pip.setMinSize(88, 110);
        pip.setMaxSize(88, 110);
        pip.setStyle(
                "-fx-background-color: #1a2634;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: rgba(255,255,255,0.15);" +
                        "-fx-border-radius: 12px;" +
                        "-fx-border-width: 2px;"
        );

        Label quality = new Label("● Qualité excellente  ·  TCP");
        quality.setFont(Font.font("Arial", 11));
        quality.setTextFill(Color.web("#8696A0"));
        quality.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 20px; -fx-padding: 3px 12px;");

        VBox remoteArea = new VBox(14, remoteAvatar, remoteName, remoteSub);
        remoteArea.setAlignment(Pos.CENTER);

        StackPane videoStage = new StackPane();
        videoStage.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1F1A, #071210);");
        videoStage.getChildren().addAll(remoteArea, timerLabel, pip, quality);
        StackPane.setAlignment(timerLabel, Pos.TOP_CENTER);
        StackPane.setMargin(timerLabel, new Insets(16, 0, 0, 0));
        StackPane.setAlignment(pip, Pos.TOP_RIGHT);
        StackPane.setMargin(pip, new Insets(16, 16, 0, 0));
        StackPane.setAlignment(quality, Pos.BOTTOM_CENTER);
        StackPane.setMargin(quality, new Insets(0, 0, 16, 0));

        // Boutons contrôle
        Button micBtn  = makeCtrlBtn("🎙️", "#2A3942");
        Button camBtn  = makeCtrlBtn("📹", "#2A3942");
        Button endBtn  = makeCtrlBtn("✕",  "#E24B4A");
        Button spkBtn  = makeCtrlBtn("🔊", "#2A3942");

        micBtn.setOnAction(e -> {
            micMuted = !micMuted;
            micBtn.setText(micMuted ? "🔇" : "🎙️");
        });
        camBtn.setOnAction(e -> {
            camOff = !camOff;
            camBtn.setText(camOff ? "📵" : "📹");
        });
        endBtn.setOnAction(e -> {
            timer.stop();
            stage.close();
            // TODO : appeler call.endCall()
        });

        VBox micBox  = makeCtrlCol(micBtn,  "Micro");
        VBox camBox  = makeCtrlCol(camBtn,  "Caméra");
        VBox endBox  = makeCtrlCol(endBtn,  "Raccrocher");
        VBox spkBox  = makeCtrlCol(spkBtn,  "HP");

        HBox controls = new HBox(0, micBox, camBox, endBox, spkBox);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 20, 0));
        controls.setStyle("-fx-background-color: #202C33;");
        for (var node : controls.getChildren())
            HBox.setHgrow(node, Priority.ALWAYS);

        VBox root = new VBox(videoStage, controls);
        VBox.setVgrow(videoStage, Priority.ALWAYS);

        Scene scene = new Scene(root, 600, 480);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> timer.stop());
        stage.show();
    }

    private Button makeCtrlBtn(String icon, String color) {
        Button btn = new Button(icon);
        btn.setMinSize(52, 52);
        btn.setMaxSize(52, 52);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-background-radius: 26px;" +
                        "-fx-cursor: hand;"
        );
        return btn;
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
