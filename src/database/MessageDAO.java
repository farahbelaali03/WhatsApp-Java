package database;

import models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO pour la table messages + message_status.
 *
 * Méthodes liées au code existant :
 *   - save()                → ClientHandler.traiterMessage()
 *   - getByConversation()   → charger l'historique dans ChatWindow
 *   - getUndelivered()      → messages envoyés pendant que le destinataire était offline
 *   - updateStatus()        → read receipts (✓ ✓✓)
 *   - searchMessages()      → recherche dans les conversations
 */
public class MessageDAO {

    private final Connection connection;

    public MessageDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    // SAUVEGARDER UN MESSAGE
    // ═══════════════════════════════════════════════════════════

    /**
     * Sauvegarde un message dans la base de données.
     * Appelé par : ClientHandler.traiterMessage()
     *
     * @param conversationId ID de la conversation
     * @param senderId       ID de l'expéditeur (users.id)
     * @param content        Contenu du message
     * @param messageType    TEXT, IMAGE, VIDEO, AUDIO, FILE
     * @return l'ID du message créé
     */
    public String save(String conversationId, String senderId, String content, String messageType) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO messages (id, conversation_id, sender_id, content, message_type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, conversationId);
            ps.setString(3, senderId);
            ps.setString(4, content);
            ps.setString(5, messageType);
            ps.executeUpdate();
            System.out.println("[MessageDAO] Message sauvegardé : " + id);
            return id;
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur save : " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarde un message à partir d'un objet Message existant.
     * Version simplifiée compatible avec le code actuel.
     */
    public String saveFromMessage(Message message, String conversationId, String senderId) {
        return save(conversationId, senderId, message.getContent(), "TEXT");
    }

