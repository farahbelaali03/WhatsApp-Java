package database;

import java.sql.*;
import java.util.UUID;

/**
 * DAO pour la table user_sessions.
 *
 * Fonctionnalités :
 *   - Enregistrer une session (connexion) quand un client se connecte
 *   - Fermer une session quand un client se déconnecte
 *   - Traquer l'historique des connexions (IP, port, durée)
 */
public class SessionDAO {

    private final Connection connection;

    public SessionDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Crée une nouvelle session quand un client se connecte.
     * Appelé par : ClientHandler sur CONNECT
     *
     * @param userId    ID de l'utilisateur
     * @param ipAddress adresse IP du client
     * @param port      port de connexion
     * @return ID de la session créée
     */
    public String createSession(String userId, String ipAddress, int port) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO user_sessions (id, user_id, ip_address, port) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, ipAddress);
            ps.setInt(4, port);
            ps.executeUpdate();
            System.out.println("[SessionDAO] Session créée : " + userId + " @ " + ipAddress);
            return id;
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur createSession : " + e.getMessage());
            return null;
        }
    }

    /**
     * Ferme une session (enregistre l'heure de déconnexion).
     * Appelé par : ClientHandler sur DISCONNECT ou déconnexion
     */
    public void closeSession(String sessionId) {
        String sql = "UPDATE user_sessions SET disconnected_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
            System.out.println("[SessionDAO] Session fermée : " + sessionId);
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur closeSession : " + e.getMessage());
        }
    }

    /**
     * Ferme toutes les sessions ouvertes d'un utilisateur.
     * Utile en cas de crash ou reconnexion.
     */
    public void closeAllSessions(String userId) {
        String sql = "UPDATE user_sessions SET disconnected_at = NOW() WHERE user_id = ? AND disconnected_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            int count = ps.executeUpdate();
            System.out.println("[SessionDAO] " + count + " session(s) fermée(s) pour " + userId);
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur closeAllSessions : " + e.getMessage());
        }
    }

    /**
     * Vérifie si un utilisateur a une session active.
     */
    public boolean hasActiveSession(String userId) {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE user_id = ? AND disconnected_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur hasActiveSession : " + e.getMessage());
        }
        return false;
    }

    /**
     * Vérifie si un utilisateur a une session active via son username.
     */
    public boolean hasActiveSessionByUsername(String username) {
        String sql = """
            SELECT COUNT(*) FROM user_sessions s
            JOIN users u ON u.id = s.user_id
            WHERE u.username = ? AND s.disconnected_at IS NULL
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur hasActiveSessionByUsername : " + e.getMessage());
        }
        return false;
    }

    /**
     * Retourne le nombre total de sessions d'un utilisateur (historique).
     */
    public int getSessionCount(String userId) {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("[SessionDAO] Erreur getSessionCount : " + e.getMessage());
        }
        return 0;
    }
}
