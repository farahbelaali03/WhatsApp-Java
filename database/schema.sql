-- ============================================================
-- WhatsApp-Java — Database Schema
-- MySQL 8.0+
-- Run: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS whatsapp_java
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE whatsapp_java;

-- ────────────────────────────────────────────────────────────
-- TABLE 1: users
-- Maps: User.java (id, username, status)
-- Added: password_hash, email, phone, profile_picture, bio,
--        last_seen, created_at
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    email           VARCHAR(100)    UNIQUE,
    phone_number    VARCHAR(20)     UNIQUE,
    profile_picture VARCHAR(500)    DEFAULT NULL,
    bio             VARCHAR(200)    DEFAULT 'Hey there! I am using WhatsApp',
    status          ENUM('ONLINE', 'OFFLINE', 'BUSY', 'AWAY')
                                    NOT NULL DEFAULT 'OFFLINE',
    last_seen       DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_last_seen (last_seen)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 2: contacts (friend list / address book)
-- NEW — Currently no contact system exists
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS contacts (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id         VARCHAR(36)     NOT NULL,
    contact_id      VARCHAR(36)     NOT NULL,
    nickname        VARCHAR(50)     DEFAULT NULL,
    is_blocked      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_favorite     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_contact (user_id, contact_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 3: conversations
-- NEW — Groups messages by conversation (1-to-1 or group)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    name            VARCHAR(100)    DEFAULT NULL,          -- NULL for 1-to-1
    type            ENUM('PRIVATE', 'GROUP') NOT NULL DEFAULT 'PRIVATE',
    group_picture   VARCHAR(500)    DEFAULT NULL,
    created_by      VARCHAR(36)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_type (type),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 4: conversation_members
-- NEW — Who is in which conversation (supports groups)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversation_members (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    conversation_id VARCHAR(36)     NOT NULL,
    user_id         VARCHAR(36)     NOT NULL,
    role            ENUM('MEMBER', 'ADMIN', 'OWNER')
                                    NOT NULL DEFAULT 'MEMBER',
    joined_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at         DATETIME        DEFAULT NULL,
    is_muted        BOOLEAN         NOT NULL DEFAULT FALSE,

    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)         REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_conv_user (conversation_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 5: messages
-- Maps: Message.java (sender, recipient, content, timestamp, isRead)
-- Added: conversation_id, message_type, media_url, reply_to,
--        is_deleted, is_forwarded
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    conversation_id VARCHAR(36)     NOT NULL,
    sender_id       VARCHAR(36)     NOT NULL,
    content         TEXT            DEFAULT NULL,
    message_type    ENUM('TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'LOCATION', 'STICKER')
                                    NOT NULL DEFAULT 'TEXT',
    media_url       VARCHAR(500)    DEFAULT NULL,
    media_size      BIGINT          DEFAULT NULL,          -- in bytes
    reply_to        VARCHAR(36)     DEFAULT NULL,          -- reply to another message
    is_forwarded    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id)       REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to)        REFERENCES messages(id) ON DELETE SET NULL,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX idx_content_search (content)            -- enables message search
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 6: message_status (per-user read receipts)
-- Maps: Message.isRead — now tracked per recipient
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS message_status (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    message_id      VARCHAR(36)     NOT NULL,
    user_id         VARCHAR(36)     NOT NULL,
    status          ENUM('SENT', 'DELIVERED', 'READ')
                                    NOT NULL DEFAULT 'SENT',
    delivered_at    DATETIME        DEFAULT NULL,
    read_at         DATETIME        DEFAULT NULL,

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_msg_user (message_id, user_id),
    INDEX idx_message (message_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 7: calls
-- Maps: Call.java (idCall, caller, recipient, callType, statut, duration)
-- Added: started_at, ended_at, MANQUE status
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS calls (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    caller_id       VARCHAR(36)     NOT NULL,
    recipient_id    VARCHAR(36)     NOT NULL,
    call_type       ENUM('AUDIO', 'VIDEO') NOT NULL,
    status          ENUM('EN_ATTENTE', 'ACCEPTE', 'REFUSE', 'TERMINE', 'MANQUE')
                                    NOT NULL DEFAULT 'EN_ATTENTE',
    duration        INT UNSIGNED    NOT NULL DEFAULT 0,    -- in seconds
    started_at      DATETIME        DEFAULT NULL,
    ended_at        DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (caller_id)    REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_caller (caller_id),
    INDEX idx_recipient (recipient_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 8: user_sessions (connection history)
-- Maps: Server.clients list + Client.connecte boolean
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_sessions (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id         VARCHAR(36)     NOT NULL,
    ip_address      VARCHAR(45)     NOT NULL,
    port            INT             DEFAULT NULL,
    connected_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at DATETIME        DEFAULT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_connected_at (connected_at)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 9: media_files (file attachments)
-- NEW — For future file sharing support
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS media_files (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    uploader_id     VARCHAR(36)     NOT NULL,
    file_name       VARCHAR(255)    NOT NULL,
    file_path       VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(50)     NOT NULL,              -- MIME type
    file_size       BIGINT          NOT NULL,              -- bytes
    uploaded_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB;


-- ────────────────────────────────────────────────────────────
-- TABLE 10: notifications (offline message queue)
-- NEW — Queue notifications for offline users
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id              VARCHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id         VARCHAR(36)     NOT NULL,
    type            ENUM('MESSAGE', 'CALL', 'GROUP_INVITE', 'SYSTEM')
                                    NOT NULL,
    reference_id    VARCHAR(36)     DEFAULT NULL,           -- message_id or call_id
    content         VARCHAR(500)    DEFAULT NULL,
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_unread (user_id, is_read)
) ENGINE=InnoDB;


-- ============================================================
-- SAMPLE DATA (for development & testing)
-- ============================================================

-- Team members as test users
INSERT INTO users (id, username, password_hash, email, status, bio) VALUES
    ('u1', 'Amal',    SHA2('password123', 256), 'amal@ensa.ma',    'OFFLINE', 'Hey there!'),
    ('u2', 'Afnane',  SHA2('password123', 256), 'afnane@ensa.ma',  'OFFLINE', 'Available'),
    ('u3', 'Souraya', SHA2('password123', 256), 'souraya@ensa.ma', 'OFFLINE', 'Busy coding'),
    ('u4', 'Chaimaa', SHA2('password123', 256), 'chaimaa@ensa.ma', 'OFFLINE', 'At work'),
    ('u5', 'Farah',   SHA2('password123', 256), 'farah@ensa.ma',   'OFFLINE', 'Hello World!');

-- Contacts
INSERT INTO contacts (user_id, contact_id) VALUES
    ('u1', 'u2'), ('u1', 'u3'), ('u1', 'u4'), ('u1', 'u5'),
    ('u2', 'u1'), ('u2', 'u3'), ('u2', 'u4'),
    ('u3', 'u1'), ('u3', 'u2'),
    ('u4', 'u1'), ('u4', 'u5'),
    ('u5', 'u1'), ('u5', 'u4');

-- Private conversation: Amal <-> Afnane
INSERT INTO conversations (id, type) VALUES ('conv1', 'PRIVATE');
INSERT INTO conversation_members (conversation_id, user_id, role) VALUES
    ('conv1', 'u1', 'MEMBER'),
    ('conv1', 'u2', 'MEMBER');

-- Private conversation: Amal <-> Souraya
INSERT INTO conversations (id, type) VALUES ('conv2', 'PRIVATE');
INSERT INTO conversation_members (conversation_id, user_id, role) VALUES
    ('conv2', 'u1', 'MEMBER'),
    ('conv2', 'u3', 'MEMBER');

-- Group conversation: Project Team
INSERT INTO conversations (id, name, type, created_by) VALUES
    ('conv3', 'Projet WhatsApp ENSA', 'GROUP', 'u1');
INSERT INTO conversation_members (conversation_id, user_id, role) VALUES
    ('conv3', 'u1', 'OWNER'),
    ('conv3', 'u2', 'ADMIN'),
    ('conv3', 'u3', 'MEMBER'),
    ('conv3', 'u4', 'MEMBER'),
    ('conv3', 'u5', 'MEMBER');

-- Sample messages in conv1 (Amal <-> Afnane)
INSERT INTO messages (id, conversation_id, sender_id, content, message_type) VALUES
    ('m1', 'conv1', 'u1', 'Salut Afnane !', 'TEXT'),
    ('m2', 'conv1', 'u2', 'Salut Amal, ça va ?', 'TEXT'),
    ('m3', 'conv1', 'u1', 'Oui très bien, tu travailles sur quoi ?', 'TEXT'),
    ('m4', 'conv1', 'u2', 'Sur la partie Client TCP', 'TEXT'),
    ('m5', 'conv1', 'u1', 'Super, on se call pour sync ?', 'TEXT');

-- Message statuses (read receipts)
INSERT INTO message_status (message_id, user_id, status, delivered_at, read_at) VALUES
    ('m1', 'u2', 'READ',      NOW(), NOW()),
    ('m2', 'u1', 'READ',      NOW(), NOW()),
    ('m3', 'u2', 'READ',      NOW(), NOW()),
    ('m4', 'u1', 'DELIVERED', NOW(), NULL),
    ('m5', 'u2', 'SENT',      NULL,  NULL);

-- Sample messages in conv3 (Group)
INSERT INTO messages (id, conversation_id, sender_id, content, message_type) VALUES
    ('m6', 'conv3', 'u1', 'Bienvenue dans le groupe du projet !', 'TEXT'),
    ('m7', 'conv3', 'u3', 'Merci Amal 👋', 'TEXT'),
    ('m8', 'conv3', 'u4', 'Je commence le UDPServer', 'TEXT');

-- Sample calls
INSERT INTO calls (id, caller_id, recipient_id, call_type, status, duration, started_at, ended_at) VALUES
    ('c1', 'u1', 'u2', 'AUDIO', 'TERMINE', 120,
     DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 118 MINUTE)),
    ('c2', 'u3', 'u1', 'VIDEO', 'TERMINE', 300,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1435 MINUTE)),
    ('c3', 'u1', 'u4', 'AUDIO', 'MANQUE', 0,
     DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL),
    ('c4', 'u2', 'u5', 'VIDEO', 'REFUSE', 0,
     DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR));

-- Sample session history
INSERT INTO user_sessions (user_id, ip_address, port, connected_at, disconnected_at) VALUES
    ('u1', '127.0.0.1', 5000, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    ('u2', '192.168.1.15', 5000, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    ('u3', '192.168.1.22', 5000, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL);


-- ============================================================
-- USEFUL VIEWS (optional, for convenience)
-- ============================================================

-- View: Last message per conversation (for sidebar display)
CREATE OR REPLACE VIEW v_conversation_preview AS
SELECT
    c.id AS conversation_id,
    c.name AS conversation_name,
    c.type,
    m.content AS last_message,
    m.created_at AS last_message_time,
    u.username AS last_sender
FROM conversations c
LEFT JOIN messages m ON m.id = (
    SELECT m2.id FROM messages m2
    WHERE m2.conversation_id = c.id
    ORDER BY m2.created_at DESC LIMIT 1
)
LEFT JOIN users u ON u.id = m.sender_id
ORDER BY m.created_at DESC;

-- View: Unread message count per user per conversation
CREATE OR REPLACE VIEW v_unread_counts AS
SELECT
    cm.user_id,
    cm.conversation_id,
    COUNT(ms.id) AS unread_count
FROM conversation_members cm
JOIN messages msg ON msg.conversation_id = cm.conversation_id
    AND msg.sender_id != cm.user_id
LEFT JOIN message_status ms ON ms.message_id = msg.id
    AND ms.user_id = cm.user_id
    AND ms.status != 'READ'
WHERE cm.left_at IS NULL
GROUP BY cm.user_id, cm.conversation_id;

-- View: Call history for a user
CREATE OR REPLACE VIEW v_call_history AS
SELECT
    c.*,
    caller.username AS caller_name,
    recipient.username AS recipient_name
FROM calls c
JOIN users caller    ON caller.id    = c.caller_id
JOIN users recipient ON recipient.id = c.recipient_id
ORDER BY c.created_at DESC;


SELECT '✅ Database whatsapp_java created successfully!' AS result;
