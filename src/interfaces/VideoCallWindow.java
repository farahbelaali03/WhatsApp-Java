package interfaces;

import call.CallManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
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
    private CallManager callManager;
    private int seconds = 0;
    private Label timerLabel;
    private Timeline timer;
    private Stage myStage;
    private boolean isClosing = false;

    public VideoCallWindow(String contactName, String contactInitials, String contactColor, CallManager callManager) {
        this.contactName = contactName;
        this.contactInitials = contactInitials;
        this.contactColor = contactColor;
        this.callManager = callManager;
    }

    public void start(Stage stage) {
        this.myStage = stage;
        stage.setTitle("Appel video - " + contactName);

        // Video distante (grande)
        ImageView remoteView = new ImageView();
        remoteView.setFitWidth(500);
        remoteView.setFitHeight(350);
        remoteView.setPreserveRatio(true);

        // Video locale (PiP)
        ImageView localView = new ImageView();
        localView.setFitWidth(130);
        localView.setFitHeight(100);
        localView.setPreserveRatio(true);

        InterfaceVideoController.setVideoViews(remoteView, localView);

        Label remoteAvatar = new Label(contactInitials);
        remoteAvatar.setMinSize(90, 90); remoteAvatar.setMaxSize(90, 90);
        remoteAvatar.setAlignment(Pos.CENTER);
        remoteAvatar.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        remoteAvatar.setTextFill(Color.WHITE);
        remoteAvatar.setStyle("-fx-background-color: " + contactColor + "; -fx-background-radius: 45px;");

        Label remoteName = new Label(contactName);
        remoteName.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        remoteName.setTextFill(Color.web("#E9EDEF"));

        VBox remoteDefault = new VBox(10, remoteAvatar, remoteName);
        remoteDefault.setAlignment(Pos.CENTER);

        StackPane remoteArea = new StackPane(remoteDefault, remoteView);
        remoteArea.setStyle("-fx-background-color: #0B141A;");

        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 20px; -fx-padding: 4px 14px;");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        StackPane pip = new StackPane(localView);
        pip.setMinSize(130, 100); pip.setMaxSize(130, 100);
        pip.setStyle("-fx-background-color: #2A3942; -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 10px; -fx-border-width: 1px;");

        StackPane videoStack = new StackPane();
        videoStack.setStyle("-fx-background-color: #0B141A;");
        videoStack.getChildren().addAll(remoteArea, timerLabel, pip);
        StackPane.setAlignment(timerLabel, Pos.TOP_CENTER);
        StackPane.setMargin(timerLabel, new Insets(12, 0, 0, 0));
        StackPane.setAlignment(pip, Pos.TOP_RIGHT);
        StackPane.setMargin(pip, new Insets(12, 12, 0, 0));

        Button micBtn = makeBtn("Micro", "#2A3942");
        Button camBtn = makeBtn("Camera", "#2A3942");
        Button endBtn = makeBtn("Raccrocher", "#E24B4A");
        Button spkBtn = makeBtn("HP", "#2A3942");

        endBtn.setOnAction(e -> raccrocher());

        HBox controls = new HBox(16, micBtn, camBtn, endBtn, spkBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 20, 0));
        controls.setStyle("-fx-background-color: #202C33;");

        VBox root = new VBox(videoStack, controls);
        VBox.setVgrow(videoStack, Priority.ALWAYS);

        Scene scene = new Scene(root, 620, 520);
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