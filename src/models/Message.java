
package models ;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private String sender;
    private String recipient;
    private String content;
    private LocalDateTime timestamp;
    private boolean isRead;

    public Message() {}

    public Message(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters
    public String getSender()          { return sender; }
    public String getRecipient()       { return recipient; }
    public String getContent()         { return content; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public boolean isRead()            { return isRead; }

    // Setters
    public void setSender(String sender)       { this.sender = sender; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setContent(String content)     { this.content = content; }
    public void setTimestamp(LocalDateTime t)  { this.timestamp = t; }
    public void setRead(boolean read)          { this.isRead = read; }

    public String formatForDisplay() {
        String heure = timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        return "[" + heure + "] " + sender + " : " + content;
    }
}