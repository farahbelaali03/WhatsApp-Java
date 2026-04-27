package interfaces;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * InterfaceVideoController — Amal
 * Reçoit les frames vidéo de Farah via UDPReceiver.
 * afficherFrame() est appelée par UDPReceiver via Platform.runLater().
 */
public class InterfaceVideoController {

    // ImageView où afficher la vidéo distante
    // Initialisé par VideoCallWindow quand l'appel commence
    private static ImageView remoteVideoView;
    private static ImageView localVideoView;

    /**
     * Appelée par Farah depuis UDPReceiver pour afficher chaque frame.
     * Toujours via Platform.runLater() — cahier des charges §2.3.
     * @param image frame vidéo reçue en JPEG
     */
    public static void afficherFrame(Image image) {
        if (remoteVideoView != null) {
            remoteVideoView.setImage(image);
        }
    }

    /**
     * Initialise les ImageViews depuis VideoCallWindow.
     * Appelle cette méthode au démarrage de l'appel vidéo.
     */
    public static void setVideoViews(ImageView remote, ImageView local) {
        remoteVideoView = remote;
        localVideoView  = local;
    }
}