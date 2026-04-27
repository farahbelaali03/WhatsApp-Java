package client;

import models.Call;
import models.Command;
import models.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {

    private String username;
    private Socket socket;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connecte;

    private List<String> utilisateursConnectes;
    private List<Message> historiqueMessages;
    private Thread threadReception;
    private Runnable onUserListUpdated;

    private MessageCallback onMessageReceived;
    private CallCallback onIncomingCall;
    private CallCallback onCallAccepted;
    private Runnable onCallRefused;
    private Runnable onCallEnded;

    public interface MessageCallback {
        void onMessage(Message message);
    }

    public interface CallCallback {
        void onCall(Call call);
    }

    public Client() {
        this.connecte = false;
        this.utilisateursConnectes = new ArrayList<>();
        this.historiqueMessages = new ArrayList<>();
    }

    public void connecter(String username, String serverIP) {
        if (connecte) {
            System.out.println("[Client] Déjà connecté.");
            return;
        }

        this.username = username;
        try {
            socket = new Socket(serverIP, 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            Command cmdConnect = new Command(Command.CONNECT, username);
            out.writeObject(cmdConnect);
            out.flush();
            Command reponse = (Command) in.readObject();
            if (reponse.getType().equals(Command.CONNECT_OK)) {
                List<String> liste = (List<String>) reponse.getData();
                utilisateursConnectes.clear();
                if (liste != null) utilisateursConnectes.addAll(liste);
                connecte = true;
                System.out.println("[Client] Connecté ! Utilisateurs : " + utilisateursConnectes);
                demarrerReception();
            } else {
                System.out.println("[Client] Réponse inattendue : " + reponse.getType());
                fermerRessources();
            }
        } catch (IOException e) {
            System.out.println("[Client] Erreur réseau : " + e.getMessage());
            fermerRessources();
        } catch (ClassNotFoundException e) {
            System.out.println("[Client] Classe inconnue : " + e.getMessage());
            fermerRessources();
        }
    }

    public void deconnecter() {
        if (!connecte) {
            System.out.println("[Client] Pas connecté.");
            return;
        }
        try {
            Command cmdDisconnect = new Command(Command.DISCONNECT);
            out.writeObject(cmdDisconnect);
            out.flush();
            System.out.println("[Client] DISCONNECT envoyé.");
        } catch (IOException e) {
            System.out.println("[Client] Impossible d'envoyer DISCONNECT : " + e.getMessage());
        } finally {
            connecte = false;
            if (threadReception != null) threadReception.interrupt();
            fermerRessources();
            System.out.println("[Client] Déconnecté proprement.");
        }
    }

    public void envoyerMessage(String content, String recipient) {
        if (!connecte) {
            System.out.println("[Client] Non connecté.");
            return;
        }
        Message message = new Message(username, recipient, content);
        try {
            out.writeObject(message);
            out.flush();
            historiqueMessages.add(message);
            System.out.println("[Client] Message envoyé à " + recipient + " : " + content);
        } catch (IOException e) {
            System.out.println("[Client] Erreur envoi message : " + e.getMessage());
        }
    }

    private void demarrerReception() {
        threadReception = new Thread(() -> {
            System.out.println("[Client] Thread de réception démarré.");
            while (connecte && !Thread.currentThread().isInterrupted()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        historiqueMessages.add(message);
                        System.out.println("[Client] Message reçu de " + message.getSender() + " : " + message.getContent());
                        if (onMessageReceived != null) onMessageReceived.onMessage(message);
                    } else if (obj instanceof Command) {
                        traiterCommandeRecue((Command) obj);
                    }
                } catch (IOException | ClassNotFoundException e) {

                    if (connecte) {
                        System.out.println("[Client] Connexion perdue : " + e.getMessage());
                        connecte = false;
                    }
                    break;
                }
            }
            System.out.println("[Client] Thread de réception arrêté.");
        });
        threadReception.setDaemon(true);
        threadReception.start();
    }

    private void traiterCommandeRecue(Command cmd) {
        switch (cmd.getType()) {
            case Command.USER_LIST:

                List<String> liste = (List<String>) cmd.getData();

                utilisateursConnectes.clear();

                if (liste != null) utilisateursConnectes.addAll(liste);
                System.out.println("[Client] USER_LIST reçu : " + utilisateursConnectes);

                if (onUserListUpdated != null) onUserListUpdated.run();
                break;

            case Command.INCOMING_CALL:
                Call appel = (Call) cmd.getData();
                System.out.println("[Client] Appel entrant de : " + appel.getCaller());
                if (onIncomingCall != null) onIncomingCall.onCall(appel);
                break;

            case Command.CALL_ACCEPTED:
                System.out.println("[Client] Appel accepté !");
                if (onCallAccepted != null) onCallAccepted.onCall(null);
                break;

            case Command.CALL_REFUSED:
                System.out.println("[Client] Appel refusé.");
                if (onCallRefused != null) onCallRefused.run();
                break;

            case Command.CALL_END:
                System.out.println("[Client] Appel terminé.");
                if (onCallEnded != null) onCallEnded.run();
                break;

            case Command.USER_OFFLINE:
                System.out.println("[Client] Utilisateur hors ligne : " + cmd.getData());
                break;

            case Command.MESSAGE_DELIVERED:
                if (!historiqueMessages.isEmpty())
                    historiqueMessages.get(historiqueMessages.size() - 1).setRead(true);
                System.out.println("[Client] Message délivré à : " + cmd.getData());
                break;

            default:
                System.out.println("[Client] Commande inconnue : " + cmd.getType());
                break;
        }
    }

    private void fermerRessources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("[Client] Erreur fermeture : " + e.getMessage());
        }
    }

    public void setOnUserListUpdated(Runnable callback) { this.onUserListUpdated = callback; }

    public void setOnMessageReceived(MessageCallback callback) { this.onMessageReceived = callback; }
    public void setOnIncomingCall(CallCallback callback) { this.onIncomingCall = callback; }
    public void setOnCallAccepted(CallCallback callback) { this.onCallAccepted = callback; }
    public void setOnCallRefused(Runnable callback) { this.onCallRefused = callback; }
    public void setOnCallEnded(Runnable callback) { this.onCallEnded = callback; }

    public String getUsername() { return username; }
    public boolean isConnecte() { return connecte; }
    public List<String> getUtilisateursConnectes() { return utilisateursConnectes; }

    public List<Message> getHistoriqueMessages() { return historiqueMessages; }

    public ObjectInputStream getIn() { return in; }
    public ObjectOutputStream getOut() { return out; }
}