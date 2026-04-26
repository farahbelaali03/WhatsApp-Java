package call;

import models.Call;
import models.Command;
import media.MediaCapture;

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

    // ── Constructeur ──────────────────────────────────────────
    /**
     * @param out        stream TCP d'Afnane (Client.getOut())
     * @param serverIP   IP du serveur pour UDP
     * @param myUsername username du client connecté
     */
    public CallManager(ObjectOutputStream out,
                       String serverIP,
                       String myUsername) {
        this.out        = out;
        this.serverIP   = serverIP;
        this.myUsername = myUsername;
        this.media      = new MediaCapture();
        System.out.println("[CallManager] Initialisé — "
                + myUsername + " @ " + serverIP);
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 1 — Appel sortant
    // Appelée par Amal quand user clique "Appeler"
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
            // Format cahier des charges : {caller, recipient, callType}
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
    // Afnane appelle ces méthodes via setOnIncomingCall()
    // ═════════════════════════════════════════════════════════

    /**
     * Reçoit l'objet Call du serveur.
     * Afnane appelle via : client.setOnIncomingCall(call -> callManager.afficherDialogAppel(call))
     * @param call objet Call complet envoyé par Chaimaa
     */
    public void afficherDialogAppel(Call call) {
        this.currentCall = call;
        System.out.println("[CallManager] Appel entrant : " + call);
        // TODO Amal :
        // Platform.runLater(() ->
        //     InterfaceVideoController.afficherDialogAppel(
        //         call.getCaller(), call.getCallType(), call.getIdCall()
        //     )
        // );
    }

    /**
     * User clique "Accepter" dans le dialog d'Amal.
     * Amal appelle directement : callManager.accepterAppel(callId)
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
     * User clique "Refuser" dans le dialog d'Amal.
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
            // TODO Amal :
            // Platform.runLater(() -> InterfaceVideoController.fermerDialog())
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur refuserAppel : "
                    + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 3 — Réponse appel sortant
    // Afnane appelle via setOnCallAccepted() et setOnCallRefused()
    // ═════════════════════════════════════════════════════════

    /**
     * B a accepté — démarrer média avec senderId et recipientId.
     * Afnane appelle via :
     *   client.setOnCallAccepted(call -> callManager.appelAccepte(call))
     * @param call objet Call avec caller et recipient
     */
    public void appelAccepte(Call call) {
        if (call != null) {
            this.currentCall = call;
            currentCall.setStatut(Call.STATUT_ACCEPTE);
        }

        // senderId   = moi (myUsername)
        // recipientId = l'autre participant
        String recipientId = (currentCall != null)
                ? currentCall.getRecipient()
                : "";

        // Démarrer capture audio + vidéo + réception UDP
        // MediaCapture.start() de Farah prend serverIP, senderId, recipientId
        media.start(serverIP, myUsername, recipientId);

        System.out.println("[CallManager] Appel accepté — média démarré"
                + " (audio=" + AUDIO_PORT
                + ", video=" + VIDEO_PORT + ")");
        // TODO Amal :
        // Platform.runLater(() -> InterfaceVideoController.ouvrirEcranVideo())
    }

    /**
     * B a refusé — notifier l'utilisateur.
     * Afnane appelle via :
     *   client.setOnCallRefused(() -> callManager.appelRefuse())
     */
    public void appelRefuse() {
        System.out.println("[CallManager] Appel refusé par le destinataire.");
        currentCall = null;
        // TODO Amal :
        // Platform.runLater(() ->
        //     InterfacePrincipaleController.afficherNotification("Appel refusé")
        // )
    }

    /**
     * Destinataire hors ligne — USER_OFFLINE reçu.
     * Géré directement dans Client.java d'Afnane.
     * @param username username hors ligne
     */
    public void userOffline(String username) {
        System.out.println("[CallManager] " + username + " est hors ligne.");
        currentCall = null;
        // TODO Amal :
        // Platform.runLater(() ->
        //     InterfacePrincipaleController.afficherNotification(
        //         "Utilisateur non disponible")
        // )
    }

    // ═════════════════════════════════════════════════════════
    // TÂCHE 4 — Fin d'appel
    // ═════════════════════════════════════════════════════════

    /**
     * User clique "Raccrocher" — bouton d'Amal.
     * Amal appelle directement : callManager.terminerAppel()
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
            // TODO Amal :
            // Platform.runLater(() -> InterfaceVideoController.fermerEcranVideo())
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur terminerAppel : "
                    + e.getMessage());
        }
    }

    /**
     * L'autre participant a raccroché.
     * Afnane appelle via :
     *   client.setOnCallEnded(() -> callManager.appelTermine())
     */
    public void appelTermine() {
        System.out.println("[CallManager] L'autre participant a raccroché.");
        if (currentCall != null) {
            currentCall.setStatut(Call.STATUT_TERMINE);
        }
        media.stop();
        currentCall = null;
        // TODO Amal :
        // Platform.runLater(() -> {
        //     InterfaceVideoController.fermerEcranVideo();
        //     InterfacePrincipaleController.afficherNotification("Appel terminé");
        // })
    }

    // ── Getters ───────────────────────────────────────────────
    public Call   getCurrentCall() { return currentCall; }
    public String getServerIP()    { return serverIP;    }
    public String getMyUsername()  { return myUsername;  }
}