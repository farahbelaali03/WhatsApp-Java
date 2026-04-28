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

    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private boolean isRunning;

    // FIX 7: Ouvrir sur les ports fixes 5001/5002 pour recevoir du serveur
    public UDPReceiver() {
        try {
            this.audioSocket = new DatagramSocket();
            this.videoSocket = new DatagramSocket();
            System.out.println("UDPReceiver: audio port=" + audioSocket.getLocalPort()
                    + " video port=" + videoSocket.getLocalPort());
        } catch (Exception e) {
            System.out.println("UDPReceiver init error: " + e.getMessage());
        }
    }

    public void startReceiving() {
        isRunning = true;

        // FIX 7: Thread audio avec SourceDataLine persistante pour eviter les coupures
        Thread audioThread = new Thread(() -> {
            SourceDataLine audioLine = null;
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(format, 8192);
                audioLine.start();

                byte[] buf = new byte[65535];
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        audioSocket.receive(packet);

                        byte[] data = packet.getData();
                        int length = packet.getLength();
                        if (length < 9) continue;

                        byte[] payload = new byte[length - 9];
                        System.arraycopy(data, 9, payload, 0, payload.length);

                        // Ecrire directement sur la ligne audio ouverte
                        audioLine.write(payload, 0, payload.length);

                    } catch (Exception e) {
                        if (isRunning) System.out.println("UDPReceiver audio error: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("UDPReceiver audio init error: " + e.getMessage());
            } finally {
                if (audioLine != null) {
                    audioLine.drain();
                    audioLine.close();
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
                    int length = packet.getLength();
                    if (length < 9) continue;

                    byte[] payload = new byte[length - 9];
                    System.arraycopy(data, 9, payload, 0, payload.length);

                    byte[] payloadCopy = payload.clone();
                    Platform.runLater(() -> {
                        try {
                            Image image = new Image(new ByteArrayInputStream(payloadCopy));
                            interfaces.InterfaceVideoController.afficherFrame(image);
                        } catch (Exception ex) {
                            // ignorer erreurs d'affichage
                        }
                    });

                } catch (Exception e) {
                    if (isRunning) System.out.println("UDPReceiver video error: " + e.getMessage());
                }
            }
        });

        audioThread.setDaemon(true);
        videoThread.setDaemon(true);
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
}
