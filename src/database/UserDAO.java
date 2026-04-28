package database;

import models.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO pour la table users.
 * Gère : inscription, connexion, recherche, statut, last seen.
 */
public class UserDAO {

    private final Connection connection;

    public UserDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    // INSCRIPTION
    // ═══════════════════════════════════════════════════════════

    public User register(String username, String password) {
        String id = UUID.randomUUID().toString();
        String hash = hashPassword(password);
        String sql = "INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, username);
            ps.setString(3, hash);
            ps.executeUpdate();
            System.out.println("[UserDAO] Utilisateur inscrit : " + username);
            return new User(id, username, "OFFLINE");
        } catch (SQLException e) {
            System.out.println("[UserDAO] Inscription échouée : " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONNEXION
    // ═══════════════════════════════════════════════════════════

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("[UserDAO] Login réussi : " + username);
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur login : " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // RECHERCHE
    // ═══════════════════════════════════════════════════════════

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur findByUsername : " + e.getMessage());
        }
        return null;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur findById : " + e.getMessage());
        }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur usernameExists : " + e.getMessage());
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    // STATUT ET PRÉSENCE
    // ═══════════════════════════════════════════════════════════

    public void updateStatus(String username, String status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur updateStatus : " + e.getMessage());
        }
    }

    public void updateLastSeen(String username) {
        String sql = "UPDATE users SET last_seen = NOW() WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur updateLastSeen : " + e.getMessage());
        }
    }

    public List<String> getAllOnlineUsernames() {
        List<String> usernames = new ArrayList<>();
        String sql = "SELECT username FROM users WHERE status = 'ONLINE'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) usernames.add(rs.getString("username"));
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur getAllOnlineUsernames : " + e.getMessage());
        }
        return usernames;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur getAllUsers : " + e.getMessage());
        }
        return users;
    }

    // ═══════════════════════════════════════════════════════════
    // PROFIL
    // ═══════════════════════════════════════════════════════════

    public void updateBio(String username, String bio) {
        String sql = "UPDATE users SET bio = ? WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bio);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur updateBio : " + e.getMessage());
        }
    }

    public void updateProfilePicture(String username, String picturePath) {
        String sql = "UPDATE users SET profile_picture = ? WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, picturePath);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[UserDAO] Erreur updateProfilePicture : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("status")
        );
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
}
