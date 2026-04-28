package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServer {

    private static final int AUDIO_PORT = 5001;
    private static final int VIDEO_PORT = 5002;
    private static final int BUFFER_SIZE = 65535;

    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private boolean running = false;

    // ── DAOs ──────────────────────────────────────────────────
    private database.SessionDAO sessionDAO;
    private database.UserDAO    userDAO;

    // Map pour stocker l'adresse UDP de chaque client
    // username → adresse IP + port UDP
    private Map<String, InetSocketAddress> udpAddresses
            = new ConcurrentHashMap<>();

    // Démarrage

    public void start() {
        try {
            // Initialiser les DAOs si nécessaire
            try {
                this.sessionDAO = new database.SessionDAO();
                this.userDAO    = new database.UserDAO();
            } catch (Exception e) {
                System.out.println("[UDPServer] DAOs non disponibles : " + e.getMessage());
            }

            audioSocket = new DatagramSocket(AUDIO_PORT);
            videoSocket = new DatagramSocket(VIDEO_PORT);
            running = true;

            System.out.println("[UDPServer] Démarré — Audio:"
                    + AUDIO_PORT + " Vidéo:" + VIDEO_PORT);

            // Thread audio
            Thread audioThread = new Thread(() -> ecouterAudio());
            audioThread.setDaemon(true);
            audioThread.start();

            // Thread vidéo
            Thread videoThread = new Thread(() -> ecouterVideo());
            videoThread.setDaemon(true);
            videoThread.start();

        } catch (IOException e) {
            System.out.println("[UDPServer] Erreur démarrage : "
                    + e.getMessage());
        }
    }

    // Écoute Audio

    private void ecouterAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);
                audioSocket.receive(packet);
                relayer(packet, audioSocket);
            } catch (IOException e) {
                if (running) {
                    System.out.println("[UDPServer] Erreur audio : "
                            + e.getMessage());
                }
            }
        }
    }

    // Écoute Vidéo

    private void ecouterVideo() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);
                videoSocket.receive(packet);
                relayer(packet, videoSocket);
            } catch (IOException e) {
                if (running) {
                    System.out.println("[UDPServer] Erreur vidéo : "
                            + e.getMessage());
                }
            }
        }
    }

    //Relayer le paquet au destinataire

    private void relayer(DatagramPacket packet, DatagramSocket socket) {
        try {
            byte[] data = packet.getData();
            int length = packet.getLength();

            // Lire le header :
            // [36 bytes senderId][36 bytes recipientId][1 byte type] = 73 bytes
            if (length < 73) return; // paquet trop petit

            // Extraire senderId et recipientId du header
            String senderId    = new String(data, 0, 36).trim();
            String recipientId = new String(data, 36, 36).trim();

            // Vérifier la session si sessionDAO est présent
            if (sessionDAO != null && !sessionDAO.hasActiveSessionByUsername(senderId)) {
                System.out.println("[UDPServer] Paquet rejeté : " + senderId + " n'a pas de session active.");
                return;
            }

            System.out.println("[UDPServer] Paquet de "
                    + senderId + " → " + recipientId);

            // Enregistrer l'adresse du sender
            InetSocketAddress senderAddress = new InetSocketAddress(
                    packet.getAddress(), packet.getPort()
            );
            udpAddresses.put(senderId, senderAddress);

            // Trouver l'adresse du destinataire
            InetSocketAddress recipientAddress =
                    udpAddresses.get(recipientId);

            if (recipientAddress != null) {
                // Retransmettre le paquet au destinataire
                DatagramPacket forward = new DatagramPacket(
                        data,
                        length,
                        recipientAddress.getAddress(),
                        recipientAddress.getPort()
                );
                socket.send(forward);
                System.out.println("[UDPServer] Paquet relayé à "
                        + recipientId);
            } else {
                System.out.println("[UDPServer] Destinataire "
                        + recipientId + " non trouvé");
            }

        } catch (IOException e) {
            System.out.println("[UDPServer] Erreur relais : "
                    + e.getMessage());
        }
    }

    // Enregistrer un client UDP

    public void enregistrerClient(String username,
                                  InetSocketAddress address) {
        udpAddresses.put(username, address);
        System.out.println("[UDPServer] Client UDP enregistré : "
                + username);
    }

    //Arrêt propre

    public void stop() {
        running = false;
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
        if (videoSocket != null && !videoSocket.isClosed()) {
            videoSocket.close();
        }
        System.out.println("[UDPServer] Arrêté proprement.");
    }

    //Getters

    public Map<String, InetSocketAddress> getUdpAddresses() {
        return udpAddresses;
    }
}