package media;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSender {

    // ─── Attributes ───────────────────────────────────────────
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int audioPort;
    private int videoPort;
    private String senderId;      // ← String to match Chaimae's UDPServer
    private String recipientId;   // ← String to match Chaimae's UDPServer

    // ─── Constructor ──────────────────────────────────────────
    public UDPSender(String serverIP, int audioPort, int videoPort,
                     String senderId, String recipientId) {
        try {
            this.socket        = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverIP);
            this.audioPort     = audioPort;
            this.videoPort     = videoPort;
            this.senderId      = senderId;
            this.recipientId   = recipientId;
        } catch (Exception e) {
            System.out.println("UDPSender init error: " + e.getMessage());
        }
    }

    // ─── Public methods ───────────────────────────────────────
    public void sendAudio(byte[] data) {
        sendPacket(data, audioPort, (byte) 0);
    }

    public void sendVideo(byte[] data) {
        sendPacket(data, videoPort, (byte) 1);
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    // ─── Private helper ───────────────────────────────────────
    private void sendPacket(byte[] data, int port, byte type) {
        try {
            byte[] packet = new byte[9 + data.length];

            // senderId as 4 bytes String — matches Chaimae's: new String(data, 0, 4).trim()
            byte[] senderBytes    = String.format("%-4s", senderId).getBytes();
            byte[] recipientBytes = String.format("%-4s", recipientId).getBytes();

            packet[0] = senderBytes[0];
            packet[1] = senderBytes[1];
            packet[2] = senderBytes[2];
            packet[3] = senderBytes[3];

            packet[4] = recipientBytes[0];
            packet[5] = recipientBytes[1];
            packet[6] = recipientBytes[2];
            packet[7] = recipientBytes[3];

            packet[8] = type;

            System.arraycopy(data, 0, packet, 9, data.length);

            DatagramPacket dp = new DatagramPacket(
                    packet, packet.length, serverAddress, port
            );
            socket.send(dp);

        } catch (Exception e) {
            System.out.println("UDPSender sendPacket error: " + e.getMessage());
        }
    }
}