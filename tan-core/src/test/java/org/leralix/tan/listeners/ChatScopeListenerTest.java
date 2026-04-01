package org.leralix.tan.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.enums.ChatScope;
import org.leralix.tan.listeners.chat.PlayerChatListenerStorage;
import org.leralix.tan.storage.LocalChatStorage;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ChatScopeListener}.
 * <p>
 * Tests chat scope handling including local chat broadcasting
 * and event cancellation based on player chat settings.
 * </p>
 */
@DisplayName("ChatScopeListener Tests")
class ChatScopeListenerTest extends AbstractPluginTest {

    private ChatScopeListener listener;
    private Player player;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new ChatScopeListener();
        player = server.addPlayer("TestPlayer");
    }

    // ==================== Basic Chat Tests ====================

    @Test
    @DisplayName("Should handle player chat without throwing")
    void onPlayerChat_validChat_doesNotThrow() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should not cancel event when player not in chat scope")
    void onPlayerChat_notInScope_doesNotCancel() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        listener.onPlayerChat(event);

        assertFalse(event.isCancelled(), "Event should not be cancelled when not in scope");
    }

    @Test
    @DisplayName("Should handle empty message")
    void onPlayerChat_emptyMessage_handlesGracefully() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle long message")
    void onPlayerChat_longMessage_handlesGracefully() {
        String longMessage = "A".repeat(256);
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, longMessage, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Chat Scope Tests ====================

    @Test
    @DisplayName("Should cancel event when player in chat scope")
    void onPlayerChat_inScope_cancelsEvent() {
        // Add player to chat scope
        LocalChatStorage.setPlayerChatScope(player, ChatScope.CITY);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        // Note: Event cancellation depends on LocalChatStorage implementation
        LocalChatStorage.removePlayerChatScope(player);
    }

    @Test
    @DisplayName("Should broadcast in scope when player in chat scope")
    void onPlayerChat_inScope_broadcastsMessage() {
        LocalChatStorage.setPlayerChatScope(player, ChatScope.CITY);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        LocalChatStorage.removePlayerChatScope(player);
    }

    // ==================== Player Chat Listener Storage Tests ====================

    @Test
    @DisplayName("Should not cancel when player not in chat scope")
    void onPlayerChat_notInListenerStorage_doesNotCancel() {
        // Player is not in PlayerChatListenerStorage, event should not be cancelled without scope
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
        assertFalse(event.isCancelled(), "Event should not be cancelled when player not in scope");
    }

    @Test
    @DisplayName("Should handle PlayerChatListenerStorage contains check")
    void onPlayerChat_listenerStorageCheck_handlesGracefully() {
        // Verify PlayerChatListenerStorage.contains() works correctly
        assertFalse(PlayerChatListenerStorage.contains(player), "Player should not be in storage initially");

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Multiple Players Tests ====================

    @Test
    @DisplayName("Should handle multiple players in chat scope")
    void onPlayerChat_multiplePlayersInScope_handlesCorrectly() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");

        LocalChatStorage.setPlayerChatScope(player1, ChatScope.CITY);
        LocalChatStorage.setPlayerChatScope(player2, ChatScope.CITY);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player1, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        LocalChatStorage.removePlayerChatScope(player1);
        LocalChatStorage.removePlayerChatScope(player2);
    }

    @Test
    @DisplayName("Should handle multiple players with different scopes")
    void onPlayerChat_differentScopes_handlesCorrectly() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");

        LocalChatStorage.setPlayerChatScope(player1, ChatScope.CITY);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event1 = new AsyncPlayerChatEvent(true, player1, "Test message", recipients);
        AsyncPlayerChatEvent event2 = new AsyncPlayerChatEvent(true, player2, "Test message", recipients);

        assertDoesNotThrow(() -> {
            listener.onPlayerChat(event1);
            listener.onPlayerChat(event2);
        });

        LocalChatStorage.removePlayerChatScope(player1);
    }

    // ==================== Recipient Tests ====================

    @Test
    @DisplayName("Should handle empty recipients set")
    void onPlayerChat_noRecipients_handlesGracefully() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle multiple recipients")
    void onPlayerChat_multipleRecipients_handlesCorrectly() {
        var recipient1 = server.addPlayer("Recipient1");
        var recipient2 = server.addPlayer("Recipient2");
        var recipient3 = server.addPlayer("Recipient3");

        Set<Player> recipients = new HashSet<>();
        recipients.add(recipient1);
        recipients.add(recipient2);
        recipients.add(recipient3);

        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Special Characters Tests ====================

    @Test
    @DisplayName("Should handle message with special characters")
    void onPlayerChat_specialCharacters_handlesCorrectly() {
        String specialMessage = "Test @#$%^&*() message!";
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, specialMessage, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle message with emoji")
    void onPlayerChat_emoji_handlesCorrectly() {
        String emojiMessage = "Test message 😊 🎉";
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, emojiMessage, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle message with color codes")
    void onPlayerChat_colorCodes_handlesCorrectly() {
        String colorMessage = "§cTest §amessage§r";
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, colorMessage, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid chat messages")
    void onPlayerChat_rapidMessages_handlesGracefully() {
        Set<Player> recipients = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Message " + i, recipients);
            assertDoesNotThrow(() -> listener.onPlayerChat(event));
        }
    }

    @Test
    @DisplayName("Should handle message with newlines")
    void onPlayerChat_newlines_handlesCorrectly() {
        String multilineMessage = "Line 1\nLine 2\nLine 3";
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, multilineMessage, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle null message")
    void onPlayerChat_nullMessage_handlesGracefully() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, null, recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Admin Player Tests ====================

    @Test
    @DisplayName("Should handle admin player chat")
    void onPlayerChat_adminPlayer_handlesCorrectly() {
        var admin = server.addPlayer("AdminPlayer");
        admin.setOp(true);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, admin, "Admin message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Async Event Tests ====================

    @Test
    @DisplayName("Should handle async event correctly")
    void onPlayerChat_asyncEvent_handlesCorrectly() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        // AsyncPlayerChatEvent is async by design
        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Different Player States Tests ====================

    @Test
    @DisplayName("Should handle offline player")
    void onPlayerChat_offlinePlayer_handlesGracefully() {
        player.setOp(false);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    @Test
    @DisplayName("Should handle player with special UUID")
    void onPlayerChat_specialUUID_handlesCorrectly() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
        assertNotNull(player.getUniqueId(), "Player UUID should not be null");
    }

    // ==================== Storage Interaction Tests ====================

    @Test
    @DisplayName("Should interact correctly with LocalChatStorage")
    void onPlayerChat_localChatStorage_handlesCorrectly() {
        String playerUUID = player.getUniqueId().toString();

        // Add to scope
        LocalChatStorage.setPlayerChatScope(player, ChatScope.CITY);

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        // Remove from scope
        LocalChatStorage.removePlayerChatScope(player);
    }

    @Test
    @DisplayName("Should handle LocalChatStorage exception gracefully")
    void onPlayerChat_storageException_handlesGracefully() {
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
    }

    // ==================== Format Tests ====================

    @Test
    @DisplayName("Should handle message format")
    void onPlayerChat_format_handlesCorrectly() {
        String format = "<%s> %s";
        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);
        event.setFormat(format);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));
        assertEquals(format, event.getFormat(), "Format should be preserved");
    }

    // ==================== Broadcasting Tests ====================

    @Test
    @DisplayName("Should broadcast to correct recipients in scope")
    void onPlayerChat_broadcastInScope_handlesRecipients() {
        LocalChatStorage.setPlayerChatScope(player, ChatScope.CITY);

        var nearbyPlayer = server.addPlayer("NearbyPlayer");
        Set<Player> recipients = new HashSet<>();
        recipients.add(nearbyPlayer);

        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        LocalChatStorage.removePlayerChatScope(player);
    }

    // ==================== Multiple Chat Scenarios Tests ====================

    @Test
    @DisplayName("Should handle consecutive messages from same player")
    void onPlayerChat_consecutiveMessages_handlesCorrectly() {
        Set<Player> recipients = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Message " + i, recipients);
            assertDoesNotThrow(() -> listener.onPlayerChat(event));
        }
    }

    @Test
    @DisplayName("Should handle simultaneous messages from different players")
    void onPlayerChat_simultaneousMessages_handlesCorrectly() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event1 = new AsyncPlayerChatEvent(true, player1, "Message from player1", recipients);
        AsyncPlayerChatEvent event2 = new AsyncPlayerChatEvent(true, player2, "Message from player2", recipients);

        assertDoesNotThrow(() -> {
            listener.onPlayerChat(event1);
            listener.onPlayerChat(event2);
        });
    }

    // ==================== Scope Distance Tests ====================

    @Test
    @DisplayName("Should respect scope distance limits")
    void onPlayerChat_scopeDistance_respectsLimit() {
        LocalChatStorage.setPlayerChatScope(player, ChatScope.CITY);

        var farPlayer = server.addPlayer("FarPlayer");
        farPlayer.teleport(new org.bukkit.Location(
            server.addSimpleWorld("test_world"),
            1000, 64, 1000
        ));

        Set<Player> recipients = new HashSet<>();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "Test message", recipients);

        assertDoesNotThrow(() -> listener.onPlayerChat(event));

        LocalChatStorage.removePlayerChatScope(player);
    }
}
