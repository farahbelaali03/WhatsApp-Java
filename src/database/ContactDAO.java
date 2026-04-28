package database;

import models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO pour la table contacts.
 *
 * Fonctionnalités :
 *   - Ajouter / supprimer un contact
 *   - Bloquer / débloquer un contact
 *   - Marquer comme favori
 *   - Lister les contacts d'un utilisateur
 *   - Vérifier si un utilisateur est bloqué avant d'envoyer un message/appel
 */
public class ContactDAO {

    private final Connection connection;

    public ContactDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    // AJOUTER / SUPPRIMER
    // ═══════════════════════════════════════════════════════════

    /**
     * Ajoute un contact (relation bidirectionnelle).
     * Quand A ajoute B, on crée les deux entrées A→B et B→A.
     */
    public boolean addContact(String userId, String contactId) {
        String sql = "INSERT INTO contacts (id, user_id, contact_id) VALUES (?, ?, ?)";
        try {
            connection.setAutoCommit(false);

            // A → B
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, userId);
                ps.setString(3, contactId);
                ps.executeUpdate();
            }

            // B → A
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, contactId);
                ps.setString(3, userId);
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("[ContactDAO] Contact ajouté : " + userId + " ↔ " + contactId);
            return true;

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            System.out.println("[ContactDAO] Erreur addContact : " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
        }
    }

    /**
     * Version par usernames.
     */
    public boolean addContactByUsernames(String username, String contactUsername) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs1 = ps.executeQuery();
            if (!rs1.next()) return false;
            String userId = rs1.getString("id");

            ps.setString(1, contactUsername);
            ResultSet rs2 = ps.executeQuery();
            if (!rs2.next()) return false;
            String contactId = rs2.getString("id");

            return addContact(userId, contactId);
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur addContactByUsernames : " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime un contact (relation bidirectionnelle).
     */
    public void removeContact(String userId, String contactId) {
        String sql = "DELETE FROM contacts WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, contactId);
            ps.setString(3, contactId);
            ps.setString(4, userId);
            ps.executeUpdate();
            System.out.println("[ContactDAO] Contact supprimé : " + userId + " ↔ " + contactId);
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur removeContact : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BLOQUER / DÉBLOQUER
    // ═══════════════════════════════════════════════════════════

    /**
     * Bloque un contact.
     * Un utilisateur bloqué ne peut plus envoyer de messages ni appeler.
     */
    public void blockContact(String userId, String contactId) {
        String sql = "UPDATE contacts SET is_blocked = TRUE WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, contactId);
            ps.executeUpdate();
            System.out.println("[ContactDAO] Contact bloqué : " + contactId);
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur blockContact : " + e.getMessage());
        }
    }

    /**
     * Débloque un contact.
     */
    public void unblockContact(String userId, String contactId) {
        String sql = "UPDATE contacts SET is_blocked = FALSE WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, contactId);
            ps.executeUpdate();
            System.out.println("[ContactDAO] Contact débloqué : " + contactId);
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur unblockContact : " + e.getMessage());
        }
    }

    /**
     * Vérifie si un utilisateur a bloqué un contact.
     * Utilisé par : ClientHandler avant de transmettre un message ou un appel.
     */
    public boolean isBlocked(String userId, String contactId) {
        String sql = "SELECT is_blocked FROM contacts WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, contactId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_blocked");
            }
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur isBlocked : " + e.getMessage());
        }
        return false;
    }

    /**
     * Vérifie si un blocage existe dans l'un ou l'autre sens.
     */
    public boolean isBlockedEitherWay(String userId, String contactId) {
        return isBlocked(userId, contactId) || isBlocked(contactId, userId);
    }

    // ═══════════════════════════════════════════════════════════
    // FAVORIS
    // ═══════════════════════════════════════════════════════════

    /**
     * Marque un contact comme favori.
     */
    public void setFavorite(String userId, String contactId, boolean favorite) {
        String sql = "UPDATE contacts SET is_favorite = ? WHERE user_id = ? AND contact_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, favorite);
            ps.setString(2, userId);
            ps.setString(3, contactId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur setFavorite : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LISTER LES CONTACTS
    // ═══════════════════════════════════════════════════════════

    /**
     * Retourne la liste des contacts d'un utilisateur (non bloqués).
     */
    public List<User> getContacts(String userId) {
        List<User> contacts = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, u.status
            FROM contacts c
            JOIN users u ON u.id = c.contact_id
            WHERE c.user_id = ? AND c.is_blocked = FALSE
            ORDER BY u.username
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contacts.add(new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur getContacts : " + e.getMessage());
        }
        return contacts;
    }

    /**
     * Retourne les contacts favoris d'un utilisateur.
     */
    public List<User> getFavoriteContacts(String userId) {
        List<User> contacts = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, u.status
            FROM contacts c
            JOIN users u ON u.id = c.contact_id
            WHERE c.user_id = ? AND c.is_favorite = TRUE AND c.is_blocked = FALSE
            ORDER BY u.username
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contacts.add(new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur getFavoriteContacts : " + e.getMessage());
        }
        return contacts;
    }

    /**
     * Retourne les contacts bloqués d'un utilisateur.
     */
    public List<User> getBlockedContacts(String userId) {
        List<User> contacts = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, u.status
            FROM contacts c
            JOIN users u ON u.id = c.contact_id
            WHERE c.user_id = ? AND c.is_blocked = TRUE
            ORDER BY u.username
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contacts.add(new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur getBlockedContacts : " + e.getMessage());
        }
        return contacts;
    }

    /**
     * Retourne les usernames des contacts (non bloqués) d'un utilisateur.
     */
    public List<String> getContactUsernames(String userId) {
        List<String> usernames = new ArrayList<>();
        String sql = """
            SELECT u.username FROM contacts c
            JOIN users u ON u.id = c.contact_id
            WHERE c.user_id = ? AND c.is_blocked = FALSE
            ORDER BY u.username
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("[ContactDAO] Erreur getContactUsernames : " + e.getMessage());
        }
        return usernames;
    }
}
