package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton — gère la connexion JDBC vers MySQL (XAMPP).
 * Utilisation :
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *
 * Configuration par défaut (XAMPP) :
 *   Host     : localhost
 *   Port     : 3306
 *   Database : whatsapp_java
 *   User     : root
 *   Password : (vide)
 */
public class DatabaseConnection {

    // ── Configuration XAMPP ───────────────────────────────────
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "whatsapp_java";
    private static final String USER     = "root";
    private static final String PASSWORD = "";   // XAMPP default = pas de mot de passe

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8";

    // ── Singleton ─────────────────────────────────────────────
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            // Charger le driver JDBC MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DatabaseConnection] Driver MySQL chargé.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] ERREUR : Driver MySQL introuvable !");
            System.err.println("Vérifiez que mysql-connector-j est dans le pom.xml.");
            e.printStackTrace();
        }
    }

    /**
     * Retourne l'instance unique de DatabaseConnection.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Retourne une connexion active vers MySQL.
     * Recrée la connexion si elle est fermée ou nulle.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("[DatabaseConnection] Connexion à " + URL);
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DatabaseConnection] Connecté à MySQL (XAMPP) ✓");
        }
        return connection;
    }

    /**
     * Ferme la connexion proprement.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DatabaseConnection] Connexion fermée.");
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Erreur fermeture : " + e.getMessage());
            }
        }
    }

    /**
     * Teste la connexion et affiche un message.
     * Utile pour vérifier que XAMPP est bien lancé.
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("[DatabaseConnection] ✓ Connexion OK !");
                System.out.println("  → Base de données : " + DATABASE);
                System.out.println("  → Host : " + HOST + ":" + PORT);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] ✗ Connexion échouée !");
            System.err.println("  → Vérifiez que XAMPP (MySQL) est démarré.");
            System.err.println("  → Vérifiez que la base '" + DATABASE + "' existe.");
            System.err.println("  → Erreur : " + e.getMessage());
        }
        return false;
    }



}