    // ═══════════════════════════════════════════════════════════
    // RÉCUPÉRER LES MESSAGES
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère les messages d'une conversation (les plus récents en dernier).
     * Utilisé par : ChatWindow pour charger l'historique.
     *
     * @param conversationId ID de la conversation
     * @param limit          nombre max de messages à récupérer
     * @param offset         décalage (pour pagination)
     */
    public List<Message> getByConversation(String conversationId, int limit, int offset) {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT m.*, u.username AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE m.conversation_id = ? AND m.is_deleted = FALSE
            ORDER BY m.created_at ASC
            LIMIT ? OFFSET ?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur getByConversation : " + e.getMessage());
        }
        return messages;
    }

    /**
     * Récupère les 50 derniers messages d'une conversation.
     */
    public List<Message> getRecentMessages(String conversationId) {
        return getByConversation(conversationId, 50, 0);
    }

    /**
     * Récupère les messages non délivrés pour un utilisateur.
     * → Messages envoyés pendant que l'utilisateur était offline.
     * Utilisé par : au moment du CONNECT pour envoyer les messages en attente.
     */
    public List<Message> getUndeliveredMessages(String userId) {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT m.*, u.username AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            JOIN conversation_members cm ON cm.conversation_id = m.conversation_id
            LEFT JOIN message_status ms ON ms.message_id = m.id AND ms.user_id = ?
            WHERE cm.user_id = ?
              AND m.sender_id != ?
              AND (ms.status IS NULL OR ms.status = 'SENT')
              AND m.is_deleted = FALSE
            ORDER BY m.created_at ASC
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur getUndeliveredMessages : " + e.getMessage());
        }
        return messages;
    }

    // ═══════════════════════════════════════════════════════════
    // READ RECEIPTS (message_status)
    // ═══════════════════════════════════════════════════════════

    /**
     * Crée le statut initial d'un message pour un destinataire.
     * Appelé après save() pour chaque membre de la conversation.
     */
    public void createMessageStatus(String messageId, String userId, String status) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO message_status (id, message_id, user_id, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, messageId);
            ps.setString(3, userId);
            ps.setString(4, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Ignore duplicate
            if (!e.getMessage().contains("Duplicate")) {
                System.out.println("[MessageDAO] Erreur createMessageStatus : " + e.getMessage());
            }
        }
    }

    /**
     * Met à jour le statut d'un message pour un utilisateur → DELIVERED.
     * Appelé quand le message arrive chez le destinataire.
     */
    public void markAsDelivered(String messageId, String userId) {
        String sql = """
            UPDATE message_status
            SET status = 'DELIVERED', delivered_at = NOW()
            WHERE message_id = ? AND user_id = ? AND status = 'SENT'
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, messageId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur markAsDelivered : " + e.getMessage());
        }
    }

    /**
     * Marque tous les messages non délivrés d'un utilisateur comme DELIVERED.
     * Appelé lors de la connexion.
     */
    public void markAllUndeliveredAsDelivered(String userId) {
        String sql = """
            UPDATE message_status
            SET status = 'DELIVERED', delivered_at = NOW()
            WHERE user_id = ? AND status = 'SENT'
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
            System.out.println("[MessageDAO] Messages marqués DELIVERED pour : " + userId);
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur markAllUndeliveredAsDelivered : " + e.getMessage());
        }
    }

    /**
     * Met à jour le statut d'un message pour un utilisateur → READ.
     * Appelé quand l'utilisateur ouvre la conversation (ChatWindow).
     */
    public void markAsRead(String messageId, String userId) {
        String sql = """
            UPDATE message_status
            SET status = 'READ', read_at = NOW()
            WHERE message_id = ? AND user_id = ?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, messageId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur markAsRead : " + e.getMessage());
        }
    }

    /**
     * Marque tous les messages d'une conversation comme lus pour un utilisateur.
     * Appelé quand l'utilisateur ouvre ChatWindow.
     */
    public void markAllAsRead(String conversationId, String userId) {
        String sql = """
            UPDATE message_status ms
            JOIN messages m ON m.id = ms.message_id
            SET ms.status = 'READ', ms.read_at = NOW()
            WHERE m.conversation_id = ? AND ms.user_id = ? AND ms.status != 'READ'
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur markAllAsRead : " + e.getMessage());
        }
    }

    /**
     * Compte les messages non lus pour un utilisateur dans une conversation.
     */
    public int getUnreadCount(String conversationId, String userId) {
        String sql = """
            SELECT COUNT(*) FROM message_status ms
            JOIN messages m ON m.id = ms.message_id
            WHERE m.conversation_id = ? AND ms.user_id = ? AND ms.status != 'READ'
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur getUnreadCount : " + e.getMessage());
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════
    // RECHERCHE
    // ═══════════════════════════════════════════════════════════

    /**
     * Recherche des messages par contenu dans une conversation.
     */
    public List<Message> searchMessages(String conversationId, String query) {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT m.*, u.username AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE m.conversation_id = ? AND m.content LIKE ? AND m.is_deleted = FALSE
            ORDER BY m.created_at DESC
            LIMIT 50
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, "%" + query + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur searchMessages : " + e.getMessage());
        }
        return messages;
    }

    // ═══════════════════════════════════════════════════════════
    // SUPPRESSION
    // ═══════════════════════════════════════════════════════════

    /**
     * Supprime un message (soft delete).
     */
    public void deleteMessage(String messageId) {
        String sql = "UPDATE messages SET is_deleted = TRUE, content = NULL WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, messageId);
            ps.executeUpdate();
            System.out.println("[MessageDAO] Message supprimé : " + messageId);
        } catch (SQLException e) {
            System.out.println("[MessageDAO] Erreur deleteMessage : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRE
    // ═══════════════════════════════════════════════════════════

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String senderName = rs.getString("sender_name");
        String content = rs.getString("content");
        Message msg = new Message(senderName, "", content);
        msg.setId(id);

        // Récupérer le timestamp depuis la base
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            msg.setTimestamp(ts.toLocalDateTime());
        }
        return msg;
    }
}
