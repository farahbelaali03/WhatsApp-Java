package interfaces;

import call.CallManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
    private CallManager callManager;
    private int seconds = 0;
    private Label timerLabel;
    private Timeline timer;
    private Stage myStage;
    private boolean isClosing = false;

    public AudioCallWindow(String contactName, String contactInitials, String contactColor, CallManager callManager) {
        this.contactName = contactName;
        this.contactInitials = contactInitials;
        this.contactColor = contactColor;
        this.callManager = callManager;
    }

    public void start(Stage stage) {
        this.myStage = stage;
        stage.setTitle("Appel audio - " + contactName);

        Label avatar = new Label(contactInitials);
        avatar.setMinSize(100, 100); avatar.setMaxSize(100, 100);
        avatar.setAlignment(Pos.CENTER);
        avatar.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        avatar.setTextFill(Color.WHITE);
        avatar.setStyle("-fx-background-color: " + contactColor + "; -fx-background-radius: 50px;");

        Label nameLabel = new Label(contactName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        nameLabel.setTextFill(Color.web("#E9EDEF"));

        Label statusLabel = new Label("Appel en cours...");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setTextFill(Color.web("#25D366"));

        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Arial", 16));
        timerLabel.setTextFill(Color.web("#8696A0"));
        timerLabel.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 20px; -fx-padding: 4px 16px;");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        Label networkLabel = new Label("Audio UDP - Port 5001");
        networkLabel.setFont(Font.font("Arial", 11));
        networkLabel.setTextFill(Color.web("#8696A0"));

        VBox centerArea = new VBox(18, avatar, nameLabel, statusLabel, timerLabel, networkLabel);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPadding(new Insets(50, 0, 30, 0));

        Button micBtn = makeBtn("Micro", "#2A3942");
        Button spkBtn = makeBtn("HP", "#2A3942");
        Button endBtn = makeBtn("Raccrocher", "#E24B4A");

        endBtn.setOnAction(e -> raccrocher());

        HBox controls = new HBox(20, micBtn, spkBtn, endBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 20, 0));
        controls.setStyle("-fx-background-color: #202C33;");

        VBox root = new VBox(centerArea, controls);
        VBox.setVgrow(centerArea, Priority.ALWAYS);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #111B21, #0B141A);");

        Scene scene = new Scene(root, 360, 480);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> { e.consume(); raccrocher(); });
        stage.show();
    }

    private void raccrocher() {
        if (isClosing) return;
        isClosing = true;
        if (timer != null) timer.stop();
        if (myStage != null) { myStage.close(); myStage = null; }
        callManager.terminerAppel();
    }

    public void fermer() {
        if (isClosing) return;
        isClosing = true;
        if (timer != null) timer.stop();
        Platform.runLater(() -> {
            if (myStage != null) { myStage.close(); myStage = null; }
        });
    }

    private Button makeBtn(String label, String color) {
        Button btn = new Button(label);
        btn.setMinSize(90, 40);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size:13px; -fx-background-radius: 20px; -fx-cursor: hand;");
        return btn;
    }
}