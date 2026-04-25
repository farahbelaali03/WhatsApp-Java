package model ;
    import java.io.Serializable;

    public class User implements Serializable {
        private String id;
        private String username;
        private String status; // "ONLINE", "OFFLINE"

        // Constructeur par défaut
        public User() {}

        // Constructeur surchargé
        public User(String id, String username, String status) {
            this.id = id;
            this.username = username;
            this.status = status;
        }

        // Getters
        public String getId()       { return id; }
        public String getUsername() { return username; }
        public String getStatus()   { return status; }

        // Setters
        public void setId(String id)           { this.id = id; }
        public void setUsername(String username){ this.username = username; }
        public void setStatus(String status)   { this.status = status; }

        @Override
        public String toString() {
            return username + " [" + status + "]";
        }
    }