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

public class CallManager {

    private ObjectOutputStream out;
    private String serverIP;
    private String myUsername;
    private Call currentCall;
    private MediaCapture media;
    private client.Client clientRef;

    private AudioCallWindow currentAudioWindow;
    private VideoCallWindow currentVideoWindow;

    private String pendingRecipient;
    private String pendingCallType;

    public CallManager(ObjectOutputStream out, String serverIP, String myUsername, client.Client clientRef) {
        this.out = out;
        this.serverIP = serverIP;
        this.myUsername = myUsername;
        this.clientRef = clientRef;
        this.media = new MediaCapture();
    }

    public void demandeAppel(String recipient, String callType) {
        if (currentCall != null) {
            showAlert("Appel en cours", "Vous avez deja un appel en cours.");
            return;
        }
        try {
            pendingRecipient = recipient;
            pendingCallType = callType;
            String[] data = { myUsername, recipient, callType };
            out.writeObject(new Command(Command.CALL_REQUEST, data));
            out.flush();
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur demandeAppel : " + e.getMessage());
        }
    }

    public void afficherDialogAppel(Call call) {
        this.currentCall = call;
        DialogAppel.afficherDialog(call, clientRef, this);
    }

    public void accepterAppel(String callId) {
        if (currentCall == null) return;
        try {
            currentCall.setStatut(Call.STATUT_ACCEPTE);
            out.writeObject(new Command(Command.CALL_ACCEPTED, callId));
            out.flush();
            media.start(serverIP, myUsername, currentCall.getCaller());
            String caller = currentCall.getCaller();
            String ini = initiales(caller);
            String type = currentCall.getCallType();
            Platform.runLater(() -> ouvrirFenetreAppel(caller, ini, type));
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur accepterAppel : " + e.getMessage());
        }
    }

    public void refuserAppel(String callId) {
        if (currentCall == null) return;
        try {
            currentCall.setStatut(Call.STATUT_REFUSE);
            out.writeObject(new Command(Command.CALL_REFUSED, callId));
            out.flush();
            currentCall = null;
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur refuserAppel : " + e.getMessage());
        }
    }

    public void appelAccepte(Call call) {
        if (pendingRecipient == null) return;
        currentCall = new Call("local", myUsername, pendingRecipient, pendingCallType);
        currentCall.setStatut(Call.STATUT_ACCEPTE);
        media.start(serverIP, myUsername, pendingRecipient);
        String recipient = pendingRecipient;
        String type = pendingCallType;
        pendingRecipient = null;
        pendingCallType = null;
        Platform.runLater(() -> ouvrirFenetreAppel(recipient, initiales(recipient), type));
    }

    public void appelRefuse() {
        pendingRecipient = null;
        pendingCallType = null;
        currentCall = null;
        showAlert("Appel refuse", "L'utilisateur a refuse votre appel.");
    }

    public void terminerAppel() {
        // ✅ Ne rien faire si deja en cours de terminaison
        if (currentCall == null && currentAudioWindow == null && currentVideoWindow == null) return;

        try {
            if (currentCall != null) {
                currentCall.setStatut(Call.STATUT_TERMINE);
                out.writeObject(new Command(Command.CALL_END, currentCall.getIdCall()));
                out.flush();
            }
            media.stop();
        } catch (IOException e) {
            System.out.println("[CallManager] Erreur terminerAppel : " + e.getMessage());
        } finally {
            currentCall = null;
            pendingRecipient = null;
            pendingCallType = null;
            // Les fenetres se ferment elles-memes via raccrocher()
            currentAudioWindow = null;
            currentVideoWindow = null;
        }
    }

    // ✅ FIX PRINCIPAL: appelTermine ferme la fenetre de l'autre cote
    public void appelTermine() {
        System.out.println("[CallManager] " + myUsername + ": l'autre a raccroche.");
        try { media.stop(); } catch (Exception e) {}

        currentCall = null;
        pendingRecipient = null;
        pendingCallType = null;

        // ✅ Fermer la fenetre locale
        AudioCallWindow aw = currentAudioWindow;
        VideoCallWindow vw = currentVideoWindow;
        currentAudioWindow = null;
        currentVideoWindow = null;

        Platform.runLater(() -> {
            if (aw != null) aw.fermer();
            if (vw != null) vw.fermer();
            showAlert("Appel termine", "L'appel est termine.");
        });
    }

    private void ouvrirFenetreAppel(String contact, String ini, String type) {
        if (Call.TYPE_VIDEO.equals(type)) {
            currentVideoWindow = new VideoCallWindow(contact, ini, "#3C3489", this);
            currentVideoWindow.start(new Stage());
        } else {
            currentAudioWindow = new AudioCallWindow(contact, ini, "#3C3489", this);
            currentAudioWindow.start(new Stage());
        }
    }

    public void userOffline(String username) {
        pendingRecipient = null;
        pendingCallType = null;
        currentCall = null;
        showAlert("Hors ligne", username + " n'est pas disponible.");
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();
        });
    }

    private String initiales(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }

    public Call getCurrentCall() { return currentCall; }
    public String getMyUsername() { return myUsername; }
    public client.Client getClientRef() { return clientRef; }
}