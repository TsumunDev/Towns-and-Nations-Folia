package org.leralix.tan.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

/**
 * RedisManager - Manages Redis communication for multi-server synchronization.
 *
 * Features:
 * - Server identification via unique server-id
 * - Heartbeat system for health monitoring
 * - Pub/Sub channels for cross-server communication
 * - Server registry to track online servers
 */
public class RedisManager {

  private static final Logger logger = Logger.getLogger(RedisManager.class.getName());
  private static final Gson GSON = new Gson();

  private static RedissonClient redisClient;
  private static String serverId;
  private static String serverName;

  // Channel names from config
  private static String channelGlobal;
  private static String channelServerEvents;
  private static String channelTownSync;
  private static String channelPlayerSync;

  // Heartbeat settings
  private static boolean heartbeatEnabled;
  private static int heartbeatInterval;
  private static int heartbeatTimeout;
  private static int heartbeatTaskId = -1;

  // Server registry (server-id -> last heartbeat timestamp)
  private static final Map<String, Long> onlineServers = new ConcurrentHashMap<>();

  // Message listeners per channel
  private static final Map<String, Set<Consumer<JsonObject>>> messageListeners = new ConcurrentHashMap<>();

  /**
   * Initialize the RedisManager with the given plugin and configuration.
   *
   * @param plugin The plugin instance
   * @param config The configuration file
   */
  public static void initialize(Plugin plugin, FileConfiguration config) {
    if (!config.getBoolean("redis.enabled", true)) {
      logger.info("[TaN-Redis] Redis is disabled in config.yml");
      return;
    }

    // Create Redis client
    redisClient = RedisClusterConfig.createRedisClient(config);
    if (redisClient == null) {
      logger.warning("[TaN-Redis] Failed to create Redis client");
      return;
    }

    // Read server identification
    serverId = config.getString("redis.server-id", "unknown");
    serverName = config.getString("server.name", "server-1");

    logger.info("[TaN-Redis] Server ID: " + serverId);
    logger.info("[TaN-Redis] Server Name: " + serverName);

    // Read channel names
    channelGlobal = config.getString("redis.channels.global", "tan:global");
    channelServerEvents = config.getString("redis.channels.server-events", "tan:server-events");
    channelTownSync = config.getString("redis.channels.town-sync", "tan:town-sync");
    channelPlayerSync = config.getString("redis.channels.player-sync", "tan:player-sync");

    logger.info("[TaN-Redis] Channels configured:");
    logger.info("[TaN-Redis]   - Global: " + channelGlobal);
    logger.info("[TaN-Redis]   - Server Events: " + channelServerEvents);
    logger.info("[TaN-Redis]   - Town Sync: " + channelTownSync);
    logger.info("[TaN-Redis]   - Player Sync: " + channelPlayerSync);

    // Initialize heartbeat
    heartbeatEnabled = config.getBoolean("redis.heartbeat.enabled", true);
    heartbeatInterval = config.getInt("redis.heartbeat.interval", 30);
    heartbeatTimeout = config.getInt("redis.heartbeat.timeout", 60);

    if (heartbeatEnabled) {
      startHeartbeat(plugin);
      startServerEventListener();
      logger.info("[TaN-Redis] Heartbeat started (interval: " + heartbeatInterval + "s, timeout: " + heartbeatTimeout + "s)");
    }

    // Initialize QueryCacheManager with Redis client
    QueryCacheManager.initialize(redisClient);

    logger.info("[TaN-Redis] RedisManager initialized successfully");
  }

  /**
   * Start the heartbeat task.
   */
  private static void startHeartbeat(Plugin plugin) {
    // Clear any existing heartbeat data for this server
    String heartbeatKey = "tan:heartbeat:" + serverId;
    redisClient.getBucket(heartbeatKey).delete();

    // Schedule heartbeat task
    heartbeatTaskId = Bukkit.getScheduler()
        .runTaskTimerAsynchronously(
            plugin,
            () -> sendHeartbeat(),
            20L, // Initial delay: 1 second (20 ticks)
            heartbeatInterval * 20L // Interval in ticks
        )
        .getTaskId();

    logger.info("[TaN-Redis] Heartbeat task scheduled (ID: " + heartbeatTaskId + ")");
  }

