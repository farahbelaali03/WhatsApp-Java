package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO pour les tables conversations et conversation_members.
 *
 * Fonctionnalités :
 *   - Créer une conversation privée (1-to-1) entre deux utilisateurs
 *   - Créer une conversation de groupe
 *   - Trouver la conversation privée entre deux utilisateurs
 *   - Lister les conversations d'un utilisateur (sidebar MainWindow)
 *   - Gérer les membres d'un groupe (ajouter, retirer, rôles)
 */
public class ConversationDAO {

    private final Connection connection;

    public ConversationDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    // CRÉER UNE CONVERSATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Crée une conversation privée (1-to-1) entre deux utilisateurs.
     * Si la conversation existe déjà, retourne son ID.
     * Appelé par : ClientHandler quand un premier message est envoyé entre deux users.
     *
     * @param userId1 ID du premier utilisateur
     * @param userId2 ID du second utilisateur
     * @return ID de la conversation
     */
    public String findOrCreatePrivateConversation(String userId1, String userId2) {
        // D'abord, vérifier si une conversation privée existe déjà
        String existing = findPrivateConversation(userId1, userId2);
        if (existing != null) {
            return existing;
        }

        // Créer une nouvelle conversation privée
        String convId = UUID.randomUUID().toString();
        try {
            connection.setAutoCommit(false);

            // Insérer la conversation
            String sqlConv = "INSERT INTO conversations (id, type) VALUES (?, 'PRIVATE')";
            try (PreparedStatement ps = connection.prepareStatement(sqlConv)) {
                ps.setString(1, convId);
                ps.executeUpdate();
            }

            // Ajouter les deux membres
            String sqlMember = "INSERT INTO conversation_members (id, conversation_id, user_id, role) VALUES (?, ?, ?, 'MEMBER')";
            try (PreparedStatement ps = connection.prepareStatement(sqlMember)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, convId);
                ps.setString(3, userId1);
                ps.executeUpdate();

                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(3, userId2);
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("[ConversationDAO] Conversation privée créée : " + convId);
            return convId;

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            System.out.println("[ConversationDAO] Erreur création conversation : " + e.getMessage());
            return null;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
        }
    }

