package interfaces;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class InterfaceVideoController {

    private static ImageView remoteVideoView;
    private static ImageView localVideoView;

    // Appellee par UDPReceiver pour afficher la video distante
    public static void afficherFrame(Image image) {
        if (remoteVideoView != null) {
            remoteVideoView.setImage(image);
        }
    }

    // FIX 6: Appellee par VideoCapture pour afficher la camera locale
    public static void afficherFrameLocale(Image image) {
        if (localVideoView != null) {
            localVideoView.setImage(image);
        }
    }

    // Initialise les ImageViews depuis VideoCallWindow
    public static void setVideoViews(ImageView remote, ImageView local) {
        remoteVideoView = remote;
        localVideoView  = local;
    }
}
