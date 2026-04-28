package server;

import database.*;
import models.Call;
import models.Command;
import models.Message;
import models.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ClientHandler extends Thread {

    private Socket socket;
    private User user;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;

    // ── DAOs ──────────────────────────────────────────────────
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private CallDAO callDAO;
    private ConversationDAO conversationDAO;
    private ContactDAO contactDAO;
    private SessionDAO sessionDAO;
    private String sessionId; // ID de la session courante

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("[ClientHandler] Erreur création streams : " + e.getMessage());
        }

        // Initialiser les DAOs
        try {
            this.userDAO         = new UserDAO();
            this.messageDAO      = new MessageDAO();
            this.callDAO         = new CallDAO();
            this.conversationDAO = new ConversationDAO();
            this.contactDAO      = new ContactDAO();
            this.sessionDAO      = new SessionDAO();
            System.out.println("[ClientHandler] DAOs initialisés ✓");
        } catch (SQLException e) {
            System.out.println("[ClientHandler] ⚠ DAOs non disponibles (pas de DB) : " + e.getMessage());
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
            // ── DB : mettre à jour statut et session ──────────
            if (user != null && userDAO != null) {
                try {
                    userDAO.updateStatus(user.getUsername(), "OFFLINE");
                    userDAO.updateLastSeen(user.getUsername());
                    if (sessionId != null) {
                        sessionDAO.closeSession(sessionId);
                    }
                    System.out.println("[ClientHandler] DB : " + user.getUsername() + " → OFFLINE");
                } catch (Exception e) {
                    System.out.println("[ClientHandler] Erreur DB déconnexion : " + e.getMessage());
                }
            }
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
                System.out.println("[ClientHandler] CONNECT reçu : " + username);

                // ── DB : enregistrer ou retrouver l'utilisateur ──
                if (userDAO != null) {
                    try {
                        User existingUser = userDAO.findByUsername(username);
                        if (existingUser != null) {
                            // Utilisateur existant → mettre à jour statut
                            this.user = existingUser;
                            user.setStatus("ONLINE");
                            userDAO.updateStatus(username, "ONLINE");
                            System.out.println("[ClientHandler] DB : Utilisateur existant → ONLINE");
                        } else {
                            // Nouveau utilisateur → enregistrer (mot de passe par défaut)
                            User newUser = userDAO.register(username, "default123");
                            if (newUser != null) {
                                this.user = newUser;
                                user.setStatus("ONLINE");
                                userDAO.updateStatus(username, "ONLINE");
                                System.out.println("[ClientHandler] DB : Nouvel utilisateur enregistré");
                            }
                        }

                        // Créer une session
                        String ip = socket.getInetAddress().getHostAddress();
                        int port = socket.getPort();
                        sessionId = sessionDAO.createSession(user.getId(), ip, port);

                    } catch (Exception e) {
                        System.out.println("[ClientHandler] Erreur DB CONNECT : " + e.getMessage());
                    }
                }

                List<String> usernames = server.getUsernameList();
                send(new Command(Command.CONNECT_OK, usernames));
                broadcastUserList();

                // ── DB : envoyer les messages non délivrés ──────
                if (messageDAO != null && user != null) {
                    try {
                        List<Message> undelivered = messageDAO.getUndeliveredMessages(user.getId());
                        for (Message msg : undelivered) {
                            send(msg);
                        }
                        messageDAO.markAllUndeliveredAsDelivered(user.getId());
                    } catch (Exception e) {
                        System.out.println("[ClientHandler] Erreur envoi messages offline : " + e.getMessage());
                    }
                }
                break;

            case Command.DISCONNECT:
                System.out.println("[ClientHandler] DISCONNECT reçu : " + user.getUsername());

                // ── DB : mettre à jour statut ──
                if (userDAO != null) {
                    try {
                        userDAO.updateStatus(user.getUsername(), "OFFLINE");
                        userDAO.updateLastSeen(user.getUsername());
                        if (sessionId != null) {
                            sessionDAO.closeSession(sessionId);
                        }
                        System.out.println("[ClientHandler] DB : " + user.getUsername() + " → OFFLINE");
                    } catch (Exception e) {
                        System.out.println("[ClientHandler] Erreur DB DISCONNECT : " + e.getMessage());
                    }
                }

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

        // ── DB : sauvegarder le message ──────────────────────
        if (messageDAO != null && conversationDAO != null && userDAO != null) {
            try {
                // Trouver ou créer la conversation entre les deux utilisateurs
                String convId = conversationDAO.findOrCreateByUsernames(
                        message.getSender(), message.getRecipient());

                // Trouver l'ID de l'expéditeur
                User sender = userDAO.findByUsername(message.getSender());

                if (convId != null && sender != null) {
                    // Sauvegarder le message
                    String msgId = messageDAO.save(convId, sender.getId(),
                            message.getContent(), "TEXT");

                    // Créer le statut pour le destinataire (SENT)
                    if (msgId != null) {
                        User recipient = userDAO.findByUsername(message.getRecipient());
                        if (recipient != null) {
                            messageDAO.createMessageStatus(msgId, recipient.getId(), "SENT");
                        }
                    }
                    System.out.println("[ClientHandler] DB : Message sauvegardé (" + msgId + ")");
                }
            } catch (Exception e) {
                System.out.println("[ClientHandler] Erreur DB message : " + e.getMessage());
            }
        }

        // ── Transmettre le message au destinataire ───────────
        ClientHandler destinataire = server.getClientByUsername(message.getRecipient());
        if (destinataire != null) {
            destinataire.send(message);
            send(new Command(Command.MESSAGE_DELIVERED, message.getRecipient()));
            System.out.println("[ClientHandler] Message transmis à " + message.getRecipient());

            // ── DB : marquer comme DELIVERED ──
            // (le message vient d'arriver chez le destinataire)
        } else {
            send(new Command(Command.USER_OFFLINE, message.getRecipient()));
            System.out.println("[ClientHandler] Destinataire hors ligne : " + message.getRecipient());
            // Le message est déjà en DB avec statut SENT → sera délivré au reconnect
        }
    }

    private void traiterDemandeAppel(Command cmd) {
        String[] data = (String[]) cmd.getData();
        String caller    = data[0];
        String recipient = data[1];
        String callType  = data[2];
        System.out.println("[ClientHandler] CALL_REQUEST : " + caller + " → " + recipient);
        ClientHandler destinataire = server.getClientByUsername(recipient);
        if (destinataire != null) {
            Call call = new Call(UUID.randomUUID().toString(), caller, recipient, callType);
            server.getActiveCalls().put(call.getIdCall(), call);
            destinataire.send(new Command(Command.INCOMING_CALL, call));
            System.out.println("[ClientHandler] INCOMING_CALL envoyé à " + recipient);

            // ── DB : sauvegarder l'appel ──
            if (callDAO != null) {
                try {
                    callDAO.saveByUsernames(caller, recipient, callType);
                    System.out.println("[ClientHandler] DB : Appel sauvegardé");
                } catch (Exception e) {
                    System.out.println("[ClientHandler] Erreur DB appel : " + e.getMessage());
                }
            }
        } else {
            send(new Command(Command.USER_OFFLINE, recipient));
            System.out.println("[ClientHandler] " + recipient + " est hors ligne");

            // ── DB : appel manqué ──
            if (callDAO != null) {
                try {
                    String callId = callDAO.saveByUsernames(caller, recipient, callType);
                    if (callId != null) callDAO.missCall(callId);
                    System.out.println("[ClientHandler] DB : Appel manqué sauvegardé");
                } catch (Exception e) {
                    System.out.println("[ClientHandler] Erreur DB appel manqué : " + e.getMessage());
                }
            }
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

        // ── DB : marquer l'appel comme accepté ──
        if (callDAO != null) {
            try {
                callDAO.acceptCall(callId);
                System.out.println("[ClientHandler] DB : Appel accepté");
            } catch (Exception e) {
                System.out.println("[ClientHandler] Erreur DB accept : " + e.getMessage());
            }
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

        // ── DB : marquer l'appel comme refusé ──
        if (callDAO != null) {
            try {
                callDAO.refuseCall(callId);
                System.out.println("[ClientHandler] DB : Appel refusé");
            } catch (Exception e) {
                System.out.println("[ClientHandler] Erreur DB refuse : " + e.getMessage());
            }
        }
    }

    private void traiterFinAppel(Command cmd) {
        String callId = (String) cmd.getData();
        System.out.println("[ClientHandler] CALL_END : " + callId);

        // ── DB : terminer l'appel ──
        if (callDAO != null) {
            try {
                // Récupérer la durée depuis l'objet Call en mémoire
                Call call = server.getActiveCalls().get(callId);
                long duration = (call != null) ? call.getDuration() : 0;
                callDAO.endCall(callId, duration);
                System.out.println("[ClientHandler] DB : Appel terminé (" + duration + "s)");
            } catch (Exception e) {
                System.out.println("[ClientHandler] Erreur DB end : " + e.getMessage());
            }
        }

        // Retirer l'appel des appels actifs
        server.getActiveCalls().remove(callId);
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