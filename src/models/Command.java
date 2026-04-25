package model;

import java.io.Serializable;

public class Command implements Serializable {

    // Constantes — toutes les commandes possibles du protocole
    public static final String CONNECT        = "CONNECT";
    public static final String CONNECT_OK     = "CONNECT_OK";
    public static final String DISCONNECT     = "DISCONNECT";
    public static final String USER_LIST      = "USER_LIST";
    public static final String CALL_REQUEST   = "CALL_REQUEST";
    public static final String CALL_ACCEPTED  = "CALL_ACCEPTED";
    public static final String CALL_REFUSED   = "CALL_REFUSED";
    public static final String CALL_END       = "CALL_END";
    public static final String USER_OFFLINE   = "USER_OFFLINE";
    public static final String INCOMING_CALL  = "INCOMING_CALL";
    public static final String MESSAGE           = "MESSAGE";
    public static final String MESSAGE_DELIVERED = "MESSAGE_DELIVERED";


    private String type;
    private Object data; // peut contenir un String, User, List<User>, Call...

    public Command() {}

    public Command(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    // Constructeur sans data (ex: DISCONNECT)
    public Command(String type) {
        this.type = type;
        this.data = null;
    }

    // Getters
    public String getType() { return type; }
    public Object getData() { return data; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "Command{type='" + type + "', data=" + data + "}";
    }
}