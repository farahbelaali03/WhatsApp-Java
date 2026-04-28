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
            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("[ClientHandler] Erreur creation streams : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("[ClientHandler] Nouveau client connecte.");
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Command) {
                    traiterCommande((Command) obj);
                } else if (obj instanceof Message) {
                    traiterMessage((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[ClientHandler] Client deconnecte : " +
                    (user != null ? user.getUsername() : "inconnu"));
        } finally {
            server.removeClient(this);
            broadcastUserList();
            close();
        }
    }

    private void traiterCommande(Command cmd) {
        switch (cmd.getType()) {
            case Command.CONNECT:
                String username = (String) cmd.getData();
                this.user = new User(UUID.randomUUID().toString(), username, "ONLINE");
                System.out.println("[ClientHandler] CONNECT recu : " + username);
                List<String> usernames = server.getUsernameList();
                send(new Command(Command.CONNECT_OK, usernames));
                broadcastUserList();
                break;

            case Command.DISCONNECT:
                System.out.println("[ClientHandler] DISCONNECT recu : " + user.getUsername());
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
                message.getSender() + " a " + message.getRecipient());
        ClientHandler destinataire = server.getClientByUsername(message.getRecipient());
        if (destinataire != null) {
            destinataire.send(message);
            send(new Command(Command.MESSAGE_DELIVERED, message.getRecipient()));
            System.out.println("[ClientHandler] Message transmis a " + message.getRecipient());
        } else {
            send(new Command(Command.USER_OFFLINE, message.getRecipient()));
            System.out.println("[ClientHandler] Destinataire hors ligne : " + message.getRecipient());
        }
    }

    private void traiterDemandeAppel(Command cmd) {
        String[] data = (String[]) cmd.getData();
        String caller    = data[0];
        String recipient = data[1];
        String callType  = data[2];
        System.out.println("[ClientHandler] CALL_REQUEST : " + caller + " -> " + recipient);
        ClientHandler destinataire = server.getClientByUsername(recipient);
        if (destinataire != null) {
            Call call = new Call(UUID.randomUUID().toString(), caller, recipient, callType);
            server.getActiveCalls().put(call.getIdCall(), call);
            destinataire.send(new Command(Command.INCOMING_CALL, call));
            System.out.println("[ClientHandler] INCOMING_CALL envoye a " + recipient);
        } else {
            send(new Command(Command.USER_OFFLINE, recipient));
            System.out.println("[ClientHandler] " + recipient + " est hors ligne");
        }
    }

    private void traiterAppelAccepte(Command cmd) {
        String callId = (String) cmd.getData();
        System.out.println("[ClientHandler] CALL_ACCEPTED : " + callId);
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

        // FIX 5: Notifier l'AUTRE participant que l'appel est termine
        Call call = server.getActiveCalls().get(callId);
        if (call != null) {
            String autreUsername = call.getCaller().equals(getUsername())
                    ? call.getRecipient()
                    : call.getCaller();
            ClientHandler autre = server.getClientByUsername(autreUsername);
            if (autre != null) {
                autre.send(new Command(Command.CALL_END, callId));
                System.out.println("[ClientHandler] CALL_END notifie a " + autreUsername);
            }
            server.getActiveCalls().remove(callId);
        }
    }

    private String extraireCaller(String callId) {
        Call call = server.getActiveCalls().get(callId);
        return call != null ? call.getCaller() : "";
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