  /**
   * Start listening to server events channel.
   */
  private static void startServerEventListener() {
    RTopic topic = redisClient.getTopic(channelServerEvents, StringCodec.INSTANCE);

    topic.addListener(String.class, (channel, message) -> {
      try {
        JsonObject json = GSON.fromJson(message, JsonObject.class);
        String type = json.get("type").getAsString();
        String senderServerId = json.get("serverId").getAsString();

        // Ignore own messages
        if (senderServerId.equals(serverId)) {
          return;
        }

        switch (type) {
          case "heartbeat":
            handleHeartbeat(json);
            break;
          case "shutdown":
            handleShutdown(json);
            break;
        }
      } catch (Exception e) {
        logger.warning("[TaN-Redis] Error processing server event: " + e.getMessage());
      }
    });

    logger.info("[TaN-Redis] Listening to server events on: " + channelServerEvents);
  }

  /**
   * Send a heartbeat message to indicate this server is online.
   */
  private static void sendHeartbeat() {
    try {
      JsonObject heartbeat = new JsonObject();
      heartbeat.addProperty("type", "heartbeat");
      heartbeat.addProperty("serverId", serverId);
      heartbeat.addProperty("serverName", serverName);
      heartbeat.addProperty("timestamp", System.currentTimeMillis());
      heartbeat.addProperty("players", Bukkit.getOnlinePlayers().size());
      heartbeat.addProperty("maxPlayers", Bukkit.getMaxPlayers());

      String key = "tan:heartbeat:" + serverId;
      redisClient.<String>getBucket(key).set(heartbeat.toString());
      redisClient.getBucket(key).expire(heartbeatTimeout + 5, TimeUnit.SECONDS);

      // Also publish to server events channel
      publishToChannel(channelServerEvents, heartbeat.toString());

      // Clean up offline servers
      cleanupOfflineServers();
    } catch (Exception e) {
      logger.warning("[TaN-Redis] Error sending heartbeat: " + e.getMessage());
    }
  }

  /**
   * Handle incoming heartbeat from another server.
   */
  private static void handleHeartbeat(JsonObject json) {
    String senderServerId = json.get("serverId").getAsString();
    long timestamp = json.get("timestamp").getAsLong();

    onlineServers.put(senderServerId, timestamp);
  }

  /**
   * Handle shutdown event from another server.
   */
  private static void handleShutdown(JsonObject json) {
    String senderServerId = json.get("serverId").getAsString();
    onlineServers.remove(senderServerId);
    logger.info("[TaN-Redis] Server " + senderServerId + " has shut down");
  }

  /**
   * Remove servers that haven't sent a heartbeat within the timeout period.
   */
  private static void cleanupOfflineServers() {
    long currentTime = System.currentTimeMillis();
    long timeoutMs = heartbeatTimeout * 1000L;

    onlineServers.entrySet().removeIf(entry -> {
      if (currentTime - entry.getValue() > timeoutMs) {
        logger.info("[TaN-Redis] Server " + entry.getKey() + " timed out (no heartbeat)");
        return true;
      }
      return false;
    });
  }

  /**
   * Publish a message to a specific channel.
   *
   * @param channel The channel name
   * @param message The message to publish (JSON string)
   */
  public static void publishToChannel(String channel, String message) {
    if (redisClient == null) {
      logger.warning("[TaN-Redis] Cannot publish - Redis not initialized");
      return;
    }

    try {
      RTopic topic = redisClient.getTopic(channel, StringCodec.INSTANCE);
      topic.publish(message);
    } catch (Exception e) {
      logger.warning("[TaN-Redis] Error publishing to channel " + channel + ": " + e.getMessage());
    }
  }

  /**
   * Subscribe to a channel and register a message listener.
   *
   * @param channel The channel name
   * @param listener The callback function for incoming messages
   */
  public static void subscribeToChannel(String channel, Consumer<JsonObject> listener) {
    if (redisClient == null) {
      logger.warning("[TaN-Redis] Cannot subscribe - Redis not initialized");
      return;
    }

    // Register listener
    messageListeners.computeIfAbsent(channel, k -> new HashSet<>()).add(listener);

    RTopic topic = redisClient.getTopic(channel, StringCodec.INSTANCE);
    topic.addListener(String.class, (ch, message) -> {
      try {
        JsonObject json = GSON.fromJson(message, JsonObject.class);

        // Check if message is from this server
        if (json.has("serverId") && json.get("serverId").getAsString().equals(serverId)) {
          return; // Ignore own messages
        }

        // Notify all listeners for this channel
        for (Consumer<JsonObject> callback : messageListeners.get(channel)) {
          try {
            callback.accept(json);
          } catch (Exception e) {
            logger.warning("[TaN-Redis] Error in message listener: " + e.getMessage());
          }
        }
      } catch (Exception e) {
        logger.warning("[TaN-Redis] Error parsing message: " + e.getMessage());
      }
    });

    logger.info("[TaN-Redis] Subscribed to channel: " + channel);
  }

