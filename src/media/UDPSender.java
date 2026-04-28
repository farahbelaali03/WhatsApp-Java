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
            // Header length: 36 (sender) + 36 (recipient) + 1 (type) = 73 bytes
            byte[] packet = new byte[73 + data.length];

            // UUIDs or Usernames as 36 bytes String
            byte[] senderBytes    = String.format("%-36s", senderId).getBytes();
            byte[] recipientBytes = String.format("%-36s", recipientId).getBytes();

            // Copy 36 bytes for sender
            System.arraycopy(senderBytes, 0, packet, 0, 36);

            // Copy 36 bytes for recipient
            System.arraycopy(recipientBytes, 0, packet, 36, 36);

            // Type at index 72
            packet[72] = type;

            // Data starts at index 73
            System.arraycopy(data, 0, packet, 73, data.length);

            DatagramPacket dp = new DatagramPacket(
                    packet, packet.length, serverAddress, port
            );
            socket.send(dp);

        } catch (Exception e) {
            System.out.println("UDPSender sendPacket error: " + e.getMessage());
        }
    }
}