package media;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioCapture {

    // ─── Attributes ───────────────────────────────────────────
    private TargetDataLine targetDataLine;
    private Thread captureThread;
    private boolean running;
    private UDPSender udpSender;

    // ─── Constructor ──────────────────────────────────────────
    public AudioCapture(UDPSender udpSender) {
        this.udpSender = udpSender;
    }

    // ─── Methods ──────────────────────────────────────────────
    public void startCapture() {
        try {
            // Audio format: 44100 Hz, 16 bit, mono, signed, big-endian
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            running = true;

            // Thread that reads mic bytes in a loop
            captureThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (running) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        udpSender.sendAudio(buffer);
                    }
                }
            });

            captureThread.start();
            System.out.println("AudioCapture: started");

        } catch (Exception e) {
            System.out.println("AudioCapture startCapture error: " + e.getMessage());
        }
    }

    public void stopCapture() {
        running = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        System.out.println("AudioCapture: stopped");
    }
}