  /**
   * Broadcast a message to all servers via the global channel.
   *
   * @param type The message type
   * @param data The data to include in the message
   */
  public static void broadcastGlobal(String type, JsonObject data) {
    JsonObject message = new JsonObject();
    message.addProperty("type", type);
    message.addProperty("serverId", serverId);
    message.addProperty("timestamp", System.currentTimeMillis());
    message.add("data", data);

    publishToChannel(channelGlobal, message.toString());
  }

  /**
   * Send a town synchronization message.
   *
   * @param type The sync type (create, update, delete)
   * @param townId The town ID
   * @param data Additional town data
   */
  public static void syncTown(String type, String townId, JsonObject data) {
    JsonObject message = new JsonObject();
    message.addProperty("type", type);
    message.addProperty("serverId", serverId);
    message.addProperty("townId", townId);
    message.addProperty("timestamp", System.currentTimeMillis());
    if (data != null) {
      message.add("data", data);
    }

    publishToChannel(channelTownSync, message.toString());
  }

  /**
   * Send a player synchronization message.
   *
   * @param type The sync type (join, quit, balance_change, etc.)
   * @param playerUuid The player UUID
   * @param data Additional player data
   */
  public static void syncPlayer(String type, String playerUuid, JsonObject data) {
    JsonObject message = new JsonObject();
    message.addProperty("type", type);
    message.addProperty("serverId", serverId);
    message.addProperty("playerUuid", playerUuid);
    message.addProperty("timestamp", System.currentTimeMillis());
    if (data != null) {
      message.add("data", data);
    }

    publishToChannel(channelPlayerSync, message.toString());
  }

  /**
   * Shutdown the RedisManager and cleanup resources.
   */
  public static void shutdown() {
    if (heartbeatTaskId != -1) {
      Bukkit.getScheduler().cancelTask(heartbeatTaskId);
      heartbeatTaskId = -1;
    }

    if (redisClient != null) {
      // Send shutdown event
      JsonObject shutdown = new JsonObject();
      shutdown.addProperty("type", "shutdown");
      shutdown.addProperty("serverId", serverId);
      shutdown.addProperty("timestamp", System.currentTimeMillis());
      publishToChannel(channelServerEvents, shutdown.toString());

      // Delete heartbeat key
      String heartbeatKey = "tan:heartbeat:" + serverId;
      redisClient.getBucket(heartbeatKey).delete();

      redisClient.shutdown();
      redisClient = null;
    }

    onlineServers.clear();
    messageListeners.clear();

    logger.info("[TaN-Redis] RedisManager shut down");
  }

  /**
   * Get the Redisson client.
   */
  public static RedissonClient getClient() {
    return redisClient;
  }

  /**
   * Get the unique server ID.
   */
  public static String getServerId() {
    return serverId;
  }

  /**
   * Get the server name.
   */
  public static String getServerName() {
    return serverName;
  }

  /**
   * Check if a server is online (based on heartbeat).
   */
  public static boolean isServerOnline(String serverId) {
    Long lastHeartbeat = onlineServers.get(serverId);
    if (lastHeartbeat == null) {
      // Check Redis directly
      String key = "tan:heartbeat:" + serverId;
      String data = redisClient.<String>getBucket(key).get();
      return data != null;
    }
    return (System.currentTimeMillis() - lastHeartbeat) < (heartbeatTimeout * 1000L);
  }

  /**
   * Get all online servers.
   */
  public static Set<String> getOnlineServers() {
    return new HashSet<>(onlineServers.keySet());
  }

  /**
   * Get the global channel name.
   */
  public static String getChannelGlobal() {
    return channelGlobal;
  }

  /**
   * Get the server events channel name.
   */
  public static String getChannelServerEvents() {
    return channelServerEvents;
  }

  /**
   * Get the town sync channel name.
   */
  public static String getChannelTownSync() {
    return channelTownSync;
  }

  /**
   * Get the player sync channel name.
   */
  public static String getChannelPlayerSync() {
    return channelPlayerSync;
  }

  /**
   * Check if Redis is enabled and connected.
   */
  public static boolean isEnabled() {
    return redisClient != null;
  }
}
