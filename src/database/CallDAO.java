package database;

import models.Call;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO pour la table calls.
 *
 * Méthodes liées au code existant :
 *   - save()          → ClientHandler.traiterDemandeAppel() quand CALL_REQUEST arrive
 *   - updateStatus()  → traiterAppelAccepte(), traiterAppelRefuse(), traiterFinAppel()
 *   - findById()      → extraireCaller() pour retrouver l'appel en cours
 *   - getHistory()    → historique des appels (nouveau feature)
 */
public class CallDAO {

    private final Connection connection;

    public CallDAO() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    // CRÉER UN APPEL
    // ═══════════════════════════════════════════════════════════

    /**
     * Sauvegarde un nouvel appel dans la base de données.
     * Appelé par : ClientHandler.traiterDemandeAppel()
     *
     * @param call objet Call déjà créé par le serveur
     * @param callerId    ID de l'appelant (users.id)
     * @param recipientId ID du destinataire (users.id)
     */
    public void save(Call call, String callerId, String recipientId) {
        String sql = """
            INSERT INTO calls (id, caller_id, recipient_id, call_type, status)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, call.getIdCall());
            ps.setString(2, callerId);
            ps.setString(3, recipientId);
            ps.setString(4, call.getCallType());
            ps.setString(5, call.getStatut());
            ps.executeUpdate();
            System.out.println("[CallDAO] Appel sauvegardé : " + call.getIdCall());
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur save : " + e.getMessage());
        }
    }

    /**
     * Version simplifiée — crée un appel à partir des usernames.
     */
    public String saveByUsernames(String callerUsername, String recipientUsername, String callType) {
        String callId = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO calls (id, caller_id, recipient_id, call_type, status)
            VALUES (?,
                (SELECT id FROM users WHERE username = ?),
                (SELECT id FROM users WHERE username = ?),
                ?, 'EN_ATTENTE')
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, callId);
            ps.setString(2, callerUsername);
            ps.setString(3, recipientUsername);
            ps.setString(4, callType);
            ps.executeUpdate();
            System.out.println("[CallDAO] Appel créé : " + callId);
            return callId;
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur saveByUsernames : " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // METTRE À JOUR LE STATUT
    // ═══════════════════════════════════════════════════════════

    /**
     * Met à jour le statut d'un appel.
     * Appelé par : traiterAppelAccepte (ACCEPTE), traiterAppelRefuse (REFUSE),
     *              traiterFinAppel (TERMINE)
     */
    public void updateStatus(String callId, String status) {
        String sql = "UPDATE calls SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, callId);
            ps.executeUpdate();
            System.out.println("[CallDAO] Statut appel " + callId + " → " + status);
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur updateStatus : " + e.getMessage());
        }
    }

    /**
     * Marque un appel comme accepté et enregistre l'heure de début.
     */
    public void acceptCall(String callId) {
        String sql = "UPDATE calls SET status = 'ACCEPTE', started_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, callId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur acceptCall : " + e.getMessage());
        }
    }

    /**
     * Marque un appel comme refusé.
     */
    public void refuseCall(String callId) {
        updateStatus(callId, "REFUSE");
    }

    /**
     * Termine un appel — calcule la durée et enregistre l'heure de fin.
     */
    public void endCall(String callId, long durationSeconds) {
        String sql = "UPDATE calls SET status = 'TERMINE', duration = ?, ended_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, durationSeconds);
            ps.setString(2, callId);
            ps.executeUpdate();
            System.out.println("[CallDAO] Appel terminé : " + callId + " (" + durationSeconds + "s)");
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur endCall : " + e.getMessage());
        }
    }

    /**
     * Marque un appel comme manqué (non répondu).
     */
    public void missCall(String callId) {
        updateStatus(callId, "MANQUE");
    }

    // ═══════════════════════════════════════════════════════════
    // RECHERCHE
    // ═══════════════════════════════════════════════════════════

    /**
     * Trouve un appel par son ID.
     * Utilisé par : ClientHandler.extraireCaller()
     */
    public Call findById(String callId) {
        String sql = """
            SELECT c.*, caller.username AS caller_name, recipient.username AS recipient_name
            FROM calls c
            JOIN users caller    ON caller.id    = c.caller_id
            JOIN users recipient ON recipient.id = c.recipient_id
            WHERE c.id = ?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, callId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToCall(rs);
            }
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur findById : " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // HISTORIQUE
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère l'historique des appels d'un utilisateur (par username).
     * Trié du plus récent au plus ancien.
     */
    public List<Call> getCallHistory(String username) {
        List<Call> calls = new ArrayList<>();
        String sql = """
            SELECT c.*, caller.username AS caller_name, recipient.username AS recipient_name
            FROM calls c
            JOIN users caller    ON caller.id    = c.caller_id
            JOIN users recipient ON recipient.id = c.recipient_id
            WHERE caller.username = ? OR recipient.username = ?
            ORDER BY c.created_at DESC
            LIMIT 50
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                calls.add(mapResultSetToCall(rs));
            }
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur getCallHistory : " + e.getMessage());
        }
        return calls;
    }

    /**
     * Récupère les appels manqués non consultés d'un utilisateur.
     */
    public List<Call> getMissedCalls(String username) {
        List<Call> calls = new ArrayList<>();
        String sql = """
            SELECT c.*, caller.username AS caller_name, recipient.username AS recipient_name
            FROM calls c
            JOIN users caller    ON caller.id    = c.caller_id
            JOIN users recipient ON recipient.id = c.recipient_id
            WHERE recipient.username = ? AND c.status = 'MANQUE'
            ORDER BY c.created_at DESC
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                calls.add(mapResultSetToCall(rs));
            }
        } catch (SQLException e) {
            System.out.println("[CallDAO] Erreur getMissedCalls : " + e.getMessage());
        }
        return calls;
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRE
    // ═══════════════════════════════════════════════════════════

    private Call mapResultSetToCall(ResultSet rs) throws SQLException {
        Call call = new Call(
                rs.getString("id"),
                rs.getString("caller_name"),
                rs.getString("recipient_name"),
                rs.getString("call_type")
        );
        call.setStatut(rs.getString("status"));
        call.setDuration(rs.getLong("duration"));
        return call;
    }
}
