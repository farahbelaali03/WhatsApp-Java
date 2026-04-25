package server;

import models.Call;
import models.Command;
import models.Message;
import models.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

public class ClientHandler extends Thread {

    private Socket socket;
    private User user;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            // ObjectOutputStream AVANT ObjectInputStream — obligatoire
            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("[ClientHandler] Erreur création streams : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("[ClientHandler] Nouveau client connecté.");
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Command) {
                    traiterCommande((Command) obj);
                } else if (obj instanceof Message) {
                    traiterMessage((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[ClientHandler] Client déconnecté : " +
                    (user != null ? user.getUsername() : "inconnu"));
        } finally {
            server.removeClient(this);
            // Broadcaster la nouvelle liste
            broadcastUserList();
            close();
        }
    }

    private void traiterCommande(Command cmd) {
        switch (cmd.getType()) {

            case Command.CONNECT:
                // Créer le User
                String username = (String) cmd.getData();
                this.user = new User(
                        UUID.randomUUID().toString(),
                        username,
                        "ONLINE"
                );
                System.out.println("[ClientHandler] CONNECT reçu : " + username);

                // Envoyer CONNECT_OK + liste des users
                List<String> usernames = server.getUsernameList();
                send(new Command(Command.CONNECT_OK, usernames));

                // Broadcaster la nouvelle liste à tous
                broadcastUserList();
                break;

            case Command.DISCONNECT:
                System.out.println("[ClientHandler] DISCONNECT reçu : " +
                        user.getUsername());
                server.removeClient(this);
                broadcastUserList();
                close();
                break;

            case Command.CALL_REQUEST:
                traiterDemandeAppel(cmd);
                break;

            case Command.CALL_ACCEPTED:
                traiterAppelAccepte(cmd);
                break;

            case Command.CALL_REFUSED:
                traiterAppelRefuse(cmd);
                break;

            case Command.CALL_END:
                traiterFinAppel(cmd);
                break;

            default:
                System.out.println("[ClientHandler] Commande inconnue : " + cmd.getType());
                break;
        }
    }

    private void traiterMessage(Message message) {
        System.out.println("[ClientHandler] Message de " +
                message.getSender() + " à " + message.getRecipient());

        // Trouver le destinataire
        ClientHandler destinataire = server.getClientByUsername(message.getRecipient());

        if (destinataire != null) {
            // Envoyer le message au destinataire
            destinataire.send(message);
            // Confirmer à l'expéditeur
            send(new Command(Command.MESSAGE_DELIVERED, message.getRecipient()));
            System.out.println("[ClientHandler] Message transmis à " + message.getRecipient());
        } else {
            send(new Command(Command.USER_OFFLINE, message.getRecipient()));
            System.out.println("[ClientHandler] Destinataire hors ligne : " + message.getRecipient());
        }
    }

    private void traiterDemandeAppel(Command cmd) {
        // data = {caller, recipient, callType}
        String[] data = (String[]) cmd.getData();
        String caller    = data[0];
        String recipient = data[1];
        String callType  = data[2];

        System.out.println("[ClientHandler] CALL_REQUEST : " + caller + " → " + recipient);

        // Trouver le destinataire
        ClientHandler destinataire = server.getClientByUsername(recipient);

        if (destinataire != null) {
            // Créer le Call
            Call call = new Call(
                    UUID.randomUUID().toString(),
                    caller,
                    recipient,
                    callType
            );
            // Notifier le destinataire
            destinataire.send(new Command(Command.INCOMING_CALL, call));
            System.out.println("[ClientHandler] INCOMING_CALL envoyé à " + recipient);
        } else {
            // Destinataire hors ligne
            send(new Command(Command.USER_OFFLINE, recipient));
            System.out.println("[ClientHandler] " + recipient + " est hors ligne");
        }
    }

    private void traiterAppelAccepte(Command cmd) {
        String callId = (String) cmd.getData();
        System.out.println("[ClientHandler] CALL_ACCEPTED : " + callId);

        // Notifier l'appelant avec les ports UDP
        String caller = extraireCaller(callId);
        ClientHandler appelant = server.getClientByUsername(caller);
        if (appelant != null) {
            String[] portsUDP = {"5001", "5002"};
            appelant.send(new Command(Command.CALL_ACCEPTED, portsUDP));
        }
    }

    private void traiterAppelRefuse(Command cmd) {
        String callId = (String) cmd.getData();
        System.out.println("[ClientHandler] CALL_REFUSED : " + callId);

        String caller = extraireCaller(callId);
        ClientHandler appelant = server.getClientByUsername(caller);
        if (appelant != null) {
            appelant.send(new Command(Command.CALL_REFUSED, callId));
        }
    }

    private void traiterFinAppel(Command cmd) {
        String callId = (String) cmd.getData();
        System.out.println("[ClientHandler] CALL_END : " + callId);
        // Notifier l'autre participant
    }

    private String extraireCaller(String callId) {
        // TODO : implémenter avec une Map<callId, Call> dans Server
        return "";
    }

    private void broadcastUserList() {
        List<String> usernames = server.getUsernameList();
        server.broadcast(new Command(Command.USER_LIST, usernames));
    }

    public synchronized void send(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            System.out.println("[ClientHandler] Erreur envoi : " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("[ClientHandler] Erreur fermeture : " + e.getMessage());
        }
    }

    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    public User getUser() { return user; }
    public Socket getSocket() { return socket; }
    public Server getServer() { return server; }
}