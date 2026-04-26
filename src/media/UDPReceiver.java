package media;

import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver {

    // ─── Attributes ───────────────────────────────────────────
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private boolean isRunning;

    // ─── Constructor ──────────────────────────────────────────
    public UDPReceiver(int audioPort, int videoPort) {
        try {
            this.audioSocket = new DatagramSocket(audioPort);
            this.videoSocket = new DatagramSocket(videoPort);
            System.out.println("UDPReceiver: audio=" + audioPort + " video=" + videoPort);
        } catch (Exception e) {
            System.out.println("UDPReceiver init error: " + e.getMessage());
        }
    }

    // ─── Getters ──────────────────────────────────────────────
    public boolean isRunning() { return isRunning; }

    // ─── Methods ──────────────────────────────────────────────
    public void startReceiving() {
        isRunning = true;

        Thread audioThread = new Thread(() -> {
            byte[] buf = new byte[65535];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    audioSocket.receive(packet);

                    byte[] data = packet.getData();
                    int length  = packet.getLength();
                    if (length < 9) continue;

                    byte[] payload = new byte[length - 9];
                    System.arraycopy(data, 9, payload, 0, payload.length);
                    playAudio(payload);

                } catch (Exception e) {
                    if (isRunning) System.out.println("UDPReceiver audio error: " + e.getMessage());
                }
            }
        });

        Thread videoThread = new Thread(() -> {
            byte[] buf = new byte[65535];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    videoSocket.receive(packet);

                    byte[] data = packet.getData();
                    int length  = packet.getLength();
                    if (length < 9) continue;

                    byte[] payload = new byte[length - 9];
                    System.arraycopy(data, 9, payload, 0, payload.length);

                    Image image = new Image(new ByteArrayInputStream(payload));
                    Platform.runLater(() ->
                            interfaces.InterfaceVideoController.afficherFrame(image)
                    );

                } catch (Exception e) {
                    if (isRunning) System.out.println("UDPReceiver video error: " + e.getMessage());
                }
            }
        });

        audioThread.start();
        videoThread.start();
        System.out.println("UDPReceiver: started receiving");
    }

    public void stopReceiving() {
        isRunning = false;
        if (audioSocket != null && !audioSocket.isClosed()) audioSocket.close();
        if (videoSocket != null && !videoSocket.isClosed()) videoSocket.close();
        System.out.println("UDPReceiver: stopped");
    }

    // ─── Private helper ───────────────────────────────────────
    private void playAudio(byte[] audioData) {
        try {
            AudioFormat format  = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info  = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            line.write(audioData, 0, audioData.length);
            line.drain();
            line.close();
        } catch (Exception e) {
            System.out.println("UDPReceiver playAudio error: " + e.getMessage());
        }
    }
}