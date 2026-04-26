package interfaces;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application WhatsApp Java.
 * Lance 2 fenêtres LoginWindow pour simuler 2 clients
 * Chaque fenêtre crée son propre Client et se connecte au serveur.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Fenêtre 1 — Client A
        LoginWindow login1 = new LoginWindow();
        login1.start(primaryStage);

        // Fenêtre 2 — Client B
        Stage stage2 = new Stage();
        stage2.setX(520);
        stage2.setY(80);
        LoginWindow login2 = new LoginWindow();
        login2.start(stage2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}