    /**
     * Version par username — trouve ou crée la conversation privée.
     */
    public String findOrCreateByUsernames(String username1, String username2) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username1);
            ResultSet rs1 = ps.executeQuery();
            if (!rs1.next()) return null;
            String id1 = rs1.getString("id");

            ps.setString(1, username2);
            ResultSet rs2 = ps.executeQuery();
            if (!rs2.next()) return null;
            String id2 = rs2.getString("id");

            return findOrCreatePrivateConversation(id1, id2);
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur findOrCreateByUsernames : " + e.getMessage());
            return null;
        }
    }

    /**
     * Crée une conversation de groupe.
     *
     * @param name      Nom du groupe
     * @param creatorId ID du créateur (sera OWNER)
     * @param memberIds Liste des IDs des membres
     * @return ID de la conversation de groupe
     */
    public String createGroupConversation(String name, String creatorId, List<String> memberIds) {
        String convId = UUID.randomUUID().toString();
        try {
            connection.setAutoCommit(false);

            // Créer la conversation
            String sqlConv = "INSERT INTO conversations (id, name, type, created_by) VALUES (?, ?, 'GROUP', ?)";
            try (PreparedStatement ps = connection.prepareStatement(sqlConv)) {
                ps.setString(1, convId);
                ps.setString(2, name);
                ps.setString(3, creatorId);
                ps.executeUpdate();
            }

            // Ajouter le créateur comme OWNER
            addMemberInternal(convId, creatorId, "OWNER");

            // Ajouter les autres membres
            for (String memberId : memberIds) {
                if (!memberId.equals(creatorId)) {
                    addMemberInternal(convId, memberId, "MEMBER");
                }
            }

            connection.commit();
            System.out.println("[ConversationDAO] Groupe créé : " + name + " (" + convId + ")");
            return convId;

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            System.out.println("[ConversationDAO] Erreur création groupe : " + e.getMessage());
            return null;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RECHERCHE
    // ═══════════════════════════════════════════════════════════

    /**
     * Trouve la conversation privée entre deux utilisateurs.
     * @return ID de la conversation ou null
     */
    public String findPrivateConversation(String userId1, String userId2) {
        String sql = """
            SELECT c.id FROM conversations c
            JOIN conversation_members cm1 ON cm1.conversation_id = c.id AND cm1.user_id = ?
            JOIN conversation_members cm2 ON cm2.conversation_id = c.id AND cm2.user_id = ?
            WHERE c.type = 'PRIVATE'
            LIMIT 1
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId1);
            ps.setString(2, userId2);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur findPrivateConversation : " + e.getMessage());
        }
        return null;
    }

    /**
     * Retourne toutes les conversations d'un utilisateur.
     * Utilisé par : MainWindow pour afficher la sidebar.
     * Retourne : [conversationId, name, type, lastMessage, lastMessageTime]
     */
    public List<String[]> getConversationsForUser(String userId) {
        List<String[]> conversations = new ArrayList<>();
        String sql = """
            SELECT c.id, c.name, c.type,
                   (SELECT m.content FROM messages m WHERE m.conversation_id = c.id
                    ORDER BY m.created_at DESC LIMIT 1) AS last_message,
                   (SELECT m.created_at FROM messages m WHERE m.conversation_id = c.id
                    ORDER BY m.created_at DESC LIMIT 1) AS last_message_time
            FROM conversations c
            JOIN conversation_members cm ON cm.conversation_id = c.id
            WHERE cm.user_id = ? AND cm.left_at IS NULL
            ORDER BY last_message_time DESC
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                conversations.add(new String[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("last_message"),
                        rs.getString("last_message_time")
                });
            }
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur getConversationsForUser : " + e.getMessage());
        }
        return conversations;
    }

    // ═══════════════════════════════════════════════════════════
    // GESTION DES MEMBRES
    // ═══════════════════════════════════════════════════════════

    /**
     * Ajoute un membre à une conversation (groupe).
     */
    public void addMember(String conversationId, String userId) {
        try {
            addMemberInternal(conversationId, userId, "MEMBER");
            System.out.println("[ConversationDAO] Membre ajouté : " + userId + " → " + conversationId);
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur addMember : " + e.getMessage());
        }
    }

    /**
     * Retire un membre d'une conversation (soft delete via left_at).
     */
    public void removeMember(String conversationId, String userId) {
        String sql = "UPDATE conversation_members SET left_at = NOW() WHERE conversation_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ps.executeUpdate();
            System.out.println("[ConversationDAO] Membre retiré : " + userId);
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur removeMember : " + e.getMessage());
        }
    }

    /**
     * Retourne les usernames des membres d'une conversation.
     */
    public List<String> getMemberUsernames(String conversationId) {
        List<String> usernames = new ArrayList<>();
        String sql = """
            SELECT u.username FROM users u
            JOIN conversation_members cm ON cm.user_id = u.id
            WHERE cm.conversation_id = ? AND cm.left_at IS NULL
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur getMemberUsernames : " + e.getMessage());
        }
        return usernames;
    }

    /**
     * Retourne les IDs des membres d'une conversation.
     */
    public List<String> getMemberIds(String conversationId) {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT user_id FROM conversation_members WHERE conversation_id = ? AND left_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur getMemberIds : " + e.getMessage());
        }
        return ids;
    }

    /**
     * Mute/unmute une conversation pour un utilisateur.
     */
    public void setMuted(String conversationId, String userId, boolean muted) {
        String sql = "UPDATE conversation_members SET is_muted = ? WHERE conversation_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, muted);
            ps.setString(2, conversationId);
            ps.setString(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[ConversationDAO] Erreur setMuted : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRE PRIVÉ
    // ═══════════════════════════════════════════════════════════

    private void addMemberInternal(String conversationId, String userId, String role) throws SQLException {
        String sql = "INSERT INTO conversation_members (id, conversation_id, user_id, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, conversationId);
            ps.setString(3, userId);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }
}
