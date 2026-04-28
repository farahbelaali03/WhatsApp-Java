package media;

import interfaces.InterfaceVideoController;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;

public class VideoCapture {

    private org.opencv.videoio.VideoCapture camera;
    private Thread captureThread;
    private boolean running;
    private UDPSender udpSender;

    public VideoCapture(UDPSender udpSender) {
        this.udpSender = udpSender;
    }

    public void startCapture() {
        nu.pattern.OpenCV.loadLocally();
        camera = new org.opencv.videoio.VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("VideoCapture: Cannot open camera");
            return;
        }

        running = true;

        captureThread = new Thread(() -> {
            Mat mat = new Mat();
            while (running) {
                if (camera.read(mat)) {
                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", mat, mob);
                    byte[] bytes = mob.toArray();

                    // Envoyer via UDP au serveur
                    udpSender.sendVideo(bytes);

                    // FIX 6: Afficher la camera locale dans la fenetre video
                    byte[] bytesCopy = bytes.clone();
                    Platform.runLater(() -> {
                        try {
                            Image localImage = new Image(new ByteArrayInputStream(bytesCopy));
                            InterfaceVideoController.afficherFrameLocale(localImage);
                        } catch (Exception ex) {
                            // ignorer erreurs d'affichage
                        }
                    });
                }
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
        System.out.println("VideoCapture: started");
    }

    public void stopCapture() {
        running = false;
        if (camera != null) {
            camera.release();
        }
        System.out.println("VideoCapture: stopped");
    }
}
