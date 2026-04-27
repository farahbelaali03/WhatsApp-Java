package call;

import models.Call;
import models.Command;
import media.MediaCapture;
import interfaces.DialogAppel;
import interfaces.VideoCallWindow;
import interfaces.AudioCallWindow;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * CallManager — Souraya
 * Gère la logique des appels audio/vidéo côté client.
 * TCP 5000 signalisation | UDP 5001 audio | UDP 5002 vidéo
 */
public class CallManager {

    // ── Constantes ports UDP (cahier des charges) ─────────────
    private static final int AUDIO_PORT = 5001;
    private static final int VIDEO_PORT = 5002;

    // ── Attributs privés ──────────────────────────────────────
    private ObjectOutputStream out;
    private String             serverIP;
    private String             myUsername;
    private Call               currentCall;
    private MediaCapture       media;
    private client.Client      clientRef;

    // ── Constructeur ──────────────────────────────────────────
    /**
     * @param out        stream TCP d'Afnane (Client.getOut())
     * @param serverIP   IP du serveur pour UDP
     * @param myUsername username du client connecté
     * @param clientRef  référence au Client d'Afnane
     */
    public CallManager(ObjectOutputStream out,
                       String serverIP,
                       String myUsername,
                       client.Client clientRef) {
        this.out        = out;
        this.serverIP   = serverIP;
        this.myUsername = myUsername;
        this.clientRef  = clientRef;
        this.media      = new MediaCapture();
        System.out.println("[CallManager] Initialisé — "
                + myUsername + " @ " + serverIP);
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 1 — Appel sortant
    // Appelée par Amal (MainWindow) quand user clique 📞 ou 📹
    // ═════════════════════════════════════════════════════════

    /**
     * Envoie CALL_REQUEST au serveur via TCP.
     * @param recipient username du destinataire
     * @param callType  Call.TYPE_AUDIO ou Call.TYPE_VIDEO
     */
    public void demandeAppel(String recipient, String callType) {
        if (out == null) {
            System.out.println("[CallManager] Erreur : stream non initialisé.");
            return;
        }
        try {
            String[] data = { myUsername, recipient, callType };
            out.writeObject(new Command(Command.CALL_REQUEST, data));
            out.flush();
            System.out.println("[CallManager] CALL_REQUEST → "
                    + recipient + " (" + callType + ")");
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur demandeAppel : "
                    + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 2 — Appel entrant
    // Appelée par thread d'Afnane via setOnIncomingCall()
    // ═════════════════════════════════════════════════════════

    /**
     * Reçoit l'objet Call et ouvre le dialog d'Amal.
     * @param call objet Call complet envoyé par Chaimaa
     */
    public void afficherDialogAppel(Call call) {
        this.currentCall = call;
        System.out.println("[CallManager] Appel entrant : " + call);
        // DialogAppel d'Amal gère Platform.runLater en interne
        DialogAppel.afficherDialog(call, clientRef);
    }

    /**
     * Appelée par Amal quand user clique "Accepter".
     * @param callId identifiant de l'appel
     */
    public void accepterAppel(String callId) {
        if (currentCall == null) {
            System.out.println("[CallManager] Aucun appel en attente.");
            return;
        }
        try {
            currentCall.setStatut(Call.STATUT_ACCEPTE);
            out.writeObject(new Command(Command.CALL_ACCEPTED, callId));
            out.flush();
            System.out.println("[CallManager] CALL_ACCEPTED envoyé : "
                    + callId);
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur accepterAppel : "
                    + e.getMessage());
        }
    }

    /**
     * Appelée par Amal quand user clique "Refuser".
     * @param callId identifiant de l'appel
     */
    public void refuserAppel(String callId) {
        if (currentCall == null) {
            System.out.println("[CallManager] Aucun appel en attente.");
            return;
        }
        try {
            currentCall.setStatut(Call.STATUT_REFUSE);
            out.writeObject(new Command(Command.CALL_REFUSED, callId));
            out.flush();
            System.out.println("[CallManager] CALL_REFUSED envoyé : "
                    + callId);
            currentCall = null;
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur refuserAppel : "
                    + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 3 — Réponse appel sortant
    // Appelée par thread d'Afnane via setOnCallAccepted()
    // ═════════════════════════════════════════════════════════

    /**
     * B a accepté — démarrer média et ouvrir fenêtre d'Amal.
     * @param call objet Call avec caller, recipient, callType
     */
    public void appelAccepte(Call call) {
        if (call != null) {
            this.currentCall = call;
            currentCall.setStatut(Call.STATUT_ACCEPTE);
        }

        String recipientId = (currentCall != null)
                ? currentCall.getRecipient()
                : "";

        // Démarrer capture audio+vidéo — Farah
        // start(serverIP, senderId, recipientId)
        media.start(serverIP, myUsername, recipientId);
        System.out.println("[CallManager] Appel accepté — média démarré"
                + " (audio=" + AUDIO_PORT + ", video=" + VIDEO_PORT + ")");

        // Ouvrir fenêtre d'Amal selon le type d'appel
        if (currentCall != null) {
            String ini   = initiales(recipientId);
            String color = "#3C3489";
            String type  = currentCall.getCallType();
            Platform.runLater(() -> {
                if (type.equals(Call.TYPE_VIDEO)) {
                    new VideoCallWindow(recipientId, ini, color)
                            .start(new Stage());
                } else {
                    new AudioCallWindow(recipientId, ini, color)
                            .start(new Stage());
                }
            });
        }
    }

    /**
     * B a refusé — afficher alerte.
     * Appelée par thread d'Afnane via setOnCallRefused()
     */
    public void appelRefuse() {
        System.out.println("[CallManager] Appel refusé.");
        currentCall = null;
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appel refusé");
            alert.setHeaderText(null);
            alert.setContentText("L'utilisateur a refusé votre appel.");
            alert.show();
        });
    }

    /**
     * Destinataire hors ligne.
     * @param username username hors ligne
     */
    public void userOffline(String username) {
        System.out.println("[CallManager] " + username + " est hors ligne.");
        currentCall = null;
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Utilisateur hors ligne");
            alert.setHeaderText(null);
            alert.setContentText(username + " n'est pas disponible.");
            alert.show();
        });
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 4 — Fin d'appel
    // ═════════════════════════════════════════════════════════

    /**
     * Appelée par Amal quand user clique "Raccrocher".
     */
    public void terminerAppel() {
        if (currentCall == null) {
            System.out.println("[CallManager] Aucun appel en cours.");
            return;
        }
        try {
            currentCall.setStatut(Call.STATUT_TERMINE);
            out.writeObject(new Command(
                    Command.CALL_END,
                    currentCall.getIdCall()
            ));
            out.flush();
            System.out.println("[CallManager] CALL_END envoyé : "
                    + currentCall.getIdCall());
            media.stop();
            currentCall = null;
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur terminerAppel : "
                    + e.getMessage());
        }
    }

    /**
     * L'autre participant a raccroché.
     * Appelée par thread d'Afnane via setOnCallEnded()
     */
    public void appelTermine() {
        System.out.println("[CallManager] L'autre participant a raccroché.");
        if (currentCall != null) {
            currentCall.setStatut(Call.STATUT_TERMINE);
        }
        media.stop();
        currentCall = null;
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appel terminé");
            alert.setHeaderText(null);
            alert.setContentText("L'appel est terminé.");
            alert.show();
        });
    }

    // ── Méthode utilitaire ────────────────────────────────────
    /**
     * Génère les initiales — même logique qu'Amal dans MainWindow.
     * @param name username
     * @return initiales en majuscules max 2 caractères
     */
    private String initiales(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length()))
                    .toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }

    // ── Getters ───────────────────────────────────────────────
    public Call          getCurrentCall() { return currentCall; }
    public String        getServerIP()    { return serverIP;    }
    public String        getMyUsername()  { return myUsername;  }
    public client.Client getClientRef()   { return clientRef;   }
}