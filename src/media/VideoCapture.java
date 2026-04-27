package media;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

public class VideoCapture {

    // ─── Attributes ───────────────────────────────────────────
    private org.opencv.videoio.VideoCapture camera;
    private Thread captureThread;
    private boolean running;
    private UDPSender udpSender;

    // ─── Constructor ──────────────────────────────────────────
    public VideoCapture(UDPSender udpSender) {
        this.udpSender = udpSender;
    }

    // ─── Methods ──────────────────────────────────────────────
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
                    udpSender.sendVideo(bytes);
                }
            }
        });

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