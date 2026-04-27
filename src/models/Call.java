package models ;

import java.io.Serializable;

public class Call implements Serializable {

    // Constantes (variable de classe final)
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTE = "ACCEPTE";
    public static final String STATUT_REFUSE= "REFUSE";
    public static final String STATUT_TERMINE = "TERMINE";

    private String idCall;
    private String caller;
    private String recipient;
    private String callType;
    private String statut;
    private long duration; // en secondes

    public Call() {}

    public Call(String idCall, String caller, String recipient, String callType) {
        this.idCall= idCall;
        this.caller= caller;
        this.recipient = recipient;
        this.callType= callType;
        this.statut= STATUT_EN_ATTENTE;
        this.duration  = 0;
    }

    // Getters
    public String getIdCall() { return idCall; }
    public String getCaller()  { return caller; }
    public String getRecipient() { return recipient; }
    public String getCallType() { return callType; }
    public String getStatut()  { return statut; }
    public long getDuration() { return duration; }

    // Setters
    public void setStatut(String statut)   { this.statut = statut; }
    public void setDuration(long duration) { this.duration = duration; }

    @Override
    public String toString() {
        return "Appel [" + callType + "] " + caller + " → " + recipient + " (" + statut + ")";
    }
}