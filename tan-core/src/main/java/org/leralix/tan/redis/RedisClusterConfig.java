package org.leralix.tan.redis;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;

public class RedisClusterConfig {
  private static final Logger logger = Logger.getLogger(RedisClusterConfig.class.getName());

  public static RedissonClient createRedisClient(FileConfiguration config) {
    String mode = config.getString("redis.mode", "single").toLowerCase();
    boolean enabled = config.getBoolean("redis.enabled", true);

    if (!enabled) {
      logger.info("[TaN-Redis] Redis is disabled in config.yml");
      return null;
    }

    logger.info("[TaN-Redis] Initializing Redis in mode: " + mode);
    switch (mode) {
      case "cluster":
        return createClusterClient(config);
      case "sentinel":
        return createSentinelClient(config);
      case "single":
      default:
        return createSingleClient(config);
    }
  }

  private static RedissonClient createSingleClient(FileConfiguration config) {
    Config redisConfig = new Config();
    String host = config.getString("redis.single.host", "127.0.0.1");
    int port = config.getInt("redis.single.port", 6379);
    String password = config.getString("redis.password", "");
    String username = config.getString("redis.username", "");
    int database = config.getInt("redis.database", 0);

    // Connection parameters from config
    int connectionTimeout = config.getInt("redis.connection.timeout", 3000);
    int retryAttempts = config.getInt("redis.connection.retry-attempts", 3);
    int retryInterval = config.getInt("redis.connection.retry-interval", 1500);
    int subscriptionPoolSize = config.getInt("redis.connection.subscription-connection-pool-size", 50);
    int subscriptionsPerConnection = config.getInt("redis.connection.subscriptions-per-connection", 5);
    int connectionPoolSize = config.getInt("redis.connection.connection-pool-size", 64);
    int connectionMinIdleSize = config.getInt("redis.connection.connection-minimum-idle-size", 10);
    boolean keepAlive = config.getBoolean("redis.connection.keep-alive", true);
    int pingInterval = config.getInt("redis.connection.ping-interval", 30000);

    String address = "redis://" + host + ":" + port;
    SingleServerConfig serverConfig =
        redisConfig
            .useSingleServer()
            .setAddress(address)
            .setDatabase(database)
            .setConnectionPoolSize(connectionPoolSize)
            .setConnectionMinimumIdleSize(connectionMinIdleSize)
            .setConnectTimeout(connectionTimeout)
            .setRetryAttempts(retryAttempts)
            .setRetryInterval(retryInterval)
            .setSubscriptionConnectionPoolSize(subscriptionPoolSize)
            .setSubscriptionsPerConnection(subscriptionsPerConnection)
            .setTimeout(config.getInt("redis.timeout", 5000))
            .setPingConnectionInterval(pingInterval)
            .setKeepAlive(keepAlive);

    // Configure username/password (Redis 6.0+ ACL support)
    if (username != null && !username.isEmpty()) {
      serverConfig.setUsername(username);
    }
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(password);
    }

    logger.info("[TaN-Redis] Single server mode: " + address + " (database: " + database + ")");
    return Redisson.create(redisConfig);
  }

  private static RedissonClient createClusterClient(FileConfiguration config) {
    Config redisConfig = new Config();
    List<String> nodes = config.getStringList("redis.cluster.nodes");
    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException("Redis cluster nodes not configured in config.yml");
    }
    String password = config.getString("redis.password", "");
    String username = config.getString("redis.username", "");
    int scanInterval = config.getInt("redis.cluster.scan-interval", 5000);
    int maxRedirects = config.getInt("redis.cluster.max-redirects", 5);

    // Connection parameters
    int connectionPoolSize = config.getInt("redis.connection.connection-pool-size", 64);
    int connectionMinIdleSize = config.getInt("redis.connection.connection-minimum-idle-size", 10);
    int connectionTimeout = config.getInt("redis.connection.timeout", 3000);
    int retryAttempts = config.getInt("redis.connection.retry-attempts", 3);
    int retryInterval = config.getInt("redis.connection.retry-interval", 1500);

    ClusterServersConfig clusterConfig =
        redisConfig
            .useClusterServers()
            .setScanInterval(scanInterval)
            .setMaxRedirects(maxRedirects)
            .setMasterConnectionPoolSize(connectionPoolSize)
            .setSlaveConnectionPoolSize(connectionPoolSize)
            .setMasterConnectionMinimumIdleSize(connectionMinIdleSize)
            .setSlaveConnectionMinimumIdleSize(connectionMinIdleSize)
            .setConnectTimeout(connectionTimeout)
            .setRetryAttempts(retryAttempts)
            .setRetryInterval(retryInterval);

    for (String node : nodes) {
      if (!node.startsWith("redis://")) {
        node = "redis://" + node;
      }
      clusterConfig.addNodeAddress(node);
    }

    if (username != null && !username.isEmpty()) {
      clusterConfig.setUsername(username);
    }
    if (password != null && !password.isEmpty()) {
      clusterConfig.setPassword(password);
    }

    logger.info("[TaN-Redis] Cluster mode with " + nodes.size() + " nodes");
    return Redisson.create(redisConfig);
  }

  private static RedissonClient createSentinelClient(FileConfiguration config) {
    Config redisConfig = new Config();
    String masterName = config.getString("redis.sentinel.master-name");
    List<String> sentinels = config.getStringList("redis.sentinel.nodes");
    if (masterName == null || masterName.isEmpty()) {
      throw new IllegalArgumentException("Redis sentinel master-name not configured");
    }
    if (sentinels == null || sentinels.isEmpty()) {
      throw new IllegalArgumentException("Redis sentinel nodes not configured");
    }
    String password = config.getString("redis.password", "");
    String username = config.getString("redis.username", "");
    int database = config.getInt("redis.database", 0);

    // Connection parameters
    int connectionPoolSize = config.getInt("redis.connection.connection-pool-size", 64);
    int connectionMinIdleSize = config.getInt("redis.connection.connection-minimum-idle-size", 10);
    int connectionTimeout = config.getInt("redis.connection.timeout", 3000);
    int retryAttempts = config.getInt("redis.connection.retry-attempts", 3);
    int retryInterval = config.getInt("redis.connection.retry-interval", 1500);
    int scanInterval = config.getInt("redis.connection.ping-interval", 30000);

    SentinelServersConfig sentinelConfig =
        redisConfig
            .useSentinelServers()
            .setMasterName(masterName)
            .setDatabase(database)
            .setMasterConnectionPoolSize(connectionPoolSize)
            .setSlaveConnectionPoolSize(connectionPoolSize)
            .setMasterConnectionMinimumIdleSize(connectionMinIdleSize)
            .setSlaveConnectionMinimumIdleSize(connectionMinIdleSize)
            .setConnectTimeout(connectionTimeout)
            .setRetryAttempts(retryAttempts)
            .setRetryInterval(retryInterval)
            .setScanInterval(scanInterval);

    for (String sentinel : sentinels) {
      if (!sentinel.startsWith("redis://")) {
        sentinel = "redis://" + sentinel;
      }
      sentinelConfig.addSentinelAddress(sentinel);
    }

    if (username != null && !username.isEmpty()) {
      sentinelConfig.setUsername(username);
    }
    if (password != null && !password.isEmpty()) {
      sentinelConfig.setPassword(password);
    }

    logger.info(
        "[TaN-Redis] Sentinel mode with master: "
            + masterName
            + ", "
            + sentinels.size()
            + " sentinels");
    return Redisson.create(redisConfig);
  }
}
